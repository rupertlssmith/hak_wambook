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
package com.thesett.aima.logic.fol.wam.printer;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Predicate;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.aima.logic.fol.wam.compiler.WAMOptimizeableListing;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;

/**
 * ByteCodePrinter prints the compiled bytecode in its unoptimzed state.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Print the unoptimized compiled byte code.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class UnoptimizedByteCodePrinter extends BasePrinter
{
    /**
     * Creates a printer.
     *
     * @param interner    The name interner.
     * @param symbolTable The compiler symbol table.
     * @param traverser   The positional context traverser.
     * @param column      The column to print to.
     * @param table       The table to inform of cell sizes and positions.
     */
    public UnoptimizedByteCodePrinter(VariableAndFunctorInterner interner,
        SymbolTable<Integer, String, Object> symbolTable, PositionalTermTraverser traverser, int column,
        TextTableModel table)
    {
        super(interner, symbolTable, traverser, column, table);
    }

    /** {@inheritDoc} */
    protected void enterClause(Clause clause)
    {
        if (clause instanceof WAMCompiledQuery)
        {
            WAMOptimizeableListing query = (WAMCompiledQuery) clause;

            for (WAMInstruction instruction : query.getUnoptimizedInstructions())
            {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }

    /** {@inheritDoc} */
    protected void enterPredicate(Predicate predicate)
    {
        if (predicate instanceof WAMCompiledPredicate)
        {
            WAMOptimizeableListing compiledPredicate = (WAMCompiledPredicate) predicate;

            for (WAMInstruction instruction : compiledPredicate.getUnoptimizedInstructions())
            {
                addLineToRow(instruction.toString());
                nextRow();
            }
        }
    }
}
