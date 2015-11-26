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
package com.thesett.aima.logic.fol.l2;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.common.util.ByteBufferUtils;
import com.thesett.common.util.Sizeable;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.SizeableList;

/**
 * L2Instruction provides a structured in-memeory representation of the L2 instruction set, as well as utilities to
 * emmit the instructions as byte code, disassemble them back into the structured representation, and pretty print them.
 *
 * <p/>Instructions implement {@link Sizeable} reporting their length in bytes as their 'size of'.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide the translation of the L2 instruction set down to bytes.
 * <tr><td> Provide dissasembly of byte encoded instructions back into structured instructions.
 * <tr><td> Calculate the length of an instruction in bytes. <td> {@link com.thesett.common.util.Sizeable}
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class L2Instruction implements Sizeable
{
    /** Instruction to write out a struc onto the heap. */
    public static final byte PUT_STRUC = 0x01;

    /** The instruction to set a register as a variable. */
    public static final byte SET_VAR = 0x02;

    /** The instruction to set a register to a heap location. */
    public static final byte SET_VAL = 0x03;

    /** The instruction to compare a register to a structure on the heap. */
    public static final byte GET_STRUC = 0x04;

    /** The instruction to unify a register with a variable. */
    public static final byte UNIFY_VAR = 0x05;

    /** The instruction to unify a register with a location on the heap. */
    public static final byte UNIFY_VAL = 0x06;

    /** The instruction to copy a heap location into an argument register. */
    public static final byte PUT_VAR = 0x07;

    /** The instruction to copy a register into an argument register. */
    public static final byte PUT_VAL = 0x08;

    /** The instruction to unify an argument register with a variable. */
    public static final byte GET_VAR = 0x09;

    /** The instruction to unify a register with a location on the heap. */
    public static final byte GET_VAL = 0x0a;

    /** The instruction to call a predicate. */
    public static final byte CALL = 0x0b;

    /** The instruction to return from a called predicate. */
    public static final byte PROCEED = 0x0c;

    /** The stack frame allocation instruction. */
    public static final byte ALLOCATE = 0x0d;

    /** The stack frame deallocation instruction. */
    public static final byte DEALLOCATE = 0x0e;

    // === Defines the addressing modes.

    /** Used to specify addresses relative to registers. */
    public static final byte REG_ADDR = 0x01;

    /** Used to specify addresses relative to the current stack frame. */
    public static final byte STACK_ADDR = 0x02;

    // === Defines the heap cell marker types.

    /** Indicates a register on the heap. */
    public static final byte REF = 0x01;

    /** Indicates the begining of a struc on the heap. */
    public static final byte STR = 0x02;

    /** Defines the L0 virtual machine instruction set as constants. */
    public enum L2InstructionSet
    {
        /** Instruction to write out a struc onto the heap. */
        PutStruc(PUT_STRUC, "put_struc", 7)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Fn(code, ip, instruction, machine);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Fn(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Fn(pretty, instruction);
            }
        },

        /** The instruction to set a register as a variable. */
        SetVar(SET_VAR, "set_var", 3),

        /** The instruction to set a register to a heap location. */
        SetVal(SET_VAL, "set_val", 3),

        /** The instruction to compare a register to a structure on the heap. */
        GetStruc(GET_STRUC, "get_struc", 7)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Fn(code, ip, instruction, machine);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Fn(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Fn(pretty, instruction);
            }
        },

        /** The instruction to unify a register with a variable. */
        UnifyVar(UNIFY_VAR, "unify_var", 3),

        /** The instruction to unify a register with a location on the heap. */
        UnifyVal(UNIFY_VAL, "unify_val", 3),

        /** The instruction to copy a heap location into an argument register. */
        PutVar(PUT_VAR, "put_var", 4)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Reg2(code, ip, instruction);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Reg2(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Reg2(pretty, instruction);
            }
        },

        /** The instruction to copy a register into an argument register. */
        PutVal(PUT_VAL, "put_val", 4)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Reg2(code, ip, instruction);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Reg2(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Reg2(pretty, instruction);
            }
        },

        /** The instruction to unify an argument register with a variable. */
        GetVar(GET_VAR, "get_var", 4)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Reg2(code, ip, instruction);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Reg2(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Reg2(pretty, instruction);
            }
        },

        /** The instruction to unify a register with a location on the heap. */
        GetVal(GET_VAL, "get_val", 4)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                disassembleReg1Reg2(code, ip, instruction);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                emmitCodeReg1Reg2(codeBuf, code, ip, instruction, machine);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return toStringReg1Reg2(pretty, instruction);
            }
        },

        /** The instruction to call a predicate. */
        Call(CALL, "call", 5)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                // Do nothing as this instruction takes no arguments.
                int fn = ByteBufferUtils.getIntFromBytes(code, ip + 5);
                int f = fn >> 8;
                instruction.fn = machine.getDeinternedFunctorName(f);
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param  instruction The instruction to emmit.
             * @param  ip          The location to write the byte code to.
             * @param  codeBuf     The code buffer to write to.
             * @param  machine     The binary machine to write the code to.
             *
             * @throws LinkageException If the functor to call does not exist on the machine.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
                throws LinkageException
            {
                int toCall = machine.internFunctorName(instruction.fn);

                L2CallPoint callPoint = machine.resolveCallPoint(toCall);

                // Ensure that a valid call point was returned, otherwise a linkage error has occurrd.
                if (callPoint == null)
                {
                    throw new LinkageException("Could not resolve call to " + instruction.fn + ".", null, null,
                        "Unable to resolve call to " + instruction.fn.getName() + "/" + instruction.fn.getArity() +
                        ".");
                }

                int entryPoint = callPoint.entryPoint;

                codeBuf[ip] = code;
                ByteBufferUtils.writeIntToByteArray(codeBuf, ip + 1, entryPoint);
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return pretty + " " +
                    ((instruction.fn != null) ? (instruction.fn.getName() + "/" + instruction.fn.getArity()) : "");
            }
        },

        /** The instruction to return from a called predicate. */
        Proceed(PROCEED, "proceed", 1)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                // Do nothing as this instruction takes no arguments.
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                codeBuf[ip] = code;
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return pretty;
            }
        },

        /** The stack frame allocation instruction. */
        Allocate(ALLOCATE, "allocate", 2)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                // Do nothing as this instruction takes no arguments.
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                codeBuf[ip] = code;
                codeBuf[ip + 1] = instruction.reg1;
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return pretty + " " + instruction.reg1;
            }
        },

        /** The stack frame deallocation instruction. */
        Deallocate(DEALLOCATE, "deallocate", 1)
        {
            /**
             * Disassembles the arguments of this instruction.
             *
             * @param instruction The instruction to disassemble.
             * @param ip          The start of the instruction within the buffer.
             * @param code        The code buffer.
             * @param machine     The binary machine to disassemble from.
             */
            protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
            {
                // Do nothing as this instruction takes no arguments.
            }

            /**
             * Writes out the instruction plus arguments in the byte code format to the specified location within a code
             * buffer.
             *
             * @param instruction The instruction to emmit.
             * @param ip          The location to write the byte code to.
             * @param codeBuf     The code buffer to write to.
             * @param machine     The binary machine to write code to.
             */
            public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            {
                codeBuf[ip] = code;
            }

            /**
             * Prints the human readable form of the instruction for debugging purposes.
             *
             * @param  instruction The instruction to pretty print.
             *
             * @return The human readable form of the instruction for debugging purposes.
             */
            public String toString(L2Instruction instruction)
            {
                return pretty;
            }
        };

        /** Holds a mapping of the instruction by byte code. */
        private static final Map<Byte, L2InstructionSet> codeToValue = new HashMap<Byte, L2InstructionSet>();

        static
        {
            for (L2InstructionSet instruction : EnumSet.allOf(L2InstructionSet.class))
            {
                codeToValue.put(instruction.code, instruction);
            }
        }

        /** Holds the byte representation of the instruction. */
        protected byte code;

        /** Holds the human readable form of the instruction. */
        protected String pretty;

        /** Holds the length of the instruction in bytes. */
        protected int length;

        /**
         * Creates a new L0 instruction with the specified byte code.
         *
         * @param i      The byte code for the instruction.
         * @param pretty The human readable form of the isntruction.
         * @param length The length of the instruction plus arguments in bytes.
         */
        L2InstructionSet(byte i, String pretty, int length)
        {
            this.code = i;
            this.pretty = pretty;
            this.length = length;
        }

        /**
         * Creates an instruction from a byte code.
         *
         * @param  code The byte coded form of the instruction.
         *
         * @return An instruction matching the byte code, or <tt>null <tt>if no isntruction matches the code.
         */
        public static L2InstructionSet fromCode(byte code)
        {
            return codeToValue.get(code);
        }

        /**
         * Writes out the instruction plus arguments in the byte code format to the specified location within a code
         * buffer.
         *
         * @param  instruction The instruction, including its arguments.
         * @param  ip          The location to write the byte code to.
         * @param  codeBuf     The code buffer to write to.
         * @param  machine     The binary machine to write the code into.
         *
         * @throws LinkageException If required symbols to link to cannot be found in the binary machine.
         */
        public void emmitCode(L2Instruction instruction, int ip, byte[] codeBuf, L2Machine machine)
            throws LinkageException
        {
            codeBuf[ip] = code;
            codeBuf[ip + 1] = instruction.mode1;
            codeBuf[ip + 2] = instruction.reg1;
        }

        /**
         * Prints the human readable form of the instruction for debugging purposes.
         *
         * @param  instruction The instruction, including its arguments.
         *
         * @return The human readable form of the instruction for debugging purposes.
         */
        public String toString(L2Instruction instruction)
        {
            return toStringReg1(pretty, instruction);
        }

        /**
         * Gets the byte coded representation of the instruction.
         *
         * @return The byte coded representation of the instruction.
         */
        public byte getCode()
        {
            return code;
        }

        /**
         * Calculates the length of this instruction in bytes.
         *
         * @return The length of this instruction in bytes.
         */
        public int length()
        {
            return length;
        }

        /**
         * Disassembles the arguments of an instruction. This is a default implementation to handle the most common
         * case; instructions with one register argument.
         *
         * @param instruction The instruction, including its arguments.
         * @param ip          The start of the instruction within the buffer.
         * @param code        The code buffer.
         * @param machine     The binary machine to disassemble from.
         */
        protected void disassembleArguments(L2Instruction instruction, int ip, byte[] code, L2Machine machine)
        {
            disassembleReg1(code, ip, instruction);
        }

        /**
         * Writes out the instruction plus arguments in the byte code format to the specified location within a code
         * buffer.
         *
         * @param codeBuf     The code buffer to write to.
         * @param code        The instruction mnemonic.
         * @param ip          The location to write the byte code to.
         * @param instruction The instruction, including its arguments.
         * @param machine     The binary machine to write the code into.
         */
        private static void emmitCodeReg1(byte[] codeBuf, byte code, int ip, L2Instruction instruction,
            L2Machine machine)
        {
            codeBuf[ip] = code;
            codeBuf[ip + 1] = instruction.mode1;
            codeBuf[ip + 2] = instruction.reg1;
        }

        /**
         * Writes out the instruction plus arguments in the byte code format to the specified location within a code
         * buffer.
         *
         * @param codeBuf     The code buffer to write to.
         * @param code        The instruction mnemonic.
         * @param ip          The location to write the byte code to.
         * @param instruction The instruction, including its arguments.
         * @param machine     The binary machine to write the code into.
         */
        private static void emmitCodeReg1Reg2(byte[] codeBuf, byte code, int ip, L2Instruction instruction,
            L2Machine machine)
        {
            codeBuf[ip] = code;
            codeBuf[ip + 1] = instruction.mode1;
            codeBuf[ip + 2] = instruction.reg1;
            codeBuf[ip + 3] = instruction.reg2;
        }

        /**
         * Writes out the instruction plus arguments in the byte code format to the specified location within a code
         * buffer.
         *
         * @param codeBuf     The code buffer to write to.
         * @param code        The instruction mnemonic.
         * @param ip          The location to write the byte code to.
         * @param instruction The instruction, including its arguments.
         * @param machine     The binary machine to write the code into.
         */
        private static void emmitCodeReg1Fn(byte[] codeBuf, byte code, int ip, L2Instruction instruction,
            L2Machine machine)
        {
            codeBuf[ip] = code;
            codeBuf[ip + 1] = instruction.mode1;
            codeBuf[ip + 2] = instruction.reg1;
            codeBuf[ip + 3] = (byte) instruction.fn.getArity();
            ByteBufferUtils.write24BitIntToByteArray(codeBuf, ip + 4, machine.internFunctorName(instruction.fn));
        }

        /**
         * Helper print function that prints an instruction with one register argument.
         *
         * @param  pretty      The pretty printed instruction mnenomic.
         * @param  instruction The instruction data.
         *
         * @return A pretty printed instruction.
         */
        private static String toStringReg1(String pretty, L2Instruction instruction)
        {
            return pretty + " X" + instruction.reg1;
        }

        /**
         * Helper print function that prints an instruction with two register arguments.
         *
         * @param  pretty      The pretty printed instruction mnenomic.
         * @param  instruction The instruction data.
         *
         * @return A pretty printed instruction.
         */
        private static String toStringReg1Reg2(String pretty, L2Instruction instruction)
        {
            return pretty + " X" + instruction.reg1 + ", A" + instruction.reg2;
        }

        /**
         * Helper print function that prints an instruction with one register argument and a functor reference.
         *
         * @param  pretty      The pretty printed instruction mnenomic.
         * @param  instruction The instruction data.
         *
         * @return A pretty printed instruction.
         */
        private static String toStringReg1Fn(String pretty, L2Instruction instruction)
        {
            return pretty + " X" + instruction.reg1 + ", " +
                ((instruction.fn != null) ? (instruction.fn.getName() + "/" + instruction.fn.getArity()) : "");
        }

        /**
         * Disassembles the arguments to an instruction that takes one register and one functor reference.
         *
         * @param code        The code buffer to disassemble from.
         * @param ip          The instruction pointer within the code buffer.
         * @param instruction The instruction to store the disassembles arguments in.
         * @param machine     The binary machine to disassemble from.
         */
        private static void disassembleReg1Fn(byte[] code, int ip, L2Instruction instruction, L2Machine machine)
        {
            //instruction.mode1 = code[ip + 1];
            instruction.reg1 = code[ip + 1];

            int fn = ByteBufferUtils.getIntFromBytes(code, ip + 2);
            int f = fn >> 8;
            instruction.fn = machine.getDeinternedFunctorName(f);
        }

        /**
         * Disassembles the arguments to an instruction that takes one register argument.
         *
         * @param code        The code buffer to disassemble from.
         * @param ip          The instruction pointer within the code buffer.
         * @param instruction The instruction to store the disassembles arguments in.
         */
        private static void disassembleReg1(byte[] code, int ip, L2Instruction instruction)
        {
            instruction.mode1 = code[ip + 1];
            instruction.reg1 = code[ip + 2];
        }

        /**
         * Disassembles the arguments to an instruction that takes one register argument.
         *
         * @param code        The code buffer to disassemble from.
         * @param ip          The instruction pointer within the code buffer.
         * @param instruction The instruction to store the disassembles arguments in.
         */
        private static void disassembleReg1Reg2(byte[] code, int ip, L2Instruction instruction)
        {
            instruction.mode1 = code[ip + 1];
            instruction.reg1 = code[ip + 2];
            instruction.reg2 = code[ip + 3];
        }
    }

    /** The instruction. */
    protected L2InstructionSet mnemonic;

    /** Holds the addressing mode of the first register argument to the instruction. */
    protected byte mode1;

    /** Holds the first register argument to the instruction. */
    protected byte reg1;

    /** Holds the second register argument to the instruction. */
    protected byte reg2;

    /** Holds the functor argument to the instruction. */
    protected FunctorName fn;

    /**
     * Creates an instruction for the specified mnemonic.
     *
     * @param mnemonic The instruction mnemonic.
     */
    public L2Instruction(L2InstructionSet mnemonic)
    {
        this.mnemonic = mnemonic;
    }

    /**
     * Creates an instruction with the mnemonic resolved from its byte encoded form.
     *
     * @param code The byte encoded form of the instruction mnemonic.
     */
    public L2Instruction(byte code)
    {
        this.mnemonic = L2InstructionSet.fromCode(code);
    }

    /**
     * Creates an instruction for the specified mnemonic that takes one register and one functor argument.
     *
     * @param mnemonic The instruction mnemonic.
     * @param mode1    The addressing mode to use with the register argument.
     * @param reg1     The register argument.
     * @param fn       The functor argument.
     */
    public L2Instruction(L2InstructionSet mnemonic, byte mode1, byte reg1, FunctorName fn)
    {
        this.mnemonic = mnemonic;
        this.mode1 = mode1;
        this.reg1 = reg1;
        this.fn = fn;
    }

    /**
     * Creates an instruction for the specified mnemonic that takes two register arguments.
     *
     * @param mnemonic The instruction mnemonic.
     * @param mode1    The addressing mode to use with the first register argument.
     * @param reg1     The first register argument.
     * @param reg2     The second register argument.
     */
    public L2Instruction(L2InstructionSet mnemonic, byte mode1, byte reg1, byte reg2)
    {
        this.mnemonic = mnemonic;
        this.mode1 = mode1;
        this.reg1 = reg1;
        this.reg2 = reg2;
    }

    /**
     * Creates an instruction for the specified mnemonic that takes a single register argument.
     *
     * @param mnemonic The instruction mnemonic.
     * @param mode1    The addressing mode to use with the register argument.
     * @param reg1     The single register argument.
     */
    public L2Instruction(L2InstructionSet mnemonic, byte mode1, byte reg1)
    {
        this.mnemonic = mnemonic;
        this.mode1 = mode1;
        this.reg1 = reg1;
    }

    /**
     * Creates an instruction for the specified mnemonic that takes a single functor argument.
     *
     * @param mnemonic The instruction mnemonic.
     * @param fn       The functor argument.
     */
    public L2Instruction(L2InstructionSet mnemonic, FunctorName fn)
    {
        this.mnemonic = mnemonic;
        this.fn = fn;
    }

    /**
     * Dissasembles the instructions from the specified byte buffer, starting at a given location (ip). An interner for
     * the functor names encountered in the instruction buffer must also be supplied, in order to look up the functor
     * names by encoded value.
     *
     * @param  ip      The start instruction pointer into the buffer.
     * @param  code    The code buffer.
     * @param  machine The binary machine to disassemble from.
     *
     * @return A list of instructions disassembles from the code buffer.
     */
    public static SizeableList<L2Instruction> dissasemble(int ip, byte[] code, L2Machine machine)
    {
        SizeableList<L2Instruction> result = new SizeableLinkedList<L2Instruction>();

        int end = code.length;

        while (ip < end)
        {
            byte iCode = code[ip];

            L2Instruction instruction = new L2Instruction(iCode);

            //instruction.mnemonic = L2InstructionSet.fromCode(iCode);
            instruction.mnemonic.disassembleArguments(instruction, ip, code, machine);

            result.add(instruction);

            ip += instruction.mnemonic.length();
        }

        return result;
    }

    /**
     * Gets the instruction mnemonic.
     *
     * @return The instruction mnemonic.
     */
    public L2InstructionSet getMnemonic()
    {
        return mnemonic;
    }

    /**
     * Gets the first register to which the instruction applies.
     *
     * @return The first register to which the instruction applies.
     */
    public byte getReg1()
    {
        return reg1;
    }

    /**
     * Sets the first register to which the instruction applies.
     *
     * @param reg1 The first register to which the instruction applies.
     */
    public void setReg1(byte reg1)
    {
        this.reg1 = reg1;
    }

    /**
     * Gets the second register to which the instruction applies.
     *
     * @return The second register to which the instruction applies.
     */
    public byte getReg2()
    {
        return reg2;
    }

    /**
     * Sets the second register to which the instruction applies.
     *
     * @param reg2 The second register to which the instruction applies.
     */
    public void setReg2(byte reg2)
    {
        this.reg2 = reg2;
    }

    /**
     * Gets the functor argument, if any, to which the instruction applies.
     *
     * @return The functor argument to which the instruction applies, or <tt>null <tt>if there is none.
     */
    public FunctorName getFn()
    {
        return fn;
    }

    /**
     * Sets the functor argument to which the instruction applies.
     *
     * @param fn The functor argument to which the instruction applies.
     */
    public void setFn(FunctorName fn)
    {
        this.fn = fn;
    }

    /**
     * Writes out the instruction plus arguments in the byte code format to the specified location within a code buffer.
     *
     * @param  ip         The location to write the byte code to.
     * @param  codeBuffer The code buffer to write to.
     * @param  machine    The binary machine to write the code into.
     *
     * @throws LinkageException If required symbols to link to cannot be found in the binary machine.
     */
    public void emmitCode(int ip, byte[] codeBuffer, L2Machine machine) throws LinkageException
    {
        mnemonic.emmitCode(this, ip, codeBuffer, machine);
    }

    /**
     * Calculates the length of this instruction in bytes.
     *
     * @return The length of this instruction in bytes.
     */
    public long sizeof()
    {
        return mnemonic.length();
    }

    /**
     * Prints the human readable form of the instruction for debugging purposes.
     *
     * @return The human readable form of the instruction for debugging purposes.
     */
    public String toString()
    {
        return mnemonic.toString(this);
    }
}
