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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorTermPredicate;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.GET_STRUC;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.PUT_STRUC;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.SET_VAL;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.SET_VAR;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.UNIFY_VAR;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.ByteBufferUtils;

/**
 * L0Compiler implements a compiler for the logical language, L0, into a form suitable for passing to an
 * {@link VariableAndFunctorInterner}. The L0Machine accepts sentences in the language that are compiled into a byte
 * code form. The byte instructions used in the compiled language are enumerated as constants in the
 * {@link L0InstructionSet} class.
 *
 * <p/>The compilation process is described in "Warren's Abstact Machine, A Tutorial Reconstruction, by Hassan Ait-Kaci"
 * and is followed as closely as possible to the L0 compiler given there. A brief overview of the compilation process
 * is:
 *
 * <pre><p/><ul>
 * <li>Terms to be compiled are allocated registers, breadth first, enumerating from outermost functors down to
 *     inermost atoms or variables.</li>
 * <li>Queries are 'flattened' by traversing them in postfix order of their functors, then exploring the functors
 *     arguments.</li>
 * <li>Programs are 'flattened' by traversing them breadth first, the same as for the original register allocation,
 *     then exploring the functors arguments.</li>
 * </ul></pre>
 *
 * <p/>Query terms are compiled into a sequence of instructions, that build up a representation of the term to be
 * unified, on the heap. Program terms are compiled into a sequence of instructions that, when run against the heap,
 * attempt to unify with it.
 *
 * <p/>The effect of flattening queries using a post fix ordering, is that the values of inner functors and variables
 * are loaded into registers first, before their containing functor is executed, which writes the functor and its
 * arguments onto the heap. Programs do not need to be expanded in this way, they simply match functors followed by
 * their arguments against the heap, so a breadth first traversal is all that is needed.
 *
 * <p/>Evaluating a flattened query consists of doing the following as different query tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For a register associated with a functor, push an STR onto the heap and copy that cell into the register.
 *     A put_struc (functor, register) instruction is created for this.</li>
 * <li>For a register argument not previously seen, push a REF onto the heap that refers to itself, and copy that cell
 *     into the register. A set_var (register) instruction is emmitted for this.</li>
 * <li>For a register argument previously seen, push a new cell onto the heap and copy into it the register's value.
 *     A set_val (register) instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <p/>Evaluating a flattened program consists of doing the following as different program tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For a register associated with a functor, load that register with a reference to the functor. A get_struc
 *     (functor, register) instruction is created for this.</li>
 * <li>For a register argument not previously seen, bind that register to its argument. A unify_var (register)
 *     instruction is output for this.</li>
 * <li>For a register argument previously seen, unify that register against the heap. A unify_val (register)
 *     instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Transform L0 sentences into compiled byte code.
 *     <td> {@link VariableAndFunctorInterner}, {@link L0Sentence}, {@link L0CompiledTerm}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L0Compiler implements LogicCompiler<Term, L0CompiledFunctor, L0CompiledFunctor>
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L0Compiler.class.getName()); */

    /** Holds the machine to compile into. */
    VariableAndFunctorInterner machine;

    /** Holds the observer to notify of compiler outputs. */
    LogicCompilerObserver<L0CompiledFunctor, L0CompiledFunctor> observer;

    /**
     * Creates a new L0Compiler. 2147483647
     *
     * @param machine The L0 machine to output compiled code into.
     */
    public L0Compiler(VariableAndFunctorInterner machine)
    {
        // Keep the machine to compile into.
        this.machine = machine;
    }

    /** {@inheritDoc} */
    public void compile(Sentence<Term> sentence) throws SourceCodeException
    {
        /*log.fine("public L0CompiledTerm compile(Sentence<Term> sentence = " + sentence + "): called");*/

        // The compiled code is built up in this byte array.
        byte[] compBuf = new byte[1000];
        int offset = 0;

        // A mapping from registers to variable names is built up in this.
        Map<Byte, Integer> varNames = new HashMap<Byte, Integer>();

        // Ensure that the sentence to compile is a valid sentence in L0.
        if (!(sentence instanceof L0Sentence))
        {
            throw new IllegalArgumentException("The sentence must be an L0Sentence.");
        }

        L0Sentence l0Sentence = (L0Sentence) sentence;
        Functor expression = l0Sentence.getExpression();

        // Need to assign registers to the syntax tree, working in from the outside (i.e. breadth first).
        // Build these up in an array list?
        QueueBasedSearchMethod<Term, Term> outInSearch = new BreadthFirstSearch<Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator<Term> treeWalker = Searches.allSolutions(outInSearch);

        // For each term encountered: set X++ = term.
        int x = 0;

        while (treeWalker.hasNext())
        {
            Term term = treeWalker.next();

            if (term.getAllocation() == -1)
            {
                if (term instanceof Functor)
                {
                    /*log.fine("X" + x + " = " + machine.getFunctorFunctorName(((Functor) term).getName()));*/
                }
                else if (term instanceof Variable)
                {
                    /*log.fine("X" + x + " = " + machine.getVariableName(((Variable) term).getName()));*/

                    varNames.put((byte) x, ((Variable) term).getName());
                }

                term.setAllocation(x++);
            }
        }

        // Programs and Queries are compiled differently, so branch on the sentence type.
        if (sentence instanceof L0Sentence.L0Query)
        {
            // Heap cells are to be created in an order such that no heap cell can appear before other cells that it
            // refers to. A postfix traversal of the functors in the term to compile is used to achieve this, as
            // child functors in an head will be visited first.
            // Walk over the query term in post-fix order, picking out just the functors.
            QueueBasedSearchMethod<Term, Term> postfixSearch = new PostFixSearch<Term, Term>();
            postfixSearch.reset();
            postfixSearch.addStartState(expression);
            postfixSearch.setGoalPredicate(new FunctorTermPredicate());

            treeWalker = Searches.allSolutions(postfixSearch);

            // This is used to keep track of variables as they are seen.
            Set<Variable> seenVars = new HashSet<Variable>();

            // For each functor encountered: put_struc.
            while (treeWalker.hasNext())
            {
                Functor nextFunctor = (Functor) treeWalker.next();
                int register = nextFunctor.getAllocation();

                /*log.fine("put_struc " + machine.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() + ", X" +
                    (register));*/

                compBuf[offset++] = PUT_STRUC;
                compBuf[offset++] = (byte) (register & 0xff);
                compBuf[offset++] = (byte) nextFunctor.getArity();
                ByteBufferUtils.write24BitIntToByteArray(compBuf, offset, nextFunctor.getName());
                offset += 3;

                // For each argument of the functor.
                int numArgs = nextFunctor.getArity();

                for (int i = 0; i < numArgs; i++)
                {
                    Term nextArg = nextFunctor.getArgument(i);
                    int registerRef = nextArg.getAllocation();

                    // If it is new variable: set_var.
                    if (nextArg.isVar() && !seenVars.contains(nextArg))
                    {
                        seenVars.add((Variable) nextArg);
                        /*log.fine("set_var X" + (registerRef));*/

                        compBuf[offset++] = SET_VAR;
                        compBuf[offset++] = (byte) (registerRef & 0xff);
                    }

                    // If it is variable or functor already seen: set_val.
                    else
                    {
                        /*log.fine("set_value X" + (registerRef));*/

                        compBuf[offset++] = SET_VAL;
                        compBuf[offset++] = (byte) (registerRef & 0xff);
                    }
                }
            }
        }
        else
        {
            // Program instructions are generated in the same order as the registers are assigned, the postfix
            // ordering used for queries is not needed.
            outInSearch.reset();
            outInSearch.addStartState(expression);

            treeWalker = Searches.allSolutions(outInSearch);

            // This is used to keep track of registers as they are seen.
            Set<Integer> seenRegisters = new HashSet<Integer>();

            // For each term encountered: set X++ = term.
            while (treeWalker.hasNext())
            {
                Term nextTerm = treeWalker.next();

                // For each functor encountered: get_struc.
                if (nextTerm.isFunctor())
                {
                    Functor nextFunctor = (Functor) nextTerm;
                    int register = nextFunctor.getAllocation();

                    /*log.fine("get_struc " + machine.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                        ", X" + (register));*/

                    compBuf[offset++] = GET_STRUC;
                    compBuf[offset++] = (byte) (register & 0xff);
                    compBuf[offset++] = (byte) nextFunctor.getArity();
                    ByteBufferUtils.write24BitIntToByteArray(compBuf, offset, nextFunctor.getName());
                    offset += 3;

                    // For each argument of the functor.
                    int numArgs = nextFunctor.getArity();

                    for (int i = 0; i < numArgs; i++)
                    {
                        Term nextArg = nextFunctor.getArgument(i);
                        int registerRef = nextArg.getAllocation();

                        // If it is register not seen before: unify_var.
                        if (!seenRegisters.contains(registerRef))
                        {
                            seenRegisters.add(registerRef);
                            /*log.fine("unify_var X" + (registerRef));*/

                            compBuf[offset++] = UNIFY_VAR;
                            compBuf[offset++] = (byte) (registerRef & 0xff);
                        }

                        // If it is register seen before: unify_val.
                        else
                        {
                            /*log.fine("unify_value X" + (registerRef));*/

                            compBuf[offset++] = UNIFY_VAL;
                            compBuf[offset++] = (byte) (registerRef & 0xff);
                        }
                    }
                }
            }
        }

        // Create an L0 compiled term with a reference to its compiled against machine.
        byte[] code = new byte[offset];
        System.arraycopy(compBuf, 0, code, 0, offset);

        L0CompiledFunctor result;

        if (sentence instanceof L0Sentence.L0Query)
        {
            result = new L0CompiledQueryFunctor(machine, code, varNames);
            observer.onQueryCompilation(result);
        }
        else
        {
            result = new L0CompiledProgramFunctor(machine, code, varNames);
            observer.onCompilation(result);
        }
    }

    /** {@inheritDoc} */
    public void setCompilerObserver(LogicCompilerObserver<L0CompiledFunctor, L0CompiledFunctor> observer)
    {
        this.observer = observer;
    }

    /** {@inheritDoc} */
    public void endScope()
    {
    }
}
