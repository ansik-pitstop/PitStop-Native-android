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
package com.pitstop.bluetooth.elm.commands.control;

import com.pitstop.bluetooth.elm.enums.AvailableCommandNames;
import com.pitstop.bluetooth.elm.enums.ObdProtocols;

/**
 * It is not needed no know how many DTC are stored.
 * Because when no DTC are stored response will be NO DATA
 * And where are more messages it will be stored in frames that have 7 bytes.
 * In one frame are stored 3 DTC.
 * If we find out DTC P0000 that mean no message are we can end.
 *
 */
public class PendingTroubleCodesCommand extends CodesCommand {

    private final String TAG = getClass().getSimpleName();

    /**
     * <p>Constructor for PendingTroubleCodesCommand.</p>
     */
    public PendingTroubleCodesCommand(ObdProtocols protocol, boolean hasHeaders) {
        super("07",protocol,hasHeaders);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.control.PendingTroubleCodesCommand} object.
     */
    public PendingTroubleCodesCommand(PendingTroubleCodesCommand other) {
        super(other);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.PENDING_TROUBLE_CODES.getValue();
    }
}
