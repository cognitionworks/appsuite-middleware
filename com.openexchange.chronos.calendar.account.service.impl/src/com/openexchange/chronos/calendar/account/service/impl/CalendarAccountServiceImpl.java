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

package com.openexchange.chronos.calendar.account.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.exception.CalendarExceptionCodes;
import com.openexchange.chronos.provider.AutoProvisioningCalendarProvider;
import com.openexchange.chronos.provider.CalendarAccount;
import com.openexchange.chronos.provider.CalendarProvider;
import com.openexchange.chronos.provider.CalendarProviderRegistry;
import com.openexchange.chronos.provider.DefaultCalendarAccount;
import com.openexchange.chronos.provider.SingleAccountCalendarProvider;
import com.openexchange.chronos.provider.account.AdministrativeCalendarAccountService;
import com.openexchange.chronos.provider.account.CalendarAccountService;
import com.openexchange.chronos.service.CalendarParameters;
import com.openexchange.chronos.storage.CalendarAccountStorage;
import com.openexchange.chronos.storage.CalendarStorage;
import com.openexchange.chronos.storage.operation.OSGiCalendarStorageOperation;
import com.openexchange.exception.OXException;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link CalendarAccountServiceImpl}
 *
 * @author <a href="mailto:Jan-Oliver.Huhn@open-xchange.com">Jan-Oliver Huhn</a>
 * @since v7.10.0
 */
