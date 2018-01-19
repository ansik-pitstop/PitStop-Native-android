/**
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.pitstop.bluetooth.elm.commands;

import android.util.Log;

import com.pitstop.bluetooth.elm.exceptions.BusInitException;
import com.pitstop.bluetooth.elm.exceptions.MisunderstoodCommandException;
import com.pitstop.bluetooth.elm.exceptions.NoDataException;
import com.pitstop.bluetooth.elm.exceptions.NonNumericResponseException;
import com.pitstop.bluetooth.elm.exceptions.ResponseException;
import com.pitstop.bluetooth.elm.exceptions.StoppedException;
import com.pitstop.bluetooth.elm.exceptions.UnableToConnectException;
import com.pitstop.bluetooth.elm.exceptions.UnknownErrorException;
import com.pitstop.bluetooth.elm.exceptions.UnsupportedCommandException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Base OBD command.
 *
 */
public abstract class ObdCommand {

    private final String TAG = ObdCommand.class.getSimpleName();

    /**
     * Error classes to be tested in order
     */
    private final Class[] ERROR_CLASSES = {
            UnableToConnectException.class,
            BusInitException.class,
            MisunderstoodCommandException.class,
            NoDataException.class,
            StoppedException.class,
            UnknownErrorException.class,
            UnsupportedCommandException.class
    };
    private final int HEADER_LEN = 3; //Length of headers in each response, seems to be three by default

    protected ArrayList<Integer> buffer = null;
    protected String cmd = null;
    protected boolean useImperialUnits = false;
    protected String rawData = null;
    protected Long responseDelayInMs = null;
    protected int byteLen; //Length of data in the response
    private long start;
    private long end;
    protected boolean hasHeaders = false;
    protected boolean singleResponse;

    private List<String> headers;
    private List<String> data;
    private List<String> requestCode;

    public ObdCommand(String command, boolean hasHeaders, int byteLen){
        this.byteLen = byteLen;
        this.cmd = command;
        this.buffer = new ArrayList<>();
        this.hasHeaders = hasHeaders;
        this.headers = new ArrayList<>();
        this.data = new ArrayList<>();
        this.requestCode = new ArrayList<>();
    }

    public ObdCommand(String command, boolean hasHeaders, boolean singleResponse, int byteLen){
        this(command,hasHeaders,byteLen);
        this.singleResponse = singleResponse;
    }

    /**
     * Prevent empty instantiation
     */
    private ObdCommand() {
    }

    public boolean hasHeaders() {
        return hasHeaders;
    }

    /**
     * Copy ctor.
     *
     * @param other the ObdCommand to copy.
     */
    public ObdCommand(ObdCommand other) {
        this(other.cmd,other.hasHeaders,other.byteLen);
    }

    /**
     * Sends the OBD-II request and deals with the response.
     * <p>
     * This method CAN be overriden in fake commands.
     *
     * @param in  a {@link java.io.InputStream} object.
     * @param out a {@link java.io.OutputStream} object.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    public void run(InputStream in, OutputStream out) throws IOException,
            InterruptedException {
        synchronized (ObdCommand.class) {//Only one command can write and read a data in one time.
            start = System.currentTimeMillis();
            sendCommand(out);
            readResult(in);
            end = System.currentTimeMillis();
        }
    }

    /**
     * Sends the OBD-II request.
     * <p>
     * This method may be overriden in subclasses, such as ObMultiCommand or
     * TroubleCodesCommand.
     *
     * @param out The output stream.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    protected void sendCommand(OutputStream out) throws IOException,
            InterruptedException {
        // write to OutputStream (i.e.: a BluetoothSocket) with an added
        // Carriage return
        out.write((cmd + "\r").getBytes());
        out.flush();
        if (responseDelayInMs != null && responseDelayInMs > 0) {
            Thread.sleep(responseDelayInMs);
        }
    }

    /**
     * Resends this command.
     *
     * @param out a {@link java.io.OutputStream} object.
     * @throws java.io.IOException            if any.
     * @throws java.lang.InterruptedException if any.
     */
    protected void resendCommand(OutputStream out) throws IOException,
            InterruptedException {
        out.write("\r".getBytes());
        out.flush();
        if (responseDelayInMs != null && responseDelayInMs > 0) {
            Thread.sleep(responseDelayInMs);
        }
    }

