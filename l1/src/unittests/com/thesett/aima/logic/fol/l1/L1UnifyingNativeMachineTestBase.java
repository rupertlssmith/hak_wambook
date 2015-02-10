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

import junit.framework.Test;
import junit.framework.TestSuite;

import org.apache.log4j.NDC;

import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Unifier;
import com.thesett.aima.logic.fol.UnifierUnitTestBase;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.Token;

/**
 * L1UnifyingJavaMachineTest tests unification over a range of terms in first order logic, in order to test all success
 * and failure paths through an L1 byte code machine.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L1UnifyingNativeMachineTestBase extends UnifierUnitTestBase<Term, L1CompiledFunctor>
{
    /** The machine to test. */
    public static L1UnifyingMachine machine;

    /**
     * Creates a simple unification test for the specified unifier, using the specified compiler.
     *
     * @param name     The name of the test.
     * @param unifier  The unifier to test.
     * @param compiler The compiler to prepare terms for unification with.
     * @param parser   The parser to parse programs with.
     * @param qparser  The parser to parse queries with.
     * @param interner
     */
    public L1UnifyingNativeMachineTestBase(String name, Unifier<L1CompiledFunctor> unifier,
        LogicCompiler<Term, L1CompiledFunctor, L1CompiledFunctor> compiler, Parser<Term, Token> parser,
        Parser<Term, Token> qparser, VariableAndFunctorInterner interner)
    {
        super(name, unifier, compiler, parser, qparser, interner);
    }

    /** Compile all the tests for the default tests for unifiers into a suite, plus the tests defined in this class. */
    public static Test suite() throws Exception
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("L1UnifyingNativeMachine Tests");

        machine = L1UnifyingNativeMachine.getInstance();

        LogicCompiler<Term, L1CompiledFunctor, L1CompiledFunctor> compiler = new L1Compiler(machine);
        Parser<Term, Token> parser = new L1UnifyingJavaMachineTest.StatementParser(machine);
        Parser<Term, Token> qparser = new L1UnifyingJavaMachineTest.QueryParser(machine);

        // Add all tests defined in the ClassifyingMachineUnitTestBase class
        suite.addTest(new L1UnifyingNativeMachineTestBase("testAtomsUnifyOk", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testNonMatchingAtomsFailUnify", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeLeftVarUnifiesAtomOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeLeftVarUnifiesFunctorOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeRightVarUnifiesAtomOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeRightVarUnifiesFunctorOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeVarUnifiesWithSameNameOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFreeVarUnifiesWithDifferentNameOk", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testBoundVarUnifiesWithDifferentEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testBoundVarToFunctorUnifiesWithEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testBoundVarFailsToUnifyWithDifferentBinding", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testProgBoundVarUnifiesWithDifferentEqualBoundVarOk",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testProgBoundVarFailsToUnifyWithDifferentBinding", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testBoundVarFailsToUnifyWithDifferentlyBoundVar", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testBoundVarPropagatesIntoFunctors", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFunctorsSameArityUnify", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFunctorsDifferentArityFailToUnify", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFunctorsSameArityDifferentArgsFailToUnify", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingNativeMachineTestBase("testFunctorsDifferentNameSameArgsDoNotUnify", machine,
                compiler, parser, qparser, machine));

        // Add all the tests defined in this class (using the default constructor)
        // suite.addTestSuite(PrologUnifierTest.class);

        return suite;
    }

    protected void setUp()
    {
        NDC.push(getName());

        // Reset the L1 machine on every tests, to ensure that old programs do not interfere with test queries.
        machine.reset();
    }

    protected void tearDown()
    {
        NDC.pop();
    }
}
