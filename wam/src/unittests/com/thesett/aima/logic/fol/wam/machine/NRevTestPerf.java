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

import java.util.Iterator;
import java.util.Set;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiler;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

public class NRevTestPerf
{
    public static final int NUM_ITERS = 1000;
    public static final int NUM_TEST_LOOPS = 20000;
    private final ResolutionEngine<Clause, WAMCompiledPredicate, WAMCompiledQuery> engine;
    private final Parser<Clause, Token> parser;

    public NRevTestPerf()
    {
        SymbolTableImpl<Integer, String, Object> symbolTable = new SymbolTableImpl<Integer, String, Object>();

        WAMResolvingJavaMachine machine = new WAMResolvingJavaMachine(symbolTable);

        LogicCompiler<Clause, WAMCompiledPredicate, WAMCompiledQuery> compiler = new WAMCompiler(symbolTable, machine);
        parser = new ClauseParser(machine);

        engine = new WAMEngine(parser, machine, compiler, machine);
    }

    public static void main(String[] args)
    {
        try
        {
            new NRevTestPerf().testNRev(NUM_ITERS, NUM_TEST_LOOPS);
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }

    /**
     * Runs the nrev tests.
     *
     * @param  numIters     The number of nrev iterations in each test loop.
     * @param  numTestLoops The number of test loops to run in total.
     *
     * @throws Exception If any of the test code fails to compile.
     */
    public void testNRev(int numIters, int numTestLoops) throws SourceCodeException
    {
        engine.reset();

        addClause("nrev([], [])");
        addClause("nrev([X|Rest], Ans) :- nrev(Rest, L), append(L, [X], Ans)");
        addClause("donrev(Rev) :- " + "nrev([a1, a2, a3, a4, a5, a6, a7, a8, a9, a10, a11, a12, a13, a14, a15, " +
            "a16, a17, a18, a19, a20, a21, a22, a23, a24, a25, a26, a27, a28, a29, a30], Rev)");
        addClause("nrevforever(_) :- donrev(_)");
        addClause("nrevforever(_) :- nrevforever(_)");

        engine.endScope();

        for (int j = 0; j < numTestLoops; j++)
        {
            long start = System.currentTimeMillis();

            setQuery("?- nrevforever(_).");

            Iterator<Set<Variable>> solutionIterator = engine.iterator();

            for (int i = 0; i < numIters; i++)
            {
                Set<Variable> nextSolution = solutionIterator.next();
            }

            long end = System.currentTimeMillis();
            long length = end - start;

            System.out.println(numIters + " iterations in " + length + " millis, which is " +
                ((numIters * 1000) / length) + " iterations/sec.");
        }
    }

    /**
     * Parses and sets the current query on the resolution engine.
     *
     * @param  queryString The query to set.
     *
     * @throws SourceCodeException If the query will not parse or compile.
     */
    private void setQuery(String queryString) throws SourceCodeException
    {
        engine.setTokenSource(TokenSource.getTokenSourceForString(queryString));

        engine.compile(engine.parse());
    }

    /**
     * Parses and adds a clause to the resolution engine.
     *
     * @param  termText The clause to add.
     *
     * @throws SourceCodeException If the clause will not parse or compile.
     */
    private void addClause(String termText) throws SourceCodeException
    {
        parser.setTokenSource(TokenSource.getTokenSourceForString(termText));

        Sentence<Clause> sentence = parser.parse();
        engine.compile(sentence);
    }
}
