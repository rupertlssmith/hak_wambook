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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Unifier;
import com.thesett.aima.logic.fol.Variable;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.REF;

/**
 * L1UnifyingMachine is a unifier for the first order logical language L1. It unifies a query against all of its stored
 * programs, outputing the unifications that it finds.
 *
 * <p/>The {@link L1Compiler} outputs byte code that this {@link L1Machine} understands. All instructions have one byte
 * for the opcode, and one byte for the register. The put_struc and get_struc instructions also take an additional
 * functor reference which is an integer enumeration of the functor's particular name and arity. The instruction size
 * and format is therefore, two bytes, followed by an optional 4-byte integer.
 *
 * <p/>This machine builds up queries to be unified on the heap, then executes a program against that heap, to check for
 * unification.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Unify a query against a statement, binding variables in the query.
 * <tr><td> Decode results into an abstract source tree from the binary heap format. <td> {@link Term}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class L1UnifyingMachine extends L1BaseMachine implements Unifier<L1CompiledFunctor>
{
    /** Used for debugging. */
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(L1UnifyingMachine.class.getName());

    /** Static counter for inventing new variable names. */
    protected static final AtomicInteger varNameId = new AtomicInteger();

    /**
     * Unifies a query against the programs stored in the machine and produces a list of bound variables that form the
     * unification, when it it possible. The term to unify must be a compiled L1 query. The query will be run and it
     * will call the program that matches it, to unify against the query representation on the heap.
     *
     * @param  query The term to unify against programs in the machine.
     *
     * @return A list of bound variables to form the unification, or <tt>null</tt> when no unification is possible.
     */
    public List<Variable> unify(L1CompiledFunctor query)
    {
        // Check that the first argument is a compiled L1 query.
        if (!(query instanceof L1CompiledQueryFunctor))
        {
            throw new IllegalArgumentException("The first unification argument must be a compiled L1 query.");
        }

        // Execute the query and program.
        boolean unified = execute(query);

        // Used to collect the results in.
        List<Variable> results = null;

        // Collect the results only if the unification was successfull.
        if (unified)
        {
            results = new ArrayList<Variable>();

            // The same variable context is used accross all of the results, for common use of variables in the
            // results.
            Map<Integer, Variable> varContext = new HashMap<Integer, Variable>();

            // For each of the free variables in the query, extract its value from the location on the heap pointed to
            // by the register that holds the variable.
            for (byte reg : query.varNames.keySet())
            {
                int varName = query.varNames.get(reg);

                int addr = deref(reg);
                Term term = decodeHeap(addr, varContext);

                results.add(new Variable(varName, term, false));
            }
        }

        return results;
    }

    /**
     * Unifies two terms and produces a list of variable binding of the free variables in the left term, that form the
     * unification, when it it possible. Note that the returned list of bindings should not contain bindings for free
     * variables in the right hand term. For example:
     *
     * <p/>
     * <pre>
     * Unifying f(X) with f(x) should return X = x.
     * Unifying f(x) with f(X) should succeed but return an empty list of bindings.
     * </pre>
     *
     * <p/>Note that the {@link L1Compiler} compiles its code into the machine, and queries are compiled to call
     * matching programs. Therefore the program specified as the right hand argument will already be available in the
     * machine, so this argument is ignored.
     *
     * @param  left  The left term to unify, this is the query with variables to bind.
     * @param  right The right term to unify, this is the statement to unify against. Ignored, as assuming this
     *               statement is already compiled into the machine.
     *
     * @return A list of bound variables to form the unification, or <tt>null</tt> when no unification is possible.
     */
    public List<Variable> unify(L1CompiledFunctor left, L1CompiledFunctor right)
    {
        // Copy the program out of the machine.

        // Reset the machine.

        // Copy the program back into the machine.

        return unify(left);
    }

    /**
     * Executes a compiled functor returning an indication of whether or not a unification was found.
     *
     * @param  functor The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected abstract boolean execute(L1CompiledFunctor functor);

    /**
     * Dereferences a heap pointer (or register), returning the address that it refers to after following all reference
     * chains to their conclusion. This method is also side effecting, in that the contents of the refered to heap cell
     * are also loaded into fields and made available through the {@link #getDerefTag()} and {@link #getDerefVal()}
     * methods.
     *
     * @param  a The address to dereference.
     *
     * @return The address that the reference refers to.
     */
    protected abstract int deref(int a);

    /**
     * Gets the heap cell tag for the most recent dereference operation.
     *
     * @return The heap cell tag for the most recent dereference operation.
     */
    protected abstract byte getDerefTag();

    /**
     * Gets the heap call value for the most recent dereference operation.
     *
     * @return The heap call value for the most recent dereference operation.
     */
    protected abstract int getDerefVal();

    /**
     * Gets the value of the heap cell at the specified location.
     *
     * @param  addr The address to fetch from the heap.
     *
     * @return The heap cell at the specified location.
     */
    protected abstract int getHeap(int addr);

    /**
     * Decodes a term from the raw byte representation on the machines heap, into an abstract syntax tree.
     *
     * @param  start           The start offset of the term on the heap.
     * @param  variableContext The variable context for the decoded variables. This may be shared amongst all variables
     *                         decoded for a particular unifcation.
     *
     * @return The term decoded from its heap representation.
     */
    private Term decodeHeap(int start, Map<Integer, Variable> variableContext)
    {
        /*log.fine("private Term decodeHeap(int start = " + start + ", Map<Integer, Variable> variableContext = " +
            variableContext + "): called");*/

        // Used to hold the decoded argument in.
        Term result;

        // Dereference the initial heap pointer.
        int addr = deref(start);
        byte tag = getDerefTag();
        int val = getDerefVal();

        /*log.fine("addr = " + addr);*/
        /*log.fine("tag = " + tag);*/
        /*log.fine("val = " + val);*/

        // If a variable is encountered dereference it.
        if (tag == REF)
        {
            // Check if a variable for the address has already been created in this context, and use it if so.
            Variable var = variableContext.get(val);

            if (var == null)
            {
                var = new Variable(varNameId.decrementAndGet(), null, false);

                variableContext.put(val, var);
            }

            result = var;
        }

        // If the next or dereferenced cell is a functor create a new functor.
        else // if (deref_tag == STR)
        {
            // Decode f/n from the STR data.
            int fn = getHeap(val);
            int f = (fn & 0xFFFFFF00) >> 8;

            /*log.fine("fn = " + fn);*/
            /*log.fine("f = " + f);*/

            // Look up and initialize this functor name from the symbol table.
            FunctorName functorName = getDeinternedFunctorName(f);

            // Fill in this functors name and arity and allocate storage space for its arguments.
            int arity = functorName.getArity();
            Term[] arguments = new Term[arity];

            // Loop over all of the functors arguments, recursively decoding them.
            for (int i = 0; i < arity; i++)
            {
                arguments[i] = decodeHeap(val + 1 + i, variableContext);
            }

            // Create a new functor to hold the decoded data.
            Functor functor = new Functor(f, arguments);

            result = functor;
        }

        return result;
    }
}
