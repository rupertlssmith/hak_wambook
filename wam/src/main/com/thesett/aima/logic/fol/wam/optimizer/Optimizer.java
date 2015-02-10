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
package com.thesett.aima.logic.fol.wam.optimizer;

import com.thesett.aima.logic.fol.wam.compiler.WAMOptimizeableListing;

/**
 * An optimized is a function over {@link WAMOptimizeableListing} that produces an optimized version of the instruction
 * listing.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Apply optimizations to an instruction listing.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface Optimizer
{
    /**
     * Applies optimizations to an instruction listing.
     *
     * @param  listing The instruction listing.
     * @param  <T>     The type of the instruction listing.
     *
     * @return An optimized instruction listing.
     */
    public <T extends WAMOptimizeableListing> T apply(WAMOptimizeableListing listing);
}
