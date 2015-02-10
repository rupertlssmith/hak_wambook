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
package com.thesett.aima.logic.fol.wam.builtins;

import com.thesett.aima.logic.fol.Functor;
import com.thesett.aima.logic.fol.FunctorName;
import com.thesett.aima.logic.fol.wam.compiler.DefaultBuiltIn;
import com.thesett.aima.logic.fol.wam.compiler.WAMInstruction;
import com.thesett.common.util.SizeableLinkedList;

/**
 * NonUnifies is the ISO Prolog built in operator '\='/2. It performs a standard unification (no occurrs check) on its
 * left and right arguments, possibly binding variables as a result of the unification, and fails iff the unification
 * succeeds. As failure will cause this proof step to be undone, any variable bindings resulting from the unification
 * will immediately be discarded.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Check if unification of the left and right arguments of the non-unify operator fails.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class NonUnifies extends BaseBuiltIn
{
    /**
     * Creates a cut built-in to implement the specified functor.
     *
     * @param functor        The functor to implement as a built-in.
     * @param defaultBuiltIn The default built in, for standard compilation and interners and symbol tables.
     */
    public NonUnifies(Functor functor, DefaultBuiltIn defaultBuiltIn)
    {
        super(functor, defaultBuiltIn);
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyArguments(Functor expression, boolean isFirstBody,
        FunctorName clauseName, int bodyNumber)
    {
        return new SizeableLinkedList<WAMInstruction>();
    }

    /** {@inheritDoc} */
    public SizeableLinkedList<WAMInstruction> compileBodyCall(Functor expression, boolean isFirstBody,
        boolean isLastBody, boolean chainRule, int permVarsRemaining)
    {
        return new SizeableLinkedList<WAMInstruction>();
    }

    /**
     * Creates a string representation of this functor, mostly used for debugging purposes.
     *
     * @return A string representation of this functor.
     */
    public String toString()
    {
        return "NonUnifies: [ arguments = " + toStringArguments() + " ]";
    }
}
