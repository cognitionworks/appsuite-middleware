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

package com.openexchange.mail.json.actions;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.documentation.RequestMethod;
import com.openexchange.documentation.annotations.Action;
import com.openexchange.documentation.annotations.Parameter;
import com.openexchange.exception.OXException;
import com.openexchange.folderstorage.cache.CacheFolderStorage;
import com.openexchange.java.Strings;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailField;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.IMailFolderStorage;
import com.openexchange.mail.api.IMailMessageStorage;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailFolderDescription;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.permission.DefaultMailPermission;
import com.openexchange.mail.permission.MailPermission;
import com.openexchange.mail.search.ComparisonType;
import com.openexchange.mail.search.ReceivedDateTerm;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.TimeZoneUtils;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link ArchiveFolderAction}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
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
    protected AJAXRequestResult performArchive(final MailRequest req) throws OXException {
        MailAccess<? extends IMailFolderStorage, ? extends IMailMessageStorage> mailAccess = null;
        try {
            /*
             * Read in parameters
             */
            int days;
            {
                String sDays = req.getRequest().getParameter("days");
                days = Strings.isEmpty(sDays) ? MailProperties.getInstance().getDefaultArchiveDays() : Strings.parsePositiveInt(sDays.trim());
            }
            String sourceFolder = req.checkParameter(AJAXServlet.PARAMETER_FOLDERID);
            ServerSession session = req.getSession();
            FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(sourceFolder);
            int accountId = fa.getAccountId();

            // Connect mail access
            mailAccess = MailAccess.getInstance(session, accountId);
            mailAccess.connect();

            // Check archive full name
            int[] separatorRef = new int[1];
            String archiveFullname = checkArchiveFullNameFor(mailAccess, req, separatorRef);
            char separator = (char) separatorRef[0];

            // Check location
            {
                String fullName = fa.getFullname();
                if (fullName.equals(archiveFullname) || fullName.startsWith(archiveFullname + separator)) {
                    return new AJAXRequestResult(Boolean.TRUE, "native");
                }
            }

            // Move to archive folder
            Calendar cal = Calendar.getInstance(TimeZoneUtils.getTimeZone("UTC"));
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.add(Calendar.DATE, days * -1);

            ReceivedDateTerm term = new ReceivedDateTerm(ComparisonType.LESS_THAN, cal.getTime());
            MailMessage[] msgs = mailAccess.getMessageStorage().searchMessages(fa.getFullname(), null, MailSortField.RECEIVED_DATE, OrderDirection.DESC, term, new MailField[] { MailField.ID, MailField.RECEIVED_DATE });
            if (null == msgs || msgs.length <= 0) {
                return new AJAXRequestResult(Boolean.TRUE, "native");
            }

            Map<Integer, List<String>> map = new HashMap<Integer, List<String>>(4);
            for (MailMessage mailMessage : msgs) {
                Date receivedDate = mailMessage.getReceivedDate();
                cal.setTime(receivedDate);
                Integer year = Integer.valueOf(cal.get(Calendar.YEAR));
                List<String> ids = map.get(year);
                if (null == ids) {
                    ids = new LinkedList<String>();
                    map.put(year, ids);
                }
                ids.add(mailMessage.getMailId());
            }

            for (Map.Entry<Integer, List<String>> entry : map.entrySet() ) {
                String sYear = entry.getKey().toString();
                String fn = archiveFullname + separator + sYear;
                if (!mailAccess.getFolderStorage().exists(fn)) {
                    final MailFolderDescription toCreate = new MailFolderDescription();
                    toCreate.setAccountId(accountId);
                    toCreate.setParentAccountId(accountId);
                    toCreate.setParentFullname(archiveFullname);
                    toCreate.setExists(false);
                    toCreate.setFullname(fn);
                    toCreate.setName(sYear);
                    toCreate.setSeparator(separator);
                    {
                        final DefaultMailPermission mp = new DefaultMailPermission();
                        mp.setEntity(session.getUserId());
                        final int p = MailPermission.ADMIN_PERMISSION;
                        mp.setAllPermission(p, p, p, p);
                        mp.setFolderAdmin(true);
                        mp.setGroupPermission(false);
                        toCreate.addPermission(mp);
                    }
                    try {
                        mailAccess.getFolderStorage().createFolder(toCreate);
                    } catch (final OXException e) {
                        if (SUBFOLDERS_NOT_ALLOWED_PREFIX.equals(e.getPrefix()) && e.getCode() == SUBFOLDERS_NOT_ALLOWED_ERROR_CODE) {
                            if (mailAccess.getFolderStorage().exists(archiveFullname)) {
                                fn = archiveFullname;
                            } else {
                                throw MailExceptionCode.ARCHIVE_SUBFOLDER_NOT_ALLOWED.create(e);
                            }
                        } else {
                            throw e;
                        }
                    }
                    CacheFolderStorage.getInstance().removeFromCache(MailFolderUtility.prepareFullname(accountId, archiveFullname), "0", true, session);
                }

                List<String> ids = entry.getValue();
                mailAccess.getMessageStorage().moveMessages(fa.getFullname(), fn, ids.toArray(new String[ids.size()]), true);
            }

            return new AJAXRequestResult(Boolean.TRUE, "native");
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        } catch (final OXException e) {
            if (SUBFOLDERS_NOT_ALLOWED_PREFIX.equals(e.getPrefix()) && e.getCode() == SUBFOLDERS_NOT_ALLOWED_ERROR_CODE) {
                throw MailExceptionCode.ARCHIVE_SUBFOLDER_NOT_ALLOWED.create(e);
            }
            throw e;
        } finally {
            if (null != mailAccess) {
                mailAccess.close(true);
            }
        }
    }

}
