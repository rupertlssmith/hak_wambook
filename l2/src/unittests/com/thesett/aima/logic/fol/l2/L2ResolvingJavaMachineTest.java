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

import org.apache.log4j.NDC;

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
 * L2ResolvingJavaMachineTest tests resolution and unification over a range of terms in first order logic, in order to
 * test all success and failure paths, through an L2 byte code machine. The L2 machine handles resolution without
 * backtracking, in addition to full unification without the occurs check.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Run all basic resolution tests. <td> {@link BasicResolverUnitTestBase}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L2ResolvingJavaMachineTest extends TestCase
{
    /** Holds the L2 machine to run the tests through. */
    private static L2ResolvingMachine machine;

    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L2ResolvingJavaMachineTest.class.getName()); */

    /**
     * Creates a test with the specified name.
     *
     * @param name The name of the test.
     */
    public L2ResolvingJavaMachineTest(String name)
    {
        super(name);
    }

    /**
     * Compile all the tests for the default tests for unifiers into a suite, plus the tests defined in this class.
     *
     * @return A test suite.
     */
    public static Test suite()
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("L2ResolvingJavaMachine Tests");

        machine = new L2ResolvingJavaMachine();

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

        // Add all tests defined in the BasicUnificationTestBase class
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>("testAtomsUnifyOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingAtomsFailUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeLeftVarUnifiesAtomOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeLeftVarUnifiesFunctorOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeRightVarUnifiesAtomOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeRightVarUnifiesFunctorOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeVarUnifiesWithSameNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFreeVarUnifiesWithDifferentNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarPropagatesIntoFunctors", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundVarUnifiesToSameVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundProgVarUnifiesToDifferentQueryVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testBoundQueryVarUnifiesToDifferentProgVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorsSameArityUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorsDifferentArityFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorsSameArityDifferentArgsFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorsDifferentNameSameArgsDoNotUnify", engine));

        // Add all tests defined in the BasicResolverUnitTestBase class
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAtomAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBinding", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAtomAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAtomAsArgumentToFunctorResolvesInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testResolutionFailsWhenNoMatchingFunctor", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testResolutionFailsWhenNoMatchingFunctorInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testFunctorAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAnonymousVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAnonymousIdentifiedVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAnonymousIdentifiedVariableBindingPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testAnonymousVariableBindingNotPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testMultipleVariablesAreBoundOk", engine));

        // Add all tests defined in the ConjunctionResolverUnitTestBase class.
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjunctionResolves", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjunctionFailsToResolveWhenFirstPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjunctionFailsToResolveWhenSecondPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjunctionResolvesWhenFirstPathRevisitsSecond", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjunctionResolvesWhenSecondPathRevisitsFirst", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableBindingFromQueryPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunctionAndFailsOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjoinedVariablesPropagateAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testConjoinedVariablesPropagateAccrossConjunctionFailingOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testVariableInClauseMayTakeMultipleSimultaneousBindings", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L2CompiledClause, L2CompiledClause>(
                "testSuccesiveConjunctiveTermsOk", engine));

        // Disable check for extra solutions on all tests. Termination conditions have not been implemented on L2.
        for (int i = 0; i < suite.testCount(); i++)
        {
            Test test = suite.testAt(i);

            if (test instanceof BasicResolverUnitTestBase)
            {
                ((BasicResolverUnitTestBase) test).withCheckExtraSolutions(false);
            }
        }
        // Add all the tests defined in this class.

        return suite;
    }

    protected void setUp()
    {
        NDC.push(getName());
    }

    protected void tearDown()
    {
        NDC.pop();
    }
}