    /**
     * Reads the OBD-II response.
     * <p>
     * This method may be overriden in subclasses, such as ObdMultiCommand.
     *
     * @param in a {@link java.io.InputStream} object.
     * @throws java.io.IOException if any.
     */
    protected void readResult(InputStream in) throws IOException {
        readRawData(in);
        checkForErrors();
        fillBuffer();
        performCalculations();
    }

    /**
     * This method exists so that for each command, there must be a method that is
     * called only once to perform calculations.
     */
    protected abstract void performCalculations();


    private static Pattern WHITESPACE_PATTERN = Pattern.compile("\\s");
    private static Pattern BUSINIT_PATTERN = Pattern.compile("(BUS INIT)|(BUSINIT)|(\\.)");
    private static Pattern SEARCHING_PATTERN = Pattern.compile("SEARCHING");
    private static Pattern DIGITS_LETTERS_PATTERN = Pattern.compile("([0-9A-F])+");

    protected String replaceAll(Pattern pattern, String input, String replacement) {
        return pattern.matcher(input).replaceAll(replacement);
    }

    protected String removeAll(Pattern pattern, String input) {
        return pattern.matcher(input).replaceAll("");
    }

    /**
     * <p>fillBuffer.</p>
     */
    protected void fillBuffer() {
        rawData = removeAll(WHITESPACE_PATTERN, rawData); //removes all [ \t\n\x0B\f\r]
        rawData = removeAll(BUSINIT_PATTERN, rawData);

        Log.d(TAG,getName()+": rawData after removing patterns: "+rawData);

        if (!DIGITS_LETTERS_PATTERN.matcher(rawData).matches()) {
            throw new NonNumericResponseException(rawData);
        }

        // read string each two chars
        buffer.clear();
        int begin = 0;
        int end = 2;
        while (end <= rawData.length()) {
            buffer.add(Integer.decode("0x" + rawData.substring(begin, end)));
            begin = end;
            end += 2;
        }
        Log.d(TAG,getName()+": buffer after filling: "+buffer.toString());
    }

    /**
     * <p>
     * readRawData.</p>
     *
     * @param in a {@link java.io.InputStream} object.
     * @throws java.io.IOException if any.
     */
    protected void readRawData(InputStream in) throws IOException {
        //Log.d(TAG,"readRawData() command: "+getName());
        byte b = 0;
        StringBuilder res = new StringBuilder();

        // read until '>' arrives OR end of stream reached
        char c;
        // -1 if the end of the stream is reached
        while (((b = (byte) in.read()) > -1)) {
            c = (char) b;
            if (c == '>') // read until '>' arrives
            {
                break;
            }
            res.append(c);
        }

        System.out.println(TAG+":"+ getName() +": rawData: "+res);

        /*
         * Imagine the following response 41 0c 00 0d.
         *
         * ELM sends strings!! So, ELM puts spaces between each "byte". And pay
         * attention to the fact that I've put the word byte in quotes, because 41
         * is actually TWO bytes (two chars) in the socket. So, we must do some more
         * processing..
         */
        rawData = removeAll(SEARCHING_PATTERN, res.toString());

        /*
         * Data may have echo or informative text like "INIT BUS..." or similar.
         * The response ends with two carriage return characters. So we need to take
         * everything from the last carriage return before those two (trimmed above).
         */
        //kills multiline.. rawData = rawData.substring(rawData.lastIndexOf(13) + 1);
        rawData = removeAll(WHITESPACE_PATTERN, rawData);//removes all [ \t\n\x0B\f\r]

        System.out.println(TAG+":"+ getName() +": rawData: "+res);

        /*
        * Data is formatted like the following "HEADER REQUEST_CODE DATA HEADER REQUEST_CODE DATA ..."
        * where each ECU responds with one HEADER, REQUEST_CODE and DATA. We need to store the
        * raw data appropriately for each ECU
        *
        * If the response is one which only has a single response then don't perform this parsing
         */
        if (!singleResponse){
            String cmdNoSpace = cmd.replace(" ","");
            int singleECUResponseLen = (byteLen*2) + getHeaderLen() + cmdNoSpace.length();
            int numResponses = rawData.length()/singleECUResponseLen;
            System.out.println(TAG+": single ECU response length: "+singleECUResponseLen
                    +", number of responses: " +numResponses+", rawData len: "+rawData.length());
            if (singleECUResponseLen * numResponses != rawData.length()){
                throw new IOException();
            }

            for (int i=0;i<numResponses;i++){
                int curIndex = singleECUResponseLen*i;
                headers.add(rawData.substring(curIndex,curIndex+getHeaderLen()));
                curIndex += getHeaderLen();
                requestCode.add(rawData.substring(curIndex,curIndex+cmdNoSpace.length()));
                curIndex += cmdNoSpace.length();
                data.add(rawData.substring(curIndex,curIndex+(byteLen*2)));
            }
        }
    }

