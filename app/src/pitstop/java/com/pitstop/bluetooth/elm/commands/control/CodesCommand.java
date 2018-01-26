package com.pitstop.bluetooth.elm.commands.control;

import android.util.Log;

import com.pitstop.bluetooth.elm.commands.ObdCommand;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Karol Zdebel on 1/22/2018.
 */

public abstract class CodesCommand extends ObdCommand {

    private final String TAG = getClass().getSimpleName();


    /** Constant <code>dtcLetters={'P', 'C', 'B', 'U'}</code> */
    protected final static char[] dtcLetters = {'P', 'C', 'B', 'U'};
    /** Constant <code>hexArray="0123456789ABCDEF".toCharArray()</code> */
    protected final static char[] hexArray = "0123456789ABCDEF".toCharArray();

    protected List<Integer> codeCount;
    protected ObdProtocols obdProtocols;
    protected List<String> codes;

    /**
     * <p>Constructor for PendingTroubleCodesCommand.</p>
     */
    public CodesCommand(String command, ObdProtocols protocol, boolean hasHeaders) throws IllegalArgumentException{
        super(command,hasHeaders,4);
        codeCount = new ArrayList<>();
        obdProtocols = protocol;
        codes = new ArrayList<>();
        if (protocol == null) throw new IllegalArgumentException();
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand} object.
     */
    public CodesCommand(CodesCommand other) {
        super(other);
        codeCount = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    protected void fillBuffer() {
    }

    /** {@inheritDoc} */
    @Override
    protected void performCalculations() {

        Log.d(TAG,"performCalculations() obdProtocol: "+obdProtocols);
        /*
        * Store raw header, data and request code variables
        *
         */
        boolean ISO_15765 = obdProtocols == ObdProtocols.ISO_15765_4_CAN
                || obdProtocols == ObdProtocols.ISO_15765_4_CAN_B
                || obdProtocols == ObdProtocols.ISO_15765_4_CAN_C
                || obdProtocols == ObdProtocols.ISO_15765_4_CAN_D;
        String workingData = rawData.trim();
        if (ISO_15765 && workingData.contains(":")) {//CAN(ISO-15765) protocol two and more frames.
            parseISO15765_CAN_OTHER(workingData);  //This is used when the number of dtcs is high
        }else if (ISO_15765) {//CAN(ISO-15765) protocol one frame.
            try{
                parseISO15765_CAN_ONE(workingData); //This is used when the number of dtcs is low
            }catch(Exception e){
                e.printStackTrace();
                parseOtherProtocol(workingData);
            }
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            parseOtherProtocol(workingData);
        }

        System.out.printf(TAG+": After parsing Pending Trouble Codes, data: %s, header: %s" +
                ", request code: %s",data.toString(),headers.toString(),requestCode.toString());

        for (String d: data){
            String dtc = "";
            byte b1 = hexStringToByteArray(d.charAt(0));
            int ch1 = ((b1 & 0xC0) >> 6);
            int ch2 = ((b1 & 0x30) >> 4);
            dtc += dtcLetters[ch1];
            dtc += hexArray[ch2];
            dtc += d.substring(1,4);
            if (dtc.equals("P0000") || dtc.length() < 5) {
                return;
            }
            codes.add(dtc);
        }
    }

    private boolean parseISO15765_CAN_ONE(String rawData){
        Log.d(TAG,"praseISO15765_CAN_ONE() rawData: "+rawData);
        int index = 0;
        try{
            while (index < rawData.length()){
                if (hasHeaders()){
                    headers.add(rawData.substring(index,index+getHeaderLen()));
                    index += getHeaderLen();
                }
                requestCode.add(rawData.substring(index,index+2));
                index +=2;
                int dtcCount = Integer.parseInt(rawData.substring(index,index+2),16);
                codeCount.add(dtcCount);
                index+=2;
                System.out.println(TAG+": dtcCount: "+dtcCount);
                for (int j=0;j<dtcCount;j++){
                    data.add(rawData.substring(index,index+4));
                    index+=4;
                }
            }
        }catch(IndexOutOfBoundsException e){
            e.printStackTrace();
            return false;
        }
        return true;
    }

    private void parseISO15765_CAN_OTHER(String rawData){
        String workingData = rawData.replace(":", "");  //xxx47yy{codes}
        Log.d(TAG,"praseISO15765_CAN_OTHER() workingData: "+workingData);
        System.out.println("workingData: "+workingData);
        //Start at the 9th character, because the first 8 are the header
        for (int begin = 8; begin < workingData.length(); begin += 4) {
            data.add(workingData.substring(begin,begin+4));
        }
    }

    private void parseOtherProtocol(String rawData){
        Log.d(TAG,"praseOtherProtocol() rawData: "+rawData);
        String workingData = rawData.replaceAll("^47|^43|[\r\n]47|[\r\n]43|[\r\n]", "");

        for (int begin = 0; begin < workingData.length(); begin += 4) {
            data.add(workingData.substring(begin,begin+4));
        }

    }

    private byte hexStringToByteArray(char s) {
        return (byte) ((Character.digit(s, 16) << 4));
    }

    /**
     * <p>formatResult.</p>
     *
     * @return the formatted result of this command in string representation.
     * @deprecated use #getCalculatedResult instead
     */
    public String formatResult() {
        return codes.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getCalculatedResult() {
        return String.valueOf(codes);
    }


    /** {@inheritDoc} */
    @Override
    protected void readRawData(InputStream in) throws IOException {
        byte b;
        StringBuilder res = new StringBuilder();

        // read until '>' arrives OR end of stream reached (and skip ' ')
        char c;
        while (true) {
            b = (byte) in.read();
            if (b == -1) // -1 if the end of the stream is reached
            {
                break;
            }
            c = (char) b;
            if (c == '>') // read until '>' arrives
            {
                break;
            }
            if (c != ' ') // skip ' '
            {
                res.append(c);
            }
        }
        rawData = removeUnwantedPatterns(res.toString().trim());
        Log.d(TAG,"readRawData(): rawData: "+rawData+", length: "+rawData.length()+", headerLen: "
                +getHeaderLen());
    }

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return codes.toString();
    }

    public List<String> getCodes(){
        return codes;
    }

}
