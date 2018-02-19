package com.beproject.group1.vta.helpers

/**
 * Created by Nimesh on 19-02-2018.
 */
import java.text.DecimalFormat

object ETA {
    internal fun distance(srcLat: Double, srcLong: Double, destLat: Double, destLong: Double): Double {
        val R = 6371000
        val diffLat = Math.toRadians(destLat - srcLat)
        val diffLong = Math.toRadians(destLong - srcLong)
        val a = Math.pow(Math.sin(diffLat / 2), 2.0) + Math.cos(Math.toRadians(srcLat)) * Math.cos(Math.toRadians(destLat)) * Math.pow(Math.sin(diffLong / 2), 2.0)
        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }

    internal fun speed(mode: Int): Double {
        when (mode) {
            0 -> return 13.8889
            1 -> return 8.33333
            2 -> return 2.77778
            3 -> return 1.38889
        }
        return 0.0
    }

    internal fun time(a: Double, b: Double, c: Double, d: Double, m1: Int, m2: Int): Double {
        val df = DecimalFormat(".##")
        val dis = distance(a, b, c, d)
        val s1 = speed(m1)
        val s2 = speed(m2)
        val t1 = dis / 2 / s1
        val t2 = dis / 2 / s2
        return df.format(t1 + t2).toDouble()

    }
}