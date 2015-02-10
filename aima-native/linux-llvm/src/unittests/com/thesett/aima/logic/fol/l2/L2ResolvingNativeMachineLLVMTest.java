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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.thesett.aima.logic.fol.BasicResolverUnitTestBase;
import com.thesett.aima.logic.fol.BasicUnificationUnitTestBase;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.ConjunctionResolverUnitTestBase;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 */
public class L2ResolvingNativeMachineLLVMTest extends TestCase
{
    /** Holds the L2 machine to run the tests through. */
    private static L2ResolvingMachine machine;

    public L2ResolvingNativeMachineLLVMTest(String name)
    {
        super(name);
    }

    public static Test suite() throws Exception
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("L2UnifyingNativeMachine Tests");

        machine = L2ResolvingNativeMachine.getInstance();

        LogicCompiler<Clause, L2CompiledClause, L2CompiledClause> compiler =
            new L2Compiler(new SymbolTableImpl<Integer, String, Object>(), machine);
        Parser<Clause, Token> parser = new ClauseParser(machine);

        ResolutionEngine<Clause, L2CompiledClause, L2CompiledClause> engine =
            new ResolutionEngine<Clause, L2CompiledClause, L2CompiledClause>(parser, machine, compiler, machine)
            {
                public void reset()
                {
                    machine.reset();
                }
            };

        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAnonymousVariableBindingNotPropagatedAccrossCall", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testSuccesiveConjunctiveTermsOk", engine));

        return suite;
    }
}
