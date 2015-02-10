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
package com.thesett.aima.logic.fol.wam.nativemachine;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.NDC;

import com.thesett.aima.logic.fol.BacktrackingResolverUnitTestBase;
import com.thesett.aima.logic.fol.BasicResolverUnitTestBase;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.ConjunctionResolverUnitTestBase;
import com.thesett.aima.logic.fol.DisjunctionResolverUnitTestBase;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiler;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingJavaMachineTest;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachine;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * WAMResolvingJavaMachineTest tests resolution and unification over a range of terms in first order logic, in order to
 * test all success and failure paths, through an WAM byte code machine. The WAM machine handles resolution without
 * backtracking, in addition to full unification without the occurs check.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Run all basic resolution tests. <td> {@link BasicResolverUnitTestBase}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMResolvingNativeMachineTestBase extends TestCase
{
    /** Holds the WAM machine to run the tests through. */
    private static WAMResolvingMachine machine;

    /** Used for debugging. */
    java.util.logging.Logger log = java.util.logging.Logger.getLogger(WAMResolvingJavaMachineTest.class.getName());

    /**
     * Creates a test with the specified name.
     *
     * @param name The name of the test.
     */
    public WAMResolvingNativeMachineTestBase(String name)
    {
        super(name);
    }

    /**
     * Compile all the tests for the default tests for unifiers into a suite, plus the tests defined in this class.
     *
     * @return A test suite.
     */
    public static Test suite() throws Exception
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("WAMUnifyingNativeMachine Tests");

        SymbolTableImpl<Integer, String, Object> symbolTable = new SymbolTableImpl<Integer, String, Object>();

        machine = WAMResolvingNativeMachine.getInstance(symbolTable);

        LogicCompiler<Clause, WAMCompiledPredicate, WAMCompiledQuery> compiler = new WAMCompiler(symbolTable, machine);
        Parser<Clause, Token> parser = new ClauseParser(machine);

        ResolutionEngine<Clause, WAMCompiledPredicate, WAMCompiledQuery> engine =
            new ResolutionEngine<Clause, WAMCompiledPredicate, WAMCompiledQuery>(parser, machine, compiler, machine)
            {
                public void reset()
                {
                    machine.reset();
                }
            };

        // Add all tests defined in the BasicUnificationTestBase class
        /*suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testAtomsUnifyOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testNonMatchingAtomsFailUnify",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFreeLeftVarUnifiesAtomOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFreeLeftVarUnifiesFunctorOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFreeRightVarUnifiesAtomOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFreeRightVarUnifiesFunctorOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFreeVarUnifiesWithSameNameOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testFreeVarUnifiesWithDifferentNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testBoundVarPropagatesIntoFunctors",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testBoundVarUnifiesToSameVar",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundProgVarUnifiesToDifferentQueryVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testBoundQueryVarUnifiesToDifferentProgVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>("testFunctorsSameArityUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testFunctorsDifferentArityFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testFunctorsSameArityDifferentArgsFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate>(
                "testFunctorsDifferentNameSameArgsDoNotUnify", engine));*/

        // Add all tests defined in the BasicResolverUnitTestBase class
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAtomAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBinding", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAtomAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAtomAsArgumentToFunctorResolvesInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testResolutionFailsWhenNoMatchingFunctor", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testResolutionFailsWhenNoMatchingFunctorInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousIdentifiedVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousIdentifiedVariableBindingPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousVariableBindingNotPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testMultipleVariablesAreBoundOk", engine));

        // Add all tests defined in the ConjunctionResolverUnitTestBase class.
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionResolves", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionFailsToResolveWhenFirstPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionFailsToResolveWhenSecondPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionResolvesWhenFirstPathRevisitsSecond", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionResolvesWhenSecondPathRevisitsFirst", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindingFromQueryPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunctionAndFailsOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjoinedVariablesPropagateAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjoinedVariablesPropagateAccrossConjunctionFailingOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableInClauseMayTakeMultipleSimultaneousBindings", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testSuccesiveConjunctiveTermsOk", engine));

        // Add all tests defined in the DisjunctionResolverUnitTestBase class.
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testResolvesOnFirstMatchingPossibleFunctor", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testResolvesOnSecondMatchingPossibleFunctor", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFailsOnNoMatchingOutOfSeveralPossibleFunctors", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableTakesBindingsFromTwoDisjunctionPaths", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableTakesBindingsFromManyDisjunctionPaths", engine));

        // Add all tests defined in the BacktrackingResolverUnitTestBase class.
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testMultipleFactsProduceMultipleSolutions", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testInstantiatingClausesSeveralTimesWithSameVariableDoesNotConflictVariableBindings", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testInstantiatingClausesSeveralTimesWithDifferentVariableAllowsIndependentBindings", engine));

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
