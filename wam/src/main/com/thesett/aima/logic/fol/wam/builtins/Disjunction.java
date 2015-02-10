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

import java.util.ArrayList;
import java.util.List;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import static com.thesett.aima.logic.fol.wam.compiler.SymbolTableKeys.SYMKEY_PERM_VARS_REMAINING;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.aima.logic.fol.wam.compiler.WAMLabel;
import com.thesett.common.util.SizeableLinkedList;

/**
 * Disjunction implements the Prolog disjunction operator ';' that sets up multiple choice points potentially leading to
 * multiple solutions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Implement the disjunction operator.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class Disjunction extends BaseBuiltIn
{
    /**
     * Creates a cut built-in to implement the specified functor.
     *
     * @param functor        The functor to implement as a built-in.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public Disjunction(Functor functor, DefaultBuiltIn defaultBuiltIn)
    {
        super(functor, defaultBuiltIn);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor functor, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        SizeableLinkedList<WAMInstruction> result = new SizeableLinkedList<WAMInstruction>();
        SizeableLinkedList<WAMInstruction> instructions;

        // Invent some unique names for choice points within a clause.
        clauseName = new FunctorName(clauseName.getName() + "_" + bodyNumber, 0);

        FunctorName choicePointRootName = new FunctorName(clauseName.getName() + "_ilc", 0);
        FunctorName continuationPointName = new FunctorName(clauseName.getName() + "_cnt", 0);

        // Labels the continuation point to jump to, when a choice point succeeds.
        WAMLabel continueLabel = new WAMLabel(continuationPointName, 0);

        // Do a loop over the children of this disjunction, and any child disjunctions encountered. This could be a
        // search? or just recursive exploration. I think it will need to be a DFS.
        List<Term> expressions = new ArrayList<Term>();
        gatherDisjunctions((Disjunction) functor, expressions);

        for (int i = 0; i < expressions.size(); i++)
        {
            Functor expression = (Functor) expressions.get(i);

            boolean isFirst = i == 0;
            boolean isLast = i == (expressions.size() - 1);

            // Labels the entry point to each choice point.
            WAMLabel entryLabel = new WAMLabel(choicePointRootName, i);

            // Label for the entry point to the next choice point, to backtrack to.
            WAMLabel retryLabel = new WAMLabel(choicePointRootName, i + 1);

            if (isFirst && !isLast)
            {
                // try me else.
                result.add(new WAMInstruction(entryLabel, WAMInstruction.WAMInstructionSet.TryMeElse, retryLabel));
            }
            else if (!isFirst && !isLast)
            {
                // retry me else.
                result.add(new WAMInstruction(entryLabel, WAMInstruction.WAMInstructionSet.RetryMeElse, retryLabel));
            }
            else if (isLast)
            {
                // trust me.
                result.add(new WAMInstruction(entryLabel, WAMInstruction.WAMInstructionSet.TrustMe));
            }

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
            instructions = builtIn.compileBodyArguments(expression, false, clauseName, i);
            result.addAll(instructions);

            // Call the body. The number of permanent variables remaining is specified for environment trimming.
            instructions = builtIn.compileBodyCall(expression, false, false, false, 0 /*permVarsRemaining*/);
            result.addAll(instructions);

            // Proceed if this disjunctive branch completes successfully. This does not need to be done for the last
            // branch, as the continuation point will come immediately after.
            if (!isLast)
            {
                result.add(new WAMInstruction(null, WAMInstruction.WAMInstructionSet.Continue, continueLabel));
            }
        }

        result.add(new WAMInstruction(continueLabel, WAMInstruction.WAMInstructionSet.NoOp));

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
        return "Disjunction: [ arguments = " + toStringArguments() + " ]";
    }

    /**
     * Gathers the functors to compile as a sequence of choice points. These exist as the arguments to disjunctions
     * recursively below the supplied disjunction. They are flattened into a list, by performing a left-to-right depth
     * first traversal over the disjunctions, and adding their arguments into a list.
     *
     * @param disjunction The disjunction to explore the arguments of.
     * @param expressions The flattened list of disjunctive terms.
     */
    private void gatherDisjunctions(Disjunction disjunction, List<Term> expressions)
    {
        // Left argument.
        gatherDisjunctionsExploreArgument(disjunction.getArguments()[0], expressions);

        // Right argument.
        gatherDisjunctionsExploreArgument(disjunction.getArguments()[1], expressions);
    }

    /**
     * Explores one argument of a disjunction as part of the {@link #gatherDisjunctions(Disjunction, List)} function.
     *
     * @param term        The argument to explore.
     * @param expressions The flattened list of disjunctive terms.
     */
    private void gatherDisjunctionsExploreArgument(Term term, List<Term> expressions)
    {
        if (term instanceof Disjunction)
        {
            gatherDisjunctions((Disjunction) term, expressions);
        }
        else
        {
            expressions.add(term);
        }
    }
}
