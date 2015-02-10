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

import java.util.ArrayList;
import java.util.List;

import com.thesett.aima.logic.fol.AllTermsVisitor;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.DelegatingAllTermsVisitor;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.PositionalTermVisitor;
import com.thesett.aima.logic.fol.Predicate;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.wam.compiler.InstructionCompiler;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;
import com.thesett.text.impl.model.TextTableImpl;

/**
 * WAMCompiledTermsPrintingVisitor assists with pretty printing queries and predicates compiled by the
 * {@link InstructionCompiler}.
 *
 * <p/>This class is abstract because it implements a default behaviour for all terms, which is to apply a stack of
 * column printers to them. Depending on the entry point of this pretty printer, the top-level term being printed should
 * have its visit method overridden to set up the stack of printers on entry, and to finalize the output on exit. The
 * {@link #initializePrinters()} and {@link #printTable()} methods are provided to do this.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Pretty print compiled terms internal information about the compilation.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class WAMCompiledTermsPrintingVisitor extends DelegatingAllTermsVisitor implements PositionalTermVisitor
{
    /** The positional traverser used to traverse the clause being printed. */
    protected PositionalTermTraverser traverser;

    /** The name interner. */
    private VariableAndFunctorInterner interner;

    /** The symbol table. */
    private SymbolTable<Integer, String, Object> symbolTable;

    /** Holds the string buffer to pretty print the results into. */
    private StringBuffer result;

    /** Holds a list of all column printers to apply. */
    List<AllTermsVisitor> printers = new ArrayList<AllTermsVisitor>();

    /** Holds the table model to render the output to. */
    TextTableModel printTable = new TextTableImpl();

    /**
     * Creates a pretty printing visitor for clauses being compiled in WAM.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public WAMCompiledTermsPrintingVisitor(VariableAndFunctorInterner interner,
        SymbolTable<Integer, String, Object> symbolTable, StringBuffer result)
    {
        super(null);
        this.interner = interner;
        this.result = result;
        this.symbolTable = symbolTable;

    }

    /** {@inheritDoc} */
    public void setPositionalTraverser(PositionalTermTraverser traverser)
    {
        this.traverser = traverser;
    }

    /** {@inheritDoc} */
    public void visit(Predicate predicate)
    {
        for (AllTermsVisitor printer : printers)
        {
            printer.visit(predicate);
        }

        super.visit(predicate);
    }

    /** {@inheritDoc} */
    public void visit(Clause clause)
    {
        for (AllTermsVisitor printer : printers)
        {
            printer.visit(clause);
        }

        super.visit(clause);
    }

    /** {@inheritDoc} */
    public void visit(Functor functor)
    {
        for (AllTermsVisitor printer : printers)
        {
            printer.visit(functor);
        }

        super.visit(functor);
    }

    /** {@inheritDoc} */
    public void visit(Variable variable)
    {
        for (AllTermsVisitor printer : printers)
        {
            printer.visit(variable);
        }

        super.visit(variable);
    }

    /** Sets up the stack of column printers. */
    protected void initializePrinters()
    {
        int maxColumns = 0;

        printers.add(new SourceClausePrinter(interner, symbolTable, traverser, maxColumns++, printTable));
        printers.add(new PositionPrinter(interner, symbolTable, traverser, maxColumns++, printTable));
        printers.add(new UnoptimizedLabelPrinter(interner, symbolTable, traverser, maxColumns++, printTable));
        printers.add(new UnoptimizedByteCodePrinter(interner, symbolTable, traverser, maxColumns++, printTable));
        printers.add(new LabelPrinter(interner, symbolTable, traverser, maxColumns++, printTable));
        printers.add(new ByteCodePrinter(interner, symbolTable, traverser, maxColumns++, printTable));
    }

    /**
     * Assembles the accumulated output in all rows and columns into a table. The table is appended onto {@link #result}
     */
    protected void printTable()
    {
        for (int i = 0; i < printTable.getRowCount(); i++)
        {
            for (int j = 0; j < printTable.getColumnCount(); j++)
            {
                String valueToPrint = printTable.get(j, i);
                valueToPrint = (valueToPrint == null) ? "" : valueToPrint;
                result.append(valueToPrint);

                Integer maxColumnSize = printTable.getMaxColumnSize(j);
                int padding = ((maxColumnSize == null) ? 0 : maxColumnSize) - valueToPrint.length();
                padding = (padding < 0) ? 0 : padding;

                for (int s = 0; s < padding; s++)
                {
                    result.append(" ");
                }

                result.append(" % ");
            }

            result.append("\n");
        }
    }
}
