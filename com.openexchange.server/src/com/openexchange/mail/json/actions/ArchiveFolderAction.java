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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.mail.json.actions;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestDataTools;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ArchiveFolderAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 */
@Action(method = RequestMethod.PUT, name = "archive_folder", description = "Moves mails to archive folder from given folder using certain criteria", parameters = {
    @Parameter(name = "session", description = "A session ID previously obtained from the login module."),
    @Parameter(name = "days", description = "The days threshold to use."),
    @Parameter(name = "folder", description = "Object ID of the source folder.")
},
responseDescription = "A JSON true response.")
public final class ArchiveFolderAction extends AbstractArchiveMailAction {

    /**
     * Initializes a new {@link ArchiveFolderAction}.
     *
     * @param services
     */
    public ArchiveFolderAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        int days;
        {
            String sDays = req.getRequest().getParameter("days");
            days = Strings.isEmpty(sDays) ? MailProperties.getInstance().getDefaultArchiveDays() : Strings.parsePositiveInt(sDays.trim());
        }
        try {
            final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String folderId = req.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
            /*
             * Get mail interface
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            boolean useDefaultName = AJAXRequestDataTools.parseBoolParameter("useDefaultName", req.getRequest(), true);
            boolean createIfAbsent = AJAXRequestDataTools.parseBoolParameter("createIfAbsent", req.getRequest(), true);
            mailInterface.archiveMailFolder(days, folderId, session, useDefaultName, createIfAbsent);
            return new AJAXRequestResult(Boolean.TRUE, "native");
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
