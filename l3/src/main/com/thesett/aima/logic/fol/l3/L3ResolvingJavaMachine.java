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

import java.util.Iterator;
import java.util.Set;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.Variable;
import static com.thesett.aima.logic.fol.l3.L3Instruction.ALLOCATE;
import static com.thesett.aima.logic.fol.l3.L3Instruction.CALL;
import static com.thesett.aima.logic.fol.l3.L3Instruction.DEALLOCATE;
import static com.thesett.aima.logic.fol.l3.L3Instruction.GET_STRUC;
import static com.thesett.aima.logic.fol.l3.L3Instruction.GET_VAL;
import static com.thesett.aima.logic.fol.l3.L3Instruction.GET_VAR;
import static com.thesett.aima.logic.fol.l3.L3Instruction.PROCEED;
import static com.thesett.aima.logic.fol.l3.L3Instruction.PUT_STRUC;
import static com.thesett.aima.logic.fol.l3.L3Instruction.PUT_VAL;
import static com.thesett.aima.logic.fol.l3.L3Instruction.PUT_VAR;
import static com.thesett.aima.logic.fol.l3.L3Instruction.REF;
import static com.thesett.aima.logic.fol.l3.L3Instruction.RETRY_ME_ELSE;
import static com.thesett.aima.logic.fol.l3.L3Instruction.SET_VAL;
import static com.thesett.aima.logic.fol.l3.L3Instruction.SET_VAR;
import static com.thesett.aima.logic.fol.l3.L3Instruction.STACK_ADDR;
import static com.thesett.aima.logic.fol.l3.L3Instruction.STR;
import static com.thesett.aima.logic.fol.l3.L3Instruction.SUSPEND;
import static com.thesett.aima.logic.fol.l3.L3Instruction.TRUST_ME;
import static com.thesett.aima.logic.fol.l3.L3Instruction.TRY_ME_ELSE;
import static com.thesett.aima.logic.fol.l3.L3Instruction.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l3.L3Instruction.UNIFY_VAR;
import com.thesett.common.util.ByteBufferUtils;
import com.thesett.common.util.SequenceIterator;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * L3ResolvingJavaMachine is a byte code interpreter for L3 written in java. This is a direct implementation of the
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
 * <tr><td> Execute compiled L3 programs and queries.
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
public class L3ResolvingJavaMachine extends L3ResolvingMachine
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L3ResolvingJavaMachine.class.getName()); */

    /** Used for tracing instruction executions. */
    private static final java.util.logging.Logger trace =
        java.util.logging.Logger.getLogger("TRACE.L3ResolvingJavaMachine");

    /** Defines the register capacity for the virtual machine. */
    private static final int REG_SIZE = 10;

    /** Defines the heap size to use for the virtual machine. */
    private static final int HEAP_SIZE = 10000;

    /** Defines the offset of the base of the heap in the data area. */
    private static final int HEAP_BASE = REG_SIZE;

    /** Defines the stack size to use for the virtual machine. */
    private static final int STACK_SIZE = 10000;

    /** Defines the offset of the base of the stack in the data area. */
    private static final int STACK_BASE = REG_SIZE + HEAP_SIZE;

    /** Defines the trail size to use for the virtual machine. */
    private static final int TRAIL_SIZE = 10000;

    /** Defines the offset of the base of the trail in the data area. */
    private static final int TRAIL_BASE = REG_SIZE + HEAP_SIZE + STACK_SIZE;

    /** Defines the max unification stack depth for the virtual machine. */
    private static final int PDL_SIZE = 1000;

    /** Defines the highest address in the data area of the virtual machine. */
    private static final int TOP = REG_SIZE + HEAP_SIZE + STACK_SIZE + TRAIL_SIZE + PDL_SIZE;

    /** Defines the initial code area size for the virtual machine. */
    private static final int CODE_SIZE = 10000;

    /** Holds the byte code. */
    private byte[] code;

    /** Holds the current load offset within the code area. */
    private int loadPoint;

    /** Holds the current instruction pointer into the code. */
    private int ip;

    /** Holds the entire data segment of the machine. All registers, heaps and stacks are held in here. */
    private int[] data;

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
     * Creates a unifying virtual machine for L3 with default heap sizes.
     *
     * @param symbolTable The symbol table for the machine.
     */
    public L3ResolvingJavaMachine(SymbolTable<Integer, String, Object> symbolTable)
    {
        super(symbolTable);

        // Reset the machine to its initial state.
        reset();
    }

    /** {@inheritDoc} */
    public void emmitCode(L3CompiledPredicate predicate) throws LinkageException
    {
        // Keep track of the offset into which the code was loaded.
        int entryPoint = loadPoint;
        int length = (int) predicate.sizeof();

        // Store the predicates entry point in the call table.
        L3CallPoint callPoint = setCodeAddress(predicate.getName(), entryPoint, loadPoint - entryPoint);

        // Emmit code for the clause into this machine.
        predicate.emmitCode(loadPoint, code, this, callPoint);
        loadPoint += length;
    }

    /** {@inheritDoc} */
    public void emmitCode(L3CompiledQuery query) throws LinkageException
    {
        // Keep track of the offset into which the code was loaded.
        int entryPoint = loadPoint;
        int length = (int) query.sizeof();

        // If the code is for a program clause, store the programs entry point in the call table.
        L3CallPoint callPoint = new L3CallPoint(loadPoint, length, -1);

        // Emmit code for the clause into this machine.
        query.emmitCode(loadPoint, code, this, callPoint);
        loadPoint += length;
    }

    /** {@inheritDoc} */
    public void emmitCode(int offset, int address)
    {
        ByteBufferUtils.writeIntToByteArray(code, offset, address);
    }

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] retrieveCode(L3CallPoint callPoint)
    {
        byte[] result = new byte[callPoint.length];

        System.arraycopy(code, callPoint.entryPoint, result, 0, callPoint.length);

        return result;
    }

    /**
     * Resets the machine, to its initial state. This clears any programs from the machine, and clears all of its stacks
     * and heaps.
     */
    public void reset()
    {
        // Create fresh heaps, code areas and stacks.
        data = new int[TOP];
        code = new byte[CODE_SIZE];

        // Registers are on the top of the data area, the heap comes next.
        hp = HEAP_BASE;
        hbp = HEAP_BASE;
        sp = HEAP_BASE;

        // The stack comes after the heap. Pointers are zero initially, since no stack frames exist yet.
        ep = 0;
        bp = 0;

        // The trail comes after the stack.
        trp = TRAIL_BASE;

        // The unification stack (PDL) is a push down stack at the end of the data area.
        up = TOP;

        // Turn off write mode.
        writeMode = false;

        // Reset the instruction pointer to that start of the code area, ready for fresh code to be loaded there.
        ip = 0;
        loadPoint = 0;

        // Could probably not bother resetting these, but will do it anyway just to be sure.
        derefTag = 0;
        derefVal = 0;

        // The machine is initially not suspended.
        suspended = false;

        // Ensure that the overridden reset method of L3BaseMachine is run too, to clear the call table.
        super.reset();
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
        throw new UnsupportedOperationException("L3ResolvingJavaMachine does not support max steps limit on search.");
    }

    /** {@inheritDoc} */
    protected int derefStack(int a)
    {
        /*log.fine("Stack deref from " + (a + ep + 3) + ", ep = " + ep);*/

        return deref(a + ep + 3);
    }

    /** {@inheritDoc} */
    protected boolean execute(L3CallPoint callPoint)
    {
        /*log.fine("protected boolean execute(L3CallPoint callPoint): called");*/

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
        int cp = loadPoint;

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
            byte instruction = code[ip];

            switch (instruction)
            {
            // put_struc Xi, f/n:
            case PUT_STRUC:
            {
                // grab addr, f/n
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                int fn = ByteBufferUtils.getIntFromBytes(code, ip + 3);

                trace.fine(ip + ": PUT_STRUC " + printSlot(xi, mode) + ", " + fn);

                // heap[h] <- STR, h + 1
                data[hp] = (L3Instruction.STR << 24) | ((hp + 1) & 0xFFFFFF);

                // heap[h+1] <- f/n
                data[hp + 1] = fn;

                // Xi <- heap[h]
                data[xi] = data[hp];

                // h <- h + 2
                hp += 2;

                // P <- instruction_size(P)
                ip += 7;

                break;
            }

            // set_var Xi:
            case SET_VAR:
            {
                // grab addr
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

                trace.fine(ip + ": SET_VAR " + printSlot(xi, mode));

                // heap[h] <- REF, h
                data[hp] = (L3Instruction.REF << 24) | (hp & 0xFFFFFF);

                // Xi <- heap[h]
                data[xi] = data[hp];

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
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

                trace.fine(ip + ": SET_VAL " + printSlot(xi, mode));

                // heap[h] <- Xi
                data[hp] = data[xi];

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
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                int fn = ByteBufferUtils.getIntFromBytes(code, ip + 3);

                trace.fine(ip + ": GET_STRUC " + printSlot(xi, mode) + ", " + fn);

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
                    data[hp] = (L3Instruction.STR << 24) | ((hp + 1) & 0xFFFFFF);

                    // heap[h+1] <- f/n
                    data[hp + 1] = fn;

                    // bind(addr, h)
                    //data[addr] = (L3Instruction.REF << 24) | (hp & 0xFFFFFF);
                    bind(addr, hp);

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
                    if (data[a] == fn)
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
                }

                // P <- instruction_size(P)
                ip += 7;

                break;
            }

            // unify_var Xi:
            case UNIFY_VAR:
            {
                // grab addr
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

                trace.fine(ip + ": UNIFY_VAR " + printSlot(xi, mode));

                // switch mode
                if (!writeMode)
                {
                    // case read:
                    // Xi <- heap[s]
                    data[xi] = data[sp];

                }
                else
                {
                    // case write:
                    // heap[h] <- REF, h
                    data[hp] = (L3Instruction.REF << 24) | (hp & 0xFFFFFF);

                    // Xi <- heap[h]
                    data[xi] = data[hp];

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
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);

                trace.fine(ip + ": UNIFY_VAL " + printSlot(xi, mode));

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
                    data[hp] = data[xi];

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
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                byte ai = code[ip + 3];

                trace.fine(ip + ": PUT_VAR " + printSlot(xi, mode) + ", A" + ai);

                // heap[h] <- REF, H
                data[hp] = (L3Instruction.REF << 24) | (hp & 0xFFFFFF);

                // Xn <- heap[h]
                data[xi] = data[hp];

                // Ai <- heap[h]
                data[ai] = data[hp];

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
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                byte ai = code[ip + 3];

                trace.fine(ip + ": PUT_VAL " + printSlot(xi, mode) + ", A" + ai);

                // Ai <- Xn
                data[ai] = data[xi];

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // get var Xn, Ai:
            case GET_VAR:
            {
                // grab addr, Ai
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                byte ai = code[ip + 3];

                trace.fine(ip + ": GET_VAR " + printSlot(xi, mode) + ", A" + ai);

                // Xn <- Ai
                data[xi] = data[ai];

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // get_val Xn, Ai:
            case GET_VAL:
            {
                // grab addr, Ai
                byte mode = code[ip + 1];
                int xi = (int) code[ip + 2] + ((mode == STACK_ADDR) ? (ep + 3) : 0);
                byte ai = code[ip + 3];

                trace.fine(ip + ": GET_VAL " + printSlot(xi, mode) + ", A" + ai);

                // unify (Xn, Ai)
                failed = !unify(xi, ai);

                // P <- P + instruction_size(P)
                ip += 4;

                break;
            }

            // call @(p/n):
            case CALL:
            {
                // grab @(p/n)
                int pn = ByteBufferUtils.getIntFromBytes(code, ip + 1);
                int n = code[ip + 5];

                // num_of_args <- n
                numOfArgs = n;

                // CP <- P + instruction_size(P)
                cp = ip + 6;

                trace.fine(ip + ": CALL " + pn + "/" + n + " (cp = " + cp + ")]");

                // Ensure that the predicate to call is known and linked in, otherwise fail.
                if (pn == -1)
                {
                    failed = true;

                    break;
                }

                // P <- @(p/n)
                ip = pn;

                break;
            }

            // proceed:
            case PROCEED:
            {
                trace.fine(ip + ": PROCEED" + " (cp = " + cp + ")]");

                // P <- CP
                ip = cp;

                break;
            }

            // allocate N:
            case ALLOCATE:
            {
                // grab N
                int n = (int) code[ip + 1];

                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newE] <- E
                data[esp] = ep;

                // STACK[E + 1] <- CP
                data[esp + 1] = cp;

                // STACK[E + 2] <- N
                data[esp + 2] = n;

                // E <- newE
                // newE <- E + n + 3
                ep = esp;
                esp = esp + n + 3;

                trace.fine(ip + ": ALLOCATE");
                trace.fine("-> env @ " + ep + " " + traceEnvFrame());

                // P <- P + instruction_size(P)
                ip += 2;

                break;
            }

            // deallocate:
            case DEALLOCATE:
            {
                int newip = data[ep + 1];

                // E <- STACK[E]
                ep = data[ep];

                trace.fine(ip + ": DEALLOCATE");
                trace.fine("<- env @ " + ep + " " + traceEnvFrame());

                // P <- STACK[E + 1]
                ip = newip;

                break;
            }

            // try me else L:
            case TRY_ME_ELSE:
            {
                // grab L
                int l = (int) code[ip + 1];

                // if E > B
                //  then newB <- E + STACK[E + 2] + 3
                // else newB <- B + STACK[B] + 7
                int esp = nextStackFrame();

                // STACK[newB] <- num_of_args
                // n <- STACK[newB]
                int n = numOfArgs;
                data[esp] = n;

                // for i <- 1 to n do STACK[newB + i] <- Ai
                for (int i = 0; i < n; i++)
                {
                    data[esp + i + 1] = data[i];
                }

                // STACK[newB + n + 1] <- E
                data[esp + n + 1] = ep;

                // STACK[newB + n + 2] <- CP
                data[esp + n + 2] = cp;

                // STACK[newB + n + 3] <- B
                data[esp + n + 3] = bp;

                // STACK[newB + n + 4] <- L
                data[esp + n + 4] = l;

                // STACK[newB + n + 5] <- TR
                data[esp + n + 5] = trp;

                // STACK[newB + n + 6] <- H
                data[esp + n + 6] = hp;

                // B <- new B
                bp = esp;

                // HB <- H
                hbp = hp;

                trace.fine(ip + ": TRY_ME_ELSE");
                trace.fine("-> chp @ " + bp + " " + traceChoiceFrame());

                // P <- P + instruction_size(P)
                ip += 5;

                break;
            }

            // retry me else L:
            case RETRY_ME_ELSE:
            {
                // grab L
                int l = (int) code[ip + 1];

                // n <- STACK[B]
                int n = data[bp];

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data[i] = data[bp + i + 1];
                }

                // E <- STACK[B + n + 1]
                ep = data[bp + n + 1];

                // CP <- STACK[B + n + 2]
                cp = data[bp + n + 2];

                // STACK[B + n + 4] <- L
                data[bp + n + 4] = l;

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data[bp + n + 5], trp);

                // TR <- STACK[B + n + 5]
                trp = data[bp + n + 5];

                // H <- STACK[B + n + 6]
                hp = data[bp + n + 6];

                // HB <- H
                hbp = hp;

                trace.fine(ip + ": RETRY_ME_ELSE");
                trace.fine("-- chp @ " + bp + " " + traceChoiceFrame());

                // P <- P + instruction_size(P)
                ip += 5;

                break;
            }

            // trust me (else fail):
            case TRUST_ME:
            {
                // n <- STACK[B]
                int n = data[bp];

                // for i <- 1 to n do Ai <- STACK[B + i]
                for (int i = 0; i < n; i++)
                {
                    data[i] = data[bp + i + 1];
                }

                // E <- STACK[B + n + 1]
                ep = data[bp + n + 1];

                // CP <- STACK[B + n + 2]
                cp = data[bp + n + 2];

                // unwind_trail(STACK[B + n + 5], TR)
                unwindTrail(data[bp + n + 5], trp);

                // TR <- STACK[B + n + 5]
                trp = data[bp + n + 5];

                // H <- STACK[B + n + 6]
                hp = data[bp + n + 6];

                // HB <- STACK[B + n + 6]
                hbp = hp;

                // B <- STACK[B + n + 3]
                bp = data[bp + n + 3];

                trace.fine(ip + ": TRUST_ME");
                trace.fine("<- chp @ " + bp + " " + traceChoiceFrame());

                // P <- P + instruction_size(P)
                ip += 1;

                break;
            }

            // suspend on success:
            case SUSPEND:
            {
                trace.fine(ip + ": SUSPEND");
                ip += 1;
                suspended = true;

                return true;
            }
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
        return "env: [ ep = " + data[ep] + ", cp = " + data[ep + 1] + ", n = " + data[ep + 2] + "]";
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

        int n = data[bp];

        return "choice: [ n = " + data[bp] + ", ep = " + data[bp + n + 1] + ", cp = " + data[bp + n + 2] + ", bp = " +
            data[bp + n + 3] + ", l = " + data[bp + n + 4] + ", trp = " + data[bp + n + 5] + ", hp = " +
            data[bp + n + 6];
    }

    /** {@inheritDoc} */
    protected int deref(int a)
    {
        // tag, value <- STORE[a]
        int addr = a;
        int tmp = data[a];
        derefTag = (byte) ((tmp & 0xFF000000) >> 24);
        derefVal = tmp & 0x00FFFFFF;

        // while tag = REF and value != a
        while ((derefTag == L3Instruction.REF))
        {
            // tag, value <- STORE[a]
            addr = derefVal;
            tmp = data[derefVal];
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
        return data[addr];
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
            return ep + data[ep + 2] + 3;
        }
        else
        {
            return bp + data[bp] + 7;
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
            //   P <- STACK[B + STACK[B] + 4]
            ip = data[bp + data[bp] + 4];

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
        int t1 = (byte) ((data[a1] & 0xFF000000) >> 24);

        // <t2, _> <- STORE[a2]
        int t2 = (byte) ((data[a2] & 0xFF000000) >> 24);

        // if (t1 = REF) /\ ((t2 != REF) \/ (a2 < a1))
        if ((t1 == L3Instruction.REF) && ((t2 != L3Instruction.REF) || (a2 < a1)))
        {
            //  STORE[a1] <- STORE[a2]
            data[a1] = (L3Instruction.REF << 24) | (a2 & 0xFFFFFF);

            //  trail(a1)
            trail(a1);
        }
        else if (t2 == L3Instruction.REF)
        {
            //  STORE[a2] <- STORE[a1]
            data[a2] = (L3Instruction.REF << 24) | (a1 & 0xFFFFFF);

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
            data[trp] = addr;

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
            int tmp = data[addr];
            data[tmp] = (L3Instruction.REF << 24) | (tmp & 0xFFFFFF);
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
                if ((t1 == L3Instruction.REF))
                {
                    //data[d1] = (L3Instruction.REF << 24) | (d2 & 0xFFFFFF);
                    bind(d1, d2);
                }
                else if (t2 == L3Instruction.REF)
                {
                    //data[d2] = (L3Instruction.REF << 24) | (d1 & 0xFFFFFF);
                    bind(d1, d2);
                }
                else
                {
                    // f1/n1 <- STORE[v1]
                    // f2/n2 <- STORE[v2]
                    int fn1 = data[v1];
                    int fn2 = data[v2];
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
        data[--up] = val;
    }

    /**
     * Pops a value from the unification stack.
     *
     * @return The top value from the unification stack.
     */
    private int uPop()
    {
        return data[up++];
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
