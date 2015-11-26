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

import com.thesett.aima.logic.fol.Term;

/**
 * A compiled term is a handle onto a compiled down to binary code term. Compiled terms are not Java code and may be
 * executed outside of the JVM, but the Java retains a handle on them and provides sufficient wrapping around them that
 * they can be made to look as if they are a transparent abstract syntax tree within the JVM.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities
 * <tr><td> Decompile/decode a binary term to restore its abstract syntax tree.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public interface L0CompiledTerm extends Term
{
    /** Decompiles the term. */
    void decompile();
}
