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

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Sentence;

/**
 * L2 is a resolution language, over first order logic. Its sentences can be in one of two forms, a program sentence,
 * that puts a first order term into its term store, and a query sentence, that fetches terms from its term store and
 * unifies with and resolves them against the term store. The two sentence types are closely related, in that a program
 * sentence has a head and a body and its body is essentially the same thing as a query. A query unifies against a
 * matching program head, and if that program has a body, it is run as a query.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Create L2 programs and queries from clauses.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L2Sentence implements Sentence<Clause>
{
    /** Holds the clause that forms this sentence. */
    Clause clause;

    /**
     * Creates a program or query sentence in L2.
     *
     * @param clause The clause that forms this sentence.
     */
    public L2Sentence(Clause clause)
    {
        this.clause = clause;
    }

    /**
     * Gets the wrapped sentence in the logical language over clauses.
     *
     * @return The wrapped sentence in the logical language.
     */
    public Clause getT()
    {
        return clause;
    }

    /**
     * Outputs this sentence as a string, mainly for debugging purposes.
     *
     * @return This sentence as a string, mainly for debugging purposes.
     */
    public String toString()
    {
        return "L2Sentence: [ clause = " + clause + " ]";
    }
}
