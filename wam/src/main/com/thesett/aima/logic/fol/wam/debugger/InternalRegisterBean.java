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

import com.thesett.aima.logic.fol.wam.machine.WAMInternalRegisters;
import com.thesett.common.util.event.EventListenerSupport;

/**
 * InternalRegisterBean represents WAMInternalRegisters as a Java bean, and adds the ability to attach a
 * {@link PropertyChangeListener} to it. The change listener will be notified when registers or flags change value.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide change notification on registers and flags.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class InternalRegisterBean extends WAMInternalRegisters implements RegisterModel
{
    /** Defines the names of the machine registers. */
    public static final String[] REGISTER_NAMES =
        new String[] { "ip", "hp", "hbp", "sp", "up", "ep", "bp", "b0", "trp" };

    /** Defines the names of the machine flags. */
    public static final String[] FLAG_NAMES = new String[] { "writeMode" };

    /** Holds any property change listeners to notify of register value changes. */
    private EventListenerSupport<PropertyChangeListener> listeners = new EventListenerSupport<PropertyChangeListener>();

    /**
     * Creates an instance of the WAM machine internal register file and flags.
     *
     * @param ip        The current instruction pointer into the code.
     * @param hp        The heap pointer.
     * @param hbp       The top of heap at the latest choice point.
     * @param sp        The secondary heap pointer, used for the heap address of the next term to match.
     * @param up        The unification stack pointer.
     * @param ep        The environment base pointer.
     * @param bp        The choice point base pointer.
     * @param b0        The last call choice point pointer.
     * @param trp       The trail pointer.
     * @param writeMode The write mode flag.
     */
    public InternalRegisterBean(int ip, int hp, int hbp, int sp, int up, int ep, int bp, int b0, int trp,
        boolean writeMode)
    {
        super(ip, hp, hbp, sp, up, ep, bp, b0, trp, writeMode);
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
     * Updates the register file, with a new set of registers.
     *
     * @param registers The new register values.
     */
    public void updateRegisters(WAMInternalRegisters registers)
    {
        List<PropertyChangeEvent> changes = delta(this, registers);

        ip = registers.ip;
        hp = registers.hp;
        hbp = registers.hbp;
        sp = registers.sp;
        up = registers.up;
        ep = registers.ep;
        bp = registers.bp;
        b0 = registers.b0;
        trp = registers.trp;
        writeMode = registers.writeMode;

        notifyChanges(changes);
    }

    /** Provides the current instruction pointer into the code. */
    public int getIP()
    {
        return ip;
    }

    /** Provides the heap pointer. */
    public int getHP()
    {
        return hp;
    }

    /** Provides the top of heap at the latest choice point. */
    public int getHBP()
    {
        return hbp;
    }

    /** Provides the secondary heap pointer, used for the heap address of the next term to match. */
    public int getSP()
    {
        return sp;
    }

    /** Provides the unification stack pointer. */
    public int getUp()
    {
        return up;
    }

    /** Provides the environment base pointer. */
    public int getEP()
    {
        return ep;
    }

    /** Provides the choice point base pointer. */
    public int getBP()
    {
        return bp;
    }

    /** Provides the last call choice point pointer. */
    public int getB0()
    {
        return b0;
    }

    /** Provides the trail pointer. */
    public int getTRP()
    {
        return trp;
    }

    /** Provides the write mode flag. */
    public boolean getWriteMode()
    {
        return writeMode;
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
    private List<PropertyChangeEvent> delta(WAMInternalRegisters oldRegisters, WAMInternalRegisters newRegisters)
    {
        List<PropertyChangeEvent> result = new LinkedList<PropertyChangeEvent>();

        if (oldRegisters.ip != newRegisters.ip)
        {
            result.add(new PropertyChangeEvent(this, "IP", oldRegisters.ip, newRegisters.ip));
        }

        if (oldRegisters.hp != newRegisters.hp)
        {
            result.add(new PropertyChangeEvent(this, "HP", oldRegisters.hp, newRegisters.hp));
        }

        if (oldRegisters.hbp != newRegisters.hbp)
        {
            result.add(new PropertyChangeEvent(this, "HBP", oldRegisters.hbp, newRegisters.hbp));
        }

        if (oldRegisters.sp != newRegisters.sp)
        {
            result.add(new PropertyChangeEvent(this, "SP", oldRegisters.sp, newRegisters.sp));
        }

        if (oldRegisters.up != newRegisters.up)
        {
            result.add(new PropertyChangeEvent(this, "UP", oldRegisters.up, newRegisters.up));
        }

        if (oldRegisters.ep != newRegisters.ep)
        {
            result.add(new PropertyChangeEvent(this, "EP", oldRegisters.ep, newRegisters.ep));
        }

        if (oldRegisters.bp != newRegisters.bp)
        {
            result.add(new PropertyChangeEvent(this, "BP", oldRegisters.bp, newRegisters.bp));
        }

        if (oldRegisters.b0 != newRegisters.b0)
        {
            result.add(new PropertyChangeEvent(this, "B0", oldRegisters.b0, newRegisters.b0));
        }

        if (oldRegisters.trp != newRegisters.trp)
        {
            result.add(new PropertyChangeEvent(this, "TRP", oldRegisters.trp, newRegisters.trp));
        }

        if (oldRegisters.writeMode != newRegisters.writeMode)
        {
            result.add(new PropertyChangeEvent(this, "writeMode", oldRegisters.writeMode, newRegisters.writeMode));
        }

        return result;
    }

    /**
     * Fires off a list of property change events to any interested listeners.
     *
     * @param changes The list of property changes to fire off.
     */
    private void notifyChanges(List<PropertyChangeEvent> changes)
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
