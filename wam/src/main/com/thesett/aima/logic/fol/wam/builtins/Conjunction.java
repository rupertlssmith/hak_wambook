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
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_PERM_VARS_REMAINING;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.common.util.SizeableLinkedList;

/**
 * Conjunction implements the Prolog conjunction operator ',' that sets up multiple choice points potentially leading to
 * multiple solutions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Implement the conjunction operator.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class Conjunction extends BaseBuiltIn
{
    /**
     * Creates a cut built-in to implement the specified functor.
     *
     * @param functor        The functor to implement as a built-in.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public Conjunction(Functor functor, DefaultBuiltIn defaultBuiltIn)
    {
        super(functor, defaultBuiltIn);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor functor, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        SizeableLinkedList<WAMInstruction> result = new SizeableLinkedList<WAMInstruction>();
        SizeableLinkedList<WAMInstruction> instructions;

        Term[] expressions = functor.getArguments();

        for (int i = 0; i < expressions.length; i++)
        {
            Functor expression = (Functor) expressions[i];

            Integer permVarsRemaining =
                (Integer) defaultBuiltIn.getSymbolTable().get(expression.getSymbolKey(), SYMKEY_PERM_VARS_REMAINING);

            // Select a non-default built-in implementation to compile the functor with, if it is a built-in.
            BuiltIn builtIn;

            if (expression instanceof BuiltIn)
            {
                builtIn = (BuiltIn) expression;
            }
            else
            {
                builtIn = defaultBuiltIn;
            }

            // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule.
            instructions = builtIn.compileBodyArguments(expression, false, clauseName, bodyNumber);
            result.addAll(instructions);

            // Call the body. The number of permanent variables remaining is specified for environment trimming.
            instructions = builtIn.compileBodyCall(expression, false, false, false, 0 /*permVarsRemaining*/);
            result.addAll(instructions);
        }

        return result;
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyCall(Functor expression, boolean isFirstBody,
        boolean isLastBody, boolean chainRule, int permVarsRemaining)
    {
        return new SizeableLinkedList<WAMInstruction>();
    }

    /**
     * Creates a string representation of this functor, mostly used for debugging purposes.
     *
     * @return A string representation of this functor.
     */
    public String toString()
    {
        return "Conjunction: [ arguments = " + toStringArguments() + " ]";
    }
}
