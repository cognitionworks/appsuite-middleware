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

package com.openexchange.groupware.container;

import javax.mail.MessagingException;

import com.openexchange.IMAPPermission;
import com.openexchange.api2.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.imap.IMAPException;
import com.openexchange.groupware.imap.IMAPProperties;
import com.openexchange.groupware.imap.IMAPUtils;
import com.openexchange.groupware.imap.OXMailException;
import com.openexchange.groupware.imap.OXMailException.MailCode;
import com.openexchange.groupware.ldap.LdapException;
import com.sun.mail.imap.ACL;
import com.sun.mail.imap.DefaultFolder;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.Rights;

public class MailFolderObject {
	
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(MailFolderObject.class);
	
	private static final Rights RIGHTS_EMPTY = new Rights();
	
	/**
	 * New mailbox attribute added by the "LIST-EXTENDED" extension
	 */
	private static final String ATTRIBUTE_NON_EXISTENT = "\\NonExistent";
	
	private String fullName;
	
	private String parentFullName;
	
	private String name;
	
	private boolean hasSubfolders;
	
	private Rights ownRights;
	
	private boolean rootFolder;
	
	private String summary;
	
	private int total, newi, unread, deleted;
	
	private ACL[] acls;
	
	private boolean b_acls;
	
	private boolean exists;
	
	private char separator = '0';
	
	private IMAPFolder imapFolder;
	
	private boolean subscribed;
	
	private boolean b_subscribed;

	public static final String DEFAULT_IMAP_FOLDER = "default";
	
	public MailFolderObject() {
		super();
	}
	
	public MailFolderObject(final String fullName, final boolean exists) {
		super();
		this.exists = exists;
		this.fullName = fullName;
	}
	
	public MailFolderObject(final IMAPFolder folder) throws MessagingException, OXException {
		super();
		this.exists = folder.exists();
		final String[] attrs = folder.getAttributes();
		Attribs: for (String attribute : attrs) {
			if (ATTRIBUTE_NON_EXISTENT.equalsIgnoreCase(attribute)) {
				this.exists = false;
				break Attribs;
			}
		}
		this.fullName = prepareFullname(folder.getFullName(), folder.getSeparator());
		this.name = folder.getName();
		this.parentFullName = prepareParentFullname(folder.getParent());
		this.separator = folder.getSeparator();
		this.hasSubfolders = IMAPUtils.hasSubfolders(folder);
		this.ownRights = this.exists ? getOwnRightsInternal(folder) : (Rights) RIGHTS_EMPTY.clone();
		this.rootFolder = (folder instanceof DefaultFolder);
		if ((folder.getType() & IMAPFolder.HOLDS_MESSAGES) > 0) {
			this.summary = new StringBuilder().append('(').append(folder.getMessageCount()).append('/').append(
					folder.getUnreadMessageCount()).append(')').toString();
			this.total = folder.getMessageCount();
			this.newi = folder.getNewMessageCount();
			this.unread = folder.getUnreadMessageCount();
			this.deleted = folder.getDeletedMessageCount();
		}
		this.subscribed = folder.isSubscribed();
		b_subscribed = true;
		if (IMAPProperties.isSupportsACLs() && this.exists && !(folder instanceof DefaultFolder)) {
			try {
				this.acls = folder.getACL();
				b_acls = true;
			} catch (final MessagingException e) {
				if (LOG.isWarnEnabled()) {
					LOG.warn(new StringBuilder("ACL could not be requested for folder ").append(folder.getFullName()),
							e);
				}
				this.acls = null;
				b_acls = false;
			}
		}
		this.imapFolder = folder;
	}
	
	private static final String STR_MAILBOX_NOT_EXISTS = "NO Mailbox does not exist";
	
	private static final String STR_FULL_RIGHTS = "acdilprsw";
	
	private static final Rights getOwnRightsInternal(final IMAPFolder folder) throws MessagingException, OXException {
		if (folder instanceof DefaultFolder) {
			return null;
		}
		final Rights retval;
		if (IMAPProperties.isSupportsACLs()) {
			try {
				retval = folder.myRights();
			} catch (MessagingException e) {
				if (e.getNextException() instanceof com.sun.mail.iap.CommandFailedException
						&& e.getNextException().getMessage().indexOf(STR_MAILBOX_NOT_EXISTS) != -1) {
					/*
					 * This occurs when requesting MYRIGHTS on a shared folder.
					 * Just log a warning!
					 */
					if (LOG.isWarnEnabled()) {
						LOG.warn(OXMailException.getFormattedMessage(MailCode.FOLDER_NOT_FOUND, folder.getFullName()));
					}
				} else {
					LOG.error(e.getMessage(), e);
				}
				/*
				 * Write empty string as rights. Nevertheless user may see
				 * folder!
				 */
				return (Rights) RIGHTS_EMPTY.clone();
			} catch (Throwable t) {
				LOG.error(t.getMessage(), t);
				/*
				 * Write empty string as rights.
				 * Nevertheless user may see folder!
				 */
				return (Rights) RIGHTS_EMPTY.clone();
			}
		} else {
			/*
			 * No ACLs enabled. User has full access.
			 */
			retval = new Rights(STR_FULL_RIGHTS);
		}
		if ((folder.getType() & javax.mail.Folder.HOLDS_FOLDERS) == 0) {
			/*
			 * NoInferiors detected: No create access
			 */
			retval.remove(Rights.Right.CREATE);
		} else if ((folder.getType() & javax.mail.Folder.HOLDS_MESSAGES) == 0) {
			/*
			 * NoSelect detected: No read access
			 */
			retval.remove(Rights.Right.READ);
		}
		return retval;
	}
	
