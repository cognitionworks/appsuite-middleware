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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.file.storage.dropbox;

import java.io.Serializable;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.datatypes.genericonf.DynamicFormDescription;
import com.openexchange.datatypes.genericonf.FormElement;
import com.openexchange.datatypes.genericonf.ReadOnlyDynamicFormDescription;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.AccountAware;
import com.openexchange.file.storage.CompositeFileStorageAccountManagerProvider;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageAccountManager;
import com.openexchange.file.storage.FileStorageAccountManagerLookupService;
import com.openexchange.file.storage.FileStorageAccountManagerProvider;
import com.openexchange.file.storage.generic.DefaultFileStorageAccount;
import com.openexchange.oauth.API;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthAccountDeleteListener;
import com.openexchange.oauth.OAuthUtilizerCreator;
import com.openexchange.session.Session;

/**
 * {@link DropboxFileStorageService} - The Dropbox file storage service.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DropboxFileStorageService implements AccountAware, OAuthUtilizerCreator, OAuthAccountDeleteListener {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(DropboxFileStorageService.class);

    private static final String SERVICE_ID = DropboxConstants.ID;

    /**
     * Creates a new Dropbox file storage service.
     *
     * @return A new Dropbox file storage service
     */
    public static DropboxFileStorageService newInstance() {
        return new DropboxFileStorageService();
    }

    /**
     * Creates a new Dropbox file storage service.
     *
     * @param compositeAccountManager The composite account manager
     * @return A new Dropbox file storage service
     */
    public static DropboxFileStorageService newInstance(final CompositeFileStorageAccountManagerProvider compositeAccountManager) {
        final DropboxFileStorageService newInst = new DropboxFileStorageService();
        newInst.applyCompositeAccountManager(compositeAccountManager);
        return newInst;
    }

    /**
     * The attribute expiration in millis.
     */
    public static final int DEFAULT_ATTR_EXPIRATION_PERIOD = 300000;

    private final DynamicFormDescription formDescription;
    private volatile FileStorageAccountManager accountManager;
    private volatile CompositeFileStorageAccountManagerProvider compositeAccountManager;

    /**
     * Initializes a new {@link DropboxFileStorageService}.
     */
    private DropboxFileStorageService() {
        super();
        final DynamicFormDescription tmpDescription = new DynamicFormDescription();
        /*
         * API & secret key
         */
        final FormElement oauthAccount = FormElement.custom("oauthAccount", "account", FormStrings.ACCOUNT_LABEL);
        oauthAccount.setOption("type", "com.openexchange.oauth.dropbox");
        tmpDescription.add(oauthAccount);
        formDescription = new ReadOnlyDynamicFormDescription(tmpDescription);
    }

    private FileStorageAccountManager getAccountManager0() throws OXException {
        FileStorageAccountManager m = accountManager;
        if (null == m) {
            synchronized (this) {
                m = accountManager;
                if (null == m) {
                    final FileStorageAccountManagerLookupService lookupService = DropboxServices.getService(FileStorageAccountManagerLookupService.class);
                    m = lookupService.getAccountManagerFor(SERVICE_ID);
                    accountManager = m;
                }
            }
        }
        return m;
    }

    private void applyCompositeAccountManager(final CompositeFileStorageAccountManagerProvider compositeAccountManager) {
        this.compositeAccountManager = compositeAccountManager;
    }

    /**
     * Gets the composite account manager.
     *
     * @return The composite account manager
     */
    public CompositeFileStorageAccountManagerProvider getCompositeAccountManager() {
        return compositeAccountManager;
    }

    // --------------------------------------------------------------------------------------------------------------------------------- //

    @Override
    public void onBeforeOAuthAccountDeletion(int oauthAccountId, Map<String, Object> eventProps, int user, int cid, Connection con) {
        // Nothing
    }

    @Override
    public void onAfterOAuthAccountDeletion(int oauthAccountId, Map<String, Object> eventProps, int user, int cid, Connection con) {
        try {
            // Acquire account manager
            FileStorageAccountManager accountManager = getAccountManager();

            List<FileStorageAccount> toDelete = new LinkedList<FileStorageAccount>();
            FakeSession session = new FakeSession(null, user, cid);
            for (FileStorageAccount account : accountManager.getAccounts(session)) {
                Object obj = account.getConfiguration().get("account");
                if (null != obj && Integer.toString(oauthAccountId).equals(obj.toString())) {
                    toDelete.add(account);
                }
            }

            for (FileStorageAccount deleteMe : toDelete) {
                accountManager.deleteAccount(deleteMe, session);
            }
        } catch (Exception e) {
            LOG.warn("Could not delete possibly existing Dropbox accounts associated with deleted OAuth account {} for user {} in context {}", oauthAccountId, user, cid, e);
        }
    }

    @Override
    public API getApplicableApi() {
        return API.DROPBOX;
    }

    @Override
    public String createUtilizer(OAuthAccount oauthAccount, Session session) throws OXException {
        if (false == API.DROPBOX.equals(oauthAccount.getAPI())) {
            return null;
        }

        if (false == getAccounts(session).isEmpty()) {
            return null;
        }

        // Acquire account manager
        FileStorageAccountManager accountManager = getAccountManager();

        // Create file storage account instance
        DefaultFileStorageAccount fileStorageAccount = new DefaultFileStorageAccount();
        fileStorageAccount.setDisplayName("Dropbox");
        fileStorageAccount.setFileStorageService(this);
        fileStorageAccount.setServiceId(SERVICE_ID);

        // Set its configuration
        Map<String, Object> configuration = new HashMap<String, Object>(2);
        configuration.put("account", Integer.toString(oauthAccount.getId()));
        fileStorageAccount.setConfiguration(configuration);

        // Add that account
        return accountManager.addAccount(fileStorageAccount, session);
    }

    // --------------------------------------------------------------------------------------------------------------------------------- //

    @Override
    public String getId() {
        return SERVICE_ID;
    }

    @Override
    public String getDisplayName() {
        return "Dropbox File Storage Service";
    }

    @Override
    public DynamicFormDescription getFormDescription() {
        return formDescription;
    }

    @Override
    public Set<String> getSecretProperties() {
        return Collections.emptySet();
    }

    private static final class FileStorageAccountInfo {
        protected final FileStorageAccount account;
        protected final int ranking;

        protected FileStorageAccountInfo(FileStorageAccount account, int ranking) {
            super();
            this.account = account;
            this.ranking = ranking;
        }
    }

    /**
     * Gets all service's accounts associated with session user.
     *
     * @param session The session providing needed user data
     * @return All accounts associated with session user.
     * @throws OXException If listing fails
     */
    @Override
    public List<FileStorageAccount> getAccounts(final Session session) throws OXException {
        final CompositeFileStorageAccountManagerProvider compositeAccountManager = this.compositeAccountManager;
        if (null == compositeAccountManager) {
            return getAccountManager0().getAccounts(session);
        }
        final Map<String, FileStorageAccountInfo> accountsMap = new LinkedHashMap<String, FileStorageAccountInfo>(8);
        for (final FileStorageAccountManagerProvider provider : compositeAccountManager.providers()) {
            for (final FileStorageAccount account : provider.getAccountManagerFor(SERVICE_ID).getAccounts(session)) {
                final FileStorageAccountInfo info = new FileStorageAccountInfo(account, provider.getRanking());
                final FileStorageAccountInfo prev = accountsMap.get(account.getId());
                if (null == prev || prev.ranking < info.ranking) {
                    // Replace with current
                    accountsMap.put(account.getId(), info);
                }
            }
        }
        final List<FileStorageAccount> ret = new ArrayList<FileStorageAccount>(accountsMap.size());
        for (final FileStorageAccountInfo info : accountsMap.values()) {
            ret.add(info.account);
        }
        return ret;
    }

    @Override
    public FileStorageAccountManager getAccountManager() throws OXException {
        final CompositeFileStorageAccountManagerProvider compositeAccountManager = this.compositeAccountManager;
        if (null == compositeAccountManager) {
            return getAccountManager0();
        }
        try {
            return compositeAccountManager.getAccountManagerFor(SERVICE_ID);
        } catch (final OXException e) {
            LOG.warn("", e);
            return getAccountManager0();
        }
    }

    @Override
    public FileStorageAccountAccess getAccountAccess(final String accountId, final Session session) throws OXException {
        final FileStorageAccount account;
        {
            final CompositeFileStorageAccountManagerProvider compositeAccountManager = this.compositeAccountManager;
            if (null == compositeAccountManager) {
                account = getAccountManager0().getAccount(accountId, session);
            } else {
                account = compositeAccountManager.getAccountManager(accountId, session).getAccount(accountId, session);
            }
        }
        return new DropboxAccountAccess(this, account, session);
    }

    // ------------------------------------------------------------------------------------------------------------------------- //

    private static final class FakeSession implements Session, Serializable {

        private static final long serialVersionUID = -4527564586038651789L;

        private final String password;
        private final int userId;
        private final int contextId;
        private final ConcurrentMap<String, Object> parameters;

        FakeSession(final String password, final int userId, final int contextId) {
            super();
            this.password = password;
            this.userId = userId;
            this.contextId = contextId;
            parameters = new ConcurrentHashMap<String, Object>(8);
        }

        @Override
        public int getContextId() {
            return contextId;
        }

        @Override
        public String getLocalIp() {
            return null;
        }

        @Override
        public void setLocalIp(final String ip) {
            // Nothing to do
        }

        @Override
        public String getLoginName() {
            return null;
        }

        @Override
        public boolean containsParameter(final String name) {
            return parameters.containsKey(name);
        }

        @Override
        public Object getParameter(final String name) {
            return parameters.get(name);
        }

        @Override
        public String getPassword() {
            return password;
        }

        @Override
        public String getRandomToken() {
            return null;
        }

        @Override
        public String getSecret() {
            return null;
        }

        @Override
        public String getSessionID() {
            return null;
        }

        @Override
        public int getUserId() {
            return userId;
        }

        @Override
        public String getUserlogin() {
            return null;
        }

        @Override
        public String getLogin() {
            return null;
        }

        @Override
        public void setParameter(final String name, final Object value) {
            if (null == value) {
                parameters.remove(name);
            } else {
                parameters.put(name, value);
            }
        }

        @Override
        public String getAuthId() {
            return null;
        }

        @Override
        public String getHash() {
            return null;
        }

        @Override
        public void setHash(final String hash) {
            // Nope
        }

        @Override
        public String getClient() {
            return null;
        }

        @Override
        public void setClient(final String client) {
            // Nothing to do
        }

        @Override
        public boolean isTransient() {
            return false;
        }

    }

}
