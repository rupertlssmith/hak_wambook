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

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicInteger;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.MarkerTerm;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.Variable;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.CALL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.GET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PROCEED;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_STRUC;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.PUT_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.SET_VAR;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l1.L1InstructionSet.UNIFY_VAR;
import com.thesett.common.util.ByteBufferUtils;

/**
 * An L1CompiledFunctor is a {@link Functor} which has been compiled to byte code. In order to implement the methods of
 * {@link Functor}, to be traversable as an abstract syntax tree, the byte code needs to be decompiled to restore the
 * abstract syntax tree of the original. The byte code for queries and program statements is compiled differently, hence
 * this class abstracts out the common part of the decompilation process and delegates some of it to the conrete
 * sub-classes.
 *
 * <p/>A compiled functor is always in the context of an {@link L1Machine}, which holds its byte code in its code area
 * and which contains the symbol tables for converting interned functors and variable names back into strings. Interned
 * variable names are discarded at compile time, unlike functor names which are encoded into the instructions. Variables
 * within the context of a functor instance are compiled to a particular register, so to recover the variables that are
 * assigned during unification or to decompile functors from byte code, a mapping from registers to variable names is
 * needed. This is stored in the compiled functor as an array of integers, indexed by registers.
 *
 * <p/>Decompiling the byte code to recover the abstract syntax tree, may seem unnecessary, because the code was
 * compiled from such a tree, which could have been retained. It is done to make it possible to load byte code from a
 * file, once the original syntax tree has been discarded, and recover that tree in order to manipulate and query the
 * code through its syntax tree. This also means that writing a decompiler tool for the language will be trivial and
 * part of its toolset from the outset.
 *
 * <p/>Queries are compiled in two passes, one using a bread first ordering to write out register value for parts of the
 * query out-in, and one to generate the instructions for a query in a post-fix order, such that arguments are
 * instantiated in registers, in-out, before their enclosing functors are encountered. The resulting instruction stream
 * loads values into registers, then writes out the functor, then writes out the arguments to the functor from the
 * loaded register. This combination of forward and backward phases means that when decompiling queries the isntruction
 * stream is explored in reverse, with forward exploration to extract the registers that hold a query functors
 * arguments. Program functors can be decompiled with forward exploration only.
 *
 * <p/>The decompilation process has been written using 3 passes over the instruction stream. The first pass walks over
 * the stream, picking out the offsets of all put_struc instructions, which identify the functors in the instructions.
 * The second pass walks over the instruction stream, using the functor instruction offsets from the first step. The
 * first step is done because an instruction argument could have the same value as the put_struc instruction, so a
 * backward pass (in the case of queries) cannot differentiate between instructions and arguments without the first
 * step. In pass two, every register write-out or unify instruction results in a pending value for later completion to
 * be added to the functors arguments in step 3. Every functor, or variable creation results in a new functor or
 * variable being created, and noting which register it is loaded into. The third pass walks recursively down the
 * abstract syntax tree, filling in all arguments pending completion from the register load instructions.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Associate a compiled functor with a byte code machine.
 * <tr><td> Provide the byte code for the functor.
 * <tr><td> Decompile the functor from the byte code, on demand.
 * <tr><td> Provide a mapping from registers to variable id's.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Instead of copying the byte code out of the machine, before decompiling it. Use a ref to a byte[] and and
 *         offset and length. Then for the Java machine can just use offset/length into the code buffer in the machine.
 *         For native machine, with direct buffers, doing a bulk copy before the finer byte by byte iteration over the
 *         code will be faster. So do copy it out and use a zero offset on the fresh array. Need to pass down offset
 *         parameter into decompilation methods.
 */
public abstract class L1CompiledFunctor extends Functor implements L1CompiledTerm, Sentence<L1CompiledFunctor>
{
    /** Static counter for inventing new variable names. */
    protected static AtomicInteger varNameId = new AtomicInteger();

    /** Holds the register to variable id mapping for the functor. */
    Map<Byte, Integer> varNames;

    /** Holds the offset of the compiled code for the functor within the machine it is compiled to. */
    L1CallTableEntry callTableEntry;

