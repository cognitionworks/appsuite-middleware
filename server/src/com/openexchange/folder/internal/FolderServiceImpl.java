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

package com.openexchange.folder.internal;

import com.openexchange.exception.OXException;
import com.openexchange.folder.FolderException;
import com.openexchange.folder.FolderService;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.userconfiguration.UserConfigurationStorage;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.tools.oxfolder.OXFolderAccess;

/**
 * {@link FolderServiceImpl} - TODO Short description of this class' purpose.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderServiceImpl implements FolderService {

    /**
     * Initializes a new {@link FolderServiceImpl}.
     */
    public FolderServiceImpl() {
        super();
    }

    public FolderObject getFolderObject(final int folderId, final int contextId) throws FolderException {
        try {
            return new OXFolderAccess(ContextStorage.getStorageContext(contextId)).getFolderObject(folderId);
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

    public EffectivePermission getFolderPermission(final int folderId, final int userId, final int contextId) throws FolderException {
        try {
            final Context ctx = ContextStorage.getStorageContext(contextId);
            return new OXFolderAccess(ctx).getFolderPermission(
                folderId,
                userId,
                UserConfigurationStorage.getInstance().getUserConfiguration(userId, ctx));
        } catch (final OXException e) {
            throw new FolderException(e);
        } catch (final OXException e) {
            throw new FolderException(e);
        }
    }

}
