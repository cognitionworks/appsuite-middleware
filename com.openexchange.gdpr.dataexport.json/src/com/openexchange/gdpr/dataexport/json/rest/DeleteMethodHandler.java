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

package com.openexchange.gdpr.dataexport.json.rest;

import java.io.IOException;
import javax.servlet.http.HttpServletRequest;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.exception.OXException;
import com.openexchange.tools.servlet.AjaxExceptionCodes;

/**
 * {@link DeleteMethodHandler} - Serves the REST-like <code>DELETE</code> request.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class DeleteMethodHandler extends AbstractDataExportMethodHandler {

    /**
     * Initializes a new {@link DeleteMethodHandler}.
     */
    public DeleteMethodHandler() {
        super();
    }

    // DELETE /gdpr/dataexport

    @Override
    protected void modifyByPathInfo(AJAXRequestData requestData, String[] restPathElements, HttpServletRequest req) throws IOException, OXException {
        String action = getAction(restPathElements, req);
        if (action == null) {
            throw AjaxExceptionCodes.BAD_REQUEST.create();
        }
        requestData.setAction(action);

        if ("cancel".equals(action)) {
            return;
        }
        if ("delete".equals(action)) {
            return;
        }
        throw AjaxExceptionCodes.UNKNOWN_ACTION.create(req.getPathInfo());
    }

    @Override
    protected String doGetAction(String[] restPathElements, HttpServletRequest restRequest) {
        if (hasNoPathInfo(restPathElements)) {
            // Cancel user-associated task.
            return "cancel";
        }

        int length = restPathElements.length;
        if (0 == length) {
            // Cancel user-associated task.
            return "cancel";
        }

        if (1 == length && "delete".equals(restPathElements[0])) {
            // DELETE /gdpr/dataexport/delete
            return "delete";
        }

        return null;
    }

}