    void checkForErrors() {
        for (Class<? extends ResponseException> errorClass : ERROR_CLASSES) {
            ResponseException messageError;

            try {
                messageError = errorClass.newInstance();
                messageError.setCommand(this.cmd);
            } catch (InstantiationException e) {
                throw new RuntimeException(e);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

            if (messageError.isError(rawData)) {
                throw messageError;
            }
        }
    }

    /**
     * <p>getResult.</p>
     *
     * @return the raw command response in string representation.
     */
    public String getResult() {
        return rawData;
    }

    /**
     * <p>getFormattedResult.</p>
     *
     * @return a formatted command response in string representation.
     */
    public abstract String getFormattedResult();

    /**
     * <p>getCalculatedResult.</p>
     *
     * @return the command response in string representation, without formatting.
     */
    public abstract String getCalculatedResult();

    /**
     * <p>Getter for the field <code>buffer</code>.</p>
     *
     * @return a list of integers
     */
    protected ArrayList<Integer> getBuffer() {
        return buffer;
    }

    /**
     * <p>useImperialUnits.</p>
     *
     * @return true if imperial units are used, or false otherwise
     */
    public boolean useImperialUnits() {
        return useImperialUnits;
    }

    /**
     * The unit of the result, as used in {@link #getFormattedResult()}
     *
     * @return a String representing a unit or "", never null
     */
    public String getResultUnit() {
        return "";//no unit by default
    }

    /**
     * Set to 'true' if you want to use imperial units, false otherwise. By
     * default this value is set to 'false'.
     *
     * @param isImperial a boolean.
     */
    public void useImperialUnits(boolean isImperial) {
        this.useImperialUnits = isImperial;
    }

    /**
     * <p>getName.</p>
     *
     * @return the OBD command name.
     */
    public abstract String getName();

    /**
     * Time the command waits before returning from #sendCommand()
     *
     * @return delay in ms (may be null)
     */
    public Long getResponseTimeDelay() {
        return responseDelayInMs;
    }

    /**
     * Time the command waits before returning from #sendCommand()
     *
     * @param responseDelayInMs a Long (can be null)
     */
    public void setResponseTimeDelay(Long responseDelayInMs) {
        this.responseDelayInMs = responseDelayInMs;
    }

    //fixme resultunit
    /**
     * <p>Getter for the field <code>start</code>.</p>
     *
     * @return a long.
     */
    public long getStart() {
        return start;
    }

    /**
     * <p>Setter for the field <code>start</code>.</p>
     *
     * @param start a long.
     */
    public void setStart(long start) {
        this.start = start;
    }

    /**
     * <p>Getter for the field <code>end</code>.</p>
     *
     * @return a long.
     */
    public long getEnd() {
        return end;
    }

    /**
     * <p>Setter for the field <code>end</code>.</p>
     *
     * @param end a long.
     */
    public void setEnd(long end) {
        this.end = end;
    }

    /**
     * <p>getCommandPID.</p>
     *
     * @return a {@link java.lang.String} object.
     * @since 1.0-RC12
     */
    public final String getCommandPID() {
        return cmd.substring(3);
    }

    /**
     * <p>getCommandMode.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public final String getCommandMode() {
        if (cmd.length() >= 2) {
            return cmd.substring(0, 2);
        } else {
            return cmd;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ObdCommand that = (ObdCommand) o;

        return cmd != null ? cmd.equals(that.cmd) : that.cmd == null;
    }

    @Override
    public int hashCode() {
        return cmd != null ? cmd.hashCode() : 0;
    }

    public List<String> getData() {
        return data;
    }

    public List<String> getHeaders() {
        return headers;
    }

    public List<String> getRequestCode() {
        return requestCode;
    }

    public boolean isSingleResponse() {
        return singleResponse;
    }

    private int getHeaderLen(){
        if (hasHeaders){
            return HEADER_LEN;
        }else{
            return 0;
        }
    }
}
