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

package com.openexchange.mail.json.actions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import com.openexchange.ajax.Mail;
import com.openexchange.ajax.parser.SearchTermParser;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.json.MailRequest;
import com.openexchange.mail.json.writer.MessageWriter;
import com.openexchange.mail.json.writer.MessageWriter.MailFieldWriter;
import com.openexchange.server.ServiceLookup;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link SearchAction}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SearchAction extends AbstractMailAction {

    /**
     * Initializes a new {@link SearchAction}.
     * 
     * @param services
     */
    public SearchAction(final ServiceLookup services) {
        super(services);
    }

    @Override
    protected AJAXRequestResult perform(final MailRequest req) throws OXException {
        try {
            final ServerSession session = req.getSession();
            /*
             * Read in parameters
             */
            final String folderId = req.checkParameter(Mail.PARAMETER_MAILFOLDER);
            final int[] columns = req.checkIntArray(Mail.PARAMETER_COLUMNS);
            final String sort = req.getParameter(Mail.PARAMETER_SORT);
            final String order = req.getParameter(Mail.PARAMETER_ORDER);
            if (sort != null && order == null) {
                throw MailExceptionCode.MISSING_PARAM.create(Mail.PARAMETER_ORDER);
            }
            final JSONValue searchValue = (JSONValue) req.getRequest().getData();
            /*
             * Get mail interface
             */
            final MailServletInterface mailInterface = getMailInterface(req);
            /*
             * Perform search dependent on passed JSON value
             */
            final AJAXRequestResult result;
            if (searchValue.isArray()) {
                /*
                 * Parse body into a JSON array
                 */
                final JSONArray ja = (JSONArray) searchValue;
                final int length = ja.length();
                if (length > 0) {
                    final int[] searchCols = new int[length];
                    final String[] searchPats = new String[length];
                    for (int i = 0; i < length; i++) {
                        final JSONObject tmp = ja.getJSONObject(i);
                        searchCols[i] = tmp.getInt(Mail.PARAMETER_COL);
                        searchPats[i] = tmp.getString(Mail.PARAMETER_SEARCHPATTERN);
                    }
                    /*
                     * Search mails
                     */
                    final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                    final int userId = session.getUserId();
                    final int contextId = session.getContextId();
                    int orderDir = OrderDirection.ASC.getOrder();
                    if (order != null) {
                        if (order.equalsIgnoreCase("asc")) {
                            orderDir = OrderDirection.ASC.getOrder();
                        } else if (order.equalsIgnoreCase("desc")) {
                            orderDir = OrderDirection.DESC.getOrder();
                        } else {
                            throw MailExceptionCode.INVALID_INT_VALUE.create(Mail.PARAMETER_ORDER);
                        }
                    }
                    final OXJSONWriter jsonWriter = new OXJSONWriter();
                    /*
                     * Start response
                     */
                    jsonWriter.array();
                    SearchIterator<MailMessage> it = null;
                    try {
                        if (("thread".equalsIgnoreCase(sort))) {
                            it =
                                mailInterface.getThreadedMessages(
                                    folderId,
                                    null,
                                    MailSortField.RECEIVED_DATE.getField(),
                                    orderDir,
                                    searchCols,
                                    searchPats,
                                    true,
                                    columns);
                            final int size = it.size();
                            for (int i = 0; i < size; i++) {
                                final MailMessage mail = it.next();
                                final JSONArray arr = new JSONArray();
                                for (final MailFieldWriter writer : writers) {
                                    writer.writeField(arr, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                                }
                                jsonWriter.value(arr);
                            }
                        } else {
                            final int sortCol = sort == null ? MailListField.RECEIVED_DATE.getField() : Integer.parseInt(sort);
                            it = mailInterface.getMessages(folderId, null, sortCol, orderDir, searchCols, searchPats, true, columns);
                            final int size = it.size();
                            for (int i = 0; i < size; i++) {
                                final MailMessage mail = it.next();
                                final JSONArray arr = new JSONArray();
                                for (final MailFieldWriter writer : writers) {
                                    writer.writeField(arr, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                                }
                                jsonWriter.value(arr);
                            }
                        }
                    } finally {
                        if (it != null) {
                            it.close();
                        }
                    }
                    jsonWriter.endArray();
                    result = new AJAXRequestResult(jsonWriter.getObject(), "json");
                } else {
                    result = new AJAXRequestResult(new JSONArray(), "json");
                }
            } else {
                final JSONArray searchArray = ((JSONObject) searchValue).getJSONArray(Mail.PARAMETER_FILTER);
                /*
                 * Pre-Select field writers
                 */
                final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                final int userId = session.getUserId();
                final int contextId = session.getContextId();
                int orderDir = OrderDirection.ASC.getOrder();
                if (order != null) {
                    if (order.equalsIgnoreCase("asc")) {
                        orderDir = OrderDirection.ASC.getOrder();
                    } else if (order.equalsIgnoreCase("desc")) {
                        orderDir = OrderDirection.DESC.getOrder();
                    } else {
                        throw MailExceptionCode.INVALID_INT_VALUE.create(Mail.PARAMETER_ORDER);
                    }
                }
                final int sortCol = sort == null ? MailListField.RECEIVED_DATE.getField() : Integer.parseInt(sort);
                final SearchIterator<MailMessage> it =
                    mailInterface.getMessages(folderId, null, sortCol, orderDir, SearchTermParser.parse(searchArray), true, columns);
                final int size = it.size();
                final OXJSONWriter jsonWriter = new OXJSONWriter();
                /*
                 * Start response
                 */
                jsonWriter.array();
                for (int i = 0; i < size; i++) {
                    final MailMessage mail = it.next();
                    final JSONArray arr = new JSONArray();
                    for (final MailFieldWriter writer : writers) {
                        writer.writeField(arr, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                    }
                    jsonWriter.value(arr);
                }
                jsonWriter.endArray();
                result = new AJAXRequestResult(jsonWriter.getObject(), "json");
            }
            return result;
        } catch (final JSONException e) {
            throw MailExceptionCode.JSON_ERROR.create(e, e.getMessage());
        } catch (final RuntimeException e) {
            throw MailExceptionCode.UNEXPECTED_ERROR.create(e, e.getMessage());
        }
    }

}
