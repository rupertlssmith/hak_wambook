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
package com.thesett.aima.logic.fol.l3.nativemachine;

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
import com.thesett.aima.logic.fol.l3.L3CompiledPredicate;
import com.thesett.aima.logic.fol.l3.L3CompiledQuery;
import com.thesett.aima.logic.fol.l3.L3Compiler;
import com.thesett.aima.logic.fol.l3.L3ResolvingJavaMachineTest;
import com.thesett.aima.logic.fol.l3.L3ResolvingMachine;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * L3ResolvingJavaMachineTest tests resolution and unification over a range of terms in first order logic, in order to
 * test all success and failure paths, through an L3 byte code machine. The L3 machine handles resolution without
 * backtracking, in addition to full unification without the occurs check.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Run all basic resolution tests. <td> {@link BasicResolverUnitTestBase}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L3ResolvingNativeMachineTestBase extends TestCase
{
    /** Holds the L3 machine to run the tests through. */
    private static L3ResolvingMachine machine;

    /** Used for debugging. */
    java.util.logging.Logger log = java.util.logging.Logger.getLogger(L3ResolvingJavaMachineTest.class.getName());

    /**
     * Creates a test with the specified name.
     *
     * @param name The name of the test.
     */
    public L3ResolvingNativeMachineTestBase(String name)
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
        TestSuite suite = new TestSuite("L3UnifyingNativeMachine Tests");

        SymbolTableImpl<Integer, String, Object> symbolTable = new SymbolTableImpl<Integer, String, Object>();

        machine = L3ResolvingNativeMachine.getInstance(symbolTable);

        LogicCompiler<Clause, L3CompiledPredicate, L3CompiledQuery> compiler = new L3Compiler(symbolTable, machine);
        Parser<Clause, Token> parser = new ClauseParser(machine);

        ResolutionEngine<Clause, L3CompiledPredicate, L3CompiledQuery> engine =
            new ResolutionEngine<Clause, L3CompiledPredicate, L3CompiledQuery>(parser, machine, compiler, machine)
            {
                public void reset()
                {
                    machine.reset();
                }
            };

        // Add all tests defined in the BasicUnificationTestBase class
        /*suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testAtomsUnifyOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testNonMatchingAtomsFailUnify",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFreeLeftVarUnifiesAtomOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFreeLeftVarUnifiesFunctorOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFreeRightVarUnifiesAtomOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFreeRightVarUnifiesFunctorOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFreeVarUnifiesWithSameNameOk",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testFreeVarUnifiesWithDifferentNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testBoundVarPropagatesIntoFunctors",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testBoundVarUnifiesToSameVar",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundProgVarUnifiesToDifferentQueryVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testBoundQueryVarUnifiesToDifferentProgVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>("testFunctorsSameArityUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testFunctorsDifferentArityFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testFunctorsSameArityDifferentArgsFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, L3CompiledPredicate>(
                "testFunctorsDifferentNameSameArgsDoNotUnify", engine));*/

        // Add all tests defined in the BasicResolverUnitTestBase class
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAtomAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBinding", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAtomAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAtomAsArgumentToFunctorResolvesInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testNonMatchingAtomAsArgumentToFunctorFailsResolutionInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableAsArgumentToFunctorResolvesToCorrectBindingInTwoChainedCalls", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testResolutionFailsWhenNoMatchingFunctor", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testResolutionFailsWhenNoMatchingFunctorInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testFunctorAsArgumentToFunctorResolves", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolution", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testFunctorAsArgumentToFunctorResolvesInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testNonMatchingFunctorAsArgumentToFunctorFailsResolutionInChainedCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAnonymousVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAnonymousIdentifiedVariableBindingNotReported", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAnonymousIdentifiedVariableBindingPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testAnonymousVariableBindingNotPropagatedAccrossCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testMultipleVariablesAreBoundOk", engine));

        // Add all tests defined in the ConjunctionResolverUnitTestBase class.
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjunctionResolves", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjunctionFailsToResolveWhenFirstPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjunctionFailsToResolveWhenSecondPathFails", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjunctionResolvesWhenFirstPathRevisitsSecond", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjunctionResolvesWhenSecondPathRevisitsFirst", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableBindingFromQueryPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableBindingFromFirstPathPropagatesAccrossConjunctionAndFailsOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjoinedVariablesPropagateAccrossConjunction", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testConjoinedVariablesPropagateAccrossConjunctionFailingOnNonUnification", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableInClauseMayTakeMultipleSimultaneousBindings", engine));
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testSuccesiveConjunctiveTermsOk", engine));

        // Add all tests defined in the DisjunctionResolverUnitTestBase class.
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testResolvesOnFirstMatchingPossibleFunctor", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testResolvesOnSecondMatchingPossibleFunctor", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testFailsOnNoMatchingOutOfSeveralPossibleFunctors", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableTakesBindingsFromTwoDisjunctionPaths", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testVariableTakesBindingsFromManyDisjunctionPaths", engine));

        // Add all tests defined in the BacktrackingResolverUnitTestBase class.
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testMultipleFactsProduceMultipleSolutions", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
                "testInstantiatingClausesSeveralTimesWithSameVariableDoesNotConflictVariableBindings", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, L3CompiledPredicate, L3CompiledQuery>(
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
