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

package com.openexchange.processing.internal;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;


/**
 * {@link ProcessorThreadFactory} - The thread factory for a processor.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.8.1
 */
public final class ProcessorThreadFactory implements ThreadFactory {

    private final AtomicInteger threadNumber;
    private final String namePrefix;
    private final boolean supportMDC;

    /**
     * Initializes a new {@link ProcessorThreadFactory}.
     *
     * @param name The name prefix; e.g. <code>"MyThread"</code>
     * @param supportMDC Whether to support MDC log properties; otherwise MDC will be cleared prior to each log output
     */
    public ProcessorThreadFactory(String name, boolean supportMDC) {
        super();
        this.supportMDC = supportMDC;
        threadNumber = new AtomicInteger();
        this.namePrefix = null == name ? "ProcessorThread-" : new StringBuilder(name).append('-').toString();
    }

    @Override
    public Thread newThread(Runnable r) {
        final Thread t = supportMDC ? new PseudoOXThread(r, getThreadName(getThreadNumber(), namePrefix)) : new Thread(r, getThreadName(getThreadNumber(), namePrefix));
        t.setUncaughtExceptionHandler(ProcessorUncaughtExceptionhandler.getInstance());
        return t;
    }

    private int getThreadNumber() {
        int number;
        do {
            number = threadNumber.incrementAndGet();
            if (number > 0) {
                return number;
            }
        } while (!threadNumber.compareAndSet(number, 0));
        return threadNumber.incrementAndGet();
    }

    private static String getThreadName(int threadNumber, String namePrefix) {
        StringBuilder retval = new StringBuilder(namePrefix.length() + 7);
        retval.append(namePrefix);
        for (int i = threadNumber; i < 1000000; i *= 10) {
            retval.append('0');
        }
        retval.append(threadNumber);
        return retval.toString();
    }

}
