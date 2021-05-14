package com.example.wearablenotification.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.wearablenotification.R
import com.example.wearablenotification.main.intersection.checkIntersections
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var cameraExecutor: ExecutorService
    private var trafficLightIsDetected = false

    private var speed = 0.0 // 車の移動速度[km/h]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // sleep locked


        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        requestPermission()

        // 位置情報のコールバック
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for(location in locationResult.locations) {
                    // 時速に変換
                    speed = location.speed * 3.6
                    Log.d(TAG, "speed: %.3f".format(speed))
                    // 予測地点に交差点があるなら画像処理を行う
                    trafficLightIsDetected = if(checkIntersections(location)) {
                        Log.d(TAG, "進行方向に交差点があります")
                        true
                    } else {
                        Log.d(TAG, "交差点を探しています...")
                        false
                    }
                }
            }
        }

        //startCamera()

        //cameraExecutor = Executors.newSingleThreadExecutor()
    }

    inner class TrafficLightAnalyzer() : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {

            if(trafficLightIsDetected) {
                TODO("Not yet implemented")
            }


            runOnUiThread(Runnable() {
                run() {
                    TODO("bitmapをresult_image_view_mainに表示")
                }
            })
        }


    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, TrafficLightAnalyzer())
                    }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                        this, cameraSelector, imageAnalyzer
                )
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    override fun onRequestPermissionsResult(
            requestCode: Int,
            permissions: Array<String>,
            grantResults: IntArray) {

        if(requestCode != LOCATION_PERMISSION_REQUEST_CODE) return
        if(isPermissionGranted()) {
            if(ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, LOCATION_PERMISSION_REQUEST_CODE)
            }
        } else {
            Toast.makeText(this,
                    "位置情報の使用が拒否されたため終了しました",
                    Toast.LENGTH_LONG).show()
            finish()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(this,
                REQUIRED_PERMISSIONS,
                LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    private fun isPermissionGranted() = REQUIRED_PERMISSIONS.all {
        ActivityCompat.checkSelfPermission(
                this, it
        ) == PackageManager.PERMISSION_GRANTED
    }

    override fun onResume() {
        super.onResume()
        Log.d(TAG, "onResume called")
        startLocationUpdates()
    }

    override fun onPause() {
        super.onPause()
        Log.d(TAG, "onPause called")
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        val locationRequest = createLocationRequest() ?: return
        fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null)
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun createLocationRequest(): LocationRequest? {
        return LocationRequest.create()?.apply {
            // interval [ms]
            interval = 100
            // 高精度なリクエスト
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }
    }




    companion object {
        private const val TAG = "Main"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}