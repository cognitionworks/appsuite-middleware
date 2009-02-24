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

package com.openexchange.charset;

import java.nio.charset.Charset;
import java.nio.charset.IllegalCharsetNameException;
import java.nio.charset.UnsupportedCharsetException;
import java.nio.charset.spi.CharsetProvider;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * {@link AliasCharsetProvider}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class AliasCharsetProvider extends CharsetProvider {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AliasCharsetProvider.class);

    private static Map<String, Charset> name2charset;

    /**
     * Default constructor
     */
    public AliasCharsetProvider() {
        super();
    }

    /**
     * Retrieves a charset for the given charset name. </p>
     * 
     * @param charsetName The name of the requested charset; may be either a canonical name or an alias
     * @return A charset object for the named charset, or <tt>null</tt> if the named charset is not supported by this provider
     */
    @Override
    public Charset charsetForName(final String charsetName) {
        /*
         * Get charset instance for given name (case insensitive)
         */
        return name2charset.get(charsetName.toLowerCase());
    }

    /**
     * Creates an iterator that iterates over the charsets supported by this provider. This method is used in the implementation of the
     * {@link java.nio.charset.Charset#availableCharsets Charset.availableCharsets} method. </p>
     * 
     * @return The new iterator
     */
    @Override
    public Iterator<Charset> charsets() {
        return name2charset.values().iterator();
    }

    static {
        /*
         * Prepare supported charsets
         */
        Charset macRoman = null;
        try {
            macRoman = Charset.forName("MacRoman");
        } catch (final IllegalCharsetNameException e) {
            // Cannot occur
            LOG.warn("Illegal charset name \"" + e.getCharsetName() + "\".");
        } catch (final UnsupportedCharsetException e) {
            LOG.warn("Detected no support for charset \"MacRoman\".");
        }

        final List<Charset> cs = new ArrayList<Charset>(8);
        cs.add(new AliasCharset("BIG-5", new String[] { "BIG_5" }, Charset.forName("BIG5")));
        cs.add(new AliasCharset("UTF_8", null, Charset.forName("UTF-8")));
        cs.add(new AliasCharset("x-unknown", null, Charset.forName("US-ASCII")));
        cs.add(new AliasCharset("ISO", null, Charset.forName("ISO-8859-1")));
        if (null != macRoman) {
            cs.add(new AliasCharset("MACINTOSH", null, macRoman));
        }
        final Map<String, Charset> n2c = new HashMap<String, Charset>();
        final int size = cs.size();
        for (int i = 0; i < size; i++) {
            final Charset c = cs.get(i);
            n2c.put(c.name().toLowerCase(), c);
        }
        name2charset = Collections.unmodifiableMap(n2c);
    }
}
