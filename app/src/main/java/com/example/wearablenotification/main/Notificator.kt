package com.example.wearablenotification.main

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.wearablenotification.R
import com.example.wearablenotification.main.MainActivity.Companion.alert1
import com.example.wearablenotification.main.MainActivity.Companion.audioAttributes
import com.example.wearablenotification.main.MainActivity.Companion.notificationBuilderPattern1
import com.example.wearablenotification.main.MainActivity.Companion.notificationBuilderPattern2
import com.example.wearablenotification.main.MainActivity.Companion.soundPool
import com.example.wearablenotification.main.MainActivity.Companion.speed

class Notificator {
    lateinit var audioAttributes: AudioAttributes
    var alert1 = 0
    lateinit var notificationBuilderPattern1: NotificationCompat.Builder
    lateinit var notificationBuilderPattern2: NotificationCompat.Builder
    lateinit var soundPool: SoundPool

    fun initSoundPool(context: Context) {
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

    fun buildVibrator(context: Context) {
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
    }

    fun alertDriver(context: Context, speed: Double) {
        if(speed >= 55.0) {
            alert01(context)
        } else if(speed >= 10.0) {
            alert02(context)
        }
    }

    private fun alert01(context: Context) {
        soundPool.play(alert1, 1.0f, 1.0f, 5, 2, 1.0f)

        // 通知の表示
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern1.build())
        }

        Log.d(MainActivity.TAG, "pattern1: 50km/h over")
    }

    private fun alert02(context: Context) {
        // 通知の表示
        with(NotificationManagerCompat.from(context)) {
            // notificationId is a unique int for each notification that you must define
            notify(NOTIFICATION_ID, notificationBuilderPattern2.build())
        }

        Log.d(MainActivity.TAG, "pattern2: from 10km/h to 50km/h")

    }

    companion object {
        const val NOTIFICATION_ID = 10
        const val CHANNEL_ID_1 = "channel_01"
        const val CHANNEL_ID_2 = "channel_02"
    }

}







