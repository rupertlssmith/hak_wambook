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
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.REF;

/**
 * L0UnifyingMachine is a unifier for the first order logical language L0. It unifies a query against all of its stored
 * programs, outputing the unifications that it finds.
 *
 * <p/>The {@link L0Compiler} outputs byte code that this machine understands. All instructions have one byte for the
 * opcode, and one byte for the register. The put_struc and get_struc instructions also take an additional functor
 * reference which is an integer enumeration of the functor's particular name and arity. The instruction size and format
 * is therefore, two bytes, followed by an optional 4-byte integer.
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
public abstract class L0UnifyingMachine extends L0BaseMachine implements Unifier<L0CompiledFunctor>
{
    /** Used for debugging. */
    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(L0UnifyingMachine.class.getName());

    /** Static counter for inventing new variable names. */
    protected static final AtomicInteger varNameId = new AtomicInteger();

    /**
     * Unifies two terms and produces a list of bound variables that form the unification, when it it possible. The
     * first term to unify must be a compiled L0 query, and the second term to unify against must be a compiled L0
     * program. The query will be run first, to build up its heap representation, and then the program will be run to
     * unify against the query on the heap.
     *
     * @param  left  The left term to unify.
     * @param  right The right term to unify.
     *
     * @return A list of bound variables to form the unification, or <tt>null</tt> when no unification is possible.
     */
    public List<Variable> unify(L0CompiledFunctor left, L0CompiledFunctor right)
    {
        // Check that the first argument is a compiled L0 query.
        if (!(left instanceof L0CompiledQueryFunctor))
        {
            throw new IllegalArgumentException("The first unification argument must be a compiled L0 query.");
        }

        // Check that the second argument is a compiled L0 program.
        if (!(right instanceof L0CompiledProgramFunctor))
        {
            throw new IllegalArgumentException("The second unification argument must be a compiled L0 program.");
        }

        // Execute the query and program.
        execute(left);

        boolean unified = execute(right);

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
            for (byte reg : left.varNames.keySet())
            {
                int varName = left.varNames.get(reg);

                int addr = deref(reg);
                Term term = decodeHeap(addr, varContext);

                results.add(new Variable(varName, term, false));
            }
        }

        return results;
    }

    /**
     * Executes a compiled functor returning an indication of whether or not a unification was found.
     *
     * @param  functor The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected abstract boolean execute(L0CompiledFunctor functor);

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
            Term functor = new Functor(f, arguments);

            result = functor;
        }

        return result;
    }
}
