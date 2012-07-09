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

package com.openexchange.imap.cache;

import gnu.trove.map.hash.TObjectIntHashMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.mail.Folder;
import javax.mail.MessagingException;
import com.openexchange.exception.OXException;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.mail.mime.MimeMailException;
import com.sun.mail.iap.Argument;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import com.sun.mail.imap.Rights;
import com.sun.mail.imap.protocol.BASE64MailboxDecoder;
import com.sun.mail.imap.protocol.BASE64MailboxEncoder;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * {@link ListLsubCollection}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
final class ListLsubCollection {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ListLsubCollection.class));

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static final String ROOT_FULL_NAME = "";

    private static final String INBOX = "INBOX";

    private final ConcurrentMap<String, ListLsubEntryImpl> listMap;

    private final ConcurrentMap<String, ListLsubEntryImpl> lsubMap;

    private final AtomicBoolean deprecated;

    private final String[] shared;

    private final String[] user;

    private Boolean mbox;

    private long stamp;

    /**
     * Initializes a new {@link ListLsubCollection}.
     *
     * @param imapFolder The IMAP folder
     * @param shared The shared namespaces
     * @param user The user namespaces
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws MessagingException If a messaging error occurs
     */
    protected ListLsubCollection(final IMAPFolder imapFolder, final String[] shared, final String[] user, final boolean doStatus, final boolean doGetAcl) throws MessagingException {
        super();
        listMap = new ConcurrentHashMap<String, ListLsubEntryImpl>();
        lsubMap = new ConcurrentHashMap<String, ListLsubEntryImpl>();
        deprecated = new AtomicBoolean();
        this.shared = shared == null ? new String[0] : shared;
        this.user = user == null ? new String[0] : user;
        init(false, imapFolder, doStatus, doGetAcl);
    }

    /**
     * Initializes a new {@link ListLsubCollection}.
     *
     * @param imapStore The IMAP store
     * @param shared The shared namespaces
     * @param user The user namespaces
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws OXException If initialization fails
     */
    protected ListLsubCollection(final IMAPStore imapStore, final String[] shared, final String[] user, final boolean doStatus, final boolean doGetAcl) throws OXException {
        super();
        listMap = new ConcurrentHashMap<String, ListLsubEntryImpl>();
        lsubMap = new ConcurrentHashMap<String, ListLsubEntryImpl>();
        deprecated = new AtomicBoolean();
        this.shared = shared == null ? new String[0] : shared;
        this.user = user == null ? new String[0] : user;
        init(false, imapStore, doStatus, doGetAcl);
    }

    private void checkDeprecated() {
        if (deprecated.get()) {
            throw new ListLsubRuntimeException("LIST/LSUB cache is deprecated.");
        }
    }

    /**
     * Checks if specified full name starts with either shared or user namespace prefix.
     *
     * @param fullName The full name to check
     * @return <code>true</code> if full name starts with either shared or user namespace prefix; otherwise <code>false</code>
     */
    protected boolean isNamespace(final String fullName) {
        for (final String sharedNamespace : shared) {
            if (fullName.startsWith(sharedNamespace)) {
                return true;
            }
        }
        for (final String userNamespace : user) {
            if (fullName.startsWith(userNamespace)) {
                return true;
            }
        }
        return false;
    }

    protected boolean equalsNamespace(final String fullName) {
        for (final String sharedNamespace : shared) {
            if (fullName.equals(sharedNamespace)) {
                return true;
            }
        }
        for (final String userNamespace : user) {
            if (fullName.equals(userNamespace)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if associated mailbox is considered as MBox format.
     *
     * @return {@link Boolean#TRUE} for MBox format, {@link Boolean#FALSE} for no MBOX format or <code>null</code> for undetermined
     */
    public Boolean consideredAsMBox() {
        return mbox;
    }

    /**
     * Checks if this collection is marked as deprecated.
     *
     * @return <code>true</code> if deprecated; otherwise <code>false</code>
     */
    public boolean isDeprecated() {
        return deprecated.get();
    }

    /**
     * Clears this collection and resets its time stamp to force re-initialization.
     */
    public void clear() {
        deprecated.set(true);
        stamp = 0;
        if (DEBUG) {
            final StringBuilder builder = new StringBuilder("Cleared LIST/LSUB cache.\n");
            appendStackTrace(new Throwable().getStackTrace(), builder);
            LOG.debug(builder.toString());
        }
    }

    /**
     * Removes the associated entry.
     *
     * @param fullName The full name
     */
    public void remove(final String fullName) {
        /*
         * Cleanse from LIST map
         */
        removeFrom(fullName, listMap);
        /*
         * Cleanse from LSUB map, too
         */
        removeFrom(fullName, lsubMap);
    }

    private static void removeFrom(final String fullName, final ConcurrentMap<String, ListLsubEntryImpl> map) {
        final ListLsubEntryImpl entry = map.remove(fullName);
        if (null != entry) {
            final ListLsubEntryImpl parent = entry.getParentImpl();
            if (null != parent) {
                parent.removeChild(entry);
            }
            for (final ListLsubEntry child : entry.getChildrenSet()) {
                removeFromMap(child.getFullName(), map);
            }
        }
    }

    private static void removeFromMap(final String fullName, final ConcurrentMap<String, ListLsubEntryImpl> map) {
        final ListLsubEntryImpl entry = map.remove(fullName);
        if (null == entry) {
            return;
        }
        for (final ListLsubEntry child : entry.getChildrenSet()) {
            removeFromMap(child.getFullName(), map);
        }
    }

    /**
     * Re-initializes this collection.
     *
     * @param imapStore The IMAP store
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws OXException If re-initialization fails
     */
    public void reinit(final IMAPStore imapStore, final boolean doStatus, final boolean doGetAcl) throws OXException {
        clear();
        init(true, imapStore, doStatus, doGetAcl);
    }

    /**
     * Re-initializes this collection.
     *
     * @param imapFolder The IMAP folder
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws MessagingException If a messaging error occurs
     */
    public void reinit(final IMAPFolder imapFolder, final boolean doStatus, final boolean doGetAcl) throws MessagingException {
        clear();
        init(true, imapFolder, doStatus, doGetAcl);
    }

    private void init(final boolean clearMaps, final IMAPStore imapStore, final boolean doStatus, final boolean doGetAcl) throws OXException {
        try {
            init(clearMaps, (IMAPFolder) imapStore.getFolder("INBOX"), doStatus, doGetAcl);
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    private void init(final boolean clearMaps, final IMAPFolder imapFolder, final boolean doStatus, final boolean doGetAcl) throws MessagingException {
        if (clearMaps) {
            listMap.clear();
            lsubMap.clear();
        }
        final long st = DEBUG ? System.currentTimeMillis() : 0L;
        /*
         * Perform LIST "" ""
         */
        imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                doRootListCommand(protocol);
                return null;
            }

        });
        /*
         * Perform LSUB "" "*"
         */
        imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                doListLsubCommand(protocol, true);
                return null;
            }

        });
        /*
         * Perform LIST "" "*"
         */
        imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                doListLsubCommand(protocol, false);
                return null;
            }

        });
        /*
         * Debug logs
         */
        if (DEBUG) {
            final StringBuilder sb = new StringBuilder(1024);
            {
                final TreeMap<String, ListLsubEntryImpl> tm = new TreeMap<String, ListLsubEntryImpl>(listMap);
                sb.append("LIST cache contains after (re-)initialization:\n");
                for (final Entry<String, ListLsubEntryImpl> entry : tm.entrySet()) {
                    sb.append('"').append(entry.getKey()).append("\"=").append(entry.getValue()).append('\n');
                }
                LOG.debug(sb.toString());
            }
            {
                final TreeMap<String, ListLsubEntryImpl> tm = new TreeMap<String, ListLsubEntryImpl>(lsubMap);
                sb.setLength(0);
                sb.append("LSUB cache contains after (re-)initialization:\n");
                for (final Entry<String, ListLsubEntryImpl> entry : tm.entrySet()) {
                    sb.append('"').append(entry.getKey()).append("\"=").append(entry.getValue()).append('\n');
                }
                LOG.debug(sb.toString());
            }
        }
        /*
         * Consistency check
         */
        checkConsistency();
        /*
         * Status if enabled
         */
        if (doStatus) {
            final ConcurrentMap<String, ListLsubEntryImpl> primary;
            final ConcurrentMap<String, ListLsubEntryImpl> lookup;
            if (listMap.size() > lsubMap.size()) {
                primary = lsubMap;
                lookup = listMap;
            } else {
                primary = listMap;
                lookup = lsubMap;
            }
            /*
             * Gather STATUS for each entry
             */
            for (final Iterator<ListLsubEntryImpl> iter = primary.values().iterator(); iter.hasNext();) {
                final ListLsubEntryImpl listEntry = iter.next();
                if (listEntry.canOpen()) {
                    try {
                        final String fullName = listEntry.getFullName();
                        final int[] status = IMAPCommandsCollection.getStatus(fullName, imapFolder);
                        if (null != status) {
                            listEntry.setStatus(status);
                            final ListLsubEntryImpl lsubEntry = lookup.get(fullName);
                            if (null != lsubEntry) {
                                lsubEntry.setStatus(status);
                            }
                        }
                    } catch (final Exception e) {
                        // Swallow failed STATUS command
                        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ListLsubCollection.class)).debug(
                            "STATUS command failed for " + imapFolder.getStore().toString(),
                            e);
                    }
                }
            }
        }
        /*
         * ACLs if enabled
         */
        if (doGetAcl) {
            initACLs(imapFolder);
        }
        if (DEBUG) {
            final long dur = System.currentTimeMillis() - st;
            final StringBuilder sb = new StringBuilder(128);
            sb.append("LIST/LSUB cache");
            if (doStatus || doGetAcl) {
                sb.append(" (");
                if (doStatus) {
                    sb.append(" including STATUS");
                }
                if (doGetAcl) {
                    sb.append(" including GETACL");
                }
                sb.append(')');
            }
            sb.append(" built in ").append(dur).append("msec.");
            LOG.debug(sb.toString());
        }
        /*
         * Set time stamp
         */
        stamp = System.currentTimeMillis();
        deprecated.set(false);
    }

    /**
     * Initializes ACL lists.
     * 
     * @param imapFolder The IMAP folder to obtain <tt>IMAPProtocol</tt> instance from
     * @throws MessagingException If ACL capability cannot be checked
     */
    public void initACLs(final IMAPFolder imapFolder) throws MessagingException {
        if (!((IMAPStore) imapFolder.getStore()).hasCapability("ACL")) {
            return;
        }
        final long st = DEBUG ? System.currentTimeMillis() : 0L;
        final ConcurrentMap<String, ListLsubEntryImpl> primary;
        final ConcurrentMap<String, ListLsubEntryImpl> lookup;
        if (listMap.size() > lsubMap.size()) {
            primary = lsubMap;
            lookup = listMap;
        } else {
            primary = listMap;
            lookup = lsubMap;
        }
        /*
         * Perform GETACL command for each entry
         */
        for (final Iterator<ListLsubEntryImpl> iter = primary.values().iterator(); iter.hasNext();) {
            final ListLsubEntryImpl listEntry = iter.next();
            if (listEntry.canOpen()) {
                try {
                    final String fullName = listEntry.getFullName();
                    final List<ACL> aclList = IMAPCommandsCollection.getAcl(fullName, imapFolder, false);
                    listEntry.setAcls(aclList);
                    final ListLsubEntryImpl lsubEntry = lookup.get(fullName);
                    if (null != lsubEntry) {
                        lsubEntry.setAcls(aclList);
                    }
                } catch (final Exception e) {
                    // Swallow failed ACL command
                    com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(ListLsubCollection.class)).debug(
                        "ACL/MYRIGHTS command failed for " + imapFolder.getStore().toString(),
                        e);
                }
            }
        }
        if (DEBUG) {
            final long dur = System.currentTimeMillis() - st;
            final StringBuilder sb = new StringBuilder(64);
            sb.append("LIST/LSUB cache built GETACL entries in ").append(dur).append("msec.");
            LOG.debug(sb.toString());
        }
    }

    private void checkConsistency() {
        final ListLsubEntryImpl rootEntry = listMap.get(ROOT_FULL_NAME);
        /*
         * Ensure every LSUB'ed entry occurs in LIST'ed entries
         */
        for (final Entry<String, ListLsubEntryImpl> entry : new TreeMap<String, ListLsubEntryImpl>(lsubMap).entrySet()) {
            final String fullName = entry.getKey();
            if (!listMap.containsKey(fullName)) {
                /*
                 * Distinguish between personal and other namespace
                 */
                if (isNamespace(fullName)) {
                    /*
                     * Either shared or user namespace
                     */
                    final ListLsubEntryImpl lle = new ListLsubEntryImpl(entry.getValue(), true);
                    listMap.put(fullName, lle);
                    /*
                     * Determine parent
                     */
                    final int pos = fullName.lastIndexOf(lle.getSeparator());
                    if (pos >= 0) {
                        /*
                         * Non-root level
                         */
                        final String parentFullName = fullName.substring(0, pos);
                        final ListLsubEntryImpl parent = listMap.get(parentFullName);
                        {
                            lle.setParent(parent);
                            parent.addChild(lle);
                        }
                    } else {
                        /*
                         * Root level
                         */
                        lle.setParent(rootEntry);
                        rootEntry.addChild(lle);
                    }
                } else {
                    /*
                     * A personal full name: Drop LSUB'ed entries which do not occur in LIST'ed entries.
                     */
                    final ListLsubEntryImpl lle = entry.getValue();
                    dropEntryFrom(lle, listMap);
                }
            }
        }
    }

    private static void dropEntryFrom(final ListLsubEntryImpl lle, final ConcurrentMap<String, ListLsubEntryImpl> map) {
        final Set<ListLsubEntryImpl> tmp = new HashSet<ListLsubEntryImpl>(lle.getChildrenSet());
        for (final ListLsubEntryImpl child : tmp) {
            dropEntryFrom(child, map);
        }
        map.remove(lle.getFullName());
        /*
         * Drop from parent's children
         */
        final ListLsubEntryImpl p = lle.getParentImpl();
        if (null != p) {
            p.removeChild(lle);
        }
    }

    /**
     * Gets current entry for specified full name.
     *
     * @param fullName The full name of the starting folder node
     * @param imapFolder The connected IMAP folder
     * @throws MailException If update fails
     */
    public ListLsubEntry getActualEntry(final String fullName, final IMAPFolder imapFolder) throws OXException {
        try {
            /*
             * Perform LIST "" <full-name>
             */
            return (ListLsubEntry) imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

                @Override
                public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                    return doSingleListCommandWithLsub(protocol, fullName);
                }

            });
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Updates a sub-tree starting at specified full name.
     *
     * @param fullName The full name of the starting folder node
     * @param imapStore The connected IMAP store
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws OXException If update fails
     */
    public void update(final String fullName, final IMAPStore imapStore, final boolean doStatus, final boolean doGetAcl) throws OXException {
        try {
            update(fullName, (IMAPFolder) imapStore.getFolder("INBOX"), doStatus, doGetAcl);
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Updates a sub-tree starting at specified full name.
     *
     * @param fullName The full name of the starting folder node
     * @param imapFolder An IMAP folder providing connected protocol
     * @param doStatus Whether STATUS command shall be performed
     * @param doGetAcl Whether ACL command shall be performed
     * @throws MessagingException If a messaging error occurs
     */
    public void update(final String fullName, final IMAPFolder imapFolder, final boolean doStatus, final boolean doGetAcl) throws MessagingException {
        if (deprecated.get() || ROOT_FULL_NAME.equals(fullName)) {
            init(true, imapFolder, doStatus, doGetAcl);
            return;
        }
        /*
         * Do a full re-build anyway...
         */
        init(true, imapFolder, doStatus, doGetAcl);
    }

    /**
     * Performs a dummy LSUB "" "" which seems to reveal folders which got not displayed before... Please don't ask why.
     *
     * @param protocol The IMAP protocol
     */
    protected void doDummyLsub(final IMAPProtocol protocol) {
        final Response[] r = protocol.command("LSUB \"\" \"\"", null);
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            return;
        }
        try {
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            protocol.handleResult(response);
        } catch (final ProtocolException e) {
            LOG.warn("Dummy >>LSUB \"\" \"\"<< command failed.", e);
        }
    }

    private static final Set<String> ATTRIBUTES_NON_EXISTING_PARENT = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "\\noselect",
        "\\haschildren")));

    private static final Set<String> ATTRIBUTES_NON_EXISTING_NAMESPACE = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
        "\\noselect",
        "\\hasnochildren")));

    /**
     * Performs a LIST/LSUB command with specified IMAP protocol.
     *
     * @param protocol The IMAP protocol
     * @param lsub <code>true</code> to perform a LSUB command; otherwise <code>false</code> for LIST
     * @throws ProtocolException If a protocol error occurs
     */
    protected void doListLsubCommand(final IMAPProtocol protocol, final boolean lsub) throws ProtocolException {
        /*
         * Perform command
         */
        final String command = lsub ? "LSUB" : "LIST";
        final Response[] r;
        if (DEBUG) {
            final String sCmd = new StringBuilder(command).append(" \"\" \"*\"").toString();
            r = protocol.command(sCmd, null);
            LOG.debug((command) + " cache filled with >>" + sCmd + "<< which returned " + r.length + " response line(s).");
        } else {
            r = protocol.command(new StringBuilder(command).append(" \"\" \"*\"").toString(), null);
        }
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            final ConcurrentMap<String, ListLsubEntryImpl> map = lsub ? lsubMap : listMap;
            final Map<String, List<ListLsubEntryImpl>> parentMap = new HashMap<String, List<ListLsubEntryImpl>>(4);
            final ListLsubEntryImpl rootEntry = map.get(ROOT_FULL_NAME);
            /*
             * Get sorted responses
             */
            final List<ListLsubEntryImpl> listResponses = sortedListResponses(r, command, lsub);
            char separator = '\0';
            for (final ListLsubEntryImpl next : listResponses) {
                ListLsubEntryImpl listLsubEntry = next;
                /*
                 * Check for MBox format while iterating LIST/LSUB responses.
                 */
                if (listLsubEntry.hasInferiors() && listLsubEntry.canOpen()) {
                    mbox = Boolean.FALSE;
                }
                final String fullName = listLsubEntry.getFullName();
                /*
                 * (Re-)Set children
                 */
                {
                    final ListLsubEntryImpl oldEntry = map.get(fullName);
                    if (oldEntry == null) {
                        /*
                         * Wasn't in map before
                         */
                        map.put(fullName, listLsubEntry);
                    } else {
                        /*
                         * Already contained in map
                         */
                        oldEntry.clearChildren();
                        oldEntry.copyFrom(listLsubEntry);
                        listLsubEntry = oldEntry;
                    }
                }
                /*
                 * Determine parent
                 */
                final int pos = fullName.lastIndexOf((separator = listLsubEntry.getSeparator()));
                if (pos >= 0) {
                    /*
                     * Non-root level
                     */
                    final String parentFullName = fullName.substring(0, pos);
                    final ListLsubEntryImpl parent = map.get(parentFullName);
                    if (null == parent) {
                        /*
                         * Parent not (yet) in map
                         */
                        List<ListLsubEntryImpl> children = parentMap.get(parentFullName);
                        if (null == children) {
                            children = new ArrayList<ListLsubCollection.ListLsubEntryImpl>(8);
                            parentMap.put(parentFullName, children);
                        }
                        children.add(listLsubEntry);
                    } else {
                        listLsubEntry.setParent(parent);
                        parent.addChild(listLsubEntry);
                    }
                } else {
                    /*
                     * Root level
                     */
                    listLsubEntry.setParent(rootEntry);
                    rootEntry.addChild(listLsubEntry);
                }
            } // End of for loop
            if (!parentMap.isEmpty()) {
                /*
                 * Handle parent map
                 */
                handleParentMap(parentMap, separator, rootEntry, lsub, map);
            }
            /*
             * Check namespace folders
             */
            if (!lsub) {
                handleNamespaces(map, rootEntry, separator);
            }
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
        } else {
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            protocol.handleResult(response);
        }
    }

    private void handleNamespaces(final ConcurrentMap<String, ListLsubEntryImpl> map, final ListLsubEntryImpl rootEntry, final char separator) {
        for (final String sharedNamespace : shared) {
            final ListLsubEntryImpl entry = map.get(sharedNamespace);
            if (null == entry) {
                final ListLsubEntryImpl namespaceFolder =
                    new ListLsubEntryImpl(
                        sharedNamespace,
                        ATTRIBUTES_NON_EXISTING_NAMESPACE,
                        separator,
                        ListLsubEntry.ChangeState.UNDEFINED,
                        true,
                        false,
                        Boolean.FALSE,
                        lsubMap).setNamespace(true);
                namespaceFolder.setParent(rootEntry);
                rootEntry.addChildIfAbsent(namespaceFolder);
                map.put(sharedNamespace, namespaceFolder);
            } else {
                entry.setCanOpen(false);
            }
        }
        for (final String userNamespace : user) {
            final ListLsubEntryImpl entry = map.get(userNamespace);
            if (null == entry) {
                final ListLsubEntryImpl namespaceFolder =
                    new ListLsubEntryImpl(
                        userNamespace,
                        ATTRIBUTES_NON_EXISTING_NAMESPACE,
                        separator,
                        ListLsubEntry.ChangeState.UNDEFINED,
                        true,
                        false,
                        Boolean.FALSE,
                        lsubMap).setNamespace(true);
                namespaceFolder.setParent(rootEntry);
                rootEntry.addChildIfAbsent(namespaceFolder);
                map.put(userNamespace, namespaceFolder);
            } else {
                entry.setCanOpen(false);
            }
        }
    }

    private List<ListLsubEntryImpl> sortedListResponses(final Response[] r, final String command, final boolean lsub) {
        final List<ListLsubEntryImpl> list = new ArrayList<ListLsubCollection.ListLsubEntryImpl>(r.length);
        for (int i = 0, len = r.length - 1; i < len; i++) {
            if (!(r[i] instanceof IMAPResponse)) {
                continue;
            }
            final IMAPResponse ir = (IMAPResponse) r[i];
            if (ir.keyEquals(command)) {
                final ListLsubEntryImpl entryImpl = parseListResponse(ir, lsub ? null : lsubMap);
                list.add(entryImpl);
                r[i] = null;
            }
        }
        Collections.sort(list);
        return list;
    }

    /**
     * Handles specified parent map.
     *
     * @param parentMap The parent map
     * @param separator The separator character
     * @param rootEntry The root entry
     * @param lsub <code>true</code> for <code>LSUB</code>; otherwise <code>false</code> for <code>LIST</code>
     * @param map The entry map
     * @param set The set of full names
     * @param add <code>true</code> to add to <code>set</code> parameter; otherwise <code>false</code> to remove from it
     */
    private void handleParentMap(final Map<String, List<ListLsubEntryImpl>> parentMap, final char separator, final ListLsubEntryImpl rootEntry, final boolean lsub, final ConcurrentMap<String, ListLsubEntryImpl> map) {
        /*
         * Handle children
         */
        boolean handleChildren = true;
        while (handleChildren) {
            handleChildren = false;
            String grandFullName = null;
            ListLsubEntryImpl newEntry = null;
            Next: for (final Entry<String, List<ListLsubEntryImpl>> entry : parentMap.entrySet()) {
                final String parentFullName = entry.getKey();
                ListLsubEntryImpl parent = map.get(parentFullName);
                if (null == parent) {
                    /*
                     * Add dummy parent
                     */
                    parent =
                        new ListLsubEntryImpl(
                            parentFullName,
                            ATTRIBUTES_NON_EXISTING_PARENT,
                            separator,
                            ListLsubEntry.ChangeState.UNDEFINED,
                            true,
                            false,
                            Boolean.TRUE,
                            lsub ? null : lsubMap);
                    if (isNamespace(parentFullName)) {
                        parent.setNamespace(true);
                        if (equalsNamespace(parentFullName)) {
                            parent.setCanOpen(false);
                        }
                    }
                    map.put(parentFullName, parent);
                    final int pos = parentFullName.lastIndexOf(separator);
                    if (pos >= 0) {
                        grandFullName = parentFullName.substring(0, pos);
                        newEntry = parent;
                        break Next;
                    }
                    /*
                     * Grand parent is root folder
                     */
                    parent.setParent(rootEntry);
                    rootEntry.addChildIfAbsent(parent);
                }
                for (final ListLsubEntryImpl child : entry.getValue()) {
                    child.setParent(parent);
                    parent.addChildIfAbsent(child);
                }
            }
            if (grandFullName != null && newEntry != null) {
                List<ListLsubEntryImpl> children = parentMap.get(grandFullName);
                if (null == children) {
                    children = new ArrayList<ListLsubCollection.ListLsubEntryImpl>(8);
                    parentMap.put(grandFullName, children);
                }
                if (!children.contains(newEntry)) {
                    children.add(newEntry);
                }
                /*
                 * Next loop...
                 */
                handleChildren = true;
            }
        }
    }

    /**
     * Performs a LIST command for root folder with specified IMAP protocol.
     *
     * @param protocol The IMAP protocol
     * @throws ProtocolException If a protocol error occurs
     */
    protected void doRootListCommand(final IMAPProtocol protocol) throws ProtocolException {
        doDummyLsub(protocol);
        /*
         * Perform command: LIST "" ""
         */
        final Response[] r = protocol.command("LIST \"\" \"\"", null);
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            final String cmd = "LIST";
            for (int i = 0, len = r.length; i < len; i++) {
                if (!(r[i] instanceof IMAPResponse)) {
                    continue;
                }
                final IMAPResponse ir = (IMAPResponse) r[i];
                if (ir.keyEquals(cmd)) {
                    final ListLsubEntryImpl listLsubEntry = parseListResponse(ir, null);
                    {
                        final ListLsubEntryImpl oldEntry = listMap.get(ROOT_FULL_NAME);
                        if (null == oldEntry) {
                            listMap.put(ROOT_FULL_NAME, listLsubEntry);
                            lsubMap.put(ROOT_FULL_NAME, listLsubEntry);
                        } else {
                            oldEntry.clearChildren();
                            oldEntry.copyFrom(listLsubEntry);
                        }
                    }
                    r[i] = null;
                }
            }
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
        } else {
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            protocol.handleResult(response);
        }
    }

    /**
     * Performs a LIST command for a single folder with specified IMAP protocol.
     *
     * @param protocol The IMAP protocol
     * @param fullName The full name
     * @throws ProtocolException If a protocol error occurs
     */
    protected ListLsubEntryImpl doSingleListCommandWithLsub(final IMAPProtocol protocol, final String fullName) throws ProtocolException {
        doDummyLsub(protocol);
        /*
         * Perform command: LIST "" <full-name>
         */
        final String mbox = BASE64MailboxEncoder.encode(fullName);
        final Argument args = new Argument();   
        args.writeString(mbox);
        final Response[] r = protocol.command("LIST \"\"", args);
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            ListLsubEntryImpl listLsubEntry = null;
            for (int i = 0, len = r.length; i < len; i++) {
                if (!(r[i] instanceof IMAPResponse)) {
                    continue;
                }
                final IMAPResponse ir = (IMAPResponse) r[i];
                if (null == listLsubEntry && ir.keyEquals("LIST")) {
                    listLsubEntry = parseListResponse(ir, null);
                    r[i] = null;
                }
            }
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            if (null != listLsubEntry) {
                /*
                 * Check subscription status
                 */
                listLsubEntry.setSubscribed(doSubscriptionCheck(protocol, mbox));
            }
            return listLsubEntry;
        }
        /*
         * Dispatch remaining untagged responses
         */
        protocol.notifyResponseHandlers(r);
        protocol.handleResult(response);
        return null; // Never reached if response is not OK
    }

    /**
     * Performs a check if denoted folder is subscribed.
     *
     * @param protocol The IMAP protocol
     * @param mbox The encoded full name
     * @return <code>true</code> if subscribed; otherwise <code>false</code>
     * @throws ProtocolException If a protocol error occurs
     */
    private boolean doSubscriptionCheck(final IMAPProtocol protocol, final String mbox) throws ProtocolException {
        /*
         * Perform command: LIST "" <full-name>
         */
        final Argument args = new Argument();   
        args.writeString(mbox);
        final Response[] r = protocol.command("LSUB \"\"", args);
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            boolean ret = false;
            for (int i = 0, len = r.length; i < len; i++) {
                if (!(r[i] instanceof IMAPResponse)) {
                    continue;
                }
                final IMAPResponse ir = (IMAPResponse) r[i];
                if (ir.keyEquals("LSUB")) {
                    ret |= mbox.equals(parseEncodedFullName(ir));
                    r[i] = null;
                }
            }
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            return ret;
        }
        /*
         * Dispatch remaining untagged responses
         */
        protocol.notifyResponseHandlers(r);
        protocol.handleResult(response);
        return false; // Never reached if response is not OK
    }

    /**
     * Adds single entry to collection.
     *
     * @param fullName The full name
     * @param imapStore The IMAP store
     * @param doStatus Whether to perform STATUS command
     * @param doGetAcl Whether to perform GETACL command
     * @throws OXException If operation fails
     */
    public void addSingle(final String fullName, final IMAPStore imapStore, final boolean doStatus, final boolean doGetAcl) throws OXException {
        try {
            addSingle(fullName, (IMAPFolder) imapStore.getFolder("INBOX"), doStatus, doGetAcl);
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Adds single entry to collection.
     *
     * @param fullName The full name
     * @param imapFolder The IMAP folder
     * @param doStatus Whether to perform STATUS command
     * @param doGetAcl Whether to perform GETACL command
     * @throws OXException If operation fails
     */
    public void addSingle(final String fullName, final IMAPFolder imapFolder, final boolean doStatus, final boolean doGetAcl) throws OXException {
        try {
            imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

                @Override
                public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                    doSingleListCommand(fullName, protocol, false);
                    return null;
                }

            });

            imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

                @Override
                public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                    doSingleListCommand(fullName, protocol, true);
                    return null;
                }

            });

            doOther(fullName, imapFolder, doStatus, doGetAcl);
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Performs a LIST/LSUB command for a single folder with specified IMAP protocol.
     *
     * @param fullName The full name
     * @param protocol The IMAP protocol
     * @throws ProtocolException If a protocol error occurs
     */
    protected ListLsubEntryImpl doSingleListCommand(final String fullName, final IMAPProtocol protocol, final boolean lsub) throws ProtocolException {
        if (!lsub) {
            doDummyLsub(protocol);
        }
        /*
         * Perform command: LIST "" "INBOX"
         */
        final String command = lsub ? "LSUB" : "LIST";
        String mbox = BASE64MailboxEncoder.encode(fullName);
        Argument args = new Argument();
        args.writeString(mbox);
        final Response[] r = protocol.command(new StringBuilder(command).append(" \"\"").toString(), args);
        args = null;
        mbox = null;
        final Response response = r[r.length - 1];
        if (response.isOK()) {
            ListLsubEntryImpl retval = null;
            final ConcurrentMap<String, ListLsubEntryImpl> map = lsub ? lsubMap : listMap;
            for (int i = 0, len = r.length; i < len; i++) {
                if (!(r[i] instanceof IMAPResponse)) {
                    continue;
                }
                final IMAPResponse ir = (IMAPResponse) r[i];
                if (ir.keyEquals(command)) {
                    final ListLsubEntryImpl listLsubEntry = parseListResponse(ir, null);
                    retval = listLsubEntry;
                    {
                        final ListLsubEntryImpl oldEntry = map.get(fullName);
                        final ListLsubEntryImpl parent;
                        if (null != oldEntry) {
                            for (final ListLsubEntryImpl child : oldEntry.getChildrenSet()) {
                                child.setParent(listLsubEntry);
                                listLsubEntry.addChild(child);
                            }
                            parent = oldEntry.getParentImpl();
                        } else {
                            final int pos = fullName.lastIndexOf(listLsubEntry.getSeparator());
                            if (pos > 0) {
                                final String parentFullName = fullName.substring(0, pos);
                                final ListLsubEntryImpl tmp = map.get(parentFullName);
                                parent = null == tmp ? doSingleListCommand(parentFullName, protocol, lsub) : tmp;
                            } else {
                                parent = map.get(ROOT_FULL_NAME);
                            }
                        }
                        if (null != parent) {
                            listLsubEntry.setParent(parent);
                            parent.addChild(listLsubEntry);
                        }
                    }
                    map.put(fullName, listLsubEntry);
                    r[i] = null;
                }
            }
            /*
             * Dispatch remaining untagged responses
             */
            protocol.notifyResponseHandlers(r);
            /*
             * Debug logs
             */
            if (DEBUG) {
                final TreeMap<String, ListLsubEntryImpl> tm = new TreeMap<String, ListLsubEntryImpl>();
                tm.putAll(map);
                final StringBuilder sb = new StringBuilder(1024);
                sb.append((lsub ? "LSUB" : "LIST") + " cache contains after adding single entry \"");
                sb.append(fullName).append("\":\n");
                for (final Entry<String, ListLsubEntryImpl> entry : tm.entrySet()) {
                    sb.append('"').append(entry.getKey()).append("\"=").append(entry.getValue()).append('\n');
                }
                LOG.debug(sb.toString());
            }
            return retval;
        }
        /*
         * Dispatch remaining untagged responses
         */
        protocol.notifyResponseHandlers(r);
        protocol.handleResult(response);
        /*
         * Never reached...
         */
        return null;
    }

    private void doOther(final String fullName, final IMAPFolder imapFolder, final boolean doStatus, final boolean doGetAcl) throws OXException {
        try {
            final ListLsubEntryImpl listEntry = listMap.get(fullName);
            if (null == listEntry) {
                return;
            }
            if (doStatus) {
                /*
                 * Do STATUS
                 */
                if (listEntry.canOpen()) {
                    try {
                        final int[] status = IMAPCommandsCollection.getStatus(fullName, imapFolder);
                        if (null != status) {
                            listEntry.setStatus(status);
                            final ListLsubEntryImpl lsubEntry = lsubMap.get(fullName);
                            if (null != lsubEntry) {
                                lsubEntry.setStatus(status);
                            }
                        }
                    } catch (final Exception e) {
                        // Swallow failed STATUS command
                        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ListLsubCollection.class)).debug(
                            "STATUS command failed for " + imapFolder.getStore().toString(),
                            e);
                    }
                }
            }
            if (doGetAcl && ((IMAPStore) imapFolder.getStore()).hasCapability("ACL")) {
                /*
                 * Do GETACL
                 */
                if (listEntry.canOpen()) {
                    try {
                        final List<ACL> aclList = IMAPCommandsCollection.getAcl(fullName, imapFolder, false);
                        listEntry.setAcls(aclList);
                        final ListLsubEntryImpl lsubEntry = lsubMap.get(fullName);
                        if (null != lsubEntry) {
                            lsubEntry.setAcls(aclList);
                        }
                    } catch (final Exception e) {
                        // Swallow failed ACL command
                        com.openexchange.log.Log.valueOf(com.openexchange.log.LogFactory.getLog(ListLsubCollection.class)).debug(
                            "ACL/MYRIGHTS command failed for " + imapFolder.getStore().toString(),
                            e);
                    }
                }
            }
        } catch (final MessagingException e) {
            throw MimeMailException.handleMessagingException(e);
        }
    }

    /**
     * Gets the time stamp when last initialization was performed.
     *
     * @return The stamp of last initialization
     */
    public long getStamp() {
        return stamp;
    }

    /**
     * Checks for any subscribed subfolder in IMAP folder tree located below denoted folder.
     *
     * @param fullName The full name
     * @return <code>true</code> if a subscribed subfolder exists; otherwise <code>false</code>
     */
    public boolean hasAnySubscribedSubfolder(final String fullName) {
        checkDeprecated();
        final ListLsubEntryImpl parent = lsubMap.get(fullName);
        if (null != parent && !parent.getChildrenSet().isEmpty()) {
            return true;
        }
        final Iterator<Entry<String, ListLsubEntryImpl>> iter = lsubMap.entrySet().iterator();
        if (!iter.hasNext()) {
            return false;
        }
        final String prefix;
        {
            final Entry<String, ListLsubEntryImpl> entry = iter.next();
            prefix = fullName + entry.getValue().getSeparator();
            if (entry.getKey().startsWith(prefix)) {
                return true;
            }
        }
        while (iter.hasNext()) {
            final Entry<String, ListLsubEntryImpl> entry = iter.next();
            if (entry.getKey().startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Gets the LIST entry for specified full name.
     *
     * @param fullName The full name
     * @return The LIST entry for specified full name or <code>null</code>
     */
    public ListLsubEntry getList(final String fullName) {
        checkDeprecated();
        return listMap.get(fullName);
    }

    /**
     * Gets the LSUB entry for specified full name.
     *
     * @param fullName The full name
     * @return The LSUB entry for specified full name or <code>null</code>
     */
    public ListLsubEntry getLsub(final String fullName) {
        checkDeprecated();
        return lsubMap.get(fullName);
    }

    private static final TObjectIntHashMap<String> POS_MAP;

    static {
        POS_MAP = new TObjectIntHashMap<String>(6);
        POS_MAP.put("\\marked", 1);
        POS_MAP.put("\\unmarked", 2);
        POS_MAP.put("\\noselect", 3);
        POS_MAP.put("\\noinferiors", 4);
        POS_MAP.put("\\haschildren", 5);
        POS_MAP.put("\\hasnochildren", 6);
    }

    private ListLsubEntryImpl parseListResponse(final IMAPResponse listResponse, final ConcurrentMap<String, ListLsubEntryImpl> lsubMap) {
        /*
         * LIST (\NoInferiors \UnMarked) "/" "Sent Items"
         */
        final String[] s = listResponse.readSimpleList();
        /*
         * Check attributes
         */
        final Set<String> attributes;
        ListLsubEntry.ChangeState changeState = ListLsubEntry.ChangeState.UNDEFINED;
        boolean canOpen = true;
        boolean hasInferiors = true;
        Boolean hasChildren = null;
        if (s != null) {
            /*
             * Non-empty attribute list
             */
            attributes = new HashSet<String>(s.length);
            for (int i = 0; i < s.length; i++) {
                final String attr = s[i].toLowerCase(Locale.US);
                switch (POS_MAP.get(attr)) {
                case 1:
                    changeState = ListLsubEntry.ChangeState.CHANGED;
                    break;
                case 2:
                    changeState = ListLsubEntry.ChangeState.UNCHANGED;
                    break;
                case 3:
                    canOpen = false;
                    break;
                case 4:
                    hasInferiors = false;
                    break;
                case 5:
                    hasChildren = Boolean.TRUE;
                    break;
                case 6:
                    hasChildren = Boolean.FALSE;
                    break;
                default:
                    // Nothing
                    break;
                }
                attributes.add(attr);
            }
        } else {
            attributes = Collections.emptySet();
        }
        /*
         * Read separator character
         */
        char separator = '/';
        listResponse.skipSpaces();
        if (listResponse.readByte() == '"') {
            if ((separator = (char) listResponse.readByte()) == '\\') {
                /*
                 * Escaped separator character
                 */
                separator = (char) listResponse.readByte();
            }
            listResponse.skip(1);
        } else {
            listResponse.skip(2);
        }
        /*
         * Read full name; decode the name (using RFC2060's modified UTF7)
         */
        listResponse.skipSpaces();
        final String name = BASE64MailboxDecoder.decode(listResponse.readAtomString());
        /*
         * Return
         */
        return new ListLsubEntryImpl(name, attributes, separator, changeState, hasInferiors, canOpen, hasChildren, lsubMap).setNamespace(isNamespace(name));
    }

    private String parseEncodedFullName(final IMAPResponse listResponse) {
        /*-
         * LIST (\NoInferiors \UnMarked) "/" "Sent Items"
         *
         * Consume attributes
         */
        listResponse.readSimpleList();
        /*
         * Read separator character
         */
        listResponse.skipSpaces();
        if (listResponse.readByte() == '"') {
            if (((char) listResponse.readByte()) == '\\') {
                /*
                 * Escaped separator character
                 */
                listResponse.readByte();
            }
            listResponse.skip(1);
        } else {
            listResponse.skip(2);
        }
        /*
         * Read full name; decode the name (using RFC2060's modified UTF7)
         */
        listResponse.skipSpaces();
        return listResponse.readAtomString();
    }

    /**
     * Creates an empty {@link ListLsubEntry} for specified full name.
     *
     * @param fullName The full name
     * @return An empty {@link ListLsubEntry}
     */
    protected static ListLsubEntry emptyEntryFor(final String fullName) {
        return new EmptyListLsubEntry(fullName);
    }

    private static class EmptyListLsubEntry implements ListLsubEntry {

        private final String fullName;

        public EmptyListLsubEntry(final String fullName) {
            super();
            this.fullName = fullName;
        }

        @Override
        public String getName() {
            return fullName.substring(fullName.lastIndexOf('/') + 1);
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public ListLsubEntry getParent() {
            return null;
        }

        @Override
        public List<ListLsubEntry> getChildren() {
            return Collections.emptyList();
        }

        @Override
        public String getFullName() {
            return fullName;
        }

        @Override
        public Set<String> getAttributes() {
            return Collections.emptySet();
        }

        @Override
        public char getSeparator() {
            return '/';
        }

        @Override
        public ListLsubEntry.ChangeState getChangeState() {
            return ListLsubEntry.ChangeState.UNCHANGED;
        }

        @Override
        public boolean hasInferiors() {
            return false;
        }

        @Override
        public boolean canOpen() {
            return false;
        }

        @Override
        public int getType() {
            return 0;
        }

        @Override
        public boolean isSubscribed() {
            return false;
        }

        @Override
        public int getMessageCount() {
            return -1;
        }

        @Override
        public int getNewMessageCount() {
            return -1;
        }

        @Override
        public int getUnreadMessageCount() {
            return -1;
        }

        @Override
        public List<ACL> getACLs() {
            return null;
        }

        @Override
        public void rememberACLs(final List<ACL> aclList) {
            // Nothing to do
        }

        @Override
        public void rememberCounts(final int total, final int recent, final int unseen) {
            // Nothing to do
        }

        @Override
        public boolean isNamespace() {
            return false;
        }

        @Override
        public boolean hasChildren() {
            return false;
        }

        @Override
        public Rights getMyRights() {
            return null;
        }

    }

    /**
     * A LIST/LSUB entry.
     */
    private static final class ListLsubEntryImpl implements ListLsubEntry, Comparable<ListLsubEntryImpl> {

        private ListLsubEntryImpl parent;

        private Set<ListLsubEntryImpl> children;

        private int[] status;

        private final String fullName;

        private Set<String> attributes;

        private char separator;

        private ChangeState changeState;

        private boolean hasInferiors;

        private boolean canOpen;

        private boolean namespace;

        private int type;

        private final ConcurrentMap<String, ListLsubEntryImpl> lsubMap;

        private List<ACL> acls;

        private Boolean hasChildren;

        private Rights myRights;

        private Boolean subscribed;

        protected ListLsubEntryImpl(final String fullName, final Set<String> attributes, final char separator, final ChangeState changeState, final boolean hasInferiors, final boolean canOpen, final Boolean hasChildren, final ConcurrentMap<String, ListLsubEntryImpl> lsubMap) {
            super();
            this.fullName =
                String.valueOf(separator).equals(fullName) ? ROOT_FULL_NAME : (INBOX.equalsIgnoreCase(fullName) ? INBOX : fullName);
            this.attributes = attributes;
            this.separator = separator;
            this.changeState = changeState;
            this.hasInferiors = hasInferiors;
            this.canOpen = canOpen;
            this.hasChildren = hasChildren;
            int type = 0;
            if (hasInferiors) {
                type |= Folder.HOLDS_FOLDERS;
            }
            if (canOpen) {
                type |= Folder.HOLDS_MESSAGES;
            }
            this.type = type;
            this.lsubMap = lsubMap;
        }

        protected ListLsubEntryImpl(final ListLsubEntryImpl newEntry, final boolean subscribed) {
            super();
            fullName = newEntry.fullName;
            attributes = newEntry.attributes;
            canOpen = newEntry.canOpen;
            changeState = newEntry.changeState;
            hasInferiors = newEntry.hasInferiors;
            separator = newEntry.separator;
            type = newEntry.type;
            namespace = newEntry.namespace;
            hasChildren = newEntry.hasChildren;
            this.subscribed = Boolean.valueOf(subscribed);
            lsubMap = null;
        }

        protected void copyFrom(final ListLsubEntryImpl newEntry) {
            if (newEntry == null) {
                return;
            }
            attributes = newEntry.attributes;
            canOpen = newEntry.canOpen;
            changeState = newEntry.changeState;
            hasInferiors = newEntry.hasInferiors;
            separator = newEntry.separator;
            type = newEntry.type;
            namespace = newEntry.namespace;
            hasChildren = newEntry.hasChildren;
        }

        protected void clearChildren() {
            if (children != null) {
                children.clear();
            }
        }

        @Override
        public String getName() {
            return fullName.substring(fullName.lastIndexOf(separator) + 1);
        }

        /**
         * Sets this LIST/LSUB entry's parent.
         *
         * @param parent The parent
         */
        protected void setParent(final ListLsubEntryImpl parent) {
            this.parent = parent;
        }

        @Override
        public ListLsubEntry getParent() {
            return parent;
        }

        /**
         * Gets the parent.
         *
         * @return The parent
         */
        protected ListLsubEntryImpl getParentImpl() {
            return parent;
        }

        /**
         * Adds specified LIST/LSUB entry to this LIST/LSUB entry's children
         *
         * @param child The child LIST/LSUB entry
         */
        protected void addChild(final ListLsubEntryImpl child) {
            if (null == child) {
                return;
            }
            if (null == children) {
                children = new HashSet<ListLsubEntryImpl>(8);
                children.add(child);
            } else {
                if (!children.add(child)) {
                    /*
                     * Remove previous entry and add again
                     */
                    children.remove(child);
                    children.add(child);
                }
            }
        }

        /**
         * Removes specified LIST/LSUB entry from this LIST/LSUB entry's children
         *
         * @param child The child LIST/LSUB entry
         */
        protected void removeChild(final ListLsubEntryImpl child) {
            if (null == child) {
                return;
            }
            if (null == children) {
                return;
            }
            children.remove(child);
        }

        /**
         * Adds (if absent) specified LIST/LSUB entry to this LIST/LSUB entry's children
         *
         * @param child The child LIST/LSUB entry
         */
        protected void addChildIfAbsent(final ListLsubEntryImpl child) {
            if (null == child) {
                return;
            }
            if (null == children) {
                children = new HashSet<ListLsubEntryImpl>(8);
            }
            children.add(child);
        }

        @Override
        public List<ListLsubEntry> getChildren() {
            return null == children ? Collections.<ListLsubEntry> emptyList() : new ArrayList<ListLsubEntry>(children);
        }

        protected Set<ListLsubEntryImpl> getChildrenSet() {
            return null == children ? Collections.<ListLsubEntryImpl> emptySet() : children;
        }

        @Override
        public String getFullName() {
            return fullName;
        }

        @Override
        public Set<String> getAttributes() {
            return attributes;
        }

        @Override
        public char getSeparator() {
            return separator;
        }

        @Override
        public ChangeState getChangeState() {
            return changeState;
        }

        @Override
        public boolean hasInferiors() {
            return hasInferiors;
        }

        protected ListLsubEntryImpl setCanOpen(final boolean canOpen) {
            this.canOpen = canOpen;
            return this;
        }

        @Override
        public boolean canOpen() {
            return canOpen;
        }

        @Override
        public int getType() {
            return type;
        }

        @Override
        public boolean exists() {
            return true;
        }

        protected void setSubscribed(final boolean subscribed) {
            this.subscribed = Boolean.valueOf(subscribed);
        }

        @Override
        public boolean isSubscribed() {
            return null == subscribed ? (null == lsubMap ? true : lsubMap.containsKey(fullName)) : subscribed.booleanValue();
        }

        /**
         * Sets the status.
         *
         * @param status The status
         */
        protected void setStatus(final int[] status) {
            if (null == status) {
                this.status = null;
                return;
            }
            this.status = new int[status.length];
            System.arraycopy(status, 0, this.status, 0, status.length);
        }

        @Override
        public int getMessageCount() {
            return null == status ? -1 : status[0];
        }

        @Override
        public int getNewMessageCount() {
            return null == status ? -1 : status[1];
        }

        @Override
        public int getUnreadMessageCount() {
            return null == status ? -1 : status[2];
        }

        protected void setMyRights(final Rights myRights) {
            this.myRights = myRights;
        }

        /**
         * Gets MYRIGHTS.
         *
         * @return MYRIGHTS or <code>null</code> if absent
         */
        @Override
        public Rights getMyRights() {
            return myRights;
        }

        /**
         * Sets the ACLs.
         *
         * @param acls The ACL list
         */
        protected void setAcls(final List<ACL> acls) {
            this.acls = acls;
        }

        @Override
        public List<ACL> getACLs() {
            return acls == null ? null : new ArrayList<ACL>(acls);
        }

        @Override
        public void rememberACLs(final List<ACL> aclList) {
            this.acls = new ArrayList<ACL>(aclList);
        }

        @Override
        public void rememberCounts(final int total, final int recent, final int unseen) {
            if (null == status) {
                status = new int[3];
            }
            status[0] = total;
            status[1] = recent;
            status[2] = unseen;
        }

        @Override
        public int hashCode() {
            return ((fullName == null) ? 0 : fullName.hashCode());
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof ListLsubEntryImpl)) {
                return false;
            }
            final ListLsubEntryImpl other = (ListLsubEntryImpl) obj;
            if (fullName == null) {
                if (other.fullName != null) {
                    return false;
                }
            } else if (!fullName.equals(other.fullName)) {
                return false;
            }
            return true;
        }

        @Override
        public String toString() {
            final StringBuilder sb = new StringBuilder(128).append("{ ").append(lsubMap == null ? "LSUB" : "LIST");
            sb.append(" fullName=\"").append(fullName).append('"');
            sb.append(", parent=");
            if (null == parent) {
                sb.append("null");
            } else {
                sb.append('"').append(parent.getFullName()).append('"');
            }
            sb.append(", canOpen=").append(canOpen);
            sb.append(", attributes=(");
            if (null != attributes && !attributes.isEmpty()) {
                final Iterator<String> iterator = new TreeSet<String>(attributes).iterator();
                sb.append('"').append(iterator.next()).append('"');
                while (iterator.hasNext()) {
                    sb.append(", \"").append(iterator.next()).append('"');
                }
            }
            sb.append(')');
            sb.append(", children=(");
            if (null != children && !children.isEmpty()) {
                final Iterator<ListLsubEntryImpl> iterator = new TreeSet<ListLsubEntryImpl>(children).iterator();
                sb.append('"').append(iterator.next().getFullName()).append('"');
                while (iterator.hasNext()) {
                    sb.append(", \"").append(iterator.next().getFullName()).append('"');
                }
            }
            sb.append(") }");
            return sb.toString();
        }

        @Override
        public int compareTo(final ListLsubEntryImpl anotherEntry) {
            final String anotherFullName = anotherEntry.fullName;
            return fullName == null ? (anotherFullName == null ? 0 : -1) : fullName.compareToIgnoreCase(anotherFullName);
        }

        /**
         * Sets the namespace flag
         *
         * @param namespace The namespace flag
         */
        protected ListLsubEntryImpl setNamespace(final boolean namespace) {
            this.namespace = namespace;
            return this;
        }

        @Override
        public boolean isNamespace() {
            return namespace;
        }

        @Override
        public boolean hasChildren() {
            return null == hasChildren ? (null != children && !children.isEmpty()) : hasChildren.booleanValue();
        }

    } // End of class ListLsubEntryImpl

    private static void appendStackTrace(final StackTraceElement[] trace, final StringBuilder sb) {
        if (null == trace) {
            sb.append("<missing stack trace>\n");
            return;
        }
        for (final StackTraceElement ste : trace) {
            final String className = ste.getClassName();
            if (null != className) {
                sb.append("\tat ").append(className).append('.').append(ste.getMethodName());
                if (ste.isNativeMethod()) {
                    sb.append("(Native Method)");
                } else {
                    final String fileName = ste.getFileName();
                    if (null == fileName) {
                        sb.append("(Unknown Source)");
                    } else {
                        final int lineNumber = ste.getLineNumber();
                        sb.append('(').append(fileName);
                        if (lineNumber >= 0) {
                            sb.append(':').append(lineNumber);
                        }
                        sb.append(')');
                    }
                }
                sb.append('\n');
            }
        }
    }

}
