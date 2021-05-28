package com.example.wearablenotification.main.intersection

import android.location.Location
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

const val DISTANCE_LONGITUDE = 0.00054832 // 予測地点までの距離(50mの経度)
const val DISTANCE_LATITUDE = 0.00045066   // 予測地点までの距離(50mの緯度)
private val NUMBER_OF_INTERSECTIONS = intersection.count()

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
    val predictedLatitude = location.latitude + DISTANCE_LATITUDE * cos(degree)
    val predictedLongitude = location.longitude + DISTANCE_LONGITUDE * sin(degree)

    return LatLng(predictedLatitude, predictedLongitude)
}