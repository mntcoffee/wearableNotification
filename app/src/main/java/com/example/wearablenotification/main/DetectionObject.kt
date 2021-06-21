package com.example.wearablenotification.main

import android.graphics.RectF

/**
 * 検出結果を入れるクラス
 */
data class DetectionObject(
    val score: Float,
    var label: String,
    var boundingBox: RectF
)