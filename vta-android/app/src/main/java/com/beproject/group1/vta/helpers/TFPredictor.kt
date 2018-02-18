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
    private val tf: TensorFlowInferenceInterface
    private val normalizeCoeffs = ArrayList<ArrayList<Float>>()
    init {
        tf = TensorFlowInferenceInterface(act.assets, act.filesDir.absolutePath+"/freeze.pb")
        val fis = act.openFileInput("normalize.csv")
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
                normalize(latitude, normalizeCoeffs.get(0).get(LAT_INDEX), normalizeCoeffs.get(1).get(LAT_INDEX)),
                normalize(longitude, normalizeCoeffs.get(0).get(LNG_INDEX), normalizeCoeffs.get(1).get(LNG_INDEX)),
                normalize(weekday.toFloat(), normalizeCoeffs.get(0).get(WEEK_INDEX), normalizeCoeffs.get(1).get(WEEK_INDEX)),
                normalize(hour.toFloat(), normalizeCoeffs.get(0).get(HOUR_INDEX), normalizeCoeffs.get(1).get(HOUR_INDEX)),
                normalize(minutes.toFloat(), normalizeCoeffs.get(0).get(MIN_INDEX), normalizeCoeffs.get(1).get(MIN_INDEX))
        )
        tf.feed("Placeholder", input, 1, 5)
        val outputNode = "dnn/head/predictions/ExpandDims"
        val outputNodes = arrayOf(outputNode)
        tf.run(outputNodes)
        val outputs = LongArray(1)
        tf.fetch(outputNode, outputs)
        Log.d("TF", Arrays.toString(outputs))
        return outputs[0]
    }

    private fun normalize(value: Float, min: Float, max: Float): Float {
        return (value - min)/(max - min)
    }
}