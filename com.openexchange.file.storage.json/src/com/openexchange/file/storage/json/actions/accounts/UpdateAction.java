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

package com.openexchange.file.storage.json.actions.accounts;

import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.annotation.restricted.RestrictedAction;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageAccountMetaDataUtil;
import com.openexchange.file.storage.FileStorageExceptionCodes;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.LoginAwareFileStorageServiceExtension;
import com.openexchange.file.storage.SharingFileStorageService;
import com.openexchange.file.storage.json.FileStorageAccountConstants;
import com.openexchange.file.storage.json.actions.files.AbstractFileAction;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.tools.session.ServerSession;


/**
 * Updates a messaging account. The request must contain a JSON representation of the changes to the messaging account (that is: all fields that are to be changed)
 * and the account id. Returns "1" on success.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
@RestrictedAction(module = AbstractFileAction.MODULE, type = RestrictedAction.Type.WRITE)
public class UpdateAction extends AbstractFileStorageAccountAction {

    public UpdateAction(final FileStorageServiceRegistry registry) {
        super(registry);
    }

    @Override
    protected AJAXRequestResult doIt(final AJAXRequestData request, final ServerSession session) throws JSONException, OXException {

        final JSONObject data = (JSONObject) request.requireData();
        if (!data.has(FileStorageAccountConstants.ID)) {
            throw FileStorageExceptionCodes.MISSING_PARAMETER.create(FileStorageAccountConstants.ID);
        }

        final FileStorageAccount account = parser.parse(data);
        FileStorageService fileStorageService = account.getFileStorageService();
        if(fileStorageService instanceof SharingFileStorageService) {
            //Clear last recent error in order to try the new configuration
            ((SharingFileStorageService)fileStorageService).resetRecentError(account.getId(), session);
        }
        final boolean doConnectionCheck = account.getFileStorageService() instanceof LoginAwareFileStorageServiceExtension && account.getConfiguration() != null;

        //load existing account for resetting if the connection check failed
        FileStorageAccount existingAccount = account.getFileStorageService().getAccountManager().getAccount(account.getId(), session);
        if(existingAccount != null) {
            //Preserve account meta data when updating
            FileStorageAccountMetaDataUtil.copy(existingAccount, account);
        }

        //perform update
        account.getFileStorageService().getAccountManager().updateAccount(account, session);

        if (doConnectionCheck) {
            try {
                //test connection
                ((LoginAwareFileStorageServiceExtension) account.getFileStorageService()).testConnection(account, session);
            } catch (OXException e) {
                //reset
                if(existingAccount != null) {
                    account.getFileStorageService().getAccountManager().updateAccount(existingAccount, session);
                }
                throw e;
            }
        }
        return new AJAXRequestResult(Integer.valueOf(1));
    }

}
