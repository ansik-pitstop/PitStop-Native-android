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
package com.pitstop.bluetooth.elm.commands.pressure;

import com.pitstop.bluetooth.elm.enums.AvailableCommandNames;

/**
 * <p>FuelPressureCommand class.</p>
 *
 */
public class FuelPressureCommand extends PressureCommand {

    /**
     * <p>Constructor for FuelPressureCommand.</p>
     */
    public FuelPressureCommand(boolean hasHeaders) {
        super("01 0A",hasHeaders,1);
    }

    /**
     * <p>Constructor for FuelPressureCommand.</p>
     *
     * @param other a {@link com.pitstop.bluetooth.elm.commands.pressure.FuelPressureCommand} object.
     */
    public FuelPressureCommand(FuelPressureCommand other) {
        super(other);
    }

    /**
     * {@inheritDoc}
     * <p>
     * TODO describe of why we multiply by 3
     */
    @Override
    protected final int preparePressureValue() {
        return buffer.get(2) * 3;
    }

    /** {@inheritDoc} */
    @Override
    public String getName() {
        return AvailableCommandNames.FUEL_PRESSURE.getValue();
    }

}
