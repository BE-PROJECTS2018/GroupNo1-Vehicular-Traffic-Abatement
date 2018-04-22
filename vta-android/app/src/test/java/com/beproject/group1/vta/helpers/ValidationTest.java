package com.beproject.group1.vta.helpers;

import org.junit.Test;

import static junit.framework.Assert.assertEquals;

/**
 * Created by Nimesh on 25-03-2018.
 */
public class ValidationTest {
    Validation vt = new Validation();
    @Test
    public void isEmailValid() throws Exception {
        boolean expected = true;
        boolean actual = vt.isEmailValid("123@abc.in");
        assertEquals(expected, actual);
    }

    @Test
    public void isPasswordValid() throws Exception {

        boolean expected = false;
        boolean actual = vt.isPasswordValid("123");
        assertEquals(expected, actual);

    }

}