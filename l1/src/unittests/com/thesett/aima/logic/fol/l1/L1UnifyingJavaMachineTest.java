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

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Unifier;
import com.thesett.aima.logic.fol.UnifierUnitTestBase;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TermParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;

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
public class L1UnifyingJavaMachineTest extends UnifierUnitTestBase<Term, L1CompiledFunctor>
{
    /** Holds the L1 machine to run the tests through. */
    private static L1UnifyingMachine machine;

    /** Used for debugging. */
    java.util.logging.Logger log = java.util.logging.Logger.getLogger(L1UnifyingJavaMachineTest.class.getName());

    /**
     * Creates a simple unification test for the specified unifier, using the specified compiler.
     *
     * @param name     The name of the test.
     * @param unifier  The unifier to test.
     * @param compiler The compiler to prepare terms for unification with.
     * @param parser   The parser to parse programs with.
     * @param qparser  The parser to parse queries with.
     * @param interner The variable and functor interner.
     */
    public L1UnifyingJavaMachineTest(String name, Unifier<L1CompiledFunctor> unifier,
        LogicCompiler<Term, L1CompiledFunctor, L1CompiledFunctor> compiler, Parser<Term, Token> parser,
        Parser<Term, Token> qparser, VariableAndFunctorInterner interner)
    {
        super(name, unifier, compiler, parser, qparser, interner);
    }

    /**
     * Compile all the tests for the default tests for unifiers into a suite, plus the tests defined in this class.
     *
     * @return A test suite.
     */
    public static Test suite()
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("L1UnifyingJavaMachine Tests");

        machine = new L1UnifyingJavaMachine();

        LogicCompiler<Term, L1CompiledFunctor, L1CompiledFunctor> compiler = new L1Compiler(machine);
        Parser<Term, Token> parser = new StatementParser(machine);
        Parser<Term, Token> qparser = new QueryParser(machine);

        // Add all tests defined in the ClassifyingMachineUnitTestBase class
        suite.addTest(new L1UnifyingJavaMachineTest("testAtomsUnifyOk", machine, compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testNonMatchingAtomsFailUnify", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeLeftVarUnifiesAtomOk", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeLeftVarUnifiesFunctorOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeRightVarUnifiesAtomOk", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeRightVarUnifiesFunctorOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeVarUnifiesWithSameNameOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFreeVarUnifiesWithDifferentNameOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testQueryAtomDoesNotUnifyWithProgFunctorSameName", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testProgAtomDoesNotUnifyWithQueryFunctorSameName", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarUnifiesWithDifferentEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarToFunctorUnifiesWithEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarFailsToUnifyWithDifferentBinding", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testProgBoundVarFailsToUnifyWithDifferentBinding", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarInQueryUnifiesAgainstVarInProg", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarFailsToUnifyWithDifferentlyBoundVar", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarPropagatesIntoFunctors", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundVarUnifiesToSameVar", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundProgVarUnifiesToDifferentQueryVar", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testBoundQueryVarUnifiesToDifferentProgVar", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFunctorsSameArityUnify", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFunctorsDifferentArityFailToUnify", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFunctorsSameArityDifferentArgsFailToUnify", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new L1UnifyingJavaMachineTest("testFunctorsDifferentNameSameArgsDoNotUnify", machine, compiler,
                parser, qparser, machine));

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

    /**
     * Parses terms in first order logic as L0 programs.
     */
    public static class StatementParser extends TermParser
    {
        /**
         * Creates a parser on the specified interner.
         *
         * @param interner The functor and variable name interner.
         */
        public StatementParser(VariableAndFunctorInterner interner)
        {
            super(interner);
        }

        /**
         * Parses the next sentence from the current token source.
         *
         * @return The fully parsed syntax tree for the next sentence.
         */
        public Sentence<Term> parse() throws SourceCodeException
        {
            return L1Sentence.createProgram((Functor) parser.termSentence());
        }
    }

    /**
     * Parses L0 queries but without the leading "?-" or trailing '.'.
     */
    public static class QueryParser extends TermParser
    {
        /**
         * Creates a parser on the specified interner.
         *
         * @param interner The functor and variable name interner.
         */
        public QueryParser(VariableAndFunctorInterner interner)
        {
            super(interner);
        }

        /**
         * Parses the next sentence from the current token source.
         *
         * @return The fully parsed syntax tree for the next sentence.
         */
        public Sentence<Term> parse() throws SourceCodeException
        {
            return L1Sentence.createQuery((Functor) parser.termSentence());
        }
    }
}
