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
package com.thesett.aima.logic.fol.wam.compiler;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.LogicCompiler;
import com.thesett.aima.logic.fol.LogicCompilerObserver;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.bytecode.BaseMachine;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.util.doublemaps.SymbolTable;

/**
 * WAMCompiler implements the {@link LogicCompiler} interface for the complete WAM compilation chain. It is a
 * supervising compiler, that chains together the work of the compiler pipe-line stages.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Chain together the compiler pipe-line stages.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMCompiler extends BaseMachine implements LogicCompiler<Clause, WAMCompiledPredicate, WAMCompiledQuery>
{
    /** Holds the pre-compiler, for analyzing and transforming terms prior to compilation proper. */
    PreCompiler preCompiler;

    /** Holds the instruction generating compiler. */
    InstructionCompiler instructionCompiler;

    /**
     * Creates a new WAMCompiler.
     *
     * @param symbolTable The symbol table.
     * @param interner    The machine to translate functor and variable names.
     */
    public WAMCompiler(SymbolTable<Integer, String, Object> symbolTable, VariableAndFunctorInterner interner)
    {
        super(symbolTable, interner);

        instructionCompiler = new InstructionCompiler(symbolTable, interner);
        preCompiler = new PreCompiler(symbolTable, interner, instructionCompiler);

        preCompiler.setCompilerObserver(new ClauseChainObserver());
    }

    /** {@inheritDoc} */
    public void compile(Sentence<Clause> sentence) throws SourceCodeException
    {
        preCompiler.compile(sentence);
    }

    /** {@inheritDoc} */
    public void setCompilerObserver(LogicCompilerObserver<WAMCompiledPredicate, WAMCompiledQuery> observer)
    {
        instructionCompiler.setCompilerObserver(observer);
    }

    /** {@inheritDoc} */
    public void endScope() throws SourceCodeException
    {
        preCompiler.endScope();
        instructionCompiler.endScope();
    }

    /**
     * Chains compilation completion events onto the instruction compiler.
     */
    class ClauseChainObserver implements LogicCompilerObserver<Clause, Clause>
    {
        /** {@inheritDoc} */
        public void onCompilation(Sentence<Clause> sentence) throws SourceCodeException
        {
            instructionCompiler.compile(sentence);
        }

        /** {@inheritDoc} */
        public void onQueryCompilation(Sentence<Clause> sentence) throws SourceCodeException
        {
            instructionCompiler.compile(sentence);
        }
    }
}
