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

package com.openexchange.contact.account.service.impl;

import static com.openexchange.contact.common.ContactsParameters.PARAMETER_CONNECTION;
import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.i;
import static com.openexchange.osgi.Tools.requireService;
import static com.openexchange.session.Sessions.isGuest;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.Lock;
import java.util.stream.Collectors;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import com.google.common.collect.ImmutableList;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.contact.common.ContactsAccount;
import com.openexchange.contact.common.ContactsParameters;
import com.openexchange.contact.common.DefaultContactsAccount;
import com.openexchange.contact.provider.AutoProvisioningContactsProvider;
import com.openexchange.contact.provider.ContactsAccountService;
import com.openexchange.contact.provider.ContactsProvider;
import com.openexchange.contact.provider.ContactsProviderExceptionCodes;
import com.openexchange.contact.provider.ContactsProviderRegistry;
import com.openexchange.contact.provider.ContactsProviders;
import com.openexchange.contact.provider.basic.BasicContactsProvider;
import com.openexchange.contact.provider.basic.ContactsSettings;
import com.openexchange.contact.storage.ContactStorages;
import com.openexchange.contact.storage.ContactsAccountStorage;
import com.openexchange.contact.storage.ContactsStorageFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.provider.DBTransactionPolicy;
import com.openexchange.database.provider.SimpleDBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.lock.LockService;
import com.openexchange.policy.retry.ExponentialBackOffRetryPolicy;
import com.openexchange.policy.retry.RetryPolicy;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;