    /** Holds a reference to the byte code machine, that provides the symbol table for the code. */
    L1Machine machine;

    /** Flag used to indicate when the functor has been decompiled, so that it can be done on demand. */
    protected boolean decompiled;

    /**
     * Builds a compiled down L1 functor on a buffer of compiled code.
     *
     * @param machine        The L1 byte code machine that the functor has been compiled to.
     * @param callTableEntry The offset of the code buffer within its compiled to machine.
     * @param varNames       A mapping from register to variable names.
     */
    public L1CompiledFunctor(L1Machine machine, L1CallTableEntry callTableEntry, Map<Byte, Integer> varNames)
    {
        super(-1, null);

        this.callTableEntry = callTableEntry;
        this.varNames = varNames;
        this.machine = machine;
    }

    /**
     * Gets the location of the compiled code for the term within its compiled to machine.
     *
     * @return The location of the compiled code for the term.
     */
    public L1CallTableEntry getCallTableEntry()
    {
        return callTableEntry;
    }

    /**
     * Gets the wrapped sentence in the logical language over fcuntors.
     *
     * @return The wrapped sentence in the logical language.
     */
    public L1CompiledFunctor getT()
    {
        return this;
    }

    /**
     * Reports whether or not this term is a constant (a number of a functor of arity zero).
     *
     * @return <tt>true</tt> if this term is constant, <tt>false</tt> otherwise.
     */
    public boolean isConstant()
    {
        // Ensure that the functors abstract syntax tree has been decompiled first.
        if (!decompiled)
        {
            decompile();
        }

        // Delegate to the super class.
        return super.isConstant();
    }

    /** Decompiles the term. */
    public void decompile()
    {
        // Ensure that the functors is only decompiled when it has not already been.
        if (!decompiled)
        {
            decompileFunctor(machine.getByteCode(callTableEntry));
        }
    }

    /**
     * Gets an instance of a queue implementation, to hold the functor and variable creating instruction start offsets
     * in. Queries and programs can provide different implementations, in order to scan the instructions forward or
     * backwards.
     *
     * @return A queue to hold the instruction start offsets in.
     */
    protected abstract Queue<Integer> getInstructionQueue();

    /**
     * Decompiles this single functor and fills in its arguments from the code buffer. A query functor is presented as a
     * put_struc followed by register-to-heap writes to fill in its arguments on the heap. The arguments themselves are
     * assinged to registers by put_strucs that precede the functor in the code buffer. So parsing a functor consists of
     * getting the functors and their register assignments and then working recusively down all the functors arguments,
     * filling them in from the register assignments seen earlier in the code stream.
     *
     * <p/>Prior to parsing the code, a single pass is made through it to pick out all the instruction start offsets.
     * This is done because parsing the code backwards is difficult, as different instructions can have different
     * numbers of arguments, and there is nothing to stop arguments having the same values as instructions and being
     * mistaken for them.
     *
     * @param code The code buffer.
     */
    protected void decompileFunctor(byte[] code)
    {
        // Prepare the code for parsing by making a single forward pass through it to pick out all the instructions.
        // The starting offset is set to the last instruction.
        Queue<Integer> instructions = getInstructionQueue();
        instructionScan(code, instructions);

        // Used to fill in known register values as they are encountered.
        Map<Byte, Term> registers = new HashMap<Byte, Term>();

        // Create a map of variables, for all variables in the context of the query.
        Map<Integer, Variable> variableContext = new HashMap<Integer, Variable>();

        // Parse this as the outermost functor.
        decompileOuterFunctor(code, instructions, registers, variableContext, this);

        // Keep pulling out the remaining functors, creating them as new instances.
        while (!instructions.isEmpty())
        {
            decompileInstructions(code, instructions, registers, variableContext, null);
        }

        // Now recursively fill in the arguments, by looking up the registers.
        completeArguments(registers, this);

        decompiled = true;
    }

