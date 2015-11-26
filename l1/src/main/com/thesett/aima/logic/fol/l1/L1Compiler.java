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

import java.util.Collection;
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
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.CALL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PROCEED;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAR;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.SearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.ByteBufferUtils;

/**
 * L1Compiled implements a compiler for the logical language, L1, into a form suitable for passing to an
 * {@link L1Machine}. The L1Machine accepts sentences in the language that are compiled into a byte code form. The byte
 * instructions used in the compiled language are enumerated as constants in the {@link L1InstructionSet} class.
 *
 * <p/>The compilation process is described in "Warren's Abstact Machine, A Tutorial Reconstruction, by Hassan Ait-Kaci"
 * and is followed as closely as possible to the L1 compiler given there. The description of the L0 compilation process
 * is very clear in the text but the L1 compilation is a little ambiguous. It does not fully describe the flattening
 * process and presents some conflicting examples of register assignment. (The flattening process is essentially the
 * same as for L0, except that each argument of the outermost functor is flattened/compiled independently). The register
 * assignment process is harder to fathom, on page 22, the register assignment for p(Z, h(Z,W), f(W)) is presented with
 * the following assignment given:
 *
 * <pre>
 * A1 = Z
 * A2 = h(A1,X4)
 * A3 = f(X4)
 * X4 = W
 * </pre>
 *
 * In figure 2.9 a compilation example is given, from which it can be seen that the assignment should be:
 *
 * <pre>
 * A1 = Z (loaded from X4)
 * A2 = h(X4,X5)
 * A3 = f(X5)
 * X4 = Z
 * X5 = W
 * </pre>
 *
 * <p/>From figure 2.9 it was concluded that argument registers may only be assigned to functors. Functors can be
 * created on the heap and assigned to argument registers directly. Argument registers for variables, should be loaded
 * from a separate register assigned to the variable, that comes after the argument registers; so that a variable
 * assignment can be copied into multiple arguments, where the same variable is presented multiple times in a predicate
 * call. The register assignment process is carried out in two phases to do this, the first pass covers the argument
 * registers and the arguments of the outermost functor, only assigning to functors, the second pass continues for
 * higher numbered registers, starts again at the begining of the arguments, and assigns to variables and functors (not
 * already assigned) as for the L0 process.
 *
 * <p/>A brief overview of the compilation process is:
 *
 * <pre><p/><ul>
 * <li>Terms to be compiled are allocated registers, breadth first, enumerating from outermost functors down to
 *     inermost atoms or variables.</li>
 * <li>The outermost functor itself is treated specially, and is not allocated to a register. Its i arguments are
 *     allocated to registers, and are additionaly associated with the first i argument registers. The outermost
 *     functor is the instigator of a call, in the case of queries, or the recipient of a call, in the case of programs.
 * <li>Queries are 'flattened' by traversing each of their arguments in postfix order of their functors, then exploring
 *     the functors arguments.</li>
 * <li>Programs are 'flattened' by traversing each of their arguments breadth first, the same as for the original
 *     register allocation, then exploring the functors arguments.</li>
 * </ul></pre>
 *
 * <p/>Query terms are compiled into a sequence of instructions, that build up a representation of their argument terms,
 * to be unified, on the heap, and assigning registers to refer to those terms on the heap, then calling the matching
 * program for the query terms name and arity. Program terms are compiled into a sequence of instructions that, when run
 * against the argument registers, attempt to unify all of the arguments with the heap.
 *
 * <p/>The effect of flattening queries using a post fix ordering, is that the values of inner functors and variables
 * are loaded into registers first, before their containing functor is executed, which writes the functor and its
 * arguments onto the heap. Programs do not need to be expanded in this way, they simply match functors followed by
 * their arguments against the heap, so a breadth first traversal is all that is needed.
 *
 * <p/>Evaluating a flattened query consists of doing the following as different query tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then make a CALL (functor) to the matching program.
 * <li>For a register associated with an inner functor, push an STR onto the heap and copy that cell into the register.
 *     A put_struc (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, push a REF onto the heap that refers to iself,
 *     and copy that value into that variables register, as well as argument register i. A put_var (register, register)
 *     instruction is emmitted for this.
 * <li>For a register argument of an inner functor, not previously seen, push a REF onto the heap that refers to itself,
 *     and copy that cell into the register. A set_var (register) instruction is emmitted for this.</li>
 * <li>For a variables in argument position i in the outermost functor, previosly seen, copy its assigned register
 *     into its argument register. A put_val (register, register) instruction is emmitted for this.</li>
 * <li>For a register argument previously seen, push a new cell onto the heap and copy into it the register's value.
 *     A set_val (register) instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <p/>Evaluating a flattened program consists of doing the following as different program tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then execute a PROCEED instruction to indicate success.
 * <li>For a register associated with an inner functor, load that register with a reference to the functor. A get_struc
 *     (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, copy its argument register into its assigned
 *     rgister. A get_var (register, register) instruction is emmitted for this.
 * <li>For a register argument of an innter functor, not previously seen, bind that register to its argument. A
 *     unify_var (register) instruction is output for this.</li>
 * <li>For a variable in argument position i in the outermost functor, unify its assigned register with the
 *     argument register. A get_val (register, register) instruction is emmitted for this.</li>
 * <li>For a register argument of an inner functor, previously seen, unify that register against the heap. A
 *     unify_val (register) instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Transform L1 sentences into compiled byte code.
 *     <td> {@link L1Machine}, {@link L1Sentence}, {@link L1CompiledTerm}
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Document more things not fully described in the text: When to regard a variable as already seen? Globally
 *         over the whole sentence, I think. Flattening process for queires/programs. Queries: loop over all args,
 *         postfix flatten each in turn. Programs: BFS over the whole, but only output proceed for the outermost
 *         functor, and only output get_var and _val instructions for variables in the outermost functor.
 */
