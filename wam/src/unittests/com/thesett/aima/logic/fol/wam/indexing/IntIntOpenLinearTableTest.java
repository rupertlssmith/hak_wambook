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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import junit.framework.TestCase;

/**
 * Tests the hash table implementation in {@link IntIntOpenLinearTable}.
 *
 * @author Rupert Smith
 */
public class IntIntOpenLinearTableTest extends TestCase
{
    /** Check that random keys are not matched against an empty table. */
    public void testPutGetZeroForNonExistentKeyEmpty() throws Exception
    {
        CodeBufferTable testMap = createTestTable();
        Random r = new Random();

        for (int i = 1; i <= 1000; i++)
        {
            int key = r.nextInt(Integer.MAX_VALUE);

            if (testMap.get(key) != 0)
            {
                fail("Empty map did not return zero value for key " + key + ".");
            }
        }
    }

    /** Check that random keys can be inserted starting from an empty table. */
    public void testPutOkEmpty() throws Exception
    {
        CodeBufferTable testMap = createTestTable();
        Random r = new Random();

        for (int i = 1; i <= 1000; i++)
        {
            testMap.put(r.nextInt(Integer.MAX_VALUE), i);
        }
    }

    /** Check that random keys are not matched against an existing table not containing those keys. */
    public void testPutGetZeroForNonExistentKey() throws Exception
    {
        CodeBufferTable testMap = createTestTable();
        Random r = new Random();
        Set<Integer> keySet = new HashSet<Integer>();

        for (int i = 1; i <= 1000; i++)
        {
            int key = r.nextInt(Integer.MAX_VALUE);
            testMap.put(key, i);
            keySet.add(key);
        }

        for (int i = 1; i <= 1000; i++)
        {
            int key = r.nextInt(Integer.MAX_VALUE);

            // Try again if the key is inserted.
            if (keySet.contains(key))
            {
                i--;

                continue;
            }

            if (testMap.get(key) != 0)
            {
                fail("Map did not return zero value for key " + key + ".");
            }
        }
    }

    /** Check that random keys are matched against an existing table not containing those keys to the correct values. */
    public void testPutGetOk() throws Exception
    {
        CodeBufferTable testMap = createTestTable();
        Random r = new Random();
        Map<Integer, Integer> entries = new HashMap<Integer, Integer>();

        for (int i = 1; i <= 1000; i++)
        {
            int key;

            do
            {
                key = r.nextInt(Integer.MAX_VALUE);
            }
            while (entries.containsKey(key));

            testMap.put(key, i);
            entries.put(key, i);
        }

        for (Map.Entry<Integer, Integer> entry : entries.entrySet())
        {
            int key = entry.getKey();
            int value = entry.getValue();

            int testValue = testMap.get(key);

            if (testValue != value)
            {
                fail("Map did not return expected value " + value + " for key " + key + " but got " + testValue + ".");
            }
        }
    }

    private CodeBufferTable createTestTable()
    {
        CodeBufferTable testMap = new IntIntOpenLinearTable();
        testMap.setup(ByteBuffer.allocate(16000), 500, 12000);

        return testMap;
    }
}
