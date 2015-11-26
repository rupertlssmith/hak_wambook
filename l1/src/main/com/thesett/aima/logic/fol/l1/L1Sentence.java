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

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;

/**
 * L1 is a unification only language, over first order logic. Its sentences can be in one of two forms, a program
 * sentence, that puts a first order term into its term store, and a query sentence, that fetches terms from its term
 * store and unifies against them. If a succesfull unification is found, it is returned, otherwise the execution fails.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Create L1 query sentences from functors.
 * <tr><td> Create L1 program sentences from functors.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L1Sentence implements Sentence<Term>
{
    /** Holds the functor that makes up the program or query. */
    protected Functor expression;

    /**
     * Creates a declaration sentence in L1.
     *
     * @param  f The functor to declare.
     *
     * @return An L1 sentence for the declared term.
     */
    public static Sentence createProgram(Functor f)
    {
        return new L1Program(f);
    }

    /**
     * Creates a query sentence in L1.
     *
     * @param  f The functor to query with.
     *
     * @return An L1 sentence for the query.
     */
    public static Sentence createQuery(Functor f)
    {
        return new L1Query(f);
    }

    /**
     * Gets the functor that forms the programs head.
     *
     * @return The functor that forms the programs head.
     */
    public Functor getExpression()
    {
        return expression;
    }

    /**
     * Gets the wrapped sentence in the logical language over functors.
     *
     * @return The wrapped sentence in the logical language.
     */
    public Term getT()
    {
        return expression;
    }

    /**
     * Serves as a marker for L1 sentences that are programs.
     */
    public static class L1Program extends L1Sentence
    {
        /**
         * Creates an L1 program from a functor.
         *
         * @param f The functor to turn into an L1 program.
         */
        public L1Program(Functor f)
        {
            this.expression = f;
        }

        /**
         * Outputs this sentence as a string, mainly for debugging purposes.
         *
         * @return This sentence as a string, mainly for debugging purposes.
         */
        public String toString()
        {
            return "L1Program: [ expression = " + expression + " ]";
        }
    }

    /**
     * Serves as a marker for L1 sentences that are queries.
     */
    public static class L1Query extends L1Sentence
    {
        /**
         * Creates an L1 program from a functor.
         *
         * @param f The functor to turn into an L1 query.
         */
        public L1Query(Functor f)
        {
            this.expression = f;
        }

        /**
         * Outputs this sentence as a string, mainly for debugging purposes.
         *
         * @return This sentence as a string, mainly for debugging purposes.
         */
        public String toString()
        {
            return "L1Query: [ expression = " + expression + " ]";
        }
    }
}
