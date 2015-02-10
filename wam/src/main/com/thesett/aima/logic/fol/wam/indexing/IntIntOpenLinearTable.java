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
 * IntIntOpenLinearTable implements a {@link CodeBufferTable} using open addressing and linear probing with a step size
 * of one.
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
public class IntIntOpenLinearTable implements CodeBufferTable
{
    /** The byte buffer holding the table data. */
    private ByteBuffer buffer;

    /** The offset of the base of the table within the byte buffer. */
    private int offset;

    /** The size of the table in number of entries (not bytes or integers). */
    private int size;

    /** {@inheritDoc} */
    public void setup(ByteBuffer buffer, int t, int n)
    {
        this.buffer = buffer;
        this.offset = t;
        this.size = (n >> 3); // Shift by 3 as key and value must be stored as a pair, making 8 bytes per entry.
    }

    /** {@inheritDoc} */
    public int get(int key)
    {
        int addr = addr(hash(key));

        while (true)
        {
            int tableKey = buffer.getInt(addr);

            if (key == tableKey)
            {
                return buffer.getInt(addr + 4);
            }

            if (tableKey == 0)
            {
                return 0;
            }

            addr += 8;
        }
    }

    /** {@inheritDoc} */
    public void put(int key, int val)
    {
        int addr = addr(hash(key));

        while (true)
        {
            int tableKey = buffer.getInt(addr);

            if (key == tableKey)
            {
                break;
            }

            if (tableKey == 0)
            {
                break;
            }

            addr += 8;
        }

        buffer.putInt(addr, key);
        buffer.putInt(addr + 4, val);
    }

    /**
     * Converts an entry offset into the table, into an address within the table, relative to the zero offset of the
     * underlying byte buffer.
     *
     * @param  entry The entry offset.
     *
     * @return The address within the byte buffer at which the entry can be found.
     */
    private int addr(int entry)
    {
        return offset + ((entry % size) << 3);
    }

    /**
     * Computes a hash of the key.
     *
     * @param  key The key to hash.
     *
     * @return The hashed key.
     */
    private int hash(int key)
    {
        return key;
    }
}
