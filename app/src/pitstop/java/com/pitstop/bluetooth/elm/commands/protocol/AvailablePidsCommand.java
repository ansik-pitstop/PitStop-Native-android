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
package com.pitstop.bluetooth.elm.commands.protocol;

import com.pitstop.bluetooth.elm.commands.ObdCommand;

/**
 * Retrieve available PIDs ranging from 21 to 40.
 *
 */
public abstract class AvailablePidsCommand extends ObdCommand {

    /**
     * Default ctor.
     *
     * @param command a {@link java.lang.String} object.
     */
    public AvailablePidsCommand(String command, boolean header) {
        super(command, header,4);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand} object.
     */

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand(AvailablePidsCommand other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    protected void performCalculations() {

    }

    /** {@inheritDoc} */
    @Override
    public String getFormattedResult() {
        return getCalculatedResult();
    }

    /** {@inheritDoc} */
    @Override
    public String getCalculatedResult() {
        return rawData;
    }


}
