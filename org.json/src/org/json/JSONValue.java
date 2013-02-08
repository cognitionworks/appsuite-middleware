/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package org.json;

import java.io.Serializable;
import java.io.Writer;

/**
 * {@link JSONValue} - The base class for all JSON representations.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface JSONValue extends Serializable {

    /**
     * Write the contents of this JSON value as JSON text to a writer. For compactness, no whitespace is added.<br>
     * Invokes {@link #write(Writer, boolean)} with latter parameter set to <code>false</code>.
     * <p>
     * <b>Warning</b>: This method assumes that the data structure is acyclically.
     *
     * @param The writer to write to
     * @return The specified writer for chained invocations
     * @throws JSONException If writing the JSON object fails (e.g. I/O error)
     */
    public Writer write(Writer writer) throws JSONException;

    /**
     * Write the contents of this JSON value as JSON text to a writer. For compactness, no whitespace is added.
     * <p>
     * <b>Warning</b>: This method assumes that the data structure is acyclically.
     *
     * @param The writer to write to
     * @param asciiOnly <code>true</code> to only write ASCII characters; otherwise <code>false</code>
     * @return The specified writer for chained invocations
     * @throws JSONException If writing the JSON object fails (e.g. I/O error)
     */
    public Writer write(Writer writer, boolean asciiOnly) throws JSONException;

    /**
     * Resets this JSON value for re-use.
     */
    public void reset();

    /**
     * Get the number of elements stored in this JSON value.
     *
     * @return The number of elements stored in this JSON value.
     */
    public int length();

    /**
     * Checks if this JSON value contains no elements.
     *
     * @return <tt>true</tt> if this JSON value contains no elements
     */
    boolean isEmpty();

    /**
     * Make a pretty-printed JSON text of this JSON value.
     * <p>
     * Warning: This method assumes that the data structure is acyclically.
     *
     * @param indentFactor The number of spaces to add to each level of indentation.
     * @param indent The indention of the top level.
     * @return A printable, displayable, and transmittable representation of the JSON value.
     * @throws JSONException If JSON value cannot be pretty-printed
     */
    public String toString(final int indentFactor, final int indent) throws JSONException;

    /**
     * Check if this value represents a JSON array.
     *
     * @return <code>true</code> if this value represents a JSON array; otherwise <code>false</code>
     */
    public boolean isArray();

    /**
     * Check if this value represents a JSON object.
     *
     * @return <code>true</code> if this value represents a JSON object; otherwise <code>false</code>
     */
    public boolean isObject();

    /**
     * Gets the {@link JSONValue}'s {@link JSONObject} representation (if appropriate).
     *
     * @return The associated {@link JSONObject} or <code>null</code>
     */
    public JSONObject toObject();

    /**
     * Gets the {@link JSONValue}'s {@link JSONArray} representation (if appropriate).
     *
     * @return The associated {@link JSONArray} or <code>null</code>
     */
    public JSONArray toArray();
}
