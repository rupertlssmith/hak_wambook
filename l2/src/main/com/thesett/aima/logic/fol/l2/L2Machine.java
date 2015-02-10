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

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

/**
 * L2Machine is an abstract machine capable of handling the L2 language in its compiled form.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide symbol table for functors names.
 * <tr><td> Provide symbol table for variable names.
 * <tr><td> Accept injected byte code for functors into the machine.
 * <tr><td> Provide the address of that start offset of code for named functors.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface L2Machine extends VariableAndFunctorInterner
{
    /**
     * Provides a call point for the named functor. This method is primarily for use when storing or linking compiled
     * byte code, into its binary form in a machine. The returned call point provides a binary address to use for calls
     * to the specified functor.
     *
     * @param  functorName The interned name of the functor to get a binary call point for.
     *
     * @return The call point of the specified functor to call.
     */
    public L2CallPoint resolveCallPoint(int functorName);

    /**
     * Adds compiled byte code to the code area of the machine.
     *
     * @param  clause The compiled clause to add byte code to the machine for.
     *
     * @throws LinkageException If the clause to be added to the machine, cannot be added to it, because it depends on
     *                          the existence of other callable clause heads which are not in the machine.
     *                          Implementations may elect to raise this as an error at the time the functor is added to
     *                          the machine, or during execution, or simply to fail to find a solution.
     */
    public void emmitCode(L2CompiledClause clause) throws LinkageException;

    /**
     * Extracts the binary byte code from the machine for a given call point.
     *
     * @param  callPoint The call point giving the location of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] retrieveCode(L2CallPoint callPoint);
}
