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
package com.thesett.aima.logic.fol.wam.debugger;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.nio.ByteBuffer;

import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachineDPI;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachineDPIMonitor;
import com.thesett.common.util.SizeableList;
import com.thesett.text.api.model.TextTableModel;
import com.thesett.text.impl.model.TextTableImpl;

/**
 * SimpleMonitor is a simple implementation of {@link WAMResolvingMachineDPIMonitor} that dumps all events on the target
 * machine to the standard out.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Dump reset state to standard out. </td><td> {@link WAMResolvingMachineDPI} </td></tr>
 * <tr><td> Dump stepped state to standard out. </td><td> {@link WAMResolvingMachineDPI} </td></tr>
 * <tr><td> Dump code execution state to standard out. </td><td> {@link WAMResolvingMachineDPI} </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class SimpleMonitor implements WAMResolvingMachineDPIMonitor, PropertyChangeListener
{
    /** Holds a copy of the memory layout registers and monitors them for changes. */
    InternalMemoryLayoutBean layoutRegisters;

    /** Holds a copy of the internal registers and monitors them for changes. */
    InternalRegisterBean internalRegisters;

    /** Holds a printing table to render output to. */
    TextTableModel tableModel = new TextTableImpl();

    /** {@inheritDoc} */
    public void onReset(WAMResolvingMachineDPI dpi)
    {
        System.out.println("reset");
        tableModel = new TextTableImpl();

        layoutRegisters = new InternalMemoryLayoutBean(0, 0, 0, 0, 0, 0, 0, 0, 0, 0);
        layoutRegisters.addPropertyChangeListener(this);
        layoutRegisters.updateRegisters(layoutRegisters);

        internalRegisters = new InternalRegisterBean(0, 0, 0, 0, 0, 0, 0, 0, 0, false);
        internalRegisters.addPropertyChangeListener(this);
        internalRegisters.updateRegisters(dpi.getInternalRegisters());
    }

    /** {@inheritDoc} */
    public void onCodeUpdate(WAMResolvingMachineDPI dpi, int start, int length)
    {
        System.out.println("Code updated, " + length + " bytes at " + start + ".");
        tableModel = new TextTableImpl();

        ByteBuffer code = dpi.getCodeBuffer(start, length);

        SizeableList<WAMInstruction> instructions =
            WAMInstruction.disassemble(start, length, code, dpi.getVariableAndFunctorInterner(), dpi);

        int row = 0;

        for (WAMInstruction instruction : instructions)
        {
            if (instruction.getLabel() != null)
            {
                tableModel.put(0, row, instruction.getLabel().toPrettyString());
            }

            tableModel.put(1, row, instruction.toString());
            row++;
        }

        printTable(tableModel);
    }

    /** {@inheritDoc} */
    public void onExecute(WAMResolvingMachineDPI dpi)
    {
        /*System.out.println("execute");*/
        layoutRegisters.updateRegisters(dpi.getMemoryLayout());
        internalRegisters.updateRegisters(dpi.getInternalRegisters());
    }

    /** {@inheritDoc} */
    public void onStep(WAMResolvingMachineDPI dpi)
    {
        /*System.out.println("step");*/
        layoutRegisters.updateRegisters(dpi.getMemoryLayout());
        internalRegisters.updateRegisters(dpi.getInternalRegisters());
    }

    /** {@inheritDoc} */
    public void propertyChange(PropertyChangeEvent evt)
    {
        /*System.out.println(evt.getPropertyName() + ", " + evt.getNewValue());*/
    }

    /** Renders the table. */
    protected void printTable(TextTableModel printTable)
    {
        for (int i = 0; i < printTable.getRowCount(); i++)
        {
            StringBuffer result = new StringBuffer();

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
            System.out.print(result);
        }
    }
}
