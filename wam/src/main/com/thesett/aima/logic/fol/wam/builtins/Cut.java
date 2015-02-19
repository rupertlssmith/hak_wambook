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
package com.thesett.aima.logic.fol.wam.builtins;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.Cut;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.WAMInstructionSet.NeckCut;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.doublemaps.SymbolKey;

/**
 * Cut implements the prolog '!' operator, that prevents back-tracking within a functor. '!' is true; that is it does
 * not ever fail. All choice points between the cut and the parent goal are removed. The effect is to commit to use both
 * the current clause and the variable substitutions found at the point of the cut.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Implement the cut operator.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class Cut extends BaseBuiltIn
{
    /**
     * Creates a cut built-in to implement the specified functor.
     *
     * @param functor        The functor to implement as a built-in.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public Cut(Functor functor, DefaultBuiltIn defaultBuiltIn)
    {
        // Here the cut functor is transformed from ! to !(Y), where Y is a level variable that may be needed
        // to hold the current choice point.
        super(new Functor(functor.getName(), new Term[] { CutLevelVariable.CUT_LEVEL_VARIABLE }), defaultBuiltIn);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor expression, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        return new SizeableLinkedList<WAMInstruction>();
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyCall(Functor expression, boolean isFirstBody,
        boolean isLastBody, boolean chainRule, int permVarsRemaining)
    {
        SizeableLinkedList<WAMInstruction> instructions = new SizeableLinkedList<WAMInstruction>();

        if (isFirstBody)
        {
            instructions.add(new WAMInstruction(NeckCut));
        }
        else
        {
            Integer cutLevelVarAllocation =
                (Integer) defaultBuiltIn.getSymbolTable().get(CutLevelVariable.CUT_LEVEL_VARIABLE.getSymbolKey(),
                    SymbolTableKeys.SYMKEY_ALLOCATION);

            instructions.add(new WAMInstruction(Cut, (byte) (cutLevelVarAllocation & 0xff)));
        }

        return instructions;
    }

    /**
     * Creates a string representation of this functor, mostly used for debugging purposes.
     *
     * @return A string representation of this functor.
     */
    public String toString()
    {
        return "Cut";
    }

    /**
     * CutLevelVariable is an internal variable used to hold the current choice point when entering a clause with a deep
     * cut, in order that the choice point may be restored on the cut.
     *
     * <p/>A single instance of this variable is all that is needed, since it is only used temporarily during the
     * compilation of clauses. This instance will be annotated by the symbol key initialization so that it can be found in
     * the correct place within each clause; on compiling a new clause it will be re-initialized into that clauses symbol
     * table.
     *
     * <p/><b>Note:</b> A singleton instance here will prevent the compiler from ever being multi-threaded, since clauses
     * can no longer be compiled in parallel. This can be worked around easily by making this variable a thread local.
     */
    public static class CutLevelVariable extends Variable
    {
        /** The single cut level variable instance. */
        public static final CutLevelVariable CUT_LEVEL_VARIABLE = new CutLevelVariable();

        /** Private constructor to prevent instantiation other than the singleton. */
        CutLevelVariable()
        {
            super(-1, null, true);
        }
    }
}
