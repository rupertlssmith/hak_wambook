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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import jline.ConsoleReader;

import com.thesett.aima.logic.fol.Clause;
import com.thesett.aima.logic.fol.Parser;
import com.thesett.aima.logic.fol.Sentence;
import com.thesett.aima.logic.fol.Variable;
import com.thesett.aima.logic.fol.isoprologparser.PrologParser;
import com.thesett.aima.logic.fol.isoprologparser.PrologParserConstants;
import com.thesett.aima.logic.fol.isoprologparser.Token;
import com.thesett.aima.logic.fol.isoprologparser.TokenSource;
import com.thesett.common.parsing.SourceCodeException;
import com.thesett.common.parsing.SourceCodePosition;
import com.thesett.common.util.Sink;
import com.thesett.common.util.Source;

/**
 * ResolutionInterpreter implements an interactive Prolog like interpreter, built on top of a {@link ResolutionEngine}.
 * It implements a top-level interpreter loop where queries or domain clauses may be entered. Queries are resolved
 * against the current domain using the resolver, after they have been compiled.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Parse text into first order logic clauses. <td> {@link com.thesett.aima.logic.fol.Parser}.
 * <tr><td> Compile clauses down to their compiled form. <td> {@link Compiler}.
 * <tr><td> Add facts to the current knowledge base. <td> {@link com.thesett.aima.logic.fol.Resolver}.
 * <tr><td> Resolve queries against the current knowledge base. <td> {@link com.thesett.aima.logic.fol.Resolver}.
 * <tr><td> Print the variable bindings resulting from resolution.
 *     <td> {@link com.thesett.aima.logic.fol.VariableAndFunctorInterner}.
 * </table></pre>
 *
 * @param  <T> The compiled clause type that the compiler produces.
 * @param  <Q> The compiled query type that the compiler produces.
 *
 * @author Rupert Smith
 */
public class ResolutionInterpreter<T, Q>
{
    /** Used for debugging purposes. */
    /* private static final Logger log = Logger.getLogger(ResolutionInterpreter.class.getName()); */

    /** The prompt to use in query mode. */
    public static final String QUERY_PROMPT = "?- ";

    /** The line continuation prompt for query mode. */
    public static final String MULTI_LINE_QUERY_PROMPT = "   ";

    /** The prompt to use in program mode. */
    public static final String PROGRAM_PROMPT = "";

    /** The line continuation prompt for program mode. */
    public static final String MULTI_LINE_PROGRAM_PROMPT = "   ";

    /** ASCII for a semicolon. */
    public static final int SEMICOLON = 59;

    /** Describes the possible parsing modes. */
    public enum Mode
    {
        Program(PROGRAM_PROMPT), ProgramMultiLine(MULTI_LINE_PROGRAM_PROMPT), Query(QUERY_PROMPT),
        QueryMultiLine(MULTI_LINE_QUERY_PROMPT);

        private final String prompt;

        Mode(String prompt)
        {
            this.prompt = prompt;
        }
    }

    /** Holds the JLine console reader. */
    private ConsoleReader consoleReader;

    /** Holds the resolution engine that the interpreter loop runs on. */
    ResolutionEngine<Clause, T, Q> engine;

    /** Holds the interactive parser that the interpreter loop runs on. */
    private final InteractiveParser parser;

    /** Holds the name of the predicate currently being parsed, clause by clause. */
    private Integer currentPredicateName;

    /** Holds the current interaction mode. */
    private Mode mode = Mode.Query;

    /**
     * Builds an interactive logical resolution interpreter from a parser, interner, compiler and resolver, encapsulated
     * as a resolution engine.
     *
     * @param engine The resolution engine. This must be using an {@link InteractiveParser}.
     */
    public ResolutionInterpreter(ResolutionEngine<Clause, T, Q> engine)
    {
        this.engine = engine;

        Parser<Clause, Token> parser = engine.getParser();

        if (!(parser instanceof InteractiveParser))
        {
            throw new IllegalArgumentException("'engine' must be built on an InteractiveParser.");
        }

        this.parser = (InteractiveParser) parser;
    }

