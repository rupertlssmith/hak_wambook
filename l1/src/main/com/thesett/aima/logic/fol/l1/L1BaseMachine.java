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

import java.util.HashMap;
import java.util.Map;

import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;

/**
 * L1BaseMachine provides the basic services common to all L1 machines. This consists of managing the interning name
 * tables for functors and variables, and managing the call table for the entry addresses of procedures.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide symbol table for functors names.
 * <tr><td> Provide symbol table for variable names.
 * <tr><td> Store and retrieve the entry points to byte code procedures.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Think about how to handle procedures where the byte code is from different modules, or to be loaded/stored to
 *         disk. Cannot just use a plain offset for the entry point of procedures, because the machine being loaded into
 *         may already hold some code, and the loaded code will come after it, shifting all the entry points. The entry
 *         point addresses need to be handled in a more abstract way; as f/n names in unlinked code, and translated to
 *         raw addresses in linked code. Need to draw a distinction between unlinked and linked code for file machines
 *         and execution machines. The linking is an optimization to perform the address look-ups ahead of execution
 *         time.
 */
public abstract class L1BaseMachine extends VariableAndFunctorInternerImpl implements L1Machine
{
    /** Used to hold the addresses of the functors code. */
    private Map<Integer, L1CallTableEntry> callTable = new HashMap<Integer, L1CallTableEntry>();

    /** Creates the base machine, providing variable and functor symbol tables. */
    protected L1BaseMachine()
    {
        super("L1_Variable_Namespace", "L1_Functor_Namespace");
    }

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     *
     * @return The call table entry of the functors code within the code area of the machine, or <tt>null</tt> if the
     *         functor is not known to the machine.
     */
    public L1CallTableEntry getCodeAddress(int functorName)
    {
        return callTable.get(functorName);
    }

    /**
     * Records the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     * @param  offset      The offset of the start of the functors code within the code area.
     * @param  length      The size of the code to set the address for.
     *
     * @return The call table entry for the functors code within the code area of the machine.
     */
    public L1CallTableEntry setCodeAddress(int functorName, int offset, int length)
    {
        L1CallTableEntry entry = new L1CallTableEntry(offset, length, functorName);
        callTable.put(functorName, entry);

        return entry;
    }

    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public void reset()
    {
        // Clear the call table.
        callTable = new HashMap<Integer, L1CallTableEntry>();
    }
}
