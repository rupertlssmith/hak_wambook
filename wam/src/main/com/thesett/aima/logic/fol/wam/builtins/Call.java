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
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.common.util.SizeableLinkedList;

/**
 * Call implements the Prolog 'call' operator. Call resolves its argument as a query. The argument to call may be a
 * variable, but must be bound to a callable functor or atom at the time the call is made in order to be valid.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Call a callable functor or atom as a query for resolution.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class Call extends BaseBuiltIn
{
    /**
     * Creates a cut built-in to implement the specified functor.
     *
     * @param functor        The functor to implement as a built-in.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public Call(Functor functor, DefaultBuiltIn defaultBuiltIn)
    {
        super(functor, defaultBuiltIn);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor expression, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        // Build the argument to call in the usual way.
        return defaultBuiltIn.compileBodyArguments(expression, isFirstBody, clauseName, bodyNumber);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyCall(Functor expression, boolean isFirstBody,
        boolean isLastBody, boolean chainRule, int permVarsRemaining)
    {
        // Used to build up the results in.
        SizeableLinkedList<WAMInstruction> instructions = new SizeableLinkedList<WAMInstruction>();

        // Generate the call or tail-call instructions, followed by the call address, which is f_n of the
        // called program.
        if (isLastBody)
        {
            // Deallocate the stack frame at the end of the clause, but prior to calling the last
            // body predicate.
            // This is not required for chain rules, as they do not need a stack frame.
            if (!chainRule)
            {
                instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.Deallocate));
            }

            instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.CallInternal,
                    (byte) (permVarsRemaining & 0xff), new FunctorName("execute", 1)));
        }
        else
        {
            instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.CallInternal,
                    (byte) (permVarsRemaining & 0xff), new FunctorName("call", 1)));
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
        return "Call: [ arguments = " + toStringArguments() + " ]";
    }
}
