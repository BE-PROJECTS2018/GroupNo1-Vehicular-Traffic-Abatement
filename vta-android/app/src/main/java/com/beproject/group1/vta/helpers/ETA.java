package com.beproject.group1.vta.helpers;

import java.text.DecimalFormat;

/**
 * Created by Nimesh on 19-02-2018.
 */

public class ETA {
    public static double distance(double srcLat, double srcLong, double destLat, double destLong)
    {
        int R = 6371000;
        double diffLat = Math.toRadians(destLat - srcLat);
        double diffLong = Math.toRadians(destLong - srcLong);
        double a = Math.pow(Math.sin(diffLat / 2), 2) + Math.cos(Math.toRadians(srcLat)) * Math.cos(Math.toRadians(destLat)) * Math.pow(Math.sin(diffLong / 2), 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }
    public static double speed(int mode)
    {
        switch(mode)
        {
            case 0:
                return 8.3333; //30kmph
            case 1:
                return 5.5556; //20kmph
            case 2:
                return 2.7778; //10kmph
            case 3:
                return 1.3889; //5kmph
        }
        return 0.0;
    }
    static double time(double a,double b,double c,double d,int m1,int m2)
    {
        DecimalFormat df = new DecimalFormat(".##");
        double dis = distance(a, b, c, d);
        double s1 = speed(m1);
        double s2 = speed(m2);
        double t1 = (dis / 2) / s1;
        double t2 = (dis / 2) / s2;
        return Double.parseDouble(df.format(t1 + t2));

    }
}