public class L1Compiler implements LogicCompiler<Term, L1CompiledFunctor, L1CompiledFunctor>
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L1Compiler.class.getName()); */

    /** Holds the machine to compile into. */
    L1Machine machine;

    /** Holds the compiler output observer. */
    LogicCompilerObserver<L1CompiledFunctor, L1CompiledFunctor> observer;

    /**
     * Creates a new L1Compiler.
     *
     * @param machine The L1 machine to output compiled code into.
     */
    public L1Compiler(L1Machine machine)
    {
        // Keep the machine to compile into.
        this.machine = machine;
    }

    /** {@inheritDoc} */
    public void compile(Sentence<Term> sentence) throws SourceCodeException
    {
        /*log.fine("public L1CompiledTerm compile(Sentence<Term> sentence = " + sentence + "): called");*/

        // Ensure that the sentence to compile is a valid sentence in L1.
        if (!(sentence instanceof L1Sentence))
        {
            throw new IllegalArgumentException("The sentence must be an L1Sentence.");
        }

        // The compiled code is built up in this byte array.
        byte[] compBuf = new byte[1000];
        int offset = 0;

        // A mapping from registers to variable names is built up in this.
        Map<Byte, Integer> varNames = new HashMap<Byte, Integer>();

        L1Sentence l1Sentence = (L1Sentence) sentence;
        Functor expression = l1Sentence.getExpression();

        // Assign argument registers to functors appearing directly in the argument of the outermost functor.
        // Variables are never assigned directly to argument registers.
        int x;

        for (x = 0; x < expression.getArity(); x++)
        {
            Term term = expression.getArgument(x);

            if (term instanceof Functor)
            {
                /*log.fine("X" + x + " = " + machine.getFunctorFunctorName((Functor) term));*/

                term.setAllocation(x);
            }
        }

        // Need to assign registers to the whole syntax tree, working in from the outermost functor. The outermost
        // functor itself is not assigned to a register in l1 (only in l0). Functors already directly assigned to
        // argument registers will not be re-assigned by this, variables as arguments will be assigned.
        SearchMethod outInSearch = new BreadthFirstSearch<Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator<Term> treeWalker = Searches.allSolutions(outInSearch);

        // Discard the outermost functor from the variable allocation.
        treeWalker.next();

        // For each term encountered: set X++ = term.
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
        if (sentence instanceof L1Sentence.L1Query)
        {
            // This is used to keep track of variables as they are seen.
            Collection<Variable> seenVars = new HashSet<Variable>();

            // Loop over all of the arguments to the outermost functor.
            int numOutermostArgs = expression.getArity();

            for (int j = 0; j < numOutermostArgs; j++)
            {
                Term nextOutermostArg = expression.getArgument(j);
                int registerRef = nextOutermostArg.getAllocation();

                // On the first occurrence of a variable output a put_var.
                if (nextOutermostArg.isVar() && !seenVars.contains(nextOutermostArg))
                {
                    seenVars.add((Variable) nextOutermostArg);

                    // The variable has been moved into an argument register.
                    varNames.remove((byte) registerRef);
                    varNames.put((byte) j, ((Variable) nextOutermostArg).getName());

                    /*log.fine("PUT_VAR X" + (registerRef) + ", A" + j);*/
                    compBuf[offset++] = PUT_VAR;
                    compBuf[offset++] = (byte) (registerRef & 0xff);
                    compBuf[offset++] = (byte) (j & 0xff);

                }

                // On a subsequent variable occurrence output a put_val.
                else if (nextOutermostArg.isVar())
                {
                    /*log.fine("PUT_VAL X" + (registerRef) + ", A" + j);*/
                    compBuf[offset++] = PUT_VAL;
                    compBuf[offset++] = (byte) (registerRef & 0xff);
                    compBuf[offset++] = (byte) (j & 0xff);
                }

                // When a functor is encountered, output a put_struc.
                else if (nextOutermostArg.isFunctor())
                {
                    Term nextFunctorArg = (Functor) nextOutermostArg;

                    // Heap cells are to be created in an order such that no heap cell can appear before other cells that it
                    // refers to. A postfix traversal of the functors in the term to compile is used to achieve this, as
                    // child functors in an head will be visited first.
                    // Walk over the query term in post-fix order, picking out just the functors.
                    QueueBasedSearchMethod<Term, Term> postfixSearch = new PostFixSearch<Term, Term>();
                    postfixSearch.reset();
                    postfixSearch.addStartState(nextFunctorArg);
                    postfixSearch.setGoalPredicate(new FunctorTermPredicate());

                    treeWalker = Searches.allSolutions(postfixSearch);

                    // For each functor encountered: put_struc.
                    while (treeWalker.hasNext())
                    {
                        Functor nextFunctor = (Functor) treeWalker.next();
                        int register = nextFunctor.getAllocation();

                        // Ouput a put_struc instuction, except on the outermost functor.
                        /*log.fine("PUT_STRUC " + machine.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                            ", X" + (register));*/

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
                            registerRef = nextArg.getAllocation();

                            // If it is new variable: set_var or put_var.
                            if (nextArg.isVar() && !seenVars.contains(nextArg))
                            {
                                seenVars.add((Variable) nextArg);

                                /*log.fine("SET_VAR X" + (registerRef));*/
                                compBuf[offset++] = SET_VAR;
                                compBuf[offset++] = (byte) (registerRef & 0xff);
                            }

                            // If it is variable or functor already seen: set_val or put_val.
                            else
                            {
                                /*log.fine("SET_VAL X" + (registerRef));*/
                                compBuf[offset++] = SET_VAL;
                                compBuf[offset++] = (byte) (registerRef & 0xff);
                            }
                        }
                    }
                }
            }

            // Complete the outermost query functor with a call to its matching program.
            /*log.fine("CALL " + machine.getFunctorName(expression) + "/" + expression.getArity());*/

            L1CallTableEntry callTableEntry = machine.getCodeAddress(expression.getName());
            int entryPoint = (callTableEntry == null) ? -1 : callTableEntry.entryPoint;

            // Write out the call instructions, followed by the call address, followed by f_n of the calling query,
            // in order that the query f_n remains known when there is no callable program that matches the query.
            compBuf[offset++] = CALL;
            ByteBufferUtils.writeIntToByteArray(compBuf, offset, entryPoint);
            offset += 4;

            compBuf[offset++] = (byte) expression.getArity();
            ByteBufferUtils.write24BitIntToByteArray(compBuf, offset, expression.getName());
            offset += 3;
        }
        else
        {
            // Program instructions are generated in the same order as the registers are assigned, the postfix
            // ordering used for queries is not needed.
            outInSearch.reset();
            outInSearch.addStartState(expression);

            treeWalker = Searches.allSolutions(outInSearch);

            // This is used to keep track of registers as they are seen.
            Collection<Integer> seenRegisters = new HashSet<Integer>();

            // Skip the outermost functor.
            treeWalker.next();

            // Keep track of processing of the arguments to the outermost functor as get_val and get_var instructions
            // need to be output for variables encountered in the arguments only.
            int numOutermostArgs = expression.getArity();

            for (int j = 0; treeWalker.hasNext(); j++)
            {
                Term nextTerm = treeWalker.next();

                /*log.fine("nextTerm = " + nextTerm);*/

                // For each functor encountered: get_struc.
                if (nextTerm.isFunctor())
                {
                    Functor nextFunctor = (Functor) nextTerm;
                    int register = nextFunctor.getAllocation();

                    // Ouput a get_struc instruction, except on the outermost functor.
                    /*log.fine("GET_STRUC " + machine.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
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

                        /*log.fine("nextArg = " + nextArg);*/

                        // If it is register not seen before: unify_var.
                        if (!seenRegisters.contains(registerRef))
                        {
                            /*log.fine("UNIFY_VAR X" + (registerRef));*/

                            seenRegisters.add(registerRef);

                            compBuf[offset++] = UNIFY_VAR;
                            compBuf[offset++] = (byte) (registerRef & 0xff);
                        }

                        // If it is register seen before: unify_val.
                        else
                        {
                            /*log.fine("UNIFY_VAL X" + (registerRef));*/

                            compBuf[offset++] = UNIFY_VAL;
                            compBuf[offset++] = (byte) (registerRef & 0xff);
                        }
                    }
                }
                else if (j < numOutermostArgs)
                {
                    Term nextFunctor = (Variable) nextTerm;
                    int registerRef = nextFunctor.getAllocation();

                    // If it is register not seen before: get_var.
                    if (!seenRegisters.contains(registerRef))
                    {
                        /*log.fine("GET_VAR X" + (registerRef) + ", A" + j);*/

                        seenRegisters.add(registerRef);

                        compBuf[offset++] = GET_VAR;
                        compBuf[offset++] = (byte) (registerRef & 0xff);
                        compBuf[offset++] = (byte) (j & 0xff);
                    }

                    // If it is register seen before: get_val.
                    else
                    {
                        /*log.fine("GET_VAL X" + (registerRef) + ", A" + j);*/

                        compBuf[offset++] = GET_VAL;
                        compBuf[offset++] = (byte) (registerRef & 0xff);
                        compBuf[offset++] = (byte) (j & 0xff);
                    }
                }
            }

            // Output a proceed instruction at the end of the outermost functor only.
            /*log.fine("PROCEED");*/
            compBuf[offset++] = PROCEED;
        }

        // Insert the compiled code into the byte code machine's code area.
        L1CallTableEntry callTableEntry =
            machine.addCode(compBuf, 0, offset, expression.getName(), (sentence instanceof L1Sentence.L1Query));

        // Create an L1 compiled term with a reference to its compiled against machine and code offset within
        // the machine.
        L1CompiledFunctor result;

        if (sentence instanceof L1Sentence.L1Query)
        {
            result = new L1CompiledQueryFunctor(machine, callTableEntry, varNames);
            observer.onQueryCompilation(result);
        }
        else
        {
            result = new L1CompiledProgramFunctor(machine, callTableEntry, varNames);
            observer.onCompilation(result);
        }
    }

    /** {@inheritDoc} */
    public void setCompilerObserver(LogicCompilerObserver<L1CompiledFunctor, L1CompiledFunctor> observer)
    {
        this.observer = observer;
    }

    /** {@inheritDoc} */
    public void endScope()
    {
    }
}
