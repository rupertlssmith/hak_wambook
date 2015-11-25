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
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.GET_STRUC;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.PUT_STRUC;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.SET_VAL;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.SET_VAR;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.UNIFY_VAL;
import static com.thesett.aima.logic.fol.l0.L0InstructionSet.UNIFY_VAR;
import com.thesett.common.util.ByteBufferUtils;

/**
 * A L0CompiledFunctor is a {@link Functor} which has been compiled to byte code. In order to implement the methods of
 * {@link Functor}, to be traversable as an abstract syntax tree, the byte code needs to be decompiled. The byte code
 * for queries and program statements is compiled differently, also functors on the heap have another representation,
 * hence this class is abstract and the decompilation implemented by different sub-classes.
 *
 * <p/>A compiled functor is always in the context of an {@link VariableAndFunctorInterner}, which contains the symbol
 * tables for converting interned functor and variable names back into strings. Interned variable names are discarded at
 * compile time, unlike functor names which are encoded into the instructions. Variables within the context of a functor
 * instance are compiled to a particular register, so to recover the variables that are assigned during unification or
 * to decompile functors from byte code, a mapping from registers to variable names is needed. This is stored in the
 * compiled functor as an array of integers, indexed by registers.
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
 */
public abstract class L0CompiledFunctor extends Functor implements L0CompiledTerm, Sentence<L0CompiledFunctor>
{
    /** Static counter for inventing new variable names. */
    protected static AtomicInteger varNameId = new AtomicInteger();

    /** Holds the register to variable id mapping for the functor. */
    Map<Byte, Integer> varNames;

    /** Holds the compiled code for the functor. */
    byte[] code;

    /** Holds a reference to the byte code machine, that provides the symbol table for the code. */
    VariableAndFunctorInterner machine;

    /** Flag used to indicate when the functor has been decompiled, so that it can be done on demand. */
    protected boolean decompiled;

    /**
     * Builds a compiled down L0 functor on a buffer of compiled code.
     *
     * @param machine  The L0 byte code machine that the functor has been compiled to.
     * @param code     The code buffer for the functor.
     * @param varNames A mapping from register to variable names.
     */
    public L0CompiledFunctor(VariableAndFunctorInterner machine, byte[] code, Map<Byte, Integer> varNames)
    {
        super(-1, null);

        this.code = code;
        this.varNames = varNames;
        this.machine = machine;
    }

    /**
     * Gets the wrapped sentence in the logical language over functors.
     *
     * @return The wrapped sentence in the logical language.
     */
    public L0CompiledFunctor getT()
    {
        return this;
    }

