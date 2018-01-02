package com.beproject.group1.vta.activities

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.beproject.group1.vta.R
import android.support.v4.content.ContextCompat.startActivity
import android.content.Intent
import android.os.Handler


class SplashScreen : AppCompatActivity() {
    private val SPLASH_DISPLAY_LENGTH: Long = 2000
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        // TODO: check shared preferences to decide which activity to launch
        val activity = LoginActivity::class.java
        Handler().postDelayed({
            /* Create an Intent that will start the Main-Activity. */
            val mainIntent = Intent(this@SplashScreen, activity)
            this@SplashScreen.startActivity(mainIntent)
            this@SplashScreen.finish()
        }, SPLASH_DISPLAY_LENGTH)
    }
}
