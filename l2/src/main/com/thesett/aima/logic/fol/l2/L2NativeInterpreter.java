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

import java.io.PrintStream;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.interpreter.ResolutionInterpreter;
import com.thesett.aima.logic.fol.isoprologparser.SentenceParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * PrologInterpreter builds an interactive resolving interpreter using the interpreted resolution engine
 * {@link L2ResolvingNativeMachine}.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Create an interpreter for L2.
 *     <td> {@link SentenceParser}, {@link L2Compiler}, {@link L2ResolvingNativeMachine}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L2NativeInterpreter
{
    /** Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(L2NativeInterpreter.class.getName()); */

    /**
     * Creates the interpreter and launches its top-level run loop.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args)
    {
        try
        {
            final L2ResolvingMachine machine = new L2ResolvingNativeMachine();

            Parser<Clause, Token> parser = new SentenceParser(machine);
            parser.setTokenSource(TokenSource.getTokenSourceForInputStream(System.in));

            LogicCompiler<Clause, L2CompiledClause, L2CompiledClause> compiler =
                new L2Compiler(new SymbolTableImpl<Integer, String, Object>(), machine);

            ResolutionEngine<Clause, L2CompiledClause, L2CompiledClause> engine =
                new ResolutionEngine<Clause, L2CompiledClause, L2CompiledClause>(parser, machine, compiler, machine)
                {
                    public void reset()
                    {
                        machine.reset();
                    }
                };

            ResolutionInterpreter<L2CompiledClause, L2CompiledClause> interpreter =
                new ResolutionInterpreter<L2CompiledClause, L2CompiledClause>(engine);

            interpreter.interpreterLoop();
        }
        catch (Exception e)
        {
            /*log.log(java.util.logging.Level.SEVERE, e.getMessage(), e);*/
            e.printStackTrace(new PrintStream(System.err));
            System.exit(-1);
        }
    }
}
