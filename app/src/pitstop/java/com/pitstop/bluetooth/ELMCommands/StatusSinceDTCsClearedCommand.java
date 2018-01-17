package com.pitstop.bluetooth.ELMCommands;

import com.pitstop.bluetooth.elm.commands.control.DtcNumberCommand;
import com.pitstop.bluetooth.elm.enums.AvailableCommandNames;

/**
 * Created by ishan on 2017-12-20.
 */

public class StatusSinceDTCsClearedCommand extends DtcNumberCommand {

    private int codeCount = 0;
    private boolean milOn = false;
    private String  ignitionType;

    public int getCodeCount() {
        return codeCount;
    }

    public boolean isMilOn() {
        return milOn;
    }

    public String getIgnitionType() {
        return ignitionType;
    }

    /**
     * Default ctor.
     */
    public StatusSinceDTCsClearedCommand() {
        super();
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.control.DtcNumberCommand} object.
     */
    public StatusSinceDTCsClearedCommand(DtcNumberCommand other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    protected void performCalculations() {
        // ignore first two bytes [hh hh] of the response
        final int mil = buffer.get(2);
        milOn = (mil & 0x80) == 128;
        codeCount = mil & 0x7F;
        final int ignition = buffer.get(3) & 0x08;
        if (ignition == 0)
            ignitionType = "Spark";
        else
            ignitionType = "Diesel";

    }

    /**
     * <p>getFormattedResult.</p>
     *
     * @return a {@link java.lang.String} object.
     */
    public String getFormattedResult() {
        final String res = milOn ? "MIL is ON " : "MIL is OFF ";
        return res + codeCount + " codes ignition type is " + ignitionType;
    }

    /** {@inheritDoc} */
    @Override
    public String getCalculatedResult() {
        return String.valueOf(codeCount);
    }

    /**
     * <p>getTotalAvailableCodes.</p>
     *
     * @return the number of trouble codes currently flaggd in the ECU.
     */
    public int getTotalAvailableCodes() {
        return codeCount;
    }

    /**
     * <p>Getter for the field <code>milOn</code>.</p>
     *
     * @return the state of the check engine light state.
     */
    public boolean getMilOn() {
        return milOn;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.DTC_NUMBER.getValue();
    }

}