    /**
     * Fills in the arguments of this functor from the register values, recursively chaining the process down into any
     * arguments that are themselves functors. Although recursion is used, it is not likely to exhaust the stack as
     * functor nesting of that depth is unusual.
     *
     * @param registers  The register values to fill in arguments from.
     * @param toComplete The functor to recursively fill in the register arguments of.
     */
    protected void completeArguments(Map<Byte, Term> registers, Functor toComplete)
    {
        // Parse each of the arguments in turn.
        for (int i = toComplete.getArity() - 1; i >= 0; i--)
        {
            // Check if the argument is pending completion from a register, and fetch it from the decompiled
            // registers if so.
            if (toComplete.getArgument(i) instanceof RegisterArg)
            {
                toComplete.setArgument(i, registers.get(((RegisterArg) toComplete.getArgument(i)).register));
            }

            // Check if the argument is a functor to be filled in recursively, and step down into if so.
            if (toComplete.getArgument(i) instanceof Functor)
            {
                completeArguments(registers, (Functor) toComplete.getArgument(i));
            }
        }
    }

    /**
     * Decompiles the outermost functor from its byte code. The registers containing the functors arguments are also
     * gathered, for later completion by subsequent parsing of inner functors in the code buffer.
     *
     * @param  code            The code buffer.
     * @param  instructions    A queue of offsets of functor or variable creating instructions.
     * @param  registers       The register collection to store register values in.
     * @param  variableContext A mapping from variable id's to variables, to scope variables to the program sentence.
     * @param  inPlaceQuery    The compiled L1 query to decompile in to, this is used to decompile this. This argument
     *                         may be <tt>null</tt> in which case a new query functor is created an decompiled into when
     *                         a functor is encountered.
     *
     * @return The outermost functor, decompiled from the byte code isntructions.
     */
    protected Term decompileOuterFunctor(byte[] code, Queue<Integer> instructions, Map<Byte, Term> registers,
        Map<Integer, Variable> variableContext, L1CompiledFunctor inPlaceQuery)
    {
        // Used to hold the functors interned name.
        int f;

        // Programs:
        // Look up the code offset in the machines call table to get the functors name and arity.
        if (this instanceof L1CompiledProgramFunctor)
        {
            f = callTableEntry.name;
        }

        // Queries:
        // Pull out the first insruction and get the f/n from the call.
        else
        {
            int addr = instructions.remove();

            // Ensure that the query really does end with a call.
            if (code[addr] != CALL)
            {
                throw new IllegalStateException("Call instructions should be the last instruction of a query.");
            }

            // Skip the call instruction and call offset.
            addr += 5;

            // Extract fn.
            int fn = ByteBufferUtils.getIntFromBytes(code, addr);
            f = (fn & 0xFFFFFF00) >> 8;
        }

        // Look up and initialize this functor name from the symbol table.
        FunctorName name = machine.getDeinternedFunctorName(f);

        // Fill in this functors name and arity and arguments.
        this.name = f;
        this.arity = name.getArity();

        // Arguments to a predicate call will always be held in the first n registers, so create temporary
        // register argument place holders for these, for later filling in once the arguments themselves have
        // been decompiled.
        if (name.getArity() > 0)
        {
            arguments = new Term[name.getArity()];

            for (byte i = 0; i < this.arity; i++)
            {
                arguments[i] = new RegisterArg(i);
            }
        }

        return this;
    }

