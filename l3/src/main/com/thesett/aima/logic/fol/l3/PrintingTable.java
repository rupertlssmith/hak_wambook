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

/**
 * PrintingTable collects information about the row count, and row and column sizes, in order to print information in a
 * table format.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Collect row count, row and column size stats.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface PrintingTable
{
    /**
     * Updates the maximum row count.
     *
     * @param row The new maximum row count, accepted if larger than the previous value.
     */
    void setMaxRowCount(int row);

    /**
     * Updates the maximum row height.
     *
     * @param row    The row to update stats for.
     * @param height The new maximum row height, accepted if larger than the previous value.
     */
    void setMaxRowHeight(int row, int height);

    /**
     * Updates the maximum column width.
     *
     * @param column The column to update stats for.
     * @param width  The new maximum column width, accepted if larger than the previous value.
     */
    void setMaxColumnWidth(int column, int width);
}
