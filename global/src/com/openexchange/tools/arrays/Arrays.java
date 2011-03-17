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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.tools.arrays;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Contains convenience methods for dealing with arrays.
 * 
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public final class Arrays {

    /**
     * Prevent instantiation
     */
    private Arrays() {
        super();
    }

    /**
     * Searches the given int value in the int array.
     * 
     * @param array int array tested for containing the search parameter.
     * @param search this int is tested if the array contains it.
     * @return <code>true</code> if the array contains the int value.
     */
    public static boolean contains(final int[] array, final int search) {
        boolean found = false;
        for (final int test : array) {
            if (test == search) {
                found = true;
                break;
            }
        }
        return found;
    }

    public static int[] addUniquely(final int[] toExtend, final int... other) {
        if (other == null) {
            return toExtend;
        }
        final Set<Integer> tmp = new HashSet<Integer>();
        for (final int i : toExtend) {
            tmp.add(new Integer(i));
        }
        for (final int i : other) {
            tmp.add(new Integer(i));
        }
        final int[] retval = new int[tmp.size()];
        int pos = 0;
        for (final Integer i : tmp) {
            retval[pos++] = i.intValue();
        }
        return retval;
    }

    public static <T> T[] remove(T[] removeFrom, T... toRemove) {
        List<T> tmp = new ArrayList<T>();
        for (T copy : removeFrom) {
            tmp.add(copy);
        }
        for (T remove : toRemove) {
            tmp.remove(remove);
        }
        @SuppressWarnings("unchecked")
        T[] retval = tmp.toArray((T[]) Array.newInstance(removeFrom.getClass().getComponentType(), tmp.size()));
        return retval;
    }

    public static <T> T[] add(T[] toExtend, T... other) {
        if (other == null) {
            return toExtend;
        }
        @SuppressWarnings("unchecked")
        T[] tmp = (T[]) Array.newInstance(toExtend.getClass().getComponentType(), toExtend.length + other.length);
        System.arraycopy(toExtend, 0, tmp, 0, toExtend.length);
        System.arraycopy(other, 0, tmp, toExtend.length, other.length);
        return tmp;
    }

    public static <T> T[] clone(T[] toClone) {
        @SuppressWarnings("unchecked")
        T[] retval = (T[]) Array.newInstance(toClone.getClass().getComponentType(), toClone.length);
        System.arraycopy(toClone, 0, retval, 0, toClone.length);
        return retval;
    }

    public static int[] extract(int[] source, int start, int length) {
        final int realLength;
        if (start + length > source.length) {
            realLength = source.length - start;
        } else {
            realLength = length;
        }
        final int[] retval = new int[realLength];
        System.arraycopy(source, start, retval, 0, realLength);
        return retval;
    }
}
