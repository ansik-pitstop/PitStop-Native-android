package com.pitstop.utils;

import android.util.Log;

import com.pitstop.bluetooth.dataPackages.ELM327PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD212PidPackage;
import com.pitstop.bluetooth.dataPackages.OBD215PidPackage;
import com.pitstop.bluetooth.dataPackages.PidPackage;

import java.text.DecimalFormat;
import java.util.HashMap;

/**
 * Created by David Liu on 3/12/2016.
 */
public class PIDParser {
    private static final String DTAG = "PIDParser";

    private static final String pidTypes[] = {"210C", "210D", "2106", "2107", "2110", "2124", "2105",
    "210E", "210F", "2142", "210A", "210B", "2104", "2111", "212C", "212D", "215C", "2103", "212E"};
    /**
     * Takes PID string and returns converted value for PID
     // 2102: DTC
     // 2103: Fuel System 1 Status
     // 214F: Fuel System 2 Status
     // 2104: Calculated Load Value (%)
     // 2105: Engine Coolant Temperature (C)
     // 2106: Short Term Fuel Trim - Bank1 (%)
     // 2107: Long Term Fuel Trim - Bank1 (%)
     // 2108: Short Term Fuel Trim - Bank2 (%)
     // 2109: Long Term Fuel Trim - Bank2 (%)
     // 210C: Engine RPM (/min)
     // 210D: Vehicle Speed Sensor (km/h)
     // 210E: Ignition Advance (degrees)
     // 210F: Intake Air Temperature (C)
     // 2110: Mass Air Flow Sensor (g/s)
     // 2111: Absolute Throttle Position (%)
     * @param input
     * @return Pair
     */
    public static Pair<String,String> ParsePID(String input, int[] values, String info){
        if (info.length()>2){

            // TODO Fix Parse error

            values = new int[]{
                    Integer.parseInt(info.substring(0,2),16),
                    Integer.parseInt(info.substring(2),16)
            };

        }else{
            values = new int[]{
                    Integer.parseInt(info,16)
            };
        }
        Pair<String,String> response = new Pair<String,String>(input,null);
        final DecimalFormat f = new DecimalFormat("0.#");
        switch (input) {
            case "2102":
                response.setValue(parseDTCs(info));
                break;
            case "2103":
            case "214F":
                // tmp should have value 1 or be modulo 2 to be valid
                if (values[0] % 2 == 0 || values[0] == 1) {
                    switch (values[0]) {
                        case 1:
                            response.setValue("OL1");
                            break;
                        case 2:
                            response.setValue("CL");
                            break;
                        case 4:
                            response.setValue("OL2");
                            break;
                        case 8:
                            response.setValue("OL Fault");
                            break;
                        case 16:
                            response.setValue("CL Fault");
                            break;
                        default:
                            // Invalid reading from device
                            response.setValue("N/A");
                    }
                }
                break;
            case "2104":
                response.setValue(f.format(values[0] * 100/255));
                break;
            case "2105":
                response.setValue(f.format(values[0]-40));
                break;

            case "2106":
            case "2107":
            case "2108":
            case "2109":
                response.setValue(f.format(values[0] * 199.2/255-100.0));
                break;

            case "210C":
                response.setValue(f.format((values[0] * 256 + values[1]) * 16384/65535));
                break;

            case "210D":
                response.setValue(""+values[0]);
                break;

            case "210E":
                response.setValue(f.format(values[0] * 127/255 - 64));
                break;

            case "210F":
                response.setValue(""+(values[0]- 40));
                break;

            case "2110":
                response.setValue(f.format((values[0] * 256 + values[1]) * 0.01));
                break;
            case "2111":
                response.setValue(f.format(values[0]* 100/255));
                break;
        }
        return response;
    }


