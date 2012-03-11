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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.messaging.registry.osgi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.messaging.MessagingException;
import com.openexchange.messaging.MessagingExceptionCodes;
import com.openexchange.messaging.MessagingService;
import com.openexchange.messaging.registry.MessagingServiceRegistry;


/**
 * {@link OSGIMessagingServiceRegistry}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class OSGIMessagingServiceRegistry implements MessagingServiceRegistry {

    private BundleContext context;
    private ServiceTracker tracker;

    public OSGIMessagingServiceRegistry(BundleContext context) {
        this.context = context;
    }
    
    public void start() {
        this.tracker = new ServiceTracker(context, MessagingService.class.getName(), null);
    }
    
    public void stop() {
        this.tracker.close();
    }
    
    public List<MessagingService> getAllServices() throws MessagingException {
        Object[] services = tracker.getServices();
        if(null == services) {
            return Collections.emptyList();
        }
        List<MessagingService> messagingServices = new ArrayList<MessagingService>(services.length);
        for (Object object : services) {
            messagingServices.add((MessagingService) object);
        }
        return messagingServices;
    }

    public MessagingService getMessagingService(String id) throws MessagingException {
        for (MessagingService messagingService : getAllServices()) {
            if(messagingService.getId().equals(id)) {
                return messagingService;
            }
        }
        throw MessagingExceptionCodes.UNKNOWN_MESSAGING_SERVICE.create(id);
    }

}
