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
package com.thesett.aima.logic.fol.l3;

import com.thesett.aima.logic.fol.LinkageException;
import com.thesett.aima.logic.fol.VariableAndFunctorInterner;

/**
 * L3Machine is an abstract machine capable of handling the L3 language in its compiled form.
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
public interface L3Machine extends VariableAndFunctorInterner
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
    L3CallPoint resolveCallPoint(int functorName);

    /**
     * Reserves a forward address referencing a label id, within the current predicate. The forward address will be
     * filled in once the label to which it refers is known. This is done using the {@link #resolveLabelPoint} method.
     *
     * @param labelName The interned name of the label to reserve a code address space for.
     * @param offset    The referenced from offset within the code area to the label.
     */
    void reserveReferenceToLabel(int labelName, int offset);

    /**
     * Resolves all forward reference to a label id, to a known address. The address of the label is also specified.
     * Once a label address has been resolved by calling this method, any subsequent reference to the label, created
     * with the {@link #reserveReferenceToLabel} method, will immediately resolve onto it. That is to say that, this
     * method only needs to be invoked once for each label.
     *
     * @param labelName The interned name of the label to resolve.
     * @param address   The address of the resolved label.
     */
    void resolveLabelPoint(int labelName, int address);

    /**
     * Adds compiled byte code to the code area of the machine.
     *
     * @param  predicate The compiled predicate to add byte code to the machine for.
     *
     * @throws LinkageException If the predicate to be added to the machine, cannot be added to it, because it depends
     *                          on the existence of other callable predicate heads which are not in the machine.
     *                          Implementations may elect to raise this as an error at the time the functor is added to
     *                          the machine, or during execution, or simply to fail to find a solution.
     */
    void emmitCode(L3CompiledPredicate predicate) throws LinkageException;

    /**
     * Adds compiled byte code for a query to the code area of the machine.
     *
     * @param  query The compiled query to add byte code to the machine for.
     *
     * @throws LinkageException If the query to be added to the machine, cannot be added to it, because it depends on
     *                          the existence of other callable query heads which are not in the machine.
     *                          Implementations may elect to raise this as an error at the time the functor is added to
     *                          the machine, or during execution, or simply to fail to find a solution.
     */
    void emmitCode(L3CompiledQuery query) throws LinkageException;

    /**
     * Adds an address into code at a specified offset. This will happen when working with forward references. When code
     * is initially added, a forward reference may not yet reside at a known address, in which case the address can be
     * filled in with a dummy value, and the offset recorded. This can be used to fill in the correct value later.
     *
     * @param offset  The offset into the code area to write the address.
     * @param address The address to fill in.
     */
    void emmitCode(int offset, int address);

    /**
     * Extracts the binary byte code from the machine for a given call point.
     *
     * @param  callPoint The call point giving the location of the code.
     *
     * @return The byte code at the specified location.
     */
    byte[] retrieveCode(L3CallPoint callPoint);
}
