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

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Predicate;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.common.util.doublemaps.DoubleKeyedMap;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * LabelPrinter prints labels for any bytecode instructions that are labelled.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Print any labels on the bytecode instructions.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class LabelPrinter extends BasePrinter
{
    /**
     * Creates a printer.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     * @param column      The column to print to.
     * @param grid        The grid to print to.
     * @param table       The table to inform of cell sizes and positions.
     */
    public LabelPrinter(VariableAndFunctorInterner interner, SymbolTable<Integer, String, Object> symbolTable,
        PositionalTermTraverser traverser, int column, DoubleKeyedMap<Long, Long, String> grid, PrintingTable table)
    {
        super(interner, symbolTable, traverser, column, grid, table);
    }

    /** {@inheritDoc} */
    protected void enterClause(Clause clause)
    {
        if (clause instanceof L3CompiledQuery)
        {
            L3CompiledQuery query = (L3CompiledQuery) clause;

            for (L3Instruction instruction : query.getInstructions())
            {
                L3Label label = instruction.getLabel();
                addLineToRow((label != null) ? (label.toPrettyString() + ":") : "");
                nextRow();
            }
        }
    }

    /** {@inheritDoc} */
    protected void enterPredicate(Predicate predicate)
    {
        if (predicate instanceof L3CompiledPredicate)
        {
            L3CompiledPredicate compiledPredicate = (L3CompiledPredicate) predicate;

            for (L3Instruction instruction : compiledPredicate.getInstructions())
            {
                L3Label label = instruction.getLabel();
                addLineToRow((label != null) ? (label.toPrettyString() + ":") : "");
                nextRow();
            }
        }
    }
}
