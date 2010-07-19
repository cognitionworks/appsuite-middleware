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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.config;

import java.util.HashMap;
import java.util.Map;


/**
 * {@link ConfigTools} collect common parsing operations for configuration options.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ConfigTools {
    private static final Map<String, Long> UNITS = new HashMap<String, Long>() {{
        put("MS", 1l);
        put("S", 1000l);
        put("M", 1000*60l);
        put("H", 1000*60*60l);
        put("D", 1000*60*60*24l);
        put("W", 1000*60*60*24*7l);
    }};
    
    /**
     * A timespan specification consists of a number and a unit of measurement. Units are:
     * ms for miliseconds
     * s for seconds
     * m for minutes
     * h for hours
     * D for days
     * W for weeks
     * 
     * So, for example 2D 1h 12ms would be 2 days and one hour and 12 milliseconds  
     * @param span
     * @return
     */
    public static long parseTimespan(String span) {
        StringBuilder numberBuilder = new StringBuilder();
        StringBuilder unitBuilder = new StringBuilder();
        int mode = 0;
        long tally = 0;
        
        for(char c : span.toCharArray()) {
            if(Character.isDigit(c)) {
                if(mode == 0) {
                    numberBuilder.append(c);
                } else {
                    String unit = unitBuilder.toString().toUpperCase();
                    Long factor = UNITS.get(unit);
                    if(factor == null) {
                        throw new IllegalArgumentException("I don't know unit "+unit);
                    }
                    tally += Long.parseLong(numberBuilder.toString()) * factor;
                    numberBuilder.setLength(0);
                    unitBuilder.setLength(0);
                    mode = 0;
                    numberBuilder.append(c);
                }
            } else if(Character.isLetter(c)){
                mode = 1;
                unitBuilder.append(c);
            } else {
                // IGNORE
            }
        }
        if(numberBuilder.length() != 0) {
            String unit = unitBuilder.toString().toUpperCase();
            Long factor = UNITS.get(unit);
            if(factor == null) {
                throw new IllegalArgumentException("I don't know unit "+unit);
            }
            tally += Long.parseLong(numberBuilder.toString()) * factor;
        }
        return tally;
    }

}