/**
 * {@link ContactsAccountServiceImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactsAccountServiceImpl implements ContactsAccountService {

    private final ServiceLookup services;

    /**
     * Initializes a new {@link ContactsAccountServiceImpl}.
     */
    public ContactsAccountServiceImpl(ServiceLookup serviceLookup) {
        super();
        this.services = serviceLookup;
    }

    @Override
    public ContactsSettings probeAccountSettings(Session session, String providerId, ContactsSettings settings, ContactsParameters parameters) throws OXException {
        ContactsProvider provider = requireCapability(getProvider(providerId), session);
        if (isGuest(session) || false == BasicContactsProvider.class.isInstance(provider)) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(providerId);
        }
        return BasicContactsProvider.class.cast(provider).probe(session, settings, parameters);
    }

    @Override
    public ContactsAccount createAccount(Session session, String providerId, ContactsSettings settings, ContactsParameters parameters) throws OXException {
        ContactsProvider provider = requireCapability(getProvider(providerId), session);
        if (isGuest(session) || false == BasicContactsProvider.class.isInstance(provider)) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(providerId);
        }
        return insertAccount(session, provider, BasicContactsProvider.class.cast(provider).configureAccount(session, settings, parameters), settings.getConfig(), parameters);
    }

    @Override
    public ContactsAccount updateAccount(Session session, int id, long clientTimestamp, ContactsSettings settings, ContactsParameters parameters) throws OXException {
        ContactsAccount storedAccount = getAccount(session, id, parameters);
        if (null != storedAccount.getLastModified() && storedAccount.getLastModified().getTime() > clientTimestamp) {
            throw ContactsProviderExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(id), L(clientTimestamp), L(storedAccount.getLastModified().getTime()));
        }
        String providerId = storedAccount.getProviderId();
        ContactsProvider provider = requireCapability(getProvider(providerId), session);
        if (isGuest(session) || false == BasicContactsProvider.class.isInstance(provider)) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(providerId);
        }
        return updateAccount(session, id, clientTimestamp, BasicContactsProvider.class.cast(provider).configureAccount(session, settings, parameters), settings.getConfig(), parameters);
    }

    @Override
    public void deleteAccount(Session session, int id, long clientTimestamp, ContactsParameters parameters) throws OXException {
        ContactsAccount storedAccount = initAccountStorage(session.getContextId(), parameters).loadAccount(session.getUserId(), id);
        if (null != storedAccount.getLastModified() && storedAccount.getLastModified().getTime() > clientTimestamp) {
            throw ContactsProviderExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(id), L(clientTimestamp), L(storedAccount.getLastModified().getTime()));
        }
        Optional<ContactsProvider> contactsProvider = getProviderRegistry().getContactProvider(storedAccount.getProviderId());
        if (contactsProvider.isPresent() && AutoProvisioningContactsProvider.class.isInstance(contactsProvider.get())) {
            throw ContactsProviderExceptionCodes.UNSUPPORTED_OPERATION_FOR_PROVIDER.create(contactsProvider.get().getId());
        }
        doDeleteAccount(session, id, clientTimestamp, parameters);
    }

    @Override
    public ContactsAccount getAccount(Session session, int id, ContactsParameters parameters) throws OXException {
        return getAccounts(session, ImmutableList.of(I(id)), parameters).get(0);
    }

    @Override
    public List<ContactsAccount> getAccounts(Session session, ContactsParameters parameters) throws OXException {
        List<ContactsAccount> accounts = sort(initAccountStorage(session.getContextId(), parameters).loadAccounts(session.getUserId()));
        if (false == isGuest(session)) {
            autoProvisionAccounts(session, accounts, parameters);
        }
        if (accounts.isEmpty() && isGuest(session)) {
            // Just return the virtual default contacts account for the guest users
            return ImmutableList.of(getVirtualDefaultAccount(session));
        }
        return sort(accounts);
    }

    @Override
    public List<ContactsAccount> getAccounts(Session session, List<Integer> ids, ContactsParameters parameters) throws OXException {
        List<ContactsAccount> storedAccounts = initAccountStorage(session.getContextId(), parameters).loadAccounts(session.getUserId(), ids);
        int index = 0;
        for (ContactsAccount account : storedAccounts) {
            if (null == account && ContactsAccount.DEFAULT_ACCOUNT.getAccountId() == i(ids.get(index))) {
                // Return a virtual default contacts account for guest users or
                // get default account from list to implicitly trigger pending 
                // auto-provisioning tasks of the default account.
                storedAccounts.set(index, isGuest(session) ? getVirtualDefaultAccount(session) : find(getAccounts(session, parameters), ContactsAccount.DEFAULT_ACCOUNT.getProviderId()));
            }
            if (null == storedAccounts.get(index)) {
                throw ContactsProviderExceptionCodes.ACCOUNT_NOT_FOUND.create(ids.get(index));
            }
            index++;
        }
        return storedAccounts;
    }

    @Override
    public List<ContactsAccount> getAccounts(Session session, String providerId, ContactsParameters parameters) throws OXException {
        return sort(findAll(getAccounts(session, parameters), providerId));
    }

    ///////////////////////////////////////// HELPERS /////////////////////////////////////////

    /**
     * Retrieves the {@link ContactsProviderRegistry} if present
     *
     * @return The {@link ContactsProviderRegistry} if present
     * @throws OXException if the registry is absent
     */
    private ContactsProviderRegistry getProviderRegistry() throws OXException {
        return services.getServiceSafe(ContactsProviderRegistry.class);
    }

    /**
     * Retrieves the {@link ContactsProvider} with the specified identifier
     *
     * @param providerId The provider's identifier
     * @return The {@link ContactsProvider}
     * @throws OXException if the provider is not available
     */
    private ContactsProvider getProvider(String providerId) throws OXException {
        return getProviderRegistry().getContactProvider(providerId).orElseThrow(() -> ContactsProviderExceptionCodes.PROVIDER_NOT_AVAILABLE.create(providerId));
    }

    /**
     * Gets a list of auto-provisioning contacts providers where no contacts account is found in the supplied list of accounts, i.e. those
     * providers who where a provisioning task is required.
     *
     * @param session The current session
     * @param existingAccounts The accounts to check against the registered auto-provisioning contacts providers
     * @return The auto-provisioning contacts providers where no contacts account was found
     */
    List<AutoProvisioningContactsProvider> getProvidersRequiringProvisioning(Session session, List<ContactsAccount> existingAccounts) throws OXException {
        ContactsProviderRegistry providerRegistry = getProviderRegistry();
        List<AutoProvisioningContactsProvider> unprovisionedProviders = new ArrayList<>();
        for (AutoProvisioningContactsProvider contactsProvider : providerRegistry.getAutoProvisioningContactsProviders()) {
            if (null == find(existingAccounts, contactsProvider.getId()) && hasCapability(contactsProvider, session)) {
                unprovisionedProviders.add(contactsProvider);
            }
        }
        return unprovisionedProviders;
    }

    /**
     * Initialises the contacts account storage for a specific context.
     *
     * @param contextId The context identifier
     * @return The account storage
     */
    private ContactsAccountStorage initAccountStorage(int contextId, ContactsParameters parameters) throws OXException {
        ContactsStorageFactory storageFactory = requireService(ContactsStorageFactory.class, services);
        Context context = requireService(ContextService.class, services).getContext(contextId);
        Connection connection = null == parameters ? null : parameters.get(PARAMETER_CONNECTION(), Connection.class);
        if (null != connection) {
            SimpleDBProvider dbProvider = new SimpleDBProvider(connection, connection);
            return storageFactory.create(context, dbProvider, DBTransactionPolicy.NO_TRANSACTIONS).getContactsAccountsStorage();
        }
        return storageFactory.create(context).getContactsAccountsStorage();
    }

    /**
     * Sorts the accounts
     *
     * @param accounts The accounts to sort
     * @return The sorted accounts
     */
    private List<ContactsAccount> sort(List<ContactsAccount> accounts) {
        if (null != accounts && 1 < accounts.size()) {
            accounts.sort(ACCOUNT_COMPARATOR);
        }
        return accounts;
    }

    /**
     * Finds the account with the specified provider id
     *
     * @param accounts The accounts to search
     * @param providerId The provider identifier
     * @return the found account or <code>null</code> if none found.
     */
    private ContactsAccount find(Collection<ContactsAccount> accounts, String providerId) {
        return accounts.stream().filter(account -> providerId.equals(account.getProviderId())).findFirst().orElse(null);
    }

    /**
     * Returns a list with all contacts accounts that match the specified provider
     *
     * @param accounts the accounts
     * @param providerId the provider identifier
     * @return the matched accounts
     */
    private List<ContactsAccount> findAll(Collection<ContactsAccount> accounts, String providerId) {
        return accounts.stream().filter(account -> providerId.equals(account.getProviderId())).collect(Collectors.toList());
    }

    /**
     * Returns the default virtual account
     *
     * @param session the session
     * @return the default virtual account
     */
    private ContactsAccount getVirtualDefaultAccount(Session session) {
        return new DefaultContactsAccount(ContactsAccount.DEFAULT_ACCOUNT.getProviderId(), ContactsAccount.DEFAULT_ACCOUNT.getAccountId(), session.getUserId(), new JSONObject(), new JSONObject(), new Date());
    }

    /**
     * Inserts the specified account
     *
     * @param session The session
     * @param provider The contacts provider
     * @param internalConfig The internal configuration
     * @param userConfig The user configuration
     * @param parameters Additional contacts parameters, or <code>null</code> if not set
     * @return The inserted contacts account
     * @throws OXException if an error is occurred
     */
    private ContactsAccount insertAccount(Session session, ContactsProvider provider, JSONObject internalConfig, JSONObject userConfig, ContactsParameters parameters) throws OXException {
        return new GroupwareContactsDatabasePerformer<ContactsAccount>(services, session.getContextId(), parameters) {

            @Override
            ContactsAccount perform(ContactStorages storage) throws OXException {
                ContactsAccountStorage accountsStorage = storage.getContactsAccountsStorage();
                int accountId = ContactsAccount.DEFAULT_ACCOUNT.getProviderId().equals(provider.getId()) ? ContactsAccount.DEFAULT_ACCOUNT.getAccountId() : accountsStorage.nextId();
                accountsStorage.insertAccount(new DefaultContactsAccount(provider.getId(), accountId, session.getUserId(), internalConfig, userConfig, new Date()));
                return accountsStorage.loadAccount(session.getUserId(), accountId);
            }
        }.executeUpdate();
    }

    /**
     * Prepares and stores a new contacts account.
     *
     * @param storage The contacts account storage
     * @param providerId The provider identifier
     * @param userId The user identifier
     * @param internalConfig The account's internal / protected configuration data
     * @param userConfig The account's external / user configuration data
     * @return The new contacts account
     */
    ContactsAccount insertAccount(ContactsAccountStorage storage, String providerId, int userId, JSONObject internalConfig, JSONObject userConfig) throws OXException {
        int accountId = ContactsAccount.DEFAULT_ACCOUNT.getProviderId().equals(providerId) ? ContactsAccount.DEFAULT_ACCOUNT.getAccountId() : storage.nextId();
        storage.insertAccount(new DefaultContactsAccount(providerId, accountId, userId, internalConfig, userConfig, new Date()));
        return storage.loadAccount(userId, accountId);
    }

    /**
     * Updates the specified account
     *
     * @param session The session
     * @param accountId The account's identifier
     * @param clientTimestamp The client timestamp
     * @param internalConfig The internal configuration
     * @param userConfig The user configuration
     * @param parameters Additional contacts parameters, or <code>null</code> if not set
     * @return The updated contacts account
     * @throws OXException if an error is occurred
     */
    private ContactsAccount updateAccount(Session session, int accountId, long clientTimestamp, JSONObject internalConfig, JSONObject userConfig, ContactsParameters parameters) throws OXException {
        return new GroupwareContactsDatabasePerformer<ContactsAccount>(services, session.getContextId(), parameters) {

            @Override
            ContactsAccount perform(ContactStorages storage) throws OXException {
                ContactsAccount account = storage.getContactsAccountsStorage().loadAccount(session.getUserId(), accountId);
                if (null != account.getLastModified() && account.getLastModified().getTime() > clientTimestamp) {
                    throw ContactsProviderExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(accountId), L(clientTimestamp), L(account.getLastModified().getTime()));
                }
                ContactsAccount accountUpdate = new DefaultContactsAccount(account.getProviderId(), account.getAccountId(), account.getUserId(), internalConfig, userConfig, new Date());
                storage.getContactsAccountsStorage().updateAccount(accountUpdate, clientTimestamp);
                return storage.getContactsAccountsStorage().loadAccount(session.getUserId(), accountId);
            }

        }.executeUpdate();
    }

    /**
     * Deletes the account with the specified identifier
     *
     * @param session The session
     * @param accountId The account's identifier
     * @param clientTimestamp The client timestamp
     * @param parameters Additional contacts parameters, or <code>null</code> if not set
     * @throws OXException if an error is occurred
     */
    private void doDeleteAccount(Session session, int accountId, long clientTimestamp, ContactsParameters parameters) throws OXException {
        new GroupwareContactsDatabasePerformer<Void>(services, session.getContextId(), parameters) {

            @Override
            protected Void perform(ContactStorages storage) throws OXException {
                ContactsAccount account = storage.getContactsAccountsStorage().loadAccount(session.getUserId(), accountId);
                if (null != account.getLastModified() && account.getLastModified().getTime() > clientTimestamp) {
                    throw ContactsProviderExceptionCodes.CONCURRENT_MODIFICATION.create(String.valueOf(accountId), L(clientTimestamp), L(account.getLastModified().getTime()));
                }
                storage.getContactsAccountsStorage().deleteAccount(session.getUserId(), accountId, clientTimestamp);
                return null;
            }
        }.executeUpdate();
    }

    /**
     * Checks whether the specified {@link ContactsProvider}'s capability is enabled
     *
     * @param provider The provider
     * @param session the session
     * @return <code>true</code> if the capability is set; <code>false</code> otherwise
     * @throws OXException if an error is occurred
     */
    private boolean hasCapability(ContactsProvider provider, Session session) throws OXException {
        String capabilityName = ContactsProviders.getCapabilityName(provider);
        CapabilitySet capabilities = requireService(CapabilityService.class, services).getCapabilities(session);
        return capabilities.contains(capabilityName);
    }

    /**
     * Checks whether for the specified provider the equivalent capability is enabled
     *
     * @param provider The provider
     * @param session The session
     * @return The {@link ContactsProvider} for chained calls
     * @throws OXException if the capability is missing
     */
    private ContactsProvider requireCapability(ContactsProvider provider, Session session) throws OXException {
        if (false == hasCapability(provider, session)) {
            throw ContactsProviderExceptionCodes.MISSING_CAPABILITY.create(ContactsProviders.getCapabilityName(provider));
        }
        return provider;
    }

    /**
     * Performs the auto-provisioning of contacts accounts
     *
     * @param session The session
     * @param parameters Additional contacts parameters, or <code>null</code> if not set
     * @throws OXException if an error is occurred
     */
    private void autoProvisionAccounts(Session session, List<ContactsAccount> accounts, ContactsParameters parameters) throws OXException {
        if (getProvidersRequiringProvisioning(session, accounts).isEmpty()) {
            return;
        }

        LockService lockService = requireService(LockService.class, services);
        Lock lock = lockService.getSelfCleaningLockFor("autoProvisionContactsAccountsFor User:" + session.getUserId() + ", Context:" + session.getContextId());
        lock.lock();

        try {
            if (getProvidersRequiringProvisioning(session, accounts).isEmpty()) {
                return;
            }
            // Initialise once...
            GroupwareContactsDatabasePerformer<List<ContactsAccount>> performer = new GroupwareContactsDatabasePerformer<List<ContactsAccount>>(services, session.getContextId(), parameters) {

                @Override
                List<ContactsAccount> perform(ContactStorages storage) throws OXException {
                    List<ContactsAccount> accounts = storage.getContactsAccountsStorage().loadAccounts(session.getUserId());
                    for (AutoProvisioningContactsProvider contactsProvider : getProvidersRequiringProvisioning(session, accounts)) {
                        JSONObject userConfig = new JSONObject();
                        JSONObject internalConfig = contactsProvider.autoConfigureAccount(session, userConfig, null);
                        try {
                            accounts.add(insertAccount(storage.getContactsAccountsStorage(), contactsProvider.getId(), session.getUserId(), internalConfig, userConfig));
                        } catch (OXException e) {
                            if (false == ContactsProviderExceptionCodes.ACCOUNT_NOT_FOUND.equals(e)) {
                                throw e;
                            }
                            LoggerFactory.getLogger(ContactsAccountServiceImpl.class).warn("Failed to auto-provision contacts account for user '{}' in context '{}'", I(session.getUserId()), I(session.getContextId()));
                        }
                    }
                    return accounts;
                }
            };
            RetryPolicy retryPolicy = new ExponentialBackOffRetryPolicy(5);
            do {
                try {
                    // ... execute as many times as necessary
                    accounts.addAll(performer.executeUpdate());
                    return;
                } catch (OXException e) {
                    if (false == ContactsProviderExceptionCodes.ACCOUNT_NOT_WRITTEN.equals(e)) {
                        throw e;
                    }
                }
            } while (retryPolicy.isRetryAllowed());
        } finally {
            lock.unlock();
        }
    }

    /**
     * Simple comparator for contacts accounts to deliver accounts in a deterministic order
     */
    private static final Comparator<ContactsAccount> ACCOUNT_COMPARATOR = (account1, account2) -> {
        if (null == account1) {
            return null == account2 ? 0 : 1;
        }
        if (null == account2) {
            return -1;
        }
        return Integer.compare(account1.getAccountId(), account2.getAccountId());
    };
}
