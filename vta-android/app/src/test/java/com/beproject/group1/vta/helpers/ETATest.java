package com.beproject.group1.vta.helpers;

import org.junit.Test;

import java.text.DecimalFormat;
import java.util.Random;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Nimesh on 13-03-2018.
 */
public class ETATest {
    double srcLat = 19.0454606;
    double srcLong = 72.8875286;
    double destLat = 19.0997161;
    double destLong = 72.9142363;
    double dist = 6653.87;
    double delta = 0.1;

    DecimalFormat df = new DecimalFormat(".##");
    ETA eta = new ETA();

    @Test
    public void distance() throws Exception {
        double output;
        double expected = dist;

        output = Double.parseDouble(df.format(eta.distance(srcLat, srcLong, destLat, destLong)));
        assertEquals(expected, output, delta);
    }

    @Test
    public void speed() throws Exception {
        double expected[] = {8.3333, 5.5556, 2.7778, 1.3889};
        Random r = new Random();
        int i1 = r.nextInt(4 - 0) + 0;

        double output = eta.speed(i1);
        assertEquals(expected[i1], output);
    }

    @Test
    public void time() throws Exception {
        double output;
        Random r = new Random();
        int i1 = r.nextInt(4 - 0) + 0;
        int i2 = r.nextInt(4 - 0) + 0;
        double s1 = eta.speed(i1);
        double s2 = eta.speed(i2);
        double t1 = (dist / 2) / s1;
        double t2 = (dist / 2) / s2;
        double expected = Double.parseDouble(df.format(t1 + t2));
        output = eta.time(srcLat, srcLong, destLat, destLong, i1, i2);
        assertEquals(expected, output, delta);
    }

}