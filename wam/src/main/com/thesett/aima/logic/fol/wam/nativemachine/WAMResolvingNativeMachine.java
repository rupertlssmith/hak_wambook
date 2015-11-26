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
package com.thesett.aima.logic.fol.wam.nativemachine;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Set;

import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.machine.WAMInternalRegisters;
import com.thesett.aima.logic.fol.wam.machine.WAMMemoryLayout;
import com.thesett.aima.logic.fol.wam.machine.WAMResolvingMachine;
import com.thesett.common.error.ImplementationUnavailableException;
import com.thesett.common.error.NotImplementedException;
import com.thesett.common.util.SequenceIterator;
import com.thesett.common.util.doublemaps.SymbolTable;
import com.thesett.common.util.doublemaps.SymbolTableImpl;

/**
 * WAMResolvingNativeMachine is a byte code interpreter for WAM implemented externally as native code. The code here is
 * a wrapper for the native library. It provides a method to get an instance of this l3 machine that can throw a checked
 * exception when the native library is not available.
 *
 * <p/>The other interesting feature of this machine, is that the byte code is passed to the native library in a direct
 * byte buffer. The intention is that the compiler (or in the future a loader) outputs compiled code into the machine
 * which holds it in a direct byte buffer. No copying accross will be done prior to execution.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled WAM programs and queries.
 * <tr><td> Provide access to the heap.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMResolvingNativeMachine extends WAMResolvingMachine
{
    /** Used for tracing instruction executions. */
    /* private static final Logger trace = Logger.getLogger("TRACE.WAMResolvingNativeMachine"); */

    /** Defines the initial code area size for the virtual machine. */
    private static final int CODE_SIZE = 10000;

    /** Used to record whether the native implementation library was successfully loaded. */
    private static boolean libraryFound;

    /** Used to record whether an attempt to load the native library has been made. */
    private static boolean libraryLoadAttempted;

    /**
     * Creates a unifying virtual machine for WAM with default heap sizes.
     *
     * @param symbolTable The symbol table for the machine.
     */
    public WAMResolvingNativeMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super(symbolTable);

        // Reset the machine to its initial state.
        reset();
    }

    /**
     * Creates an instance of this machine, loading and checking for availability of the native implementation library
     * as required.
     *
     * @param  symbolTable The symbol table for the machine.
     *
     * @return An instance of the native l3 machine.
     *
     * @throws ImplementationUnavailableException If the native library cannot be loaded and linked.
     */
    public static WAMResolvingNativeMachine getInstance(SymbolTableImpl<Integer, String, Object> symbolTable)
        throws ImplementationUnavailableException
    {
        try
        {
            if (!libraryLoadAttempted)
            {
                libraryLoadAttempted = true;

                System.loadLibrary("aima_native");

                libraryFound = true;
            }

            if (libraryFound)
            {
                return new WAMResolvingNativeMachine(symbolTable);
            }
            else
            {
                throw new ImplementationUnavailableException("The native library could not be found.", null, null,
                    null);
            }
        }
        catch (UnsatisfiedLinkError e)
        {
            libraryFound = false;
            throw new ImplementationUnavailableException("The native library could not be found.", e, null, null);
        }
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public void reset()
    {
        // Clear the code buffer.
        codeBuffer = ByteBuffer.allocateDirect(CODE_SIZE);

        // Reset the native part of the machine.
        nativeReset();

        // Ensure that the overridden reset method of WAMBaseMachine is run too, to clear the call table.
        super.reset();
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public native void nativeReset();

    /**
     * Provides an iterator that generates all solutions on demand as a sequence of variable bindings.
     *
     * @return An iterator that generates all solutions on demand as a sequence of variable bindings.
     */
    public Iterator<Set<Variable>> iterator()
    {
        return new SequenceIterator<Set<Variable>>()
            {
                public Set<Variable> nextInSequence()
                {
                    return resolve();
                }
            };
    }

    /**
     * Sets the maximum number of search steps that a search method may take. If it fails to find a solution before this
     * number of steps has been reached its search method should fail and return null. What exactly constitutes a single
     * step, and the granularity of the step size, is open to different interpretation by different search algorithms.
     * The guideline is that this is the maximum number of states on which the goal test should be performed.
     *
     * @param max The maximum number of states to goal test. If this is zero or less then the maximum number of steps
     *            will not be checked for.
     */
    public void setMaxSteps(int max)
    {
        throw new NotImplementedException();
    }

    /** {@inheritDoc} */
    public IntBuffer getDataBuffer()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public WAMInternalRegisters getInternalRegisters()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    public WAMMemoryLayout getMemoryLayout()
    {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    protected boolean execute(WAMCallPoint callPoint)
    {
        return execute(codeBuffer, callPoint.entryPoint);
    }

    /**
     * {@inheritDoc}
     *
     * <p/>This provides a hook in point at which the machine may, if required, compile the code down below the byte
     * code level.
     */
    protected native void codeAdded(ByteBuffer codeBuffer, int codeOffset, int length);

    /**
     * Executes a compiled byte code returning an indication of whether or not a unification was found.
     *
     * @param  codeBuffer A reference to the byte code buffer to run the machine on.
     * @param  codeOffset The start address of the compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected native boolean execute(ByteBuffer codeBuffer, int codeOffset);

    /** {@inheritDoc} */
    protected native int deref(int a);

    /** {@inheritDoc} */
    protected native int derefStack(int a);

    /**
     * Gets the heap cell tag for the most recent dereference operation.
     *
     * @return The heap cell tag for the most recent dereference operation.
     */
    protected native byte getDerefTag();

    /**
     * Gets the heap call value for the most recent dereference operation.
     *
     * @return The heap call value for the most recent dereference operation.
     */
    protected native int getDerefVal();

    /**
     * Gets the value of the heap cell at the specified location.
     *
     * @param  addr The address to fetch from the heap.
     *
     * @return The heap cell at the specified location.
     */
    protected native int getHeap(int addr);

    /**
     * This is a call back onto the trace logger, that the native code can use to do any trace logging through Java,
     * instead of calling 'printf' for example.
     *
     * @param message The string to log to the tracer.
     */
    private void trace(String message)
    {
        /*trace.fine(message);*/
    }
}