    /**
     * Works through the instruction queue processing until a put_struc or get_struc is encountered, signifying the
     * start of a functor, or a _var instruction is encountered that signifies the creation point of a variable. For
     * functors, the parsing reverses direction and the functors register arguments are gathered, for later completion
     * by subsequent parsing of earlier functors in the code buffer.
     *
     * @param  code            The code buffer.
     * @param  instructions    A queue of offsets of functor or variable creating instructions.
     * @param  registers       The register collection to store register values in.
     * @param  variableContext A mapping from variable id's to variables, to scope variables to the program sentence.
     * @param  inPlaceQuery    The compiled L1 query to decompile in to, this is used to decompile this. This argument
     *                         may be <tt>null</tt> in which case a new query functor is created an decompiled into when
     *                         a functor is encountered.
     *
     * @return A functor or variable, decompiled from the byte code instructions.
     */
    protected Term decompileInstructions(byte[] code, Queue<Integer> instructions, Map<Byte, Term> registers,
        Map<Integer, Variable> variableContext, L1CompiledFunctor inPlaceQuery)
    {
        Term result = null;

        // Loop backwards until functor or variable is encountered.
        while (!instructions.isEmpty())
        {
            int next = instructions.remove();

            byte instruction = code[next];

            // All instructions except call and proceed have at least one register argument.
            byte xi = code[next + 1];

            // If the instruction is a put_struc or get_struc then create a new functor or fill this one in, leaving
            // markers to fill in the arguments later.
            if ((instruction == PUT_STRUC) || (instruction == GET_STRUC))
            {
                // Grab and decode the functors interned id.
                int fn = ByteBufferUtils.getIntFromBytes(code, next + 2);
                int f = (fn & 0xFFFFFF00) >> 8;
                // int f = ByteBufferUtils.get24BitIntFromBytes(code, next + 2);

                // Look up and initialize this functor name from the symbol table.
                FunctorName name = machine.getDeinternedFunctorName(f);

                // Check if decompiling this, or creating a new functor.
                Functor functor = null;

                // Gather all the argument references.
                Term[] arguments = null;

                if (name.getArity() > 0)
                {
                    int argOffset = next + 6;
                    arguments = new Term[name.getArity()];

                    for (int i = 0; i < name.getArity(); i++)
                    {
                        instruction = code[argOffset++];

                        byte argXi = code[argOffset++];

                        // Ensure that the instruciton is set_val or set_var (it should be).
                        if ((instruction == SET_VAL) || (instruction == SET_VAR) || (instruction == UNIFY_VAL) ||
                                (instruction == UNIFY_VAR))
                        {
                            // Save register and functor and arg position that register goes in, for later filling in
                            // once the register argument is known from earlier code.
                            arguments[i] = new RegisterArg(argXi);
                        }
                    }
                }

                // If this functor is to be filled in, in place then decompile into this.
                if (inPlaceQuery != null)
                {
                    functor = this;

                    // Fill in this functors name and arity and arguments.
                    this.name = f;
                    this.arity = name.getArity();
                    this.arguments = arguments;

                }

                // Create new functors when not decompiling into this.
                else
                {
                    // query = new L1CompiledQueryFunctor(machine, code);
                    functor = new Functor(f, arguments);
                }

                // Associate this functor with the register it is loaded into.
                registers.put(xi, functor);

                result = functor;

                break;
            }

            // If its set_var, unify_var or get_var create variable.
            else if ((instruction == SET_VAR) || (instruction == UNIFY_VAR) || (instruction == GET_VAR))
            {
                // Look up the registers corresponding variable name in the symbol tables.
                int varName = varNames.get(xi);
                // machine.getVariableInterner().getAttributeFromInt(varName).getValue();

                // Check if a variable for that name has already been created for this query, and use it if so.
                Variable var = variableContext.get(varName);

                if (var == null)
                {
                    var = new Variable(varName, null, false);

                    variableContext.put(varName, var);
                }

                // Associate this variable with the register it is loaded into.
                registers.put(xi, var);
            }

            // If its put_var or get_var create variable, but take note of the move of the variable register into
            // an argument register.
            else if (instruction == PUT_VAR)
            {
                byte ai = code[next + 2];

                // Look up the registers corresponding variable name in the symbol tables.
                int varName = varNames.get(ai);
                // machine.getVariableInterner().getAttributeFromInt(varName).getValue();

                // Check if a variable for that name has already been created for this query, and use it if so.
                Variable var = variableContext.get(varName);

                if (var == null)
                {
                    var = new Variable(varName, null, false);

                    variableContext.put(varName, var);
                }

                // Associate this variable with the register it is loaded into.
                registers.put(xi, var);
            }
        }

        // Set ref to the point where the parsing stopped.

        return result;
    }

