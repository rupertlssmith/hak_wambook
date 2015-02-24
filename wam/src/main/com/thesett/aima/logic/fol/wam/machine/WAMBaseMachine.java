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

import java.util.HashMap;
import java.util.Map;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMReservedLabel;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * WAMBaseMachine provides the basic services common to all WAM machines. This consists of managing the interning name
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
 */
public abstract class WAMBaseMachine extends VariableAndFunctorInternerImpl implements WAMMachine, WAMCodeView
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(WAMBaseMachine.class.getName()); */

    /** The symbol table key for call points. */
    protected static final String SYMKEY_CALLPOINTS = "call_points";

    /** Holds the symbol table. */
    protected SymbolTable<Integer, String, Object> symbolTable;

    /** Holds the reverse symbol table to look up names by addresses. */
    protected Map<Integer, Integer> reverseTable = new HashMap<Integer, Integer>();

    /**
     * Creates the base machine, providing variable and functor symbol tables.
     *
     * @param symbolTable The symbol table.
     */
    protected WAMBaseMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super("WAM_Variable_Namespace", "WAM_Functor_Namespace");
        this.symbolTable = symbolTable;
    }

    /** {@inheritDoc} */
    public abstract void emmitCode(WAMCompiledPredicate predicate) throws LinkageException;

    /** {@inheritDoc} */
    public abstract void emmitCode(WAMCompiledQuery query) throws LinkageException;

    /** {@inheritDoc} */
    public abstract void emmitCode(int offset, int address);

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public abstract byte[] retrieveCode(WAMCallPoint callPoint);

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     *
     * @return The call table entry of the functors code within the code area of the machine, or an invalid address if
     *         the functor is not known to the machine.
     */
    public WAMCallPoint resolveCallPoint(int functorName)
    {
        /*log.fine("public WAMCallPoint resolveCallPoint(int functorName): called");*/

        WAMCallPoint result = (WAMCallPoint) symbolTable.get(functorName, SYMKEY_CALLPOINTS);

        if (result == null)
        {
            result = new WAMCallPoint(-1, 0, functorName);
        }

        return result;
    }

    /** {@inheritDoc} */
    public void reserveReferenceToLabel(int labelName, int offset)
    {
        // Create call point with label name if it does not already exist.
        WAMReservedLabel label = (WAMReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null)
        {
            label = new WAMReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        // Add to the mapping from the label to referenced from addresses to fill in later.
        label.referenceList.add(offset);
    }

    /** {@inheritDoc} */
    public void resolveLabelPoint(int labelName, int address)
    {
        // Create the label with resolved address, if it does not already exist.
        WAMReservedLabel label = (WAMReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null)
        {
            label = new WAMReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        label.entryPoint = address;

        // Fill in all references to the label with the correct value. This does nothing if the label was just created.
        for (Integer offset : label.referenceList)
        {
            emmitCode(offset, label.entryPoint);
        }

        // Keep a reverse lookup from address to label name.
        reverseTable.put(address, labelName);
    }

    /** {@inheritDoc} */
    public Integer getNameForAddress(int address)
    {
        return reverseTable.get(address);
    }

    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public void reset()
    {
        // Clear the entire symbol table.
        symbolTable.clear();
        reverseTable.clear();
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
    protected WAMCallPoint setCodeAddress(int functorName, int offset, int length)
    {
        WAMCallPoint entry = new WAMCallPoint(offset, length, functorName);
        symbolTable.put(functorName, SYMKEY_CALLPOINTS, entry);

        // Keep a reverse lookup from address to functor name.
        reverseTable.put(offset, functorName);

        return entry;
    }

    /**
     * Records the id of an internal function for the named functor. The method name uses the word 'address' but this is
     * not really accurate, the address field is used to hold an id of the internal function to be invoked. This method
     * differs from {@link #setCodeAddress(int, int, int)}, as it does not set the reverse mapping from the address to
     * the functor name, since an address is not really being used.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     * @param  id          The offset of the start of the functors code within the code area.
     *
     * @return The call table entry for the functors code within the code area of the machine.
     */
    protected WAMCallPoint setInternalCodeAddress(int functorName, int id)
    {
        WAMCallPoint entry = new WAMCallPoint(id, 0, functorName);
        symbolTable.put(functorName, SYMKEY_CALLPOINTS, entry);

        return entry;
    }
}
