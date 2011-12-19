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

package com.openexchange.unifiedinbox.converters;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolder.DefaultFolderType;
import com.openexchange.mail.dataobjects.ReadOnlyMailFolder;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.mailaccount.UnifiedINBOXManagement;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.unifiedinbox.UnifiedINBOXAccess;
import com.openexchange.unifiedinbox.UnifiedINBOXException;
import com.openexchange.unifiedinbox.services.UnifiedINBOXServiceRegistry;
import com.openexchange.unifiedinbox.utility.LoggingCallable;
import com.openexchange.unifiedinbox.utility.TrackingCompletionService;
import com.openexchange.unifiedinbox.utility.UnifiedINBOXCompletionService;
import com.openexchange.unifiedinbox.utility.UnifiedINBOXUtility;

/**
 * {@link UnifiedINBOXFolderConverter} - Converts a Unified INBOX folder to an instance of {@link MailFolder}.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UnifiedINBOXFolderConverter {

    static final int[] EMPTY_COUNTS = new int[] { 0, 0, 0, 0 };

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(UnifiedINBOXFolderConverter.class));

    private static final MailFolder ROOT_UNIFIED_INBOX_FOLDER;

    static {
        final MailFolder tmp = new MailFolder();
        tmp.setSubscribed(true);
        tmp.setSupportsUserFlags(false);
        tmp.setRootFolder(true);
        tmp.setExists(true);
        tmp.setSeparator('/');
        // Only the default folder contains subfolders, to be more precise it only contains the INBOX folder.
        tmp.setSubfolders(true);
        tmp.setSubscribedSubfolders(true);
        tmp.setFullname(MailFolder.DEFAULT_FOLDER_ID);
        tmp.setParentFullname(null);
        tmp.setName(UnifiedINBOXManagement.NAME_UNIFIED_INBOX);
        tmp.setHoldsFolders(true);
        tmp.setHoldsMessages(false);
        {
            final MailPermission ownPermission = new DefaultMailPermission();
            ownPermission.setFolderPermission(OCLPermission.READ_FOLDER);
            ownPermission.setAllObjectPermission(OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
            ownPermission.setFolderAdmin(false);
            tmp.setOwnPermission(ownPermission);
        }
        {
            final MailPermission permission = new DefaultMailPermission();
            permission.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
            permission.setGroupPermission(true);
            permission.setFolderPermission(OCLPermission.READ_FOLDER);
            permission.setAllObjectPermission(OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS, OCLPermission.NO_PERMISSIONS);
            permission.setFolderAdmin(false);
            tmp.addPermission(permission);
        }
        tmp.setDefaultFolder(false);
        tmp.setDefaultFolderType(DefaultFolderType.NONE);
        tmp.setMessageCount(-1);
        tmp.setNewMessageCount(-1);
        tmp.setUnreadMessageCount(-1);
        tmp.setDeletedMessageCount(-1);
        ROOT_UNIFIED_INBOX_FOLDER = new ReadOnlyMailFolder(tmp);
    }

    /**
     * Prevent instantiation
     */
    private UnifiedINBOXFolderConverter() {
        super();
    }

    /**
     * Gets the instance of {@link MailFolder} for root folder.
     *
     * @return The instance of {@link MailFolder} for root folder.
     */
    public static MailFolder getRootFolder() {
        return ROOT_UNIFIED_INBOX_FOLDER;
    }

    /**
     * Gets the appropriately filled instance of {@link MailFolder}.
     *
     * @param unifiedInboxAccountId The account ID of the Unified INBOX account
     * @param session The session
     * @param fullName The folder's full name
     * @param localizedName The localized name of the folder
     * @return The appropriately filled instance of {@link MailFolder}
     * @throws OXException If converting mail folder fails
     */
    public static MailFolder getUnifiedINBOXFolder(final int unifiedInboxAccountId, final Session session, final String fullname, final String localizedName) throws OXException {
        return getUnifiedINBOXFolder(unifiedInboxAccountId, session, fullname, localizedName, null);
    }

    /**
     * Gets the appropriately filled instance of {@link MailFolder}.
     *
     * @param unifiedInboxAccountId The account ID of the Unified INBOX account
     * @param session The session
     * @param fullname The folder's full name
     * @param localizedName The localized name of the folder
     * @param executor The executor to use to concurrently load accounts' message counts
     * @return The appropriately filled instance of {@link MailFolder}
     * @throws OXException If converting mail folder fails
     */
    public static MailFolder getUnifiedINBOXFolder(final int unifiedInboxAccountId, final Session session, final String fullname, final String localizedName, final Executor executor) throws OXException {
        final MailFolder tmp = new MailFolder();
        // Subscription not supported by Unified INBOX, so every folder is "subscribed"
        tmp.setSubscribed(true);
        tmp.setSupportsUserFlags(true);
        tmp.setRootFolder(false);
        tmp.setExists(true);
        tmp.setSeparator('/');
        tmp.setFullname(fullname);
        tmp.setParentFullname(MailFolder.DEFAULT_FOLDER_ID);
        tmp.setName(localizedName);
        tmp.setHoldsFolders(true);
        tmp.setHoldsMessages(true);
        {
            final MailPermission ownPermission = new DefaultMailPermission();
            ownPermission.setFolderPermission(OCLPermission.READ_FOLDER);
            ownPermission.setAllObjectPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_ALL_OBJECTS);
            ownPermission.setFolderAdmin(false);
            tmp.setOwnPermission(ownPermission);
        }
        {
            final MailPermission permission = new DefaultMailPermission();
            permission.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
            permission.setGroupPermission(true);
            permission.setFolderPermission(OCLPermission.READ_FOLDER);
            permission.setAllObjectPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_ALL_OBJECTS);
            permission.setFolderAdmin(false);
            tmp.addPermission(permission);
        }
        // What else?!
        tmp.setDefaultFolder(true);
        if (UnifiedINBOXAccess.INBOX.equals(fullname)) {
            tmp.setDefaultFolderType(DefaultFolderType.INBOX);
        } else if (UnifiedINBOXAccess.TRASH.equals(fullname)) {
            tmp.setDefaultFolderType(DefaultFolderType.TRASH);
        } else if (UnifiedINBOXAccess.SENT.equals(fullname)) {
            tmp.setDefaultFolderType(DefaultFolderType.SENT);
        } else if (UnifiedINBOXAccess.SPAM.equals(fullname)) {
            tmp.setDefaultFolderType(DefaultFolderType.SPAM);
        } else if (UnifiedINBOXAccess.DRAFTS.equals(fullname)) {
            tmp.setDefaultFolderType(DefaultFolderType.DRAFTS);
        } else {
            tmp.setDefaultFolderType(DefaultFolderType.NONE);
        }
        // Set message counts
        final boolean hasAtLeastOneSuchFolder = setMessageCounts(fullname, unifiedInboxAccountId, session, tmp, executor);
        if (hasAtLeastOneSuchFolder) {
            tmp.setSubfolders(true);
            tmp.setSubscribedSubfolders(true);
        }
        return tmp;
    }

    public static void setOwnPermission(final MailFolder mailFolder, final int userId) {
        final MailPermission ownPermission = new DefaultMailPermission();
        ownPermission.setEntity(userId);
        ownPermission.setGroupPermission(false);
        ownPermission.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
        ownPermission.setAllObjectPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_ALL_OBJECTS);
        ownPermission.setFolderAdmin(false);
        mailFolder.setOwnPermission(ownPermission);
    }

    public static void setPermissions(final MailFolder mailFolder) {
        final MailPermission permission = new DefaultMailPermission();
        permission.setEntity(OCLPermission.ALL_GROUPS_AND_USERS);
        permission.setGroupPermission(true);
        permission.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
        permission.setAllObjectPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.NO_PERMISSIONS, OCLPermission.DELETE_ALL_OBJECTS);
        permission.setFolderAdmin(false);
        mailFolder.removePermissions();
        mailFolder.addPermission(permission);
    }

    private static boolean setMessageCounts(final String fullname, final int unifiedInboxAccountId, final Session session, final MailFolder tmp, final Executor executor) throws UnifiedINBOXException, OXException {
        final MailAccount[] accounts;
        {
            final MailAccountStorageService storageService =
                UnifiedINBOXServiceRegistry.getServiceRegistry().getService(MailAccountStorageService.class, true);
            final MailAccount[] arr = storageService.getUserMailAccounts(session.getUserId(), session.getContextId());
            final List<MailAccount> l = new ArrayList<MailAccount>(arr.length);
            for (final MailAccount mailAccount : arr) {
                if (unifiedInboxAccountId != mailAccount.getId() && mailAccount.isUnifiedINBOXEnabled()) {
                    l.add(mailAccount);
                }
            }
            accounts = l.toArray(new MailAccount[l.size()]);
        }
        // Create completion service for simultaneous access
        final int length = accounts.length;
        final Executor exec;
        if (executor == null) {
            exec = ThreadPools.getThreadPool().getExecutor();
        } else {
            exec = executor;
        }
        final TrackingCompletionService<int[]> completionService = new UnifiedINBOXCompletionService<int[]>(exec);
        final AtomicBoolean retval = new AtomicBoolean();
        // Iterate
        {
            final StringBuilder sb = new StringBuilder(128);
            for (final MailAccount mailAccount : accounts) {
                sb.setLength(0);
                completionService.submit(new LoggingCallable<int[]>(session) {

                    public int[] call() throws Exception {
                        final MailAccess<?, ?> mailAccess;
                        try {
                            mailAccess = MailAccess.getInstance(session, mailAccount.getId());
                            mailAccess.connect();
                        } catch (final OXException e) {
                            getLogger().debug(e.getMessage(), e);
                            return EMPTY_COUNTS;
                        }
                        try {
                            final String accountFullname = UnifiedINBOXUtility.determineAccountFullname(mailAccess, fullname);
                            // Check if account fullname is not null
                            if (null == accountFullname) {
                                return EMPTY_COUNTS;
                            }
                            // Get counts
                            final MailFolder mailFolder = mailAccess.getFolderStorage().getFolder(accountFullname);
                            final int[] counts =
                                new int[] {
                                    mailFolder.getMessageCount(), mailFolder.getUnreadMessageCount(), mailFolder.getDeletedMessageCount(),
                                    mailFolder.getNewMessageCount() };
                            retval.set(true);
                            return counts;
                        } finally {
                            mailAccess.close(true);
                        }
                    }
                });
            }
        }
        // Wait for completion of each submitted task
        try {
            // Init counts
            int totaCount = 0;
            int unreadCount = 0;
            int deletedCount = 0;
            int newCount = 0;
            // Take completed tasks and apply their counts
            for (int i = 0; i < length; i++) {
                final int[] counts = completionService.take().get();
                // Add counts
                totaCount += counts[0];
                unreadCount += counts[1];
                deletedCount += counts[2];
                newCount += counts[3];
            }
            if (LOG.isDebugEnabled()) {
                LOG.debug(new StringBuilder("Retrieving message counts of folder \"").append(fullname).append("\" took ").append(
                    completionService.getDuration()).append("msec."));
            }
            // Apply counts
            tmp.setMessageCount(totaCount);
            tmp.setNewMessageCount(newCount);
            tmp.setUnreadMessageCount(unreadCount);
            tmp.setDeletedMessageCount(deletedCount);
            return retval.get();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw MailExceptionCode.INTERRUPT_ERROR.create(
                e);
        } catch (final ExecutionException e) {
            throw ThreadPools.launderThrowable(e, OXException.class);
        }
    }

    /**
     * Gets the default folder's message counts of denoted account.
     *
     * @param accountId The account ID
     * @param session The session providing needed user data
     * @param fullnames The fullnames
     * @return The default folder's message counts of denoted account
     * @throws OXException If a mail error occurs
     */
    public static int[][] getAccountDefaultFolders(final int accountId, final Session session, final String[] fullnames) throws OXException {
        final int[][] retval;
        if (LOG.isDebugEnabled()) {
            final long s = System.currentTimeMillis();
            retval = getAccountDefaultFolders0(accountId, session, fullnames);
            LOG.debug(new StringBuilder(64).append("Getting account ").append(accountId).append(" default folders took ").append(
                (System.currentTimeMillis() - s)).append("msec").toString());
        } else {
            retval = getAccountDefaultFolders0(accountId, session, fullnames);
        }
        return retval;
    }

    private static int[][] getAccountDefaultFolders0(final int accountId, final Session session, final String[] fullnames) throws OXException {
        final int[][] retval = new int[fullnames.length][];
        // Get & connect appropriate mail access
        final MailAccess<?, ?> mailAccess;
        try {
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
            return new int[0][];
        }
        try {
            for (int i = 0; i < retval.length; i++) {
                final String accountFullname = UnifiedINBOXUtility.determineAccountFullname(mailAccess, fullnames[i]);
                if (null != accountFullname && mailAccess.getFolderStorage().exists(accountFullname)) {
                    final MailFolder mf = mailAccess.getFolderStorage().getFolder(accountFullname);
                    retval[i] =
                        new int[] { mf.getMessageCount(), mf.getUnreadMessageCount(), mf.getDeletedMessageCount(), mf.getNewMessageCount() };
                } else if (LOG.isDebugEnabled()) {
                    LOG.debug(new StringBuilder(32).append("Missing folder \"").append(fullnames[i]).append("\" in account ").append(
                        accountId).toString());
                }
            }
        } finally {
            mailAccess.close(true);
        }
        return retval;
    }

    /**
     * Merges specified default folders.
     *
     * @param accountFolders The default folders
     * @param fullnames The fullnames
     * @param localizedNames The localized names
     * @return The merged default folders
     */
    public static MailFolder[] mergeAccountDefaultFolders(final List<int[][]> accountFolders, final String[] fullnames, final String[] localizedNames) {
        if (accountFolders.isEmpty()) {
            return new MailFolder[0];
        }
        final MailFolder[] retval = new MailFolder[accountFolders.get(0).length];
        final int size = accountFolders.size();
        for (int i = 0; i < retval.length; i++) {
            final MailFolder tmp = retval[i] = new MailFolder();
            // Subscription not supported by Unified INBOX, so every folder is "subscribed"
            final String fullname = fullnames[i];
            tmp.setSubscribed(true);
            tmp.setSupportsUserFlags(true);
            tmp.setRootFolder(false);
            tmp.setExists(true);
            tmp.setSeparator('/');
            tmp.setFullname(fullname);
            tmp.setParentFullname(MailFolder.DEFAULT_FOLDER_ID);
            tmp.setName(localizedNames[i]);
            tmp.setHoldsFolders(true);
            tmp.setHoldsMessages(true);
            {
                final MailPermission ownPermission = new DefaultMailPermission();
                ownPermission.setFolderPermission(OCLPermission.CREATE_OBJECTS_IN_FOLDER);
                tmp.setOwnPermission(ownPermission);
                tmp.addPermission(ownPermission);
            }
            // What else?!
            tmp.setDefaultFolder(true);
            if (UnifiedINBOXAccess.INBOX.equals(fullname)) {
                tmp.setDefaultFolderType(DefaultFolderType.INBOX);
            } else if (UnifiedINBOXAccess.TRASH.equals(fullname)) {
                tmp.setDefaultFolderType(DefaultFolderType.TRASH);
            } else if (UnifiedINBOXAccess.SENT.equals(fullname)) {
                tmp.setDefaultFolderType(DefaultFolderType.SENT);
            } else if (UnifiedINBOXAccess.SPAM.equals(fullname)) {
                tmp.setDefaultFolderType(DefaultFolderType.SPAM);
            } else if (UnifiedINBOXAccess.DRAFTS.equals(fullname)) {
                tmp.setDefaultFolderType(DefaultFolderType.DRAFTS);
            } else {
                tmp.setDefaultFolderType(DefaultFolderType.NONE);
            }
            // Gather counts
            int totaCount = 0;
            int unreadCount = 0;
            int deletedCount = 0;
            int newCount = 0;
            final Iterator<int[][]> it = accountFolders.iterator();
            boolean hasSubfolders = false;
            for (int j = 0; j < size; j++) {
                final int[] accountDefaultFolder = it.next()[i];
                if (null != accountDefaultFolder) {
                    hasSubfolders = true;
                    // Add counts
                    totaCount += accountDefaultFolder[0];
                    unreadCount += accountDefaultFolder[1];
                    deletedCount += accountDefaultFolder[2];
                    newCount += accountDefaultFolder[3];
                }
            }
            // Apply results
            tmp.setSubfolders(hasSubfolders);
            tmp.setSubscribedSubfolders(hasSubfolders);
            tmp.setMessageCount(totaCount);
            tmp.setNewMessageCount(newCount);
            tmp.setUnreadMessageCount(unreadCount);
            tmp.setDeletedMessageCount(deletedCount);
        }
        return retval;
    }

}
