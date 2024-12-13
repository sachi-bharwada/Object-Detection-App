package com.example.objectdetection

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeler
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.objects.DetectedObject
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions

class MainActivity : AppCompatActivity() {
    companion object {
        private const val IMAGE_REQUEST_CODE = 5
        private const val CAMERA_PERMISSION_CODE = 1 // for permission code
        private const val CAMERA_REQUEST_CODE = 2 // for the intent
    }
    var  bitmap: Bitmap? = null
    var Gate: String = "No"
    var objLabel: String = "I DON'T KNOW??"
    var Index : Int = 0
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val button1 : Button = findViewById(R.id.button)
        val button2 : Button = findViewById(R.id.button3)
        val button3 : Button = findViewById(R.id.button2)
        val switch1 : Switch = findViewById(R.id.switch1)
        val text : TextView = findViewById(R.id.textView)

        switch1.setOnClickListener{
            if(switch1.isChecked){
                Gate = "Yes"
            }
            else {
                Gate = "No"
            }
            switch1.text =  "Using image labeling model : " + Gate
        }

        button1.setOnClickListener{
            pickImageGallery()
        }

        button2.setOnClickListener{
            text.text = ""
            text.alpha = 0f
            Index = 0
            findObjects(bitmap,text)
        }

        button3.setOnClickListener{
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED){
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            } else {
                ActivityCompat.requestPermissions(this,
                    arrayOf(android.Manifest.permission.CAMERA),
                    CAMERA_PERMISSION_CODE)
            }
        }
    }

    private fun findObjects(bitmap: Bitmap?,text: TextView) {
        val options = ObjectDetectorOptions.Builder()
            .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
            .enableMultipleObjects()
            .enableClassification()
            .build()

        val objectDetector = ObjectDetection.getClient(options)
        val labeler = ImageLabeling.getClient(ImageLabelerOptions.DEFAULT_OPTIONS)

        if (bitmap != null) {
            var image = InputImage.fromBitmap(bitmap, 0)
            objectDetector.process(image).addOnSuccessListener { objects ->
                bitmap.apply {
                    writeLabelsAndDrawRectangles(objects, labeler, text)
                }

            }
        }
    }

    fun Bitmap.writeLabelsAndDrawRectangles(objects:List<DetectedObject>, labeler: ImageLabeler, text:TextView):Bitmap? {
        val imageView : ImageView = findViewById(R.id.imageView)
        var myBitmap = config?.let { copy(it, true) }
        val canvas = myBitmap?.let { Canvas(it) }
        var outputText = ""
        var objIndx = 0
        for (obj in objects) {
            val boundingBox = obj.boundingBox
            var prevConf: Int = 0
            var currConf: Int = 0
            var objLabel = "I DON'T KNOW"
            objIndx  =  objIndx + 1
            if (Gate == "No") {
                for (label in obj.labels) {
                    currConf = label.confidence.toInt()
                    if (currConf >= prevConf) {
                        objLabel = label.text.toString()
                    }
                    prevConf = currConf
                }
            }
            else if (Gate == "Yes") {
                val croppedBitmap = myBitmap?.let {
                    Bitmap.createBitmap(
                        it,
                        boundingBox.left,
                        boundingBox.top,
                        boundingBox.width(),boundingBox.height(),
                    )
                }
                val img = croppedBitmap?.let { InputImage.fromBitmap(it, 0) }

                if (img != null) {
                    labeler.process(img).addOnSuccessListener {labels ->
                        Index += 1
                        if (labels.count() > 0){
                            outputText = text.text.toString()
                            outputText = outputText + Index + "-->" + labels[0].text.toString() + ":" + labels[0].confidence.toString() + "\n"
                            text.text = outputText
                            text.alpha = 0.8f
                        }
                    }
                }

                objLabel = objIndx.toString()
            }


            Paint().apply {
                color = Color.RED
                style = Paint.Style.STROKE
                textSize = 32.0f
                strokeWidth = 4.0f
                isAntiAlias = true
                if (canvas != null) {
                    canvas.drawRect(boundingBox, this)
                }
                if (canvas != null) {
                    canvas.drawText(
                        objLabel.toString(),
                        boundingBox.left.toFloat(),
                        boundingBox.top.toFloat(), this
                    )
                }
            }

        }

        imageView.setImageBitmap(myBitmap)
        return myBitmap

    }


    private fun pickImageGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/"
        startActivityForResult(intent, IMAGE_REQUEST_CODE)

    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if(requestCode == CAMERA_PERMISSION_CODE ){
            if(grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED){
                startActivityForResult(intent, CAMERA_REQUEST_CODE)
            }else{
                Toast.makeText(this, "Oops you just denied the permission for camera",
                    Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val imageView : ImageView = findViewById(R.id.imageView)
        if(resultCode == Activity.RESULT_OK){
            if(requestCode == IMAGE_REQUEST_CODE){
                //val bitmap: Bitmap = data!!.extras!!.get("data") as Bitmap
                val imageUri: Uri? = data?.data
                bitmap = MediaStore.Images.Media.getBitmap(
                    contentResolver,Uri.parse(imageUri.toString()))
                imageView.setImageBitmap(bitmap)
            }
            else if (requestCode == CAMERA_REQUEST_CODE){
                bitmap = data!!.extras!!.get("data") as Bitmap
                imageView.setImageBitmap(bitmap)
            }
        }
    }
}