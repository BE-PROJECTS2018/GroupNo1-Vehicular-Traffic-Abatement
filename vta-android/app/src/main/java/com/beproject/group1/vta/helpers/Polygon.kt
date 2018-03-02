package com.beproject.group1.vta.helpers

import java.util.*

/**
 * Created by pavan on 19/1/18.
 */
data class Polygon(val x: DoubleArray, val y: DoubleArray) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Polygon

        if (!Arrays.equals(x, other.x)) return false
        if (!Arrays.equals(y, other.y)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = Arrays.hashCode(x)
        result = 31 * result + Arrays.hashCode(y)
        return result
    }

}