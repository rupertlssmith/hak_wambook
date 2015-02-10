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
import com.thesett.common.error.NotImplementedException;

/**
 * L2FileMachine is an {@link L2Machine} that can load and store compiled L2 byte code to files.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Load byte code from a file.
 * <tr><td> Store byte code to a file.
 * </table></pre>
 *
 * @author Rupert Smith
 * @todo   Provide a way for this machine to pass the byte code to an execution machine. For example, by wrapping one.
 */
public class L2FileMachine extends L2BaseMachine
{
    /** {@inheritDoc} */
    public void emmitCode(L2CompiledClause clause) throws LinkageException
    {
        throw new NotImplementedException();
    }

    /**
     * Extracts the raw byte code from the machine for a given call table entry.
     *
     * @param  callPoint The call table entry giving the location and length of the code.
     *
     * @return The byte code at the specified location.
     */
    public byte[] retrieveCode(L2CallPoint callPoint)
    {
        throw new NotImplementedException();
    }
}
