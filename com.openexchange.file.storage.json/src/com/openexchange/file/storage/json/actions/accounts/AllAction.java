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

package com.openexchange.file.storage.json.actions.accounts;

import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.file.storage.FileStorageService;
import com.openexchange.file.storage.json.FileStorageAccountConstants;
import com.openexchange.file.storage.registry.FileStorageServiceRegistry;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.tools.session.ServerSession;


/**
 * A class implementing the "all" action for listing file storage accounts. Optionally only accounts of a certain service
 * are returned. Parameters are:
 * <dl>
 *  <dt>filestorageService</dt><dd>(optional) The ID of the file storage service. If present lists only accounts of this service.</dd>
 * </dl>
 * Returns a JSONArray of JSONObjects representing the file storage accounts.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class AllAction extends AbstractFileStorageAccountAction {

    public AllAction(final FileStorageServiceRegistry registry) {
        super(registry);
    }

    @Override
    protected AJAXRequestResult doIt(final AJAXRequestData request, final ServerSession session) throws AbstractOXException, JSONException {
        
        final String fsServiceId = request.getParameter(FileStorageAccountConstants.FILE_STORAGE_SERVICE);
        
        final List<FileStorageService> services = new ArrayList<FileStorageService>();
        if(fsServiceId != null) {
            services.add(registry.getFileStorageService(fsServiceId));
        } else {
            services.addAll(registry.getAllServices());
        }
        
        final JSONArray result = new JSONArray();
        
        for (final FileStorageService messagingService : services) {
            for (final FileStorageAccount account : messagingService.getAccountManager().getAccounts(session)) {
                result.put(writer.write(account));
            }
        }
        
        return new AJAXRequestResult(result);
    }

}
