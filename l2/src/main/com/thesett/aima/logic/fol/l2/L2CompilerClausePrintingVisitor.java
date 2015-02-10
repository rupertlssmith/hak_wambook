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
package com.thesett.aima.logic.fol.l2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.thesett.aima.logic.fol.AllTermsVisitor;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.DelegatingAllTermsVisitor;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.common.util.doublemaps.DoubleKeyedMap;
import com.thesett.common.util.doublemaps.HashMapXY;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * L2CompilerClausePritingVisitor pretty prints clauses visited by the {@link L2Compiler}.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Pretty print clauses with internal information about the compilation.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L2CompilerClausePrintingVisitor extends DelegatingAllTermsVisitor implements PrintingTable
{
    /** The positional traverser used to traverse the clause being printed. */
    private PositionalTermTraverser traverser;

    /** The name interner. */
    private VariableAndFunctorInterner interner;

    /** The symbol table. */
    private SymbolTable<Integer, String, Object> symbolTable;

    /** Holds the string buffer to pretty print the results into. */
    private StringBuffer result;

    /** Holds a table with cell data to pretty print. */
    DoubleKeyedMap<Long, Long, String> grid = new HashMapXY<String>(10);

    /** Holds the maximum row sizes in the grid. */
    Map<Integer, Integer> maxRowSizes = new HashMap<Integer, Integer>();

    /** Holds the maximum column sizes in the grid. */
    Map<Integer, Integer> maxColumnSizes = new HashMap<Integer, Integer>();

    /** Holds a list of all column printers to apply. */
    List<AllTermsVisitor> printers;

    /** Used to count the maximum row with data in it. */
    int maxRows;

    /** Used to count the maximum column with data in it. */
    int maxColumns;

    /**
     * Creates a pretty printing visitor for clauses being compiled in L2.
     *
     * @param interner    The symbol name table.
     * @param symbolTable The symbol table for the compilation.
     * @param result      A string buffer to place the results in.
     */
    public L2CompilerClausePrintingVisitor(VariableAndFunctorInterner interner,
        SymbolTable<Integer, String, Object> symbolTable, StringBuffer result)
    {
        super(null);
        this.interner = interner;
        this.result = result;
        this.symbolTable = symbolTable;

    }

    /**
     * Sets up the symbol key traverser used to traverse the clause being printed, and providing a positional context as
     * it does so.
     *
     * @param traverser The symbol key traverser traverser used to traverse the clause being printed.
     */
    public void setPositionalTraverser(PositionalTermTraverser traverser)
    {
        this.traverser = traverser;
    }

    /** {@inheritDoc} */
    public void visit(Clause clause)
    {
        if (traverser.isEnteringContext())
        {
            printers = new ArrayList<AllTermsVisitor>();
            printers.add(new SourceClausePrinter(interner, symbolTable, traverser, maxColumns++, grid, this));
            printers.add(new PositionPrinter(interner, symbolTable, traverser, maxColumns++, grid, this));
        }
        else if (traverser.isLeavingContext())
        {
            for (int i = 0; i < maxRows; i++)
            {
                for (int j = 0; j < maxColumns; j++)
                {
                    String valueToPrint = grid.get((long) j, (long) i);
                    valueToPrint = (valueToPrint == null) ? "" : valueToPrint;
                    result.append(valueToPrint);

                    int padding = maxColumnSizes.get(j) - valueToPrint.length();
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

    /**
     * Updates the maximum row count of the data table.
     *
     * @param row The maximum row count reached.
     */
    public void setMaxRowCount(int row)
    {
        if (maxRows < row)
        {
            maxRows = row;
        }
    }

    /**
     * Updates the maximum row height for a row of the data table.
     *
     * @param row    The row to update.
     * @param height The max height reached.
     */
    public void setMaxRowHeight(int row, int height)
    {
        Integer previousValue = maxRowSizes.get(row);

        if (previousValue == null)
        {
            maxRowSizes.put(row, height);
        }
        else if (previousValue < height)
        {
            maxRowSizes.put(row, height);
        }
    }

    /**
     * Updates the maximum column width for a column of the data table.
     *
     * @param column The column to update.
     * @param width  The max width reached.
     */
    public void setMaxColumnWidth(int column, int width)
    {
        Integer previousValue = maxColumnSizes.get(column);

        if (previousValue == null)
        {
            maxColumnSizes.put(column, width);
        }
        else if (previousValue < width)
        {
            maxColumnSizes.put(column, width);
        }
    }
}
