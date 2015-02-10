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

/**
 * Holds symbol table key definitions. These are used as column selectors to store/retrieve information on the symbol
 * table.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Hold symbol table key definitions.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class SymbolTableKeys
{
    /** The symbol table key for allocations. */
    public static final String SYMKEY_ALLOCATION = "allocation";

    /** The symbol table key for the number of permanent variables remaining. */
    public static final String SYMKEY_PERM_VARS_REMAINING = "perm_vars_remaining";

    /** The symbol table key for variable occurrence counts. */
    public static final String SYMKEY_VAR_OCCURRENCE_COUNT = "var_occurrence_count";

    /** The symbol table key for variable position of occurrence. */
    public static final String SYMKEY_VAR_NON_ARG = "var_non_arg";

    /** The symbol table key for functor position of occurrence. */
    public static final String SYMKEY_FUNCTOR_NON_ARG = "functor_non_arg";

    /** The symbol table key for variable introduction type. */
    public static final String SYMKEY_VARIABLE_INTRO = "variable_intro";

    /** The symbol table key for the last functor in which a variable occurs, if it is purely in argument position. */
    public static final String SYMKEY_VAR_LAST_ARG_FUNCTOR = "var_last_arg_functor";

    /** The symbol table key for functors that are top-level within a clause. */
    public static final String SYMKEY_TOP_LEVEL_FUNCTOR = "top_level_functor";

    /** The symbol table key for permanent variable offset to hold a cut to choice point frame in. */
    public static final String SYMKEY_CLAUSE_PERM_CUT = "clause_perm_cut";

    /** The symbol table key for predicate sources. */
    public static final String SYMKEY_PREDICATES = "source_predicates";
}
