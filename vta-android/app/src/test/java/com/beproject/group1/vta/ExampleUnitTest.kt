package com.beproject.group1.vta

import com.beproject.group1.vta.helpers.ETA
import org.junit.Assert.assertEquals
import org.junit.Test
import java.text.DecimalFormat
import java.util.*

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ETATest {

    //sample source and destination
    internal var srcLat = 19.0454606
    internal var srcLong = 72.8875286
    internal var destLat = 19.0997161
    internal var destLong = 72.9142363

    //their distance
    internal var dist = 6653.87
    internal var delta = 0.1    //precision loss

    internal var df = DecimalFormat(".##")

    @Test
    @Throws(Exception::class)
    fun distance() {

        val expected = dist                         //hard coded values
        val output = java.lang.Double.parseDouble(df.format(ETA.distance(srcLat, srcLong, destLat, destLong)));

        assertEquals(expected, output, delta)       //compare expected and actual
    }

    @Test
    @Throws(Exception::class)
    fun speed() {
        val r = Random()
        val i1 = r.nextInt(4 - 0) + 0

        val expected = doubleArrayOf(8.3333, 5.5556, 2.7778, 1.3889)        //hard coded values
        val output = ETA.speed(i1)

        assertEquals(expected[i1], output, delta)     //compare expected and actual
    }

    @Test
    @Throws(Exception::class)
    fun time() {
        val r = Random()
        val i1 = r.nextInt(4 - 0) + 0
        val i2 = r.nextInt(4 - 0) + 0
        val s1 = ETA.speed(i1)
        val s2 = ETA.speed(i2)
        val t1 = dist / 2 / s1
        val t2 = dist / 2 / s2

        val expected = java.lang.Double.parseDouble(df.format(t1 + t2))         //hard coded values
        val output = ETA.time(srcLat, srcLong, destLat, destLong, i1, i2)

        assertEquals(expected, output, delta)       //compare expected and actual
    }
}
