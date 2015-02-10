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
package com.thesett.aima.logic.fol.l0;

import com.thesett.aima.logic.fol.VariableAndFunctorInternerImpl;

/**
 * L0BaseMachine provides basic services common to all L0 machines.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Provide symbol table for functors names.
 * <tr><td> Provide symbol table for variable names.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public abstract class L0BaseMachine extends VariableAndFunctorInternerImpl
{
    /** Creates the base machine, providing variable and functor symbol tables. */
    protected L0BaseMachine()
    {
        super("L0_Variable_Namespace", "L0_Functor_Namespace");
    }
}
