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
package com.thesett.aima.logic.fol.wam.machine;

import java.nio.ByteBuffer;

/**
 * WAMCodeView provides a read-only view onto the code buffer of a machine.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Allow portions of the code buffer to be examined. </td></tr>
 * <tr><td> Provide reverse look-ups from addresses to interned names for locations. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface WAMCodeView
{
    /**
     * Provides read access to the machines bytecode buffer.
     *
     * @param  start  The start offset within the buffer to read.
     * @param  length Then length within the buffer to read.
     *
     * @return The requested portion of the machines bytecode buffer.
     */
    ByteBuffer getCodeBuffer(int start, int length);

    /**
     * Attempts to find a label or functor name for a given address within the code area of the machine.
     *
     * @param  address The address to look up.
     *
     * @return The label or functor name matching the address, or <tt>null</tt> if none is set at that address.
     */
    Integer getNameForAddress(int address);
}
