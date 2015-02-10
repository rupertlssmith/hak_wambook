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

/**
 * StateMachine is used to implement a FSMD, that is driven by a {@link Matcher}.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Accept the matcher that drives this state machine. </td></tr>
 * <tr><td> Accept input from the matcher. </td></tr>
 * <tr><td> Accept end of input from the matcher. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface StateMachine<S, T>
{
    /**
     * Sets up the matcher that drives this state machine.
     *
     * @param matcher The matcher that drives this state machine.
     */
    public void setMatcher(Matcher<S, T> matcher);

    /**
     * Accepts the next input from the matcher.
     *
     * @param next The next input data item.
     */
    void apply(S next);

    /** Accepts end of input notification from the matcher. */
    void end();
}
