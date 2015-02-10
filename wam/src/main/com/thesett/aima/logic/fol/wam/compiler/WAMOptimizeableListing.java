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
package com.thesett.aima.logic.fol.wam.compiler;

import java.util.List;

import com.thesett.common.util.SizeableList;

/**
 * WAMOptimizeableListing provides an instruction listing, and allows that instruction listing to be replaced with a
 * more optimized version.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> List the WAM instructions. </td></tr>
 * <tr><td> Allow the instructions to be replaced with a more optimized instruction listing. </td></tr>
 * <tr><td> List the original unoptimized instructions. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface WAMOptimizeableListing
{
    /**
     * Provides the compiled byte code instructions as an unmodifiable list.
     *
     * @return A list of the byte code instructions for this query.
     */
    List<WAMInstruction> getInstructions();

    /**
     * Replaces the instruction listing with a more optimized listing.
     *
     * @param instructions An optimized instruction listing.
     */
    void setOptimizedInstructions(SizeableList<WAMInstruction> instructions);

    /**
     * Provides the original unoptimized instruction listing, after the optimization replacement.
     *
     * @return The original unoptimized instruction listing.
     */
    List<WAMInstruction> getUnoptimizedInstructions();
}
