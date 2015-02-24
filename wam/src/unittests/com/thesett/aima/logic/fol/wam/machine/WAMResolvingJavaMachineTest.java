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

import com.thesett.aima.logic.fol.CallAndNotResolverUnitTestBase;
import com.thesett.aima.logic.fol.CutResolverUnitTestBase;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.NDC;

import com.thesett.aima.logic.fol.BacktrackingResolverUnitTestBase;
import com.thesett.aima.logic.fol.BasicResolverUnitTestBase;
import com.thesett.aima.logic.fol.BasicUnificationUnitTestBase;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.ConjunctionResolverUnitTestBase;
import com.thesett.aima.logic.fol.DisjunctionResolverUnitTestBase;
import com.thesett.aima.logic.fol.ListResolverUnitTestBase;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.TrueAndFailResolverUnitTestBase;
import com.thesett.aima.logic.fol.UnifyAndNonUnifyResolverUnitTestBase;
import com.thesett.aima.logic.fol.interpreter.ResolutionEngine;
import com.thesett.aima.logic.fol.isoprologparser.ClauseParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiler;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * WAMResolvingJavaMachineTest tests resolution and unification over a range of terms in first order logic, in order to
 * test all success and failure paths, through an WAM byte code machine. The WAM machine handles resolution with
 * backtracking, in addition to full unification without the occurs check.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Run all basic resolution tests. <td> {@link BasicResolverUnitTestBase}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMResolvingJavaMachineTest extends TestCase
{
    /** Holds the WAM machine to run the tests through. */
    private static WAMResolvingMachine machine;

    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(WAMResolvingJavaMachineTest.class.getName()); */

    /**
     * Creates a test with the specified name.
     *
     * @param name The name of the test.
     */
    public WAMResolvingJavaMachineTest(String name)
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
        TestSuite suite = new TestSuite("WAMResolvingJavaMachine Tests");

        SymbolTableImpl<Integer, String, Object> symbolTable = new SymbolTableImpl<Integer, String, Object>();

        machine = new WAMResolvingJavaMachine(symbolTable);

        LogicCompiler<Clause, WAMCompiledPredicate, WAMCompiledQuery> compiler = new WAMCompiler(symbolTable, machine);
        Parser<Clause, Token> parser = new ClauseParser(machine);

        ResolutionEngine<Clause, WAMCompiledPredicate, WAMCompiledQuery> engine =
            new WAMEngine(parser, machine, compiler, machine);

        // Attach the debugger to the machine.
        /*SimpleMonitor monitor = new SimpleMonitor();
        machine.attachMonitor(monitor);*/

        // Add all tests defined in the BasicUnificationTestBase class
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAtomsUnifyOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingAtomsFailUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeLeftVarUnifiesAtomOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeLeftVarUnifiesFunctorOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeRightVarUnifiesAtomOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeRightVarUnifiesFunctorOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeVarUnifiesWithSameNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeVarUnifiesWithDifferentNameOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testQueryAtomDoesNotUnifyWithProgFunctorArgSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgAtomDoesNotUnifyWithQueryFunctorArgSameName", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testDeeperBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarPropagatesIntoFunctors", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarUnifiesToSameVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundProgVarUnifiesToDifferentQueryVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundQueryVarUnifiesToDifferentProgVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsSameArityUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsDifferentArityFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsSameArityDifferentArgsFailToUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsDifferentNameSameArgsDoNotUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsMultipleInnerConstantsUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsMultipleInnerConstantsUnifyWithVar", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsVarUnifiesWithMultipleInnerConstants", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsMultipleDifferentInnerConstantsUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsMultipleNestedMixedVarsAndConstantsUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsNestedConstantsMixedWithVarsUnify", engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>("testWamBook2_9",
                engine));
        suite.addTest(new BasicUnificationUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testWamBook2_9OtherWay", engine));

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
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousProgramAndQuery", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAnonymousNestedProgramAndQuery", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorInHead", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorArgInHead", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorInBody", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorArgInBody", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorInHeadThroughCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariableBindsToFunctorArgInHeadThroughCall", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testTemporaryRegistersNotOverwritten", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBodyVariableBindsOk", engine));
        suite.addTest(new BasicResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testVariablesUnboundOnBacktrackingMemberOk", engine));

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
                "testNonPermanentVariableResolvedOkNoBinding", engine));
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
        suite.addTest(new ConjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testManyVariablesAcrossCallsOk", engine));

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
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testJunctionBracketingFalse", engine));
        suite.addTest(new DisjunctionResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testJunctionBracketingAllowsDisjunction", engine));

        // Add all tests defined in the BacktrackingResolverUnitTestBase class.
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testMultipleFactsProduceMultipleSolutions", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testThreeFactsProduceThreeSolutions", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testInstantiatingClausesSeveralTimesWithSameVariableDoesNotConflictVariableBindings", engine));
        suite.addTest(new BacktrackingResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testInstantiatingClausesSeveralTimesWithDifferentVariableAllowsIndependentBindings", engine));

        // Add all tests defined in the ListResolverUnitTestBase class.
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>("testNilRecognized",
                engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonEmptyListRecognized", engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>("testConsRecognized",
                engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testListIterationTerminatesOnEmpty", engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testListIterationTerminatesOnList", engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testListIterationTerminatesOnNonEmptyFinalCase", engine));
        suite.addTest(new ListResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testListIterationBacktracks", engine));

        // Add all tests defined in the CutResolverUnitTestBase class.
        suite.addTest(new CutResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNeckCutPreventsBacktrackingOk", engine));
        suite.addTest(new CutResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testDeepCutPreventsBacktrackingOk", engine));

        // Add all tests defined in the CallAndNotResolverUnitTestBase class.
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testSimpleCallOk", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testCallFunctorWithArgumentBindsVariable", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testCallFunctorWithArgumentBindsVariableInChainedCall", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNotFunctorWithArgumentOkWhenArgumentsDoNotMatch", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNotFunctorWithArgumentDoesNotBindVariableInDoubleNegation", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNotFailSucceeds", engine));
        suite.addTest(new CallAndNotResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNotTrueFails", engine));

        // Add all tests defined in the TrueAndFailResolverUnitTestBase class.
        suite.addTest(new TrueAndFailResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testTrueSucceeds", engine));
        suite.addTest(new TrueAndFailResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFailFails", engine));

        /*suite.addTest(new TrueAndFailResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testDisjunctionOfTrueAndFailSucceeds", engine));*/
        suite.addTest(new TrueAndFailResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionOfTrueAndFailFails", engine));
        suite.addTest(new TrueAndFailResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testConjunctionOfTruesSucceeds", engine));

        // Add all the tests defined in the UnifyAndNonUnifyResolverUnitTestBase class.
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testAtomsUnifyOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testNonMatchingAtomsFailUnify", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeLeftVarUnifiesAtomOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeLeftVarUnifiesFunctorOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeRightVarUnifiesAtomOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeRightVarUnifiesFunctorOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeVarUnifiesWithSameNameOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFreeVarUnifiesWithDifferentNameOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarInQueryUnifiesAgainstVarInProg", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarPropagatesIntoFunctors", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundVarUnifiesToSameVar", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundProgVarUnifiesToDifferentQueryVar", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testBoundQueryVarUnifiesToDifferentProgVar", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsSameArityUnify", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsDifferentArityFailToUnify", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsSameArityDifferentArgsFailToUnify", engine));
        suite.addTest(new UnifyAndNonUnifyResolverUnitTestBase<Clause, WAMCompiledPredicate, WAMCompiledQuery>(
                "testFunctorsDifferentNameSameArgsDoNotUnify", engine));

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