    /**
     * Implements the top-level interpreter loop. This will parse and evaluate sentences until it encounters an CTRL-D
     * in query mode, at which point the interpreter will terminate.
     *
     * @throws SourceCodeException If malformed code is encountered.
     * @throws IOException         If an IO error is encountered whilst reading the source code.
     */
    public void interpreterLoop() throws IOException
    {
        // Display the welcome message.
        printIntroduction();

        // Initialize the JLine console.
        consoleReader = initializeCommandLineReader();

        // Used to buffer input, and only feed it to the parser when a PERIOD is encountered.
        TokenBuffer tokenBuffer = new TokenBuffer();

        // Used to hold the currently buffered lines of input, for the purpose of presenting this back to the user
        // in the event of a syntax or other error in the input.
        ArrayList<String> inputLines = new ArrayList<String>();

        // Used to count the number of lines entered.
        int lineNo = 0;

        while (true)
        {
            String line = null;

            try
            {
                line = consoleReader.readLine(mode.prompt);
                inputLines.add(line);

                // JLine returns null if CTRL-D is pressed. Exit program mode back to query mode, or exit the
                // interpreter completely from query mode.
                if ((line == null) && ((mode == Mode.Query) || (mode == Mode.QueryMultiLine)))
                {
                    /*log.fine("CTRL-D in query mode, exiting.");*/

                    System.out.println();

                    break;
                }
                else if ((line == null) && ((mode == Mode.Program) || (mode == Mode.ProgramMultiLine)))
                {
                    /*log.fine("CTRL-D in program mode, returning to query mode.");*/

                    System.out.println();
                    mode = Mode.Query;

                    continue;
                }

                // Check the input to see if a system directive was input. This is only allowed in query mode, and is
                // handled differently to normal queries.
                if (mode == Mode.Query)
                {
                    Source<Token> tokenSource =
                        new OffsettingTokenSource(TokenSource.getTokenSourceForString(line), lineNo);
                    parser.setTokenSource(tokenSource);

                    PrologParser.Directive directive = parser.peekAndConsumeDirective();

                    if (directive != null)
                    {
                        switch (directive)
                        {
                        case Trace:

                            /*log.fine("Got trace directive.");*/
                            break;

                        case Info:

                            /*log.fine("Got info directive.");*/
                            break;

                        case User:

                            /*log.fine("Got user directive, entering program mode.");*/
                            mode = Mode.Program;
                            break;
                        }

                        inputLines.clear();

                        continue;
                    }
                }

                // For normal queries, the query functor '?-' begins every statement, this is not passed back from
                // JLine even though it is used as the command prompt.
                if (mode == Mode.Query)
                {
                    line = QUERY_PROMPT + line;
                    inputLines.set(inputLines.size() - 1, line);
                }

                // Buffer input tokens until EOL is reached, of the input is terminated with a PERIOD.
                Source<Token> tokenSource =
                    new OffsettingTokenSource(TokenSource.getTokenSourceForString(line), lineNo);
                Token nextToken;

                while (true)
                {
                    nextToken = tokenSource.poll();

                    if (nextToken == null)
                    {
                        break;
                    }

                    if (nextToken.kind == PrologParserConstants.PERIOD)
                    {
                        /*log.fine("Token was PERIOD.");*/
                        mode = (mode == Mode.QueryMultiLine) ? Mode.Query : mode;
                        mode = (mode == Mode.ProgramMultiLine) ? Mode.Program : mode;

                        tokenBuffer.offer(nextToken);

                        break;
                    }
                    else if (nextToken.kind == PrologParserConstants.EOF)
                    {
                        /*log.fine("Token was EOF.");*/
                        mode = (mode == Mode.Query) ? Mode.QueryMultiLine : mode;
                        mode = (mode == Mode.Program) ? Mode.ProgramMultiLine : mode;

                        lineNo++;

                        break;
                    }

                    tokenBuffer.offer(nextToken);
                }

                // Evaluate the current token buffer, whenever the input is terminated with a PERIOD.
                if ((nextToken != null) && (nextToken.kind == PrologParserConstants.PERIOD))
                {
                    parser.setTokenSource(tokenBuffer);

                    // Parse the next clause.
                    Sentence<Clause> nextParsing = parser.parse();

                    /*log.fine(nextParsing.toString());*/
                    evaluate(nextParsing);

                    inputLines.clear();
                }
            }
            catch (SourceCodeException e)
            {
                SourceCodePosition sourceCodePosition = e.getSourceCodePosition().asZeroOffsetPosition();
                int startLine = sourceCodePosition.getStartLine();
                int endLine = sourceCodePosition.getEndLine();
                int startColumn = sourceCodePosition.getStartColumn();
                int endColumn = sourceCodePosition.getEndColumn();

                System.out.println("[(" + startLine + ", " + startColumn + "), (" + endLine + ", " + endColumn + ")]");

                for (int i = 0; i < inputLines.size(); i++)
                {
                    String errorLine = inputLines.get(i);
                    System.out.println(errorLine);

                    // Check if the line has the error somewhere in it, and mark the part of it that contains the error.
                    int pos = 0;

                    if (i == startLine)
                    {
                        for (; pos < startColumn; pos++)
                        {
                            System.out.print(" ");
                        }
                    }

                    if (i == endLine)
                    {
                        for (; pos <= endColumn; pos++)
                        {
                            System.out.print("^");
                        }

                        System.out.println();
                    }

                    if ((i > startLine) && (i < endLine))
                    {
                        for (; pos < errorLine.length(); pos++)
                        {
                            System.out.print("^");
                        }

                        System.out.println();
                    }
                }

                System.out.println();
                System.out.println(e.getMessage());
                System.out.println();

                inputLines.clear();
                tokenBuffer.clear();
            }
        }
    }

    /** Prints a welcome message. */
    private void printIntroduction()
    {
        System.out.println("| WAM Book Prolog.");
        System.out.println("| Copyright The Sett Ltd.");
        System.out.println("| Licensed under the Apache License, Version 2.0.");
        System.out.println("| http://www.apache.org/licenses/LICENSE-2.0");
        System.out.println();
    }

