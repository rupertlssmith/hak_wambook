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
package com.thesett.aima.logic.fol.l3;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Resolver;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import static com.thesett.aima.logic.fol.l3.L3Instruction.REF;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * A {@link Resolver} implements a resolution (or proof) procedure over logical clauses. This abstract class is the root
 * of all resolvers that operate over compiled clauses in the L3 language. Implementations may interpret the compiled
 * code directly, or further reduce it into more efficient binary forms.
 *
 * <p/>Clauses to be queried over, have their binary byte code inserted into the machine, in preparation for calling by
 * queries. Queries also have their binary byte code inserted into the machine, and a reference to the most recently
 * inserted query is retained for invokation by the search method.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Resolve a query over a set of compiled Horn clauses in the L3 language.
 * <tr><td> Decode results into an abstract source tree from the binary heap format. <td> {@link Term}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class L3ResolvingMachine extends L3BaseMachine implements Resolver<L3CompiledPredicate, L3CompiledQuery>
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L3ResolvingMachine.class.getName()); */

    /** Static counter for inventing new variable names. */
    protected static final AtomicInteger varNameId = new AtomicInteger();

    /** Holds the most recently set query, to run when the resolution search is invoked. */
    L3CompiledQuery currentQuery;

    /**
     * Creates a resolving machine with the specified symbol table.
     *
     * @param symbolTable The symbol table.
     */
    protected L3ResolvingMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super(symbolTable);
    }

    /** {@inheritDoc} */
    public void addToDomain(L3CompiledPredicate term) throws LinkageException
    {
        /*log.fine("public void addToDomain(L3CompiledClause term = " + term + "): called");*/

        // Emmit code for the term into this machine.
        emmitCode(term);
    }

    /** {@inheritDoc} */
    public void setQuery(L3CompiledQuery query) throws LinkageException
    {
        /*log.fine("public void setQuery(L3CompiledClause query = " + query + "): called");*/

        // Emmit code for the clause into this machine.
        emmitCode(query);

        // Keep hold of the query to run.
        currentQuery = query;
    }

    /** {@inheritDoc} */
    public Set<Variable> resolve()
    {
        // Check that a query has been set to resolve.
        if (currentQuery == null)
        {
            throw new IllegalStateException("No query set to resolve.");
        }

        // Execute the byte code, starting from the first functor of the query.
        return executeAndExtractBindings(currentQuery);
    }

    /**
     * Dereferences an offset from the current environment frame on the stack. Storage slots in the current environment
     * may point to other environment frames, but should not contain unbound variables, so ultimately this dereferencing
     * should resolve onto a structure or variable on the heap.
     *
     * @param  a The offset into the current environment stack frame to dereference.
     *
     * @return The dereferences structure or variable.
     */
    protected abstract int derefStack(int a);

    /**
     * Executes compiled code at the specified call point returning an indication of whether or not the execution was
     * succesfull.
     *
     * @param  callPoint The call point of the compiled byte code to execute.
     *
     * @return <tt>true</tt> iff execution succeeded.
     */
    protected abstract boolean execute(L3CallPoint callPoint);

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
     * Runs a query, and for every non-anonymous variable in the query, decodes its binding value from the heap and
     * returns it in a set of variable bindings.
     *
     * @param  query The query to execute.
     *
     * @return A set of variable bindings resulting from the query.
     */
    protected Set<Variable> executeAndExtractBindings(L3CompiledQuery query)
    {
        // Execute the query and program. The starting point for the execution is the first functor in the query
        // body, this will follow on to the subsequent functors and make calls to functors in the compiled programs.
        boolean success = execute(query.callPoint);

        // Used to collect the results in.
        Set<Variable> results = null;

        // Collect the results only if the resolution was successfull.
        if (success)
        {
            results = new HashSet<Variable>();

            // The same variable context is used accross all of the results, for common use of variables in the
            // results.
            Map<Integer, Variable> varContext = new HashMap<Integer, Variable>();

            // For each of the free variables in the query, extract its value from the location on the heap pointed to
            // by the register that holds the variable.
            /*log.fine("query.getVarNames().size() =  " + query.getVarNames().size());*/

            for (byte reg : query.getVarNames().keySet())
            {
                int varName = query.getVarNames().get(reg);

                if (query.getNonAnonymousFreeVariables().contains(varName))
                {
                    int addr = derefStack(reg);
                    Term term = decodeHeap(addr, varContext);

                    results.add(new Variable(varName, term, false));
                }
            }
        }

        return results;
    }

    /**
     * Decodes a term from the raw byte representation on the machines heap, into an abstract syntax tree.
     *
     * @param  start           The start offset of the term on the heap.
     * @param  variableContext The variable context for the decoded variables. This may be shared amongst all variables
     *                         decoded for a particular unifcation.
     *
     * @return The term decoded from its heap representation.
     */
    protected Term decodeHeap(int start, Map<Integer, Variable> variableContext)
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
            result = new Functor(f, arguments);
        }

        return result;
    }
}
