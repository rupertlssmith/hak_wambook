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

import com.thesett.common.util.ByteBufferUtils;

/**
 * L0UnifyingJavaMachine is a byte code interpreter for L0 written in java. This is a direct implementation of the
 * instruction interpretations given in "Warren's Abstract Machine: A Tutorial Reconstruction". The pseudo algorithm
 * presented there can be read in the comments interspersed with the code. There are a couple of small differences:
 *
 * <pre><p/><ul>
 * <li>The book describes a STORE[addr] operation that loads or stores either a heap or register addresses. To simplify
 *     implementing this, the registers are placed at the top of the heap, and the initial heap pointer set below the
 *     registers. The STORE operation translates exactly into an array lookup on the heap.</li>
 * <li>The deref operation is presented in the book as a recursive function. It was turned into an equivalent iterative
 *     looping function instead. The deref operation returns multiple parameters, but as Java only supports single return
 *     types, a choice had to be made between creating a simple class to hold the return types, or storing the return
 *     values in member variables, and reading them from there. The member variables solution was chosen.</li>
 * </ul></pre>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled L0 programs and queries.
 * <tr><td> Provide access to the heap.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L0UnifyingJavaMachine extends L0UnifyingMachine
{
    /** Defines the heap size to use for the virtual machine. */
    private static final int HEAP_SIZE = 10000;

    /** Defines the register capacity for the virtual machine. */
    private static final int REG_SIZE = 10;

    /** Defines the max unification stack depth for the virtual machine. */
    private static final int USTACK_SIZE = 1000;

    /** Holds the working heap. */
    private int[] heap;

    /** Holds the heap pointer. */
    private int hp;

    /** Holds the secondary heap pointer, used for the heap address of the next term to match. */
    private int sp;

    /** Holds the unification stack. */
    private int[] ustack;

    /** Holds the unification stack pointer. */
    private int up = -1;

    /** Used to record whether the machine is in structure read or write mode. */
    private boolean writeMode;

    /** Holds the heap cell tag from the most recent dereference. */
    private byte derefTag;

    /** Holds the heap call value from the most recent dereference. */
    private int derefVal;

    /** Creates a unifying virtual machine for L0 with default heap sizes. */
    public L0UnifyingJavaMachine()
    {
        heap = new int[REG_SIZE + HEAP_SIZE];
        ustack = new int[USTACK_SIZE];

        // Registers are on the top of the heap, so initialize the heap pointers to the heap area.
        hp = REG_SIZE;
        sp = REG_SIZE;

        writeMode = false;
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
        byte[] code = functor.getCode();
        int ip = 0;
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
            case L0InstructionSet.PUT_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

                // heap[h] <- STR, h + 1
                heap[hp] = (L0InstructionSet.STR << 24) | ((hp + 1) & 0xFFFFFF);

                // heap[h+1] <- f/n
                heap[hp + 1] = fn;

                // Xi <- heap[h]
                heap[xi] = heap[hp];

                // h <- h + 2
                hp += 2;

                break;
            }

            // set_var Xi:
            case L0InstructionSet.SET_VAR:
            {
                // heap[h] <- REF, h
                heap[hp] = (L0InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                // Xi <- heap[h]
                heap[xi] = heap[hp];

                // h <- h + 1
                hp++;

                break;
            }

            // set_val Xi:
            case L0InstructionSet.SET_VAL:
            {
                // heap[h] <- Xi
                heap[hp] = heap[xi];

                // h <- h + 1
                hp++;

                break;
            }

            // get_struc Xi,
            case L0InstructionSet.GET_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

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
                case L0InstructionSet.REF:
                {
                    // heap[h] <- STR, h + 1
                    heap[hp] = (L0InstructionSet.STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    heap[hp + 1] = fn;

                    // bind(addr, h)
                    heap[addr] = (L0InstructionSet.REF << 24) | (hp & 0xFFFFFF);

                    // h <- h + 2
                    hp += 2;

                    // mode <- write
                    writeMode = true;

                    break;
                }

                // case STR, a:
                case L0InstructionSet.STR:
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
                    throw new IllegalStateException("Unkown tag type.");
                }
                }

                break;
            }

            // unify_var Xi:
            case L0InstructionSet.UNIFY_VAR:
            {
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
                    heap[hp] = (L0InstructionSet.REF << 24) | (hp & 0xFFFFFF);

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
            case L0InstructionSet.UNIFY_VAL:
            {
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

            default:
            {
                throw new IllegalStateException("Unkown instruction type.");
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
        while ((derefTag == L0InstructionSet.REF))
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
                if ((t1 == L0InstructionSet.REF))
                {
                    heap[d1] = (L0InstructionSet.REF << 24) | (d2 & 0xFFFFFF);
                }
                else if (t2 == L0InstructionSet.REF)
                {
                    heap[d2] = (L0InstructionSet.REF << 24) | (d1 & 0xFFFFFF);
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
