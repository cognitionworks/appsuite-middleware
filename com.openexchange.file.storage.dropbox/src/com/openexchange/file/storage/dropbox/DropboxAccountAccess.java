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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageFileAccess;
import com.openexchange.file.storage.FileStorageFolder;
import com.openexchange.file.storage.FileStorageFolderAccess;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.dropbox.session.DropboxOAuthAccess;
import com.openexchange.session.Session;

/**
 * {@link DropboxAccountAccess}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DropboxAccountAccess implements FileStorageAccountAccess {

    private final FileStorageAccount account;
    private final Session session;
    private final FileStorageService service;
    private final DropboxOAuthAccess dropboxOAuthAccess;

    /**
     * Initializes a new {@link DropboxAccountAccess}.
     * 
     * @throws OXException If initialization fails
     */
    public DropboxAccountAccess(final FileStorageService service, final FileStorageAccount account, final Session session) throws OXException {
        super();
        this.service = service;
        this.account = account;
        this.session = session;
        dropboxOAuthAccess = DropboxOAuthAccess.accessFor(account, session);
    }

    @Override
    public void connect() throws OXException {
        // Nope
    }

    @Override
    public boolean isConnected() {
        return true;
    }

    @Override
    public void close() {
        // Nope
    }

    @Override
    public boolean ping() throws OXException {
        return true;
    }

    @Override
    public boolean cacheable() {
        return false;
    }

    @Override
    public String getAccountId() {
        return account.getId();
    }

    @Override
    public FileStorageFileAccess getFileAccess() throws OXException {
        return new DropboxFileAccess(dropboxOAuthAccess, account, session, this);
    }

    @Override
    public FileStorageFolderAccess getFolderAccess() throws OXException {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public FileStorageFolder getRootFolder() throws OXException {
        connect();
        return getFolderAccess().getRootFolder();
    }

    @Override
    public FileStorageService getService() {
        return service;
    }

}
