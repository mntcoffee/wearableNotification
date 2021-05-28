package com.example.wearablenotification.main

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.SoundPool
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
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.wearablenotification.R
import com.example.wearablenotification.main.intersection.checkIntersections
import com.example.wearablenotification.setup.SetupActivity
import com.google.android.gms.location.*
import kotlinx.android.synthetic.main.activity_main.*
import java.security.spec.MGF1ParameterSpec
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var cameraExecutor: ExecutorService
    private var trafficLightIsDetected = false

    // notification
    private lateinit var soundPool: SoundPool
    private lateinit var audioAttributes: AudioAttributes
    private var alert1 = 0
    private lateinit var notificationBuilderPattern1: NotificationCompat.Builder
    private lateinit var notificationBuilderPattern2: NotificationCompat.Builder

    private var speed = 0.0 // 車の移動速度[km/h]

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // sleep locked

        // soundPool の初期化
        initSoundPool()

        // buildVibration
        buildVibrator()

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

    inner class TrafficLightAnalyzer() : ImageAnalysis.Analyzer {
        override fun analyze(image: ImageProxy) {

            if(trafficLightIsDetected) {
                TODO("Not yet implemented. detect traffic light by tfFlow-light")
            } else {
                TODO("Not yet implemented. not in the traffic intersection, so not analyze")
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

    /*------------- Notification -------------*/

    private fun initSoundPool() {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(1)
            .build()

        alert1 = soundPool.load(this, R.raw.alert1, 1)

        soundPool.setOnLoadCompleteListener{ _: SoundPool, sampleId: Int, status: Int ->
            Log.d(TAG, "sampleId = $sampleId")
            Log.d(TAG, "status = $status")
        }
    }

    private fun buildVibrator() {
        notificationBuilderPattern1 = NotificationCompat.Builder(this, CHANNEL_ID_1)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("パターン1")
            .setContentText("通知テスト")
            .setPriority(NotificationCompat.PRIORITY_MAX)   /// 通知の優先度(緊急)
            .setVibrate(longArrayOf(0, 200, 25, 200, 25, 1000))

        notificationBuilderPattern2 = NotificationCompat.Builder(this, CHANNEL_ID_2)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("パターン2")
            .setContentText("通知テスト")
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // 通知の優先度(高)
            .setVibrate(longArrayOf(0, 100, 100, 100, 100, 100))  // 通知の振動設定
    }

    private fun alert01() {
        soundPool.play(alert1, 1.0f, 1.0f, 5, 2, 1.0f)

        // 通知の表示
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern1.build())
        }

        Log.d(TAG, "pattern1: 50km/h over")
    }

    private fun alert02() {
        // 通知の表示
        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern2.build())
        }

        Log.d(TAG, "pattern2: from 10km/h to 50km/h")

    }

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
        private const val TAG = "Main"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        private const val NOTIFICATION_ID = 10
        private const val CHANNEL_ID_1 = "channel_01"
        private const val CHANNEL_ID_2 = "channel_02"
    }
}