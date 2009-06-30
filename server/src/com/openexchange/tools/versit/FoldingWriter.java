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

package com.openexchange.tools.versit;

import java.io.IOException;
import java.io.Writer;

public class FoldingWriter implements VersitDefinition.Writer {

    private final Writer w;

    private int LineLength;
    
    private final int MAX = 75;
    
    private final String INDENTATION = " ";

    public FoldingWriter(final Writer w) {
        this.w = w;
    }

    public void sdfwrite(final String s) throws IOException {
        int start = 0, len = s.length() + LineLength;
        while (len > 75) {
            final int delta = 75 - LineLength;
            w.write(s.substring(start, start + delta));
            w.write("\n ");
            start += delta;
            len -= 75;
            LineLength = 1;
        }
        w.write(s.substring(start));
        LineLength += s.length() - start;
    }

    public void write(String s) throws IOException {
        if (s.length() > MAX) {
            writeLong(s);
        } else {
            w.write(s);
        }
    }
    
    private void writeLong(String s) throws IOException {
        w.write("\n");
        w.write(INDENTATION);
        if (s.length() > MAX) {
            w.write(s.substring(0, MAX - INDENTATION.length() - 2));
            writeLong(s.substring(MAX - INDENTATION.length() - 2, s.length()));
        } else {
            w.write(s);
        }
    }
    
    public void writeln() throws IOException {
        w.write("\n");
        LineLength = 0;
    }

    public void writeln(final String s) throws IOException {
        write(s);
        writeln();
    }

    public void flush() throws IOException {
        w.flush();
    }

    public void close() throws IOException {
        w.close();
    }

}
