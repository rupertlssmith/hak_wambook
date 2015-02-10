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

import com.thesett.aima.logic.fol.FunctorName;

/**
 * WAMLabel is a code label within a predicate.
 *
 * <pre><p/><table id="crc"><caption>CRC Card</caption>
 * <tr><th> Responsibilities <th> Collaborations
 * <tr><td> Represent a code label uniquely identifying it by its containing predicate name, arity, and id.
 * <tr><td> Provide equality checking that considers labels with identical name, arity and id to be equal.
 * </table></pre>
 *
 * @author Rupert Smith
 */
public class WAMLabel extends FunctorName
{
    /** Holds the label identifier. */
    private final int id;

    /**
     * Creates a label within a parent predicate.
     *
     * @param parent The name of the parent predicate.
     * @param id     The unique label id.
     */
    public WAMLabel(FunctorName parent, int id)
    {
        super(parent.getName(), parent.getArity());
        this.id = id;
    }

    /**
     * Provides the labels id.
     *
     * @return The labels id.
     */
    public int getId()
    {
        return id;
    }

    /** {@inheritDoc} */
    public boolean equals(Object o)
    {
        if (this == o)
        {
            return true;
        }

        if ((o == null) || (getClass() != o.getClass()))
        {
            return false;
        }

        if (!super.equals(o))
        {
            return false;
        }

        WAMLabel l3Label = (WAMLabel) o;

        if (arity != l3Label.arity)
        {
            return false;
        }

        if (id != l3Label.id)
        {
            return false;
        }

        if ((name != null) ? (!name.equals(l3Label.name)) : (l3Label.name != null))
        {
            return false;
        }

        return true;
    }

    /** {@inheritDoc} */
    public int hashCode()
    {
        int result = super.hashCode();
        result = (31 * result) + id;
        result = (31 * result) + ((name != null) ? name.hashCode() : 0);
        result = (31 * result) + arity;

        return result;
    }

    /**
     * Pretty prints the label in a standard format.
     *
     * @return The label pretty printed in a standard format.
     */
    public String toPrettyString()
    {
        return (getName() + "/" + getArity() + "_" + getId());
    }

    /**
     * Outputs the label as a functor name and id, used mainly for debugging purposes.
     *
     * @return The label.
     */
    public String toString()
    {
        return "WAMLabel: [ name = " + name + ", arity = " + arity + ", id = " + id + " ]";
    }
}
