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

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * WAMReservedLabel is an {@link WAMCallPoint} that is an address for a label. The label in question may be forward
 * referenced, so its address cannot be resolved until the label is encountered as code is generated. In the situation
 * that a forward reference to a label is encountered, a dummy invalid address can be substituted for the label, and the
 * offset at which that dummy address is written can be recorded. Once the label has been resolved to a valid address,
 * the dummy values can be re-visited and corrected. WAMReservedLabel is an WAMCallPoint that also maintains a list of
 * referenced from address to be corrected, once the label address is known.
 *
 * <p/>An WAMReservedLabel is created with an entry point of -1, which means that it has not been resolved to a known
 * address yet. Once the entry point is set to a value other than -1, it is considered to have been resolved.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Maintain a list of referenced from address against a label.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMReservedLabel extends WAMCallPoint
{
    /** The list of referenced from addresses. */
    public Collection<Integer> referenceList = new LinkedList<Integer>();

    /**
     * Creates a call table entry for the code with the specified entry address and length.
     *
     * @param functorName The functors interned name.
     */
    public WAMReservedLabel(int functorName)
    {
        super(-1, 0, functorName);
    }

    /**
     * Checks if this label has been resolved to a concrete address yet.
     *
     * @return <tt>true</tt> iff this label has been resolved to a concrete address.
     */
    public boolean isResolved()
    {
        return entryPoint != -1;
    }
}
