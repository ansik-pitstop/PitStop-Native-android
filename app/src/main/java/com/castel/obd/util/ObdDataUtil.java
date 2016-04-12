package com.castel.obd.util;

import java.util.HashMap;

/**
 * Created by Paul Soladoye on 12/04/2016.
 */
public class ObdDataUtil {
    public static String parseDTCs(String hex) {
        int start = 1;
        char head = hex.charAt(0);
        HashMap<Character, String> map = new HashMap<Character, String>();
        map.put('0',"P0");
        map.put('1',"P1");
        map.put('2',"P2");
        map.put('3',"P3");

        map.put('4',"C0");
        map.put('5',"C1");
        map.put('6',"C2");
        map.put('7',"C3");

        map.put('8',"B0");
        map.put('9',"B1");
        map.put('A',"B2");
        map.put('B',"B3");

        map.put('C',"U0");
        map.put('D',"U1");
        map.put('E',"U2");
        map.put('F',"U3");

        return map.get(head)+hex.substring(start);
    }
}
