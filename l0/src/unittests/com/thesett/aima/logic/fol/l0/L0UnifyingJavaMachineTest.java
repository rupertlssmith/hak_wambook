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
package com.thesett.aima.logic.fol.l0;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.UnifierUnitTestBase;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.TermParser;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.common.parsing.SourceCodeException;

/**
 * L0UnifyingJavaMachineTest tests unification over a range of terms in first order logic, in order to test all success
 * and failure paths through an L0 byte code machine.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L0UnifyingJavaMachineTest extends TestCase
{
    public L0UnifyingJavaMachineTest(String name)
    {
        super(name);
    }

    /** Compile all the tests for the default tests for unifiers into a suite, plus the tests defined in this class. */
    public static Test suite()
    {
        // Build a new test suite
        TestSuite suite = new TestSuite("L0UnifyingJavaMachine Tests");

        L0UnifyingMachine machine = new L0UnifyingJavaMachine();
        LogicCompiler<Term, L0CompiledFunctor, L0CompiledFunctor> compiler = new L0Compiler(machine);
        Parser<Term, Token> parser = new StatementParser(machine);
        Parser<Term, Token> qparser = new QueryParser(machine);

        // Add all tests defined in the ClassifyingMachineUnitTestBase class
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testAtomsUnifyOk", machine, compiler, parser,
                qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testNonMatchingAtomsFailUnify", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeLeftVarUnifiesAtomOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeLeftVarUnifiesFunctorOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeRightVarUnifiesAtomOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeRightVarUnifiesFunctorOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeVarUnifiesWithSameNameOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFreeVarUnifiesWithDifferentNameOk", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testQueryAtomDoesNotUnifyWithProgFunctorSameName", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testProgAtomDoesNotUnifyWithQueryFunctorSameName", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testBoundVarUnifiesWithDifferentEqualBoundVarOk", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testBoundVarToFunctorUnifiesWithEqualBoundVarOk", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundVarFailsToUnifyWithDifferentBinding",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testProgBoundVarUnifiesWithDifferentEqualBoundVarOk", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testProgBoundVarToFunctorUnifiesWithEqualBoundVarOk", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testProgBoundVarFailsToUnifyWithDifferentBinding", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testProgBoundVarToFunctorFailsToUnifyWithDifferentFunctorBinding", machine, compiler, parser, qparser,
                machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundVarInQueryUnifiesAgainstVarInProg",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>(
                "testBoundVarFailsToUnifyWithDifferentlyBoundVar", machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundVarPropagatesIntoFunctors", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundVarUnifiesToSameVar", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundProgVarUnifiesToDifferentQueryVar",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testBoundQueryVarUnifiesToDifferentProgVar",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFunctorsSameArityUnify", machine, compiler,
                parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFunctorsDifferentArityFailToUnify", machine,
                compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFunctorsSameArityDifferentArgsFailToUnify",
                machine, compiler, parser, qparser, machine));
        suite.addTest(new UnifierUnitTestBase<Term, L0CompiledFunctor>("testFunctorsDifferentNameSameArgsDoNotUnify",
                machine, compiler, parser, qparser, machine));

        // Add all the tests defined in this class (using the default constructor)
        // suite.addTestSuite(PrologUnifierTest.class);

        return suite;
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
            return L0Sentence.createProgram((Functor) parser.termSentence());
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
            return L0Sentence.createQuery((Functor) parser.termSentence());
        }
    }
}
