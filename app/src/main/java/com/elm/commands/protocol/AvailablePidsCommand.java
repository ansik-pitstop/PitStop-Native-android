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
package com.elm.commands.protocol;

import com.elm.commands.PersistentCommand;

/**
 * Retrieve available PIDs ranging from 21 to 40.
 *
 */
public abstract class AvailablePidsCommand extends PersistentCommand {

    /**
     * Default ctor.
     *
     * @param command a {@link java.lang.String} object.
     */
    public AvailablePidsCommand(String command, boolean header) {
        super(command, header);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.elm.commands.protocol.AvailablePidsCommand} object.
     */
    public AvailablePidsCommand(AvailablePidsCommand other, boolean header) {
        super(other, header);
    }

    /**
     * Copy ctor.
     *
     * @param other a {@link com.elm.commands.protocol.AvailablePidsCommand} object.
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
