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

package com.openexchange.marker;

import java.io.Closeable;
import com.openexchange.startup.impl.ThreadLocalCloseableControl;

/**
 * {@link OXThreadMarkers} - Utility class for {@link OXThreadMarker}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.1
 */
public class OXThreadMarkers {

    /**
     * Initializes a new {@link OXThreadMarkers}.
     */
    private OXThreadMarkers() {
        super();
    }

    /**
     * Checks if current thread is processing an HTTP request.
     *
     * @return <code>true</code> if is processing an HTTP request; otherwise <code>false</code>
     */
    public static boolean isHttpRequestProcessing() {
        return isHttpRequestProcessing(Thread.currentThread());
    }

    /**
     * Checks if specified thread is processing an HTTP request.
     *
     * @param t The thread to check
     * @return <code>true</code> if is processing an HTTP request; otherwise <code>false</code>
     */
    public static boolean isHttpRequestProcessing(Thread t) {
        return ((t instanceof OXThreadMarker) && ((OXThreadMarker) t).isHttpRequestProcessing());
    }

    /**
     * Remembers specified {@code Closeable} instance.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully added; otherwise <code>false</code>
     */
    public static boolean rememberCloseable(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if (t instanceof OXThreadMarker) {
            try {
                return ThreadLocalCloseableControl.getInstance().addCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    /**
     * Remembers specified {@code Closeable} instance if current thread is processing an HTTP request.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully added; otherwise <code>false</code>
     */
    public static boolean rememberCloseableIfHttpRequestProcessing(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if ((t instanceof OXThreadMarker) && ((OXThreadMarker) t).isHttpRequestProcessing()) {
            try {
                return ThreadLocalCloseableControl.getInstance().addCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

    /**
     * Un-Remembers specified {@code Closeable} instance.
     *
     * @param closeable The {@code Closeable} instance
     * @return <code>true</code> if successfully removed; otherwise <code>false</code>
     */
    public static boolean unrememberCloseable(Closeable closeable) {
        if (null == closeable) {
            return false;
        }

        Thread t = Thread.currentThread();
        if (t instanceof OXThreadMarker) {
            try {
                return ThreadLocalCloseableControl.getInstance().removeCloseable(closeable);
            } catch (Exception e) {
                // Ignore
            }
        }

        return false;
    }

}
