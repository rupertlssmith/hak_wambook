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
package com.thesett.aima.logic.fol.wam.optimizer;

import static java.lang.Boolean.TRUE;

import java.util.Deque;
import java.util.LinkedList;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.GetConstant;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.GetList;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.GetStruc;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.GetVar;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.PutConstant;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.PutList;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.PutStruc;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.SetConstant;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.SetVal;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.SetVar;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.SetVoid;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.UnifyConstant;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.UnifyVar;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.UnifyVoid;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * Performs an optimization pass for specialized constant instructions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Optimize constant instructions in the head of a clause.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class OptimizeInstructions implements StateMachine<WAMInstruction, WAMInstruction>
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(OptimizeInstructions.class.getName()); */

    /** Defines the possible states that this state machine can be in. */
    private enum State
    {
        /** No Match. */
        NM,

        /** UnifyVar to UnifyVoid elimination. */
        UVE,

        /** SetVar to SetVoid elimination. */
        SVE;
    }

    /** Holds the matcher that is driving this state machine. */
    private Matcher<WAMInstruction, WAMInstruction> matcher;

    /** Holds the current state machine state. */
    private State state = State.NM;

    /** Holds a buffer of pending instructions to output. */
    private final Deque<WAMInstruction> buffer = new LinkedList<WAMInstruction>();

    /** The symbol table. */
    protected final SymbolTable<Integer, String, Object> symbolTable;

    /** Holds the variable and functor name interner for the machine. */
    private final VariableAndFunctorInterner interner;

    /** Counts the number of void variables seen in a row. */
    private int voidCount = 0;

    /**
     * Builds an instruction optimizer.
     *
     * @param symbolTable The symbol table to get instruction analysis from.
     * @param interner    The functor and variable name interner.
     */
    public OptimizeInstructions(SymbolTable<Integer, String, Object> symbolTable, VariableAndFunctorInterner interner)
    {
        this.symbolTable = symbolTable;
        this.interner = interner;
    }

    /** {@inheritDoc} */
    public void apply(WAMInstruction next)
    {
        shift(next);

        // Anonymous or singleton variable optimizations.
        if ((UnifyVar == next.getMnemonic()) && isVoidVariable(next))
        {
            if (state != State.UVE)
            {
                voidCount = 0;
            }

            discard((voidCount == 0) ? 1 : 2);

            WAMInstruction unifyVoid = new WAMInstruction(UnifyVoid, WAMInstruction.REG_ADDR, (byte) ++voidCount);
            shift(unifyVoid);
            state = State.UVE;

            /*log.fine(next + " -> " + unifyVoid);*/
        }
        else if ((SetVar == next.getMnemonic()) && isVoidVariable(next))
        {
            if (state != State.SVE)
            {
                voidCount = 0;
            }

            discard((voidCount == 0) ? 1 : 2);

            WAMInstruction setVoid = new WAMInstruction(SetVoid, WAMInstruction.REG_ADDR, (byte) ++voidCount);
            shift(setVoid);
            state = State.SVE;

            /*log.fine(next + " -> " + setVoid);*/
        }
        else if ((GetVar == next.getMnemonic()) && (next.getMode1() == WAMInstruction.REG_ADDR) &&
                (next.getReg1() == next.getReg2()))
        {
            discard(1);

            /*log.fine(next + " -> eliminated");*/

            state = State.NM;
        }

        // Constant optimizations.
        else if ((UnifyVar == next.getMnemonic()) && isConstant(next) && isNonArg(next))
        {
            discard(1);

            FunctorName functorName = interner.getDeinternedFunctorName(next.getFunctorNameReg1());
            WAMInstruction unifyConst = new WAMInstruction(UnifyConstant, functorName);
            shift(unifyConst);
            flush();
            state = State.NM;

            /*log.fine(next + " -> " + unifyConst);*/
        }
        else if ((GetStruc == next.getMnemonic()) && isConstant(next) && isNonArg(next))
        {
            discard(1);
            state = State.NM;

            /*log.fine(next + " -> eliminated");*/
        }
        else if ((GetStruc == next.getMnemonic()) && isConstant(next) && !isNonArg(next))
        {
            discard(1);

            WAMInstruction getConst = new WAMInstruction(GetConstant, next.getMode1(), next.getReg1(), next.getFn());
            shift(getConst);
            flush();
            state = State.NM;

            /*log.fine(next + " -> " + getConst);*/
        }
        else if ((PutStruc == next.getMnemonic()) && isConstant(next) && isNonArg(next))
        {
            discard(1);
            state = State.NM;

            /*log.fine(next + " -> eliminated");*/
        }
        else if ((PutStruc == next.getMnemonic()) && isConstant(next) && !isNonArg(next))
        {
            discard(1);

            WAMInstruction putConst = new WAMInstruction(PutConstant, next.getMode1(), next.getReg1(), next.getFn());
            shift(putConst);
            state = State.NM;

            /*log.fine(next + " -> " + putConst);*/
        }
        else if ((SetVal == next.getMnemonic()) && isConstant(next) && isNonArg(next))
        {
            discard(1);

            FunctorName functorName = interner.getDeinternedFunctorName(next.getFunctorNameReg1());
            WAMInstruction setConst = new WAMInstruction(SetConstant, functorName);
            shift(setConst);
            flush();
            state = State.NM;

            /*log.fine(next + " -> " + setConst);*/
        }

        // List optimizations.
        else if ((GetStruc == next.getMnemonic()) &&
                ("cons".equals(next.getFn().getName()) && (next.getFn().getArity() == 2)))
        {
            discard(1);

            WAMInstruction getList = new WAMInstruction(GetList, next.getMode1(), next.getReg1());
            shift(getList);
            state = State.NM;

            /*log.fine(next + " -> " + getList);*/
        }
        else if ((PutStruc == next.getMnemonic()) &&
                ("cons".equals(next.getFn().getName()) && (next.getFn().getArity() == 2)))
        {
            discard(1);

            WAMInstruction putList = new WAMInstruction(PutList, next.getMode1(), next.getReg1());
            shift(putList);
            state = State.NM;

            /*log.fine(next + " -> " + putList);*/
        }

        // Default.
        else
        {
            state = State.NM;
            flush();
        }
    }

    /** {@inheritDoc} */
    public void end()
    {
        flush();
    }

    /** {@inheritDoc} */
    public void setMatcher(Matcher<WAMInstruction, WAMInstruction> matcher)
    {
        this.matcher = matcher;
    }

    /**
     * Checks if the term argument to an instruction was a constant.
     *
     * @param  instruction The instruction to test.
     *
     * @return <tt>true</tt> iff the term argument to an instruction was a constant. <tt>false</tt> will be returned if
     *         this information was not recorded, and cannot be determined.
     */
    public boolean isConstant(WAMInstruction instruction)
    {
        Integer name = instruction.getFunctorNameReg1();

        if (name != null)
        {
            FunctorName functorName = interner.getDeinternedFunctorName(name);

            if (functorName.getArity() == 0)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the term argument to an instruction was a singleton, non-argument position variable. The variable must
     * also be non-permanent to ensure that singleton variables in queries are created.
     *
     * @param  instruction The instruction to test.
     *
     * @return <tt>true</tt> iff the term argument to the instruction was a singleton, non-argument position variable.
     */
    private boolean isVoidVariable(WAMInstruction instruction)
    {
        SymbolKey symbolKey = instruction.getSymbolKeyReg1();

        if (symbolKey != null)
        {
            Integer count = (Integer) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT);
            Boolean nonArgPositionOnly = (Boolean) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_VAR_NON_ARG);
            Integer allocation = (Integer) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_ALLOCATION);

            boolean singleton = (count != null) && count.equals(1);
            boolean nonArgPosition = (nonArgPositionOnly != null) && TRUE.equals(nonArgPositionOnly);
            boolean permanent =
                (allocation != null) && ((byte) ((allocation & 0xff00) >> 8) == WAMInstruction.STACK_ADDR);

            if (singleton && nonArgPosition && !permanent)
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if the term argument to an instruction was in a non-argument position.
     *
     * @param  instruction The instruction to test.
     *
     * @return <tt>true</tt> iff the term argument to an instruction was in a non-argument position. <tt>false</tt> will
     *         be returned if this information was not recorded, and cannot be determined.
     */
    private boolean isNonArg(WAMInstruction instruction)
    {
        SymbolKey symbolKey = instruction.getSymbolKeyReg1();

        if (symbolKey != null)
        {
            Boolean nonArgPositionOnly = (Boolean) symbolTable.get(symbolKey, SymbolTableKeys.SYMKEY_FUNCTOR_NON_ARG);

            if (TRUE.equals(nonArgPositionOnly))
            {
                return true;
            }
        }

        return false;
    }

    /**
     * Discards the specified number of most recent instructions from the output buffer.
     *
     * @param n The number of instructions to discard.
     */
    private void discard(int n)
    {
        for (int i = 0; i < n; i++)
        {
            buffer.pollLast();
        }
    }

    /**
     * Adds an instruction to the output buffer.
     *
     * @param instruction The instruction to add.
     */
    private void shift(WAMInstruction instruction)
    {
        buffer.offer(instruction);
    }

    /** Flushes the output buffer. */
    private void flush()
    {
        matcher.sinkAll(buffer);
    }
}
