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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.java;

import javax.annotation.concurrent.NotThreadSafe;


/**
 * {@link Reference} - A simple reference class.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.6.1
 * @param <V> The type of object referred to by this reference
 */
@NotThreadSafe
public final class Reference<V> {

    /** The value */
    private V value;

    /**
     * Initializes a new {@link Reference}.
     */
    public Reference() {
        this(null);
    }

    /**
     * Initializes a new {@link Reference}.
     *
     * @param value The value to set
     */
    public Reference(V value) {
        super();
        this.value = value;
    }

    /**
     * Checks for a <code>null</code> value
     *
     * @return <code>true</code> if this reference holds a <code>null</code>  value; otherwise <code>false</code>
     */
    public boolean hasNoValue() {
        return value == null;
    }

    /**
     * Checks for a non-<code>null</code> value
     *
     * @return <code>true</code> if this reference holds a non-<code>null</code>  value; otherwise <code>false</code>
     */
    public boolean hasValue() {
        return value != null;
    }

    /**
     * Gets the value
     *
     * @return The value
     */
    public V getValue() {
        return value;
    }

    /**
     * Gets the current value & sets to the new value.
     *
     * @return The current value
     */
    public V getAndSetValue(V newValue) {
        V value = this.value;
        this.value = newValue;
        return value;
    }

    /**
     * Sets the value
     *
     * @param value The value to set
     * @return This reference with new value applied
     */
    public Reference<V> setValue(V value) {
        this.value = value;
        return this;
    }

}
