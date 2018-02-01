package com.pitstop.bluetooth.elm.commands.other;

import com.pitstop.bluetooth.elm.commands.PersistentCommand;

/**
 * Created by ishan on 2017-12-20.
 */

public class CalibrationIDCommand extends PersistentCommand {

    private final String TAG = CalibrationIDCommand.class.getSimpleName();

    public CalibrationIDCommand(boolean hasHeaders){
        super("09 04",hasHeaders,16);
    }

    @Override
    public String getFormattedResult() {
        return rawData;
    }

    @Override
    public String getCalculatedResult() {
        return rawData;
    }

    @Override
    public String getName() {
        return CalibrationIDCommand.class.getSimpleName();
    }

    public String convertHexToString(String hex) {
        System.out.println(TAG+": Hex string: "+hex);
        StringBuilder sb = new StringBuilder();
        //49204c6f7665204a617661 split into two characters 49, 20, 4c...
        for (int i = 0; i < hex.length() - 1; i += 2) {

            //grab the hex in pairs
            String output = hex.substring(i, (i + 2));
            //convert hex to decimal
            int decimal = Integer.parseInt(output, 16);
            //convert the decimal to character
            sb.append((char) decimal);
        }
        return sb.toString();
    }

    @Override
    protected void fillBuffer() {
    }

    @Override
    protected void performCalculations() {
        final String result = getResult();
        String workingData;
        if (result.contains(":")) {//CAN(ISO-15765) protocol.
            workingData = result.replaceAll(".:", "");//9 is xxx490201, xxx is bytes of information to follow.
            int startOfVinIndex = workingData.indexOf("490401") + 6;
            workingData = workingData.substring(startOfVinIndex);
            //Matcher m = Pattern.compile("[^a-z0-9 ]", Pattern.CASE_INSENSITIVE).matcher(convertHexToString(workingData));
            //if(m.find()) workingData = result.replaceAll("0:49", "").replaceAll(".:", "");
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = result.replaceAll("49040.", "");
        }
        String calId = convertHexToString(workingData).replaceAll("[\u0000-\u001f]", "");
        data.add(calId);
    }
}
