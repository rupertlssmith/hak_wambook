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
package com.thesett.aima.logic.fol.wam.machine;

/**
 * WAMResolvingMachineDPIMonitor is a call-back interface, that can be attached to a {@link WAMResolvingMachineDPI}, in
 * order to receive notification of certain events published by the abstract machine through it debugging interface.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Accept notification of the machine being reset. </td></tr>
 * <tr><td> Accept notification that the machine has stepped by one instruction. </td></tr>
 * <tr><td> Accept notification that the machine is starting a code execution. </td></tr>
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface WAMResolvingMachineDPIMonitor
{
    /**
     * Accepts notification that the machine has been reset.
     *
     * @param dpi The machines DPI
     */
    void onReset(WAMResolvingMachineDPI dpi);

    /**
     * Accepts notification of changes to byte code loaded into the machine.
     *
     * @param dpi    The machines DPI.
     * @param start  The start offset of the changed byte code within the machines code buffer.
     * @param length The length of the changed byte code within the machines code buffer.
     */
    void onCodeUpdate(WAMResolvingMachineDPI dpi, int start, int length);

    /**
     * Accepts notification that the machine is starting a code execution.
     *
     * @param dpi The machines DPI
     */
    void onExecute(WAMResolvingMachineDPI dpi);

    /**
     * Accepts notification that the machine has been stepped by one instruction.
     *
     * @param dpi The machines DPI
     */
    void onStep(WAMResolvingMachineDPI dpi);
}