public class CalendarAccountServiceImpl implements CalendarAccountService, AdministrativeCalendarAccountService {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link CalendarAccountServiceImpl}.
     *
     * @param serviceLookup A service lookup reference
     */
    public CalendarAccountServiceImpl(ServiceLookup serviceLookup) {
        super();
        this.services = serviceLookup;
    }

    @Override
    public List<CalendarProvider> getProviders() throws OXException {
        return getProviderRegistry().getCalendarProviders();
    }

    @Override
    public CalendarAccount createAccount(Session session, String providerId, JSONObject userConfig, CalendarParameters parameters) throws OXException {
        if (isGuest(session)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(providerId);
        }
        /*
         * get associated calendar provider & initialize account config
         */
        CalendarProvider calendarProvider = getProvider(providerId);
        JSONObject internalConfig = calendarProvider.configureAccount(session, userConfig, parameters);
        /*
         * insert calendar account in storage within transaction
         */
        CalendarAccount account = insertAccount(session.getContextId(), calendarProvider, session.getUserId(), internalConfig, userConfig);
        /*
         * let provider perform any additional initialization
         */
        calendarProvider.onAccountCreated(session, account, parameters);
        return account;
    }

    @Override
    public CalendarAccount updateAccount(Session session, int id, Boolean enabled, JSONObject userConfig, long clientTimestamp, CalendarParameters parameters) throws OXException {
        /*
         * get & check stored calendar account
         */
        CalendarAccount storedAccount = getAccount(session, id);
        if (null != storedAccount.getLastModified() && storedAccount.getLastModified().getTime() > clientTimestamp) {
            throw CalendarExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(id), clientTimestamp, storedAccount.getLastModified().getTime());
        }
        if (isGuest(session)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(storedAccount.getProviderId());
        }
        /*
         * get associated calendar provider & re-initialize account config
         */
        CalendarProvider calendarProvider = getProvider(storedAccount.getProviderId());

        JSONObject internalConfig = calendarProvider.reconfigureAccount(session, storedAccount, userConfig, parameters);
        /*
         * update calendar account in storage within transaction
         */
        CalendarAccount account = updateAccount(session.getContextId(), session.getUserId(), id, enabled, internalConfig, userConfig, clientTimestamp);
        /*
         * let provider perform any additional initialization
         */
        calendarProvider.onAccountUpdated(session, account, parameters);
        return account;
    }

    @Override
    public void deleteAccount(Session session, int id, long timestamp, CalendarParameters parameters) throws OXException {
        /*
         * get & check stored calendar account
         */
        CalendarAccount storedAccount = new OSGiCalendarStorageOperation<CalendarAccount>(services, session.getContextId(), -1) {

            @Override
            protected CalendarAccount call(CalendarStorage storage) throws OXException {
                return storage.getAccountStorage().loadAccount(session.getUserId(), id);
            }
        }.executeQuery();
        if (null == storedAccount) {
            throw CalendarExceptionCodes.ACCOUNT_NOT_FOUND.create(id);
        }
        if (null != storedAccount.getLastModified() && storedAccount.getLastModified().getTime() > timestamp) {
            throw CalendarExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(id), timestamp, storedAccount.getLastModified().getTime());
        }
        CalendarProvider calendarProvider = getProviderRegistry().getCalendarProvider(storedAccount.getProviderId());
        if (null != calendarProvider && AutoProvisioningCalendarProvider.class.isInstance(calendarProvider)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(calendarProvider.getId());
        }
        if (isGuest(session)) {
            throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(storedAccount.getProviderId());
        }
        /*
         * delete calendar account in storage within transaction
         */
        new OSGiCalendarStorageOperation<Void>(services, session.getContextId(), -1) {

            @Override
            protected Void call(CalendarStorage storage) throws OXException {
                CalendarAccount account = storage.getAccountStorage().loadAccount(session.getUserId(), id);
                if (null != account.getLastModified() && account.getLastModified().getTime() > timestamp) {
                    throw CalendarExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(id), timestamp, account.getLastModified().getTime());
                }
                storage.getAccountStorage().deleteAccount(session.getUserId(), id);
                return null;
            }
        }.executeUpdate();
        invalidateStorage(session.getContextId(), session.getUserId(), id);
        /*
         * finally let provider perform any additional initialization
         */
        if (null == calendarProvider) {
            LoggerFactory.getLogger(CalendarAccountServiceImpl.class).warn("Provider '{}' not available, skipping additional cleanup tasks for deleted account {}.",
                storedAccount.getProviderId(), storedAccount, CalendarExceptionCodes.PROVIDER_NOT_AVAILABLE.create(storedAccount.getProviderId()));
        } else {
            calendarProvider.onAccountDeleted(session, storedAccount, parameters);
        }
    }

    @Override
    public CalendarAccount getAccount(Session session, int id) throws OXException {
        CalendarAccount storedAccount = new OSGiCalendarStorageOperation<CalendarAccount>(services, session.getContextId(), -1) {

            @Override
            protected CalendarAccount call(CalendarStorage storage) throws OXException {
                return storage.getAccountStorage().loadAccount(session.getUserId(), id);
            }
        }.executeQuery();
        if (null == storedAccount && CalendarAccount.DEFAULT_ACCOUNT.getAccountId() == id) {
            if (isGuest(session)) {
                /*
                 * return a virtual default calendar account for guest users
                 */
                storedAccount = getVirtualDefaultAccount(session);
            } else {
                /*
                 * get default account from list to implicitly trigger pending auto-provisioning tasks of the default account
                 */
                storedAccount = find(getAccounts(session), CalendarAccount.DEFAULT_ACCOUNT.getProviderId());
            }
        }
        if (null == storedAccount) {
            throw CalendarExceptionCodes.ACCOUNT_NOT_FOUND.create(id);
        }
        return storedAccount;
    }

    @Override
    public List<CalendarAccount> getAccounts(Session session) throws OXException {
        /*
         * get accounts from storage
         */
        List<CalendarAccount> accounts = getAccounts(session.getContextId(), session.getUserId());
        /*
         * check for pending provisioning tasks
         */
        if (false == getProvidersRequiringProvisioning(accounts).isEmpty() && false == isGuest(session)) {
            CalendarParameters parameters = null;
            accounts = new OSGiCalendarStorageOperation<List<CalendarAccount>>(services, session.getContextId(), -1) {

                @Override
                protected List<CalendarAccount> call(CalendarStorage storage) throws OXException {
                    /*
                     * re-check account list for pending auto-provisioning within transaction & auto-provision as needed
                     */
                    List<CalendarAccount> accounts = storage.getAccountStorage().loadAccounts(session.getUserId());
                    for (AutoProvisioningCalendarProvider calendarProvider : getProvidersRequiringProvisioning(accounts)) {
                        JSONObject userConfig = new JSONObject();
                        JSONObject internalConfig = calendarProvider.autoConfigureAccount(session, userConfig, parameters);
                        CalendarAccount account = insertAccount(storage.getAccountStorage(), calendarProvider.getId(), session.getUserId(), internalConfig, userConfig);
                        calendarProvider.onAccountCreated(session, account, parameters);
                        accounts.add(account);
                    }
                    return accounts;
                }
            }.executeUpdate();
            /*
             * (re-)invalidate caches outside of transaction
             */
            invalidateStorage(session.getContextId(), session.getUserId());
        }
        if (accounts.isEmpty() && isGuest(session)) {
            /*
             * include a virtual default calendar account for guest users
             */
            accounts = Collections.singletonList(getVirtualDefaultAccount(session));
        }
        return accounts;
    }

    @Override
    public List<CalendarAccount> getAccounts(Session session, String providerId) throws OXException {
        return findAll(getAccounts(session), providerId);
    }

    @Override
    public List<CalendarAccount> getAccounts(int contextId, int userId) throws OXException {
        /*
         * get accounts from storage
         */
        List<CalendarAccount> accounts = new OSGiCalendarStorageOperation<List<CalendarAccount>>(services, contextId, -1) {

            @Override
            protected List<CalendarAccount> call(CalendarStorage storage) throws OXException {
                return storage.getAccountStorage().loadAccounts(userId);
            }
        }.executeQuery();
        return accounts;
    }

    @Override
    public List<CalendarAccount> getAccounts(int contextId, int[] userIds, String providerId) throws OXException {
        return new OSGiCalendarStorageOperation<List<CalendarAccount>>(services, contextId, -1) {

            @Override
            protected List<CalendarAccount> call(CalendarStorage storage) throws OXException {
                return storage.getAccountStorage().loadAccounts(userIds, providerId);
            }
        }.executeQuery();
    }

    @Override
    public CalendarAccount getAccount(int contextId, int userId, String providerId) throws OXException {
        return new OSGiCalendarStorageOperation<CalendarAccount>(services, contextId, -1) {

            @Override
            protected CalendarAccount call(CalendarStorage storage) throws OXException {
                if (CalendarAccount.DEFAULT_ACCOUNT.getProviderId().equals(providerId)) {
                    return storage.getAccountStorage().loadAccount(userId, CalendarAccount.DEFAULT_ACCOUNT.getAccountId());
                }
                return storage.getAccountStorage().loadAccount(userId, providerId);
            }
        }.executeQuery();
    }

    @Override
    public CalendarAccount updateAccount(int contextId, int userId, int accountId, Boolean enabled, JSONObject internalConfig, JSONObject userConfig, long clientTimestamp) throws OXException {
        CalendarAccount account = new OSGiCalendarStorageOperation<CalendarAccount>(services, contextId, -1) {

            @Override
            protected CalendarAccount call(CalendarStorage storage) throws OXException {
                CalendarAccount account = storage.getAccountStorage().loadAccount(userId, accountId);
                if (null != account.getLastModified() && account.getLastModified().getTime() > clientTimestamp) {
                    throw CalendarExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(accountId), clientTimestamp, account.getLastModified().getTime());
                }
                if (Boolean.FALSE.equals(enabled) && CalendarAccount.DEFAULT_ACCOUNT.getProviderId().equals(account.getProviderId())) {
                    throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(account.getProviderId());
                }
                Date now = new Date();
                CalendarAccount accountUpdate = new DefaultCalendarAccount(account.getProviderId(), account.getAccountId(), account.getUserId(),
                    null != enabled ? enabled.booleanValue() : account.isEnabled(), internalConfig, userConfig, now);
                storage.getAccountStorage().updateAccount(accountUpdate);
                return new DefaultCalendarAccount(account.getProviderId(), account.getAccountId(), account.getUserId(),
                    null != enabled ? enabled.booleanValue() : account.isEnabled(),
                    null != internalConfig ? internalConfig : account.getInternalConfiguration(),
                    null != userConfig ? userConfig : account.getUserConfiguration(), now);
            }
        }.executeUpdate();
        invalidateStorage(contextId, userId, accountId);
        return account;
    }

    /**
     * Gets a list of auto-provisioning calendar providers where no calendar account is found in the supplied list of accounts, i.e. those
     * providers who where a provisioning task is required.
     *
     * @param existingAccounts The accounts to check against the registered auto-provisioning calendar providers
     * @return The auto-provisioning calendar providers where no calendar account was found
     */
    private List<AutoProvisioningCalendarProvider> getProvidersRequiringProvisioning(List<CalendarAccount> existingAccounts) throws OXException {
        CalendarProviderRegistry providerRegistry = getProviderRegistry();
        List<AutoProvisioningCalendarProvider> unprovisionedProviders = new ArrayList<AutoProvisioningCalendarProvider>();
        for (AutoProvisioningCalendarProvider calendarProvider : providerRegistry.getAutoProvisioningCalendarProviders()) {
            if (null == find(existingAccounts, calendarProvider.getId())) {
                unprovisionedProviders.add(calendarProvider);
            }
        }
        return unprovisionedProviders;
    }

    /**
     * Prepares and stores a new calendar account.
     *
     * @param contextId The context identifier
     * @param calendarProvider The calendar provider
     * @param userId The user identifier
     * @param internalConfig The account's internal / protected configuration data
     * @param userConfig The account's external / user configuration data
     * @return The new calendar account
     */
    private CalendarAccount insertAccount(int contextId, CalendarProvider calendarProvider, int userId, JSONObject internalConfig, JSONObject userConfig) throws OXException {
        CalendarAccount account = new OSGiCalendarStorageOperation<CalendarAccount>(services, contextId, -1) {

            @Override
            protected CalendarAccount call(CalendarStorage storage) throws OXException {
                /*
                 * check for an existing account for this provider if required
                 */
                if (false == allowsMultipleAccounts(calendarProvider) && 0 < storage.getAccountStorage().loadAccounts(new int[] { userId }, calendarProvider.getId()).size()) {
                    throw CalendarExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(calendarProvider.getId());
                }
                return insertAccount(storage.getAccountStorage(), calendarProvider.getId(), userId, internalConfig, userConfig);
            }
        }.executeUpdate();
        /*
         * (re-)invalidate caches outside of transaction
         */
        invalidateStorage(contextId, userId);
        return account;
    }

    /**
     * Prepares and stores a new calendar account.
     *
     * @param storage The calendar storage
     * @param providerId The provider identifier
     * @param userId The user identifier
     * @param internalConfig The account's internal / protected configuration data
     * @param userConfig The account's external / user configuration data
     * @return The new calendar account
     */
    private CalendarAccount insertAccount(CalendarAccountStorage storage, String providerId, int userId, JSONObject internalConfig, JSONObject userConfig) throws OXException {
        int accountId;
        if (CalendarAccount.DEFAULT_ACCOUNT.getProviderId().equals(providerId)) {
            accountId = CalendarAccount.DEFAULT_ACCOUNT.getAccountId();
        } else {
            accountId = storage.nextId();
        }
        DefaultCalendarAccount account = new DefaultCalendarAccount(providerId, accountId, userId, true, internalConfig, userConfig, new Date());
        storage.insertAccount(account);
        return account;
    }

    private CalendarProvider getProvider(String providerId) throws OXException {
        CalendarProvider calendarProvider = getProviderRegistry().getCalendarProvider(providerId);
        if (null == calendarProvider) {
            throw CalendarExceptionCodes.PROVIDER_NOT_AVAILABLE.create(providerId);
        }
        return calendarProvider;
    }

    private CalendarProviderRegistry getProviderRegistry() throws OXException {
        CalendarProviderRegistry providerRegistry = services.getOptionalService(CalendarProviderRegistry.class);
        if (null == providerRegistry) {
            throw ServiceExceptionCode.absentService(CalendarProviderRegistry.class);
        }
        return providerRegistry;
    }

    private static CalendarAccount find(Collection<CalendarAccount> accounts, String providerId) {
        return accounts.stream().filter(account -> providerId.equals(account.getProviderId())).findFirst().orElse(null);
    }

    private static List<CalendarAccount> findAll(Collection<CalendarAccount> accounts, String providerId) {
        return accounts.stream().filter(account -> providerId.equals(account.getProviderId())).collect(Collectors.toList());
    }

    private static boolean allowsMultipleAccounts(CalendarProvider provider) {
        return false == SingleAccountCalendarProvider.class.isInstance(provider);
    }

    private static boolean isGuest(Session session) {
        return Boolean.TRUE.equals(session.getParameter(Session.PARAM_GUEST));
    }

    private static CalendarAccount getVirtualDefaultAccount(Session session) {
        return new DefaultCalendarAccount(CalendarAccount.DEFAULT_ACCOUNT.getProviderId(), CalendarAccount.DEFAULT_ACCOUNT.getAccountId(), session.getUserId(), true, new JSONObject(), new JSONObject(), new Date());
    }

    private void invalidateStorage(int contextId, int userId) throws OXException {
        invalidateStorage(contextId, userId, -1);
    }

    private void invalidateStorage(int contextId, int userId, int accountId) throws OXException {
        new OSGiCalendarStorageOperation<Void>(services, contextId, -1) {

            @Override
            protected Void call(CalendarStorage storage) throws OXException {
                storage.getAccountStorage().invalidateAccount(contextId, accountId);
                return null;
            }
        }.executeQuery();
    }

}
