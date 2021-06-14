package com.example.wearablenotification.main

import android.graphics.RectF
import android.util.Size

class RoiCalculator() {

    // TODO : ROI自動化
    // ROIの計算
    fun calcRoi(imageProxySize: Size): RectF {

        return RectF(
            // (ImageProxy座標)
            imageProxySize.width / 2f - 320f ,
            imageProxySize.height / 2f - 240f,
            imageProxySize.width / 2f + 320f,
            imageProxySize.height / 2f + 240f
        )
    }

}