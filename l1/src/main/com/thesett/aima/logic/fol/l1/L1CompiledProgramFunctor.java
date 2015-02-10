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
package com.thesett.aima.logic.fol.l1;

import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * L1CompiledProgramFunctor is a {@link L1CompiledFunctor} for L1 progrmas.
 *
 * <p/>It provides a FIFO stack to order the instructions going forward for the decompilation algorithm in
 * {@link L1CompiledFunctor}.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide FIFO ordering for instruction scan.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L1CompiledProgramFunctor extends L1CompiledFunctor
{
    /**
     * Builds a compiled down L1 functor on a buffer of compiled code.
     *
     * @param machine        The L1 byte code machine that the functor has been compiled to.
     * @param callTableEntry The offset of the code buffer within its compiled to machine.
     * @param varNames       A mapping from register to variable names.
     */
    public L1CompiledProgramFunctor(L1Machine machine, L1CallTableEntry callTableEntry, Map<Byte, Integer> varNames)
    {
        super(machine, callTableEntry, varNames);
    }

    /**
     * Gets an instance of a queue implementation, to hold the functor and variable creating instruction start offsets
     * in. Programs use a FIFO queue to scan instructions forwards.
     *
     * @return A queue to hold the instruction start offsets in.
     */
    protected Queue<Integer> getInstructionQueue()
    {
        return new LinkedList<Integer>();
    }
}
