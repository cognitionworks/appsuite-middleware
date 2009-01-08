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

package com.openexchange.spamhandler.spamassassin.osgi;

import java.util.Dictionary;
import java.util.Hashtable;
import java.util.concurrent.atomic.AtomicBoolean;
import org.osgi.framework.ServiceRegistration;
import com.openexchange.server.osgiservice.DeferredActivator;
import com.openexchange.spamhandler.SpamHandler;
import com.openexchange.spamhandler.spamassassin.SpamAssassinSpamHandler;

/**
 * {@link SpamAssassinSpamHandlerActivator}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SpamAssassinSpamHandlerActivator extends DeferredActivator {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(SpamAssassinSpamHandlerActivator.class);

    private static final Class<?>[] NEEDED_SERVICES = new Class<?>[0];

    private final AtomicBoolean started;

    private final Dictionary<String, String> dictionary;

    private ServiceRegistration serviceRegistration;

    /**
     * Initializes a new {@link SpamAssassinSpamHandlerActivator}
     */
    public SpamAssassinSpamHandlerActivator() {
        super();
        started = new AtomicBoolean();
        dictionary = new Hashtable<String, String>();
        dictionary.put("name", SpamAssassinSpamHandler.getInstance().getSpamHandlerName());
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.server.osgiservice.DeferredActivator#getNeededServices()
     */
    @Override
    protected Class<?>[] getNeededServices() {
        return NEEDED_SERVICES;
    }

    @Override
    protected void handleUnavailability(final Class<?> clazz) {
    }

    @Override
    protected void handleAvailability(final Class<?> clazz) {
    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.server.osgiservice.DeferredActivator#startBundle()
     */
    @Override
    protected void startBundle() throws Exception {
        try {
            if (!started.compareAndSet(false, true)) {
                /*
                 * Don't start the server again. A duplicate call to startBundle() is probably caused by temporary absent service(s) whose
                 * re-availability causes to trigger this method again.
                 */
                LOG.info("A temporary absent service is available again");
                return;
            }
            serviceRegistration = context.registerService(SpamHandler.class.getName(), SpamAssassinSpamHandler.getInstance(), dictionary);
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        }

    }

    /*
     * (non-Javadoc)
     * @see com.openexchange.server.osgiservice.DeferredActivator#stopBundle()
     */
    @Override
    protected void stopBundle() throws Exception {
        try {
            if (null != serviceRegistration) {
                serviceRegistration.unregister();
                serviceRegistration = null;
            }
        } catch (final Throwable t) {
            LOG.error(t.getMessage(), t);
            throw t instanceof Exception ? (Exception) t : new Exception(t);
        } finally {
            started.set(false);
        }

    }

}
