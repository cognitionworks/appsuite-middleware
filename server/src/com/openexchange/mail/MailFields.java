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

package com.openexchange.mail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * {@link MailFields} - Container for instances of {@link MailField} providing common set-specific methods.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailFields {

    private static final MailField[] VALUES = MailField.values();

    private final boolean[] arr;

    /**
     * Initializes an empty instance of {@link MailFields}
     */
    public MailFields() {
        super();
        arr = new boolean[VALUES.length];
        Arrays.fill(arr, false);
    }

    /**
     * Initializes a new instance of {@link MailFields} pre-filled with specified array of {@link MailField} constants.
     * 
     * @param mailFields The mail fields to add
     */
    public MailFields(final MailField[] mailFields) {
        this();
        for (final MailField mailField : mailFields) {
            arr[mailField.ordinal()] = true;
        }
    }

    /**
     * Initializes a new instance of {@link MailFields} pre-filled with specified collection of {@link MailField} constants.
     * 
     * @param mailFields The collection of mail fields to add
     */
    public MailFields(final Collection<MailField> mailFields) {
        this();
        for (final MailField mailField : mailFields) {
            arr[mailField.ordinal()] = true;
        }
    }

    /**
     * Adds specified {@link MailField} constant.
     * 
     * @param mailField The mail field to add
     */
    public void add(final MailField mailField) {
        arr[mailField.ordinal()] = true;
    }

    /**
     * Adds specified {@link MailField} constants.
     * 
     * @param mailFields The mail fields to add
     */
    public void addAll(final MailField[] mailFields) {
        for (final MailField mailField : mailFields) {
            arr[mailField.ordinal()] = true;
        }
    }

    /**
     * Adds specified collection of {@link MailField} constants.
     * 
     * @param mailFields The collection of {@link MailField} constants to add
     */
    public void addAll(final Collection<MailField> mailFields) {
        for (final MailField mailField : mailFields) {
            arr[mailField.ordinal()] = true;
        }
    }

    /**
     * Removes specified {@link MailField} constant.
     * 
     * @param mailField The mail field to remove
     */
    public void removeMailField(final MailField mailField) {
        arr[mailField.ordinal()] = false;
    }

    /**
     * Removes specified {@link MailField} constants.
     * 
     * @param mailFields The mail fields to remove
     */
    public void removeMailFields(final MailField[] mailFields) {
        for (final MailField mailField : mailFields) {
            arr[mailField.ordinal()] = false;
        }
    }

    /**
     * Checks if specified {@link MailField} constant is contained.
     * 
     * @param mailField The mail field to check
     * @return <code>true</code> if specified {@link MailField} constant is contained; otherwise <code>false</code>.
     */
    public boolean contains(final MailField mailField) {
        return arr[mailField.ordinal()];
    }

    /**
     * Returns a newly created array of {@link MailField} constants
     * 
     * @return A newly created array of {@link MailField} constants
     */
    public MailField[] toArray() {
        final List<MailField> l = new ArrayList<MailField>(arr.length);
        for (int i = 0; i < arr.length; i++) {
            if (arr[i]) {
                l.add(VALUES[i]);
            }
        }
        return l.toArray(new MailField[l.size()]);
    }
}
