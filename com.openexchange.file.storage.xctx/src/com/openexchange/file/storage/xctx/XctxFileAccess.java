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

package com.openexchange.file.storage.xctx;

import static com.openexchange.file.storage.xctx.XctxAccountAccess.XCTX_PARENT_FOLDER_IDS;
import static com.openexchange.java.Autoboxing.I;
import java.util.List;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.File;
import com.openexchange.file.storage.File.Field;
import com.openexchange.file.storage.FileStorageAccountAccess;
import com.openexchange.file.storage.FileStorageBackwardLinkAccess;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.infostore.AbstractInfostoreFileAccess;
import com.openexchange.file.storage.infostore.FileConverter;
import com.openexchange.file.storage.search.SearchTerm;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.InfostoreSearchEngine;
import com.openexchange.groupware.modules.Module;
import com.openexchange.groupware.notify.hostname.HostData;
import com.openexchange.java.Strings;
import com.openexchange.share.ShareTargetPath;
import com.openexchange.share.core.tools.ShareLinks;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorAdapter;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link XctxFileAccess}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since 7.10.5
 */
public class XctxFileAccess extends AbstractInfostoreFileAccess implements FileStorageBackwardLinkAccess {

    private final XctxAccountAccess accountAccess;
    private final XctxFileConverter fileConverter;
    private final ServerSession localSession;

    /**
     * Initializes a new {@link XctxFileAccess}.
     *
     * @param accountAccess The parent account access
     * @param localSession The user's <i>local</i> session associated with the file storage account
     * @param guestSession The <i>remote</i> session of the guest user used to access the contents of the foreign context
     */
    public XctxFileAccess(XctxAccountAccess accountAccess, ServerSession localSession, ServerSession guestSession) throws OXException {
        super(guestSession, accountAccess.getServiceSafe(InfostoreFacade.class), accountAccess.getServiceSafe(InfostoreSearchEngine.class));
        this.localSession = localSession;
        this.accountAccess = accountAccess;
        this.fileConverter = new XctxFileConverter(accountAccess, guestSession);
    }

    @Override
    protected FileConverter getConverter() {
        return fileConverter;
    }

    @Override
    protected InfostoreFacade getInfostore(String folderId) throws OXException {
        if (XCTX_PARENT_FOLDER_IDS.contains(folderId)) {
            throw FileStorageExceptionCodes.FOLDER_NOT_FOUND.create(
                folderId, accountAccess.getAccountId(), accountAccess.getService().getId(), I(localSession.getUserId()), I(localSession.getContextId()));
        }
        return super.getInfostore(folderId);
    }

    @Override
    public FileStorageAccountAccess getAccountAccess() {
        return accountAccess;
    }

    @Override
    public SearchIterator<File> getUserSharedDocuments(List<Field> fields, Field sort, SortDirection order) throws OXException {
        return SearchIteratorAdapter.emptyIterator();
    }

    @Override
    public SearchIterator<File> search(String pattern, List<Field> fields, String folderId, boolean includeSubfolders, Field sort, SortDirection order, int start, int end) throws OXException {
        return filterUnsubscribed(super.search(pattern, fields, folderId, includeSubfolders, sort, order, start, end));
    }

    @Override
    public SearchIterator<File> search(List<String> folderIds, SearchTerm<?> searchTerm, List<Field> fields, Field sort, SortDirection order, int start, int end) throws OXException {
        return filterUnsubscribed(super.search(folderIds, searchTerm, fields, sort, order, start, end));
    }

    @Override
    public SearchIterator<File> search(String folderId, boolean includeSubfolders, SearchTerm<?> searchTerm, List<Field> fields, Field sort, SortDirection order, int start, int end) throws OXException {
        return filterUnsubscribed(super.search(folderId, includeSubfolders, searchTerm, fields, sort, order, start, end));
    }

    @Override
    public String getBackwardLink(String folderId, String id, Map<String, String> additionals) throws OXException {
        String shareUrl = (String) accountAccess.getAccount().getConfiguration().get("url");
        if (Strings.isEmpty(shareUrl)) {
            throw FileStorageExceptionCodes.MISSING_CONFIG.create("url", accountAccess.getAccount().getId());
        }
        HostData hostData = ShareLinks.extractHostData(shareUrl);
        String guestToken = ShareLinks.extractBaseToken(shareUrl);
        ShareTargetPath targetPath = new ShareTargetPath(Module.INFOSTORE.getFolderConstant(), folderId, id, additionals);
        return ShareLinks.generateExternal(hostData, guestToken, targetPath);
    }

    @Override
    public String toString() {
        return "XctxFileAccess [accountId=" + accountAccess.getAccountId() +
            ", localUser=" + localSession.getUserId() + '@' + localSession.getContextId() +
            ", guestUser=" + super.session.getUserId() + '@' + super.session.getContextId() + ']';
    }

    private SearchIterator<File> filterUnsubscribed(SearchIterator<File> searchIterator) {
        return accountAccess.getSubscribedHelper().filterUnsubscribed(searchIterator, (id) -> accountAccess.getFolderAccess().getFolder(id));
    }

}
