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

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import com.thesett.aima.logic.fol.AllTermsVisitor;
import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.DelegatingAllTermsVisitor;
import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorTermPredicate;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Term;
import com.thesett.aima.logic.fol.TermUtils;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverser;
import com.thesett.aima.logic.fol.compiler.PositionalTermTraverserImpl;
import com.thesett.aima.logic.fol.compiler.SymbolKeyTraverser;
import com.thesett.aima.logic.fol.compiler.TermWalker;
import static com.thesett.aima.logic.fol.l2.L2Instruction.L2InstructionSet;
import static com.thesett.aima.logic.fol.l2.L2Instruction.REG_ADDR;
import static com.thesett.aima.logic.fol.l2.L2Instruction.STACK_ADDR;
import com.thesett.aima.search.QueueBasedSearchMethod;
import com.thesett.aima.search.util.Searches;
import com.thesett.aima.search.util.backtracking.DepthFirstBacktrackingSearch;
import com.thesett.aima.search.util.uninformed.BreadthFirstSearch;
import com.thesett.aima.search.util.uninformed.PostFixSearch;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.SizeableLinkedList;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * L2Compiled implements a compiler for the logical language, L2, into a form suitable for passing to an
 * {@link L2Machine}. The L2Machine accepts sentences in the language that are compiled into a byte code form. The byte
 * instructions used in the compiled language are enumerated as constants in the {@link L2Instruction} class.
 *
 * <p/>The compilation process is described in "Warren's Abstact Machine, A Tutorial Reconstruction, by Hassan Ait-Kaci"
 * and is followed as closely as possible to the L2 compiler given there. The description of the L0 compilation process
 * is very clear in the text but the L2 compilation is a little ambiguous. It does not fully describe the flattening
 * process and presents some conflicting examples of register assignment. (The flattening process is essentially the
 * same as for L0, except that each argument of the outermost functor is flattened/compiled independently). The register
 * assignment process is harder to fathom, on page 22, the register assignment for p(Z, h(Z,W), f(W)) is presented with
 * the following assignment given:
 *
 * <pre>
 * A1 = Z
 * A2 = h(A1,X4)
 * A3 = f(X4)
 * X4 = W
 * </pre>
 *
 * In figure 2.9 a compilation example is given, from which it can be seen that the assignment should be:
 *
 * <pre>
 * A1 = Z (loaded from X4)
 * A2 = h(X4,X5)
 * A3 = f(X5)
 * X4 = Z
 * X5 = W
 * </pre>
 *
 * <p/>From figure 2.9 it was concluded that argument registers may only be assigned to functors. Functors can be
 * created on the heap and assigned to argument registers directly. Argument registers for variables, should be loaded
 * from a separate register assigned to the variable, that comes after the argument registers; so that a variable
 * assignment can be copied into multiple arguments, where the same variable is presented multiple times in a predicate
 * call. The register assignment process is carried out in two phases to do this, the first pass covers the argument
 * registers and the arguments of the outermost functor, only assigning to functors, the second pass continues for
 * higher numbered registers, starts again at the begining of the arguments, and assigns to variables and functors (not
 * already assigned) as for the L0 process.
 *
 * <p/>A brief overview of the compilation process is:
 *
 * <pre><p/><ul>
 * <li>Terms to be compiled are allocated registers, breadth first, enumerating from outermost functors down to
 *     inermost atoms or variables.</li>
 * <li>The outermost functor itself is treated specially, and is not allocated to a register. Its i arguments are
 *     allocated to registers, and are additionaly associated with the first i argument registers. The outermost functor
 *     is the instigator of a call, in the case of queries, or the recipient of a call, in the case of programs.
 * <li>Queries are 'flattened' by traversing each of their arguments in postfix order of their functors, then exploring
 *     the functors arguments.</li>
 * <li>Programs are 'flattened' by traversing each of their arguments breadth first, the same as for the original
 *     register allocation, then exploring the functors arguments.</li>
 * </ul></pre>
 *
 * <p/>Query terms are compiled into a sequence of instructions, that build up a representation of their argument terms,
 * to be unified, on the heap, and assigning registers to refer to those terms on the heap, then calling the matching
 * program for the query terms name and arity. Program terms are compiled into a sequence of instructions that, when run
 * against the argument registers, attempt to unify all of the arguments with the heap.
 *
 * <p/>The effect of flattening queries using a post fix ordering, is that the values of inner functors and variables
 * are loaded into registers first, before their containing functor is executed, which writes the functor and its
 * arguments onto the heap. Programs do not need to be expanded in this way, they simply match functors followed by
 * their arguments against the heap, so a breadth first traversal is all that is needed.
 *
 * <p/>Evaluating a flattened query consists of doing the following as different query tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then make a CALL (functor) to the matching program.
 * <li>For a register associated with an inner functor, push an STR onto the heap and copy that cell into the register.
 *     A put_struc (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, push a REF onto the heap that refers to iself,
 *     and copy that value into that variables register, as well as argument register i. A put_var (register, register)
 *     instruction is emmitted for this.
 * <li>For a register argument of an inner functor, not previously seen, push a REF onto the heap that refers to itself,
 *     and copy that cell into the register. A set_var (register) instruction is emmitted for this.</li>
 * <li>For a variables in argument position i in the outermost functor, previosly seen, copy its assigned register
 *     into its argument register. A put_val (register, register) instruction is emmitted for this.</li>
 * <li>For a register argument previously seen, push a new cell onto the heap and copy into it the register's value.
 *     A set_val (register) instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <p/>Evaluating a flattened program consists of doing the following as different program tokens are encountered:
 *
 * <pre><p/><ol>
 * <li>For the outermost functor, process all arguments, then execute a PROCEED instruction to indicate success.
 * <li>For a register associated with an inner functor, load that register with a reference to the functor. A get_struc
 *     (functor, register) instruction is created for this.</li>
 * <li>For a variable in argument position i in the outermost functor, copy its argument register into its assigned
 *     rgister. A get_var (register, register) instruction is emmitted for this.
 * <li>For a register argument of an innter functor, not previously seen, bind that register to its argument. A
 *     unify_var (register) instruction is output for this.</li>
 * <li>For a variable in argument position i in the outermost functor, unify its assigned register with the
 *     argument register. A get_val (register, register) instruction is emmitted for this.</li>
 * <li>For a register argument of an inner functor, previously seen, unify that register against the heap. A
 *     unify_val (register) instruction is emmitted for this.</li>
 * </ol></pre>
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Transform L2 sentences into compiled byte code.
 *     <td> {@link L2Machine}, {@link L2Sentence}, {@link L2CompiledClause}
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Document more things not fully described in the text: Flattening process for queries/programs. Queries: loop
 *         over all args, postfix flatten each in turn. Programs: BFS over the whole, but only output proceed for the
 *         outermost functor, and only output get_var and _val instructions for variables in the outermost functor.
 * @todo   Document this stuff that I chaged: Already seen works over registers, not variables and functors (as
 *         registers) separately. Yes its globaal over the whole sentence. Argument registers are only allocated
 *         directly to functors appearing in the head of the clause, everything else is allocated out-in.
 */
public class L2Compiler extends BaseMachine implements LogicCompiler<Clause, L2CompiledClause, L2CompiledClause>
{
    /** Used for debugging. */
    /* private static final Logger log = Logger.getLogger(L2Compiler.class.getName()); */

    /** The different sentence types in the L2 language. */
    protected enum SentenceType
    {
        /** A fact only has a head, and no body. */
        Fact,

        /** A rule has a head and a body. */
        Rule,

        /** A query has a body only and no head. */
        Query
    }

    /** Holds the compiler output observer. */
    private LogicCompilerObserver<L2CompiledClause, L2CompiledClause> observer;

    /** This is used to keep track of registers as they are seen. */
    private Set<Integer> seenRegisters = new TreeSet<Integer>();

    /**
     * Used to keep track of the last used register assignment accross assignments to multiple functors within a clause.
     */
    protected int lastAllocatedRegister;

    /** This is used to keep track of the number of permanent variables. */
    protected int numPermanentVars;

    /**
     * Creates a new L2Compiler.
     *
     * @param symbolTable The symbol table.
     * @param interner    The machine to translate functor and variable names.
     */
    public L2Compiler(SymbolTable<Integer, String, Object> symbolTable, VariableAndFunctorInterner interner)
    {
        super(symbolTable, interner);
    }

    /** {@inheritDoc} */
    public void compile(Sentence<Clause> sentence) throws SourceCodeException
    {
        /*log.fine("public L2CompiledClause compile(Sentence<Term> sentence = " + sentence + "): called");*/

        L2CompiledClause result;

        // A mapping from top stack frame slots to interned variable names is built up in this.
        // This is used to track the stack positions that variables in a query are assigned to.
        Map<Byte, Integer> varNames = new TreeMap<Byte, Integer>();

        // Used to keep track of registers as they are seen during compilation. The first time a variable is seen,
        // a variable is written onto the heap, subsequent times its value. The first time a functor is seen,
        // its structure is written onto the heap, subsequent times it is compared with.
        seenRegisters = new TreeSet<Integer>();

        // This is used to keep track of the next register available to allocate.
        lastAllocatedRegister = 0;

        // This is used to keep track of the number of permanent variables.
        numPermanentVars = 0;

        // These are used to generate pre and post instructions for the clause, for example, for the creation and
        // clean-up of stack frames.
        SizeableLinkedList<L2Instruction> preFixInstructions = new SizeableLinkedList<L2Instruction>();
        SizeableLinkedList<L2Instruction> postFixInstructions = new SizeableLinkedList<L2Instruction>();

        // Extract the clause to compile from the parsed sentence.
        Clause clause = sentence.getT();
        initialiseSymbolTable(clause);

        // Classify the sentence to compile by the different sentence types in the language.
        SentenceType type;

        if (clause.isQuery())
        {
            type = SentenceType.Query;
        }
        else if (clause.getBody() == null)
        {
            type = SentenceType.Fact;
        }
        else
        {
            type = SentenceType.Rule;
        }

        // Find all the free non-anonymous variables in the clause.
        Set<Variable> freeVars = TermUtils.findFreeNonAnonymousVariables(clause);
        Set<Integer> freeVarNames = new TreeSet<Integer>();

        for (Variable var : freeVars)
        {
            freeVarNames.add(var.getName());
        }

        // Allocate permanent variables depending on whether the clause is a query or a program clause.
        if (type == SentenceType.Query)
        {
            allocatePermanentQueryRegisters(clause, varNames);
            result = new L2CompiledClause(varNames, freeVarNames);
        }
        else
        {
            allocatePermanentProgramRegisters(clause);
            result = new L2CompiledClause();
        }

        // Generate the prefix code for the clause. Rules and queries may chain, so require stack frames.
        // Facts are always leafs so can use the global continuation point register to return from calls.
        if ((type == SentenceType.Rule) || (type == SentenceType.Query))
        {
            // Allocate a stack frame at the start of the clause.
            /*log.fine("ALLOCATE " + numPermanentVars);*/
            preFixInstructions.add(new L2Instruction(L2InstructionSet.Allocate, REG_ADDR,
                    (byte) (numPermanentVars & 0xff)));
        }

        result.addInstructions(preFixInstructions);

        // Check if the clause is a program (a fact or rule), in which case it has a head that must be compiled.
        if ((type == SentenceType.Fact) || (type == SentenceType.Rule))
        {
            Functor expression = clause.getHead();

            SizeableLinkedList<L2Instruction> instructions = compileHead(expression /*, varNames*/);

            result.setHead(expression, instructions);
        }

        // Compile all of the conjunctive parts of the body of the clause, if there are any.
        if ((type == SentenceType.Rule) || (type == SentenceType.Query))
        {
            Functor[] expressions = clause.getBody();

            for (int i = 0; i < expressions.length; i++)
            {
                Functor expression = expressions[i];

                // The 'isFirstBody' parameter is only set to true, when this is the first functor of a rule.
                SizeableLinkedList<L2Instruction> instructions =
                    compileBody(expression, /*varNames,*/
                        (SentenceType.Rule == type) && (i == 0));

                result.addBody(expression, instructions);
            }
        }

        // Generate the postfix code for the clause. Rules and queries may chain, so require stack frames.
        // Facts are always leafs so can use the global continuation point register to return from calls.
        if (type == SentenceType.Fact)
        {
            /*log.fine("PROCEED");*/
            postFixInstructions.add(new L2Instruction(L2InstructionSet.Proceed));
        }
        else
        {
            // Deallocate the stack frame at the end of the clause.
            /*log.fine("DEALLOCATE");*/
            postFixInstructions.add(new L2Instruction(L2InstructionSet.Deallocate));
        }

        result.addInstructions(postFixInstructions);

        //displayClause(result);

        if (type == SentenceType.Query)
        {
            observer.onQueryCompilation(result);
        }
        else
        {
            observer.onCompilation(result);
        }
    }

    /** {@inheritDoc} */
    public void setCompilerObserver(LogicCompilerObserver<L2CompiledClause, L2CompiledClause> observer)
    {
        this.observer = observer;
    }

    /** {@inheritDoc} */
    public void endScope()
    {
    }

    /**
     * Runs a symbol key traverser over the clause to be compiled, to ensure that all of its terms and sub-terms have
     * their symbol keys initialised.
     *
     * @param clause The clause to initialise the symbol keys of.
     */
    private void initialiseSymbolTable(Clause clause)
    {
        // Run the symbol key traverser over the clause, to ensure that all terms have their symbol keys correctly
        // set up.
        SymbolKeyTraverser symbolKeyTraverser = new SymbolKeyTraverser(interner, symbolTable, null);
        symbolKeyTraverser.setContextChangeVisitor(symbolKeyTraverser);

        TermWalker symWalker =
            new TermWalker(new DepthFirstBacktrackingSearch<Term, Term>(), symbolKeyTraverser, symbolKeyTraverser);
        symWalker.walk(clause);
    }

    /**
     * Compiles the head of a clause into an instruction listing in L2.
     *
     * @param  expression The clause head to compile.
     *
     * @return A listing of the instructions for the clause head in the L2 instruction set.
     */
    private SizeableLinkedList<L2Instruction> compileHead(Functor expression /*, Map<Byte, Integer> varNames*/)
    {
        // Used to build up the results in.
        SizeableLinkedList<L2Instruction> instructions = new SizeableLinkedList<L2Instruction>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        lastAllocatedRegister = 0;
        allocateArgumentRegisters(expression);
        allocateTemporaryRegisters(expression /*, varNames*/);

        // Program instructions are generated in the same order as the registers are assigned, the postfix
        // ordering used for queries is not needed.
        QueueBasedSearchMethod<Term, Term> outInSearch = new BreadthFirstSearch<Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator<Term> treeWalker = Searches.allSolutions(outInSearch);

        // Skip the outermost functor.
        treeWalker.next();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.

        // Keep track of processing of the arguments to the outermost functor as get_val and get_var instructions
        // need to be output for variables encountered in the arguments only.
        int numOutermostArgs = expression.getArity();

        for (int j = 0; treeWalker.hasNext(); j++)
        {
            Term nextTerm = treeWalker.next();

            /*log.fine("nextTerm = " + nextTerm);*/

            // For each functor encountered: get_struc.
            if (nextTerm.isFunctor())
            {
                Functor nextFunctor = (Functor) nextTerm;
                int allocation = (Integer) symbolTable.get(nextFunctor.getSymbolKey(), "allocation");

                byte addrMode = (byte) ((allocation & 0xff00) >> 8);
                byte address = (byte) (allocation & 0xff);

                // Ouput a get_struc instruction, except on the outermost functor.
                /*log.fine("GET_STRUC " + interner.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                    ((addrMode == REG_ADDR) ? ", X" : ", Y") + address);*/

                L2Instruction instruction =
                    new L2Instruction(L2InstructionSet.GetStruc, addrMode, address,
                        interner.getFunctorFunctorName(nextFunctor));
                instructions.add(instruction);

                // For each argument of the functor.
                int numArgs = nextFunctor.getArity();

                for (int i = 0; i < numArgs; i++)
                {
                    Term nextArg = nextFunctor.getArgument(i);
                    allocation = (Integer) symbolTable.get(nextArg.getSymbolKey(), "allocation");
                    addrMode = (byte) ((allocation & 0xff00) >> 8);
                    address = (byte) (allocation & 0xff);

                    /*log.fine("nextArg = " + nextArg);*/

                    // If it is register not seen before: unify_var.
                    if (!seenRegisters.contains(allocation))
                    {
                        /*log.fine("UNIFY_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/

                        seenRegisters.add(allocation);

                        instruction = new L2Instruction(L2InstructionSet.UnifyVar, addrMode, address);
                    }

                    // If it is register seen before: unify_val.
                    else
                    {
                        /*log.fine("UNIFY_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/

                        instruction = new L2Instruction(L2InstructionSet.UnifyVal, addrMode, address);
                    }

                    instructions.add(instruction);
                }
            }
            else if (j < numOutermostArgs)
            {
                Variable nextFunctor = (Variable) nextTerm;
                int allocation = (Integer) symbolTable.get(nextFunctor.getSymbolKey(), "allocation");
                byte addrMode = (byte) ((allocation & 0xff00) >> 8);
                byte address = (byte) (allocation & 0xff);

                L2Instruction instruction;

                // If it is register not seen before: get_var.
                if (!seenRegisters.contains(allocation))
                {
                    /*log.fine("GET_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    seenRegisters.add(allocation);

                    instruction = new L2Instruction(L2InstructionSet.GetVar, addrMode, address, (byte) (j & 0xff));
                }

                // If it is register seen before: get_val.
                else
                {
                    /*log.fine("GET_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                    instruction = new L2Instruction(L2InstructionSet.GetVal, addrMode, address, (byte) (j & 0xff));
                }

                instructions.add(instruction);
            }
        }

        return instructions;
    }

    /**
     * Compiles the body of a clause into an instruction listing in L2.
     *
     * @param  expression  The clause body to compile.
     * @param  isFirstBody <tt>true</tt> iff this is the first body of a program clause.
     *
     * @return A listing of the instructions for the clause body in the L2 instruction set.
     */
    private SizeableLinkedList<L2Instruction> compileBody(Functor expression, /*Map<Byte, Integer> varNames,*/
        boolean isFirstBody)
    {
        // Used to build up the results in.
        SizeableLinkedList<L2Instruction> instructions = new SizeableLinkedList<L2Instruction>();

        // Allocate argument registers on the body, to all functors as outermost arguments.
        // Allocate temporary registers on the body, to all terms not already allocated.
        if (!isFirstBody)
        {
            lastAllocatedRegister = 0;
        }

        allocateArgumentRegisters(expression);
        allocateTemporaryRegisters(expression /*, varNames*/);

        // Loop over all of the arguments to the outermost functor.
        int numOutermostArgs = expression.getArity();

        for (int j = 0; j < numOutermostArgs; j++)
        {
            Term nextOutermostArg = expression.getArgument(j);
            int allocation = (Integer) symbolTable.get(nextOutermostArg.getSymbolKey(), "allocation");

            byte addrMode = (byte) ((allocation & 0xff00) >> 8);
            byte address = (byte) (allocation & 0xff);

            // On the first occurrence of a variable output a put_var.
            if (nextOutermostArg.isVar() && !seenRegisters.contains(allocation))
            {
                seenRegisters.add(allocation);

                // The variable has been moved into an argument register.
                //varNames.remove((byte) allocation);
                //varNames.put((byte) j, ((Variable) nextOutermostArg).getName());

                /*log.fine("PUT_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                L2Instruction instruction =
                    new L2Instruction(L2InstructionSet.PutVar, addrMode, address, (byte) (j & 0xff));
                instructions.add(instruction);
            }

            // On a subsequent variable occurrence output a put_val.
            else if (nextOutermostArg.isVar())
            {
                /*log.fine("PUT_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address + ", A" + j);*/

                L2Instruction instruction =
                    new L2Instruction(L2InstructionSet.PutVal, addrMode, address, (byte) (j & 0xff));
                instructions.add(instruction);
            }

            // When a functor is encountered, output a put_struc.
            else if (nextOutermostArg.isFunctor())
            {
                Functor nextFunctorArg = (Functor) nextOutermostArg;

                // Heap cells are to be created in an order such that no heap cell can appear before other cells that it
                // refers to. A postfix traversal of the functors in the term to compile is used to achieve this, as
                // child functors in a head will be visited first.
                // Walk over the query term in post-fix order, picking out just the functors.
                QueueBasedSearchMethod<Term, Term> postfixSearch = new PostFixSearch<Term, Term>();
                postfixSearch.reset();
                postfixSearch.addStartState(nextFunctorArg);
                postfixSearch.setGoalPredicate(new FunctorTermPredicate());

                Iterator<Term> treeWalker = Searches.allSolutions(postfixSearch);

                // For each functor encountered: put_struc.
                while (treeWalker.hasNext())
                {
                    Functor nextFunctor = (Functor) treeWalker.next();
                    allocation = (Integer) symbolTable.get(nextFunctor.getSymbolKey(), "allocation");
                    addrMode = (byte) ((allocation & 0xff00) >> 8);
                    address = (byte) (allocation & 0xff);

                    // Ouput a put_struc instuction, except on the outermost functor.
                    /*log.fine("PUT_STRUC " + interner.getFunctorName(nextFunctor) + "/" + nextFunctor.getArity() +
                        ((addrMode == REG_ADDR) ? ", X" : ", Y") + address);*/

                    L2Instruction instruction =
                        new L2Instruction(L2InstructionSet.PutStruc, addrMode, address,
                            interner.getDeinternedFunctorName(nextFunctor.getName()));
                    instructions.add(instruction);

                    // For each argument of the functor.
                    int numArgs = nextFunctor.getArity();

                    for (int i = 0; i < numArgs; i++)
                    {
                        Term nextArg = nextFunctor.getArgument(i);
                        allocation = (Integer) symbolTable.get(nextArg.getSymbolKey(), "allocation");
                        addrMode = (byte) ((allocation & 0xff00) >> 8);
                        address = (byte) (allocation & 0xff);

                        // If it is new variable: set_var or put_var.
                        if (nextArg.isVar() && !seenRegisters.contains(allocation))
                        {
                            seenRegisters.add(allocation);

                            /*log.fine("SET_VAR " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/
                            instruction = new L2Instruction(L2InstructionSet.SetVar, addrMode, address);
                        }

                        // If it is variable or functor already seen: set_val or put_val.
                        else
                        {
                            /*log.fine("SET_VAL " + ((addrMode == REG_ADDR) ? "X" : "Y") + address);*/
                            instruction = new L2Instruction(L2InstructionSet.SetVal, addrMode, address);
                        }

                        instructions.add(instruction);
                    }
                }
            }
        }

        // Generate the call instructions, followed by the call address, which is f_n of the called program.
        L2Instruction instruction =
            new L2Instruction(L2InstructionSet.Call, interner.getFunctorFunctorName(expression));
        instructions.add(instruction);

        return instructions;
    }

    /**
     * For a predicate of arity n, the first n registers are used to recieve its arguments in. Terms appearing directly
     * in the head of the predicate clause are allocated directly to argument registers, so that when the argument is
     * read it can be compared directly with the term for a match. Variables appearing in the head of the clause are not
     * allocated in this way, but are kept in registers with positions higher than the number of arguments (see the
     * {@link #allocateTemporaryRegisters(Functor)} method for the allocation of registers).
     *
     * @param expression The clause head functor to allocate argument registers to.
     */
    private void allocateArgumentRegisters(Functor expression)
    {
        // Assign argument registers to functors appearing directly in the argument of the outermost functor.
        // Variables are never assigned directly to argument registers.
        for (; lastAllocatedRegister < expression.getArity(); lastAllocatedRegister++)
        {
            Term term = expression.getArgument(lastAllocatedRegister);

            if (term instanceof Functor)
            {
                /*log.fine("X" + lastAllocatedRegister + " = " + interner.getFunctorFunctorName((Functor) term));*/

                int allocation = (lastAllocatedRegister & 0xff) | (REG_ADDR << 8);
                symbolTable.put(term.getSymbolKey(), "allocation", allocation);
            }
        }
    }

    /**
     * Allocates variables within a functor expression to registers. The outermost functor itself is not assigned to a
     * register in l2 (only in l0). Functors already directly assigned to argument registers will not be re-assigned by
     * this. Variables as arguments will be assigned but not as argument registers.
     *
     * @param expression The expression to walk over.
     */
    private void allocateTemporaryRegisters(Functor expression /*, Map<Byte, Integer> varNames*/)
    {
        // Need to assign registers to the whole syntax tree, working in from the outermost functor. The outermost
        // functor itself is not assigned to a register in l2 (only in l0). Functors already directly assigned to
        // argument registers will not be re-assigned by this, variables as arguments will be assigned.
        QueueBasedSearchMethod<Term, Term> outInSearch = new BreadthFirstSearch<Term, Term>();
        outInSearch.reset();
        outInSearch.addStartState(expression);

        Iterator<Term> treeWalker = Searches.allSolutions(outInSearch);

        // Discard the outermost functor from the variable allocation.
        treeWalker.next();

        // For each term encountered: set X++ = term.
        while (treeWalker.hasNext())
        {
            Term term = treeWalker.next();

            if (symbolTable.get(term.getSymbolKey(), "allocation") == null)
            {
                if (term instanceof Functor)
                {
                    /*log.fine("X" + lastAllocatedRegister + " = " +
                        interner.getDeinternedFunctorName(((Functor) term).getName()));*/
                }
                else if (term instanceof Variable)
                {
                    /*log.fine("X" + lastAllocatedRegister + " = " +
                        interner.getVariableName(((Variable) term).getName()));*/

                    //varNames.put((byte) lastAllocatedRegister, ((Variable) term).getName());
                }

                int allocation = (lastAllocatedRegister++ & 0xff) | (REG_ADDR << 8);
                symbolTable.put(term.getSymbolKey(), "allocation", allocation);
            }
        }
    }

    /**
     * Allocates stack slots where need to the variables in a program clause.
     *
     * <p/>A clause head and first body functor are taken together as the first unit, subsequent clause body functors
     * are taken as subsequent units. A variable appearing in more than one unit is said to be permanent, and must be
     * stored on the stack, rather than a register, otherwise the register that it occupies may be overwritten by calls
     * to subsequent units. These variable are called permanent, which really means that they are local variables on the
     * call stack.
     *
     * @param clause The clause to allocate registers for.
     */
    private void allocatePermanentProgramRegisters(Clause clause)
    {
        // Create a bag to hold variable occurence counts in.
        Map<Variable, Integer> variableCountBag = new TreeMap<Variable, Integer>();

        // Get the set of variables in the head and first clause body argument.
        Set<Variable> firstGroupVariables = new TreeSet<Variable>();

        if (clause.getHead() != null)
        {
            Set<Variable> headVariables = TermUtils.findFreeVariables(clause.getHead());
            firstGroupVariables.addAll(headVariables);
        }

        if ((clause.getBody() != null) && (clause.getBody().length > 0))
        {
            Set<Variable> firstArgVariables = TermUtils.findFreeVariables(clause.getBody()[0]);
            firstGroupVariables.addAll(firstArgVariables);
        }

        // Add their counts to the bag.
        for (Variable variable : firstGroupVariables)
        {
            variableCountBag.put(variable, 1);
        }

        // Get the set of variables in all subsequent clauses.
        if ((clause.getBody() != null))
        {
            for (int i = 1; i < clause.getBody().length; i++)
            {
                Set<Variable> groupVariables = TermUtils.findFreeVariables(clause.getBody()[i]);

                // Add all their counts to the bag.
                for (Variable variable : groupVariables)
                {
                    Integer count = variableCountBag.get(variable);

                    if (count == null)
                    {
                        count = 0;
                    }

                    variableCountBag.put(variable, count + 1);
                }
            }
        }

        // Search the bag for all variable occurrences greater than one, and assign them to stack slots.
        for (Map.Entry<Variable, Integer> entry : variableCountBag.entrySet())
        {
            Variable variable = entry.getKey();
            int count = entry.getValue();

            if (count > 1)
            {
                /*log.fine("Variable " + variable + " is permanent, count = " + count);*/

                int allocation = (numPermanentVars++ & (0xff)) | (STACK_ADDR << 8);
                symbolTable.put(variable.getSymbolKey(), "allocation", allocation);
            }
        }
    }

    /**
     * Allocates stack slots to all free variables in a query clause.
     *
     * <p/>At the end of processing a query its variable bindings are usually printed. For this reason all free
     * variables in a query are marked as permanent variables on the call stack, to ensure that they are preserved.
     *
     * @param clause   The clause to allocate registers for.
     * @param varNames A map of permanent variables to variable names to record the allocations in.
     */
    private void allocatePermanentQueryRegisters(Clause clause, Map<Byte, Integer> varNames)
    {
        // Allocate local variable slots for all variables in a query.
        QueryRegisterAllocatingVisitor allocatingVisitor =
            new QueryRegisterAllocatingVisitor(symbolTable, varNames, null);

        PositionalTermTraverser positionalTraverser = new PositionalTermTraverserImpl();
        positionalTraverser.setContextChangeVisitor(allocatingVisitor);

        TermWalker walker =
            new TermWalker(new DepthFirstBacktrackingSearch<Term, Term>(), positionalTraverser, allocatingVisitor);

        walker.walk(clause);
    }

    /**
     * Pretty prints a compiled clause.
     *
     * @param clause The clause to pretty print.
     */
    private void displayClause(Clause clause)
    {
        // Pretty print the clause.
        StringBuffer prettyClause = new StringBuffer();

        L2CompilerClausePrintingVisitor displayVisitor =
            new L2CompilerClausePrintingVisitor(interner, symbolTable, prettyClause);

        PositionalTermTraverser positionalTraverser = new PositionalTermTraverserImpl();
        displayVisitor.setPositionalTraverser(positionalTraverser);
        positionalTraverser.setContextChangeVisitor(displayVisitor);

        TermWalker walker =
            new TermWalker(new DepthFirstBacktrackingSearch<Term, Term>(), positionalTraverser, displayVisitor);

        walker.walk(clause);

        System.out.println(prettyClause);
    }

    /**
     * QueryRegisterAllocatingVisitor visits variables in a query, and if they are not already allocated to a permanent
     * stack slot, allocates them one. All variables in queries are stack allocated, so that they are preserved on the
     * stack at the end of the query.
     */
    public class QueryRegisterAllocatingVisitor extends DelegatingAllTermsVisitor
    {
        /** The symbol table. */
        private final SymbolTable<Integer, String, Object> symbolTable;

        /** Holds a map of permanent variables to variable names to record the allocations in. */
        private final Map<Byte, Integer> varNames;

        /**
         * Creates a query variable allocator.
         *
         * @param symbolTable The symbol table.
         * @param varNames    A map of permanent variables to variable names to record the allocations in.
         * @param delegate    The term visitor that this delegates to.
         */
        public QueryRegisterAllocatingVisitor(SymbolTable<Integer, String, Object> symbolTable,
            Map<Byte, Integer> varNames, AllTermsVisitor delegate)
        {
            super(delegate);
            this.symbolTable = symbolTable;
            this.varNames = varNames;
        }

        /**
         * {@inheritDoc}
         *
         * <p/>Allocates unallocated variables to stack slots.
         */
        public void visit(Variable variable)
        {
            if (symbolTable.get(variable.getSymbolKey(), "allocation") == null)
            {
                /*log.fine("Variable " + variable + " is permanent.");*/

                int allocation = (numPermanentVars++ & (0xff)) | (STACK_ADDR << 8);
                symbolTable.put(variable.getSymbolKey(), "allocation", allocation);
                varNames.put((byte) allocation, variable.getName());
            }

            super.visit(variable);
        }
    }
}
