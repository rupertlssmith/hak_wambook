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
package com.thesett.aima.logic.fol.wam.compiler;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.FunctorTermPredicate;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.aima.logic.fol.wam.builtins.BuiltIn;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * DefaultBuiltIn implements the standard WAM Prolog compilation for normal Prolog programs. Splitting this out into
 * DefaultBuiltIn which supplies the {@link BuiltIn} interface, allows different compilations to be used for built in
 * predicates that behave differently to the normal compilation.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Generate instructions to set up the arguments to a call to a built-in functor.</td></tr>
 * <tr><td> Generate instructions to call to a built-in functor.</td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class DefaultBuiltIn extends BaseMachine implements BuiltIn
{
    /** Used for debugging. */
    private static final java.util.logging.Logger log =
        java.util.logging.Logger.getLogger(DefaultBuiltIn.class.getName());

    /** Enumerates the possible ways in which a variable can be introduced to a clause. */
    protected enum VarIntroduction
    {
        /** Introduced by a get instruction. */
        Get,

        /** Introduced by a put instruction. */
        Put,

        /** Introduced by a set instruction. */
        Set,

        /** Introduced by a unify instruction. */
        Unify
    }

    /** This is used to keep track of registers as they are seen. */
    protected Set<Integer> seenRegisters = new TreeSet<Integer>();

    /** Used to keep track of the temporary register assignment across multiple functors within a clause. */
    protected int lastAllocatedTempReg;

    /**
     * Creates a built-in, with the specified symbol table and name interner.
     *
     * @param symbolTable The symbol table for the compiler and machine.
     * @param interner    The name interner for the compiler and machine.
     */
    public DefaultBuiltIn(SymbolTable<Integer, String, Object> symbolTable, VariableAndFunctorInterner interner)
    {
        super(symbolTable, interner);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyCall(Functor expression, boolean isFirstBody,
        boolean isLastBody, boolean chainRule, int permVarsRemaining)
    {
        // Used to build up the results in.
        SizeableLinkedList<WAMInstruction> instructions = new SizeableLinkedList<WAMInstruction>();

        // Generate the call or tail-call instructions, followed by the call address, which is f_n of the
        // called program.
        if (isLastBody)
        {
            // Deallocate the stack frame at the end of the clause, but prior to calling the last
            // body predicate.
            // This is not required for chain rules, as they do not need a stack frame.
            if (!chainRule)
            {
                instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.Deallocate));
            }

            instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.Execute,
                    interner.getFunctorFunctorName(expression)));
        }
        else
        {
            instructions.add(new WAMInstruction(WAMInstruction.WAMInstructionSet.Call,
                    (byte) (permVarsRemaining & 0xff), interner.getFunctorFunctorName(expression)));
        }

        return instructions;
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor expression, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        // Used to build up the results in.
        SizeableLinkedList<WAMInstruction> instructions = new SizeableLinkedList<WAMInstruction>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        /*if (!isFirstBody)
        {
            lastAllocatedRegister = 0;
        }*/

        allocateArgumentRegisters(expression);
        allocateTemporaryRegisters(expression);

        // Loop over all of the arguments to the outermost functor.
        int numOutermostArgs = expression.getArity();

        for (int j = 0; j < numOutermostArgs; j++)
        {
            Term nextOutermostArg = expression.getArgument(j);
            int allocation =
                (Integer) symbolTable.get(nextOutermostArg.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION);

            byte addrMode = (byte) ((allocation & 0xff00) >> 8);
            byte address = (byte) (allocation & 0xff);

            // On the first occurrence of a variable output a put_var.
            // On a subsequent variable occurrence output a put_val.
            if (nextOutermostArg.isVar() && !seenRegisters.contains(allocation))
            {
                seenRegisters.add(allocation);

                // The variable has been moved into an argument register.
                //varNames.remove((byte) allocation);
                //varNames.put((byte) j, ((Variable) nextOutermostArg).getName());

                /*log.fine("PUT_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                WAMInstruction instruction =
                    new WAMInstruction(WAMInstruction.WAMInstructionSet.PutVar, addrMode, address, (byte) (j & 0xff));
                instructions.add(instruction);

                // Record the way in which this variable was introduced into the clause.
                symbolTable.put(nextOutermostArg.getSymbolKey(), SymbolTableKeys.SYMKEY_VARIABLE_INTRO,
                    VarIntroduction.Put);
            }
            else if (nextOutermostArg.isVar())
            {
                // Check if this is the last body functor in which this variable appears, it does so only in argument
                // position, and this is the first occurrence of these conditions. In which case, an unsafe put is to
                // be used.
                if (isLastBodyTermInArgPositionOnly((Variable) nextOutermostArg, expression) &&
                        (addrMode == WAMInstruction.STACK_ADDR))
                {
                    log.fine("PUT_UNSAFE_VAL " + ((addrMode == WAMInstruction.REG_ADDR) ? "X" : "Y") + address + ", A" +
                        j);

                    WAMInstruction instruction =
                        new WAMInstruction(WAMInstruction.WAMInstructionSet.PutUnsafeVal, addrMode, address,
                            (byte) (j & 0xff));
                    instructions.add(instruction);

                    symbolTable.put(nextOutermostArg.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_LAST_ARG_FUNCTOR, null);
                }
                else
                {
                    /*log.fine("PUT_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    WAMInstruction instruction =
                        new WAMInstruction(WAMInstruction.WAMInstructionSet.PutVal, addrMode, address,
                            (byte) (j & 0xff));
                    instructions.add(instruction);
                }
            }

            // When a functor is encountered, output a put_struc.
            else if (nextOutermostArg.isFunctor())
            {
                Functor nextFunctorArg = (Functor) nextOutermostArg;

                // Heap cells are to be created in an order such that no heap cell can appear before other cells that it
                // refers to. A postfix traversal of the functors in the term to compile is used to achieve this, as
                // child functors in a head will be visited first.
                // Walk over the query term in post-fix order, picking out just the functors.
                QueueBasedSearchMethod<Term, Term> postfixSearch = new PostFixSearch<Term, Term>();
                postfixSearch.reset();
                postfixSearch.addStartState(nextFunctorArg);
                postfixSearch.setGoalPredicate(new FunctorTermPredicate());

                Iterator<Term> treeWalker = Searches.allSolutions(postfixSearch);

                // For each functor encountered: put_struc.
                while (treeWalker.hasNext())
                {
                    Functor nextFunctor = (Functor) treeWalker.next();
                    allocation =
                        (Integer) symbolTable.get(nextFunctor.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION);
                    addrMode = (byte) ((allocation & 0xff00) >> 8);
                    address = (byte) (allocation & 0xff);

                    // Ouput a put_struc instuction, except on the outermost functor.
                    /*log.fine("PUT_STRUC " + interner.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                        ((addrMode == REG_ADDR) ? ", X" : ", Y") + address);*/

                    WAMInstruction instruction =
                        new WAMInstruction(WAMInstruction.WAMInstructionSet.PutStruc, addrMode, address,
                            interner.getDeinternedFunctorName(nextFunctor.getName()), nextFunctor);
                    instructions.add(instruction);

                    // For each argument of the functor.
                    int numArgs = nextFunctor.getArity();

                    for (int i = 0; i < numArgs; i++)
                    {
                        Term nextArg = nextFunctor.getArgument(i);
                        allocation =
                            (Integer) symbolTable.get(nextArg.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION);
                        addrMode = (byte) ((allocation & 0xff00) >> 8);
                        address = (byte) (allocation & 0xff);

                        // If it is new variable: set_var or put_var.
                        // If it is variable or functor already seen: set_val or put_val.
                        if (nextArg.isVar() && !seenRegisters.contains(allocation))
                        {
                            seenRegisters.add(allocation);

                            /*log.fine("SET_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/
                            instruction =
                                new WAMInstruction(WAMInstruction.WAMInstructionSet.SetVar, addrMode, address, nextArg);

                            // Record the way in which this variable was introduced into the clause.
                            symbolTable.put(nextArg.getSymbolKey(), SymbolTableKeys.SYMKEY_VARIABLE_INTRO,
                                VarIntroduction.Set);
                        }
                        else
                        {
                            // Check if the variable is 'local' and use a local instruction on the first occurrence.
                            VarIntroduction introduction =
                                (VarIntroduction) symbolTable.get(nextArg.getSymbolKey(),
                                    SymbolTableKeys.SYMKEY_VARIABLE_INTRO);

                            if (isLocalVariable(introduction, addrMode))
                            {
                                log.fine("SET_LOCAL_VAL " + ((addrMode == WAMInstruction.REG_ADDR) ? "X" : "Y") +
                                    address);

                                instruction =
                                    new WAMInstruction(WAMInstruction.WAMInstructionSet.SetLocalVal, addrMode, address,
                                        nextArg);

                                symbolTable.put(nextArg.getSymbolKey(), SymbolTableKeys.SYMKEY_VARIABLE_INTRO, null);
                            }
                            else
                            {
                                /*log.fine("SET_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/
                                instruction =
                                    new WAMInstruction(WAMInstruction.WAMInstructionSet.SetVal, addrMode, address,
                                        nextArg);
                            }
                        }

                        instructions.add(instruction);
                    }
                }
            }
        }

        return instructions;
    }

    /**
     * For a predicate of arity n, the first n registers are used to receive its arguments in.
     *
     * <p/>Non-variable arguments appearing directly within a functor are allocated to argument registers. This means
     * that they are directly referenced from the argument registers that pass them to predicate calls, or directly
     * referenced from the argument registers that are used to read them as call arguments.
     *
     * <p/>Variables appearing as functor arguments are not allocated in this way, but are kept in registers with
     * positions higher than the number of arguments. The reason for this, is that a variable can appear multiple times
     * in an expression; it may not always be the same argument. Variables are assigned to other registers, then copied
     * into the argument registers as needed.
     *
     * <p/>Argument registers are allocated by argument position within the functor. This means that gaps will be left
     * in the numbering for variables to be copied in as needed.
     *
     * @param expression The clause head functor to allocate argument registers to.
     */
    protected void allocateArgumentRegisters(Functor expression)
    {
        // Assign argument registers to functors appearing directly in the argument of the outermost functor.
        // Variables are never assigned directly to argument registers.
        int reg = 0;

        for (; reg < expression.getArity(); reg++)
        {
            Term term = expression.getArgument(reg);

            if (term instanceof Functor)
            {
                /*log.fine("X" + lastAllocatedRegister + " = " + interner.getFunctorFunctorName((Functor) term));*/

                int allocation = (reg & 0xff) | (WAMInstruction.REG_ADDR << 8);
                symbolTable.put(term.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION, allocation);
            }
        }
    }

    /**
     * Allocates terms within a functor expression to registers. The outermost functor itself is not assigned to a
     * register in WAM (only in l0). Functors already directly assigned to argument registers will not be re-assigned by
     * this. Variables as arguments will be assigned but not as argument registers.
     *
     * @param expression The expression to walk over.
     */
    protected void allocateTemporaryRegisters(Functor expression)
    {
        // Need to assign registers to the whole syntax tree, working in from the outermost functor. The outermost
        // functor itself is not assigned to a register in l3 (only in l0). Functors already directly assigned to
        // argument registers will not be re-assigned by this, variables as arguments will be assigned.
        QueueBasedSearchMethod<Term, Term> outInSearch = new BreadthFirstSearch<Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator<Term> treeWalker = Searches.allSolutions(outInSearch);

        // Discard the outermost functor from the variable allocation.
        treeWalker.next();

        // For each term encountered: set X++ = term.
        while (treeWalker.hasNext())
        {
            Term term = treeWalker.next();

            if (symbolTable.get(term.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION) == null)
            {
                int allocation = (lastAllocatedTempReg++ & 0xff) | (WAMInstruction.REG_ADDR << 8);
                symbolTable.put(term.getSymbolKey(), SymbolTableKeys.SYMKEY_ALLOCATION, allocation);
            }
        }
    }

    /**
     * Determines whether a variable is local, that is, it may only exist on the stack. When variables are introduced
     * into clauses, the way in which they are introduced is recorded using the {@link VarIntroduction} enum. When a
     * variable is being written to the heap for the first time, this check may be used to see if a 'local' variant of
     * an instruction is needed, in order to globalize the variable on the heap.
     *
     * <p/>The conditions for a variable being deemed local are:
     *
     * <ul>
     * <li>For a permanent variable, not initialized in this clause with set_var or unify_var instruction.</li>
     * <li>For a temporary variable, not initialized in this clause with set_var or unify_var or put_var instruction.
     * </li>
     * </ul>
     *
     * @param  introduction The type of instruction that introduced the variable into the clause.
     * @param  addrMode     The addressing mode of the variable, permanent variables are stack allocated.
     *
     * @return <tt>true</tt> iff the variable is unsafe.
     */
    protected boolean isLocalVariable(VarIntroduction introduction, byte addrMode)
    {
        if (WAMInstruction.STACK_ADDR == addrMode)
        {
            return (introduction == VarIntroduction.Get) || (introduction == VarIntroduction.Put);
        }
        else
        {
            return introduction == VarIntroduction.Get;
        }
    }

    /**
     * Checks if a variable is appearing within the last body functor in which it occurs, and only does so within
     * argument position.
     *
     * @param  var  The variable to check.
     * @param  body The current body functor being processed.
     *
     * @return <tt>true</tt> iff this is the last body functor that the variable appears in and does so only in argument
     *         position.
     */
    private boolean isLastBodyTermInArgPositionOnly(Variable var, Functor body)
    {
        return body == symbolTable.get(var.getSymbolKey(), SymbolTableKeys.SYMKEY_VAR_LAST_ARG_FUNCTOR);
    }
}