    public static String parseDTCs(String hex){
        Log.i(DTAG,"Parsing DTCs - auto-connect service");
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

    public static class Pair<T, U> {
        public final T key;
        public U value;

        public Pair(T t, U u) {
            this.key= t;
            this.value= u;
        }

        public void setValue(U value){
            this.value=value;
        }
    }

    public static PidPackage pidPackageToDecimalValue(PidPackage original) {
        if (original == null){
            return null;
        }
        PidPackage decimalPidPackage = null;
        if (original instanceof ELM327PidPackage){
            decimalPidPackage = new ELM327PidPackage((ELM327PidPackage)original);
        }else if (original instanceof OBD215PidPackage){
            decimalPidPackage = new OBD215PidPackage((OBD215PidPackage)original);
        }else if (original instanceof OBD212PidPackage){
            decimalPidPackage = new OBD212PidPackage((OBD212PidPackage)original);
        }else{
            return null;
        }
        for (String s : pidTypes) {
            if (original.getPids().containsKey(s)) {
                try {
                    String decimalValue = Integer.toString(Integer.parseInt(original.getPids().get(s), 16));
                    decimalPidPackage.addPid(s, decimalValue);
                }
                catch (NumberFormatException e){
                    e.printStackTrace();
                }
            }
        }
        return decimalPidPackage;
    }

    public static String getPidName(String rawPidKey){
        switch(rawPidKey){
            case "2102": return "DTC";
            case "2103": return "Fuel System 1 Status";
            case "214F": return "Fuel System 2 Status";
            case "2100": return "Mass Airflow Sensor";
            case "2104": return "Calculated Load Value";
            case "2105": return "Engine Coolant Temperature (C)";
            case "2106": return "Short Term Fuel Trim - Bank1";
            case "2107": return "Long Term Fuel Trim - Bank1";
            case "2108": return "Short Term Fuel Trim - Bank2 (%)";
            case "2109": return "Long Term Fuel Trim - Bank2 (%)";
            case "210C": return "Engine RPM (/min)";
            case "210D": return " Vehicle Speed Sensor (km/h)";
            case "210E": return "Ignition Advance (degrees)";
            case "210F": return "Intake Air Temperature (C)";
            case "2110": return "Mass Air Flow Sensor (g/s)";
            case "2111": return "Absolute Throttle Position (%)";
            case "2142": return "Battery Voltage";
            case "2131": return "Distance Traveled Since Codes Cleared";
            case "213C": return "Catalyst Temperature: Bank 1, Sensor 1";
            case "214C": return "Commanded throttle actuator";
            case "2121": return "Distance traveled with malfunction indicator lamp (MIL) on";
            case "2119": return "Oxygen Sensor 6, A: Voltage, B: Short term fuel trim";
            case "2146": return "Ambient air temperature";
            case "2145": return "Relative throttle position";
            case "2118": return "Oxygen Sensor 5, A: Voltage, B: Short term fuel trim";
            case "212F": return "Fuel Tank Level Input";
            case "2144": return "Fuel?Air commanded equivalence ratio";
            case "2115": return "Oxygen Sensor 2, A: Voltage, B: Short term fuel trim";
            case "2143": return "Absolute load value";
            case "2150": return "Maximum value for air flow rate from mass air flow sensor";
            case "2133": return "Absolute Barometric Pressure";
            case "211C": return "OBD standards this vehicle conforms to";
            case "212E": return "Commanded evaporative purge";
            case "2114": return "Oxygen Sensor 1, A: Voltage, B: Short term fuel trim";
            case "210A": return "Fuel pressure (gauge pressure)";
            case "2101": return "Monitor status since DTCs cleared. (Includes malfunction indicator lamp (MIL) status and number of DTCs.)";
            case "213D": return "Catalyst Temperature: Bank 2, Sensor 1";
            case "2149": return "Accelerator pedal position D";
            case "211F": return "Run time since engine start";
            case "2147": return "Absolute throttle position B";
            case "2132": return "Evap. System Vapor Pressure";
            case "2151": return "Fuel Type";
            case "210B": return "Intake manifold absolute pressure";
            case "2113": return "Oxygen sensors present (in 2 banks)";
            default: return "Unknown";
        }
    }
}
