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
import java.util.LinkedList;
import java.util.List;

import com.thesett.aima.logic.fol.wam.machine.WAMMemoryLayout;
import com.thesett.common.util.event.EventListenerSupport;

/**
 * InternalMemoryLayoutBean represents {@link WAMMemoryLayout} as a Java bean, and adds the ability to attach a
 * {@link PropertyChangeListener} to it. The change listener will be notified when any registers or flags change value.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide change notification on registers and flags describing the memory layout.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class InternalMemoryLayoutBean extends WAMMemoryLayout implements RegisterModel
{
    /** Defines the names of the machine registers. */
    private static final String[] REGISTER_NAMES =
        new String[]
        {
            "regBase", "regSize", "heapBase", "heapSize", "stackBase", "stackSize", "trailBase", "trailSize", "pdlBase",
            "pdlSize"
        };

    /** Defines the names of the machine flags. */
    public static final String[] FLAG_NAMES = new String[] {};

    /** Holds any property change listeners to notify of register value changes. */
    private final EventListenerSupport<PropertyChangeListener> listeners = new EventListenerSupport<PropertyChangeListener>();

    /**
     * Creates an instance of the WAM memory layout.
     *
     * @param regBase   The start of the register file within the machines data area.
     * @param regSize   The size of the register file.
     * @param heapBase  The start of the within the machines data area.
     * @param heapSize  The size of the heap.
     * @param stackBase the start of the within the machines data area.
     * @param stackSize the size of the stack.
     * @param trailBase the start of the within the machines data area.
     * @param trailSize the size of the trail.
     * @param pdlBase   The start of the within the machines data area.
     * @param pdlSize   The size of the pdl.
     */
    public InternalMemoryLayoutBean(int regBase, int regSize, int heapBase, int heapSize, int stackBase, int stackSize,
        int trailBase, int trailSize, int pdlBase, int pdlSize)
    {
        super(regBase, regSize, heapBase, heapSize, stackBase, stackSize, trailBase, trailSize, pdlBase, pdlSize);
    }

    /**
     * Establishes a PropertyChangeListener to notify of changes to the register file or flags.
     *
     * @param listener A PropertyChangeListener to notify of changes to the register file or flags.
     */
    public void addPropertyChangeListener(PropertyChangeListener listener)
    {
        listeners.addListener(listener);
    }

    /**
     * Updates the layout register file, with a new set of layout registers.
     *
     * @param layout The new layout register values.
     */
    public void updateRegisters(WAMMemoryLayout layout)
    {
        List<PropertyChangeEvent> changes = delta(this, layout);

        regBase = layout.regBase;
        regSize = layout.regSize;
        heapBase = layout.heapBase;
        heapSize = layout.heapSize;
        stackBase = layout.stackBase;
        stackSize = layout.stackSize;
        trailBase = layout.trailBase;
        trailSize = layout.trailSize;
        pdlBase = layout.pdlBase;
        pdlSize = layout.pdlSize;

        notifyChanges(changes);
    }

    /** Provides the start of the register file within the machines data area. */
    public int getRegBase()
    {
        return regBase;
    }

    /** Provides the size of the register file. */
    public int getRegSize()
    {
        return regSize;
    }

    /** Provides the start of the within the machines data area. */
    public int getHeapBase()
    {
        return heapBase;
    }

    /** Provides the size of the heap. */
    public int getHeapSize()
    {
        return heapSize;
    }

    /** Provides the start of the within the machines data area. */
    public int getStackBase()
    {
        return stackBase;
    }

    /** Provides the size of the stack. */
    public int getStackSize()
    {
        return stackSize;
    }

    /** Provides the start of the within the machines data area. */
    public int getTrailBase()
    {
        return trailBase;
    }

    /** Provides the size of the trail. */
    public int getTrailSize()
    {
        return trailSize;
    }

    /** Provides the start of the within the machines data area. */
    public int getPdlBase()
    {
        return pdlBase;
    }

    /** Provides the size of the pdl. */
    public int getPdlSize()
    {
        return pdlSize;
    }

    /** {@inheritDoc} */
    public String[] getRegisterNames()
    {
        return REGISTER_NAMES;
    }

    /** {@inheritDoc} */
    public String[] getFlagNames()
    {
        return FLAG_NAMES;
    }

    /** {@inheritDoc} */
    public int getRegisterSizeBytes(String name)
    {
        return 4;
    }

    /** {@inheritDoc} */
    public byte[] getRegister(String name)
    {
        return new byte[0];
    }

    /** {@inheritDoc} */
    public boolean getFlag(String name)
    {
        return false;
    }

    /**
     * Compares the current register file, with a new set, and creates a list property change notifications for any that
     * have changed value.
     *
     * @param newRegisters The new register values.
     */
    private List<PropertyChangeEvent> delta(WAMMemoryLayout oldRegisters, WAMMemoryLayout newRegisters)
    {
        List<PropertyChangeEvent> result = new LinkedList<PropertyChangeEvent>();

        if (oldRegisters.regBase != newRegisters.regBase)
        {
            result.add(new PropertyChangeEvent(this, "regBase", oldRegisters.regBase, newRegisters.regBase));
        }

        if (oldRegisters.regSize != newRegisters.regSize)
        {
            result.add(new PropertyChangeEvent(this, "regSize", oldRegisters.regSize, newRegisters.regSize));
        }

        if (oldRegisters.heapBase != newRegisters.heapBase)
        {
            result.add(new PropertyChangeEvent(this, "heapBase", oldRegisters.heapBase, newRegisters.heapBase));
        }

        if (oldRegisters.heapSize != newRegisters.heapSize)
        {
            result.add(new PropertyChangeEvent(this, "heapSize", oldRegisters.heapSize, newRegisters.heapSize));
        }

        if (oldRegisters.stackBase != newRegisters.stackBase)
        {
            result.add(new PropertyChangeEvent(this, "stackBase", oldRegisters.stackBase, newRegisters.stackBase));
        }

        if (oldRegisters.stackSize != newRegisters.stackSize)
        {
            result.add(new PropertyChangeEvent(this, "stackSize", oldRegisters.stackSize, newRegisters.stackSize));
        }

        if (oldRegisters.trailBase != newRegisters.trailBase)
        {
            result.add(new PropertyChangeEvent(this, "trailBase", oldRegisters.trailBase, newRegisters.trailBase));
        }

        if (oldRegisters.trailSize != newRegisters.trailSize)
        {
            result.add(new PropertyChangeEvent(this, "trailSize", oldRegisters.trailSize, newRegisters.trailSize));
        }

        if (oldRegisters.pdlBase != newRegisters.pdlBase)
        {
            result.add(new PropertyChangeEvent(this, "pdlBase", oldRegisters.pdlBase, newRegisters.pdlBase));
        }

        if (oldRegisters.pdlSize != newRegisters.pdlSize)
        {
            result.add(new PropertyChangeEvent(this, "pdlSize", oldRegisters.pdlSize, newRegisters.pdlSize));
        }

        return result;
    }

    /**
     * Fires off a list of property change events to any interested listeners.
     *
     * @param changes The list of property changes to fire off.
     */
    private void notifyChanges(Iterable<PropertyChangeEvent> changes)
    {
        List<PropertyChangeListener> activeListeners = listeners.getActiveListeners();

        if (activeListeners != null)
        {
            for (PropertyChangeListener listener : activeListeners)
            {
                for (PropertyChangeEvent event : changes)
                {
                    listener.propertyChange(event);
                }
            }
        }
    }
}
