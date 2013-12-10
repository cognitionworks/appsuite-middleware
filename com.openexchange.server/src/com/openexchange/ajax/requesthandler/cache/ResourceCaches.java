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

package com.openexchange.ajax.requesthandler.cache;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.java.StringAllocator;
import com.openexchange.java.Strings;

/**
 * {@link ResourceCaches} - Utility class for resource cache.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class ResourceCaches {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(ResourceCaches.class);

    /**
     * Initializes a new {@link ResourceCaches}.
     */
    private ResourceCaches() {
        super();
    }

    private static final AtomicReference<ResourceCache> CACHE_REF = new AtomicReference<ResourceCache>();

    /**
     * Sets the resource cache reference.
     *
     * @param ref The reference
     */
    public static void setResourceCache(final ResourceCache ref) {
        CACHE_REF.set(ref);
    }

    /**
     * Gets the resource cache reference.
     *
     * @return The preview cache or <code>null</code> if absent
     */
    public static ResourceCache getResourceCache() {
        return CACHE_REF.get();
    }

    /**
     * Generates the key for preview cache.
     *
     * @param eTag The ETag identifier
     * @param requestData The request data
     * @param optParameters Optional parameters to consider
     * @return The appropriate cache key
     */
    public static String generatePreviewCacheKey(final String eTag, final AJAXRequestData requestData) {
        final StringAllocator sb = new StringAllocator(512);
        sb.append(requestData.getModule());
        sb.append('-').append(requestData.getAction());
        sb.append('-').append(requestData.getSession().getContextId());
        // Append sorted parameters
        {
            final List<String> parameters = new ArrayList<String>(requestData.getParameters().keySet());
            Collections.sort(parameters);
            for (final String name : parameters) {
                if (isAcceptableParameter(name)) {
                    sb.append('-').append(name);
                    final String parameter = requestData.getParameter(name);
                    if (!Strings.isEmpty(parameter)) {
                        sb.append('=').append(parameter);
                    }
                }
            }
        }
        // Generate MD5 sum
        try {
            final byte[] md5Bytes = sb.toString().getBytes("UTF-8");
            sb.setNewLength(0);
            return sb.append(eTag).append('-').append(asHex(MessageDigest.getInstance("MD5").digest(md5Bytes))).toString();
        } catch (final UnsupportedEncodingException e) {
            // Shouldn't happen
            LOG.error("", e);
        } catch (final NoSuchAlgorithmException e) {
            // Shouldn't happen
            LOG.error("", e);
        }
        return sb.toString();
    }

    private static boolean isAcceptableParameter(final String name) {
        if (Strings.isEmpty(name)) {
            return false;
        }
        final String n = Strings.toLowerCase(name);
        return !"session".equals(n) && !"action".equals(n);
    }

    private static final char[] HEX_CHARS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    /**
     * Turns array of bytes into string representing each byte as unsigned hex number.
     *
     * @param hash Array of bytes to convert to hex-string
     * @return Generated hex string
     */
    public static String asHex(final byte[] hash) {
        final int length = hash.length;
        final char[] buf = new char[length * 2];
        for (int i = 0, x = 0; i < length; i++) {
            buf[x++] = HEX_CHARS[(hash[i] >>> 4) & 0xf];
            buf[x++] = HEX_CHARS[hash[i] & 0xf];
        }
        return new String(buf);
    }

}
