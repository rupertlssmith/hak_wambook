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
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * Pretty printer for compiled queries.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Pretty print compiled queries with internal information about the compilation.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMCompiledQueryPrintingVisitor extends WAMCompiledTermsPrintingVisitor
{
    /**
     * Creates a pretty printing visitor for clauses being compiled in WAM.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public WAMCompiledQueryPrintingVisitor(VariableAndFunctorInterner interner,
        SymbolTable<Integer, String, Object> symbolTable, StringBuffer result)
    {
        super(interner, symbolTable, result);
    }

    /** {@inheritDoc} */
    public void visit(Clause clause)
    {
        if (traverser.isEnteringContext())
        {
            initializePrinters();
        }
        else if (traverser.isLeavingContext())
        {
            printTable();
        }

        super.visit(clause);
    }
}
