package com.beproject.group1.vta;

import org.junit.Test;
import static junit.framework.Assert.assertEquals;
import ch.hsr.geohash.GeoHash;

/**
 * Created by Sumeet on 14-03-2018.
 */

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
