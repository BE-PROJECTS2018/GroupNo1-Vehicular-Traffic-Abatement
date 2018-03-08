package com.beproject.group1.vta.activities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.beproject.group1.vta.R
import android.content.Intent
import android.content.SharedPreferences
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import com.android.volley.Response
import com.beproject.group1.vta.VTAApplication
import com.beproject.group1.vta.helpers.APIController
import com.beproject.group1.vta.helpers.TFPredictor.Companion.model_name
import com.beproject.group1.vta.helpers.VolleyService
import com.beproject.group1.vta.helpers.WekaPredictor
import kotlinx.android.synthetic.main.activity_splash_screen.*
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.nio.file.Files.isDirectory
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream


class SplashScreen : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH: Long = 500
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
//                            maybeSync()
                            wekaSync()
                        }
                    }
                } else {
//                    maybeSync()
                    wekaSync()
                }
            } else {
                val intent = Intent(this@SplashScreen, LoginActivity::class.java)
                this@SplashScreen.startActivity(intent)
                this@SplashScreen.finish()
            }
        }
    }

    private fun wekaSync() {
        val accessToken = sp.getString("id", null)
        val wmtime = sp.getString("wmtime", null)
        val intent = Intent(this@SplashScreen, MapsActivity::class.java)
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = tz
        apiController.getFileInfo("${WekaPredictor.model_name}.zip", accessToken){response ->
            if(response == null) {
                Toast.makeText(applicationContext, getString(R.string.offline_alert), Toast.LENGTH_SHORT).show()
                this@SplashScreen.startActivity(intent)
                this@SplashScreen.finish()
            } else {
                val newwmtime = response.getString("mtime")
                if(wmtime == null) {
                    val spe = sp.edit()
                    spe.putString("wmtime", newwmtime)
                    spe.apply()
                    syncWekaAndLaunchApp(intent, accessToken)
                } else {
                    val date1 = sdf.parse(wmtime)
                    val date2 = sdf.parse(newwmtime)
                    if (date1.before(date2)) {
                        val spe = sp.edit()
                        spe.putString("wmtime", newwmtime)
                        spe.apply()
                        syncWekaAndLaunchApp(intent, accessToken)
                    } else {
                        this@SplashScreen.startActivity(intent)
                        this@SplashScreen.finish()
                    }
                }
            }
        }
    }

    private fun syncWekaAndLaunchApp(intent: Intent, accessToken: String) {
        splash_message.visibility = View.VISIBLE
        apiController.downloadFile("${WekaPredictor.model_name}.zip", accessToken, Response.Listener<ByteArray> {response ->
            try {
                if (response != null) {
                    val outputStream: FileOutputStream
                    val name = "${WekaPredictor.model_name}.zip"
                    outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                    outputStream.write(response)
                    outputStream.close()
                    Toast.makeText(this, "WEKA Download complete.", Toast.LENGTH_LONG).show()
                    val b = unpackZip("${filesDir.absolutePath}/", name)
                    Toast.makeText(this, "WEKA unzip: $b.", Toast.LENGTH_LONG).show()
                    splash_message.visibility = View.GONE
                    this@SplashScreen.startActivity(intent)
                    this@SplashScreen.finish()
                }
            } catch (e: Exception) {
                Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD WEKA FILE")
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            error.printStackTrace()
        })
    }

    private fun unpackZip(path: String, zipname: String): Boolean {
        val `is`: InputStream
        val zis: ZipInputStream
        try {
            var filename: String
            `is` = FileInputStream(path + zipname)
            zis = ZipInputStream(BufferedInputStream(`is`))
            var ze: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(1024)
            var count: Int
            while (ze != null) {
                // zapis do souboru
                filename = ze.getName()

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    val fmd = File(path + filename)
                    fmd.mkdirs()
                    continue
                }

                val fout = FileOutputStream(path + filename)

                // cteni zipu a zapis
                count = zis.read(buffer)
                while (count != -1) {
                    fout.write(buffer, 0, count)
                    count = zis.read(buffer)
                }

                fout.close()
                zis.closeEntry()
                ze = zis.nextEntry
            }

            zis.close()
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
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
