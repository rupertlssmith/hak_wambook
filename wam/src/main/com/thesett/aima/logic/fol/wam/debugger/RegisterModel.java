/*
 * Copyright The Sett Ltd, 2005 to 2014.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thesett.aima.logic.fol.wam.debugger;

/**
 * RegisterModel describes the register and flag set of some machine, reflectively so tools can be built on top of the
 * register model dynamically.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Describe the register and flag set of some abstract machine.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface RegisterModel
{
    /**
     * Provides a list of all of the registers in the machine.
     *
     * @return A list of all of the registers in the machine.
     */
    String[] getRegisterNames();

    /**
     * Provides a list of all of the flags in the machine.
     *
     * @return A list of all of the flags in the machine.
     */
    String[] getFlagNames();

    /**
     * Provides the number of bytes that a register occupies.
     *
     * @param  name The register to get the size of.
     *
     * @return The number of bytes that a register occupies.
     */
    int getRegisterSizeBytes(String name);

    /**
     * Provides the contents of a register.
     *
     * @param  name The register to read.
     *
     * @return The registers value.
     */
    byte[] getRegister(String name);

    /**
     * Gets the status of a flag.
     *
     * @param  name The flag to read.
     *
     * @return The flags name.
     */
    boolean getFlag(String name);
}
