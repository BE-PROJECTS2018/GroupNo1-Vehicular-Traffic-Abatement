package com.beproject.group1.vta.helpers;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Nimesh on 25-03-2018.
 */

public class Validation {

    public static boolean isEmailValid(String email)
    {
        boolean isValid = false;

        String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
        CharSequence inputStr = email;

        Pattern pattern = Pattern.compile(expression, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(inputStr);
        if (matcher.matches()) {
            isValid = true;
        }
        return isValid;
    }

    public static boolean isPasswordValid(String password)
    {
        return password.length() > 4;
    }
}
