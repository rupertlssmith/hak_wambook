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
package com.thesett.aima.logic.fol.l2;

import java.util.HashMap;
import java.util.Map;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;

/**
 * L2BaseMachine provides the basic services common to all L2 machines. This consists of managing the interning name
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
 *         time. Compilation, results in instructions with references to named functors. Calls and provided call-points.
 *         Linking into a machine, assigns the call-points to addresses, and links all the calls to addresses. A mapping
 *         from functor names to addresses is created, and is queriable in both directions. Storing the compiled code to
 *         a file machine, assign the call-points to locations within a file. A mapping from functor names to file
 *         locations is created. A table of calls is added to, that provided a quick link to all the calls in the
 *         functor, and the functor name that they are calling to. This is used to quickly link all the calls to their
 *         call-points. The file must also serialize the interned names. Loading compiled code from a file machine. The
 *         table of calls is used, to find the functor name for each. Link compiled code from a file machine into an
 *         execution machine. The table of calls is used, to quickly replace each call with the address of its
 *         call-point. If the call-point does not already exist, linking fails. The newly linked functor, gets assign an
 *         address in the execution machine.
 */
public abstract class L2BaseMachine extends VariableAndFunctorInternerImpl implements L2Machine
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L2BaseMachine.class.getName()); */

    /** Used to hold the addresses of the functors code. */
    private Map<Integer, L2CallPoint> callTable = new HashMap<Integer, L2CallPoint>();

    /** Creates the base machine, providing variable and functor symbol tables. */
    protected L2BaseMachine()
    {
        super("L2_Variable_Namespace", "L2_Functor_Namespace");
    }

    /** {@inheritDoc} */
    public abstract void emmitCode(L2CompiledClause clause) throws LinkageException;

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public abstract byte[] retrieveCode(L2CallPoint callPoint);

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     *
     * @return The call table entry of the functors code within the code area of the machine, or <tt>null</tt> if the
     *         functor is not known to the machine.
     */
    public L2CallPoint resolveCallPoint(int functorName)
    {
        /*log.fine("public L2CallPoint resolveCallPoint(int functorName): called");*/

        L2CallPoint result = callTable.get(functorName);

        if (result == null)
        {
            result = new L2CallPoint(-1, 0, functorName);
        }

        return result;
    }

    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public void reset()
    {
        // Clear the call table.
        callTable = new HashMap<Integer, L2CallPoint>();
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
    protected L2CallPoint setCodeAddress(int functorName, int offset, int length)
    {
        L2CallPoint entry = new L2CallPoint(offset, length, functorName);
        callTable.put(functorName, entry);

        return entry;
    }
}
