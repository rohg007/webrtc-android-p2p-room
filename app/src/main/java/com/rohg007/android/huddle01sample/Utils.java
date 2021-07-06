package com.rohg007.android.huddle01sample;

import java.util.Random;

class Utils {
    static String getRandomString(){
        // lower limit for LowerCase Letters
        int lowerLimit = 97;

        // lower limit for LowerCase Letters
        int upperLimit = 122;

        Random random = new Random();

        // Create a StringBuffer to store the result
        StringBuffer r = new StringBuffer(6);

        for (int i = 0; i < 6; i++) {

            // take a random value between 97 and 122
            int nextRandomChar = lowerLimit
                    + (int)(random.nextFloat()
                    * (upperLimit - lowerLimit + 1));

            // append a character at the end of bs
            r.append((char)nextRandomChar);
        }

        // return the resultant string
        return r.toString();
    }
}
