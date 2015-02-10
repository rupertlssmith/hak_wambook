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
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.PositionalTermVisitor;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.wam.builtins.BuiltInFunctor;
import com.thesett.aima.logic.fol.wam.builtins.BuiltInTransform;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * BuiltInTransformVisitor should be used with a depth first positional walk over a term to compile. On leaving each
 * term, that is in a post-fix order, if the term is a functor, the built-in transformation function is applied to it.
 * If the built-in applies a transformation to a functor, it is substituted within its parent for the built-in.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Substitute built-ins into a term to compile.</td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
class BuiltInTransformVisitor extends BasePositionalVisitor implements PositionalTermVisitor
{
    /** Used for debugging. */
    private static final java.util.logging.Logger log =
        java.util.logging.Logger.getLogger(BuiltInTransformVisitor.class.getName());

    /** Holds the built in transformation function. */
    private final BuiltInTransform builtInTransform;

    /**
     * Creates the visitor with the supplied interner, symbol table and traverser.
     *
     * @param interner         The name interner.
     * @param symbolTable      The compiler symbol table.
     * @param traverser        The positional context traverser.
     * @param builtInTransform The built-in transformation function.
     */
    BuiltInTransformVisitor(VariableAndFunctorInterner interner, SymbolTable<Integer, String, Object> symbolTable,
        PositionalTermTraverser traverser, BuiltInTransform builtInTransform)
    {
        super(interner, symbolTable, traverser);

        this.builtInTransform = builtInTransform;
    }

    /** {@inheritDoc} */
    public void setPositionalTraverser(PositionalTermTraverser traverser)
    {
        this.traverser = traverser;
    }

    /**
     * Applies the built-in transform during a post-fix visit of a term.
     *
     * @param functor The functor to visit.
     */
    protected void leaveFunctor(Functor functor)
    {
        int pos = traverser.getPosition();

        if (!traverser.isInHead() && (pos >= 0))
        {
            Functor transformed = builtInTransform.apply(functor);

            if (functor != transformed)
            {
                log.fine("Transformed: " + functor + " to " + transformed.getClass());

                BuiltInFunctor builtInFunctor = (BuiltInFunctor) transformed;

                Term parentTerm = traverser.getParentContext().getTerm();

                if (parentTerm instanceof Clause)
                {
                    Clause parentClause = (Clause) parentTerm;

                    parentClause.getBody()[pos] = builtInFunctor;

                }
                else if (parentTerm instanceof Functor)
                {
                    Functor parentFunctor = (Functor) parentTerm;

                    parentFunctor.getArguments()[pos] = builtInFunctor;
                }
            }
        }
    }
}
