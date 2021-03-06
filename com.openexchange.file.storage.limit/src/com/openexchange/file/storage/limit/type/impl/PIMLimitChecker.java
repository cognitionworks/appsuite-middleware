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

package com.openexchange.file.storage.limit.type.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.limit.LimitFile;
import com.openexchange.file.storage.limit.exceptions.FileLimitExceptionCodes;
import com.openexchange.filestore.FileStorages;
import com.openexchange.filestore.Info;
import com.openexchange.filestore.QuotaFileStorage;
import com.openexchange.filestore.QuotaFileStorageService;
import com.openexchange.groupware.attach.AttachmentConfig;
import com.openexchange.groupware.attach.AttachmentExceptionCodes;
import com.openexchange.groupware.upload.impl.UploadUtility;
import com.openexchange.session.Session;

/**
 * {@link PIMLimitChecker}
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since v7.10.1
 */
public class PIMLimitChecker extends AbstractCombinedTypeLimitChecker {

    @Override
    public String getType() {
        return "pim";
    }

    @Override
    public List<OXException> check(Session session, String folderId, List<LimitFile> files) throws OXException {
        List<OXException> exceededLimits = new ArrayList<>();

        long fileTotalSize = files.stream().collect(Collectors.summingLong(LimitFile::getSize)).longValue();
        QuotaFileStorage fileStorage = getFileStorage(session.getContextId());
        if (fileStorage != null) {
            long quota = fileStorage.getQuota();
            if (quota == 0) {
                exceededLimits.add(FileLimitExceptionCodes.NOT_ALLOWED.create(folderId));
            } else if (quota > 0 && fileStorage.getUsage() + fileTotalSize > quota) {
                exceededLimits.add(FileLimitExceptionCodes.STORAGE_QUOTA_EXCEEDED.create(UploadUtility.getSize(fileTotalSize, 2, false, true), UploadUtility.getSize(quota - fileStorage.getUsage(), 2, false, true)));
            }
        }

        List<OXException> checkMaxUploadSizePerFile = checkMaxUploadSizePerFile(files);
        if (!checkMaxUploadSizePerFile.isEmpty()) {
            exceededLimits.addAll(checkMaxUploadSizePerFile);
        }
        return exceededLimits;
    }

    @Override
    protected long getMaxUploadSizePerModule() {
        return AttachmentConfig.getMaxUploadSize();
    }

    private QuotaFileStorage getFileStorage(int contextId) throws OXException {
        QuotaFileStorageService storageService = FileStorages.getQuotaFileStorageService();
        if (null == storageService) {
            throw AttachmentExceptionCodes.FILESTORE_DOWN.create();
        }
        return storageService.getQuotaFileStorage(contextId, Info.general());
    }
}
