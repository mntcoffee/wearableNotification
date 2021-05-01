package com.example.wearablenotification.main.Intersection

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val DISTANCE = 0.000700   // 予測地点までの距離(緯度軽度ベース)
const val NUMBER_OF_INTERSECTIONS = 1

fun checkIntersections(location: Location): Boolean {
    val predictedLocation = calculatePredictedLocation(location)

    // 交差点検出のフラグ
    var flag = false
    for(i in 0 until NUMBER_OF_INTERSECTIONS) {
        if(PolyUtil.containsLocation(predictedLocation, intersection[i], true)) {
            flag = true
        }
    }
    return flag
}

private fun calculatePredictedLocation(location: Location): LatLng {
    val degree = location.bearing * 2 * PI / 360
    val predictedLatitude = location.latitude + DISTANCE * cos(degree)
    val predictedLongitude = location.longitude + DISTANCE * sin(degree)

    return LatLng(predictedLatitude, predictedLongitude)
}