	public static final String prepareFullname(final String fullname, final char sep) {
		if (MailFolderObject.DEFAULT_IMAP_FOLDER.equals(fullname)) {
			return fullname;
		}
		return new StringBuilder().append(MailFolderObject.DEFAULT_IMAP_FOLDER).append(sep).append(fullname).toString();
	}

	private static final String prepareParentFullname(final javax.mail.Folder parent) throws MessagingException {
		final StringBuilder sb = new StringBuilder(50).append(MailFolderObject.DEFAULT_IMAP_FOLDER);
		if (parent instanceof DefaultFolder) {
			return sb.toString();
		} else if (parent == null) {
			return null;
		}
		return sb.append(parent.getSeparator()).append(parent.getFullName()).toString();
	}

	public String getFullName() {
		return fullName;
	}

	public void setFullName(final String fullName) {
		this.fullName = fullName;
	}

	public String getName() {
		return name;
	}

	public void setName(final String name) {
		this.name = name;
	}

	public String getParentFullName() {
		return parentFullName;
	}

	public void setParentFullName(final String parentFullName) {
		this.parentFullName = parentFullName;
	}

	public ACL[] getACL() {
		final ACL[] retval = new ACL[acls.length];
		System.arraycopy(acls, 0, retval, 0, acls.length);
		return retval;
	}
	
	public boolean containsACLs() {
		return b_acls;
	}

	public void setACL(final ACL[] acls) {
		this.acls = new ACL[acls.length];
		System.arraycopy(acls, 0, this.acls, 0, acls.length);
		b_acls = true;
	}
	
	public void addACL(final ACL acl) {
		if (acls == null) {
			this.acls = new ACL[1];
			this.acls[0] = acl;
			return;
		}
		final ACL[] tmp = this.acls;
		this.acls = new ACL[tmp.length + 1];
		System.arraycopy(tmp, 0, acls, 0, tmp.length);
		acls[acls.length - 1] = acl;
	}
	
	public void removeACL() {
		this.acls = null;
		b_acls = false;
	}
	
	public IMAPPermission[] getIMAPPermissions(final Context ctx) throws IMAPException, LdapException {
		if (!b_acls) {
			return null;
		}
		final IMAPPermission[] retval = new IMAPPermission[acls.length];
		final String fn = imapFolder == null ? null : imapFolder.getFullName();
		for (int i = 0; i < retval.length; i++) {
			final IMAPPermission ip = new IMAPPermission(ctx);
			ip.setFolderFullname(fn);
			ip.parseACL(acls[i]);
			retval[i] = ip;
		}
		return retval;
	}
	
	public boolean exists() {
		return exists;
	}

	public char getSeparator() throws MessagingException {
		if (separator != '0') {
			return separator;
		}
		throw new MessagingException("IMAP delimiter not specified!");
	}

	public void setSeparator(final char separator) {
		this.separator = separator;
	}

	public IMAPFolder getImapFolder() {
		return imapFolder;
	}

	public void setImapFolder(final IMAPFolder imapFolder) {
		this.imapFolder = imapFolder;
		try {
			setSeparator(imapFolder.getSeparator());
		} catch (MessagingException e) {
			LOG.error(e.getMessage(), e);
		}
	}

	public int getDeleted() {
		return deleted;
	}

	public boolean isExists() {
		return exists;
	}

	public boolean hasSubfolders() {
		return hasSubfolders;
	}

	public int getNew() {
		return newi;
	}

	public Rights getOwnRights() {
		return ownRights;
	}

	public boolean isRootFolder() {
		return rootFolder;
	}

	public String getSummary() {
		return summary;
	}

	public int getTotal() {
		return total;
	}

	public int getUnread() {
		return unread;
	}

	public boolean isSubscribed() {
		return subscribed;
	}
	
	public boolean containsSubscribe() {
		return b_subscribed;
	}

	public void setSubscribed(final boolean subscribe) {
		this.subscribed = subscribe;
		b_subscribed = true;
	}
	
	public void removeSubscribe() {
		this.subscribed = false;
		b_subscribed = false;
	}
	
}
