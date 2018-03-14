package com.beproject.group1.vta;

import android.support.test.runner.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;
import static junit.framework.Assert.assertEquals;
import ch.hsr.geohash.GeoHash;

/**
 * Created by Sumeet on 14-03-2018.
 */
@RunWith(AndroidJUnit4.class)
public class GeoHashTest {

    private Double lat = 19.032801;
    private Double lng = 72.896355;

    @Test
    public void testGeoHash() {
        String expectedGeoHash = "te7u6bc";
        String actualGeoHash = GeoHash.geoHashStringWithCharacterPrecision(lat, lng, 7);
        assertEquals(expectedGeoHash, actualGeoHash);
    }
}
