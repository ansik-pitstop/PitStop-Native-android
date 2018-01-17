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

import com.pitstop.bluetooth.elm.enums.AvailableCommandNames;

/**
 * Retrieve available PIDs ranging from 41 to 60.
 *
 */
public class AvailablePidsCommand_41_60 extends AvailablePidsCommand {

    /**
     * Default ctor.
     */
    public AvailablePidsCommand_41_60(boolean header) {
        super("01 40",header);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand_41_60(AvailablePidsCommand_41_60 other, boolean header) {
        super(other, header);
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.PIDS_41_60.getValue();
    }
}
