package com.example.logoapplicationyolo

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.location.Location
import android.os.Bundle
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull

class CameraActivityYolo : AppCompatActivity() {

    private lateinit var imageView: ImageView
    private lateinit var detectButton: Button
    private lateinit var resultTextView: TextView
    //private lateinit var resultTextView1: TextView
    private lateinit var resultTextView2: TextView
    private val CAMERA_PERMISSION_REQUEST_CODE = 100
    private val cloudVisionApiKey = "" // Add your api key
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var yoloModel: YoloTfliteInference
    private lateinit var recyclerView: RecyclerView

    private var userLocation: Location? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera_yolo)

        imageView = findViewById(R.id.imageView)
        detectButton = findViewById(R.id.detectButton)
        resultTextView = findViewById(R.id.resultTextView)
        //resultTextView1 = findViewById(R.id.resultTextView1)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        val files = assets.list("")
        for (file in files!!) {
            Log.d("Assets", "File: $file")
        }
        //yoloModel = YoloTfliteInference("best_float32.tflite",assets)
        //yoloModel = YoloTfliteInference("best_multilabels.tflite",assets)
        yoloModel = YoloTfliteInference("best_float32_gpu.tflite",assets)

        getUserLocation()
        // Open camera when button is clicked
        detectButton.setOnClickListener {
            requestCameraPermission()
        }
    }

    private fun requestCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_REQUEST_CODE)
        } else {
            openCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera()
            } else {
                Toast.makeText(this, "Camera permission denied", Toast.LENGTH_SHORT).show()
            }
        }
    }
    // Open Camera
    private fun openCamera() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        cameraLauncher.launch(intent)
    }

    // Handle camera result
    private val cameraLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val imageBitmap = result.data?.extras?.get("data") as Bitmap
            imageView.setImageBitmap(imageBitmap)

            // Uncomment below to use the Yolo Model you have created.
            //runYoloInference(imageBitmap)
            sendToGoogleVision(imageBitmap)


        }
    }

    private fun runYoloInference(bitmap: Bitmap) {
        try {
            val detectedMerchant = yoloModel.runInference(bitmap)
            runOnUiThread {
                resultTextView.text = detectedMerchant
                sendMerchantToBackend(detectedMerchant)
            }
        } catch (e: IOException) {
            Log.e("YOLO Inference", "Error running inference: ${e.message}")
        }
    }

    private fun sendToGoogleVision(bitmap: Bitmap) {
        val base64Image = convertBitmapToBase64(bitmap)

        val jsonBody = JSONObject().apply {
            put("requests", JSONObject().apply {
                put("image", JSONObject().apply {
                    put("content", base64Image)
                })
                put("features", JSONObject().apply {
                    put("type", "LOGO_DETECTION")
                })
            })
        }

        val requestBody = RequestBody.create("application/json".toMediaTypeOrNull(), jsonBody.toString())

        val request = Request.Builder()
            .url("https://vision.googleapis.com/v1/images:annotate?key=$cloudVisionApiKey")
            .post(requestBody)
            .build()

        val client = OkHttpClient()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("VisionAPI", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.let { responseBody ->
                    val responseString = responseBody.string()
                    val jsonResponse = JSONObject(responseString)
                    println(jsonResponse)
                    if(jsonResponse.getJSONArray("responses").getJSONObject(0).has("logoAnnotations")) {
                        val logoName = jsonResponse.getJSONArray("responses")
                            .getJSONObject(0)
                            .getJSONArray("logoAnnotations")
                            .getJSONObject(0)
                            .getString("description")

                        runOnUiThread {
                            resultTextView.text = "Detected Merchant: $logoName"
                            sendMerchantToBackend(logoName)
                        }
                    }
                    else{
                        resultTextView.text = "No merchant detected"
                    }
                }
            }
        })
    }

    private fun convertBitmapToBase64(bitmap: Bitmap): String {
        val byteArrayOutputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteArrayOutputStream)
        return Base64.encodeToString(byteArrayOutputStream.toByteArray(), Base64.DEFAULT)
    }

    private fun sendMerchantToBackend(merchantName: String) {
//        resultTextView1.text = "Offers: \n10 % off on your sports shoes!\n" +
//                "Buy 1 get 1 free on slippers. Clearance sale!\n"+
//                "Upto 1000 off on apparals!\n"

        var offers: List<Offer> = mutableListOf()
        println(merchantName)
        if(merchantName.contains("NIKE", true))
        {
            offers = listOf(
                Offer("🔥 10% off on your sports shoes!"),
                Offer("🛍️ Buy 1 get 1 free on slippers. Clearance sale!"),
                Offer("👗 Up to ₹1000 off on apparels!")
            )
        }
        else {
            offers = listOf(
                Offer("🔥 10% off on your next order!"),
                Offer("🛍️ Clearance sale on merchandise!"),
                Offer("☕ Up to ₹200 off on any large coffee!")
            )
        }

        val recyclerView: RecyclerView = findViewById(R.id.offersRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = OfferAdapter(offers)
        /*val requestBody = JSONObject().apply {
            put("merchant", merchantName)
            put("location", getUserLocation())
        }

        val request = Request.Builder()
            .url("https://your-backend.com/get-offers")
            .post(RequestBody.create("application/json".toMediaTypeOrNull(), requestBody.toString()))
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Backend", "Error: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    resultTextView.text = "Offers: ${response.body?.string()}"
                }
            }
        })*/
    }

    @SuppressLint("MissingPermission")
    private fun getUserLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1)
            return
        }

        fusedLocationClient.lastLocation.addOnSuccessListener { location: Location? ->
            if (location != null) {
                userLocation = location
                Log.d("Location", "Lat: ${location.latitude}, Lng: ${location.longitude}")
            }
        }
    }
}
