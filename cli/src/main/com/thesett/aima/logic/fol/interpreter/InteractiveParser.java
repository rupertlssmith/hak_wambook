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
package com.thesett.aima.logic.fol.interpreter;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.SentenceImpl;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;
import com.thesett.aima.logic.fol.isoprologparser.BasePrologParser;
import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import com.thesett.common.parsing.SourceCodeException;

/**
 * InteractiveParser is a Prolog parser that works when the Prolog system is being as an interactive terminal. In
 * addition to parsing first order logic Horn clauses, it handles the system directives for user mode, debugging,
 * loading files and so on.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Parse a string as a logical term relative to a variable and functor interner.
 *     <td> {@link Clause}, {@link VariableAndFunctorInterner}.
 * <tr><td> Detect system directives.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class InteractiveParser extends BasePrologParser<Clause>
{
    /**
     * Creates a clause parser over an interner.
     *
     * @param interner The interner to use to intern all functor and variable names.
     */
    public InteractiveParser(VariableAndFunctorInterner interner)
    {
        super(interner);
    }

    /**
     * Parses the next sentence from the current token source.
     *
     * @return The fully parsed syntax tree for the next sentence, or null if no more sentences are available, for
     *         example, because an end of file has been reached.
     *
     * @throws SourceCodeException If the source being parsed does not match the grammar.
     */
    public Sentence<Clause> parse() throws SourceCodeException
    {
        return new SentenceImpl<Clause>(parser.sentence());
    }

    /**
     * Peeks and consumes the next interactive system directive.
     *
     * @return The directive, or <tt>null</tt> if none is found.
     *
     * @throws SourceCodeException If the source being parsed does not match the grammar.
     */
    public PrologParser.Directive peekAndConsumeDirective() throws SourceCodeException
    {
        return parser.peekAndConsumeDirective();
    }
}
