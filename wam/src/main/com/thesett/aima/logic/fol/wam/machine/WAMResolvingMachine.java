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

import java.nio.ByteBuffer;
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
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledPredicate;
import com.thesett.aima.logic.fol.wam.compiler.WAMCompiledQuery;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.REF;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.STR;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * A {@link Resolver} implements a resolution (or proof) procedure over logical clauses. This abstract class is the root
 * of all resolvers that operate over compiled clauses in the WAM language. Implementations may interpret the compiled
 * code directly, or further reduce it into more efficient binary forms.
 *
 * <p/>Clauses to be queried over, have their binary byte code inserted into the machine, in preparation for calling by
 * queries. Queries also have their binary byte code inserted into the machine, and a reference to the most recently
 * inserted query is retained for invocation by the search method.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Resolve a query over a set of compiled Horn clauses in the WAM language.
 * <tr><td> Decode results into an abstract source tree from the binary heap format. <td> {@link Term}.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class WAMResolvingMachine extends WAMBaseMachine
    implements Resolver<WAMCompiledPredicate, WAMCompiledQuery>, WAMResolvingMachineDPI
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(WAMResolvingMachine.class.getName()); */

    /** Static counter for inventing new variable names. */
    protected static AtomicInteger varNameId = new AtomicInteger();

    /** Holds the most recently set query, to run when the resolution search is invoked. */
    protected WAMCompiledQuery currentQuery;

    /**
     * Holds the buffer of executable code in a direct byte buffer so that no copying out to the native machine needs to
     * be done.
     */
    protected ByteBuffer codeBuffer;

    /** Holds the abstract machine debugging monitor, or <tt>null</tt> if none is attached. */
    protected WAMResolvingMachineDPIMonitor monitor;

    /**
     * Creates a resolving machine with the specified symbol table.
     *
     * @param symbolTable The symbol table.
     */
    protected WAMResolvingMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super(symbolTable);
    }

    /** {@inheritDoc} */
    public void emmitCode(WAMCompiledPredicate predicate) throws LinkageException
    {
        // Keep track of the offset into which the code was loaded.
        int entryPoint = codeBuffer.position();
        int length = (int) predicate.sizeof();

        // If the code is for a program clause, store the programs entry point in the call table.
        WAMCallPoint callPoint = setCodeAddress(predicate.getName(), entryPoint, length);

        // Emmit code for the clause into this machine.
        predicate.emmitCode(codeBuffer, this, callPoint);

        // Notify the native machine of the addition of new code.
        codeAdded(codeBuffer, entryPoint, length);

        // Notify any attached DPI monitor of the addition of new code.
        if (monitor != null)
        {
            monitor.onCodeUpdate(this, entryPoint, length);
        }
    }

    /** {@inheritDoc} */
    public void emmitCode(WAMCompiledQuery query) throws LinkageException
    {
        // Keep track of the offset into which the code was loaded.
        int entryPoint = codeBuffer.position();
        int length = (int) query.sizeof();

        // If the code is for a program clause, store the programs entry point in the call table.
        WAMCallPoint callPoint = new WAMCallPoint(entryPoint, length, -1);

        // Emmit code for the clause into this machine.
        query.emmitCode(codeBuffer, this, callPoint);

        // Notify the native machine of the addition of new code.
        codeAdded(codeBuffer, entryPoint, length);

        // Notify any attached DPI monitor of the addition of new code.
        if (monitor != null)
        {
            monitor.onCodeUpdate(this, entryPoint, length);
        }
    }

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] retrieveCode(WAMCallPoint callPoint)
    {
        byte[] result = new byte[callPoint.length];

        codeBuffer.get(result, callPoint.entryPoint, callPoint.length);

        return result;
    }

    /** {@inheritDoc} */
    public void emmitCode(int offset, int address)
    {
        codeBuffer.putInt(offset, address);
    }

    /** {@inheritDoc} */
    public void addToDomain(WAMCompiledPredicate term) throws LinkageException
    {
        /*log.fine("public void addToDomain(WAMCompiledClause term = " + term + "): called");*/

        // Emmit code for the term into this machine.
        emmitCode(term);
    }

    /** {@inheritDoc} */
    public void setQuery(WAMCompiledQuery query) throws LinkageException
    {
        /*log.fine("public void setQuery(WAMCompiledClause query = " + query + "): called");*/

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

    /** {@inheritDoc} */
    public void attachMonitor(WAMResolvingMachineDPIMonitor monitor)
    {
        this.monitor = monitor;
    }

    /** {@inheritDoc} */
    public ByteBuffer getCodeBuffer(int start, int length)
    {
        // Take a read only slice onto an appropriate section of the code buffer.
        ByteBuffer readOnlyBuffer = codeBuffer.asReadOnlyBuffer();
        readOnlyBuffer.position(start);
        readOnlyBuffer.limit(start + length);

        return readOnlyBuffer;
    }

    /** {@inheritDoc} */
    public VariableAndFunctorInterner getVariableAndFunctorInterner()
    {
        return this;
    }

    /**
     * Notified whenever code is added to the machine.
     *
     * @param codeBuffer The code buffer.
     * @param codeOffset The start offset of the new code.
     * @param length     The length of the new code.
     */
    protected abstract void codeAdded(ByteBuffer codeBuffer, int codeOffset, int length);

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
    protected abstract boolean execute(WAMCallPoint callPoint);

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
    protected Set<Variable> executeAndExtractBindings(WAMCompiledQuery query)
    {
        // Execute the query and program. The starting point for the execution is the first functor in the query
        // body, this will follow on to the subsequent functors and make calls to functors in the compiled programs.
        boolean success = execute(query.getCallPoint());

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
        Term result = null;

        // Dereference the initial heap pointer.
        int addr = deref(start);
        byte tag = getDerefTag();
        int val = getDerefVal();

        /*log.fine("addr = " + addr);*/
        /*log.fine("tag = " + tag);*/
        /*log.fine("val = " + val);*/

        switch (tag)
        {
        case REF:
        {
            // Check if a variable for the address has already been created in this context, and use it if so.
            Variable var = variableContext.get(val);

            if (var == null)
            {
                var = new Variable(varNameId.decrementAndGet(), null, false);

                variableContext.put(val, var);
            }

            result = var;

            break;
        }

        case STR:
        {
            // Decode f/n from the STR data.
            int fn = getHeap(val);
            int f = fn & 0x00ffffff;

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

            break;
        }

        case WAMInstruction.CON:
        {
            //Decode f/n from the CON data.
            int f = val & 0x3fffffff;

            /*log.fine("fn = " + fn);*/
            /*log.fine("f = " + f);*/

            // Create a new functor to hold the decoded data.
            result = new Functor(f, null);

            break;
        }

        case WAMInstruction.LIS:
        {
            FunctorName functorName = new FunctorName("cons", 2);
            int f = internFunctorName(functorName);

            // Fill in this functors name and arity and allocate storage space for its arguments.
            int arity = functorName.getArity();
            Term[] arguments = new Term[arity];

            // Loop over all of the functors arguments, recursively decoding them.
            for (int i = 0; i < arity; i++)
            {
                arguments[i] = decodeHeap(val + i, variableContext);
            }

            // Create a new functor to hold the decoded data.
            result = new Functor(f, arguments);

            break;
        }

        default:
            throw new IllegalStateException("Encountered unknown tag type on the heap.");
        }

        return result;
    }
}
