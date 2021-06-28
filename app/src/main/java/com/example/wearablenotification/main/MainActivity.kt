package com.example.wearablenotification.main

import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.AssetFileDescriptor
import android.os.Bundle
import android.util.Log
import android.util.Size
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.android.camera.utils.com.example.trafficlightdetection.YuvToRgbConverter
import com.example.wearablenotification.R
import com.example.wearablenotification.main.intersection.calculatePredictedLocation
import com.example.wearablenotification.main.intersection.checkIntersections
import com.example.wearablenotification.main.intersection.checkIntersectionsCore
import com.example.wearablenotification.setup.SetupActivity
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.android.synthetic.main.activity_main.*
import org.opencv.android.OpenCVLoader
import org.tensorflow.lite.Interpreter
import java.io.BufferedReader
import java.io.FileInputStream
import java.io.InputStream
import java.io.InputStreamReader
import java.nio.ByteBuffer
import java.nio.channels.FileChannel
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


class MainActivity : AppCompatActivity(), ActivityCompat.OnRequestPermissionsResultCallback {

    private lateinit var fusedLocationClient : FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var notificator: Notificator

    // Surface Viewのコールバックをセット
    private lateinit var overlaySurfaceView: OverlaySurfaceView
    // CameraProvider
    private lateinit var cameraProviderFuture : ListenableFuture<ProcessCameraProvider>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) // sleep locked

        notificator = Notificator(this)
        // soundPool の初期化
        with(notificator) {
            this.initSoundPool()
            this.buildVibrator(
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            )
        }

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
                    val predictedLocation = calculatePredictedLocation(location)
                    intersectionIsNearing = if(checkIntersections(predictedLocation)) {
                        Log.d(TAG, "進行方向に交差点があります")
                        true
                    } else {
                        Log.d(TAG, "交差点を探しています...")
                        false
                    }
                    // 現在位置が交差点内なら画像処理をやめる
                    notInIntersection = if(checkIntersectionsCore(LatLng(location.latitude, location.longitude))) {
                        Log.d(TAG, "交差点内にいます")
                        false
                    } else {
                        Log.d(TAG, "交差点内にいません")
                        true
                    }

                }
            }
        }

        OpenCVLoader.initDebug()

        overlaySurfaceView = OverlaySurfaceView(resultView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // CameraProvider をリクエストする
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        startCamera()

        Log.d(TAG, "speed01: $SPEED_01, soeed02: $SPEED_02")
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            val preview : Preview = Preview.Builder()
                .setTargetResolution(Size(imageProxyWidth, imageProxyHeight))
                .build()

            val cameraSelector : CameraSelector = CameraSelector.Builder()
                .requireLensFacing(CameraSelector.LENS_FACING_BACK)
                .build()

            preview.setSurfaceProvider(cameraView.createSurfaceProvider())

            // 画像解析(今回は物体検知)のユースケース
            val imageAnalyzer = ImageAnalysis.Builder()
                .setTargetRotation(cameraView.display.rotation)
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST) // 最新のcameraのプレビュー画像だけをを流す
                .setTargetResolution(Size(imageProxyWidth, imageProxyHeight))
                .build()
                // 推論処理へ移動 (ObjectDetector.kt参照)
                .also {
                    it.setAnalyzer(
                        cameraExecutor,

                        // 画像解析(Analyze.kt参照)
                        Analyze(
                            notificator,
                            yuvToRgbConverter,
                            interpreter,
                            labels,
                            overlaySurfaceView,
                            Size(resultView.width, resultView.height)
                        )

                    )
                }

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer)

            } catch(exc: Exception) {
                Log.e("CameraX", "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
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
        interpreter.close()
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


    // tfliteモデルを扱うためのラッパーを含んだinterpreter
    private val interpreter: Interpreter by lazy {
        Interpreter(loadModel())
    }

    // モデルの正解ラベルリスト
    private val labels: List<String> by lazy {
        loadLabels()
    }

    // tfliteモデルをassetsから読み込む
    private fun loadModel(fileName: String = MODEL_FILE_NAME): ByteBuffer {
        lateinit var modelBuffer: ByteBuffer
        var file: AssetFileDescriptor? = null
        try {
            file = assets.openFd(fileName)
            val inputStream = FileInputStream(file.fileDescriptor)
            val fileChannel = inputStream.channel
            modelBuffer = fileChannel.map(FileChannel.MapMode.READ_ONLY, file.startOffset, file.declaredLength)
        } catch (e: Exception) {
            Toast.makeText(this, "モデルファイル読み込みエラー", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            file?.close()
        }
        return modelBuffer
    }

    // モデルの正解ラベルデータをassetsから取得
    private fun loadLabels(fileName: String = LABEL_FILE_NAME): List<String> {
        var labels = listOf<String>()
        var inputStream: InputStream? = null
        try {
            inputStream = assets.open(fileName)
            val reader = BufferedReader(InputStreamReader(inputStream))
            labels = reader.readLines()
        } catch (e: Exception) {
            Toast.makeText(this, "モデルデータを読み込めませんでした", Toast.LENGTH_SHORT).show()
            finish()
        } finally {
            inputStream?.close()
        }
        return labels
    }

    // カメラのYUV画像をRGBに変換するコンバータ
    private val yuvToRgbConverter: YuvToRgbConverter by lazy {
        YuvToRgbConverter(this)
    }


    companion object {
        const val TAG = "Main"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

        // 信号機のある交差点付近かどうか
        var intersectionIsNearing = false
        var notInIntersection = true
        // 車速
        var speed = 0.0

        // 車速の危険度しきい値
        var SPEED_01 = 45.0   // パターン1(音あり通知)
        var SPEED_02 = 10.0    // パターン2(信号機検知開始, 通常の通知)

        // モデル名とラベル名
        private const val MODEL_FILE_NAME = "ssd_mobilenet_v1.tflite"
        private const val LABEL_FILE_NAME = "coco_dataset_labels.txt"

        // 取得画像解像度
        // システムによって変更されるため、ObjectDetector.kt内で取得画像から再取得
        private const val imageProxyWidth = 1920
        private const val imageProxyHeight = 1080
    }
}