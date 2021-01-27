package com.lilHesham.ecocycle

import android.Manifest
import android.annotation.SuppressLint
import android.content.*
import android.content.IntentSender.SendIntentException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment.getExternalStoragePublicDirectory
import android.os.StrictMode
import android.os.StrictMode.VmPolicy
import android.provider.MediaStore
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import org.jetbrains.anko.activityManager
import java.io.*
import java.lang.Exception


@SuppressLint("SdCardPath")
class MainActivity : AppCompatActivity() {

    private var imageView: ImageView? = null
    private var bmpToPredict:Bitmap? = null


    lateinit var percentA:TextView
    lateinit var percentB:TextView
    lateinit var percentC:TextView

    lateinit var progressBarA:ProgressBar
    lateinit var progressBarB:ProgressBar
    lateinit var progressBarC:ProgressBar

    lateinit var plasticA:TextView
    lateinit var plasticB:TextView
    lateinit var plasticC:TextView

    //Clickable Items
    lateinit var resetBtn:Button
    lateinit var predictBtn:Button
    lateinit var photoBtn:Button
    lateinit var galleryBtn:Button
    lateinit var fixMissingBtn:TextView
    lateinit var trainModuleBtn:TextView

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val builder = VmPolicy.Builder()
        StrictMode.setVmPolicy(builder.build())

        grantPerms(this@MainActivity)
        if (!areFilesExist())
            extractFiles(this@MainActivity)


        percentA = findViewById(R.id.percentA)
        percentB = findViewById(R.id.percentB)
        percentC = findViewById(R.id.percentC)

        progressBarA = findViewById(R.id.progress_barA)
        progressBarB = findViewById(R.id.progress_barB)
        progressBarC = findViewById(R.id.progress_barC)

        plasticA = findViewById(R.id.plasticA)
        plasticB = findViewById(R.id.plasticB)
        plasticC = findViewById(R.id.plasticC)

        photoBtn = findViewById(R.id.camera_button)
        galleryBtn = findViewById(R.id.gallery_button)
        predictBtn = findViewById(R.id.predict_button)
        resetBtn = findViewById(R.id.reset_button)

        fixMissingBtn = findViewById(R.id.fixMissing)
        trainModuleBtn = findViewById(R.id.trainModule)

        imageView = findViewById(R.id.imageView)

        galleryBtn.setOnClickListener {
            setClickableModeForClickAble(false)
            openGallery(this@MainActivity)
            setClickableModeForClickAble(true)
        }

