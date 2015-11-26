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
package com.thesett.aima.logic.fol.wam.compiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.thesett.aima.logic.fol.BasePositionalVisitor;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalContext;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.common.util.doublemaps.SymbolKey;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * PositionAndOccurrenceVisitor visits a clause to gather information about the positions in which components of the
 * clause appear.
 *
 * <p/>For variables, the following information is gathered:
 *
 * <ol>
 * <li>A count of the number of times the variable occurs in the clause (singleton detection).</li>
 * <li>A flag indicating that variable only ever appears in non-argument positions.</li>
 * <li>The last functor body in the clause in which a variable appears, provided it only does so in argument
 * position.</li>
 * </ol>
 *
 * <p/>For constants, the following information is gathered:
 *
 * <ol>
 * <li>A flag indicating the constant only ever appears in non-argument positions.</li>
 * </ol>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Count variable occurrences in a clause. </td></tr>
 * <tr><td> Detect variables and constants only appearing in non-argument positions. </td></tr>
 * <tr><td> Identify the last functor in which a variable appears, if it only does as an argument. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class PositionAndOccurrenceVisitor extends BasePositionalVisitor
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(PositionAndOccurrenceVisitor.class.getName()); */

    /** Holds the current top-level body functor. <tt>null</tt> when traversing the head. */
    private Functor topLevelBodyFunctor;

    /** Holds a set of all constants encountered. */
    private final Map<Integer, List<SymbolKey>> constants = new HashMap<Integer, List<SymbolKey>>();

    /** Holds a set of all constants found to be in argument positions. */
    private final Collection<Integer> argumentConstants = new HashSet<Integer>();

    /**
     * Creates a positional visitor.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     */
    public PositionAndOccurrenceVisitor(VariableAndFunctorInterner interner,
        SymbolTable<Integer, String, Object> symbolTable, PositionalTermTraverser traverser)
    {
        super(interner, symbolTable, traverser);
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Counts variable occurrences and detects if the variable ever appears in an argument position.
     */
    protected void enterVariable(Variable variable)
    {
        // Initialize the count to one or add one to an existing count.
        Integer count = (Integer) symbolTable.get(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT);
        count = (count == null) ? 1 : (count + 1);
        symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_OCCURRENCE_COUNT, count);

        /*log.fine("Variable " + variable + " has count " + count + ".");*/

        // Get the nonArgPosition flag, or initialize it to true.
        Boolean nonArgPositionOnly =
            (Boolean) symbolTable.get(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_NON_ARG);
        nonArgPositionOnly = (nonArgPositionOnly == null) ? true : nonArgPositionOnly;

        // Clear the nonArgPosition flag if the variable occurs in an argument position.
        nonArgPositionOnly = inTopLevelFunctor(traverser) ? false : nonArgPositionOnly;
        symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_NON_ARG, nonArgPositionOnly);

        /*log.fine("Variable " + variable + " nonArgPosition is " + nonArgPositionOnly + ".");*/

        // If in an argument position, record the parent body functor against the variable, as potentially being
        // the last one it occurs in, in a purely argument position.
        // If not in an argument position, clear any parent functor recorded against the variable, as this current
        // last position of occurrence is not purely in argument position.
        if (inTopLevelFunctor(traverser))
        {
            symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_LAST_ARG_FUNCTOR, topLevelBodyFunctor);
        }
        else
        {
            symbolTable.put(variable.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_LAST_ARG_FUNCTOR, null);
        }
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Checks if a constant ever appears in an argument position.
     *
     * <p/>Sets the 'inTopLevelFunctor' flag, whenever the traversal is directly within a top-level functors arguments.
     * This is set at the end, so that subsequent calls to this will pick up the state of this flag at the point
     * immediately below a top-level functor.
     */
    protected void enterFunctor(Functor functor)
    {
        /*log.fine("Functor: " + functor.getName() + " <- " + symbolTable.getSymbolKey(functor.getName()));*/

        // Only check position of occurrence for constants.
        if (functor.getArity() == 0)
        {
            // Add the constant to the set of all constants encountered.
            List<SymbolKey> constantSymKeys = constants.get(functor.getName());

            if (constantSymKeys == null)
            {
                constantSymKeys = new LinkedList<SymbolKey>();
                constants.put(functor.getName(), constantSymKeys);
            }

            constantSymKeys.add(functor.getSymbolKey());

            // If the constant ever appears in argument position, take note of this.
            if (inTopLevelFunctor(traverser))
            {
                argumentConstants.add(functor.getName());
            }
        }

        // Keep track of the current top-level body functor.
        if (isTopLevel(traverser) && !traverser.isInHead())
        {
            topLevelBodyFunctor = functor;
        }
    }

    /**
     * Upon leaving the clause, sets the nonArgPosition flag on any constants that need it.
     *
     * @param clause The clause being left.
     */
    protected void leaveClause(Clause clause)
    {
        // Remove the set of constants appearing in argument positions, from the set of all constants, to derive
        // the set of constants that appear in non-argument positions only.
        constants.keySet().removeAll(argumentConstants);

        // Set the nonArgPosition flag on all symbol keys for all constants that only appear in non-arg positions.
        for (List<SymbolKey> symbolKeys : constants.values())
        {
            for (SymbolKey symbolKey : symbolKeys)
            {
                symbolTable.put(symbolKey, SymbolTableKeys.SYMKEY_FUNCTOR_NON_ARG, true);
            }
        }
    }

    /**
     * Checks if the current position is immediately within a top-level functor.
     *
     * @param  context The position context to examine.
     *
     * @return <tt>true</tt> iff the current position is immediately within a top-level functor.
     */
    private boolean inTopLevelFunctor(PositionalContext context)
    {
        PositionalContext parentContext = context.getParentContext();

        return parentContext.isTopLevel() || isTopLevel(parentContext);
    }

    /**
     * Functors are considered top-level when they appear at the top-level within a clause, or directly beneath a parent
     * conjunction or disjunction that is considered to be top-level.
     *
     * @param  context The position context to examine.
     *
     * @return <tt>true</tt> iff the current position is a top-level functor.
     */
    private boolean isTopLevel(PositionalContext context)
    {
        Term term = context.getTerm();

        if (term.getSymbolKey() == null)
        {
            return false;
        }

        Boolean isTopLevel = (Boolean) symbolTable.get(term.getSymbolKey(), SymbolTableKeys.SYMKEY_TOP_LEVEL_FUNCTOR);

        return (isTopLevel == null) ? false : isTopLevel;
    }
}
