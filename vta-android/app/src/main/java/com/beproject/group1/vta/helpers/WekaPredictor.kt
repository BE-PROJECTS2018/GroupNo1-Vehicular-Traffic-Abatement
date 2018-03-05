package com.beproject.group1.vta.helpers

import android.app.Activity
import android.util.Log
import ch.hsr.geohash.GeoHash
import weka.classifiers.Classifier
import android.widget.Toast
import android.R.attr.label
import weka.core.*


/**
 * Created by pavan on 5/3/18.
 */
class WekaPredictor internal constructor(act: Activity) {
    private val classifiers: ArrayList<Classifier> = ArrayList()
    companion object {
        val model_name = "v1-1-geohash"
        val ranges = intArrayOf(3,6,9,12,15,18,21,23)
    }
    init {
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-03.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-36.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-69.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-912.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-1215.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-1518.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-1821.model") as Classifier)
        classifiers.add(SerializationHelper.read("${act.filesDir.absolutePath}/$model_name-2123.model") as Classifier)
    }

    fun predict(latitude: Double, longitude: Double, weekday: Double, hour: Double, minutes: Double): Long? {
        val geohash = GeoHash.geoHashStringWithCharacterPrecision(latitude, longitude, 12)
        val geohashAttr = Attribute("geohash", null as ArrayList<String>?)
        val weekdayAttr = Attribute("weekday")
        val hourAttr = Attribute("hour")
        val minutesAttr = Attribute("min")
        val trafficAttr = Attribute("traffic")
        val attrList: ArrayList<Attribute> = ArrayList()
        attrList.add(geohashAttr)
        attrList.add(weekdayAttr)
        attrList.add(hourAttr)
        attrList.add(minutesAttr)
        attrList.add(trafficAttr)
        val dataUnpredicted = Instances("TestInstances", attrList, 1)
        // last feature is target variable
        dataUnpredicted.setClassIndex(dataUnpredicted.numAttributes() - 1)
        val newInstance = object : DenseInstance(dataUnpredicted.numAttributes()) {
            init {
                setValue(geohashAttr, geohash)
                setValue(weekdayAttr, weekday)
                setValue(hourAttr, hour)
                setValue(minutesAttr, minutes)
            }
        }
        // reference to dataset
        newInstance.setDataset(dataUnpredicted)
        var index = 0
        for(i in 0..ranges.size) {
            if(hour.toInt() <= ranges[i]) {
                index = i
                break
            }
        }
        try {
            val result = classifiers[index].classifyInstance(newInstance)
            return result.toLong()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return null
    }
}