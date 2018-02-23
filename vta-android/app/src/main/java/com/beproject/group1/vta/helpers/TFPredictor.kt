package com.beproject.group1.vta.helpers

import org.tensorflow.contrib.android.TensorFlowInferenceInterface
import android.app.Activity
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.*


/**
 * Created by pavan on 18/2/18.
 */
class TFPredictor internal constructor(act: Activity) {
    private val LAT_INDEX = 0
    private val LNG_INDEX = 1
    private val WEEK_INDEX = 2
    private val HOUR_INDEX = 3
    private val MIN_INDEX = 4
    private val model_name = "nn_v2"
    private val group_factor: Int = 8
    private val N: Double = 19.06398378548459
    private val E: Double = 72.92025113769535
    private val S: Double = 19.039645023697126
    private val W: Double = 72.88591886230472
    private val tf: TensorFlowInferenceInterface
    private val normalizeCoeffs = ArrayList<ArrayList<Float>>()
    init {
        tf = TensorFlowInferenceInterface(act.assets, act.filesDir.absolutePath+"/freeze_$model_name.pb")
        val fis = act.openFileInput("normalize_$model_name.csv")
        val isr = InputStreamReader(fis)
        val bufferedReader = BufferedReader(isr)
        var line = bufferedReader.readLine()
        while (line != null) {
            val coefs = line.split(",")
            val clist = ArrayList<Float>()
            coefs.mapTo(clist) { it.toFloat() }
            normalizeCoeffs.add(clist)
            line = bufferedReader.readLine()
        }
    }
    fun predict(latitude: Float, longitude: Float, weekday: Int, hour: Int, minutes: Int): Long {
        val input = floatArrayOf(
                normalize(findGridLat(latitude).toFloat(), normalizeCoeffs[0][LAT_INDEX], normalizeCoeffs[1][LAT_INDEX]),
                normalize(findGridLng(longitude).toFloat(), normalizeCoeffs[0][LNG_INDEX], normalizeCoeffs[1][LNG_INDEX]),
                normalize(weekday.toFloat(), normalizeCoeffs[0][WEEK_INDEX], normalizeCoeffs[1][WEEK_INDEX]),
                normalize(hour.toFloat(), normalizeCoeffs[0][HOUR_INDEX], normalizeCoeffs[1][HOUR_INDEX]),
                normalize(minutes.toFloat(), normalizeCoeffs[0][MIN_INDEX], normalizeCoeffs[1][MIN_INDEX])
        )
        tf.feed("Placeholder", input, 1, 5)
        val outputNode = "dnn/logits/BiasAdd"
        val outputNodes = arrayOf(outputNode)
        tf.run(outputNodes)
        val outputs = FloatArray(3)
        tf.fetch(outputNode, outputs)
        Log.d("TF", Arrays.toString(outputs))
        val percentTraffic = 20*outputs[0] + 40*outputs[1] + 60*outputs[2]
        return when {
            percentTraffic < 20 -> 0L
            percentTraffic < 40 -> 1L
            percentTraffic < 60 -> 2L
            else -> 3L
        }
    }

    private fun findGridLat(latitude: Float): Int {
        val s: Double = 600/(S-N)
        val p: Double = (latitude-N)*s
        return (p/group_factor).toInt()
    }



    private fun findGridLng(longitude: Float): Int {
        val s: Double = 800/(E-W)
        val p: Double = (longitude-W)*s
        return (p/group_factor).toInt()
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        return (value - min)/(max - min)
    }
}