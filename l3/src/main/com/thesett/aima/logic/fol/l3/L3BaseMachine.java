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
package com.thesett.aima.logic.fol.l3;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * L3BaseMachine provides the basic services common to all L3 machines. This consists of managing the interning name
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
public abstract class L3BaseMachine extends VariableAndFunctorInternerImpl implements L3Machine
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L3BaseMachine.class.getName()); */

    /** The symbol table key for call points. */
    protected static final String SYMKEY_CALLPOINTS = "call_points";

    /** Holds the symbol table. */
    SymbolTable<Integer, String, Object> symbolTable;

    /**
     * Creates the base machine, providing variable and functor symbol tables.
     *
     * @param symbolTable The symbol table.
     */
    protected L3BaseMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super("L3_Variable_Namespace", "L3_Functor_Namespace");
        this.symbolTable = symbolTable;
    }

    /** {@inheritDoc} */
    public abstract void emmitCode(L3CompiledPredicate predicate) throws LinkageException;

    /** {@inheritDoc} */
    public abstract void emmitCode(L3CompiledQuery query) throws LinkageException;

    /** {@inheritDoc} */
    public abstract void emmitCode(int offset, int address);

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public abstract byte[] retrieveCode(L3CallPoint callPoint);

    /**
     * Looks up the offset of the start of the code for the named functor.
     *
     * @param  functorName The interned name of the functor to find the start address of the code for.
     *
     * @return The call table entry of the functors code within the code area of the machine, or an invalid address if
     *         the functor is not known to the machine.
     */
    public L3CallPoint resolveCallPoint(int functorName)
    {
        /*log.fine("public L3CallPoint resolveCallPoint(int functorName): called");*/

        L3CallPoint result = (L3CallPoint) symbolTable.get(functorName, SYMKEY_CALLPOINTS);

        if (result == null)
        {
            result = new L3CallPoint(-1, 0, functorName);
        }

        return result;
    }

    /** {@inheritDoc} */
    public void reserveReferenceToLabel(int labelName, int offset)
    {
        // Create call point with label name if it does not already exist.
        L3ReservedLabel label = (L3ReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null)
        {
            label = new L3ReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        // Add to the mapping from the label to referenced from addresses to fill in later.
        label.referenceList.add(offset);
    }

    /** {@inheritDoc} */
    public void resolveLabelPoint(int labelName, int address)
    {
        // Create the label with resolved address, if it does not already exist.
        L3ReservedLabel label = (L3ReservedLabel) symbolTable.get(labelName, SYMKEY_CALLPOINTS);

        if (label == null)
        {
            label = new L3ReservedLabel(labelName);
            symbolTable.put(labelName, SYMKEY_CALLPOINTS, label);
        }

        label.entryPoint = address;

        // Fill in all references to the label with the correct value. This does nothing if the label was just created.
        for (Integer offset : label.referenceList)
        {
            emmitCode(offset, label.entryPoint);
        }
    }

    /**
     * Resets the machine, to its initial state. This should clear any programs from the machine, and clear all of its
     * stacks and heaps.
     */
    public void reset()
    {
        // Clear the entire symbol table.
        symbolTable.clear();
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
    protected L3CallPoint setCodeAddress(int functorName, int offset, int length)
    {
        L3CallPoint entry = new L3CallPoint(offset, length, functorName);
        symbolTable.put(functorName, SYMKEY_CALLPOINTS, entry);

        return entry;
    }
}
