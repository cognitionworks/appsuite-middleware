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

import static com.openexchange.exception.ExceptionUtils.isEitherOf;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import com.google.common.collect.ImmutableList;


/**
 * {@link NetworkCommunicationErrorAdvertisingCallable} - A special {@link Callable} calling an I/O operation, which only throws such
 * {@link IOException} that are considered as network communication error. Otherwise an instance of {@link IOResult} is returned that might
 * advertise an {@link IOException} as well.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.3
 */
public abstract class NetworkCommunicationErrorAdvertisingCallable<V> implements Callable<IOResult<V>> {

    private static final List<Class<? extends Exception>> NETWORK_COMMUNICATION_ERRORS = ImmutableList.of(
        java.net.SocketTimeoutException.class,
        java.io.EOFException.class);

    /**
     * Initializes a new {@link NetworkCommunicationErrorAdvertisingCallable}.
     */
    protected NetworkCommunicationErrorAdvertisingCallable() {
        super();
    }

    @Override
    public final IOResult<V> call() throws Exception {
        try {
            return IOResult.resultFor(performIOOperation());
        } catch (IOException e) {
            if (isEitherOf(e, NETWORK_COMMUNICATION_ERRORS)) {
                throw e;
            }
            return IOResult.errorFor(e);
        }
    }

    /**
     * Performs the I/O operation
     *
     * @return The result
     * @throws IOException An I/O error
     */
    protected abstract V performIOOperation() throws IOException;

}
