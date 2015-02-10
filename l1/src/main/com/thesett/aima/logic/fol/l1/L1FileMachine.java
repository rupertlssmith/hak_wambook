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
package com.thesett.aima.logic.fol.l1;

import com.thesett.common.error.NotImplementedException;

/**
 * L1FileMachine is an {@link L1Machine} that can load and store compiled L1 byte code to files.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsbilities <th> Collaborations
 * <tr><td> Load byte code from a file.
 * <tr><td> Store byte code to a file.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Provide a way for this machine to pass the byte code to an execution machine. For example, by wrapping one.
 */
public class L1FileMachine extends L1BaseMachine
{
    /**
     * Adds compiled byte code to the code area of the machine.
     *
     * @param  code        The compiled byte code.
     * @param  start       The start offset within the compiled code to copy into the machine.
     * @param  end         The end offset within the compiled code to copy into the machine.
     * @param  functorName The interned name of the functor that the code is for.
     * @param  isQuery     <tt>true</tt> if the code is for a query, <tt>false</ff> if it is for a program.
     *
     * @return The start offset of the functors code within the code area of the machine.
     */
    public L1CallTableEntry addCode(byte[] code, int start, int end, int functorName, boolean isQuery)
    {
        throw new NotImplementedException();
    }

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callTableEntry The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] getByteCode(L1CallTableEntry callTableEntry)
    {
        throw new NotImplementedException();
    }
}
