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
package com.thesett.aima.logic.fol.wam.optimizer;

import java.util.Iterator;
import java.util.Queue;

import com.thesett.common.util.SequenceIterator;

/**
 * Matcher is a sequence that is used to drive a {@link StateMachine}. An input sequence is fed into an FSMD and the
 * output of the state machine is presented as another sequence.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Feed an input sequence into a state machine. </td></tr>
 * <tr><td> Extract the output of a state machine as a sequence. </td></tr>
 * <tr><td> Inform the state machine when the end of the input sequence is reached. </td></tr>
 * <tr><td> Sink output from the state machine. </td></tr>
 * <tr><td> Accept flush commands from the state machine, indicating that a sequence of outputs is complete. </td></tr>
 * </table></pre>
 *
 * @param  <S> The input data element type.
 * @param  <T> The output data element type.
 *
 * @author Rupert Smith
 */
public class Matcher<S, T> extends SequenceIterator<T> implements Iterable<T>
{
    /** Holds the state machine to drive. */
    private final StateMachine<S, T> fsm;

    /** Holds the input sequence to feed to the state machine. */
    private final Iterator<S> source;

    /** Holds a buffer to sink state machine output into. */
    private Queue<T> buffer;

    /** Set when the state machine indicates its output is ready. */
    private boolean flushMode;

    /**
     * Builds the sequence driver for a state machine.
     *
     * @param source The input data source.
     * @param fsm    The state machine.
     */
    public Matcher(Iterator<S> source, StateMachine<S, T> fsm)
    {
        this.source = source;
        this.fsm = fsm;
        fsm.setMatcher(this);
    }

    /** {@inheritDoc} */
    public T nextInSequence()
    {
        T result = null;

        // Poll results from the buffer until no more are available, but only once the state machine has flushed
        // the buffer.
        if (flushMode)
        {
            result = buffer.poll();

            if (result != null)
            {
                return result;
            }

            flushMode = false;
        }

        // Feed input from the source into the state machine, until some results become available on the buffer.
        while (source.hasNext())
        {
            S next = source.next();
            fsm.apply(next);

            if (flushMode)
            {
                result = buffer.poll();

                if (result != null)
                {
                    return result;
                }

                flushMode = false;
            }
        }

        // Once the end of the input source is reached, inform the state machine of this, and try and poll any
        // buffered results.
        fsm.end();

        if (flushMode)
        {
            result = buffer.poll();
        }

        return result;
    }

    /**
     * Accepts an output source to be polled until empty and presented as the output, before consuming more inputs.
     *
     * @param source The output source to flush.
     */
    public void sinkAll(Queue<T> source)
    {
        flushMode = true;
        buffer = source;
    }

    /**
     * Presents this filterator as an iterable.
     *
     * @return This filterator as an iterable.
     */
    public Iterator<T> iterator()
    {
        return this;
    }
}
