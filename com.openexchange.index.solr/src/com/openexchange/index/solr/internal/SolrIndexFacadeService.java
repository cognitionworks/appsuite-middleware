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

package com.openexchange.index.solr.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.logging.Log;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.index.IndexAccess;
import com.openexchange.index.IndexExceptionCodes;
import com.openexchange.index.IndexFacadeService;
import com.openexchange.index.IndexManagementService;
import com.openexchange.index.solr.SolrIndexExceptionCodes;
import com.openexchange.index.solr.internal.attachments.SolrAttachmentIndexAccess;
import com.openexchange.index.solr.internal.infostore.SolrInfostoreIndexAccess;
import com.openexchange.index.solr.internal.mail.SolrMailIndexAccess;
import com.openexchange.log.LogFactory;
import com.openexchange.session.Session;
import com.openexchange.solr.SolrCoreIdentifier;
import com.openexchange.timer.ScheduledTimerTask;
import com.openexchange.timer.TimerService;

/**
 * {@link SolrIndexFacadeService} - The Solr {@link IndexFacadeService} implementation.
 * 
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class SolrIndexFacadeService implements IndexFacadeService {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SolrIndexFacadeService.class));

    private final ConcurrentHashMap<SolrCoreIdentifier, AbstractSolrIndexAccess<?>> accessMap;

    /**
     * Timeout in minutes. An index access will be released after being unused for this time and if it isn't referenced anymore.
     */
    private static final long SOFT_TIMEOUT = 10;

    /**
     * Timeout in minutes. An index access will be released after being unused for this time whether it's still referenced or not.
     */
    private static final long HARD_TIMEOUT = 60;

    private ScheduledTimerTask timerTask;

    /**
     * Initializes a new {@link SolrIndexFacadeService}.
     */
    public SolrIndexFacadeService() {
        super();
        accessMap = new ConcurrentHashMap<SolrCoreIdentifier, AbstractSolrIndexAccess<?>>();
    }

    public void init() {
        TimerService timerService = Services.getService(TimerService.class);
        timerTask = timerService.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                try {
                    List<AbstractSolrIndexAccess<?>> accessList = getCachedAccesses();
                    List<SolrCoreIdentifier> identifiers = new ArrayList<SolrCoreIdentifier>();
                    long now = System.currentTimeMillis();
                    long softBarrier = now - (TimeUnit.MINUTES.toMillis(SOFT_TIMEOUT));
                    long hardBarrier = now - (TimeUnit.MINUTES.toMillis(HARD_TIMEOUT));
                    for (AbstractSolrIndexAccess<?> access : accessList) {
                        long lastAccess = access.getLastAccess();
                        if ((lastAccess < softBarrier && !access.isRetained()) || lastAccess < hardBarrier) {
                            identifiers.add(access.getIdentifier());
                            access.releaseCore();
                        }
                    }

                    removeFromCache(identifiers);
                    if (LOG.isDebugEnabled() && !identifiers.isEmpty()) {
                        StringBuilder sb = new StringBuilder("Removed IndexAccesses:\n");
                        for (SolrCoreIdentifier identifier : identifiers) {
                            sb.append("    ");
                            sb.append(identifier.toString());
                            sb.append("\n");
                        }
                        LOG.debug(sb.toString());
                    }
                } catch (Throwable e) {
                    LOG.error("Exception during timer task execution: " + e.getMessage(), e);
                }

            }
        }, SOFT_TIMEOUT, SOFT_TIMEOUT, TimeUnit.MINUTES);
    }

    public void shutDown() {
        if (timerTask != null) {
            timerTask.cancel();
        }

        for (AbstractSolrIndexAccess<?> access : accessMap.values()) {
            access.releaseCore();
        }
    }

    @SuppressWarnings("unchecked")
	@Override
    public <V> IndexAccess<V> acquireIndexAccess(final int module, final int userId, final int contextId) throws OXException {
        IndexManagementService managementService = Services.getService(IndexManagementService.class);
        if (managementService.isLocked(contextId, userId, module)) {
            throw IndexExceptionCodes.INDEX_LOCKED.create(module, userId, contextId);
        }
        
        SolrCoreIdentifier identifier = new SolrCoreIdentifier(contextId, userId, module);
        AbstractSolrIndexAccess<?> cachedIndexAccess = accessMap.get(identifier);
        if (null == cachedIndexAccess) {
            AbstractSolrIndexAccess<?> newAccess = createIndexAccessByType(identifier);
            cachedIndexAccess = accessMap.putIfAbsent(identifier, newAccess);
            if (null == cachedIndexAccess) {
                cachedIndexAccess = newAccess;
            }
        }

        cachedIndexAccess.incrementRetainCount();
        return (IndexAccess<V>) cachedIndexAccess;
    }

    @Override
    public <V> IndexAccess<V> acquireIndexAccess(final int module, final Session session) throws OXException {
        return acquireIndexAccess(module, session.getUserId(), session.getContextId());
    }

    @Override
    public void releaseIndexAccess(final IndexAccess<?> indexAccess) throws OXException {
        AbstractSolrIndexAccess<?> cachedIndexAccess = accessMap.get(((AbstractSolrIndexAccess<?>) indexAccess).getIdentifier());
        if (null != cachedIndexAccess) {
            cachedIndexAccess.decrementRetainCount();
        }
    }

    private List<AbstractSolrIndexAccess<?>> getCachedAccesses() {
        List<AbstractSolrIndexAccess<?>> accessList = new ArrayList<AbstractSolrIndexAccess<?>>();
        for (AbstractSolrIndexAccess<?> access : accessMap.values()) {
            accessList.add(access);
        }

        return accessList;
    }

    private void removeFromCache(final List<SolrCoreIdentifier> identifiers) {
        for (final SolrCoreIdentifier identifier : identifiers) {
            accessMap.remove(identifier);
        }
    }

    private AbstractSolrIndexAccess<?> createIndexAccessByType(final SolrCoreIdentifier identifier) throws OXException {
        final int module = identifier.getModule();
        // TODO: Add other modules
        switch (module) {
            case Types.EMAIL:
                return new SolrMailIndexAccess(identifier);

            case Types.INFOSTORE:
                return new SolrInfostoreIndexAccess(identifier);

            case Types.ATTACHMENT:
                return new SolrAttachmentIndexAccess(identifier);

            default:
                throw SolrIndexExceptionCodes.MISSING_ACCESS_FOR_MODULE.create(module);

        }
    }
}
