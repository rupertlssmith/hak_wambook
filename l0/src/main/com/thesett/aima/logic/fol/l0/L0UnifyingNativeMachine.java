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

import java.nio.ByteBuffer;

import com.thesett.common.error.ImplementationUnavailableException;

/**
 * L0UnifyingNativeMachine is a byte code interpreter for L0 implemented externally as native code. The code here is
 * just a wrapper for the native library. It provides a method to get an instance of this l0 machine that can throw a
 * checked exception when the native library is not available.
 *
 * <p/>The other interesting feature of this machine, is that the byte code is passed to the native library in a direct
 * byte buffer. The instructions are copied out to the direct byte buffer from a byte array within the JVM, so there is
 * no speed advantage to doing this. It is done, because in the implementations of languages beyond L0, the byte code
 * machine has a 'code area' which holds the compiled code. The intention is that the compiler (or loader) will output
 * code into a direct byte buffer on the native machine, and no copying accross will be done prior to execution. The
 * direct byte buffer technique was used here to experiment with this idea.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled L0 programs and queires.
 * <tr><td> Provide access to the heap.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L0UnifyingNativeMachine extends L0UnifyingMachine
{
    /** Used to record whether the native implementation library was successfully loaded. */
    private static boolean libraryFound;

    /** Used to record whether an attempt to load the native library has been made. */
    private static boolean libraryLoadAttempted;

    /**
     * Creates an instance of this machine, loading and checking for availability of the native implementation library
     * as required.
     *
     * @return An instance of the native l0 machine.
     *
     * @throws ImplementationUnavailableException If the native library cannot be loaded and linked.
     */
    public static L0UnifyingNativeMachine getInstance() throws ImplementationUnavailableException
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
                return new L0UnifyingNativeMachine();
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
     * Executes a compiled functor returning an indication of whether or not a unification was found.
     *
     * @param  functor The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected boolean execute(L0CompiledFunctor functor)
    {
        ByteBuffer codeBuffer = ByteBuffer.allocateDirect(functor.getCode().length);
        codeBuffer.put(functor.getCode());

        return execute(codeBuffer);
    }

    /**
     * Executes a compiled byte code returning an indication of whether or not a unification was found.
     *
     * @param  code The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected native boolean execute(ByteBuffer code);

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
