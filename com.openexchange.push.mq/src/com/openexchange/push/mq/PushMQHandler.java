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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.push.mq;

import static com.openexchange.java.Autoboxing.I2i;
import static com.openexchange.java.Autoboxing.i;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.apache.commons.logging.Log;
import com.openexchange.log.LogFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import com.openexchange.context.ContextService;
import com.openexchange.event.CommonEvent;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.push.mq.registry.PushMQServiceRegistry;

/**
 * {@link PushMQHandler}
 * 
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 */
public class PushMQHandler implements EventHandler {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(PushMQHandler.class));

    private final PushMQPublisher publisher;

    /**
     * Initializes a new {@link PushMQHandler}.
     * 
     * @throws OXException
     */
    public PushMQHandler(PushMQPublisher publisher) throws OXException {
        super();
        this.publisher = publisher;
    }

    /*
     * (non-Javadoc)
     * @see org.osgi.service.event.EventHandler#handleEvent(org.osgi.service.event.Event)
     */
    @Override
    public void handleEvent(Event e) {
        final CommonEvent event;
        {
            final Object obj = e.getProperty(CommonEvent.EVENT_KEY);
            if (obj == null) {
                return;
            }
            try {
                event = (CommonEvent) obj;
            } catch (final ClassCastException cce) {
                LOG.warn("Unexpected type: " + cce.getMessage(), cce);
                return;
            }
        }

        final int contextId = event.getContextId();
        final Context ctx;
        try {
            final ContextService contextService = PushMQServiceRegistry.getServiceRegistry().getService(ContextService.class);
            ctx = contextService.getContext(contextId);
        } catch (final OXException exc) {
            LOG.error("cannot resolve context id: " + contextId, exc);
            return;
        }

        final int module = event.getModule();

        Object tmp = event.getSourceFolder();
        final FolderObject parentFolder;
        if (tmp instanceof FolderObject) {
            parentFolder = (FolderObject) event.getSourceFolder();
        } else {
            parentFolder = null;
        }
        if (parentFolder == null && module != Types.EMAIL) {
            LOG.warn("folder object in event is null");
            return;
        }
        switch (module) {
        case Types.APPOINTMENT:
        case Types.TASK:
        case Types.CONTACT:
        case Types.FOLDER:
            for (final Entry<Integer, Set<Integer>> entry : transform(event.getAffectedUsersWithFolder()).entrySet()) {
                event(i(entry.getKey()), I2i(entry.getValue()), module, ctx, getTimestamp((DataObject) event.getActionObj()), e);
            }
            break;
        case Types.EMAIL:
            event(1, new int[] { event.getUserId() }, module, ctx, 0, e);
            break;
        case Types.INFOSTORE:
            for (final Entry<Integer, Set<Integer>> entry : transform(event.getAffectedUsersWithFolder()).entrySet()) {
                event(
                    i(entry.getKey()),
                    I2i(entry.getValue()),
                    module,
                    ctx,
                    getTimestamp(((DocumentMetadata) event.getActionObj()).getLastModified()),
                    e);
            }
            break;
        default:
            LOG.warn("Got event with unimplemented module: " + module);
        }
    }

    private void event(final int folderId, final int[] users, final int module, final Context ctx, final long timestamp, final Event e) {
        if (users == null) {
            return;
        }
        try {
            final PushMQObject pushObject = new PushMQObject(folderId, module, ctx.getContextId(), users, false, timestamp, e.getTopic());
            publisher.publishMQObject(pushObject);
        } catch (OXException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }

    private static long getTimestamp(final Date lastModified) {
        return lastModified == null ? 0 : lastModified.getTime();
    }

    private static long getTimestamp(final DataObject object) {
        return null == object ? 0 : getTimestamp(object.getLastModified());
    }

    private static final Map<Integer, Set<Integer>> transform(final Map<Integer, Set<Integer>> map) {
        final Map<Integer, Set<Integer>> retval = new HashMap<Integer, Set<Integer>>();
        for (final Entry<Integer, Set<Integer>> entry : map.entrySet()) {
            for (final Integer folderId : entry.getValue()) {
                Set<Integer> users = retval.get(folderId);
                if (null == users) {
                    users = new HashSet<Integer>();
                    retval.put(folderId, users);
                }
                users.add(entry.getKey());
            }
        }
        return retval;
    }

}
