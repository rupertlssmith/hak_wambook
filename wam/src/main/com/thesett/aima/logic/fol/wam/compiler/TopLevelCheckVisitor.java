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

import com.thesett.aima.logic.fol.BasePositionalVisitor;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.PositionalTermVisitor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalContext;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.wam.builtins.Conjunction;
import com.thesett.aima.logic.fol.wam.builtins.Disjunction;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * Conjunctions and disjunctions are treated specially by this transform. The conjunction and disjunction operators may
 * appear within any structure, but are only to be compiled as such if they are 'top-level'. They are considered
 * top-level when they appear at the top-level within a clause, or directly beneath a parent conjunction or disjunction
 * that is considered to be top-level. Effectively they are flattened into the top-level of the clause in which they
 * appear, but the original structure is preserved rather than actually flattened at this time, as it can change meaning
 * depending on how the term is bracketed.
 *
 * <p/>This traversal simply marks all conjunctions and disjunctions that are part of the clause top-level, with the
 * top-level flag. The functors appearing as arguments to those terms, are also marked as top-level, since they will be
 * evaluated by the clause, not treated as structure definitions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Check which functors are considered to be top-level within a clause. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
class TopLevelCheckVisitor extends BasePositionalVisitor implements PositionalTermVisitor
{
    /** Used for debugging. */
    private static final java.util.logging.Logger log =
        java.util.logging.Logger.getLogger(TopLevelCheckVisitor.class.getName());

    /**
     * Creates the visitor with the supplied interner, symbol table and traverser.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     */
    TopLevelCheckVisitor(VariableAndFunctorInterner interner, SymbolTable<Integer, String, Object> symbolTable,
        PositionalTermTraverser traverser)
    {
        super(interner, symbolTable, traverser);
    }

    /** {@inheritDoc} */
    public void setPositionalTraverser(PositionalTermTraverser traverser)
    {
        this.traverser = traverser;
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Sets the top-level flag on a functor, if appropriate.
     */
    protected void enterFunctor(Functor functor)
    {
        if (isTopLevel())
        {
            symbolTable.put(functor.getSymbolKey(), SymbolTableKeys.SYMKEY_TOP_LEVEL_FUNCTOR, true);
        }
    }

    /**
     * Functors are considered top-level when they appear at the top-level within a clause, or directly beneath a parent
     * conjunction or disjunction that is considered to be top-level.
     *
     * @return <tt>true</tt> iff the current position is considered to be top-level.
     */
    private boolean isTopLevel()
    {
        if (traverser.isTopLevel())
        {
            return true;
        }
        else
        {
            PositionalContext parentContext = traverser.getParentContext();

            if (parentContext != null)
            {
                Term parentTerm = parentContext.getTerm();

                if ((parentTerm instanceof Conjunction) || (parentTerm instanceof Disjunction))
                {
                    Boolean isTopLevel =
                        (Boolean) symbolTable.get(parentTerm.getSymbolKey(), SymbolTableKeys.SYMKEY_TOP_LEVEL_FUNCTOR);

                    return (isTopLevel == null) ? false : isTopLevel;
                }
            }
        }

        return false;
    }
}