        photoBtn.setOnClickListener{
            setClickableModeForClickAble(false)
            if(checkPerms(this@MainActivity)) {
                val imageUri= getFileUri("image")
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)

            }
            setClickableModeForClickAble(true)
        }

        predictBtn.setOnClickListener {
            setClickableModeForClickAble(false)
            bmpToPredict?.let {
                predictBtn.text = "Predicting..."
                Thread{
                    run(){
                        val process = predict(bmpToPredict!!)
                        process?.apply {
                            val output:String
                             if(process.exitValue() != 0) {
                                 output = BufferedReader(InputStreamReader(process.errorStream)).readText()
                                 runOnUiThread { Toast.makeText(this@MainActivity, output, Toast.LENGTH_LONG).show() }
                                 return@run
                             }else
                                output = BufferedReader(InputStreamReader(process.inputStream)).readText()

                            val data = utils().extractInfo(output)
                            runOnUiThread {

                                progressBarA.progress = (data[0].value * 100).toInt()
                                plasticA.text = data[0].name
                                percentA.text = (data[0].value * 100).toInt().toString()+"%"

                                progressBarB.progress = (data[1].value * 100).toInt()
                                plasticB.text = data[1].name
                                percentB.text = (data[1].value * 100).toInt().toString()+"%"

                                progressBarC.progress = (data[2].value * 100).toInt()
                                plasticC.text = data[2].name
                                percentC.text = (data[2].value * 100).toInt().toString()+"%"

                                if(progressBarA.progress == 0 && progressBarB.progress == 0 && progressBarC.progress == 0) {
                                    plasticA.text = "not a plastic"
                                    if (data[1].name == "not a plastic"){
                                        plasticB.text = data[0].name
                                    }else if (data[2].name == "not a plastic"){
                                        plasticC.text = data[1].name
                                        plasticB.text = data[0].name
                                    }else{
                                        plasticB.text = data[0].name
                                        plasticC.text = data[1].name
                                    }
                                }

                                predictBtn.text = "Predict"
                            }
                        }
                    }
                }.start()
            }
            setClickableModeForClickAble(true)
        }


        resetBtn.setOnClickListener{
            setClickableModeForClickAble(false)
            reset()
            setClickableModeForClickAble(true)
        }

        fixMissingBtn.setOnClickListener {
            setClickableModeForClickAble(false)
            val alertDialog: AlertDialog = this@MainActivity.let {
                val builder = AlertDialog.Builder(it)
                builder.apply {
                    setPositiveButton("Yes",
                            DialogInterface.OnClickListener { _, _ ->
                                fixMissing()
                            })
                    setNegativeButton("Cancel",
                            DialogInterface.OnClickListener { _, _ ->
                                //close
                            })
                    setTitle("Are you sure you?")
                }
                builder.create()
            }
            alertDialog.show()

            setClickableModeForClickAble(true)
        }

        trainModuleBtn.setOnClickListener {
            val intent = Intent(this@MainActivity, TrainingActivity::class.java)
            startActivity(intent)
        }

    }


    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED && grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera/storage permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK){
            performCrop(getFileUri("image")!!, this@MainActivity)
        }else if(requestCode == 1234 && resultCode == RESULT_OK){
            imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            val uri:Uri? = data?.data
            if (null != uri) {
                bmpToPredict = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                imageView?.setImageBitmap(bmpToPredict)
                deleteFileFromUri(getFileUri("image")!!)
                deleteFileFromUri(uri)
            } else {
                bmpToPredict = data?.extras?.get("data") as Bitmap
                val bmpURI = getFileUri("image")
                val bmp = MediaStore.Images.Media.getBitmap(contentResolver, bmpURI)
                deleteFileFromUri(bmpURI!!)
                imageView?.setImageBitmap(bmp)
            }
        }else if(requestCode == PICK_IMAGE && resultCode == RESULT_OK){
            imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            val uri:Uri? = data?.data
            if (null != uri) {
                val img = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                val out = FileOutputStream("/sdcard/EcoCycle/image.jpeg", false)
                img.compress(Bitmap.CompressFormat.JPEG, 100, out)
                val imgUri = Uri.fromFile(File("/sdcard/EcoCycle/image.jpeg"))
                performCrop(imgUri, this@MainActivity)
            } else {
                Toast.makeText(this@MainActivity, "Error picking photo", Toast.LENGTH_SHORT).show()
            }
        }
    }








    private fun predict(bmp: Bitmap):Process?{
        return try {
            val fos = FileOutputStream("$appFileDirectory/image.jpeg",false)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            val p = Runtime.getRuntime().exec(arrayOf("./$appFileDirectory/${getDeviceArch()}", "predict", "${appFileDirectory}/image.jpeg"))
            p.waitFor()
            p
        }catch (e : Exception){
            Toast.makeText(this@MainActivity, e.stackTraceToString(), Toast.LENGTH_LONG).show()
            null
        }
    }

    fun fixMissing(){
        if(arm64V8a.exists()) arm64V8a.delete()
        if(armeabiV7a.exists()) armeabiV7a.delete()
        if(trainedData.exists()) trainedData.delete()
        reset()
        grantPerms(this@MainActivity)
        extractFiles(this@MainActivity)
    }

    private fun reset(){
        imageView?.apply{
            scaleType = ImageView.ScaleType.CENTER
            setImageResource(R.drawable.ic_outline_photo_camera_24)
        }

        plasticA.text = "Plastic A"
        plasticB.text = "Plastic B"
        plasticC.text = "Plastic C"

        progressBarA.progress = 0
        progressBarB.progress = 0
        progressBarC.progress = 0

        percentA.text = "0%"
        percentB.text = "0%"
        percentC.text = "0%"

        bmpToPredict = null
    }

    private fun setClickableModeForClickAble(mode:Boolean){
        resetBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
        predictBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
        photoBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
        fixMissingBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
    }

}