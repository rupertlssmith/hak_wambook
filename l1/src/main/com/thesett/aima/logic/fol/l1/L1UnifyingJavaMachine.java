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

import static com.thesett.aima.logic.fol.l1.L1InstructionSet.CALL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PROCEED;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.REF;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.STR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAR;
import com.thesett.common.util.ByteBufferUtils;

/**
 * L1UnifyingJavaMachine is a byte code interpreter for L1 written in java. This is a direct implementation of the
 * instruction interpretations given in "Warren's Abstract Machine: A Tutorial Reconstruction". The pseudo algorithm
 * presented there can be read in the comments interspersed with the code. There are a couple of small differences:
 *
 * <p/>
 * <ul>
 * <li>The book describes a STORE[addr] operation that loads or stores either a heap or register addresses. To simplify
 * implementing this, the registers are placed at the top of the heap, and the initial heap pointer set below the
 * registers. The STORE operation translates exactly into an array lookup on the heap.</li>
 * <li>The deref operation is presented in the book as a recursive function. It was turned into an equivalent iterative
 * looping function instead. The deref operation returns multiple parameters, but as Java only supports single return
 * types, a choice had to be made between creating a simple class to hold the return types, or storing the return values
 * in member variables, and reading them from there. The member variables solution was chosen.</li>
 * </ul>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled L1 programs and queries.
 * <tr><td> Provide access to the heap.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Think about unloading of byte code as well as insertion of of byte code. For example, would it be possible to
 *         unload a program, and replace it with a different one? This would require re-linking of any references to the
 *         original. So maybe want to add an index to reverse references. Call instructions should have their jumps
 *         pre-calculated for speed, but perhaps should also put the bare f/n into them, for the case where they may
 *         need to be updated. Also, add a semaphore to all call instructions, or at the entry point of all programs,
 *         this would be used to synchronize live updates to programs in a running machine, as well as to add debugging
 *         break points.
 * @todo   Think about ability to grow (and shrink?) the heap. Might be best to do this at the same time as the first
 *         garbage collector.
 */
