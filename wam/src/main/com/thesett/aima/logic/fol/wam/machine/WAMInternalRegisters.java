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
package com.thesett.aima.logic.fol.wam.machine;

/**
 * WAMInternalRegisters holds the register file and flags of the machines internal registers.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Encapsulate the WAM machine internal register file and flags.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMInternalRegisters
{
    /** Holds the current instruction pointer into the code. */
    public int ip;

    /** Holds the heap pointer. */
    public int hp;

    /** Holds the top of heap at the latest choice point. */
    public int hbp;

    /** Holds the secondary heap pointer, used for the heap address of the next term to match. */
    public int sp;

    /** Holds the unification stack pointer. */
    public int up;

    /** Holds the environment base pointer. */
    public int ep;

    /** Holds the choice point base pointer. */
    public int bp;

    /** Holds the last call choice point pointer. */
    public int b0;

    /** Holds the trail pointer. */
    public int trp;

    /** Used to record whether the machine is in structure read or write mode. */
    public boolean writeMode;

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
    public WAMInternalRegisters(int ip, int hp, int hbp, int sp, int up, int ep, int bp, int b0, int trp,
        boolean writeMode)
    {
        this.ip = ip;
        this.hp = hp;
        this.hbp = hbp;
        this.sp = sp;
        this.up = up;
        this.ep = ep;
        this.bp = bp;
        this.b0 = b0;
        this.trp = trp;
        this.writeMode = writeMode;
    }
}
