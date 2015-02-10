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
 * WAMMemoryLayout holds an internal machine register file that describes the memory layout of the machine.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Encapsulate the WAM machine internal memory layout parameters.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMMemoryLayout
{
    /** Holds the start of the register file within the machines data area. */
    public int regBase;

    /** Holds the size of the register file. */
    public int regSize;

    /** Holds the start of the within the machines data area. */
    public int heapBase;

    /** Holds the size of the heap. */
    public int heapSize;

    /** Holds the start of the within the machines data area. */
    public int stackBase;

    /** Holds the size of the stack. */
    public int stackSize;

    /** Holds the start of the within the machines data area. */
    public int trailBase;

    /** Holds the size of the trail. */
    public int trailSize;

    /** Holds the start of the within the machines data area. */
    public int pdlBase;

    /** Holds the size of the pdl. */
    public int pdlSize;

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
    public WAMMemoryLayout(int regBase, int regSize, int heapBase, int heapSize, int stackBase, int stackSize,
        int trailBase, int trailSize, int pdlBase, int pdlSize)
    {
        this.regBase = regBase;
        this.regSize = regSize;
        this.heapBase = heapBase;
        this.heapSize = heapSize;
        this.stackBase = stackBase;
        this.stackSize = stackSize;
        this.trailBase = trailBase;
        this.trailSize = trailSize;
        this.pdlBase = pdlBase;
        this.pdlSize = pdlSize;
    }
}
