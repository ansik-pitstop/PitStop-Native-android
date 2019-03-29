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
            // RVD PIDS
            case "0": return "OBD Monitor status since DTCs cleared";
            case "2": return "Engine Speed";
            case "3": return "Vehicle Speed";
            case "4": return "Calculated Load";
            case "5": return "Coolant Temperature";
            case "6": return "Ignition Timing";
            case "7": return "Intake Air Temperature";
            case "8": return "Mass Air Flow";
            case "9": return "Absolute Throttle Position";
            case "11": return "Time Since Engine Start";
            case "12": return "Distance with MIL on";
            case "13": return "Fuel Level";
            case "14": return "Warm Ups Since DTC Cleared";
            case "15": return "Distance Since DTC Cleared";
            case "16": return "Barometric Pressure";
            case "17": return "Catalyst Temperature";
            case "18": return "Control Module Voltage";
            case "19": return "Absolute Load Value";
            case "20": return "Fuel Rail Pressure";
            case "21": return "Intake Manifold Absolute Pressure";
            case "23": return "Absolute Throttle Position B";
            case "24": return "Absolute Throttle Position C";
            case "25": return "Accelerator Pedal Position D";
            case "26": return "Accelerator Pedal Position E";
            case "27": return "Accelerator Pedal Position F";
            case "28": return "Commanded throttle actuator control.";
            case "29": return "State of Charge";
            case "30": return "Engine run time while MIL is activated";
            case "31": return "Engine run time since DTCs cleared";
            case "32": return "Engine Fuel Rate";
            case "33": return "Odometer";
            case "36": return "Oil Level";
            case "37": return "Brakes Front - remaining life";
            case "38": return "Brakes Rear - remaining life";
            case "39": return "Brake Fluid Level Status";
            case "40": return "Total time from last service";
            case "41": return "Distance From Last Service";
            case "42": return "Time To Next Service";
            case "43": return "Distance To Next Service";
            case "45": return "Urea operating range";
            case "46": return "Theoretical Service Interval";
            case "47": return "Engine Oil Life Remaining";
            case "48": return "Particle Filter State - remaining life";
            case "49": return "Diesel Additive - remaining life";
            case "50": return "Spark Plugs - remaining life";
            case "51": return "Coolant - remaining life";
            case "52": return "Micro Filter - remaining life";
            case "53": return "Brake Fluid - remaining life";
            case "54": return "Vehicle check - remaining";
            case "55": return "Kilometers rounded to nearest service/ Remaining distance to next service due internal";
            case "56": return "Exhaust Inspection service due date";
            case "63": return "Battery Voltage";
            case "74": return "Battery Capacity";
            case "75": return "Kilometre reading when battery is replaced";
            case "76": return "Alternator Current";
            case "77": return "Alternator Load";
            case "78": return "Vehicle Battery Current";
            case "79": return "DPF Differential Pressure";
            case "83": return "Particle filter measured soot mass";
            case "84": return "Steering Angle";
            case "85": return "Tyre Pressure Front Left";
            case "86": return "Tyre Pressure Front Right";
            case "87": return "Tyre Pressure Rear Left";
            case "88": return "Tyre Pressure Rear Right";
            case "93": return "Front LH tyre temp.";
            case "94": return "Front RH tyre temp.";
            case "95": return "Rear LH tyre temp.";
            case "96": return "Rear RH tyre temp.";
            case "97": return "Coolant level";
            case "98": return "Distance since last DPF Regeneration";
            case "99": return "Distance to regeneration";
            case "100": return "Brake Fluid - Due Date for change";
            case "101": return "Calculated Particulate Filter Soot Accumulation";
            case "102": return "Particle Filter Oil Ash Volume";
            case "103": return "Particle filter fuel consumption since last regeneration";
            case "104": return "Time elapsed since Regeneration";
            case "105": return "Brakes Front - distance until service";
            case "106": return "Brakes Rear - distance until service";
            case "107": return "Distance till next oil servicing";
            case "108": return "Particle Filter State - distance until service";
            case "109": return "Diesel Additive - distance until service";
            case "110": return "Spark Plugs - distance until service";
            case "111": return "Coolant - distance until service";
            case "112": return "Micro Filter - distance until service";
            case "113": return "Vehicle check - distance until service";
            case "114": return "Brakes Front - number of services done";
            case "115": return "Brakes Rear - number of services done";
            case "117": return "Particle Filter State - number of services done";
            case "118": return "Diesel Additive - number of services done";
            case "119": return "Spark Plugs - number of services done";
            case "120": return "Coolant - number of services done";
            case "121": return "Micro Filter - number of services done";
            case "122": return "Brake Fluid - number of services done";
            case "134": return "Catalyst Temperature Bank 1, Sensor 1";
            case "135": return "Catalyst Temperature Bank 2, Sensor 1";
            case "136": return "Catalyst Temperature Bank 1, Sensor 2";
            case "137": return "Catalyst Temperature Bank 2, Sensor 2";
            case "138": return "Equivalence Ratio (Lambda) (Bank 1, Sensor 1)";
            case "139": return "Bank 1 Sensor 2 (wide range O2S) Equivalence Ratio (lambda)";
            case "140": return "Bank 1 Sensor 3 (wide range O2S) Equivalence Ratio (lambda)";
            case "141": return "Bank 1 Sensor 4 (wide range O2S) Equivalence Ratio (lambda)";
            case "143": return "Bank 2 Sensor 2 (wide range O2S) Equivalence Ratio (lambda)";
            case "144": return "Bank 2 Sensor 3 (wide range O2S) Equivalence Ratio (lambda)";
            case "145": return "Bank 2 Sensor 4 (wide range O2S) Equivalence Ratio (lambda)";
            case "146": return "Bank 3 Sensor 1 (wide range O2S) Equivalence Ratio (lambda)";
            case "147": return "Bank 3 Sensor 2 (wide range O2S) Equivalence Ratio (lambda)";
            case "148": return "Bank 4 Sensor 1 (wide range O2S) Equivalence Ratio (lambda)";
            case "149": return "Bank 4 Sensor 2 (wide range O2S) Equivalence Ratio (lambda)";
            case "150": return "Voltage of oxygen sensor Bank 1 sensor 1 (broadband sensor)";
            case "151": return "Bank 1 Sensor 2 (wide range O2S) Oxygen Sensor Voltage";
            case "152": return "Bank 1 Sensor 3 (wide range O2S) Oxygen Sensor Voltage";
            case "153": return "Bank 1 Sensor 4 (wide range O2S) Oxygen Sensor Voltage";
            case "154": return "Voltage of oxygen sensor Bank 2 sensor 1 (broadband sensor) : Voltage";
            case "155": return "Bank 2 Sensor 2 (wide range O2S) Oxygen Sensor Voltage";
            case "156": return "Bank 2 Sensor 3 (wide range O2S) Oxygen Sensor Voltage";
            case "157": return "Bank 2 Sensor 4 (wide range O2S) Oxygen Sensor Voltage";
            case "158": return "Bank 3 Sensor 1 (wide range O2S) Oxygen Sensor Voltage";
            case "159": return "Bank 3 Sensor 2 (wide range O2S) Oxygen Sensor Voltage";
            case "160": return "Bank 4 Sensor 1 (wide range O2S) Oxygen Sensor Voltage";
            case "161": return "Bank 4 Sensor 2 (wide range O2S) Oxygen Sensor Voltage";
            case "162": return "Fuel/Air Commanded Equivalence Ratio";
            case "163": return "OBD Monitor status during drive cycle";
            case "164": return "Actual Engine Torque";
            case "165": return "Engine torque reference value";
            case "166": return "Engine Friction - Percent Torque";
            case "167": return "Mass Air Flow Sensor A";
            case "168": return "Mass Air Flow Sensor B";
            case "169": return "Oxygen Sensor (O2S) Heater Duty Cycle Bank 1 Sensor 1";
            case "170": return "Oxygen Sensor (O2S) Heater Duty Cycle Bank 1 Sensor 2";
            case "171": return "O2 Sensor Concentration Bank 1 Sensor 3";
            case "172": return "O2 Sensor Concentration Bank 1 Sensor 4";
            case "173": return "O2 Sensor Concentration Bank 2 Sensor 1";
            case "174": return "O2 Sensor Concentration Bank 2 Sensor 2";
            case "175": return "O2 Sensor Concentration Bank 2 Sensor 3";
            case "176": return "O2 Sensor Concentration Bank 2 Sensor 4";
            case "177": return "Cylinder Fuel Rate";
            case "178": return "Diesel Particulate Filter Bank 1 Delta Pressure";
            case "179": return "Diesel Particulate Filter Bank 1 Inlet Pressure";
            case "180": return "Diesel Particulate Filter Bank 1 Outlet Pressure";
            case "181": return "Diesel Particulate Filter Bank 2 Delta Pressure";
            case "182": return "Diesel Particulate Filter Bank 2 Inlet Pressure";
            case "183": return "Diesel Particulate Filter Bank 2 Outlet Pressure";
            case "184": return "Temperature before particle trap DPF";
            case "185": return "Particle filter outlet temp.sens.1 bank 1, raw value";
            case "186": return "DPF Bank 2 Inlet Temperature Sensor";
            case "187": return "DPF Bank 2 Outlet Temperature Sensor";
            case "188": return "Oil Temperature";
            case "189": return "Ambient Air Temperature";
            case "65026": return "GNSS latitude";
            case "65027": return "GNSS longitude";
            case "65028": return "GNSS altitude";
            case "65029": return "GNSS direction";
            case "65030": return "GNSS timestamp";
            case "65037": return "GNSS speed";
            case "65041": return "GNSS age";
            case "65043": return "Horizontal dilution of precision";
            case "65044": return "GPS satellites";
            case "65045": return "GLONASS satellites";
            case "65046": return "GNSS satellites count";
            case "65047": return "Horizontal position accuracy";
            case "65048": return "Vertical position accuracy";

            // Others devices pids
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