    /**
     * Sets up the JLine console reader.
     *
     * @return A JLine console reader.
     *
     * @throws IOException If an IO error is encountered while reading the input.
     */
    private ConsoleReader initializeCommandLineReader() throws IOException
    {
        ConsoleReader reader = new ConsoleReader();
        reader.setBellEnabled(false);

        return reader;
    }

    /**
     * Evaluates a query against the resolver or adds a clause to the resolvers domain.
     *
     * @param  sentence The clausal sentence to run as a query or as a clause to add to the domain.
     *
     * @throws SourceCodeException If the query or domain clause fails to compile or link into the resolver.
     */
    private void evaluate(Sentence<Clause> sentence) throws SourceCodeException
    {
        Clause clause = sentence.getT();

        if (clause.isQuery())
        {
            engine.endScope();
            engine.compile(sentence);
            evaluateQuery();
        }
        else
        {
            // Check if the program clause is new, or a continuation of the current predicate.
            int name = clause.getHead().getName();

            if ((currentPredicateName == null) || (currentPredicateName != name))
            {
                engine.endScope();
                currentPredicateName = name;
            }

            addProgramClause(sentence);
        }
    }

    /**
     * Evaluates a query. In the case of queries, the interner is used to recover textual names for the resulting
     * variable bindings. The user is queried through the parser to if more than one solution is required.
     */
    private void evaluateQuery()
    {
        /*log.fine("Read query from input.");*/

        // Create an iterator to generate all solutions on demand with. Iteration will stop if the request to
        // the parser for the more ';' token fails.
        Iterator<Set<Variable>> i = engine.iterator();

        if (!i.hasNext())
        {
            System.out.println("false. ");

            return;
        }

        for (; i.hasNext();)
        {
            Set<Variable> solution = i.next();

            if (solution.isEmpty())
            {
                System.out.print("true");
            }
            else
            {
                for (Iterator<Variable> j = solution.iterator(); j.hasNext();)
                {
                    Variable nextVar = j.next();

                    String varName = engine.getVariableName(nextVar.getName());

                    System.out.print(varName + " = " + nextVar.getValue().toString(engine, true, false));

                    if (j.hasNext())
                    {
                        System.out.println();
                    }
                }
            }

            // Finish automatically if there are no more solutions.
            if (!i.hasNext())
            {
                System.out.println(".");

                break;
            }

            // Check if the user wants more solutions.
            try
            {
                int key = consoleReader.readVirtualKey();

                if (key == SEMICOLON)
                {
                    System.out.println(" ;");
                }
                else
                {
                    System.out.println();

                    break;
                }
            }
            catch (IOException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Adds a program clause to the domain. Multiple program clauses making up a predicate are compiled as a unit, and
     * not individually. For this reason, Prolog expects clauses for the same predicate to appear together in source
     * code. When a clause with a name and arity not seen before is encountered, a new compiler scope is entered into,
     * and this compiler scope is closed at the EOF of the current input stream, or when another clause with a different
     * name and arity is seen.
     *
     * @param sentence The clause to add to the domain.
     */
    private void addProgramClause(Sentence<Clause> sentence) throws SourceCodeException
    {
        /*log.fine("Read program clause from input.");*/

        engine.compile(sentence);
    }

    /**
     * Used to buffer tokens.
     */
    private class TokenBuffer implements Source<Token>, Sink<Token>
    {
        LinkedList<Token> tokens = new LinkedList<Token>();

        public boolean offer(Token o)
        {
            return tokens.offer(o);
        }

        public Token poll()
        {
            return tokens.poll();
        }

        public Token peek()
        {
            return tokens.peek();
        }

        public void clear()
        {
            tokens.clear();
        }
    }

    /**
     * OffsettingTokenSource is a token source that automatically adds in line offsets to all tokens, to assist the
     * parser when operating interactively line-at-a-time.
     */
    private class OffsettingTokenSource implements Source<Token>
    {
        /** Holds the underlying token source. */
        private final Source<Token> source;

        /** Holds the current line offset to add to all tokens. */
        private final int lineOffset;

        /**
         * Wraps another token source.
         *
         * @param source The token source to wrap.
         */
        private OffsettingTokenSource(Source<Token> source, int lineOffset)
        {
            this.source = source;
            this.lineOffset = lineOffset;
        }

        /** {@inheritDoc} */
        public Token poll()
        {
            return addOffset(copyToken(source.poll()));
        }

        /** {@inheritDoc} */
        public Token peek()
        {
            return addOffset(copyToken(source.peek()));
        }

        private Token addOffset(Token token)
        {
            token.beginLine += lineOffset;
            token.endLine += lineOffset;

            return token;
        }

        private Token copyToken(Token token)
        {
            Token newToken = new Token();

            newToken.kind = token.kind;
            newToken.beginLine = token.beginLine;
            newToken.beginColumn = token.beginColumn;
            newToken.endLine = token.endLine;
            newToken.endColumn = token.endColumn;
            newToken.image = token.image;
            newToken.next = token.next;
            newToken.specialToken = token.specialToken;

            return newToken;
        }
    }
}
