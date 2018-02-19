package com.example.nimesh.testeta;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }
    public double Distance(double srcLat, double srcLong, double destLat, double destLong)
    {
        LatLng src = new LatLng(srcLat, srcLong);
        LatLng dest = new LatLng(destLat, destLong);
        double distance = SphericalUtil.computeDistanceBetween(src, dest);
        return formatNumber(distance);
    }
    private String formatNumber(double distance) {
        String unit = "m";
        if (distance < 1) {
            distance *= 1000;
            unit = "mm";
        } else if (distance > 1000) {
            distance /= 1000;
            unit = "km";
        }

        return String.format("%4.3f%s", distance, unit);
    }
}
