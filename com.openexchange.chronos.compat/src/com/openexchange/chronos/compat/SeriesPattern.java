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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.compat;

import com.openexchange.java.Strings;

/**
 * {@link SeriesPattern}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class SeriesPattern {

    private int type;
    private int interval;
    private int daysOfWeek;
    private int dayOfMonth;
    private int month;
    private int occurrences;
    private long seriesStart;
    private long seriesEnd;

    public static SeriesPattern parse(String databasePattern) {
        if (Strings.isEmpty(databasePattern)) {
            return null;
        }
        SeriesPattern pattern = new SeriesPattern();
        String[] splitted = databasePattern.split("\\|");
        for (int i = 1; i < splitted.length; i += 2) {
            String key = splitted[i - 1];
            String value = splitted[i];
            switch (key) {
                case "t":
                    pattern.type = Integer.valueOf(value);
                    break;
                case "i":
                    pattern.interval = Integer.valueOf(value);
                    break;
                case "a":
                    pattern.daysOfWeek = Integer.valueOf(value);
                    break;
                case "b":
                    pattern.dayOfMonth = Integer.valueOf(value);
                    break;
                case "c":
                    pattern.month = Integer.valueOf(value);
                    break;
                case "o":
                    pattern.occurrences = Integer.valueOf(value);
                    break;
                case "s":
                    pattern.seriesStart = Long.valueOf(value);
                    break;
                case "e":
                    pattern.seriesEnd = Long.valueOf(value);
                    break;
                default:
                    throw new IllegalArgumentException("Unexpected key: " + key);
            }
        }
        return pattern;
    }

    /**
     * @return the type
     */
    public int getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(int type) {
        this.type = type;
    }

    /**
     * @return the interval
     */
    public int getInterval() {
        return interval;
    }

    /**
     * @param interval the interval to set
     */
    public void setInterval(int interval) {
        this.interval = interval;
    }

    /**
     * @return the daysOfWeek
     */
    public int getDaysOfWeek() {
        return daysOfWeek;
    }

    /**
     * @param daysOfWeek the daysOfWeek to set
     */
    public void setDaysOfWeek(int daysOfWeek) {
        this.daysOfWeek = daysOfWeek;
    }

    /**
     * @return the dayOfMonth
     */
    public int getDayOfMonth() {
        return dayOfMonth;
    }

    /**
     * @param dayOfMonth the dayOfMonth to set
     */
    public void setDayOfMonth(int dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    /**
     * @return the month
     */
    public int getMonth() {
        return month;
    }

    /**
     * @param month the month to set
     */
    public void setMonth(int month) {
        this.month = month;
    }

    /**
     * @return the occurrences
     */
    public int getOccurrences() {
        return occurrences;
    }

    /**
     * @param occurrences the occurrences to set
     */
    public void setOccurrences(int occurrences) {
        this.occurrences = occurrences;
    }

    /**
     * @return the seriesStart
     */
    public long getSeriesStart() {
        return seriesStart;
    }

    /**
     * @param seriesStart the seriesStart to set
     */
    public void setSeriesStart(long seriesStart) {
        this.seriesStart = seriesStart;
    }

    /**
     * @return the seriesEnd
     */
    public long getSeriesEnd() {
        return seriesEnd;
    }

    /**
     * @param seriesEnd the seriesEnd to set
     */
    public void setSeriesEnd(long seriesEnd) {
        this.seriesEnd = seriesEnd;
    }

}
