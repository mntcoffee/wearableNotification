package com.example.wearablenotification.main

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat.getSystemService
import com.example.wearablenotification.R
import com.example.wearablenotification.main.MainActivity.Companion.SPEED_01
import com.example.wearablenotification.main.MainActivity.Companion.SPEED_02
import com.example.wearablenotification.main.MainActivity.Companion.speed

class Notificator(
    private val context: Context
) {
    private lateinit var audioAttributes: AudioAttributes
    private var alert1 = 0
    private lateinit var notificationBuilderPattern1: NotificationCompat.Builder
    private lateinit var notificationBuilderPattern2: NotificationCompat.Builder
    private lateinit var channel1: NotificationChannel
    private lateinit var channel2: NotificationChannel
    private lateinit var soundPool: SoundPool

    fun initSoundPool() {
        audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_NOTIFICATION)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
            .build()

        soundPool = SoundPool.Builder()
            .setAudioAttributes(audioAttributes)
            .setMaxStreams(1)
            .build()

        alert1 = soundPool.load(context, R.raw.alert1, 1)

        soundPool.setOnLoadCompleteListener{ _: SoundPool, sampleId: Int, status: Int ->
            Log.d(MainActivity.TAG, "sampleId = $sampleId")
            Log.d(MainActivity.TAG, "status = $status")
        }
    }

    fun buildVibrator(manager: NotificationManager) {

        notificationBuilderPattern1 = NotificationCompat.Builder(context, CHANNEL_ID_1)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("パターン1")
            .setContentText("通知テスト")
            .setPriority(NotificationCompat.PRIORITY_MAX)   /// 通知の優先度(緊急)
            .setVibrate(longArrayOf(0, 200, 25, 200, 25, 1000))

        notificationBuilderPattern2 = NotificationCompat.Builder(context, CHANNEL_ID_2)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("パターン2")
            .setContentText("通知テスト")
            .setPriority(NotificationCompat.PRIORITY_HIGH)   // 通知の優先度(高)
            .setVibrate(longArrayOf(0, 100, 100, 100, 100, 100))  // 通知の振動設定

        // Android8（API26）以上の場合、チャネルに登録する
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){

            channel1 = NotificationChannel(
                CHANNEL_ID_1,
                "パターン1",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "通知テスト"
                this.vibrationPattern = longArrayOf(0, 200, 25, 200, 25, 1000)
            }

            channel2 = NotificationChannel(
                CHANNEL_ID_2,
                "パターン2",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                this.description = "通知テスト"
                this.vibrationPattern = longArrayOf(0, 200, 25, 200, 25, 1000)
            }

            // システムにチャネルを登録する
            manager.createNotificationChannel(channel1)
            manager.createNotificationChannel(channel2)

        }
    }

    fun alertDriver() {
        if(speed >= SPEED_01) {
            alert01()
        } else if(speed >= SPEED_02) {
            alert02()
        }
    }

    private fun alert01() {
        soundPool.play(alert1, 1.0f, 1.0f, 5, 2, 1.0f)

        // 通知の表示
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern1.build())
        }

        Log.d("Haha", "pattern1: 50km/h over")
    }

    private fun alert02() {
        // 通知の表示
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern2.build())
        }

        Log.d("Haha", "pattern2: from 10km/h to 50km/h")

    }

    companion object {
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID_1 = "channel_01"
        const val CHANNEL_ID_2 = "channel_02"
    }

}







