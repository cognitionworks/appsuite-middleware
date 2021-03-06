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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.jsieve.export.utils;

import java.io.IOException;

/** An I/O result */
public class IOResult<V> {

    private static final IOResult<?> EMPTY = new IOResult<>(null, null);

    /**
     * Creates a success result for result instance.
     *
     * @param <V> The result type
     * @param result The result to propagate
     * @return The success result
     */
    static <V> IOResult<V> resultFor(V result) {
        if (result == null) {
            @SuppressWarnings("unchecked")
            IOResult<V> ior = (IOResult<V>) EMPTY;
            return ior;
        }
        return new IOResult<V>(result, null);
    }

    /**
     * Creates an error result for given I/O error.
     *
     * @param <V> The result type
     * @param ioError The I/O error
     * @return The error result
     */
    static <V> IOResult<V> errorFor(IOException ioError) {
        if (ioError == null) {
            throw new IllegalArgumentException("I/O error must not be null");
        }
        return new IOResult<V>(null, ioError);
    }

    // -------------------------------------------------------------------------------------------------------

    private final V result;
    private final IOException ioError;

    /**
     * Initializes a new {@link IOResult}.
     *
     * @param result The result or <code>null</code>
     * @param ioError The I/O error or <code>null</code>
     */
    private IOResult(V result, IOException ioError) {
        super();
        this.result = result;
        this.ioError = ioError;
    }

    /**
     * Gets the result or <code>null</code> if an I/O error is available.
     *
     * @return The result or <code>null</code>
     */
    public V getResult() {
        return result;
    }

    /**
     * Gets the checked result.
     *
     * @return The result
     * @throws IOException If there is {@link #getIoError() an I/O error available}
     */
    public V getCheckedResult() throws IOException {
        if (ioError != null) {
            throw ioError;
        }
        return result;
    }

    /**
     * Gets the optional I/O error
     *
     * @return The I/O error or <code>null</code>
     */
    public IOException getIoError() {
        return ioError;
    }
}