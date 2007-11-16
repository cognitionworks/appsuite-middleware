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

package com.openexchange.groupware.infostore.webdav;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.FolderLock;
import com.openexchange.groupware.FolderLockManager;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.sessiond.Session;
import com.openexchange.sessiond.impl.SessionHolder;
import com.openexchange.webdav.protocol.WebdavLock;

public class FolderLockHelper extends LockHelper {

	private FolderLockManager lockManager;
	private SessionHolder sessionHolder;

	public FolderLockHelper(FolderLockManager lockManager, SessionHolder sessionHolder, String url) {
		super(lockManager, sessionHolder, url);
		this.lockManager = lockManager;
		this.sessionHolder = sessionHolder;
	}

	@Override
	protected WebdavLock toWebdavLock(Lock lock) {
		if (lock instanceof FolderLock) {
			FolderLock folderLock = (FolderLock) lock;
			WebdavLock l = new WebdavLock();
			l.setDepth(folderLock.getDepth());
			l.setTimeout(folderLock.getTimeout());
			l.setToken("http://www.open-xchange.com/webdav/locks/"+folderLock.getId());
			l.setType(WebdavLock.Type.WRITE_LITERAL);
			l.setScope((folderLock.getScope().equals(LockManager.Scope.EXCLUSIVE)? WebdavLock.Scope.EXCLUSIVE_LITERAL : WebdavLock.Scope.SHARED_LITERAL));
			l.setOwner(lock.getOwnerDescription());
			return l;
		}
		throw new IllegalArgumentException("Lock must be of type FolderLock");
	}
	
	@Override
	protected int saveLock(WebdavLock lock) throws OXException {
		Session session = sessionHolder.getSessionObject();
		return lockManager.lock(id, 
				(lock.getTimeout() == WebdavLock.NEVER) ? LockManager.INFINITE : lock.getTimeout(), //FIXME
				lock.getScope().equals(WebdavLock.Scope.EXCLUSIVE_LITERAL) ? LockManager.Scope.EXCLUSIVE : LockManager.Scope.SHARED,
				LockManager.Type.WRITE, 
				lock.getDepth(), 
				lock.getOwner(),
				session.getContext(), 
				UserStorage.getStorageUser(session.getUserId(), session.getContext()),
				UserConfigurationStorage.getInstance().getUserConfigurationSafe(session.getUserId(), session.getContext()));
	}

	@Override
	protected void relock(WebdavLock lock) {
		//TODO
	}

	@Override
	protected Lock toLock(WebdavLock lock) {
		FolderLock l = new FolderLock();
		l.setId(Integer.valueOf(lock.getToken().substring( 41 )));
		//TODOl.setOwner(userid)
		l.setOwnerDescription(lock.getOwner());
		l.setScope(lock.getScope().equals(WebdavLock.Scope.EXCLUSIVE_LITERAL) ? LockManager.Scope.EXCLUSIVE : LockManager.Scope.SHARED);
		l.setType(LockManager.Type.WRITE);
		l.setDepth(lock.getDepth());
		l.setTimeout(lock.getTimeout());
		return l;
	}

	

}
