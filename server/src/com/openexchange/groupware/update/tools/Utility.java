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

package com.openexchange.groupware.update.tools;

import java.util.Collection;
import java.util.List;

/**
 * {@link Utility} - Utility class.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Utility {

    /**
     * Initializes a new {@link Utility}.
     */
    private Utility() {
        super();
    }

    /**
     * The radix for base <code>10</code>.
     */
    private static final int RADIX = 10;

    /**
     * Parses a positive <code>int</code> value from passed {@link String} instance.
     * 
     * @param s The string to parse
     * @return The parsed positive <code>int</code> value or <code>-1</code> if parsing failed
     */
    public static int parsePositiveInt(final String s) {
        if (s == null) {
            return -1;
        }

        final int max = s.length();

        if (max <= 0) {
            return -1;
        }
        if (s.charAt(0) == '-') {
            return -1;
        }

        int result = 0;
        int i = 0;

        final int limit = -Integer.MAX_VALUE;
        final int multmin = limit / RADIX;
        int digit;

        if (i < max) {
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return -1;
            }
            result = -digit;
        }
        while (i < max) {
            /*
             * Accumulating negatively avoids surprises near MAX_VALUE
             */
            digit = Character.digit(s.charAt(i++), RADIX);
            if (digit < 0) {
                return -1;
            }
            if (result < multmin) {
                return -1;
            }
            result *= RADIX;
            if (result < limit + digit) {
                return -1;
            }
            result -= digit;
        }
        return -result;
    }

    /**
     * Generates a DB-style output for given map.
     * 
     * <pre>
     * +---------------+---------+
     * | schema        | version |
     * +---------------+---------+
     * | 345fg_dfghdfg | 12      |
     * +---------------+---------+
     * | dfgdg56       | 12      |
     * +---------------+---------+
     * </pre>
     * 
     * @param rows The rows
     * @param columnNames The map's column names
     * @param withBorders <code>true</code> to output with table borders; otherwise <code>false</code>
     * @return A DB-style output for given map
     */
    public static String toTable(final List<Object[]> rows, final String[] columnNames, final boolean withBorders) {

        /*
         * Determine max. length for each column
         */
        final int[] maxLengths = new int[columnNames.length];
        {
            for (int i = 0; i < maxLengths.length; i++) {
                maxLengths[i] = columnNames[i].length();
            }

            for (final Object[] row : rows) {
                for (int i = 0; i < row.length; i++) {
                    final int a = maxLengths[i];
                    final int b = row[i].toString().length();
                    maxLengths[i] = (a >= b) ? a : b;
                }
            }
        }

        final int size = rows.size();

        final StringBuilder sb = new StringBuilder(size * 64);

        final String delimLine;
        if (withBorders) {
            sb.append('+').append('-');
            for (int i = 0; i < maxLengths[0]; i++) {
                sb.append('-');
            }
            for (int i = 1; i < maxLengths.length; i++) {
                sb.append('-').append('+').append('-');
                for (int j = 0; j < maxLengths[i]; j++) {
                    sb.append('-');
                }
            }
            sb.append('-').append('+').append('\n');
            delimLine = sb.toString();
            sb.setLength(0);
        } else {
            delimLine = "";
        }

        sb.append(delimLine);

        appendValues(columnNames, maxLengths, sb, withBorders);

        sb.append(delimLine);

        for (final Object[] row : rows) {
            final String[] values = new String[row.length];
            for (int i = 0; i < values.length; i++) {
                values[i] = row[i].toString();
            }
            appendValues(values, maxLengths, sb, withBorders);
            sb.append(delimLine);
        }

        return sb.toString();
    }

    private static void appendValues(final String[] values, final int[] maxLengths, final StringBuilder sb, final boolean withBorders) {
        if (withBorders) {
            sb.append('|').append(' ');
        }
        sb.append(values[0]);
        for (int i = values[0].length(); i < maxLengths[0]; i++) {
            sb.append(' ');
        }
        for (int i = 1; i < values.length; i++) {
            sb.append(' ');
            if (withBorders) {
                sb.append('|');
            }
            sb.append(' ');
            sb.append(values[i]);
            for (int j = values[i].length(); j < maxLengths[i]; j++) {
                sb.append(' ');
            }
        }
        if (withBorders) {
            sb.append(' ').append('|');
        }
        sb.append('\n');
    }

    private static int getMaxLen(final Collection<? extends Object> c, final int startLen) {
        int max = startLen;
        for (final Object obj : c) {
            final int b = obj.toString().length();
            max = (max >= b) ? max : b;
        }
        return max;
    }

}
