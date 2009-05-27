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

package com.openexchange.publish.osgi;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.publish.PublicationException;
import com.openexchange.publish.PublicationService;
import com.openexchange.publish.PublicationTarget;
import com.openexchange.publish.PublicationTargetDiscoveryService;
import com.openexchange.publish.tools.PublicationTargetCollector;


/**
 * {@link OSGiPublicationTargetCollector}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class OSGiPublicationTargetCollector implements ServiceTrackerCustomizer, PublicationTargetDiscoveryService {

    private static Log LOG = LogFactory.getLog(OSGiPublicationTargetCollector.class);
    
    private BundleContext context;
    private ServiceTracker tracker;
    
    private PublicationTargetCollector delegate = new PublicationTargetCollector();
    
    private List<ServiceReference> references = new LinkedList<ServiceReference>();
    private boolean grabbedAll = false;
    
    public OSGiPublicationTargetCollector(BundleContext context) {
        this.context = context;
        this.tracker = new ServiceTracker(context, PublicationService.class.getName(), this);
    }
    
    public void close() {
        delegate.clear();
        for(ServiceReference reference : references) {
            context.ungetService(reference);
        }
        this.tracker.close();
        grabbedAll = false;
    }
    
    public Object addingService(ServiceReference reference) {
        try {
            return add(reference);
        } catch (PublicationException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }

    public void modifiedService(ServiceReference reference, Object service) {
        
    }

    public void removedService(ServiceReference reference, Object service) {
        try {
            remove(reference, service);
        } catch (PublicationException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    private void grabAll() throws PublicationException {
        grabbedAll = true;
        try {
            ServiceReference[] refs = context.getAllServiceReferences(PublicationService.class.getName(), null);
            if(refs == null) {
                return;
            }
            for(ServiceReference reference : refs) {
                add(reference);
            }
        
        } catch (InvalidSyntaxException e) {
            // Won't happen, we don't use filters;
        }
        
    }
    
    private void remove(ServiceReference reference, Object service) throws PublicationException {
        references.remove(reference);
        context.ungetService(reference);
        delegate.removePublicationService((PublicationService) service);
    }

    private PublicationService add(ServiceReference reference) throws PublicationException {
        references.add(reference);
        PublicationService publisher = (PublicationService) context.getService(reference);
        delegate.addPublicationService(publisher);
        return publisher;
    }

    public PublicationTarget getTarget(Context context, int publicationId) throws PublicationException {
        if(!grabbedAll) {
            grabAll();
        }
        return delegate.getTarget(context, publicationId);
    }

    public PublicationTarget getTarget(String id) throws PublicationException {
        if(!grabbedAll) {
            grabAll();
        }
        return delegate.getTarget(id);
    }

    public Collection<PublicationTarget> getTargetsForEntityType(String module) throws PublicationException {
        if(!grabbedAll) {
            grabAll();
        }
        return delegate.getTargetsForEntityType(module);
    }

    public boolean knows(String id) throws PublicationException {
        if(!grabbedAll) {
            grabAll();
        }
        return delegate.knows(id);
    }

    public Collection<PublicationTarget> listTargets() throws PublicationException {
        if(!grabbedAll) {
            grabAll();
        }
        return delegate.listTargets();
    }
    
    
}

