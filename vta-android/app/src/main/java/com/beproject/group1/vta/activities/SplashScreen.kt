package com.beproject.group1.vta.activities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.beproject.group1.vta.R
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.widget.Toast
import com.android.volley.Response
import com.beproject.group1.vta.VTAApplication
import com.beproject.group1.vta.helpers.APIController
import com.beproject.group1.vta.helpers.VolleyService
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.*
import java.io.FileOutputStream


class SplashScreen : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH: Long = 500
    private val model_name: String = "nn_v2"
    private lateinit var service: VolleyService
    private lateinit var apiController: APIController
    private lateinit var sp: SharedPreferences
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        sp = getSharedPreferences(VTAApplication.PREF_FILE, Context.MODE_PRIVATE)
        service = VolleyService()
        apiController = APIController(service)
        if(sp.getString("email", "null") == "null")
            Handler().postDelayed({
                val mainIntent = Intent(this@SplashScreen, LoginActivity::class.java)
                this@SplashScreen.startActivity(mainIntent)
                this@SplashScreen.finish()
            }, SPLASH_DISPLAY_LENGTH)
        else {
            val created = sp.getString("created", null)
            val ttl = Integer.parseInt(sp.getString("ttl", "0"))
            val cdate: Date
            val now = Date()
            if(created != null && ttl > 0) {
                val tz = TimeZone.getTimeZone("UTC")
                val cal = Calendar.getInstance(tz)
                val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
                sdf.calendar = cal
                cal.time = sdf.parse(created)
                cal.add(Calendar.SECOND, ttl)
                cdate = cal.time
                if(now.after(cdate)) {
                    //token expired
                    val params = JSONObject()
                    params.put("email", sp.getString("email", null))
                    params.put("password", sp.getString("password", null))
                    apiController.login(params) {response ->
                        if(response == null) {
                            val intent = Intent(this@SplashScreen, MapsActivity::class.java)
                            this@SplashScreen.startActivity(intent)
                            this@SplashScreen.finish()
                        } else {
                            val spe = sp.edit()
                            for (key in response.keys()) {
                                Log.d("RESP", key)
                                spe.putString(key, response.getString(key))
                            }
                            spe.apply()
                            maybeSync()
                        }
                    }
                } else {
                    maybeSync()
                }
            } else {
                val intent = Intent(this@SplashScreen, LoginActivity::class.java)
                this@SplashScreen.startActivity(intent)
                this@SplashScreen.finish()
            }
        }
    }

    private fun maybeSync() {
        val accessToken = sp.getString("id", null)
        val fmtime = sp.getString("fmtime", null)
        val intent = Intent(this@SplashScreen, MapsActivity::class.java)
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = tz
        apiController.getFileInfo("freeze_$model_name.pb", accessToken) { response ->
            if(response == null) {
                Toast.makeText(applicationContext, getString(R.string.offline_alert), Toast.LENGTH_SHORT).show()
                this@SplashScreen.startActivity(intent)
                this@SplashScreen.finish()
            } else {
                val newfmtime = response.getString("mtime")
                if(fmtime == null) {
                    val spe = sp.edit()
                    spe.putString("fmtime", newfmtime)
                    spe.apply()
                    syncFilesAndLaunchApp(intent, accessToken)
                } else {
                    val date1 = sdf.parse(fmtime)
                    val date2 = sdf.parse(newfmtime)
                    if (date1.before(date2)) {
                        val spe = sp.edit()
                        spe.putString("fmtime", newfmtime)
                        spe.apply()
                        syncFilesAndLaunchApp(intent, accessToken)
                    } else {
                        this@SplashScreen.startActivity(intent)
                        this@SplashScreen.finish()
                    }
                }
            }
        }
    }

    private fun syncFilesAndLaunchApp(intent: Intent, accessToken: String) {
        apiController.downloadFile("freeze_$model_name.pb", accessToken, Response.Listener<ByteArray> { response ->
            try {
                if (response != null) {
                    val outputStream: FileOutputStream
                    val name = "freeze_$model_name.pb"
                    outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                    outputStream.write(response)
                    outputStream.close()
                    Toast.makeText(this, "PB Download complete.", Toast.LENGTH_LONG).show()
                    apiController.downloadFile("normalize_$model_name.csv", accessToken, Response.Listener<ByteArray> { response ->
                        try {
                            if (response != null) {

                                val outputStream: FileOutputStream
                                val name = "normalize_$model_name.csv"
                                outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                                outputStream.write(response)
                                outputStream.close()
                                Toast.makeText(this, "CSV Download complete.", Toast.LENGTH_LONG).show()
                                this@SplashScreen.startActivity(intent)
                                this@SplashScreen.finish()
                            }
                        } catch (e: Exception) {
                            Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD CSV FILE")
                            e.printStackTrace()
                        }
                    }, Response.ErrorListener { error ->
                        error.printStackTrace()
                    })
                }
            } catch (e: Exception) {
                Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD PB FILE")
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            error.printStackTrace()
        })
    }
}
