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

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.STACK_ADDR;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.text.api.model.TextTableModel;

/**
 * PositionPrinter prints some positional context information about functors and how they relate to their compiled form.
 * The position printer will prints the name of the functor, argument or variable and whether the current position is
 * within the functor head or last body element.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Print the positional context information.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class PositionPrinter extends BasePrinter
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
    public PositionPrinter(VariableAndFunctorInterner interner, SymbolTable<Integer, String, Object> symbolTable,
        PositionalTermTraverser traverser, int column, TextTableModel table)
    {
        super(interner, symbolTable, traverser, column, table);
    }

    /** {@inheritDoc} */
    protected void enterFunctor(Functor functor)
    {
        String head = traverser.isInHead() ? "/head" : "";
        String last = traverser.isLastBodyFunctor() ? "/last" : "";
        String symKey = functor.getSymbolKey().toString();

        if (traverser.isTopLevel())
        {
            addLineToRow("functor(" + symKey + ")" + head + last);
        }
        else
        {
            addLineToRow("arg(" + symKey + ")");
        }

        nextRow();
    }

    /** {@inheritDoc} */
    protected void leaveFunctor(Functor functor)
    {
        nextRow();
    }

    /** {@inheritDoc} */
    protected void enterVariable(Variable variable)
    {
        Integer allocation = (Integer) symbolTable.get(variable.getSymbolKey(), "allocation");
        String symKey = variable.getSymbolKey().toString();

        String allocString = "";

        if (allocation != null)
        {
            int slot = (allocation & (0xff));
            int mode = allocation >> 8;

            allocString = ((mode == STACK_ADDR) ? "Y" : "X") + slot;
        }

        addLineToRow("arg/var(" + symKey + ") " + allocString);
        nextRow();
    }
}