public class L1UnifyingJavaMachine extends L1UnifyingMachine
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L1UnifyingJavaMachine.class.getName()); */

    /** Used for tracing instruction executions. */
    /* private static final Logger trace = Logger.getLogger("TRACE.L1UnifyingJavaMachine"); */

    /** Defines the heap size to use for the virtual machine. */
    private static final int HEAP_SIZE = 10000;

    /** Defines the register capacity for the virtual machine. */
    private static final int REG_SIZE = 10;

    /** Defines the max unification stack depth for the virtual machine. */
    private static final int USTACK_SIZE = 1000;

    /** Defines the initial code area size for the virtual machine. */
    private static final int CODE_SIZE = 10000;

    /** Holds the byte code. */
    private byte[] code;

    /** Holds the current load offset within the code area. */
    private int loadPoint;

    /** Holds the current instruction pointer into the code. */
    private int ip;

    /** Holds the working heap. */
    private int[] heap;

    /** Holds the heap pointer. */
    private int hp;

    /** Holds the secondary heap pointer, used for the heap address of the next term to match. */
    private int sp;

    /** Holds the unification stack. */
    private int[] ustack;

    /** Holds the unification stack pointer. */
    private int up;

    /** Used to record whether the machine is in structure read or write mode. */
    private boolean writeMode;

    /** Holds the heap cell tag from the most recent dereference. */
    private byte derefTag;

    /** Holds the heap call value from the most recent dereference. */
    private int derefVal;

    /** Creates a unifying virtual machine for L1 with default heap sizes. */
    public L1UnifyingJavaMachine()
    {
        // Reset the machine to its initial state.
        reset();
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
        int entryPoint = loadPoint;

        // Copy the code into the code area.
        int length = end - start;
        System.arraycopy(code, start, this.code, loadPoint, length);
        loadPoint += length;

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

        System.arraycopy(code, callTableEntry.entryPoint, result, 0, callTableEntry.length);

        return result;
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public void reset()
    {
        // Create fresh heaps, code areas and stacks.
        heap = new int[REG_SIZE + HEAP_SIZE];
        ustack = new int[USTACK_SIZE];
        code = new byte[CODE_SIZE];

        // Registers are on the top of the heap, so initialize the heap pointers to the heap area.
        hp = REG_SIZE;
        sp = REG_SIZE;

        // Clear the unification stack.
        up = -1;

        // Turn off write mode.
        writeMode = false;

        // Reset the instruction pointer to that start of the code area, ready for fresh code to be loaded there.
        ip = 0;
        loadPoint = 0;

        // Could probably not bother resetting these, but will do it anyway just to be sure.
        derefTag = 0;
        derefVal = 0;

        // Ensure that the overridden reset method of L1BaseMachine is run too, to clear the call table.
        super.reset();
    }

    /**
     * Executes a compiled functor returning an indication of whether or not a unification was found.
     *
     * @param  functor The compiled byte code to execute.
     *
     * @return <tt>true</tt> if a unification was found, <tt>false</tt> if the search for one failed.
     */
    protected boolean execute(L1CompiledFunctor functor)
    {
        /*log.fine("protected boolean execute(L1CompiledFunctor functor): called");*/

        ip = functor.getCallTableEntry().entryPoint;
        uClear();

        boolean failed = false;

        while (!failed && (ip < code.length))
        {
            // Grab next instruction and switch on it.
            byte instruction = code[ip++];
            int xi = (int) code[ip++];

            switch (instruction)
            {
            // put_struc Xi:
            case PUT_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

                /*trace.fine((ip - 6) + ": PUT_STRUC X" + xi + ", " + fn);*/

                // heap[h] <- STR, h + 1
                heap[hp] = (L1InstructionSet.STR << 24) | ((hp + 1) & 0xFFFFFF);

                // heap[h+1] <- f/n
                heap[hp + 1] = fn;

                // Xi <- heap[h]
                heap[xi] = heap[hp];

                // h <- h + 2
                hp += 2;

                break;
            }

            // set_var Xi:
            case SET_VAR:
            {
                /*trace.fine((ip - 2) + ": SET_VAR X" + xi);*/

                // heap[h] <- REF, h
                heap[hp] = (L1InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                // Xi <- heap[h]
                heap[xi] = heap[hp];

                // h <- h + 1
                hp++;

                break;
            }

            // set_val Xi:
            case SET_VAL:
            {
                /*trace.fine((ip - 2) + ": SET_VAL X" + xi);*/

                // heap[h] <- Xi
                heap[hp] = heap[xi];

                // h <- h + 1
                hp++;

                break;
            }

            // get_struc Xi,
            case GET_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

                /*trace.fine((ip - 6) + ": GET_STRUC X" + xi + ", " + fn);*/

                // addr <- deref(Xi);
                int addr = deref(xi);

                // switch STORE[addr]
                // int tmp = heap[addr];
                // byte tag = (byte)((tmp & 0xFF000000) >> 24);
                // int a = tmp & 0x00FFFFFF;
                byte tag = derefTag;
                int a = derefVal;

                switch (tag)
                {
                // case REF:
                case REF:
                {
                    // heap[h] <- STR, h + 1
                    heap[hp] = (L1InstructionSet.STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    heap[hp + 1] = fn;

                    // bind(addr, h)
                    heap[addr] = (L1InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                    // h <- h + 2
                    hp += 2;

                    // mode <- write
                    writeMode = true;

                    break;
                }

                // case STR, a:
                case STR:
                {
                    // if heap[a] = f/n
                    if (heap[a] == fn)
                    {
                        // s <- a + 1
                        sp = a + 1;

                        // mode <- read
                        writeMode = false;
                    }
                    else
                    {
                        // fail
                        failed = true;
                    }

                    break;
                }

                default:
                {
                    throw new RuntimeException("Unkown tag type.");
                }
                }

                break;
            }

            // unify_var Xi:
            case UNIFY_VAR:
            {
                /*trace.fine((ip - 2) + ": UNIFY_VAR X" + xi);*/

                // switch mode
                if (!writeMode)
                {
                    // case read:
                    // Xi <- heap[s]
                    heap[xi] = heap[sp];

                }
                else
                {
                    // case write:
                    // heap[h] <- REF, h
                    heap[hp] = (L1InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                    // Xi <- heap[h]
                    heap[xi] = heap[hp];

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                break;
            }

            // unify_val Xi:
            case UNIFY_VAL:
            {
                /*trace.fine((ip - 2) + ": UNIFY_VAL X" + xi);*/

                // switch mode
                if (!writeMode)
                {
                    // case read:
                    // unify (Xi, s)
                    failed = !unify(xi, sp);
                }
                else
                {
                    // case write:
                    // heap[h] <- Xi
                    heap[hp] = heap[xi];

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                break;
            }

            // put_var Xn, Ai:
            case PUT_VAR:
            {
                // grab Ai
                byte ai = code[ip++];

                /*trace.fine((ip - 3) + ": PUT_VAR X" + xi + ", A" + ai);*/

                // heap[h] <- REF, H
                heap[hp] = (L1InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                // Xn <- heap[h]
                heap[xi] = heap[hp];

                // Ai <- heap[h]
                heap[ai] = heap[hp];

                // h <- h + 1
                hp++;

                break;
            }

            // put_val Xn, Ai:
            case PUT_VAL:
            {
                // grab Ai
                byte ai = code[ip++];

                /*trace.fine((ip - 3) + ": PUT_VAL X" + xi + ", A" + ai);*/

                // Ai <- Xn
                heap[ai] = heap[xi];

                break;
            }

            // get var Xn, Ai:
            case GET_VAR:
            {
                // grab Ai
                byte ai = code[ip++];

                /*trace.fine((ip - 3) + ": GET_VAR X" + xi + ", A" + ai);*/

                // Xn <- Ai
                heap[xi] = heap[ai];

                break;
            }

            // get_val Xn, Ai:
            case GET_VAL:
            {
                // grab Ai
                byte ai = code[ip++];

                /*trace.fine((ip - 3) + ": GET_VAL X" + xi + ", A" + ai);*/

                // unify (Xn, Ai)
                failed = !unify(xi, ai);

                break;
            }

            // call @(p/n)
            case CALL:
            {
                // grab @(p/n) (ip is decremented here, because already took first byte of the address as xi).
                int pn = ByteBufferUtils.getIntFromBytes(code, ip - 1);

                /*trace.fine((ip - 2) + ": CALL " + pn);*/

                // Ensure that the predicate to call is known and linked int, otherwise fail.
                if (pn == -1)
                {
                    failed = true;

                    break;
                }

                // ip <- @(p/n)
                ip = pn;

                break;
            }

            case PROCEED:
            {
                /*trace.fine((ip - 2) + ": PROCEED");*/
                // noop.

                return !failed;
            }

            default:
            {
                throw new RuntimeException("Unkown instruction type.");
            }
            }
        }

        return !failed;
    }

    /**
     * Dereferences a heap pointer (or register), returning the address that it refers to after following all reference
     * chains to their conclusion. This method is also side effecting, in that the contents of the refered to heap cell
     * are also loaded into the fields {@link #derefTag} and {@link #derefVal}.
     *
     * @param  a The address to dereference.
     *
     * @return The address that the reference refers to.
     */
    protected int deref(int a)
    {
        // tag, value <- STORE[a]
        int addr = a;
        int tmp = heap[a];
        derefTag = (byte) ((tmp & 0xFF000000) >> 24);
        derefVal = tmp & 0x00FFFFFF;

        // while tag = REF and value != a
        while ((derefTag == L1InstructionSet.REF))
        {
            // tag, value <- STORE[a]
            addr = derefVal;
            tmp = heap[derefVal];
            derefTag = (byte) ((tmp & 0xFF000000) >> 24);
            tmp = tmp & 0x00FFFFFF;

            // Break on free var.
            if (derefVal == tmp)
            {
                break;
            }

            derefVal = tmp;
        }

        return addr;
    }

    /**
     * Gets the heap cell tag for the most recent dereference operation.
     *
     * @return The heap cell tag for the most recent dereference operation.
     */
    protected byte getDerefTag()
    {
        return derefTag;
    }

    /**
     * Gets the heap cell value for the most recent dereference operation.
     *
     * @return The heap cell value for the most recent dereference operation.
     */
    protected int getDerefVal()
    {
        return derefVal;
    }

    /**
     * Gets the value of the heap cell at the specified location.
     *
     * @param  addr The address to fetch from the heap.
     *
     * @return The heap cell at the specified location.
     */
    protected int getHeap(int addr)
    {
        return heap[addr];
    }

    /**
     * Attempts to unify structures or references on the heap, given two references to them. Structures are matched
     * element by element, free references become bound.
     *
     * @param  a1 The address of the first structure or reference.
     * @param  a2 The address of the second structure or reference.
     *
     * @return <tt>true</tt> if the two structures unify, <tt>false</tt> otherwise.
     */
    private boolean unify(int a1, int a2)
    {
        // pdl.push(a1)
        // pdl.push(a2)
        uPush(a1);
        uPush(a2);

        // fail <- false
        boolean fail = false;

        // while !empty(PDL) and not failed
        while (!uEmpty() && !fail)
        {
            // d1 <- deref(pdl.pop())
            // d2 <- deref(pdl.pop())
            // t1, v1 <- STORE[d1]
            // t2, v2 <- STORE[d2]
            int d1 = deref(uPop());
            int t1 = derefTag;
            int v1 = derefVal;

            int d2 = deref(uPop());
            int t2 = derefTag;
            int v2 = derefVal;

            // if (d1 != d2)
            if (d1 != d2)
            {
                // if (t1 = REF or t2 = REF)
                // bind(d1, d2)
                if ((t1 == L1InstructionSet.REF))
                {
                    heap[d1] = (L1InstructionSet.REF << 24) | (d2 & 0xFFFFFF);
                }
                else if (t2 == L1InstructionSet.REF)
                {
                    heap[d2] = (L1InstructionSet.REF << 24) | (d1 & 0xFFFFFF);
                }
                else
                {
                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    int fn1 = heap[v1];
                    int fn2 = heap[v2];
                    byte n1 = (byte) (fn1 & 0xFF);

                    // if f1 = f2 and n1 = n2
                    if (fn1 == fn2)
                    {
                        // for i <- 1 to n1
                        for (int i = 1; i <= n1; i++)
                        {
                            // pdl.push(v1 + i)
                            // pdl.push(v2 + i)
                            uPush(v1 + i);
                            uPush(v2 + i);
                        }
                    }
                    else
                    {
                        // fail <- true
                        fail = true;
                    }
                }
            }
        }

        return !fail;
    }

    /**
     * Pushes a value onto the unification stack.
     *
     * @param val The value to push onto the stack.
     */
    private void uPush(int val)
    {
        ustack[++up] = val;
    }

    /**
     * Pops a value from the unification stack.
     *
     * @return The top value from the unification stack.
     */
    private int uPop()
    {
        return ustack[up--];
    }

    /** Clears the unification stack. */
    private void uClear()
    {
        up = -1;
    }

    /**
     * Checks if the unification stack is empty.
     *
     * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
     */
    private boolean uEmpty()
    {
        return up <= -1;
    }
}
