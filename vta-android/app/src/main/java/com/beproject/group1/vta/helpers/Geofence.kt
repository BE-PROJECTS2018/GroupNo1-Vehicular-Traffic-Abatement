package com.beproject.group1.vta.helpers

import android.util.Log

/**
 * Created by pavan on 19/1/18.
 */
class Geofence {
    companion object {
        // All geo region polygons being served should be initialized as polygons array here
        val polygons = arrayOf(
                Polygon(
                        x=doubleArrayOf(19.039645023697126, 19.039645023697126, 19.06398378548459, 19.06398378548459),
                        y=doubleArrayOf(72.88591886230472, 72.92025113769535, 72.92025113769535 ,72.88591886230472)
                ),
                Polygon(
                        x=doubleArrayOf(19.06398378548459, 19.06398378548459, 19.087125696060983, 19.087125696060983),
                        y=doubleArrayOf(72.88591886230472, 72.92025113769535, 72.92025113769535 ,72.88591886230472)
                )
        )

        @JvmStatic
        fun containsCoordinates(x1: Double, y1: Double): Boolean {
            var isIn = false
            var isStart: Boolean
            var sign = false
            for (polygon in polygons) {
                isStart = true
                val size = polygon.x.size
                for(i in 0 until size) {
                    val a = -(polygon.y[(i+1)%size] - polygon.y[i])
                    val b = polygon.x[(i+1)%size] - polygon.x[i]
                    val c = -(a*polygon.x[i] + b*polygon.y[i])
                    val d1 = a*x1 + b*y1 + c
                    if(isStart) {
                        sign = d1 > 0
                        isStart = false
                    }
                    isIn = sign == d1 > 0
                    if(!isIn) break
                }
                if(isIn) {
                    return true
                }
            }
            return false
        }
    }
}