    /**
     * Gets the compiled code for the term.
     *
     * @return The compiled code for the term.
     */
    public byte[] getCode()
    {
        return code;
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
            decompileFunctor(code);
        }
    }

    /**
     * Disassembles the functor as a listing of the functors op codes and arguments, in a format similar to an assembly
     * language listing. This can be used for debugging purposes, or as intermediate output to expose the internal
     * workings of a compiler.
     *
     * @return The compiled functor as a listing of opcodes.
     */
    public String disassemble()
    {
        // Look up and initialize this functor name from the symbol table.
        FunctorName name = machine.getDeinternedFunctorName(getName());

        String result = "\n" + name.getName() + ":";

        for (int ip = 0; ip < code.length;)
        {
            // Get the instruction and register argument that follows it.
            byte instruction = code[ip++];
            byte xi = code[ip++];

            switch (instruction)
            {
            // put_struc Xi:
            case L0InstructionSet.PUT_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

                result += "\n    put_struc " + xi + ", " + fn;
                break;
            }

            // set_var Xi:
            case L0InstructionSet.SET_VAR:
            {
                result += "\n    set_var " + xi;
                break;
            }

            // set_val Xi:
            case L0InstructionSet.SET_VAL:
            {
                result += "\n    set_val " + xi;
                break;
            }

            // get_struc Xi,
            case L0InstructionSet.GET_STRUC:
            {
                // grab f/n
                int fn = ByteBufferUtils.getIntFromBytes(code, ip);
                ip += 4;

                result += "\n    get_struc " + xi + ", " + fn;
                break;
            }

            // unify_var Xi:
            case L0InstructionSet.UNIFY_VAR:
            {
                result += "\n    unify_var " + xi;
                break;
            }

            // unify_val Xi:
            case L0InstructionSet.UNIFY_VAL:
            {
                result += "\n    unify_val " + xi;
                break;
            }

            default:
            {
                throw new IllegalStateException("Unknown instruction.");
            }
            }
        }

        return result;
    }

    /**
     * Prints the contents of this compiled functor for debugging purposes.
     *
     * @return The contents of this compiled functor, as a string.
     */
    public String toString()
    {
        String result = "L0CompiledFunctor: [ name = " + name + ", arity = " + arity + ", arguments = [ ";

        for (int i = 0; i < arity; i++)
        {
            Term nextArg = arguments[i];
            result += ((nextArg != null) ? nextArg.toString() : "<null>") + ((i < (arity - 1)) ? ", " : " ");
        }

        result += " ], " + disassemble();
        result += " ]";

        return result;
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

        // Parse this as the first or last functor depending on the instruction queue order. Either way it will
        // correspond to the outermost functor for queries or programs alike.
        decompileInstructions(code, instructions, registers, variableContext, this);

        // Keep pulling out the remaining functors, creating them as new instances.
        while (!instructions.isEmpty())
        {
            decompileInstructions(code, instructions, registers, variableContext, null);
        }

        // Now recursively fill in the arguments, by looking up the registers.
        completeArguments(registers);

        decompiled = true;
    }

    /**
     * Fills in the arguments of this functor from the register values, recursively chaining the process down into any
     * arguments that are themselves functors. Although recursion is used, it is not likely to exhaust the stack as
     * functor nesting of that depth is unusual.
     *
     * @param registers The register values to fill in arguments from.
     */
    protected void completeArguments(Map<Byte, Term> registers)
    {
        // Parse each of the arguments in turn.
        for (int i = arity - 1; i >= 0; i--)
        {
            // Check if the argument is pending completion from a register, and fetch it from the decompiled
            // registers if so.
            if (arguments[i] instanceof RegisterArg)
            {
                arguments[i] = registers.get(((RegisterArg) arguments[i]).register);
            }

            // Check if the argument is a functor to be filled in recursively, and step down into if so.
            if (arguments[i] instanceof L0CompiledFunctor)
            {
                ((L0CompiledFunctor) arguments[i]).completeArguments(registers);
            }
        }
    }

    /**
     * Works through the instruciton queue processing until a put_struc or get_struc is encountered, signifying the
     * start of a functor, or a _var instruction is encountered that signifies the creation point of a variable. For
     * functors, the parsing reverses direction and the functors register arguments are gathered, for later completion
     * by subsequent parsing of earlier functors in the code buffer.
     *
     * @param  code            The code buffer.
     * @param  instructions    A queue of offsets of functor or variable creating instructions.
     * @param  registers       The register collection to store register values in.
     * @param  variableContext A mapping from variable id's to variables, to scope variables to the program sentence.
     * @param  inPlaceQuery    The compiled L0 query to decompile in to, this is used to decompile this. This argument
     *                         may be <tt>null</tt> in which case a new query functor is created an decompiled into when
     *                         a functor is encountered.
     *
     * @return A functor or variable, decompiled from the byte code instructions.
     */
    protected Term decompileInstructions(byte[] code, Queue<Integer> instructions, Map<Byte, Term> registers,
        Map<Integer, Variable> variableContext, L0CompiledFunctor inPlaceQuery)
    {
        Term result = null;

        // Loop backwards until functor or variable is encountered.
        while (!instructions.isEmpty())
        {
            int next = instructions.remove();

            byte instruction = code[next];
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

                // If its put_struc or get_struc then create a new functor.
                else if ((instruction == PUT_STRUC) || (instruction == GET_STRUC))
                {
                    // query = new L0CompiledQueryFunctor(machine, code);
                    functor = new Functor(f, arguments);
                }

                // Associate this functor with the register it is loaded into.
                registers.put(xi, functor);

                result = functor;

                break;
            }

            // If its set_var or unify_var create variable.
            else if ((instruction == SET_VAR) || (instruction == UNIFY_VAR))
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
     * <tr><td> PUT_STRUC <td> Marks the start of a functor in a query.
     * <tr><td> GET_STRUC <td> Marks the start of a functor in a program.
     * <tr><td> SET_VAR   <td> Marks the start of a variable in a query.
     * <tr><td> UNIFY_VAR <td> If it corresponds to a variable, marks the start of a variable in a program.
     * </table></pre>
     *
     * @param code    The code buffer to scan for instructions.
     * @param offsets The list to store the instruction start offsets in.
     */
    protected void instructionScan(byte[] code, Queue<Integer> offsets)
    {
        for (int i = 0; i < code.length; i++)
        {
            // Get the instruction and register argument that follows it.
            byte instruction = code[i];
            byte xi = code[++i];

            // Keep the position of the instruction start, but ignore set_val and unify_val instructions and unify_var
            // instructions that do not correspond with vairiables, as only interested in functor and variable creation
            // points.
            if ((instruction != SET_VAL) && (instruction != UNIFY_VAL) &&
                    ((instruction != UNIFY_VAR) || varNames.containsKey(xi)))
            {
                offsets.offer(i - 1);
            }

            // Skip the 4-byte functor argument for put_struc and get_struc instructions only.
            if ((instruction == PUT_STRUC) || (instruction == GET_STRUC))
            {
                i += 4;
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
