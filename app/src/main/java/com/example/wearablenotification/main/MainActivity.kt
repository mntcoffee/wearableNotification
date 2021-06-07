package com.example.wearablenotification.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.wearablenotification.R
import com.example.wearablenotification.main.intersection.checkIntersections
import com.example.wearablenotification.setup.SetupActivity
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.util.concurrent.ExecutorService


class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var cameraExecutor: ExecutorService
    private var trafficLightIsDetected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // sleep locked

        // soundPool の初期化
        initSoundPool(this)

        // vibrato　の初期化
        buildVibrator(this)

        // 位置情報の取得
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // 位置情報のパーミッション取得
        requestPermission()

        // 位置情報のコールバック
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {
                locationResult ?: return
                for(location in locationResult.locations) {
                    // 時速に変換
                    speed = location.speed * 3.6
                    Log.d(TAG, "speed: %.3f km/h".format(speed))
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

    /*
    inner class TrafficLightAnalyzer() : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {

            // convert ImageProxy to bitmap

            if(trafficLightIsDetected && speed > 10.0) {
                TODO("Not yet implemented. detect traffic light by tfFlow-light")
            } else {
                TODO("Not yet implemented. not in the traffic intersection, so not analyze")
            }

            runOnUiThread(Runnable() {
                run() {
                    TODO("bitmapをresult_image_view_mainに表示")
                    // result_image_view_main.setImageBitmap(image)
                }
            })
        }

    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider()
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                    .build()
                    .also {
                        it.setAnalyzer(cameraExecutor, TrafficLightAnalyzer())
                    }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                        this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

     */


    /*------------- permission -------------*/

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
                    "位置情報の使用が拒否されたため再起動しました",
                    Toast.LENGTH_LONG).show()
            val intent = Intent(this, SetupActivity::class.java)

            startActivity(intent)
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
        const val TAG = "Main"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID_1 = "channel_01"
        const val CHANNEL_ID_2 = "channel_02"

        // notification
        lateinit var soundPool: SoundPool
        lateinit var audioAttributes: AudioAttributes
        var alert1 = 0
        lateinit var notificationBuilderPattern1: NotificationCompat.Builder
        lateinit var notificationBuilderPattern2: NotificationCompat.Builder

        var speed = 0.0 // 車の移動速度[km/h]
    }
}