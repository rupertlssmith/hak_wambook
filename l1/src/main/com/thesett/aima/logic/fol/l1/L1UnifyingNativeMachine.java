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

import java.nio.ByteBuffer;

import com.thesett.common.error.ImplementationUnavailableException;

/**
 * L1UnifyingNativeMachine is a byte code interpreter for L1 implemented externally as native code. The code here is a
 * wrapper for the native library. It provides a method to get an instance of this l1 machine that can throw a checked
 * exception when the native library is not available.
 *
 * <p/>The other interesting feature of this machine, is that the byte code is passed to the native library in a direct
 * byte buffer. The intention is that the compiler (or in the future a loader) outputs compiled code into the machine
 * which holds it in a direct byte buffer. No copying accross will be done prior to execution.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled L1 programs and queries.
 * <tr><td> Provide access to the heap.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L1UnifyingNativeMachine extends L1UnifyingMachine
{
    /** Defines the initial code area size for the virtual machine. */
    private static final int CODE_SIZE = 10000;

    /** Used to record whether the native implementation library was successfully loaded. */
    private static boolean libraryFound;

    /** Used to record whether an attempt to load the native library has been made. */
    private static boolean libraryLoadAttempted;

    /**
     * Holds the buffer of executable code in a direct byte buffer so that no copying out to the native machine needs to
     * be done.
     */
    ByteBuffer codeBuffer;

    /** Creates a unifying virtual machine for L1 with default heap sizes. */
    public L1UnifyingNativeMachine()
    {
        // Reset the machine to its initial state.
        reset();
    }

    /**
     * Creates an instance of this machine, loading and checking for availability of the native implementation library
     * as required.
     *
     * @return An instance of the native l1 machine.
     *
     * @throws ImplementationUnavailableException If the native library cannot be loaded and linked.
     */
    public static L1UnifyingNativeMachine getInstance() throws ImplementationUnavailableException
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
                return new L1UnifyingNativeMachine();
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
     * Adds compiled byte code to the code area of the machine.
     *
     * @param  code        The compiled byte code.
     * @param  start       The start offset within the compiled code to copy into the machine.
     * @param  end         The end offset within the compiled code to copy into the machine.
     * @param  functorName The interned name of the functor that the code is for.
     * @param  isQuery     <tt>true</tt> if the code is for a query, <tt>false</ff> if it is for a program.
     *
     * @return The call table entry for the functors code within the code area of the machine.
     */
    public L1CallTableEntry addCode(byte[] code, int start, int end, int functorName, boolean isQuery)
    {
        // Keep track of the offset into which the code was loaded.
        int entryPoint = codeBuffer.position();

        // Copy the code into the code area.
        int length = end - start;
        codeBuffer.put(code, start, length);

        // If the code is for a program, store the programs entry point in the call table.
        if (!isQuery)
        {
            return setCodeAddress(functorName, entryPoint, length);
        }

        // If the code is for a query, return a call table entry for it but not stored in the call table, as queries
        // do the calling and are not callable themselves.
        else
        {
            return new L1CallTableEntry(entryPoint, length, functorName);
        }
    }

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callTableEntry The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] getByteCode(L1CallTableEntry callTableEntry)
    {
        byte[] result = new byte[callTableEntry.length];

        codeBuffer.get(result, callTableEntry.entryPoint, callTableEntry.length);

        return result;
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

        // Ensure that the overridden reset method of L1BaseMachine is run too, to clear the call table.
        super.reset();
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public native void nativeReset();

    /**
     * Executes a compiled functor returning an indication of whether or not a unification was found.
     *
     * @param  functor The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected boolean execute(L1CompiledFunctor functor)
    {
        return execute(codeBuffer, functor.callTableEntry.entryPoint);
    }

    /**
     * Executes a compiled byte code returning an indication of whether or not a unification was found.
     *
     * @param  codeBuffer A reference to the byte code buffer to run the machine on.
     * @param  codeOffset The start address of the compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected native boolean execute(ByteBuffer codeBuffer, int codeOffset);

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
    protected native int deref(int a);

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
}
