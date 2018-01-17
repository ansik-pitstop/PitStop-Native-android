package com.pitstop.bluetooth.OBDcommands;

import com.elm.commands.ObdCommand;

/**
 * Created by ishan on 2017-12-30.
 */

public class HeaderOffCommand extends ObdCommand {
    public HeaderOffCommand() {
        super("ATH 0");
    }

    @Override
    protected void performCalculations() {

    }

    @Override
    public String getFormattedResult() {
        return "";
    }

    @Override
    public String getCalculatedResult() {
        return "";
    }

    @Override
    public String getName() {
        return HeaderOffCommand.class.getSimpleName();
    }
}
