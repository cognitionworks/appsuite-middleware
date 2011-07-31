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

package com.openexchange.imap.acl;

import com.sun.mail.imap.Rights;
import com.sun.mail.imap.Rights.Right;

/**
 * {@link RFC4314ACLExtension} - The ACL extension according to <small><b><a href="http://www.rfc-archive.org/getrfc.php?rfc=4314">RFC
 * 4314</a></b></small>.
 * <p>
 * Due to ambiguity in <small><b><a href="http://www.rfc-archive.org/getrfc.php?rfc=2086">RFC 2086</a></b></small>, the "create" right is
 * the union of the "k" and "x" rights, and the "delete" right is the union of the "e" and "t" rights. For compatibility with <small><b><a
 * href="http://www.rfc-archive.org/getrfc.php?rfc=2086">RFC 2086</a></b></small>, this ACL extension defines two virtual rights "d" and
 * "c".
 * <p>
 * If a client includes the "d" right in a rights list, then it MUST be treated as if the client had included every member of the "delete"
 * right.
 * <p>
 * If a client includes the "c" right in a rights list, then it MUST be treated as if the client had included every member of the "create"
 * right.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
class RFC4314ACLExtension extends AbstractACLExtension {

    /**
     * "aclkx": {@link Right#ADMINISTER} + {@link Right#CREATE} + {@link Right#LOOKUP} + {@link RFC4314Rights#CREATE_MAILBOXES} +
     * {@link RFC4314Rights#DELETE_MAILBOX}.
     */
    private static final transient Rights RIGHTS_FOLDER_ADMIN = new Rights("aclkx");

    /**
     * "l": {@link Right#LOOKUP}
     */
    private static final transient Rights RIGHTS_FOLDER_VISIBLE = new Rights("l");

    /**
     * "il": {@link Right#INSERT} {@link Right#LOOKUP}
     */
    private static final transient Rights RIGHTS_FOLDER_CREATE_OBJECTS = new Rights("il");

    /**
     * "cilkx": {@link Right#CREATE} + {@link Right#INSERT} + {@link Right#LOOKUP} + {@link RFC4314Rights#CREATE_MAILBOXES} +
     * {@link RFC4314Rights#DELETE_MAILBOX}.
     */
    private static final transient Rights RIGHTS_FOLDER_CREATE_SUBFOLDERS = new Rights("cilkx");

    /**
     * "rs": {@link Right#READ} + {@link Right#KEEP_SEEN}
     */
    private static final transient Rights RIGHTS_READ_ALL_KEEP_SEEN = new Rights("rs");

    /**
     * "r": {@link Right#READ}
     */
    private static final transient Rights RIGHTS_READ_ALL = new Rights("r");

    /**
     * "w": {@link Right#WRITE}
     */
    private static final transient Rights RIGHTS_WRITE_ALL = new Rights("w");

    /**
     * "det": {@link Right#DELETE} + {@link RFC4314Rights#EXPUNGE} + {@link RFC4314Rights#DELETE_MESSAGES} +
     */
    private static final transient Rights RIGHTS_DELETE_ALL = new Rights("det");

    /**
     * "p": {@link Right#POST}
     */
    private static final transient Rights RIGHTS_UNMAPPABLE = new Rights("p");

    private final Rights fullRights;

    /**
     * Initializes a new {@link RFC4314ACLExtension}.
     */
    RFC4314ACLExtension() {
        super();
        fullRights = new ReadOnlyRights("lrswipkxtecda");
    }

    public boolean canGetACL(final Rights rights) {
        return rights.contains(Rights.Right.ADMINISTER);
    }

    public boolean canSetACL(final Rights rights) {
        return rights.contains(Rights.Right.ADMINISTER);
    }

    public boolean canCreate(final Rights rights) {
        return rights.contains(RFC4314Rights.CREATE_MAILBOXES) || rights.contains(Rights.Right.CREATE);
    }

    public boolean canDeleteMailbox(final Rights rights) {
        return rights.contains(RFC4314Rights.DELETE_MAILBOX) || rights.contains(Rights.Right.CREATE);
    }

    public boolean canDeleteMessages(final Rights rights) {
        return rights.contains(RFC4314Rights.DELETE_MESSAGES) || rights.contains(Rights.Right.DELETE);
    }

    public boolean canExpunge(final Rights rights) {
        return rights.contains(RFC4314Rights.EXPUNGE) || rights.contains(Rights.Right.DELETE);
    }

    public Rights getFullRights() {
        return fullRights;
    }

    public void addFolderAdminRights(final Rights rights) {
        rights.add(RIGHTS_FOLDER_ADMIN);
    }

    public boolean containsFolderAdminRights(final Rights rights) {
        return rights.contains(RIGHTS_FOLDER_ADMIN);
    }

    public void addFolderVisibility(final Rights rights) {
        rights.add(RIGHTS_FOLDER_VISIBLE);
    }

    public boolean containsFolderVisibility(final Rights rights) {
        return rights.contains(RIGHTS_FOLDER_VISIBLE);
    }

    public void addCreateObjects(final Rights rights) {
        rights.add(RIGHTS_FOLDER_CREATE_OBJECTS);
    }

    public void addCreateSubfolders(final Rights rights) {
        rights.add(RIGHTS_FOLDER_CREATE_SUBFOLDERS);
    }

    public void addDeleteAll(final Rights rights) {
        rights.add(RIGHTS_DELETE_ALL);
    }

    public void addNonMappable(final Rights rights) {
        rights.add(RIGHTS_UNMAPPABLE);
    }

    public void addReadAll(final Rights rights) {
        rights.add(RIGHTS_READ_ALL);
    }

    public void addReadAllKeepSeen(final Rights rights) {
        rights.add(RIGHTS_READ_ALL_KEEP_SEEN);
    }

    public void addWriteAll(final Rights rights) {
        rights.add(RIGHTS_WRITE_ALL);
    }

    public boolean containsCreateObjects(final Rights rights) {
        return rights.contains(RIGHTS_FOLDER_CREATE_OBJECTS);
    }

    public boolean containsCreateSubfolders(final Rights rights) {
        return rights.contains(RIGHTS_FOLDER_CREATE_SUBFOLDERS);
    }

    public boolean containsDeleteAll(final Rights rights) {
        return rights.contains(RIGHTS_DELETE_ALL);
    }

    public boolean containsNonMappable(final Rights rights) {
        return rights.contains(RIGHTS_UNMAPPABLE);
    }

    public boolean containsReadAll(final Rights rights) {
        return rights.contains(RIGHTS_READ_ALL);
    }

    public boolean containsReadAllKeepSeen(final Rights rights) {
        return rights.contains(RIGHTS_READ_ALL_KEEP_SEEN);
    }

    public boolean containsWriteAll(final Rights rights) {
        return rights.contains(RIGHTS_WRITE_ALL);
    }
}
