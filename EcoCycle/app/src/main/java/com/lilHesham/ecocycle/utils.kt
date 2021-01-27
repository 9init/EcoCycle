package com.lilHesham.ecocycle

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.core.app.ActivityCompat.startActivityForResult
import java.io.BufferedReader
import java.io.File
import java.io.FileOutputStream
import java.io.InputStreamReader


@SuppressLint("SdCardPath")
val appFileDirectory = "/data/data/com.lilHesham.ecocycle/binaries"
val arm64V8a = File("$appFileDirectory/arm64-v8a")
val armeabiV7a = File("$appFileDirectory/armeabi-v7a")
val trainedData = File("$appFileDirectory/trained-Data.json")
val MY_CAMERA_PERMISSION_CODE = 100
val CAMERA_REQUEST: Int = 1888
val PICK_IMAGE : Int = 1999


fun areFilesExist():Boolean{
    return arm64V8a.exists() && armeabiV7a.exists() && trainedData.exists()
}

fun extractFiles(context: Context){
    //copying file
    val inputStream = context.assets.open("data.tgz")
    val outputFile = File(appFileDirectory, "data.tgz")
    outputFile.parentFile?.mkdirs()
    val outputStream = FileOutputStream(outputFile)
    inputStream.copyTo(outputStream)
    //extracting files
    Runtime.getRuntime().exec("tar -C $appFileDirectory -xzf $appFileDirectory/data.tgz").waitFor()
    outputFile.delete()
    arm64V8a.setExecutable(true)
    armeabiV7a.setExecutable((true))
}



fun checkPerms(context: Context):Boolean{
    return context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED && context.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED  && context.checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
}

fun grantPerms(activity: Activity){
    activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.CAMERA), MY_CAMERA_PERMISSION_CODE)
}

fun getDeviceArch():String {
    val process = Runtime.getRuntime().exec("getprop ro.product.cpu.abi")
    val reader = BufferedReader(InputStreamReader(process.inputStream))
    return reader.readText().trim()
}

fun deleteFileFromUri(fileUri: Uri){
        val path = getRealPathFromURI(fileUri)
        val file = File(path!!)
        file.delete()
}

fun getRealPathFromURI(contentUri: Uri?): String? {
    return contentUri?.path
}

fun getFileUri(name: String): Uri?{
        val outputFile = File(Environment.getExternalStoragePublicDirectory("EcoCycle"), "$name.jpeg")
        outputFile.parentFile!!.mkdirs()
        return Uri.fromFile(outputFile)
}

fun openGallery(activity: Activity){
    val intent = Intent()
    intent.type = "image/*"
    intent.action = Intent.ACTION_OPEN_DOCUMENT
    activity.startActivityForResult(Intent.createChooser(intent, "Select Picture"), PICK_IMAGE)
}

fun performCrop(picUri: Uri, activity: Activity) {
    try {
        val cropIntent = Intent("com.android.camera.action.CROP")
        cropIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        cropIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        // indicate image type and Uri
        cropIntent.setDataAndType(picUri, "image/*")
        // set crop properties here
        cropIntent.putExtra("crop", true)
        // indicate aspect of desired crop
        cropIntent.putExtra("aspectX", 1)
        cropIntent.putExtra("aspectY", 1)
        // indicate output X and Y
//            cropIntent.putExtra("outputX", 200)
//            cropIntent.putExtra("outputY", 200)
        // retrieve data on return
        cropIntent.putExtra("return-data", true)
        // start the activity - we handle returning in onActivityResult
        activity.startActivityForResult(cropIntent, 1234)
    } // respond to users whose devices do not support the crop action
    catch (anfe: ActivityNotFoundException) {
        // display an error message
        val errorMessage = "Whoops - your device doesn't support the crop action!"
        val toast = Toast.makeText(activity, errorMessage, Toast.LENGTH_SHORT)
        toast.show()
    }
}