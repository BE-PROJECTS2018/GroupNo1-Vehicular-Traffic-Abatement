package com.beproject.group1.vta.helpers

import android.app.Activity
import android.util.Log
import ch.hsr.geohash.GeoHash
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

/**
 * Created by pavan on 8/3/18.
 */
class SklearnPredictor internal constructor(private val act: Activity) {
    private val ranges: IntArray = intArrayOf(0,3,6,9,12,15,18,21,23)
    private var cachedModel:String? = null
    private var extraTreesClassifier: ExtraTreesClassifier? = null
//    private val extraTreesClassifiers: ArrayList<ExtraTreesClassifier> = ArrayList()
    init {
//        for (i in 1 until ranges.size) {
//            val modelName = "${ExtraTreesClassifier.model_name}-${ranges[i-1]}${ranges[i]}.json"
//            extraTreesClassifiers.add(ExtraTreesClassifier("${act.filesDir.absolutePath}/$modelName"))
//        }
    }

    fun predict(latitude: Double, longitude: Double, weekday: Int, hour: Int, minutes: Int): Long {
        val geohash = GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, 7)
        val mins = findNearestMin(minutes)
        val sq_week = weekday*weekday
        val sq_hour = hour*hour
        val sq_mins = mins*mins
        var geohash_label:Double = (0).toDouble()
        var i:Int = 1
        for(r in ranges) {
            if(r >= hour) break
            i++
        }
        if(i==9) i--
        val model = "${ExtraTreesClassifier.model_name}-${ranges[i-1]}${ranges[i]}.json"
        val mapping = "${ExtraTreesClassifier.map_prefix}-${ranges[i-1]}${ranges[i]}-map.csv"
        Log.d("SKLEARN", "Using $model model")
        val fis = act.openFileInput(mapping)
        val isr = InputStreamReader(fis)
        val bufferedReader = BufferedReader(isr)
        var line = bufferedReader.readLine()
        var found = false
        while (line != null) {
            val coefs = line.split(",")
            if(coefs[0].equals(geohash)) {
                geohash_label = coefs[1].toDouble()
                found = true
                break
            }
            line = bufferedReader.readLine()
        }
        if(!found) {
            Log.d("SKLEARN", "Default prediction 0")
            return (0).toLong()
        }
        if(extraTreesClassifier == null) {
            extraTreesClassifier = ExtraTreesClassifier("${act.filesDir.absolutePath}/$model")
            cachedModel = String(model.toCharArray())
        } else if(cachedModel!! != model) {
            extraTreesClassifier = ExtraTreesClassifier("${act.filesDir.absolutePath}/$model")
        }
        val features = doubleArrayOf(geohash_label, weekday.toDouble(), hour.toDouble(), mins.toDouble(),
                sq_week.toDouble(), sq_hour.toDouble(), sq_mins.toDouble())
        val pred = extraTreesClassifier!!.predict(features).toLong()
        Log.d("SKLEARN", "$pred")
        return pred
    }

    private fun findNearestMin(min: Int): Int {
        val t:Int = min/15
        return t*15
    }
}