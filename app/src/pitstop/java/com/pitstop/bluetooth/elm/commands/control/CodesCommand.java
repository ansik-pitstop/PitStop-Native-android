package com.pitstop.bluetooth.elm.commands.control;

import com.pitstop.bluetooth.elm.commands.ObdCommand;
import com.pitstop.bluetooth.elm.enums.AvailableCommandNames;

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

    protected StringBuilder codes = null;
    protected List<Integer> codeCount;
    /**
     * <p>Constructor for PendingTroubleCodesCommand.</p>
     */
    public CodesCommand(String command,boolean hasHeaders) {
        super(command,hasHeaders,4);
        codes = new StringBuilder();
        codeCount = new ArrayList<>();
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand} object.
     */
    public CodesCommand(CodesCommand other) {
        super(other);
        codes = new StringBuilder();
        codeCount = new ArrayList<>();
    }

    /** {@inheritDoc} */
    @Override
    protected void fillBuffer() {
    }

    /** {@inheritDoc} */
    @Override
    protected void performCalculations() {
        final String result = getResult();
        String workingData;
        int startIndex = 0;//Header size.

        String canOneFrame = result.replaceAll("[\r\n]", "");
        int canOneFrameLength = canOneFrame.length();
        if (canOneFrameLength <= 16 && canOneFrameLength % 4 == 0) {//CAN(ISO-15765) protocol one frame.
            workingData = canOneFrame;//47yy{codes}
            startIndex = 4;//Header is 47yy, yy showing the number of data items.
        } else if (result.contains(":")) {//CAN(ISO-15765) protocol two and more frames.
            workingData = result.replaceAll("[\r\n].:", "");//xxx47yy{codes}
            startIndex = 7;//Header is xxx47yy, xxx is bytes of information to follow, yy showing the number of data items.
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = result.replaceAll("^47|[\r\n]47|[\r\n]", "");
        }
        for (int begin = startIndex; begin < workingData.length(); begin += 4) {
            String dtc = "";
            byte b1 = hexStringToByteArray(workingData.charAt(begin));
            int ch1 = ((b1 & 0xC0) >> 6);
            int ch2 = ((b1 & 0x30) >> 4);
            dtc += dtcLetters[ch1];
            dtc += hexArray[ch2];
            dtc += workingData.substring(begin+1, begin + 4);
            if (dtc.equals("P0000")) {
                return;
            }
            codes.append(dtc);
            codes.append('\n');
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

        rawData = res.toString().trim();
        System.out.println(TAG+": rawData: "+rawData);

        int startIndex = 0;
        boolean dtcCountAvailable = true; //ISO-15765 returns dtc count in response
        String workingData = rawData;
        if (workingData.contains(":")) {//CAN(ISO-15765) protocol two and more frames.
            workingData = workingData.replaceAll("[\r\n].:", "");//xxx47yy{codes}
            startIndex = 3;//Header is xxx47yy, xxx is bytes of information to follow, yy showing the number of data items.
        } else {//ISO9141-2, KWP2000 Fast and KWP2000 5Kbps (ISO15031) protocols.
            workingData = workingData.replaceAll("^47|[\r\n]47|[\r\n]", "");
        }

        /*
        * Store raw header, data and request code variables
        *
         */
        int index = startIndex;
        try{
            while (index < rawData.length()){
                if (hasHeaders()){
                    headers.add(rawData.substring(index,index+getHeaderLen()));
                    index += 2;
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
        }
        System.out.printf(TAG+": After parsing Pending Trouble Codes, data: %s, header: %s" +
                ", request code: %s",data.toString(),headers.toString(),requestCode.toString());
    }

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return codes.toString();
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.PENDING_TROUBLE_CODES.getValue();
    }

}
