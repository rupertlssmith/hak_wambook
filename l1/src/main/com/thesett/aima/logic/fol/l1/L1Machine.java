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

import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

/**
 * L1Machine is an abstract machine capable of handling the L1 language in its compiled form.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide symbol table for functors names.
 * <tr><td> Provide symbol table for variable names.
 * <tr><td> Accept injected byte code for functors into the machine.
 * <tr><td> Provide the address of that start offset of code for named functors.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface L1Machine extends VariableAndFunctorInterner
{
    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public abstract void reset();

    /**
     * Adds compiled byte code to the code area of the machine.
     *
     * @param  code        The compiled byte code.
     * @param  start       The start offset within the compiled code to copy into the machine.
     * @param  end         The end offset within the compiled code to copy into the machine.
     * @param  functorName The interned name of the functor that the code is for.
     * @param  isQuery     <tt>true</tt> if the code is for a query, <tt>false</ff> if it is for a program.
     *
     * @return The call table entry for the functors code within the code area of the machine.
     */
    public L1CallTableEntry addCode(byte[] code, int start, int end, int functorName, boolean isQuery);

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     *
     * @return The call table entry of the functors code within the code area of the machine, or <tt>null</tt> if the
     *         functor is not known to the machine.
     */
    public L1CallTableEntry getCodeAddress(int functorName);

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callTableEntry The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] getByteCode(L1CallTableEntry callTableEntry);
}
