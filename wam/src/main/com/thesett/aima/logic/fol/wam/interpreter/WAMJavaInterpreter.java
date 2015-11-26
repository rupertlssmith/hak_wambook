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
package com.thesett.aima.logic.fol.wam.interpreter;

import java.io.PrintStream;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.interpreter.InteractiveParser;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.interpreter.ResolutionInterpreter;
import com.thesett.aima.logic.fol.isoprologparser.SentenceParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.wam.compiler.InstructionCompiler;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiler;
import com.thesett.aima.logic.fol.wam.machine.WAMEngine;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingJavaMachine;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachine;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * PrologInterpreter builds an interactive resolving interpreter using the interpreted resolution engine
 * {@link WAMResolvingJavaMachine}.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Create an interpreter for WAM.
 *     <td> {@link SentenceParser}, {@link InstructionCompiler}, {@link WAMResolvingJavaMachine}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMJavaInterpreter
{
    /** Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(WAMJavaInterpreter.class.getName()); */

    /**
     * Creates the interpreter and launches its top-level run loop.
     *
     * @param args The command line arguments.
     */
    public static void main(String[] args)
    {
        try
        {
            SymbolTableImpl<Integer, String, Object> symbolTable = new SymbolTableImpl<Integer, String, Object>();

            WAMResolvingMachine machine = new WAMResolvingJavaMachine(symbolTable);

            Parser<Clause, Token> parser = new InteractiveParser(machine);

            LogicCompiler<Clause, WAMCompiledPredicate, WAMCompiledQuery> compiler =
                new WAMCompiler(symbolTable, machine);

            ResolutionEngine<Clause, WAMCompiledPredicate, WAMCompiledQuery> engine =
                new WAMEngine(parser, machine, compiler, machine);
            engine.reset();

            ResolutionInterpreter<WAMCompiledPredicate, WAMCompiledQuery> interpreter =
                new ResolutionInterpreter<WAMCompiledPredicate, WAMCompiledQuery>(engine);

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
