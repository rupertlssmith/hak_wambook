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
package com.thesett.aima.logic.fol.wam.indexing;

import java.nio.ByteBuffer;

/**
 * CodeBufferTable is an associative array interface that is kept as simple as possible, for the purpose to which it is
 * used. A CodeBufferTable is a map from integer keys, to integer address, that holds its mapping table in a ByteBuffer.
 * The table will be built at compile time, using the {@link #put(int, int)} operations, and only read from at run time,
 * using the {@link #get(int)} operation. Puts and gets will not be interleaved, and the number of items in the table
 * will be known in advance. There is no delete operation, or any other operations from standard Map interfaces.
 *
 * <p/>Prior to using the put or get operations, the table must be initialized to work on an area of a ByteBuffer, and
 * this is accomplished with the {@link #setup(ByteBuffer, int, int)} operation.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Establish a table on a ByteBuffer. </td></tr>
 * <tr><td> Associate an address with a key. </td></tr>
 * <tr><td> Look up an address by key. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface CodeBufferTable
{
    /**
     * Initializes the table against an area within a ByteBuffer.
     *
     * @param buffer The underlying buffer to store the hash table in.
     * @param t      The offset of the start of the hash table.
     * @param n      The size of the hash table in bytes.
     */
    void setup(ByteBuffer buffer, int t, int n);

    /**
     * Looks up an address by key, in the hash table of size n referred to.
     *
     * @param  key The key to look up.
     *
     * @return <tt>0</tt> iff no match is found, or the matching address.
     */
    int get(int key);

    /**
     * Inserts an address associated with a key, in the hash table of size n referred to.
     *
     * @param key  The key to insert.
     * @param addr An address to associate with the key.
     */
    void put(int key, int addr);
}
