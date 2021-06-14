package com.example.wearablenotification.main

import android.graphics.RectF

/**
 * 検出結果を入れるクラス
 */
data class DetectionObject(
    val score: Float,
    val label: String,
    var boundingBox: RectF
)