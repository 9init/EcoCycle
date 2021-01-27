package com.lilHesham.ecocycle

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.lang.Exception
import java.util.*
import kotlin.collections.ArrayList
import kotlin.math.log

class TrainingActivity: AppCompatActivity() {
    lateinit var photoBtn:Button
    lateinit var galleryBtn:Button
    lateinit var trainBtn:Button
    private var g1RadioButton: RadioGroup? = null
    private var g2RadioButton: RadioGroup? = null
    private var imageView: ImageView? = null
    private var selectedIndex: Int? = null
    private var bmpToTrain:Bitmap? = null



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.training_activity)

        g1RadioButton = null
        g2RadioButton = null
        imageView = findViewById(R.id.imageView)
        photoBtn = findViewById(R.id.camera_button)
        galleryBtn = findViewById(R.id.gallery_button)
        trainBtn = findViewById(R.id.trainBtn)
        g1RadioButton = findViewById(R.id.g1RadioButton)
        g2RadioButton = findViewById(R.id.g2RadioButton)

        var listener1:RadioGroup.OnCheckedChangeListener? = null
        var listener2:RadioGroup.OnCheckedChangeListener? = null

        grantPerms(this@TrainingActivity)
        if (!areFilesExist())
            extractFiles(this@TrainingActivity)

        galleryBtn.setOnClickListener {
            setClickableModeForClickAble(false)
            openGallery(this@TrainingActivity)
            setClickableModeForClickAble(true)
        }

        photoBtn.setOnClickListener{
            setClickableModeForClickAble(false)
            if(checkPerms(this@TrainingActivity)) {
                val imageUri= getFileUri("image")
                val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
                startActivityForResult(cameraIntent, CAMERA_REQUEST)

            }
            setClickableModeForClickAble(true)
        }

        listener1 = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1){
                g2RadioButton!!.setOnCheckedChangeListener(null)
                g2RadioButton!!.clearCheck()
                selectedIndex = checkedId
                g2RadioButton!!.setOnCheckedChangeListener(listener2)
            }
        }

        listener2 = RadioGroup.OnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1){
                g1RadioButton!!.setOnCheckedChangeListener(null)
                g1RadioButton!!.clearCheck()
                selectedIndex = checkedId
                g1RadioButton!!.setOnCheckedChangeListener(listener1)
            }
        }

        g1RadioButton!!.setOnCheckedChangeListener(listener1)
        g2RadioButton!!.setOnCheckedChangeListener(listener2)



        trainBtn.setOnClickListener {
            setClickableModeForClickAble(false)
            bmpToTrain?.let {
                trainBtn.text = "Training..."
                Thread{
                    run(){
                        val list: ArrayList<Int> = ArrayList(Collections.nCopies(8, 0))
                        selectedIndex?.let {
                            val idString = resources.getResourceEntryName(selectedIndex!!)
                            val index = idString[1].toString().toInt()
                            list[index-1] = 1
                            val output = list.toString().replace("[","").replace("]","").replace(" ","")
                            val process = train(bmpToTrain!!, output)

                            runOnUiThread {
                                if(process?.exitValue() != 0)
                                    Toast.makeText(this@TrainingActivity, "error occurred", Toast.LENGTH_LONG).show()
                                trainBtn.text = "Train"
                                Toast.makeText(this@TrainingActivity, "Finished", Toast.LENGTH_LONG).show()
                            }
                        }?: runOnUiThread {
                            trainBtn.text = "Train"
                            Toast.makeText(this@TrainingActivity, "Please select a type", Toast.LENGTH_SHORT).show()
                        }
                    }
                }.start()
            }?: Toast.makeText(this@TrainingActivity, "you have to take a photo", Toast.LENGTH_SHORT).show()
            setClickableModeForClickAble(true)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == CAMERA_REQUEST && resultCode == RESULT_OK){
            performCrop(getFileUri("image")!!, this@TrainingActivity)
        }else if(requestCode == 1234 && resultCode == RESULT_OK){
            imageView?.scaleType = ImageView.ScaleType.CENTER_CROP
            val uri:Uri? = data?.data
            if (null != uri) {
                bmpToTrain = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                imageView?.setImageBitmap(bmpToTrain)
                deleteFileFromUri(getFileUri("image")!!)
                deleteFileFromUri(uri)
            } else {
                bmpToTrain = data?.extras?.get("data") as Bitmap
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
                performCrop(imgUri, this@TrainingActivity)
            } else {
                Toast.makeText(this@TrainingActivity, "Error picking photo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String?>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == MY_CAMERA_PERMISSION_CODE) {
            if (grantResults[0] != PackageManager.PERMISSION_GRANTED && grantResults[1] != PackageManager.PERMISSION_GRANTED && grantResults[2] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "camera/storage permission denied", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun setClickableModeForClickAble(mode:Boolean){
        photoBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
        trainBtn.apply {
            isClickable = mode
            isLongClickable = mode
            isEnabled = mode
            isFocusable = mode
        }
    }

    private fun train(bmp: Bitmap, output: String):Process?{
        return try {
            val fos = FileOutputStream("$appFileDirectory/image.jpeg",false)
            bmp.compress(Bitmap.CompressFormat.JPEG, 100, fos)
            val p = Runtime.getRuntime().exec(arrayOf("./$appFileDirectory/${getDeviceArch()}", "train", "${appFileDirectory}/image.jpeg", output))
            p.waitFor()
            p
        }catch (e : Exception){
            Toast.makeText(this@TrainingActivity, e.stackTraceToString(), Toast.LENGTH_LONG).show()
            null
        }
    }
}