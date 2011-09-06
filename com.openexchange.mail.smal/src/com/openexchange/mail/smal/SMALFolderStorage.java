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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.mail.smal;

import static com.openexchange.mail.utils.MailFolderUtility.prepareMailFolderParam;
import com.openexchange.exception.OXException;
import com.openexchange.mail.IndexRange;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.Quota;
import com.openexchange.mail.Quota.Type;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailFolderStorageEnhanced;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.dataobjects.MailFolder;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.session.Session;

/**
 * {@link SMALFolderStorage} - The SMAL folder storage.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SMALFolderStorage extends AbstractSMALStorage implements IMailFolderStorage, IMailFolderStorageEnhanced {

    private final IMailFolderStorage folderStorage;

    /**
     * Initializes a new {@link SMALFolderStorage}.
     *
     * @throws OXException If initialization fails
     */
    public SMALFolderStorage(final Session session, final int accountId, final MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> delegateMailAccess) throws OXException {
        super(session, accountId, delegateMailAccess);
        folderStorage = delegateMailAccess.getFolderStorage();
    }

    @Override
    public boolean exists(final String fullName) throws OXException {
        if (MailFolder.DEFAULT_FOLDER_ID.equals(fullName)) {
            return true;
        }
        return folderStorage.exists(fullName);
    }

    @Override
    public MailFolder getFolder(final String fullName) throws OXException {
        return folderStorage.getFolder(fullName);
    }

    @Override
    public MailFolder[] getSubfolders(final String parentFullName, final boolean all) throws OXException {
        return folderStorage.getSubfolders(parentFullName, all);
    }

    @Override
    public void checkDefaultFolders() throws OXException {
        folderStorage.checkDefaultFolders();
    }

    @Override
    public String createFolder(final MailFolderDescription toCreate) throws OXException {
        return folderStorage.createFolder(toCreate);
    }

    @Override
    public String updateFolder(final String fullName, final MailFolderDescription toUpdate) throws OXException {
        return folderStorage.updateFolder(fullName, toUpdate);
    }

    @Override
    public String moveFolder(final String fullName, final String newFullName) throws OXException {
        return folderStorage.moveFolder(fullName, newFullName);
    }

    @Override
    public String deleteFolder(final String fullName, final boolean hardDelete) throws OXException {
        return folderStorage.deleteFolder(fullName, hardDelete);
    }

    @Override
    public void clearFolder(final String fullName, final boolean hardDelete) throws OXException {
        folderStorage.clearFolder(fullName, hardDelete);
    }

    @Override
    public Quota[] getQuotas(final String fullName, final Type[] types) throws OXException {
        return folderStorage.getQuotas(fullName, types);
    }

    @Override
    public String getConfirmedHamFolder() throws OXException {
        return folderStorage.getConfirmedHamFolder();
    }

    @Override
    public String getConfirmedSpamFolder() throws OXException {
        return folderStorage.getConfirmedSpamFolder();
    }

    @Override
    public String getDraftsFolder() throws OXException {
        return folderStorage.getDraftsFolder();
    }

    @Override
    public String getSpamFolder() throws OXException {
        return folderStorage.getSpamFolder();
    }

    @Override
    public String getSentFolder() throws OXException {
        return folderStorage.getSentFolder();
    }

    @Override
    public String getTrashFolder() throws OXException {
        return folderStorage.getTrashFolder();
    }

    @Override
    public MailFolder getRootFolder() throws OXException {
        return folderStorage.getFolder(MailFolder.DEFAULT_FOLDER_ID);
    }

    @Override
    public String getDefaultFolderPrefix() throws OXException {
        return folderStorage.getDefaultFolderPrefix();
    }

    @Override
    public String renameFolder(final String fullName, final String newName) throws OXException {
        return folderStorage.renameFolder(fullName, newName);
    }

    @Override
    public String deleteFolder(final String fullName) throws OXException {
        return folderStorage.deleteFolder(fullName);
    }

    @Override
    public void clearFolder(final String fullName) throws OXException {
        folderStorage.clearFolder(fullName);
    }

    @Override
    public MailFolder[] getPath2DefaultFolder(final String fullName) throws OXException {
        return folderStorage.getPath2DefaultFolder(fullName);
    }

    @Override
    public Quota getStorageQuota(final String fullName) throws OXException {
        return folderStorage.getStorageQuota(fullName);
    }

    @Override
    public Quota getMessageQuota(final String fullName) throws OXException {
        return folderStorage.getMessageQuota(fullName);
    }

    @Override
    public void releaseResources() throws OXException {
        folderStorage.releaseResources();
    }

    @Override
    public int getUnreadCounter(final String fullName) throws OXException {
        if (folderStorage instanceof IMailFolderStorageEnhanced) {
            return ((IMailFolderStorageEnhanced) folderStorage).getUnreadCounter(ensureFullName(fullName));
        }
        return delegateMailAccess.getMessageStorage().getUnreadMessages(
            ensureFullName(fullName),
            MailSortField.RECEIVED_DATE,
            OrderDirection.DESC,
            FIELDS_ID,
            -1).length;
    }

    private static String ensureFullName(final String fullName) {
        return prepareMailFolderParam(fullName).getFullname();
    }

    @Override
    public int getNewCounter(final String fullName) throws OXException {
        if (folderStorage instanceof IMailFolderStorageEnhanced) {
            return ((IMailFolderStorageEnhanced) folderStorage).getNewCounter(ensureFullName(fullName));
        }
        final MailMessage[] messages =
            delegateMailAccess.getMessageStorage().searchMessages(
                ensureFullName(fullName),
                IndexRange.NULL,
                MailSortField.RECEIVED_DATE,
                OrderDirection.ASC,
                null,
                FIELDS_FLAGS);
        int count = 0;
        for (final MailMessage mailMessage : messages) {
            if (mailMessage.isRecent()) {
                count++;
            }
        }
        return count;
    }

    @Override
    public int getTotalCounter(final String fullName) throws OXException {
        if (folderStorage instanceof IMailFolderStorageEnhanced) {
            return ((IMailFolderStorageEnhanced) folderStorage).getTotalCounter(ensureFullName(fullName));
        }
        return delegateMailAccess.getMessageStorage().searchMessages(
            ensureFullName(fullName),
            IndexRange.NULL,
            MailSortField.RECEIVED_DATE,
            OrderDirection.ASC,
            null,
            FIELDS_ID).length;
    }

}
