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

package com.openexchange.subscribe.google;

import static com.openexchange.java.Autoboxing.I;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.gdata.client.Query;
import com.google.gdata.client.contacts.ContactsService;
import com.google.gdata.data.contacts.ContactFeed;
import com.google.gdata.util.ServiceException;
import com.openexchange.exception.OXException;
import com.openexchange.google.api.client.GoogleApiClients;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.generic.FolderUpdaterRegistry;
import com.openexchange.groupware.generic.FolderUpdaterService;
import com.openexchange.log.LogProperties;
import com.openexchange.oauth.KnownApi;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.subscribe.Subscription;
import com.openexchange.subscribe.SubscriptionErrorMessage;
import com.openexchange.subscribe.google.parser.ContactParser;
import com.openexchange.subscribe.oauth.AbstractOAuthSubscribeService;
import com.openexchange.threadpool.AbstractTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.tools.iterator.SearchIteratorDelegator;

/**
 * {@link GoogleContactsSubscribeService}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public class GoogleContactsSubscribeService extends AbstractOAuthSubscribeService {

    /** The logger constant */
    static final Logger LOG = LoggerFactory.getLogger(GoogleContactsSubscribeService.class);

    public static final String SOURCE_ID = KnownApi.GOOGLE.getServiceId() + ".contact";

    /**
     * The Google Contacts' feed URL
     */
    private static final String FEED_URL = "https://www.google.com/m8/feeds/contacts/default/full";
    private static final String APP_NAME = "ox-appsuite";
    private static final int CHUNK_SIZE = 25;

    private final ServiceLookup services;

    /**
     * Initialises a new {@link GoogleContactsSubscribeService}.
     *
     * @param oAuthServiceMetaData The {@link OAuthServiceMetaData}
     * @param services The {@link ServiceLookup}
     * @throws OXException
     */
    public GoogleContactsSubscribeService(OAuthServiceMetaData oauthServiceMetadata, ServiceLookup services) throws OXException {
        super(oauthServiceMetadata, SOURCE_ID, FolderObject.CONTACT, "Google", services);
        this.services = services;

    }

    @Override
    public Collection<?> getContent(Subscription subscription) throws OXException {
        Session session = subscription.getSession();
        OAuthAccount oauthAccount = GoogleApiClients.reacquireIfExpired(session, true, getOAuthAccount(session, subscription));
        ContactsService googleContactsService = createContactsService(session, oauthAccount);
        ContactParser parser = new ContactParser(googleContactsService);

        try {
            Query cQuery = new Query(new URL(FEED_URL));
            cQuery.setMaxResults(CHUNK_SIZE);
            ContactFeed feed = googleContactsService.query(cQuery, ContactFeed.class);
            if (CHUNK_SIZE > feed.getTotalResults()) {
                List<Contact> contacts = parser.parseFeed(feed);
                LOG.debug("Parsed {} contacts for Google contact subscription for user {} in context {}", I(contacts.size()), I(subscription.getSession().getUserId()), I(subscription.getSession().getContextId()));
                return contacts;
            }

            List<Contact> firstBatch = parser.parseFeed(feed);
            int total = feed.getTotalResults();
            int startOffset = firstBatch.size();
            LOG.debug("Parsed first batch with size {} of {} contacts for Google contact subscription for user {} in context {}", I(firstBatch.size()), I(total), I(subscription.getSession().getUserId()), I(subscription.getSession().getContextId()));

            FolderUpdaterRegistry folderUpdaterRegistry = services.getOptionalService(FolderUpdaterRegistry.class);
            ThreadPoolService threadPool = services.getOptionalService(ThreadPoolService.class);
            FolderUpdaterService<Contact> folderUpdater = null == folderUpdaterRegistry ? null : folderUpdaterRegistry.<Contact> getFolderUpdater(subscription);
            if (null == threadPool || null == folderUpdater) {
                return fetchInForeground(cQuery, feed.getTotalResults(), firstBatch, googleContactsService, parser);
            }
            scheduleInBackground(subscription, cQuery, total, startOffset, threadPool, folderUpdater, googleContactsService, parser);
            return firstBatch;
        } catch (IOException e) {
            LOG.error("", e);
            throw SubscriptionErrorMessage.IO_ERROR.create(e, e.getMessage());
        } catch (ServiceException e) {
            String responseBody = e.getResponseBody();
            LOG.error(responseBody == null ? "" : responseBody, e);
            throw SubscriptionErrorMessage.COMMUNICATION_PROBLEM.create(e, e.getMessage());
        }
    }

    /**
     * Pings the contact service
     *
     * @throws OXException
     */
    public void ping(Session session, OAuthAccount account) throws OXException {
        try {
            Query cQuery = new Query(new URL(FEED_URL));
            cQuery.setMaxResults(1);
            createContactsService(session, account).query(cQuery, ContactFeed.class);
        } catch (IOException e) {
            LOG.error("", e);
            throw SubscriptionErrorMessage.IO_ERROR.create(e, e.getMessage());
        } catch (ServiceException e) {
            String responseBody = e.getResponseBody();
            LOG.error(responseBody == null ? "" : responseBody, e);
            throw SubscriptionErrorMessage.COMMUNICATION_PROBLEM.create(e, e.getMessage());
        }

    }

    /**
     * Creates a new {@link ContactsService} for the specified oauth account
     *
     * @param session The {@link Session}
     * @param oauthAccount The {@link OAuthAccount}
     * @return The {@link ContactsService}
     * @throws OXException if the {@link ContactsService} cannot be initialised
     */
    private ContactsService createContactsService(Session session, OAuthAccount oauthAccount) throws OXException {
        ContactsService googleContactsService = new ContactsService(APP_NAME);
        googleContactsService.setOAuth2Credentials(GoogleApiClients.getCredentials(oauthAccount, session));
        return googleContactsService;
    }

    /**
     * Fetches all contacts in the foreground (blocking thread)
     *
     * @param cQuery The {@link Query}
     * @param total The amount of all contacts
     * @param firstBatch The first batch of contacts
     * @return A {@link List} with all fetched contacts
     * @throws IOException if an I/O error is occurred
     * @throws ServiceException if a remote service error is occurred
     */
    private List<Contact> fetchInForeground(Query cQuery, int total, List<Contact> firstBatch, ContactsService googleContactsService, ContactParser parser) throws IOException, ServiceException {
        int offset = firstBatch.size();

        List<Contact> contacts = new ArrayList<>(total);
        contacts.addAll(firstBatch);

        while (total > offset) {
            cQuery.setStartIndex(offset);
            ContactFeed feed = googleContactsService.query(cQuery, ContactFeed.class);
            List<Contact> batch = parser.parseFeed(feed);
            contacts.addAll(batch);
            offset += batch.size();
        }

        return contacts;
    }

    /**
     * Schedules a task to fetch all contacts and executes it in the background
     *
     * @param subscription The {@link Subscription}
     * @param cQuery The {@link Query}
     * @param total The amount of all contacts
     * @param startOffset The stating 1-based index offset
     * @param threadPool the {@link ThreadPoolService}
     * @param folderUpdater The {@link FolderUpdaterService}
     * @param backgroundTaskMarker The background task marker
     */
    private void scheduleInBackground(Subscription subscription, Query cQuery, int total, int startOffset, ThreadPoolService threadPool, FolderUpdaterService<Contact> folderUpdater, ContactsService googleContactsService, ContactParser parser) {
        // Schedule task for remainder...
        threadPool.submit(new AbstractTask<Void>() {

            @Override
            public Void call() throws Exception {
                Integer iUserId = I(subscription.getSession().getUserId());
                Integer iContextId = I(subscription.getSession().getContextId());
                Integer iTotal = I(total);
                LogProperties.put(LogProperties.Name.SUBSCRIPTION_ADMIN, "true");
                try {
                    int offset = startOffset;
                    while (total > offset) {
                        cQuery.setStartIndex(offset);
                        List<Contact> batch = parser.parseFeed(googleContactsService.query(cQuery, ContactFeed.class));
                        folderUpdater.save(new SearchIteratorDelegator<>(batch), subscription);
                        offset += batch.size();
                        LOG.debug("Stored next batch with size {} ({} of {} contacts) for Google contact subscription for user {} in context {}", I(batch.size()), I(offset), iTotal, iUserId, iContextId);
                    }
                    LOG.debug("Finished storing {} contacts for Google contact subscription for user {} in context {}", iTotal, iUserId, iContextId);
                } catch (ServiceException e) {
                    String responseBody = e.getResponseBody();
                    if (responseBody == null) {
                        LOG.error("Failed storing {} contacts for Google contact subscription for user {} in context {}", iTotal, iUserId, iContextId, e);
                    } else {
                        LOG.error("Failed storing {} contacts for Google contact subscription for user {} in context {}: {}", iTotal, iUserId, iContextId, responseBody, e);
                    }
                } catch (Exception e) {
                    LOG.error("Failed storing {} contacts for Google contact subscription for user {} in context {}", iTotal, iUserId, iContextId, e);
                } finally {
                    LogProperties.remove(LogProperties.Name.SUBSCRIPTION_ADMIN);
                }
                return null;
            }
        });
    }

    @Override
    protected KnownApi getKnownApi() {
        return KnownApi.GOOGLE;
    }
}