    /**
     * Makes a forward scan though the supplied code buffer, picking out the offsets of the start of each instruction,
     * that indicates a functor or variable creation point, so that later parsing can find these instructions without
     * confusing arguments with the same values as instructions for instructions, in a backward scan.
     *
     * <p/>This scan picks out the offsets of the following instructions:
     *
     * <pre><p/><table>
     * <tr><th> Instruction <th> Size <th> Comment
     * <tr><td> PUT_STRUC   <td>  6   <td> Marks the start of a functor in a query.
     * <tr><td> GET_STRUC   <td>  6   <td> Marks the start of a functor in a program.
     * <tr><td> SET_VAR     <td>  2   <td> Marks the start of a variable in a query.
     * <tr><td> UNIFY_VAR   <td>  2   <td> If it corresponds to a variable, marks the start of a variable in a program.
     * <tr><td> PUT_VAR     <td>  3   <td> Marks the start of a variable in a query.
     * <tr><td> GET_VAR     <td>  3   <td> Marks the start of a variable in a program.
     * <tr><td> CALL        <td>  9   <td> Marks the end of a query functor.
     * </table></pre>
     *
     * @param code    The code buffer to scan for instructions.
     * @param offsets The list to store the instruction start offsets in.
     */
    protected void instructionScan(byte[] code, Queue<Integer> offsets)
    {
        for (int i = 0; i < code.length;)
        {
            // Get the next instruction.
            byte instruction = code[i];

            // Keep the position of the instruction starts that correspond to functor and variable creation points.
            switch (instruction)
            {
            case PUT_STRUC:
            case GET_STRUC:
            case SET_VAR:
            case PUT_VAR:
            case CALL:
            {
                offsets.offer(i);
                break;
            }

            case UNIFY_VAR:
            case GET_VAR:
            {
                byte xi = code[i + 1];

                if (varNames.containsKey(xi))
                {
                    offsets.offer(i);
                }

                break;
            }

            default:
            {
                throw new IllegalStateException("Unkown instruction type.");
            }
            }

            // Skip the instruction pointer ahead by the size of the instruction.
            i += instructionSize(instruction);
        }
    }

    /**
     * Calculates the size of instructions plus arguments in bytes. Instructions have the following sizes:
     *
     * <pre><p/><table>
     * <tr><th> Instruction <th> Size
     * <tr><td> PUT_STRUC   <td>  6
     * <tr><td> GET_STRUC   <td>  6
     * <tr><td> SET_VAR     <td>  2
     * <tr><td> SET_VAL     <td>  2
     * <tr><td> UNIFY_VAR   <td>  2
     * <tr><td> UNIFY_VAL   <td>  2
     * <tr><td> PUT_VAR     <td>  3
     * <tr><td> GET_VAR     <td>  3
     * <tr><td> CALL        <td>  9
     * <tr><td> PROCEED     <td>  1
     * </table></pre>
     *
     * @param  instruction The instruction to calculate the size of.
     *
     * @return The size of the specified instruction.
     */
    private int instructionSize(byte instruction)
    {
        switch (instruction)
        {
        case PUT_STRUC:
        case GET_STRUC:
        {
            return 6;
        }

        case CALL:
        {
            return 9;
        }

        case PUT_VAR:
        case PUT_VAL:
        case GET_VAR:
        case GET_VAL:
        {
            return 3;
        }

        case SET_VAR:
        case SET_VAL:
        case UNIFY_VAR:
        case UNIFY_VAL:
        {
            return 2;
        }

        case PROCEED:
        {
            return 1;
        }

        default:
        {
            throw new IllegalStateException("Unkown instruction type.");
        }
        }
    }

    /**
     * Represents a functor argument that will be filled in from a register on a later pass of the decompiler.
     *
     * <pre><p/><table id="crc"><caption>CRC Card</caption>
     * <tr><th> Responsibilities <th> Collaborations
     * <tr><td> Represents an argument pending completion from a register.
     * </table></pre>
     */
    protected static class RegisterArg extends MarkerTerm
    {
        /** Holds the register that this argument will be filled in from. */
        public byte register;

        /**
         * Creates a pending argument to be filled in later from the specified regisgter.
         *
         * @param register The register to get the argument from.
         */
        public RegisterArg(byte register)
        {
            this.register = register;
        }

        /**
         * Prints the pending register value for debugging purposes.
         *
         * @return A string containing the register value.
         */
        public String toString()
        {
            return "RegisterArg: [ register = " + register + " ]";
        }
    }
}
