package com.beproject.group1.vta.activities

import android.content.Context
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.beproject.group1.vta.R
import android.content.Intent
import android.os.Handler
import com.beproject.group1.vta.VTAApplication


class SplashScreen : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH: Long = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        // TODO: check shared preferences to decide which activity to launch
        val sp = getSharedPreferences(VTAApplication.PREF_FILE, Context.MODE_PRIVATE)
        val activity = if(sp.getString("id", "null") == "null")
                            LoginActivity::class.java
                        else
                            MapsActivity::class.java
        Handler().postDelayed({
            val mainIntent = Intent(this@SplashScreen, activity)
            this@SplashScreen.startActivity(mainIntent)
            this@SplashScreen.finish()
        }, SPLASH_DISPLAY_LENGTH)
    }
}
