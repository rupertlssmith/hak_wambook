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
import java.nio.ByteOrder;
import java.nio.IntBuffer;
import java.util.Iterator;
import java.util.Set;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.wam.compiler.WAMCallPoint;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.ALLOCATE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.ALLOCATE_N;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.CALL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.CALL_INTERNAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.CON;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.CONTINUE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.CUT;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.DEALLOCATE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.EXECUTE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_CONST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_LEVEL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_LIST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_STRUC;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.GET_VAR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.LIS;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.NECK_CUT;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.NO_OP;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PROCEED;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_CONST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_LIST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_STRUC;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_UNSAFE_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.PUT_VAR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.REF;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.RETRY;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.RETRY_ME_ELSE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SET_CONST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SET_LOCAL_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SET_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SET_VAR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SET_VOID;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.STACK_ADDR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.STR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SUSPEND;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SWITCH_ON_CONST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SWITCH_ON_STRUC;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.SWITCH_ON_TERM;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.TRUST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.TRUST_ME;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.TRY;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.TRY_ME_ELSE;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.UNIFY_CONST;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.UNIFY_LOCAL_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.UNIFY_VAL;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.UNIFY_VAR;
import static com.thesett.aima.logic.fol.wam.compiler.WAMInstruction.UNIFY_VOID;
import com.thesett.common.util.SequenceIterator;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * WAMResolvingJavaMachine is a byte code interpreter for WAM written in java. This is a direct implementation of the
 * instruction interpretations given in "Warren's Abstract Machine: A Tutorial Reconstruction". The pseudo algorithm
 * presented there can be read in the comments interspersed with the code. There are a couple of challenges to be solved
 * that are not presented in the book:
 *
 * <p/>
 * <ul>
 * <li>The book describes a STORE[addr] operation that loads or stores a heap, register or stack address. In the L1 and
 * L0 machines, only heap and register addresses had to be catered for. This made things easier because the registers
 * could be held at the top of the heap and a single common address range used for both. With increasing numbers of data
 * areas in the machine, and Java unable to use direct pointers into memory, a choice between having separate arrays for
 * each data area, or building all data areas within a single array has to be made. The single array approach was
 * chosen, because otherwise the addressing mode would need to be passed down into the 'deref' and 'unify' operations,
 * which would be complicated by having to choose amongst which of several arrays to operate on. An addressing mode has
 * had to be added to the instruction set, so that instructions loading data from registers or stack, can specify which.
 * Once addresses are resolved relative to the register or stack basis, the plain addresses offset to the base of the
 * whole data area are used, and it is these addresses that are passed to the 'deref' and 'unify' operations.
 * <li>The memory layout for the WAM is described in Appendix B.3. of the book. The same layout is usd for this machine
 * with the exception that the code area is held in a separate array. This follows the x86 machine convention of
 * separating code and data segments in memory, and also caters well for the sharing of the code area with the JVM as a
 * byte buffer.
 * <li>The deref operation is presented in the book as a recursive function. It was turned into an equivalent iterative
 * looping function instead. The deref operation returns multiple parameters, but as Java only supports single return
 * types, a choice had to be made between creating a simple class to hold the return types, or storing the return values
 * in member variables, and reading them from there. The member variables solution was chosen.</li>
 * </ul>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Execute compiled WAM programs and queries.
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
public class WAMResolvingJavaMachine extends WAMResolvingMachine
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(WAMResolvingJavaMachine.class.getName()); */

    /** Used for tracing instruction executions. */
    /* private static final Logger trace = Logger.getLogger("TRACE.WAMResolvingJavaMachine"); */

    /** The id of the internal call/1 function. */
    public static final int CALL_1_ID = 1;

    /** The id of the internal call/1 function execute variant. */
    public static final int EXECUTE_1_ID = 2;

    /** The mask to extract an address from a tagged heap cell. */
    public static final int AMASK = 0x3FFFFFFF;

    /**
     * The mask to extract a constant from a tagged heap cell. Arity of atomic constants is always zero, so just the
     * functor name needs to be stored and loaded to the heap cell.
     */
    public static final int CMASK = 0xFFFFFFF;

    /** The shift to position the tag within a tagged heap cell. */
    public static final int TSHIFT = 30;

    /** Defines the register capacity for the virtual machine. */
    private static final int REG_SIZE = 256;

    /** Defines the heap size to use for the virtual machine. */
    private static final int HEAP_SIZE = 10000000;

    /** Defines the offset of the base of the heap in the data area. */
    private static final int HEAP_BASE = REG_SIZE;

    /** Defines the stack size to use for the virtual machine. */
    private static final int STACK_SIZE = 1000000;

    /** Defines the offset of the base of the stack in the data area. */
    private static final int STACK_BASE = REG_SIZE + HEAP_SIZE;

    /** Defines the trail size to use for the virtual machine. */
    private static final int TRAIL_SIZE = 10000;

    /** Defines the offset of the base of the trail in the data area. */
    private static final int TRAIL_BASE = REG_SIZE + HEAP_SIZE + STACK_SIZE;

    /** Defines the max unification stack depth for the virtual machine. */
    private static final int PDL_SIZE = 10000;

    /** Defines the highest address in the data area of the virtual machine. */
    private static final int TOP = REG_SIZE + HEAP_SIZE + STACK_SIZE + TRAIL_SIZE + PDL_SIZE;

    /** Defines the initial code area size for the virtual machine. */
    private static final int CODE_SIZE = 1000000;

    /** Holds the current instruction pointer into the code. */
    private int ip;

    /** Holds the current continuation pointer into the code. */
    private int cp;

    /** Holds the entire data segment of the machine. All registers, heaps and stacks are held in here. */
    private IntBuffer data;

    /** Holds the heap pointer. */
    private int hp;

    /** Holds the top of heap at the latest choice point. */
    private int hbp;

    /** Holds the secondary heap pointer, used for the heap address of the next term to match. */
    private int sp;

    /** Holds the unification stack pointer. */
    private int up;

    /** Holds the environment base pointer. */
    private int ep;

    /** Holds the choice point base pointer. */
    private int bp;

    /** Holds the last call choice point pointer. */
    private int b0;

    /** Holds the trail pointer. */
    private int trp;

    /** Used to record whether the machine is in structure read or write mode. */
    private boolean writeMode;

    /** Holds the heap cell tag from the most recent dereference. */
    private byte derefTag;

    /** Holds the heap call value from the most recent dereference. */
    private int derefVal;

    /** Indicates that the machine has been suspended, upon finding a solution. */
    private boolean suspended;

    /**
     * Creates a unifying virtual machine for WAM with default heap sizes.
     *
     * @param symbolTable The symbol table for the machine.
     */
    public WAMResolvingJavaMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super(symbolTable);

        // Reset the machine to its initial state.
        reset();
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public void reset()
    {
        // Create fresh heaps, code areas and stacks.
        data = ByteBuffer.allocateDirect(TOP << 2).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer();
        codeBuffer = ByteBuffer.allocateDirect(CODE_SIZE);
        codeBuffer.order(ByteOrder.LITTLE_ENDIAN);

        // Registers are on the top of the data area, the heap comes next.
        hp = HEAP_BASE;
        hbp = HEAP_BASE;
        sp = HEAP_BASE;

        // The stack comes after the heap. Pointers are zero initially, since no stack frames exist yet.
        ep = 0;
        bp = 0;
        b0 = 0;

        // The trail comes after the stack.
        trp = TRAIL_BASE;

        // The unification stack (PDL) is a push down stack at the end of the data area.
        up = TOP;

        // Turn off write mode.
        writeMode = false;

        // Reset the instruction pointer to that start of the code area, ready for fresh code to be loaded there.
        ip = 0;

        // Could probably not bother resetting these, but will do it anyway just to be sure.
        derefTag = 0;
        derefVal = 0;

        // The machine is initially not suspended.
        suspended = false;

        // Ensure that the overridden reset method of WAMBaseMachine is run too, to clear the call table.
        super.reset();

        // Put the internal functions in the call table.
        setInternalCodeAddress(internFunctorName("call", 1), CALL_1_ID);
        setInternalCodeAddress(internFunctorName("execute", 1), EXECUTE_1_ID);

        // Notify any debug monitor that the machine has been reset.
        if (monitor != null)
        {
            monitor.onReset(this);
        }
    }

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
        throw new UnsupportedOperationException("WAMResolvingJavaMachine does not support max steps limit on search.");
    }

    /** {@inheritDoc} */
    public IntBuffer getDataBuffer()
    {
        return data;
    }

    /** {@inheritDoc} */
    public WAMInternalRegisters getInternalRegisters()
    {
        return new WAMInternalRegisters(ip, hp, hbp, sp, up, ep, bp, b0, trp, writeMode);
    }

    /** {@inheritDoc} */
    public WAMMemoryLayout getMemoryLayout()
    {
        return new WAMMemoryLayout(0, REG_SIZE, HEAP_BASE, HEAP_SIZE, STACK_BASE, STACK_SIZE, TRAIL_BASE, TRAIL_SIZE,
            TOP - PDL_SIZE, PDL_SIZE);
    }

    /**
     * {@inheritDoc}
     *
     * <p/>Does nothing.
     */
    protected void codeAdded(ByteBuffer codeBuffer, int codeOffset, int length)
    {
    }

    /** {@inheritDoc} */
    protected int derefStack(int a)
    {
        /*log.fine("Stack deref from " + (a + ep + 3) + ", ep = " + ep);*/

        return deref(a + ep + 3);
    }

    /** {@inheritDoc} */
    protected boolean execute(WAMCallPoint callPoint)
    {
        /*log.fine("protected boolean execute(WAMCallPoint callPoint): called");*/

        boolean failed;

        // Check if the machine is being woken up from being suspended, in which case immediately fail in order to
        // trigger back-tracking to find more solutions.
        if (suspended)
        {
            failed = true;
            suspended = false;
        }
        else
        {
            ip = callPoint.entryPoint;
            uClear();
            failed = false;
        }

        int numOfArgs = 0;

        // Holds the current continuation point.
        cp = codeBuffer.position();

        // Notify any debug monitor that execution is starting.
        if (monitor != null)
        {
            monitor.onExecute(this);
        }

        //while (!failed && (ip < code.length))
        while (true)
        {
            // Attempt to backtrack on failure.
            if (failed)
            {
                failed = backtrack();

                if (failed)
                {
                    break;
                }
            }

            // Grab next instruction and switch on it.
            byte instruction = codeBuffer.get(ip);

            switch (instruction)
            {
            // put_struc Xi, f/n:
            case PUT_STRUC:
            {
                // grab addr, f/n
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                int fn = codeBuffer.getInt(ip + 3);

                /*trace.fine(ip + ": PUT_STRUC " + printSlot(xi, mode) + ", " + fn);*/

                // heap[h] <- STR, h + 1
                data.put(hp, fn);

                // Xi <- heap[h]
                data.put(xi, structureAt(hp));

                // h <- h + 2
                hp += 1;

                // P <- instruction_size(P)
                ip += 7;

                break;
            }

            // set_var Xi:
            case SET_VAR:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": SET_VAR " + printSlot(xi, mode));*/

                // heap[h] <- REF, h
                data.put(hp, refTo(hp));

                // Xi <- heap[h]
                data.put(xi, data.get(hp));

                // h <- h + 1
                hp++;

                // P <- instruction_size(P)
                ip += 3;

                break;
            }

            // set_val Xi:
            case SET_VAL:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": SET_VAL " + printSlot(xi, mode));*/

                // heap[h] <- Xi
                data.put(hp, data.get(xi));

                // h <- h + 1
                hp++;

                // P <- instruction_size(P)
                ip += 3;

                break;
            }

            // get_struc Xi,
            case GET_STRUC:
            {
                // grab addr, f/n
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                int fn = codeBuffer.getInt(ip + 3);

                /*trace.fine(ip + ": GET_STRUC " + printSlot(xi, mode) + ", " + fn);*/

                // addr <- deref(Xi);
                int addr = deref(xi);
                byte tag = derefTag;
                int a = derefVal;

                // switch STORE[addr]
                switch (tag)
                {
                // case REF:
                case REF:
                {
                    // heap[h] <- STR, h + 1
                    data.put(hp, structureAt(hp + 1));

                    // heap[h+1] <- f/n
                    data.put(hp + 1, fn);

                    // bind(addr, h)
                    bind(addr, hp);

                    // h <- h + 2
                    hp += 2;

                    // mode <- write
                    writeMode = true;
                    /*trace.fine("-> write mode");*/

                    break;
                }

                // case STR, a:
                case STR:
                {
                    // if heap[a] = f/n
                    if (data.get(a) == fn)
                    {
                        // s <- a + 1
                        sp = a + 1;

                        // mode <- read
                        writeMode = false;
                        /*trace.fine("-> read mode");*/
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
                    // fail
                    failed = true;
                }
                }

                // P <- instruction_size(P)
                ip += 7;

                break;
            }

            // unify_var Xi:
            case UNIFY_VAR:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": UNIFY_VAR " + printSlot(xi, mode));*/

                // switch mode
                if (!writeMode)
                {
                    // case read:
                    // Xi <- heap[s]
                    data.put(xi, data.get(sp));
                }
                else
                {
                    // case write:
                    // heap[h] <- REF, h
                    data.put(hp, refTo(hp));

                    // Xi <- heap[h]
                    data.put(xi, data.get(hp));

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            // unify_val Xi:
            case UNIFY_VAL:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": UNIFY_VAL " + printSlot(xi, mode));*/

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
                    data.put(hp, data.get(xi));

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            // put_var Xn, Ai:
            case PUT_VAR:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                byte ai = codeBuffer.get(ip + 3);

                /*trace.fine(ip + ": PUT_VAR " + printSlot(xi, mode) + ", A" + ai);*/

                if (mode == WAMInstruction.REG_ADDR)
                {
                    // heap[h] <- REF, H
                    data.put(hp, refTo(hp));

                    // Xn <- heap[h]
                    data.put(xi, data.get(hp));

                    // Ai <- heap[h]
                    data.put(ai, data.get(hp));
                }
                else
                {
                    // STACK[addr] <- REF, addr
                    data.put(xi, refTo(xi));

                    // Ai <- STACK[addr]
                    data.put(ai, data.get(xi));
                }

                // h <- h + 1
                hp++;

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // put_val Xn, Ai:
            case PUT_VAL:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                byte ai = codeBuffer.get(ip + 3);

                /*trace.fine(ip + ": PUT_VAL " + printSlot(xi, mode) + ", A" + ai);*/

                // Ai <- Xn
                data.put(ai, data.get(xi));

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // get var Xn, Ai:
            case GET_VAR:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                byte ai = codeBuffer.get(ip + 3);

                /*trace.fine(ip + ": GET_VAR " + printSlot(xi, mode) + ", A" + ai);*/

                // Xn <- Ai
                data.put(xi, data.get(ai));

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // get_val Xn, Ai:
            case GET_VAL:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                byte ai = codeBuffer.get(ip + 3);

                /*trace.fine(ip + ": GET_VAL " + printSlot(xi, mode) + ", A" + ai);*/

                // unify (Xn, Ai)
                failed = !unify(xi, ai);

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            case PUT_CONST:
            {
                // grab addr, f/n
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                int fn = codeBuffer.getInt(ip + 3);

                /*trace.fine(ip + ": PUT_CONST " + printSlot(xi, mode) + ", " + fn);*/

                // Xi <- heap[h]
                data.put(xi, constantCell(fn));

                // P <- instruction_size(P)
                ip += 7;

                break;
            }

            case GET_CONST:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);
                int fn = codeBuffer.getInt(ip + 3);

                /*trace.fine(ip + ": GET_CONST " + printSlot(xi, mode) + ", " + fn);*/

                // addr <- deref(Xi)
                int addr = deref(xi);
                int tag = derefTag;
                int val = derefVal;

                failed = !unifyConst(fn, xi);

                // P <- P + instruction_size(P)
                ip += 7;

                break;
            }

            case SET_CONST:
            {
                int fn = codeBuffer.getInt(ip + 1);

                /*trace.fine(ip + ": SET_CONST " + fn);*/

                // heap[h] <- <CON, c>
                data.put(hp, constantCell(fn));

                // h <- h + 1
                hp++;

                // P <- instruction_size(P)
                ip += 5;

                break;
            }

            case UNIFY_CONST:
            {
                int fn = codeBuffer.getInt(ip + 1);

                /*trace.fine(ip + ": UNIFY_CONST " + fn);*/

                // switch mode
                if (!writeMode)
                {
                    // case read:
                    // addr <- deref(S)

                    // unifyConst(fn, addr)
                    failed = !unifyConst(fn, sp);
                }
                else
                {
                    // case write:
                    // heap[h] <- <CON, c>
                    data.put(hp, constantCell(fn));

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                // P <- P + instruction_size(P)
                ip += 5;

                break;
            }

            case PUT_LIST:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": PUT_LIST " + printSlot(xi, mode));*/

                // Xi <- <LIS, H>
                data.put(xi, listCell(hp));

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            case GET_LIST:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": GET_LIST " + printSlot(xi, mode));*/

                int addr = deref(xi);
                int tag = derefTag;
                int val = derefVal;

                // case STORE[addr] of
                switch (tag)
                {
                case REF:
                {
                    // <REF, _> :
                    // HEAP[H] <- <LIS, H+1>
                    data.put(hp, listCell(hp + 1));

                    // bind(addr, H)
                    bind(addr, hp);

                    // H <- H + 1
                    hp += 1;

                    // mode <- write
                    writeMode = true;
                    /*trace.fine("-> write mode");*/

                    break;
                }

                case LIS:
                {
                    // <LIS, a> :
                    // S <- a
                    sp = val;

                    // mode <- read
                    writeMode = false;
                    /*trace.fine("-> read mode");*/

                    break;
                }

                default:
                {
                    // other: fail <- true;
                    failed = true;
                }
                }

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            case SET_VOID:
            {
                // grab N
                int n = (int) codeBuffer.get(ip + 1);

                /*trace.fine(ip + ": SET_VOID " + n);*/

                // for i <- H to H + n - 1 do
                //  HEAP[i] <- <REF, i>
                for (int addr = hp; addr < (hp + n); addr++)
                {
                    data.put(addr, refTo(addr));
                }

                // H <- H + n
                hp += n;

                // P <- P + instruction_size(P)
                ip += 2;

                break;
            }

            case UNIFY_VOID:
            {
                // grab N
                int n = (int) codeBuffer.get(ip + 1);

                /*trace.fine(ip + ": UNIFY_VOID " + n);*/

                // case mode of
                if (!writeMode)
                {
                    //  read: S <- S + n
                    sp += n;
                }
                else
                {
                    //  write:
                    //   for i <- H to H + n -1 do
                    //    HEAP[i] <- <REF, i>
                    for (int addr = hp; addr < (hp + n); addr++)
                    {
                        data.put(addr, refTo(addr));
                    }

                    //   H <- H + n
                    hp += n;
                }

                // P <- P + instruction_size(P)
                ip += 2;

                break;
            }

            // put_unsafe_val Yn, Ai:
            case PUT_UNSAFE_VAL:
            {
                // grab addr, Ai
                byte mode = codeBuffer.get(ip + 1);
                int yi = (int) codeBuffer.get(ip + 2) + (ep + 3);
                byte ai = codeBuffer.get(ip + 3);

                /*trace.fine(ip + ": PUT_UNSAFE_VAL " + printSlot(yi, WAMInstruction.STACK_ADDR) + ", A" + ai);*/

                int addr = deref(yi);

                if (addr < ep)
                {
                    // Ai <- Xn
                    data.put(ai, data.get(addr));
                }
                else
                {
                    data.put(hp, refTo(hp));
                    bind(addr, hp);
                    data.put(ai, data.get(hp));
                    hp++;
                }

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // set_local_val Xi:
            case SET_LOCAL_VAL:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": SET_LOCAL_VAL " + printSlot(xi, mode));*/

                int addr = deref(xi);

                if (addr < ep)
                {
                    data.put(hp, data.get(addr));
                }
                else
                {
                    data.put(hp, refTo(hp));
                    bind(addr, hp);
                }

                // h <- h + 1
                hp++;

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            // unify_local_val Xi:
            case UNIFY_LOCAL_VAL:
            {
                // grab addr
                byte mode = codeBuffer.get(ip + 1);
                int xi = getRegisterOrStackSlot(mode);

                /*trace.fine(ip + ": UNIFY_LOCAL_VAL " + printSlot(xi, mode));*/

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
                    int addr = deref(xi);

                    if (addr < ep)
                    {
                        data.put(hp, data.get(addr));
                    }
                    else
                    {
                        data.put(hp, refTo(hp));
                        bind(addr, hp);
                    }

                    // h <- h + 1
                    hp++;
                }

                // s <- s + 1
                sp++;

                // P <- P + instruction_size(P)
                ip += 3;

                break;
            }

            // call @(p/n), perms:
            case CALL:
            {
                // grab @(p/n), perms
                int pn = codeBuffer.getInt(ip + 1);
                int n = codeBuffer.get(ip + 5);
                int numPerms = (int) codeBuffer.get(ip + 6);

                // num_of_args <- n
                numOfArgs = n;

                // Ensure that the predicate to call is known and linked in, otherwise fail.
                if (pn == -1)
                {
                    failed = true;

                    break;
                }

                // STACK[E + 2] <- numPerms
                data.put(ep + 2, numPerms);

                // CP <- P + instruction_size(P)
                cp = ip + 7;

                /*trace.fine(ip + ": CALL " + pn + "/" + n + ", " + numPerms + " (cp = " + cp + ")]");*/

                // B0 <- B
                b0 = bp;

                // P <- @(p/n)
                ip = pn;

                break;
            }

            // execute @(p/n):
            case EXECUTE:
            {
                // grab @(p/n)
                int pn = codeBuffer.getInt(ip + 1);
                int n = codeBuffer.get(ip + 5);

                // num_of_args <- n
                numOfArgs = n;

                /*trace.fine(ip + ": EXECUTE " + pn + "/" + n + " (cp = " + cp + ")]");*/

                // Ensure that the predicate to call is known and linked in, otherwise fail.
                if (pn == -1)
                {
                    failed = true;

                    break;
                }

                // B0 <- B
                b0 = bp;

                // P <- @(p/n)
                ip = pn;

                break;
            }

            // proceed:
            case PROCEED:
            {
                /*trace.fine(ip + ": PROCEED" + " (cp = " + cp + ")]");*/

                // P <- CP
                ip = cp;

                break;
            }

            // allocate:
            case ALLOCATE:
            {
                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newE] <- E
                data.put(esp, ep);

                // STACK[E + 1] <- CP
                data.put(esp + 1, cp);

                // STACK[E + 2] <- N
                data.put(esp + 2, 0);

                // E <- newE
                // newE <- E + n + 3
                ep = esp;

                /*trace.fine(ip + ": ALLOCATE");*/
                /*trace.fine("-> env @ " + ep + " " + traceEnvFrame());*/

                // P <- P + instruction_size(P)
                ip += 1;

                break;
            }

            // allocate N:
            case ALLOCATE_N:
            {
                // grab N
                int n = (int) codeBuffer.get(ip + 1);

                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newE] <- E
                data.put(esp, ep);

                // STACK[E + 1] <- CP
                data.put(esp + 1, cp);

                // STACK[E + 2] <- N
                data.put(esp + 2, n);

                // E <- newE
                // newE <- E + n + 3
                ep = esp;

                /*trace.fine(ip + ": ALLOCATE_N " + n);*/
                /*trace.fine("-> env @ " + ep + " " + traceEnvFrame());*/

                // P <- P + instruction_size(P)
                ip += 2;

                break;
            }

            // deallocate:
            case DEALLOCATE:
            {
                int newip = data.get(ep + 1);

                // E <- STACK[E]
                ep = data.get(ep);

                /*trace.fine(ip + ": DEALLOCATE");*/
                /*trace.fine("<- env @ " + ep + " " + traceEnvFrame());*/

                // CP <- STACK[E + 1]
                cp = newip;

                // P <- P + instruction_size(P)
                ip += 1;

                break;
            }

            // try me else L:
            case TRY_ME_ELSE:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newB] <- num_of_args
                // n <- STACK[newB]
                int n = numOfArgs;
                data.put(esp, n);

                // for i <- 1 to n do STACK[newB + i] <- Ai
                for (int i = 0; i < n; i++)
                {
                    data.put(esp + i + 1, data.get(i));
                }

                // STACK[newB + n + 1] <- E
                data.put(esp + n + 1, ep);

                // STACK[newB + n + 2] <- CP
                data.put(esp + n + 2, cp);

                // STACK[newB + n + 3] <- B
                data.put(esp + n + 3, bp);

                // STACK[newB + n + 4] <- L
                data.put(esp + n + 4, l);

                // STACK[newB + n + 5] <- TR
                data.put(esp + n + 5, trp);

                // STACK[newB + n + 6] <- H
                data.put(esp + n + 6, hp);

                // STACK[newB + n + 7] <- B0
                data.put(esp + n + 7, b0);

                // B <- new B
                bp = esp;

                // HB <- H
                hbp = hp;

                /*trace.fine(ip + ": TRY_ME_ELSE");*/
                /*trace.fine("-> chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- P + instruction_size(P)
                ip += 5;

                break;
            }

            // retry me else L:
            case RETRY_ME_ELSE:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                // n <- STACK[B]
                int n = data.get(bp);

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data.put(i, data.get(bp + i + 1));
                }

                // E <- STACK[B + n + 1]
                ep = data.get(bp + n + 1);

                // CP <- STACK[B + n + 2]
                cp = data.get(bp + n + 2);

                // STACK[B + n + 4] <- L
                data.put(bp + n + 4, l);

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data.get(bp + n + 5), trp);

                // TR <- STACK[B + n + 5]
                trp = data.get(bp + n + 5);

                // H <- STACK[B + n + 6]
                hp = data.get(bp + n + 6);

                // HB <- H
                hbp = hp;

                /*trace.fine(ip + ": RETRY_ME_ELSE");*/
                /*trace.fine("-- chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- P + instruction_size(P)
                ip += 5;

                break;
            }

            // trust me (else fail):
            case TRUST_ME:
            {
                // n <- STACK[B]
                int n = data.get(bp);

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data.put(i, data.get(bp + i + 1));
                }

                // E <- STACK[B + n + 1]
                ep = data.get(bp + n + 1);

                // CP <- STACK[B + n + 2]
                cp = data.get(bp + n + 2);

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data.get(bp + n + 5), trp);

                // TR <- STACK[B + n + 5]
                trp = data.get(bp + n + 5);

                // H <- STACK[B + n + 6]
                hp = data.get(bp + n + 6);

                // HB <- STACK[B + n + 6]
                hbp = hp;

                // B <- STACK[B + n + 3]
                bp = data.get(bp + n + 3);

                /*trace.fine(ip + ": TRUST_ME");*/
                /*trace.fine("<- chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- P + instruction_size(P)
                ip += 1;

                break;
            }

            case SWITCH_ON_TERM:
            {
                // grab labels
                int v = codeBuffer.getInt(ip + 1);
                int c = codeBuffer.getInt(ip + 5);
                int l = codeBuffer.getInt(ip + 9);
                int s = codeBuffer.getInt(ip + 13);

                int addr = deref(1);
                int tag = derefTag;

                // case STORE[deref(A1)] of
                switch (tag)
                {
                case REF:

                    // <REF, _> : P <- V
                    ip = v;
                    break;

                case CON:

                    // <CON, _> : P <- C
                    ip = c;
                    break;

                case LIS:

                    // <LIS, _> : P <- L
                    ip = l;
                    break;

                case STR:

                    // <STR, _> : P <- S
                    ip = s;
                    break;
                }

                break;
            }

            case SWITCH_ON_CONST:
            {
                // grab labels
                int t = codeBuffer.getInt(ip + 1);
                int n = codeBuffer.getInt(ip + 5);

                // <tag, val> <- STORE[deref(A1)]
                deref(1);

                int val = derefVal;

                // <found, inst> <- get_hash(val, T, N)
                int inst = getHash(val, t, n);

                // if found
                if (inst > 0)
                {
                    // then P <- inst
                    ip = inst;
                }
                else
                {
                    // else backtrack
                    failed = true;
                }

                break;
            }

            case SWITCH_ON_STRUC:
            {
                // grab labels
                int t = codeBuffer.getInt(ip + 1);
                int n = codeBuffer.getInt(ip + 5);

                // <tag, val> <- STORE[deref(A1)]
                deref(1);

                int val = derefVal;

                // <found, inst> <- get_hash(val, T, N)
                int inst = getHash(val, t, n);

                // if found
                if (inst > 0)
                {
                    // then P <- inst
                    ip = inst;
                }
                else
                {
                    // else backtrack
                    failed = true;
                }

                break;
            }

            case TRY:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newB] <- num_of_args
                // n <- STACK[newB]
                int n = numOfArgs;
                data.put(esp, n);

                // for i <- 1 to n do STACK[newB + i] <- Ai
                for (int i = 0; i < n; i++)
                {
                    data.put(esp + i + 1, data.get(i));
                }

                // STACK[newB + n + 1] <- E
                data.put(esp + n + 1, ep);

                // STACK[newB + n + 2] <- CP
                data.put(esp + n + 2, cp);

                // STACK[newB + n + 3] <- B
                data.put(esp + n + 3, bp);

                // STACK[newB + n + 4] <- L
                data.put(esp + n + 4, ip + 5);

                // STACK[newB + n + 5] <- TR
                data.put(esp + n + 5, trp);

                // STACK[newB + n + 6] <- H
                data.put(esp + n + 6, hp);

                // STACK[newB + n + 7] <- B0
                data.put(esp + n + 7, b0);

                // B <- new B
                bp = esp;

                // HB <- H
                hbp = hp;

                /*trace.fine(ip + ": TRY");*/
                /*trace.fine("-> chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- L
                ip = l;

                break;
            }

            case RETRY:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                // n <- STACK[B]
                int n = data.get(bp);

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data.put(i, data.get(bp + i + 1));
                }

                // E <- STACK[B + n + 1]
                ep = data.get(bp + n + 1);

                // CP <- STACK[B + n + 2]
                cp = data.get(bp + n + 2);

                // STACK[B + n + 4] <- L
                data.put(bp + n + 4, ip + 5);

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data.get(bp + n + 5), trp);

                // TR <- STACK[B + n + 5]
                trp = data.get(bp + n + 5);

                // H <- STACK[B + n + 6]
                hp = data.get(bp + n + 6);

                // HB <- H
                hbp = hp;

                /*trace.fine(ip + ": RETRY");*/
                /*trace.fine("-- chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- L
                ip = l;

                break;
            }

            case TRUST:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                // n <- STACK[B]
                int n = data.get(bp);

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data.put(i, data.get(bp + i + 1));
                }

                // E <- STACK[B + n + 1]
                ep = data.get(bp + n + 1);

                // CP <- STACK[B + n + 2]
                cp = data.get(bp + n + 2);

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data.get(bp + n + 5), trp);

                // TR <- STACK[B + n + 5]
                trp = data.get(bp + n + 5);

                // H <- STACK[B + n + 6]
                hp = data.get(bp + n + 6);

                // HB <- STACK[B + n + 6]
                hbp = hp;

                // B <- STACK[B + n + 3]
                bp = data.get(bp + n + 3);

                /*trace.fine(ip + ": TRUST");*/
                /*trace.fine("<- chp @ " + bp + " " + traceChoiceFrame());*/

                // P <- L
                ip = l;

                break;
            }

            case NECK_CUT:
            {
                if (bp > b0)
                {
                    bp = b0;
                    tidyTrail();
                }

                /*trace.fine(ip + ": NECK_CUT");*/
                /*trace.fine("<- chp @ " + bp + " " + traceChoiceFrame());*/

                ip += 1;

                break;
            }

            case GET_LEVEL:
            {
                int yn = (int) codeBuffer.get(ip + 1) + (ep + 3);

                data.put(yn, b0);

                /*trace.fine(ip + ": GET_LEVEL " + codeBuffer.get(ip + 1));*/

                ip += 2;

                break;
            }

            case CUT:
            {
                int yn = (int) codeBuffer.get(ip + 1) + (ep + 3);

                int cbp = data.get(yn);

                if (bp > cbp)
                {
                    bp = cbp;
                    tidyTrail();
                }

                /*trace.fine(ip + ": CUT " + codeBuffer.get(ip + 1));*/
                /*trace.fine("<- chp @ " + bp + " " + traceChoiceFrame());*/

                ip += 2;

                break;
            }

            case CONTINUE:
            {
                // grab L
                int l = codeBuffer.getInt(ip + 1);

                /*trace.fine(ip + ": CONTINUE " + l);*/

                ip = l;

                break;
            }

            case NO_OP:
            {
                /*trace.fine(ip + ": NO_OP");*/

                ip += 1;

                break;
            }

            // call_internal @(p/n), perms:
            case CALL_INTERNAL:
            {
                // grab @(p/n), perms
                int pn = codeBuffer.getInt(ip + 1);
                int n = codeBuffer.get(ip + 5);
                int numPerms = (int) codeBuffer.get(ip + 6);

                // num_of_args <- n
                numOfArgs = n;

                /*trace.fine(ip + ": CALL_INTERNAL " + pn + "/" + n + ", " + numPerms + " (cp = " + cp + ")]");*/

                boolean callOk = callInternal(pn, n, numPerms);

                failed = !callOk;

                break;
            }

            // suspend on success:
            case SUSPEND:
            {
                /*trace.fine(ip + ": SUSPEND");*/
                ip += 1;
                suspended = true;

                return true;
            }
            }

            // Notify any debug monitor that the machine has been stepped.
            if (monitor != null)
            {
                monitor.onStep(this);
            }
        }

        return !failed;
    }

    /**
     * Pretty prints the current environment frame, for debugging purposes.
     *
     * @return The current environment frame, pretty printed.
     */
    protected String traceEnvFrame()
    {
        return "env: [ ep = " + data.get(ep) + ", cp = " + data.get(ep + 1) + ", n = " + data.get(ep + 2) + "]";
    }

    /**
     * Pretty prints the current choice point frame, for debugging purposes.
     *
     * @return The current choice point frame, pretty printed.
     */
    protected String traceChoiceFrame()
    {
        if (bp == 0)
        {
            return "";
        }

        int n = data.get(bp);

        return "choice: [ n = " + data.get(bp) + ", ep = " + data.get(bp + n + 1) + ", cp = " + data.get(bp + n + 2) +
            ", bp = " + data.get(bp + n + 3) + ", l = " + data.get(bp + n + 4) + ", trp = " + data.get(bp + n + 5) +
            ", hp = " + data.get(bp + n + 6) + ", b0 = " + data.get(bp + n + 7);
    }

    /** {@inheritDoc} */
    protected int deref(int a)
    {
        // tag, value <- STORE[a]
        int addr = a;
        int tmp = data.get(a);
        derefTag = (byte) (tmp >>> TSHIFT);
        derefVal = tmp & AMASK;

        // while tag = REF and value != a
        while ((derefTag == WAMInstruction.REF))
        {
            // tag, value <- STORE[a]
            addr = derefVal;
            tmp = data.get(derefVal);
            derefTag = (byte) (tmp >>> TSHIFT);
            tmp = tmp & AMASK;

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
        return data.get(addr);
    }

    /**
     * Invokes an internal function.
     *
     * @param  function The id of the internal function to call.
     * @param  arity    The arity of the function to call.
     * @param  numPerms The number of permanent variables remaining in the environment.
     *
     * @return <tt>true</tt> if the call succeeded, and <tt>false</tt> if it failed.
     */
    private boolean callInternal(int function, int arity, int numPerms)
    {
        switch (function)
        {
        case CALL_1_ID:
            return internalCall_1(numPerms);

        case EXECUTE_1_ID:
            return internalExecute_1();

        default:
            throw new IllegalStateException("Unknown internal function id: " + function);
        }
    }

    /**
     * Implements the 'call/1' predicate.
     *
     * @param  numPerms The number of permanent variables remaining in the environment.
     *
     * @return <tt>true</tt> if the call succeeded, and <tt>false</tt> if it failed.
     */
    private boolean internalCall_1(int numPerms)
    {
        int pn = setupCall_1();

        if (pn == -1)
        {
            return false;
        }

        // Make the call.
        // STACK[E + 2] <- numPerms
        data.put(ep + 2, numPerms);

        // CP <- P + instruction_size(P)
        cp = ip + 7;

        /*trace.fine(ip + ": (CALL) " + pn + ", " + numPerms + " (cp = " + cp + ")]");*/

        // B0 <- B
        b0 = bp;

        // P <- @(p/n)
        ip = pn;

        return true;
    }

    /**
     * Implements the execute variant of the 'call/1' predicate.
     *
     * @return <tt>true</tt> if the call succeeded, and <tt>false</tt> if it failed.
     */
    private boolean internalExecute_1()
    {
        int pn = setupCall_1();

        if (pn == -1)
        {
            return false;
        }

        // Make the call.
        /*trace.fine(ip + ": (EXECUTE) " + pn + " (cp = " + cp + ")]");*/

        // B0 <- B
        b0 = bp;

        // P <- @(p/n)
        ip = pn;

        return true;
    }

    /**
     * Sets up the registers to make a call, for implementing call/1. The first register should reference a structure to
     * be turned into a predicate call. The arguments of this structure will be set up in the registers, and the entry
     * point of the predicate to call will be returned.
     *
     * @return The entry address of the predicate to call, or <tt>-1</tt> if the call cannot be resolved to a known
     *         predicate.
     */
    private int setupCall_1()
    {
        // Get X0.
        int addr = deref(0);
        byte tag = derefTag;
        int val = derefVal;

        // Check it points to a structure.
        int fn;

        if (tag == STR)
        {
            fn = getHeap(val);
        }
        else if (tag == CON)
        {
            fn = val;
        }
        else
        {
            /*trace.fine("call/1 not invoked against structure.");*/

            return -1;
        }

        // Look up the call point of the matching functor.
        int f = fn & 0x00ffffff;

        WAMCallPoint callPoint = resolveCallPoint(f);

        if (callPoint.entryPoint == -1)
        {
            /*trace.fine("call/1 to unknown call point.");*/

            return -1;
        }

        int pn = callPoint.entryPoint;

        // Set registers X0... to ref to args...
        FunctorName functorName = getDeinternedFunctorName(f);
        int arity = functorName.getArity();

        for (int i = 0; i < arity; i++)
        {
            data.put(i, refTo(val + 1 + i));
        }

        return pn;
    }

    /**
     * Looks up a value (an interned name referring to a constant or structure), in the hash table of size n referred
     * to.
     *
     * @param  val The value to look up.
     * @param  t   The offset of the start of the hash table.
     * @param  n   The size of the hash table in bytes.
     *
     * @return <tt>0</tt> iff no match is found, or a pointer into the code area of the matching branch.
     */
    private int getHash(int val, int t, int n)
    {
        return 0;
    }

    /**
     * Creates a heap cell contents containing a structure tag, and the address of the structure.
     *
     * <p/>Note: This only creates the contents of the cell, it does not write it to the heap.
     *
     * @param  addr The address of the structure.
     *
     * @return The heap cell contents referencing the structure.
     */
    private int structureAt(int addr)
    {
        return (WAMInstruction.STR << TSHIFT) | (addr & AMASK);
    }

    /**
     * Creates a heap cell contents containing a reference.
     *
     * <p/>Note: This only creates the contents of the cell, it does not write it to the heap.
     *
     * @param  addr The references address.
     *
     * @return The heap cell contents containing the reference.
     */
    private int refTo(int addr)
    {
        return (WAMInstruction.REF << TSHIFT) | (addr & AMASK);
    }

    /**
     * Creates a heap cell contents containing a constant.
     *
     * <p/>Note: This only creates the contents of the cell, it does not write it to the heap.
     *
     * <p/>See the comment on {@link #CMASK} about the arity always being zero on a constant.
     *
     * @param  fn The functor name and arity of the constant. Arity should always be zero.
     *
     * @return The heap cell contents containing the constant.
     */
    private int constantCell(int fn)
    {
        return (CON << TSHIFT) | (fn & CMASK);
    }

    /**
     * Creates a heap cell contents containing a list pointer.
     *
     * <p/>Note: This only creates the contents of the cell, it does not write it to the heap.
     *
     * @param  addr The address of the list contents.
     *
     * @return The heap cell contents containing the list pointer.
     */
    private int listCell(int addr)
    {
        return (WAMInstruction.LIS << TSHIFT) | (addr & AMASK);
    }

    /**
     * Loads the contents of a register, or a stack slot, depending on the mode.
     *
     * @param  mode The mode, {@link WAMInstruction#REG_ADDR} for register addressing, {@link WAMInstruction#STACK_ADDR}
     *              for stack addressing.
     *
     * @return The contents of the register or stack slot.
     */
    private int getRegisterOrStackSlot(byte mode)
    {
        return (int) codeBuffer.get(ip + 2) + ((mode == STACK_ADDR) ? (ep + 3) : 0);
    }

    /**
     * Computes the start of the next stack frame. This depends on whether the most recent stack frame is an environment
     * frame or a choice point frame, as these have different sizes. The size of the most recent type of frame is
     * computed and added to the current frame pointer to give the start of the next frame.
     *
     * @return The start of the next stack frame.
     */
    private int nextStackFrame()
    {
        // if E > B
        // then newB <- E + STACK[E + 2] + 3
        // else newB <- B + STACK[B] + 7

        if (ep == bp)
        {
            return STACK_BASE;
        }
        else if (ep > bp)
        {
            return ep + data.get(ep + 2) + 3;
        }
        else
        {
            return bp + data.get(bp) + 8;
        }
    }

    /**
     * Backtracks to the continuation label stored in the current choice point frame, if there is one. Otherwise returns
     * a fail to indicate that there are no more choice points, so no backtracking can be done.
     *
     * @return <tt>true</tt> iff this is the final failure, and there are no more choice points.
     */
    private boolean backtrack()
    {
        // if B = bottom_of_stack
        if (bp == 0)
        {
            //  then fail_and_exit_program
            return true;
        }
        else
        {
            // B0 <- STACK[B + STACK[B} + 7]
            b0 = data.get(bp + data.get(bp) + 7);

            // P <- STACK[B + STACK[B] + 4]
            ip = data.get(bp + data.get(bp) + 4);

            return false;
        }
    }

    /**
     * Creates a binding of one variable onto another. One of the supplied addresses must be an unbound variable. If
     * both are unbound variables, the higher (newer) address is bound to the lower (older) one.
     *
     * @param a1 The address of the first potential unbound variable to bind.
     * @param a2 The address of the second potential unbound variable to bind.
     */
    private void bind(int a1, int a2)
    {
        // <t1, _> <- STORE[a1]
        int t1 = (byte) (data.get(a1) >>> TSHIFT);

        // <t2, _> <- STORE[a2]
        int t2 = (byte) (data.get(a2) >>> TSHIFT);

        // if (t1 = REF) /\ ((t2 != REF) \/ (a2 < a1))
        if ((t1 == WAMInstruction.REF) && ((t2 != WAMInstruction.REF) || (a2 < a1)))
        {
            //  STORE[a1] <- STORE[a2]
            //data.put(a1, refTo(a2));
            data.put(a1, data.get(a2));

            //  trail(a1)
            trail(a1);
        }
        else if (t2 == WAMInstruction.REF)
        {
            //  STORE[a2] <- STORE[a1]
            //data.put(a2, refTo(a1));
            data.put(a2, data.get(a1));

            //  tail(a2)
            trail(a2);
        }
    }

    /**
     * Records the address of a binding onto the 'trail'. The trail pointer is advanced by one as part of this
     * operation.
     *
     * @param addr The binding address to add to the trail.
     */
    private void trail(int addr)
    {
        // if (a < HB) \/ ((H < a) /\ (a < B))
        if ((addr < hbp) || ((hp < addr) && (addr < bp)))
        {
            //  TRAIL[TR] <- a
            data.put(trp, addr);

            //  TR <- TR + 1
            trp++;
        }
    }

    /**
     * Undoes variable bindings that have been recorded on the 'trail'. Addresses recorded on the trail are reset to REF
     * to self.
     *
     * @param a1 The start address within the trail to get the first binding address to clear.
     * @param a2 The end address within the trail, this is one higher than the last address to clear.
     */
    private void unwindTrail(int a1, int a2)
    {
        // for i <- a1 to a2 - 1 do
        for (int addr = a1; addr < a2; addr++)
        {
            //  STORE[TRAIL[i]] <- <REF, TRAIL[i]>
            int tmp = data.get(addr);
            data.put(tmp, refTo(tmp));
        }
    }

    /**
     * Tidies trail when a choice point is being discarded, and a previous choice point it being made the current one.
     *
     * <p/>Copies trail bindings created since the choice point, into the trail as known to the previous choice point.
     * That is bindings on the heap created during the choice point (between HB and H).
     */
    private void tidyTrail()
    {
        int i;

        // Check that there is a current choice point to tidy down to, otherwise tidy down to the root of the trail.
        if (bp == 0)
        {
            i = TRAIL_BASE;
        }
        else
        {
            i = data.get(bp + data.get(bp) + 5);
        }

        while (i < trp)
        {
            int addr = data.get(i);

            if ((addr < hbp) || ((hp < addr) && (addr < bp)))
            {
                i++;
            }
            else
            {
                data.put(i, data.get(trp - 1));
                trp--;
            }
        }
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
                if ((t1 == WAMInstruction.REF))
                {
                    bind(d1, d2);
                }
                else if (t2 == WAMInstruction.REF)
                {
                    bind(d1, d2);
                }
                else if (t2 == WAMInstruction.STR)
                {
                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    int fn1 = data.get(v1);
                    int fn2 = data.get(v2);
                    byte n1 = (byte) (fn1 >>> 24);

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
                else if (t2 == WAMInstruction.CON)
                {
                    if ((t1 != WAMInstruction.CON) || (v1 != v2))
                    {
                        fail = true;
                    }
                }
                else if (t2 == WAMInstruction.LIS)
                {
                    if (t1 != WAMInstruction.LIS)
                    {
                        fail = true;
                    }
                    else
                    {
                        uPush(v1);
                        uPush(v2);
                        uPush(v1 + 1);
                        uPush(v2 + 1);
                    }
                }
            }
        }

        return !fail;
    }

    /**
     * A simplified unification algorithm, for unifying against a constant.
     *
     * <p/>Attempts to unify a constant or references on the heap, with a constant. If the address leads to a free
     * variable on dereferencing, the variable is bound to the constant. If the address leads to a constant it is
     * compared with the passed in constant for equality, and unification succeeds when they are equal.
     *
     * @param  fn   The constant to unify with.
     * @param  addr The address of the first constant or reference.
     *
     * @return <tt>true</tt> if the two constant unify, <tt>false</tt> otherwise.
     */
    private boolean unifyConst(int fn, int addr)
    {
        boolean success;

        int deref = deref(addr);
        int tag = derefTag;
        int val = derefVal;

        // case STORE[addr] of
        switch (tag)
        {
        case REF:
        {
            // <REF, _> :
            // STORE[addr] <- <CON, c>
            data.put(deref, constantCell(fn));

            // trail(addr)
            trail(deref);

            success = true;

            break;
        }

        case CON:
        {
            // <CON, c'> :
            // fail <- (c != c');
            success = val == fn;

            break;
        }

        default:
        {
            // other: fail <- true;
            success = false;
        }
        }

        return success;
    }

    /**
     * Pushes a value onto the unification stack.
     *
     * @param val The value to push onto the stack.
     */
    private void uPush(int val)
    {
        data.put(--up, val);
    }

    /**
     * Pops a value from the unification stack.
     *
     * @return The top value from the unification stack.
     */
    private int uPop()
    {
        return data.get(up++);
    }

    /** Clears the unification stack. */
    private void uClear()
    {
        up = TOP;
    }

    /**
     * Checks if the unification stack is empty.
     *
     * @return <tt>true</tt> if the unification stack is empty, <tt>false</tt> otherwise.
     */
    private boolean uEmpty()
    {
        return up >= TOP;
    }

    /**
     * Pretty prints a variable allocation slot for tracing purposes.
     *
     * @param  xi   The allocation slot to print.
     * @param  mode The addressing mode, stack or register.
     *
     * @return The pretty printed slot.
     */
    private String printSlot(int xi, int mode)
    {
        return ((mode == STACK_ADDR) ? "Y" : "X") + ((mode == STACK_ADDR) ? (xi - ep - 3) : xi);
    }
}
