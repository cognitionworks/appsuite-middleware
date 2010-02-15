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

package com.openexchange.ajax;

import static com.openexchange.mail.json.parser.MessageParser.parseAddressKey;
import static com.openexchange.tools.Collections.newHashMap;
import static com.openexchange.tools.oxfolder.OXFolderUtility.getFolderName;
import static com.openexchange.tools.oxfolder.OXFolderUtility.getUserName;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONValue;
import org.json.JSONWriter;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.fields.FolderChildFields;
import com.openexchange.ajax.fields.FolderFields;
import com.openexchange.ajax.fields.ResponseFields;
import com.openexchange.ajax.helper.BrowserDetector;
import com.openexchange.ajax.helper.DownloadUtility;
import com.openexchange.ajax.helper.ParamContainer;
import com.openexchange.ajax.helper.DownloadUtility.CheckedDownload;
import com.openexchange.ajax.parser.InfostoreParser;
import com.openexchange.ajax.parser.SearchTermParser;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.api.OXMandatoryFieldException;
import com.openexchange.api.OXPermissionException;
import com.openexchange.api2.OXException;
import com.openexchange.cache.OXCachingException;
import com.openexchange.contactcollector.ContactCollectorService;
import com.openexchange.filemanagement.ManagedFile;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextStorage;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.groupware.settings.SettingException;
import com.openexchange.groupware.upload.impl.UploadEvent;
import com.openexchange.groupware.upload.impl.UploadException;
import com.openexchange.groupware.upload.impl.UploadListener;
import com.openexchange.groupware.upload.impl.UploadRegistry;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.mail.FullnameArgument;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailJSONField;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.MailPath;
import com.openexchange.mail.MailServletInterface;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.OrderDirection;
import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.cache.JSONMessageCache;
import com.openexchange.mail.cache.MailMessageCache;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.json.parser.MessageParser;
import com.openexchange.mail.json.writer.JSONObjectConverter;
import com.openexchange.mail.json.writer.MessageWriter;
import com.openexchange.mail.json.writer.MessageWriter.MailFieldWriter;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.ManagedMimeMessage;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.QuotedInternetAddress;
import com.openexchange.mail.mime.converters.MIMEMessageConverter;
import com.openexchange.mail.text.HTMLProcessing;
import com.openexchange.mail.text.parser.HTMLParser;
import com.openexchange.mail.text.parser.handler.HTMLFilterHandler;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.utils.DisplayMode;
import com.openexchange.mail.utils.MailFolderUtility;
import com.openexchange.mail.utils.MessageUtility;
import com.openexchange.mailaccount.MailAccount;
import com.openexchange.mailaccount.MailAccountException;
import com.openexchange.mailaccount.MailAccountExceptionMessages;
import com.openexchange.mailaccount.MailAccountStorageService;
import com.openexchange.preferences.ServerUserSetting;
import com.openexchange.server.ServiceException;
import com.openexchange.server.impl.EffectivePermission;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.threadpool.ThreadPools;
import com.openexchange.tools.encoding.Helper;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.iterator.SearchIteratorException;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderException;
import com.openexchange.tools.oxfolder.OXFolderException.FolderCode;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.tools.servlet.OXJSONException;
import com.openexchange.tools.servlet.UploadServletException;
import com.openexchange.tools.servlet.http.Tools;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.tools.stream.UnsynchronizedByteArrayInputStream;
import com.openexchange.tools.stream.UnsynchronizedByteArrayOutputStream;
import com.openexchange.tools.versit.utility.VersitUtility;

/**
 * {@link Mail} - The servlet to handle mail requests.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class Mail extends PermissionServlet implements UploadListener {

    private static final transient org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(Mail.class);

    private static final boolean DEBUG = LOG.isDebugEnabled();

    private static final long ZERO = 0L;

    private static final String MIME_TEXT_HTML_CHARSET_UTF_8 = "text/html; charset=UTF-8";

    private static final String MIME_TEXT_PLAIN = "text/plain";

    private static final String MIME_TEXT_HTML = "text/htm";

    private static final String STR_CONTENT_DISPOSITION = "Content-disposition";

    private static final String STR_USER_AGENT = "user-agent";

    private static final String STR_DELIM = ": ";

    private static final String STR_CRLF = "\r\n";

    private static final String STR_THREAD = "thread";

    private static final long serialVersionUID = 1980226522220313667L;

    private static final AbstractOXException getWrappingOXException(final Exception cause) {
        if (LOG.isWarnEnabled()) {
            final StringBuilder warnBuilder = new StringBuilder(140);
            warnBuilder.append("An unexpected exception occurred, which is going to be wrapped for proper display.\n");
            warnBuilder.append("For safety reason its original content is display here.");
            LOG.warn(warnBuilder.toString(), cause);
        }
        final String message = cause.getMessage();
        return new AbstractOXException(
            EnumComponent.MAIL,
            Category.INTERNAL_ERROR,
            9999,
            null == message ? "[Not available]" : message,
            cause);
    }

    private static final String UPLOAD_PARAM_MAILINTERFACE = "msint";

    private static final String UPLOAD_PARAM_WRITER = "writer";

    private static final String UPLOAD_PARAM_SESSION = "sess";

    private static final String UPLOAD_PARAM_HOSTNAME = "hostn";

    private static final String UPLOAD_PARAM_PROTOCOL = "proto";

    private static final String STR_UTF8 = "UTF-8";

    private static final String STR_1 = "1";

    private static final String STR_EMPTY = "";

    private static final String STR_NULL = "null";

    /**
     * The parameter 'folder' contains the folder's id whose contents are queried.
     */
    public static final String PARAMETER_MAILFOLDER = "folder";

    public static final String PARAMETER_MAILATTCHMENT = "attachment";

    public static final String PARAMETER_DESTINATION_FOLDER = "dest_folder";

    public static final String PARAMETER_MAILCID = "cid";

    public static final String PARAMETER_SAVE = "save";

    public static final String PARAMETER_SHOW_SRC = "src";

    public static final String PARAMETER_SHOW_HEADER = "hdr";

    public static final String PARAMETER_EDIT_DRAFT = "edit";

    public static final String PARAMETER_SEND_TYPE = "sendtype";

    public static final String PARAMETER_VIEW = "view";

    public static final String PARAMETER_SRC = "src";

    public static final String PARAMETER_FLAGS = "flags";

    public static final String PARAMETER_UNSEEN = "unseen";

    public static final String PARAMETER_PREPARE = "prepare";

    public static final String PARAMETER_FILTER = "filter";

    public static final String PARAMETER_COL = "col";

    public static final String PARAMETER_MESSAGE_ID = "message_id";

    public static final String PARAMETER_HEADERS = "headers";

    private static final String VIEW_RAW = "raw";

    private static final String VIEW_TEXT = "text";

    private static final String VIEW_HTML = "html";

    private static final String VIEW_HTML_BLOCKED_IMAGES = "noimg";

    @Override
    protected void doGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType(CONTENTTYPE_JAVASCRIPT);
        /*
         * The magic spell to disable caching
         */
        Tools.disableCaching(resp);
        try {
            actionGet(req, resp);
        } catch (final Exception e) {
            LOG.error("doGet", e);
            writeError(e.toString(), new JSONWriter(resp.getWriter()));
        }
    }

    @Override
    protected void doPut(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        resp.setContentType(CONTENTTYPE_JAVASCRIPT);
        /*
         * The magic spell to disable caching
         */
        Tools.disableCaching(resp);
        try {
            actionPut(req, resp);
        } catch (final Exception e) {
            LOG.error("doGet", e);
            writeError(e.toString(), new JSONWriter(resp.getWriter()));
        }
    }

    private final static void writeError(final String error, final JSONWriter jsonWriter) {
        try {
            startResponse(jsonWriter);
            jsonWriter.value(STR_EMPTY);
            endResponse(jsonWriter, null, error);
        } catch (final Exception exc) {
            LOG.error("writeError", exc);
        }
    }

    private final void actionGet(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final String actionStr = checkStringParam(req, PARAMETER_ACTION);
        if (actionStr.equalsIgnoreCase(ACTION_ALL)) {
            actionGetAllMails(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_COUNT)) {
            actionGetMailCount(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_UPDATES)) {
            actionGetUpdates(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_REPLY) || actionStr.equalsIgnoreCase(ACTION_REPLYALL)) {
            actionGetReply(req, resp, (actionStr.equalsIgnoreCase(ACTION_REPLYALL)));
        } else if (actionStr.equalsIgnoreCase(ACTION_FORWARD)) {
            actionGetForward(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_GET)) {
            actionGetMessage(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_GET_STRUCTURE)) {
            actionGetStructure(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_MATTACH)) {
            actionGetAttachment(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_ZIP_MATTACH)) {
            actionGetMultipleAttachments(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_NEW_MSGS)) {
            actionGetNew(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_SAVE_VERSIT)) {
            actionGetSaveVersit(req, resp);
        } else {
            throw new Exception("Unknown value in parameter " + PARAMETER_ACTION + " through GET command");
        }
    }

    private final void actionPut(final HttpServletRequest req, final HttpServletResponse resp) throws Exception {
        final String actionStr = checkStringParam(req, PARAMETER_ACTION);
        if (actionStr.equalsIgnoreCase(ACTION_LIST)) {
            actionPutMailList(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_DELETE)) {
            actionPutDeleteMails(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_UPDATE)) {
            actionPutUpdateMail(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_COPY)) {
            actionPutCopyMail(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_MATTACH)) {
            actionPutAttachment(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_MAIL_RECEIPT_ACK)) {
            actionPutReceiptAck(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_SEARCH)) {
            actionPutMailSearch(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_CLEAR)) {
            actionPutClear(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_AUTOSAVE)) {
            actionPutAutosave(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_FORWARD)) {
            actionPutForwardMultiple(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_REPLY) || actionStr.equalsIgnoreCase(ACTION_REPLYALL)) {
            actionPutReply(req, resp, (actionStr.equalsIgnoreCase(ACTION_REPLYALL)));
        } else if (actionStr.equalsIgnoreCase(ACTION_GET)) {
            actionPutGet(req, resp);
        } else if (actionStr.equalsIgnoreCase(ACTION_NEW)) {
            actionPutNewMail(req, resp);
        } else {
            throw new Exception("Unknown value in parameter " + PARAMETER_ACTION + " through PUT command");
        }
    }

    public void actionGetUpdates(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionGetUpdates(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi), writer);
    }

    private final void actionGetUpdates(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(
                actionGetUpdates(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null),
                resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final transient static JSONArray EMPTY_JSON_ARR = new JSONArray();

    private final transient MailFieldWriter WRITER_ID = MessageWriter.getMailFieldWriter(new MailListField[] { MailListField.ID })[0];

    private final Response actionGetUpdates(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        jsonWriter.array();
        try {
            final String folderId = paramContainer.checkStringParam(PARAMETER_MAILFOLDER);
            final String ignore = paramContainer.getStringParam(PARAMETER_IGNORE);
            boolean bIgnoreDelete = false;
            boolean bIgnoreModified = false;
            if (ignore != null && ignore.indexOf("deleted") != -1) {
                bIgnoreDelete = true;
            }
            if (ignore != null && ignore.indexOf("changed") != -1) {
                bIgnoreModified = true;
            }
            if (!bIgnoreModified || !bIgnoreDelete) {
                final int[] columns = paramContainer.checkIntArrayParam(PARAMETER_COLUMNS);
                final int userId = session.getUserId();
                final int contextId = session.getContextId();
                MailServletInterface mailInterface = mailInterfaceArg;
                boolean closeMailInterface = false;
                try {
                    if (mailInterface == null) {
                        mailInterface = MailServletInterface.getInstance(session);
                        closeMailInterface = true;
                    }
                    if (!bIgnoreModified) {
                        final MailMessage[] modified = mailInterface.getUpdatedMessages(folderId, columns);
                        final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                        for (final MailMessage mail : modified) {
                            final JSONArray ja = new JSONArray();
                            if (mail != null) {
                                for (final MailFieldWriter writer : writers) {
                                    writer.writeField(ja, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                                }
                                jsonWriter.value(ja);
                            }
                        }
                    }
                    if (!bIgnoreDelete) {
                        final MailMessage[] deleted = mailInterface.getDeletedMessages(folderId, columns);
                        for (final MailMessage mail : deleted) {
                            final JSONArray ja = new JSONArray();
                            WRITER_ID.writeField(ja, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                            jsonWriter.value(ja);
                        }
                    }
                } finally {
                    if (closeMailInterface && mailInterface != null) {
                        mailInterface.close(true);
                    }
                }
            }
            // final FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(folderId);
            /*
             * Clean session caches
             */
            // SessionMailCache.getInstance(session, fa.getAccountId()).clear();
            /*
             * Clean message cache
             */
            // MailMessageCache.getInstance().removeFolderMessages(fa.getAccountId(), fa.getFullname(), session.getUserId(),
            // session.getContext());
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        jsonWriter.endArray();
        /*
         * Close response and flush print writer
         */
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionGetMailCount(final Session session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionGetMailCount(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi), writer);
    }

    private final void actionGetMailCount(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(
                actionGetMailCount(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null),
                resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetMailCount(final Session session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        /*
         * Some variables
         */
        final Response response = new Response();
        /*
         * Start response
         */
        Object data = JSONObject.NULL;
        try {
            final String folderId = paramContainer.checkStringParam(PARAMETER_MAILFOLDER);
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                data = Integer.valueOf(mailInterface.getAllMessageCount(folderId)[0]);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            if (!e.getCategory().equals(Category.USER_CONFIGURATION)) {
                response.setException(e);
            }
        } catch (final AbstractOXException e) {
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        return response;
    }

    public void actionGetAllMails(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws SearchIteratorException, JSONException {
        ResponseWriter.write(actionGetAllMails(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi), writer);
    }

    private final void actionGetAllMails(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(
                actionGetAllMails(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null),
                resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        } catch (final SearchIteratorException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            response.setException(e);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private static final String STR_ASC = "asc";

    private static final String STR_DESC = "desc";

    private final Response actionGetAllMails(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException, SearchIteratorException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        final long start = DEBUG ? System.currentTimeMillis() : ZERO;
        jsonWriter.array();
        SearchIterator<MailMessage> it = null;
        try {
            /*
             * Read in parameters
             */
            final String folderId = paramContainer.checkStringParam(PARAMETER_MAILFOLDER);
            final int[] columns = paramContainer.checkIntArrayParam(PARAMETER_COLUMNS);
            final String sort = paramContainer.getStringParam(PARAMETER_SORT);
            final String order = paramContainer.getStringParam(PARAMETER_ORDER);
            if (sort != null && order == null) {
                throw new MailException(MailException.Code.MISSING_PARAM, PARAMETER_ORDER);
            }

            final int[] fromToIndices;
            {
                final int leftHandLimit = paramContainer.getIntParam(LEFT_HAND_LIMIT);
                final int rigthHandLimit = paramContainer.getIntParam(RIGHT_HAND_LIMIT);
                if (leftHandLimit == ParamContainer.NOT_FOUND || rigthHandLimit == ParamContainer.NOT_FOUND) {
                    fromToIndices = null;
                } else {
                    fromToIndices = new int[] { leftHandLimit, rigthHandLimit };
                }
            }

            /*
             * Get all mails
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                /*
                 * Pre-Select field writers
                 */
                final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                final int userId = session.getUserId();
                final int contextId = session.getContextId();
                int orderDir = OrderDirection.ASC.getOrder();
                if (order != null) {
                    if (order.equalsIgnoreCase(STR_ASC)) {
                        orderDir = OrderDirection.ASC.getOrder();
                    } else if (order.equalsIgnoreCase(STR_DESC)) {
                        orderDir = OrderDirection.DESC.getOrder();
                    } else {
                        throw new MailException(MailException.Code.INVALID_INT_VALUE, PARAMETER_ORDER);
                    }
                }
                /*
                 * Check for thread-sort
                 */
                if ((STR_THREAD.equalsIgnoreCase(sort))) {
                    it =
                        mailInterface.getAllThreadedMessages(
                            folderId,
                            MailSortField.RECEIVED_DATE.getField(),
                            orderDir,
                            columns,
                            fromToIndices);
                    final int size = it.size();
                    for (int i = 0; i < size; i++) {
                        final MailMessage mail = it.next();
                        final JSONArray ja = new JSONArray();
                        if (mail != null) {
                            for (final MailFieldWriter writer : writers) {
                                writer.writeField(ja, mail, mail.getThreadLevel(), false, mailInterface.getAccountID(), userId, contextId);
                            }

                        }
                        jsonWriter.value(ja);
                    }
                } else {
                    final int sortCol = sort == null ? MailListField.RECEIVED_DATE.getField() : Integer.parseInt(sort);
                    /*
                     * Get iterator
                     */
                    it = mailInterface.getAllMessages(folderId, sortCol, orderDir, columns, fromToIndices);
                    final int size = it.size();
                    for (int i = 0; i < size; i++) {
                        final MailMessage mail = it.next();
                        final JSONArray ja = new JSONArray();
                        if (mail != null) {
                            for (final MailFieldWriter writer : writers) {
                                writer.writeField(ja, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                            }
                        }
                        jsonWriter.value(ja);
                    }
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        } finally {
            if (it != null) {
                it.close();
            }
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        if (DEBUG) {
            final long d = System.currentTimeMillis() - start;
            LOG.debug(new StringBuilder(32).append("/ajax/mail?action=all performed in ").append(d).append("msec"));
        }
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionGetReply(final ServerSession session, final JSONWriter writer, final JSONObject jo, final boolean reply2all, final MailServletInterface mailInterface) throws JSONException {
        ResponseWriter.write(actionGetReply(session, reply2all, ParamContainer.getInstance(jo, EnumComponent.MAIL), mailInterface), writer);
    }

    private final void actionGetReply(final HttpServletRequest req, final HttpServletResponse resp, final boolean reply2all) throws IOException {
        try {
            ResponseWriter.write(actionGetReply(
                getSessionObject(req),
                reply2all,
                ParamContainer.getInstance(req, EnumComponent.MAIL, resp),
                null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetReply(final ServerSession session, final boolean reply2all, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        /*
         * final Some variables
         */
        final Response response = new Response();
        Object data = JSONObject.NULL;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            final String view = paramContainer.getStringParam(PARAMETER_VIEW);
            final UserSettingMail usmNoSave = (UserSettingMail) session.getUserSettingMail().clone();
            /*
             * Deny saving for this request-specific settings
             */
            usmNoSave.setNoSave(true);
            /*
             * Overwrite settings with request's parameters
             */
            if (null != view) {
                if (VIEW_TEXT.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(false);
                } else if (VIEW_HTML.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(true);
                    usmNoSave.setAllowHTMLImages(true);
                } else {
                    LOG.warn(new StringBuilder(64).append("Unknown value in parameter ").append(PARAMETER_VIEW).append(": ").append(view).append(
                        ". Using user's mail settings as fallback."));
                }
            }
            /*
             * Get reply message
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterfaceArg == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                data =
                    MessageWriter.writeMailMessage(mailInterface.getAccountID(), mailInterface.getReplyMessageForDisplay(
                        folderPath,
                        uid,
                        reply2all,
                        usmNoSave), DisplayMode.MODIFYABLE, session, usmNoSave);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        return response;
    }

    public void actionGetForward(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mailInterface) throws JSONException {
        ResponseWriter.write(actionGetForward(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mailInterface), writer);
    }

    private final void actionGetForward(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(
                actionGetForward(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null),
                resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetForward(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        /*
         * Some variables
         */
        final Response response = new Response();
        Object data = JSONObject.NULL;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            final String view = paramContainer.getStringParam(PARAMETER_VIEW);
            final UserSettingMail usmNoSave = (UserSettingMail) session.getUserSettingMail().clone();
            /*
             * Deny saving for this request-specific settings
             */
            usmNoSave.setNoSave(true);
            /*
             * Overwrite settings with request's parameters
             */
            if (null != view) {
                if (VIEW_TEXT.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(false);
                } else if (VIEW_HTML.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(true);
                    usmNoSave.setAllowHTMLImages(true);
                } else {
                    LOG.warn(new StringBuilder(64).append("Unknown value in parameter ").append(PARAMETER_VIEW).append(": ").append(view).append(
                        ". Using user's mail settings as fallback."));
                }
            }
            /*
             * Get forward message
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                data =
                    MessageWriter.writeMailMessage(mailInterface.getAccountID(), mailInterface.getForwardMessageForDisplay(
                        new String[] { folderPath },
                        new String[] { uid },
                        usmNoSave), DisplayMode.MODIFYABLE, session, usmNoSave);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        return response;
    }

    public void actionGetStructure(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws JSONException {
        final Response response = actionGetStructure(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi);
        if (null != response) {
            ResponseWriter.write(response, writer);
        }
    }

    private final void actionGetStructure(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            final Response response =
                actionGetStructure(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null);
            if (null != response) {
                ResponseWriter.write(response, resp.getWriter());
            }
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetStructure(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        final long s = DEBUG ? System.currentTimeMillis() : ZERO;
        /*
         * Some variables
         */
        final Response response = new Response();
        Object data = JSONObject.NULL;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            // final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            final boolean unseen;
            {
                final String tmp = paramContainer.getStringParam(PARAMETER_UNSEEN);
                unseen = (STR_1.equals(tmp) || Boolean.parseBoolean(tmp));
            }
            final long maxSize;
            {
                final String tmp = paramContainer.getStringParam("max_size");
                if (null == tmp) {
                    maxSize = -1;
                } else {
                    long l = -1;
                    try {
                        l = Long.parseLong(tmp.trim());
                    } catch (final NumberFormatException e) {
                        l = -1;
                    }
                    maxSize = l;
                }
            }
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }

                final String uid;
                {
                    String tmp2 = paramContainer.getStringParam(PARAMETER_ID);
                    if (null == tmp2) {
                        tmp2 = paramContainer.getStringParam(PARAMETER_MESSAGE_ID);
                        if (null == tmp2) {
                            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAMETER_ID);
                        }
                        uid = mailInterface.getMailIDByMessageID(folderPath, tmp2);
                    } else {
                        uid = tmp2;
                    }
                }

                /*
                 * Get message
                 */
                final MailMessage mail = mailInterface.getMessage(folderPath, uid);
                if (mail == null) {
                    throw new MailException(MailException.Code.MAIL_NOT_FOUND, uid, folderPath);
                }
                final boolean wasUnseen = (mail.containsPrevSeen() && !mail.isPrevSeen());
                final boolean doUnseen = (unseen && wasUnseen);
                if (doUnseen) {
                    mail.setFlag(MailMessage.FLAG_SEEN, false);
                    final int unreadMsgs = mail.getUnreadMessages();
                    mail.setUnreadMessages(unreadMsgs < 0 ? 0 : unreadMsgs + 1);
                }
                data = MessageWriter.writeStructure(mailInterface.getAccountID(), mail, maxSize);
                if (doUnseen) {
                    /*
                     * Leave mail as unseen
                     */
                    mailInterface.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, false);
                } else if (wasUnseen) {
                    try {
                        final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                        final int contextId = session.getContextId();
                        final int userId = session.getUserId();
                        if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailAccess(
                            contextId,
                            userId).booleanValue()) {
                            triggerContactCollector(session, mail);
                        }
                    } catch (final SettingException e) {
                        LOG.warn("Contact collector could not be triggered.", e);
                    }
                }

            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }

        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        if (DEBUG) {
            final long d = System.currentTimeMillis() - s;
            LOG.debug(new StringBuilder(32).append("/ajax/mail?action=get performed in ").append(d).append("msec"));
        }
        return response;
    }

    public void actionGetMessage(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws JSONException {
        final Response response = actionGetMessage(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi);
        if (null != response) {
            ResponseWriter.write(response, writer);
        }
    }

    private final void actionGetMessage(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            final Response response =
                actionGetMessage(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null);
            if (null != response) {
                ResponseWriter.write(response, resp.getWriter());
            }
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetMessage(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        /*
         * Some variables
         */
        final Response response = new Response();
        Object data = JSONObject.NULL;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            // final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            String tmp = paramContainer.getStringParam(PARAMETER_SHOW_SRC);
            final boolean showMessageSource = (STR_1.equals(tmp) || Boolean.parseBoolean(tmp));
            tmp = paramContainer.getStringParam(PARAMETER_EDIT_DRAFT);
            final boolean editDraft = (STR_1.equals(tmp) || Boolean.parseBoolean(tmp));
            tmp = paramContainer.getStringParam(PARAMETER_SHOW_HEADER);
            final boolean showMessageHeaders = (STR_1.equals(tmp) || Boolean.parseBoolean(tmp));
            tmp = paramContainer.getStringParam(PARAMETER_SAVE);
            final boolean saveToDisk = (tmp != null && tmp.length() > 0 && Integer.parseInt(tmp) > 0);
            tmp = paramContainer.getStringParam(PARAMETER_VIEW);
            final String view = null == tmp ? null : tmp.toLowerCase(Locale.ENGLISH);
            tmp = paramContainer.getStringParam(PARAMETER_UNSEEN);
            final boolean unseen = (tmp != null && (STR_1.equals(tmp) || Boolean.parseBoolean(tmp)));
            tmp = null;
            /*
             * Get message
             */
            final long s = DEBUG ? System.currentTimeMillis() : ZERO;
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }

                final String uid;
                {
                    String tmp2 = paramContainer.getStringParam(PARAMETER_ID);
                    if (null == tmp2) {
                        tmp2 = paramContainer.getStringParam(PARAMETER_MESSAGE_ID);
                        if (null == tmp2) {
                            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAMETER_ID);
                        }
                        uid = mailInterface.getMailIDByMessageID(folderPath, tmp2);
                    } else {
                        uid = tmp2;
                    }
                }

                if (showMessageSource) {
                    /*
                     * Get message
                     */
                    final MailMessage mail = mailInterface.getMessage(folderPath, uid);
                    if (mail == null) {
                        throw new MailException(MailException.Code.MAIL_NOT_FOUND, uid, folderPath);
                    }
                    final UnsynchronizedByteArrayOutputStream baos = new UnsynchronizedByteArrayOutputStream();
                    try {
                        mail.writeTo(baos);
                    } catch (final MailException e) {
                        if (MailException.Code.NO_CONTENT.getNumber() == e.getDetailNumber()) {
                            LOG.debug(e.getMessage(), e);
                            baos.reset();
                        } else {
                            throw e;
                        }
                    }
                    if (saveToDisk) {
                        /*
                         * Write message source to output stream...
                         */
                        final ContentType contentType = new ContentType();
                        contentType.setPrimaryType("application");
                        contentType.setSubType("octet-stream");
                        final HttpServletResponse httpResponse = paramContainer.getHttpServletResponse();
                        httpResponse.setContentType(contentType.toString());
                        final String preparedFileName =
                            getSaveAsFileName(
                                new StringBuilder(mail.getSubject()).append(".eml").toString(),
                                isMSIEOnWindows(paramContainer.getHeader(STR_USER_AGENT)),
                                null);
                        httpResponse.setHeader("Content-disposition", new StringBuilder(64).append("attachment; filename=\"").append(
                            preparedFileName).append('"').toString());
                        Tools.removeCachingHeader(httpResponse);
                        // Write output stream in max. 8K chunks
                        final OutputStream out = httpResponse.getOutputStream();
                        final byte[] bytes = baos.toByteArray();
                        int offset = 0;
                        while (offset < bytes.length) {
                            final int len = Math.min(0xFFFF, bytes.length - offset);
                            out.write(bytes, offset, len);
                            offset += len;
                        }
                        out.flush();
                        /*
                         * ... and return
                         */
                        return null;
                    }
                    final ContentType ct = mail.getContentType();
                    final boolean wasUnseen = (mail.containsPrevSeen() && !mail.isPrevSeen());
                    final boolean doUnseen = (unseen && wasUnseen);
                    if (doUnseen) {
                        mail.setFlag(MailMessage.FLAG_SEEN, false);
                        final int unreadMsgs = mail.getUnreadMessages();
                        mail.setUnreadMessages(unreadMsgs < 0 ? 0 : unreadMsgs + 1);
                    }
                    data = new String(baos.toByteArray(), ct.containsCharsetParameter() ? ct.getCharsetParameter() : STR_UTF8);
                    if (doUnseen) {
                        /*
                         * Leave mail as unseen
                         */
                        mailInterface.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, false);
                    } else if (wasUnseen) {
                        /*
                         * Trigger contact collector
                         */
                        try {
                            final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                            final int contextId = session.getContextId();
                            final int userId = session.getUserId();
                            if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailAccess(
                                contextId,
                                userId).booleanValue()) {
                                triggerContactCollector(session, mail);
                            }
                        } catch (final SettingException e) {
                            LOG.warn("Contact collector could not be triggered.", e);
                        }
                    }
                } else if (showMessageHeaders) {
                    /*
                     * Get message
                     */
                    final MailMessage mail = mailInterface.getMessage(folderPath, uid);
                    if (mail == null) {
                        throw new MailException(MailException.Code.MAIL_NOT_FOUND, uid, folderPath);
                    }
                    final boolean wasUnseen = (mail.containsPrevSeen() && !mail.isPrevSeen());
                    final boolean doUnseen = (unseen && wasUnseen);
                    if (doUnseen) {
                        mail.setFlag(MailMessage.FLAG_SEEN, false);
                        final int unreadMsgs = mail.getUnreadMessages();
                        mail.setUnreadMessages(unreadMsgs < 0 ? 0 : unreadMsgs + 1);
                    }
                    data = formatMessageHeaders(mail.getHeadersIterator());
                    if (doUnseen) {
                        /*
                         * Leave mail as unseen
                         */
                        mailInterface.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, false);
                    } else if (wasUnseen) {
                        try {
                            final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                            final int contextId = session.getContextId();
                            final int userId = session.getUserId();
                            if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailAccess(
                                contextId,
                                userId).booleanValue()) {
                                triggerContactCollector(session, mail);
                            }
                        } catch (final SettingException e) {
                            LOG.warn("Contact collector could not be triggered.", e);
                        }
                    }
                } else {
                    final UserSettingMail usmNoSave = (UserSettingMail) session.getUserSettingMail().clone();
                    /*
                     * Deny saving for this request-specific settings
                     */
                    usmNoSave.setNoSave(true);
                    /*
                     * Overwrite settings with request's parameters
                     */
                    final DisplayMode displayMode = detectDisplayMode(editDraft, view, usmNoSave);
                    final FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(folderPath);
                    final JSONMessageCache cache = JSONMessageCache.getInstance();
                    /*
                     * Fetch either from JSON message cache or fetch on-the-fly from storage
                     */
                    final int accountId = fa.getAccountId();
                    final String fullname = fa.getFullname();
                    boolean fetchFromStorage = true;
                    if (null != cache) {
                        final JSONObject rawJSONMailObject = cache.remove(accountId, fullname, uid, session); // switch to get?
                        if (null != rawJSONMailObject) {
                            fetchFromStorage = false;
                            /*
                             * Check if message should be marked as seen
                             */
                            final String flagsKey = MailJSONField.FLAGS.getKey();
                            final int flags = rawJSONMailObject.getInt(flagsKey);
                            final boolean wasUnseen = ((flags & MailMessage.FLAG_SEEN) == 0);
                            final boolean doUnseen = (unseen && wasUnseen);
                            if (!doUnseen && wasUnseen) {
                                /*
                                 * Mark message as seen
                                 */
                                final ThreadPoolService threadPool =
                                    ServerServiceRegistry.getInstance().getService(ThreadPoolService.class);
                                if (null == threadPool) {
                                    // In this thread
                                    mailInterface.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, true);
                                } else {
                                    // In another thread
                                    final org.apache.commons.logging.Log logger = LOG;
                                    final MailServletInterface msi = mailInterface;
                                    final Callable<Object> seenCallable = new Callable<Object>() {

                                        public Object call() throws Exception {
                                            try {
                                                msi.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, true);
                                                return null;
                                            } catch (final Exception e) {
                                                logger.error(e.getMessage(), e);
                                                throw e;
                                            }
                                        }
                                    };
                                    threadPool.submit(ThreadPools.task(seenCallable));
                                }
                                /*
                                 * Set \Seen flag in JSON mail object
                                 */
                                rawJSONMailObject.put(flagsKey, (flags | MailMessage.FLAG_SEEN));
                                /*
                                 * Decrement UNREAD count
                                 */
                                final String unreadKey = MailJSONField.UNREAD.getKey();
                                final int unread = rawJSONMailObject.optInt(unreadKey);
                                rawJSONMailObject.put(unreadKey, unread > 0 ? unread - 1 : unread);
                            }
                            /*
                             * Turn to request-specific JSON mail object
                             */
                            final JSONObject mailObject =
                                new JSONObjectConverter(rawJSONMailObject, displayMode, session, usmNoSave, session.getContext()).raw2Json();
                            if (wasUnseen) {
                                try {
                                    final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                                    final int contextId = session.getContextId();
                                    final int userId = session.getUserId();
                                    if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailAccess(
                                        contextId,
                                        userId).booleanValue()) {
                                        triggerContactCollector(session, mailObject);
                                    }
                                } catch (final SettingException e) {
                                    LOG.warn("Contact collector could not be triggered.", e);
                                }
                            }
                            data = mailObject;
                        }
                    }
                    if (fetchFromStorage) {
                        /*
                         * Get message
                         */
                        final MailMessage mail = mailInterface.getMessage(folderPath, uid);
                        if (mail == null) {
                            throw new MailException(MailException.Code.MAIL_NOT_FOUND, uid, folderPath);
                        }
                        final boolean wasUnseen = (mail.containsPrevSeen() && !mail.isPrevSeen());
                        final boolean doUnseen = (unseen && wasUnseen);
                        if (doUnseen) {
                            mail.setFlag(MailMessage.FLAG_SEEN, false);
                            final int unreadMsgs = mail.getUnreadMessages();
                            mail.setUnreadMessages(unreadMsgs < 0 ? 0 : unreadMsgs + 1);
                        }
                        data = MessageWriter.writeMailMessage(mailInterface.getAccountID(), mail, displayMode, session, usmNoSave);
                        if (doUnseen) {
                            /*
                             * Leave mail as unseen
                             */
                            mailInterface.updateMessageFlags(folderPath, new String[] { uid }, MailMessage.FLAG_SEEN, false);
                        } else if (wasUnseen) {
                            try {
                                final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                                final int contextId = session.getContextId();
                                final int userId = session.getUserId();
                                if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailAccess(
                                    contextId,
                                    userId).booleanValue()) {
                                    triggerContactCollector(session, mail);
                                }
                            } catch (final SettingException e) {
                                LOG.warn("Contact collector could not be triggered.", e);
                            }
                        }
                    }
                    if (DEBUG) {
                        final long d = System.currentTimeMillis() - s;
                        LOG.debug(new StringBuilder(32).append("/ajax/mail?action=get performed in ").append(d).append(
                            "msec served from message ").append(fetchFromStorage ? "storage" : "cache"));
                    }
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        return response;
    }

    private static DisplayMode detectDisplayMode(final boolean editDraft, final String view, final UserSettingMail usmNoSave) {
        final DisplayMode displayMode;
        if (null != view) {
            if (VIEW_RAW.equals(view)) {
                displayMode = DisplayMode.RAW;
            } else if (VIEW_TEXT.equals(view)) {
                usmNoSave.setDisplayHtmlInlineContent(false);
                displayMode = editDraft ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else if (VIEW_HTML.equals(view)) {
                usmNoSave.setDisplayHtmlInlineContent(true);
                usmNoSave.setAllowHTMLImages(true);
                displayMode = editDraft ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else if (VIEW_HTML_BLOCKED_IMAGES.equals(view)) {
                usmNoSave.setDisplayHtmlInlineContent(true);
                usmNoSave.setAllowHTMLImages(false);
                displayMode = editDraft ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            } else {
                LOG.warn(new StringBuilder(64).append("Unknown value in parameter ").append(PARAMETER_VIEW).append(": ").append(view).append(
                    ". Using user's mail settings as fallback."));
                displayMode = editDraft ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
            }
        } else {
            displayMode = editDraft ? DisplayMode.MODIFYABLE : DisplayMode.DISPLAY;
        }
        return displayMode;
    }

    private static void triggerContactCollector(final ServerSession session, final MailMessage mail) {
        final ContactCollectorService ccs = ServerServiceRegistry.getInstance().getService(ContactCollectorService.class);
        if (null != ccs) {
            final Set<InternetAddress> addrs = new HashSet<InternetAddress>();
            addrs.addAll(Arrays.asList(mail.getFrom()));
            addrs.addAll(Arrays.asList(mail.getTo()));
            addrs.addAll(Arrays.asList(mail.getCc()));
            addrs.addAll(Arrays.asList(mail.getBcc()));
            // Strip by aliases
            try {
                final Set<InternetAddress> validAddrs = new HashSet<InternetAddress>(4);
                final UserSettingMail usm = session.getUserSettingMail();
                if (usm.getSendAddr() != null && usm.getSendAddr().length() > 0) {
                    validAddrs.add(new QuotedInternetAddress(usm.getSendAddr()));
                }
                final User user = UserStorage.getStorageUser(session.getUserId(), session.getContextId());
                validAddrs.add(new QuotedInternetAddress(user.getMail()));
                final String[] aliases = user.getAliases();
                for (final String alias : aliases) {
                    validAddrs.add(new QuotedInternetAddress(alias));
                }
                addrs.removeAll(validAddrs);
            } catch (final AddressException e) {
                LOG.warn("Collected contacts could not be stripped by user's email aliases: " + e.getMessage(), e);

            }
            if (!addrs.isEmpty()) {
                // Add addresses
                ccs.memorizeAddresses(new ArrayList<InternetAddress>(addrs), session);
            }
        }
    }

    private static void triggerContactCollector(final ServerSession session, final JSONObject mail) {
        final ContactCollectorService ccs = ServerServiceRegistry.getInstance().getService(ContactCollectorService.class);
        if (null != ccs) {
            final Set<InternetAddress> addrs = new HashSet<InternetAddress>();
            try {
                addrs.addAll(Arrays.asList(parseAddressKey(MailJSONField.FROM.getKey(), mail)));
                addrs.addAll(Arrays.asList(parseAddressKey(MailJSONField.RECIPIENT_TO.getKey(), mail)));
                addrs.addAll(Arrays.asList(parseAddressKey(MailJSONField.RECIPIENT_CC.getKey(), mail)));
                addrs.addAll(Arrays.asList(parseAddressKey(MailJSONField.RECIPIENT_BCC.getKey(), mail)));
                // Strip by aliases
                final Set<InternetAddress> validAddrs = new HashSet<InternetAddress>(4);
                final UserSettingMail usm = session.getUserSettingMail();
                if (usm.getSendAddr() != null && usm.getSendAddr().length() > 0) {
                    validAddrs.add(new QuotedInternetAddress(usm.getSendAddr()));
                }
                final User user = UserStorage.getStorageUser(session.getUserId(), session.getContextId());
                validAddrs.add(new QuotedInternetAddress(user.getMail()));
                final String[] aliases = user.getAliases();
                for (final String alias : aliases) {
                    validAddrs.add(new QuotedInternetAddress(alias));
                }
                addrs.removeAll(validAddrs);
            } catch (final AddressException e) {
                LOG.warn(MessageFormat.format("Contact collector could not be triggered: {0}", e.getMessage()), e);
            } catch (final JSONException e) {
                LOG.warn(MessageFormat.format("Contact collector could not be triggered: {0}", e.getMessage()), e);
            }
            if (!addrs.isEmpty()) {
                // Add addresses
                ccs.memorizeAddresses(new ArrayList<InternetAddress>(addrs), session);
            }
        }
    }

    private static final String formatMessageHeaders(final Iterator<Map.Entry<String, String>> iter) {
        final StringBuilder sb = new StringBuilder(1024);
        while (iter.hasNext()) {
            final Map.Entry<String, String> entry = iter.next();
            sb.append(entry.getKey()).append(STR_DELIM).append(entry.getValue()).append(STR_CRLF);
        }
        return sb.toString();
    }

    public void actionGetNew(final ServerSession session, final JSONWriter writer, final JSONObject requestObj, final MailServletInterface mi) throws SearchIteratorException, JSONException {
        ResponseWriter.write(actionGetNew(session, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi), writer);
    }

    private final void actionGetNew(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(
                actionGetNew(getSessionObject(req), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null),
                resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        } catch (final SearchIteratorException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            response.setException(e);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionGetNew(final ServerSession session, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException, SearchIteratorException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        SearchIterator<MailMessage> it = null;
        try {
            /*
             * Read in parameters
             */
            final String folderId = paramContainer.checkStringParam(PARAMETER_MAILFOLDER);
            final int[] columns = paramContainer.checkIntArrayParam(PARAMETER_COLUMNS);
            final String sort = paramContainer.getStringParam(PARAMETER_SORT);
            final String order = paramContainer.getStringParam(PARAMETER_ORDER);
            final int limit = paramContainer.getIntParam(PARAMETER_LIMIT);
            /*
             * Get new mails
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                /*
                 * Receive message iterator
                 */
                final int sortCol = sort == null ? MailListField.RECEIVED_DATE.getField() : Integer.parseInt(sort);
                int orderDir = OrderDirection.ASC.getOrder();
                if (order != null) {
                    if (order.equalsIgnoreCase(STR_ASC)) {
                        orderDir = OrderDirection.ASC.getOrder();
                    } else if (order.equalsIgnoreCase(STR_DESC)) {
                        orderDir = OrderDirection.DESC.getOrder();
                    } else {
                        throw new MailException(MailException.Code.INVALID_INT_VALUE, PARAMETER_ORDER);
                    }
                }
                /*
                 * Pre-Select field writers
                 */
                final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                it = mailInterface.getNewMessages(folderId, sortCol, orderDir, columns, limit == ParamContainer.NOT_FOUND ? -1 : limit);
                final int size = it.size();
                final int userId = session.getUserId();
                final int contextId = session.getContextId();
                for (int i = 0; i < size; i++) {
                    final MailMessage mail = it.next();
                    final JSONArray ja = new JSONArray();
                    for (final MailFieldWriter writer : writers) {
                        writer.writeField(ja, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                    }
                    jsonWriter.value(ja);
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        } finally {
            if (it != null) {
                it.close();
            }
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionGetSaveVersit(final ServerSession session, final Writer writer, final JSONObject requestObj, final MailServletInterface mi) throws JSONException, IOException {
        actionGetSaveVersit(session, writer, ParamContainer.getInstance(requestObj, EnumComponent.MAIL), mi);
    }

    private final void actionGetSaveVersit(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            actionGetSaveVersit(getSessionObject(req), resp.getWriter(), ParamContainer.getInstance(req, EnumComponent.MAIL, resp), null);
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final void actionGetSaveVersit(final ServerSession session, final Writer writer, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException, IOException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        try {
            /*
             * Read in parameters
             */
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            // final String msgUID =
            // paramContainer.checkStringParam(PARAMETER_ID);
            final String partIdentifier = paramContainer.checkStringParam(PARAMETER_MAILATTCHMENT);
            /*
             * Get new mails
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final CommonObject[] insertedObjs;
                {
                    final MailPart versitPart = mailInterface.getMessageAttachment(folderPath, uid, partIdentifier, false);
                    /*
                     * Save dependent on content type
                     */
                    final Context ctx = ContextStorage.getStorageContext(session.getContextId());
                    final List<CommonObject> retvalList = new ArrayList<CommonObject>();
                    if (versitPart.getContentType().isMimeType(MIMETypes.MIME_TEXT_X_VCARD) || versitPart.getContentType().isMimeType(
                        MIMETypes.MIME_TEXT_VCARD)) {
                        /*
                         * Save VCard
                         */
                        VersitUtility.saveVCard(
                            versitPart.getInputStream(),
                            versitPart.getContentType().getBaseType(),
                            versitPart.getContentType().containsCharsetParameter() ? versitPart.getContentType().getCharsetParameter() : MailProperties.getInstance().getDefaultMimeCharset(),
                            retvalList,
                            session,
                            ctx);
                    } else if (versitPart.getContentType().isMimeType(MIMETypes.MIME_TEXT_X_VCALENDAR) || versitPart.getContentType().isMimeType(
                        MIMETypes.MIME_TEXT_CALENDAR)) {
                        /*
                         * Save ICalendar
                         */
                        VersitUtility.saveICal(
                            versitPart.getInputStream(),
                            versitPart.getContentType().getBaseType(),
                            versitPart.getContentType().containsCharsetParameter() ? versitPart.getContentType().getCharsetParameter() : MailProperties.getInstance().getDefaultMimeCharset(),
                            retvalList,
                            session,
                            ctx);
                    } else {
                        throw new MailException(MailException.Code.UNSUPPORTED_VERSIT_ATTACHMENT, versitPart.getContentType());
                    }
                    insertedObjs = retvalList.toArray(new CommonObject[retvalList.size()]);
                }
                final JSONObject jo = new JSONObject();
                for (int i = 0; i < insertedObjs.length; i++) {
                    final CommonObject current = insertedObjs[i];
                    jo.reset();
                    jo.put(CommonFields.ID, current.getObjectID());
                    jo.put(CommonFields.FOLDER_ID, current.getParentFolderID());
                    jsonWriter.value(jo);
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        ResponseWriter.write(response, writer);
    }

    public void actionGetGetMultipleAttachments() throws MailException {
        throw new MailException(MailException.Code.UNSUPPORTED_ACTION, ACTION_ZIP_MATTACH, "Multiple servlet");
    }

    private final void actionGetMultipleAttachments(final HttpServletRequest req, final HttpServletResponse resp) {
        /*
         * Some variables
         */
        final ServerSession session = getSessionObject(req);
        boolean outSelected = false;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = checkStringParam(req, PARAMETER_FOLDERID);
            final String uid = checkStringParam(req, PARAMETER_ID);
            final String[] sequenceIds = checkStringArrayParam(req, PARAMETER_MAILATTCHMENT);
            /*
             * Get attachment
             */
            final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
            ManagedFile mf = null;
            try {
                mf = mailInterface.getMessageAttachments(folderPath, uid, sequenceIds);
                /*
                 * Set Content-Type and Content-Disposition header
                 */
                final String fileName;
                {
                    final String subject = mailInterface.getMessage(folderPath, uid).getSubject();
                    fileName = new StringBuilder(subject).append(".zip").toString();
                }
                /*
                 * We are supposed to offer attachment for download. Therefore enforce application/octet-stream and attachment disposition.
                 */
                final ContentType contentType = new ContentType();
                contentType.setPrimaryType("application");
                contentType.setSubType("octet-stream");
                resp.setContentType(contentType.toString());
                final String userAgent = req.getHeader(STR_USER_AGENT);
                final String preparedFileName =
                    getSaveAsFileName(fileName, isMSIEOnWindows(userAgent == null ? "" : userAgent), "application/zip");
                resp.setHeader(
                    "Content-disposition",
                    new StringBuilder(64).append("attachment; filename=\"").append(preparedFileName).append('"').toString());
                /*
                 * Reset response header values since we are going to directly write into servlet's output stream and then some browsers do
                 * not allow header "Pragma"
                 */
                Tools.removeCachingHeader(resp);
                final OutputStream out = resp.getOutputStream();
                outSelected = true;
                /*
                 * Write from content's input stream to response output stream
                 */
                final InputStream zipInputStream = mf.getInputStream();
                try {
                    final byte[] buffer = new byte[0xFFFF];
                    for (int len; (len = zipInputStream.read(buffer, 0, buffer.length)) != -1;) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                } finally {
                    zipInputStream.close();
                }
            } finally {
                if (mailInterface != null) {
                    mailInterface.close(true);
                }
                if (null != mf) {
                    mf.delete();
                    mf = null;
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            callbackError(resp, outSelected, true, e);
        } catch (final Exception e) {
            final AbstractOXException exc = getWrappingOXException(e);
            LOG.error(exc.getMessage(), exc);
            callbackError(resp, outSelected, true, exc);
        }
    }

    public void actionGetAttachment() throws MailException {
        throw new MailException(MailException.Code.UNSUPPORTED_ACTION, ACTION_MATTACH, "Multiple servlet");
    }

    /**
     * Looks up a mail attachment and writes its content directly into response output stream. This method is not accessible via Multiple
     * servlet
     */
    private final void actionGetAttachment(final HttpServletRequest req, final HttpServletResponse resp) {
        /*
         * Some variables
         */
        final ServerSession session = getSessionObject(req);
        boolean outSelected = false;
        boolean saveToDisk = false;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final String folderPath = checkStringParam(req, PARAMETER_FOLDERID);
            final String uid = checkStringParam(req, PARAMETER_ID);
            final String sequenceId = req.getParameter(PARAMETER_MAILATTCHMENT);
            final String imageContentId = req.getParameter(PARAMETER_MAILCID);
            {
                final String saveParam = req.getParameter(PARAMETER_SAVE);
                saveToDisk = ((saveParam == null || saveParam.length() == 0) ? false : ((Integer.parseInt(saveParam)) > 0));
            }
            final boolean filter;
            {
                final String filterParam = req.getParameter(PARAMETER_FILTER);
                filter = Boolean.parseBoolean(filterParam) || STR_1.equals(filterParam);
            }
            /*
             * Get attachment
             */
            final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
            try {
                if (sequenceId == null && imageContentId == null) {
                    throw new MailException(MailException.Code.MISSING_PARAM, new StringBuilder().append(PARAMETER_MAILATTCHMENT).append(
                        " | ").append(PARAMETER_MAILCID).toString());
                }
                final MailPart mailPart;
                InputStream attachmentInputStream;
                if (imageContentId == null) {
                    mailPart = mailInterface.getMessageAttachment(folderPath, uid, sequenceId, !saveToDisk);
                    if (mailPart == null) {
                        throw new MailException(MailException.Code.NO_ATTACHMENT_FOUND, sequenceId);
                    }
                    if (filter && !saveToDisk && mailPart.getContentType().isMimeType(MIMETypes.MIME_TEXT_HTM_ALL)) {
                        /*
                         * Apply filter
                         */
                        final ContentType contentType = mailPart.getContentType();
                        final String cs =
                            contentType.containsCharsetParameter() ? contentType.getCharsetParameter() : MailProperties.getInstance().getDefaultMimeCharset();
                        final String htmlContent = MessageUtility.readMailPart(mailPart, cs);
                        final HTMLFilterHandler filterHandler = new HTMLFilterHandler(htmlContent.length());
                        HTMLParser.parse(HTMLProcessing.getConformHTML(htmlContent, contentType), filterHandler);
                        attachmentInputStream = new UnsynchronizedByteArrayInputStream(filterHandler.getHTML().getBytes(cs));
                    } else {
                        attachmentInputStream = mailPart.getInputStream();
                    }
                    /*-
                     * TODO: Does not work, yet.
                     * 
                     * if (!saveToDisk &amp;&amp; mailPart.getContentType().isMimeType(MIMETypes.MIME_MESSAGE_RFC822)) {
                     *     // Treat as a mail get
                     *     final MailMessage mail = (MailMessage) mailPart.getContent();
                     *     final Response response = new Response();
                     *     response.setData(MessageWriter.writeMailMessage(mail, true, session));
                     *     response.setTimestamp(null);
                     *     ResponseWriter.write(response, resp.getWriter());
                     *     return;
                     * }
                     */
                } else {
                    mailPart = mailInterface.getMessageImage(folderPath, uid, imageContentId);
                    if (mailPart == null) {
                        throw new MailException(MailException.Code.NO_ATTACHMENT_FOUND, sequenceId);
                    }
                    attachmentInputStream = mailPart.getInputStream();
                }
                /*
                 * Set Content-Type and Content-Disposition header
                 */
                final String fileName = mailPart.getFileName();
                if (saveToDisk) {
                    /*
                     * We are supposed to offer attachment for download. Therefore enforce application/octet-stream and attachment
                     * disposition.
                     */
                    final ContentType contentType = new ContentType();
                    contentType.setPrimaryType("application");
                    contentType.setSubType("octet-stream");
                    resp.setContentType(contentType.toString());
                    final String preparedFileName =
                        getSaveAsFileName(fileName, isMSIEOnWindows(req.getHeader(STR_USER_AGENT)), mailPart.getContentType().toString());
                    resp.setHeader(
                        "Content-disposition",
                        new StringBuilder(64).append("attachment; filename=\"").append(preparedFileName).append('"').toString());
                } else {
                    final CheckedDownload checkedDownload =
                        DownloadUtility.checkInlineDownload(
                            attachmentInputStream,
                            fileName,
                            mailPart.getContentType().toString(),
                            req.getHeader(STR_USER_AGENT));
                    resp.setContentType(checkedDownload.getContentType());
                    resp.setHeader("Content-disposition", checkedDownload.getContentDisposition());
                    attachmentInputStream = checkedDownload.getInputStream();
                }
                /*
                 * Reset response header values since we are going to directly write into servlet's output stream and then some browsers do
                 * not allow header "Pragma"
                 */
                Tools.removeCachingHeader(resp);
                final OutputStream out = resp.getOutputStream();
                outSelected = true;
                /*
                 * Write from content's input stream to response output stream
                 */
                try {
                    final byte[] buffer = new byte[0xFFFF];
                    for (int len; (len = attachmentInputStream.read(buffer, 0, buffer.length)) != -1;) {
                        out.write(buffer, 0, len);
                    }
                    out.flush();
                } finally {
                    attachmentInputStream.close();
                }
            } finally {
                if (mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            callbackError(resp, outSelected, saveToDisk, e);
        } catch (final Exception e) {
            final AbstractOXException exc = getWrappingOXException(e);
            LOG.error(exc.getMessage(), exc);
            callbackError(resp, outSelected, saveToDisk, exc);
        }
    }

    private static void callbackError(final HttpServletResponse resp, final boolean outSelected, final boolean saveToDisk, final AbstractOXException e) {
        try {
            resp.setContentType(MIME_TEXT_HTML_CHARSET_UTF_8);
            final Writer writer;
            if (outSelected) {
                /*
                 * Output stream has already been selected
                 */
                Tools.disableCaching(resp);
                writer =
                    new PrintWriter(new BufferedWriter(new OutputStreamWriter(resp.getOutputStream(), resp.getCharacterEncoding())), true);
            } else {
                writer = resp.getWriter();
            }
            resp.setHeader(STR_CONTENT_DISPOSITION, null);
            final Response response = new Response();
            response.setException(e);
            final String callback = saveToDisk ? JS_FRAGMENT : JS_FRAGMENT_POPUP;
            writer.write(callback.replaceFirst(JS_FRAGMENT_JSON, Matcher.quoteReplacement(ResponseWriter.getJSON(response).toString())).replaceFirst(
                JS_FRAGMENT_ACTION,
                "error"));
            writer.flush();
        } catch (final UnsupportedEncodingException uee) {
            uee.initCause(e);
            LOG.error(uee.getMessage(), uee);
        } catch (final IOException ioe) {
            ioe.initCause(e);
            LOG.error(ioe.getMessage(), ioe);
        } catch (final IllegalStateException ise) {
            ise.initCause(e);
            LOG.error(ise.getMessage(), ise);
        } catch (final JSONException je) {
            je.initCause(e);
            LOG.error(je.getMessage(), je);
        }
    }

    private static boolean isMSIEOnWindows(final String userAgent) {
        final BrowserDetector browserDetector = new BrowserDetector(userAgent);
        return (browserDetector.isMSIE() && browserDetector.isWindows());
    }

    private static final Pattern PART_FILENAME_PATTERN = Pattern.compile("(part )([0-9]+)(?:(\\.)([0-9]+))*", Pattern.CASE_INSENSITIVE);

    private static final Pattern PAT_BSLASH = Pattern.compile("\\\\");

    private static final Pattern PAT_QUOTE = Pattern.compile("\"");

    private static final String DEFAULT_FILENAME = "file.dat";

    public static final String getSaveAsFileName(final String fileName, final boolean internetExplorer, final String baseCT) {
        if (null == fileName) {
            return DEFAULT_FILENAME;
        }
        final StringBuilder tmp = new StringBuilder(32);
        final Matcher m = PART_FILENAME_PATTERN.matcher(fileName);
        if (m.matches()) {
            tmp.append(fileName.replaceAll(" ", "_"));
        } else {
            try {
                tmp.append(Helper.encodeFilename(fileName, STR_UTF8, internetExplorer));
            } catch (final UnsupportedEncodingException e) {
                LOG.error("Unsupported encoding in a message detected and monitored: \"" + STR_UTF8 + '"', e);
                MailServletInterface.mailInterfaceMonitor.addUnsupportedEncodingExceptions(STR_UTF8);
                return fileName;
            }
        }
        if (null != baseCT) {
            if (baseCT.regionMatches(true, 0, MIME_TEXT_PLAIN, 0, MIME_TEXT_PLAIN.length())) {
                if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".txt")) {
                    tmp.append(".txt");
                }
            } else if (baseCT.regionMatches(true, 0, MIME_TEXT_HTML, 0, MIME_TEXT_HTML.length())) {
                if (!fileName.toLowerCase(Locale.ENGLISH).endsWith(".htm") && !fileName.toLowerCase(Locale.ENGLISH).endsWith(".html")) {
                    tmp.append(".html");
                }
            }
        }
        return PAT_QUOTE.matcher(PAT_BSLASH.matcher(tmp.toString()).replaceAll("\\\\\\\\")).replaceAll("\\\\\\\"");
    }

    public void actionPutForwardMultiple(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutForwardMultiple(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutForwardMultiple(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutForwardMultiple(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutForwardMultiple(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        Object data = JSONObject.NULL;
        /*
         * Start response
         */
        try {
            /*
             * Read in parameters
             */
            final JSONArray paths = new JSONArray(body);
            final String[] folders = new String[paths.length()];
            final String[] ids = new String[paths.length()];
            for (int i = 0; i < folders.length; i++) {
                final JSONObject folderAndID = paths.getJSONObject(i);
                folders[i] = folderAndID.getString(PARAMETER_FOLDERID);
                ids[i] = folderAndID.getString(PARAMETER_ID);
            }
            final String view = paramContainer.getStringParam(PARAMETER_VIEW);
            final UserSettingMail usmNoSave = (UserSettingMail) session.getUserSettingMail().clone();
            /*
             * Deny saving for this request-specific settings
             */
            usmNoSave.setNoSave(true);
            /*
             * Overwrite settings with request's parameters
             */
            if (null != view) {
                if (VIEW_TEXT.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(false);
                } else if (VIEW_HTML.equals(view)) {
                    usmNoSave.setDisplayHtmlInlineContent(true);
                    usmNoSave.setAllowHTMLImages(true);
                } else {
                    LOG.warn(new StringBuilder(64).append("Unknown value in parameter ").append(PARAMETER_VIEW).append(": ").append(view).append(
                        ". Using user's mail settings as fallback."));
                }
            }
            /*
             * Get forward message
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                data =
                    MessageWriter.writeMailMessage(mailInterface.getAccountID(), mailInterface.getForwardMessageForDisplay(
                        folders,
                        ids,
                        usmNoSave), DisplayMode.MODIFYABLE, session, usmNoSave);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(data);
        response.setTimestamp(null);
        return response;
    }

    public void actionPutReply(final ServerSession session, final boolean replyAll, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutReply(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), replyAll, mi), writer);
    }

    private final void actionPutReply(final HttpServletRequest req, final HttpServletResponse resp, final boolean replyAll) throws IOException {
        try {
            ResponseWriter.write(actionPutReply(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), replyAll, null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutReply(final ServerSession session, final String body, final ParamContainer paramContainer, final boolean replyAll, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Create new parameter container from body data...
         */
        final JSONArray paths = new JSONArray(body);
        final int length = paths.length();
        if (length != 1) {
            throw new IllegalArgumentException("JSON array's length is not 1");
        }
        final Map<String, String> map = newHashMap(2);
        for (int i = 0; i < length; i++) {
            final JSONObject folderAndID = paths.getJSONObject(i);
            map.put(PARAMETER_FOLDERID, folderAndID.getString(PARAMETER_FOLDERID));
            map.put(PARAMETER_ID, folderAndID.get(PARAMETER_ID).toString());
        }
        /*
         * ... and fake a GET request
         */
        return actionGetReply(session, replyAll, ParamContainer.getInstance(map, EnumComponent.MAIL), mailInterfaceArg);
    }

    public void actionPutGet(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutGet(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutGet(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutGet(
                getSessionObject(req),
                getBody(req),
                ParamContainer.getInstance(req, EnumComponent.MAIL, resp),
                null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutGet(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Create new parameter container from body data...
         */
        final JSONArray paths = new JSONArray(body);
        final int length = paths.length();
        if (length != 1) {
            throw new IllegalArgumentException("JSON array's length is not 1");
        }
        final Map<String, String> map = newHashMap(2);
        for (int i = 0; i < length; i++) {
            final JSONObject folderAndID = paths.getJSONObject(i);
            map.put(PARAMETER_FOLDERID, folderAndID.getString(PARAMETER_FOLDERID));
            map.put(PARAMETER_ID, folderAndID.get(PARAMETER_ID).toString());
        }
        try {
            String tmp = paramContainer.getStringParam(PARAMETER_SHOW_SRC);
            if (STR_1.equals(tmp) || Boolean.parseBoolean(tmp)) { // showMessageSource
                map.put(PARAMETER_SHOW_SRC, tmp);
            }
            tmp = paramContainer.getStringParam(PARAMETER_EDIT_DRAFT);
            if (STR_1.equals(tmp) || Boolean.parseBoolean(tmp)) { // editDraft
                map.put(PARAMETER_EDIT_DRAFT, tmp);
            }
            tmp = paramContainer.getStringParam(PARAMETER_SHOW_HEADER);
            if (STR_1.equals(tmp) || Boolean.parseBoolean(tmp)) { // showMessageHeaders
                map.put(PARAMETER_SHOW_HEADER, tmp);
            }
            tmp = paramContainer.getStringParam(PARAMETER_SAVE);
            if (tmp != null && tmp.length() > 0 && Integer.parseInt(tmp) > 0) { // saveToDisk
                map.put(PARAMETER_SAVE, tmp);
            }
            tmp = paramContainer.getStringParam(PARAMETER_VIEW);
            if (tmp != null) { // view
                map.put(PARAMETER_VIEW, tmp);
            }
            tmp = paramContainer.getStringParam(PARAMETER_UNSEEN);
            if (tmp != null) { // unseen
                map.put(PARAMETER_UNSEEN, tmp);
            }
            tmp = null;
        } catch (final AbstractOXException e) {
            final Response response = new Response();
            response.setException(e);
            return response;
        }
        /*
         * ... and fake a GET request
         */
        return actionGetMessage(session, ParamContainer.getInstance(map, EnumComponent.MAIL), mailInterfaceArg);
    }

    public void actionPutAutosave(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutAutosave(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutAutosave(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutAutosave(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutAutosave(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        try {
            /*
             * Autosave draft
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                String msgIdentifier = null;
                {
                    final JSONObject jsonMailObj = new JSONObject(body);
                    /*
                     * Parse with default account's transport provider
                     */
                    final ComposedMailMessage composedMail =
                        MessageParser.parse4Draft(jsonMailObj, (UploadEvent) null, session, MailAccount.DEFAULT_ID);
                    if ((composedMail.getFlags() & MailMessage.FLAG_DRAFT) == 0) {
                        LOG.warn("Missing \\Draft flag on action=autosave in JSON message object", new Throwable());
                        composedMail.setFlag(MailMessage.FLAG_DRAFT, true);
                    }
                    if ((composedMail.getFlags() & MailMessage.FLAG_DRAFT) == MailMessage.FLAG_DRAFT) {
                        /*
                         * ... and autosave draft
                         */
                        int accountId;
                        if (composedMail.containsFrom()) {
                            accountId = resolveFrom2Account(session, composedMail.getFrom()[0], false, true);
                        } else {
                            accountId = MailAccount.DEFAULT_ID;
                        }
                        /*
                         * Check if detected account has a drafts folder
                         */
                        if (mailInterface.getDraftsFolder(accountId) == null) {
                            if (MailAccount.DEFAULT_ID == accountId) {
                                // Huh... No drafts folder in default account
                                throw new MailException(MailException.Code.FOLDER_NOT_FOUND, "Drafts");
                            }
                            LOG.warn(new StringBuilder(64).append("Mail account ").append(accountId).append(" for user ").append(
                                session.getUserId()).append(" in context ").append(session.getContextId()).append(
                                " has no drafts folder. Saving draft to default account's draft folder."));
                            // No drafts folder in detected mail account; auto-save to default account
                            accountId = MailAccount.DEFAULT_ID;
                            composedMail.setFolder(mailInterface.getDraftsFolder(accountId));
                        }
                        msgIdentifier = mailInterface.saveDraft(composedMail, true, accountId);
                    } else {
                        throw new MailException(MailException.Code.UNEXPECTED_ERROR, "No new message on action=edit");
                    }
                }
                if (msgIdentifier == null) {
                    throw new MailException(MailException.Code.SEND_FAILED_UNKNOWN);
                }
                /*
                 * Fill JSON response object
                 */
                response.setData(msgIdentifier);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setTimestamp(null);
        return response;
    }

    public void actionPutClear(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutClear(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutClear(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutClear(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutClear(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        try {
            /*
             * Parse body
             */
            final JSONArray ja = new JSONArray(body);
            final int length = ja.length();
            if (length > 0) {
                MailServletInterface mailInterface = mailInterfaceArg;
                boolean closeMailInterface = false;
                try {
                    if (mailInterface == null) {
                        mailInterface = MailServletInterface.getInstance(session);
                        closeMailInterface = true;
                    }
                    /*
                     * Clear folder sequentially
                     */
                    for (int i = 0; i < length; i++) {
                        final String folderId = ja.getString(i);
                        if (!mailInterface.clearFolder(folderId)) {
                            /*
                             * Something went wrong
                             */
                            jsonWriter.value(folderId);
                        }
                    }
                } finally {
                    if (closeMailInterface && mailInterface != null) {
                        mailInterface.close(true);
                    }
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionPutMailSearch(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException, SearchIteratorException {
        ResponseWriter.write(actionPutMailSearch(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutMailSearch(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutMailSearch(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        } catch (final SearchIteratorException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            response.setException(e);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutMailSearch(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException, SearchIteratorException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        SearchIterator<MailMessage> it = null;
        try {
            /*
             * Read in parameters
             */
            final String folderId = paramContainer.checkStringParam(PARAMETER_MAILFOLDER);
            final int[] columns = paramContainer.checkIntArrayParam(PARAMETER_COLUMNS);
            final String sort = paramContainer.getStringParam(PARAMETER_SORT);
            final String order = paramContainer.getStringParam(PARAMETER_ORDER);
            if (sort != null && order == null) {
                throw new MailException(MailException.Code.MISSING_PARAM, PARAMETER_ORDER);
            }
            final JSONValue searchValue;
            if (startsWith('[', body, true)) {
                searchValue = new JSONArray(body);
            } else if (startsWith('{', body, true)) {
                searchValue = new JSONObject(body);
            } else {
                throw new JSONException(MessageFormat.format("Request body is not a JSON value: {0}", body));
            }
            /*
             * Perform search dependent on passed JSON value
             */
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
                        searchCols[i] = tmp.getInt(PARAMETER_COL);
                        searchPats[i] = tmp.getString(PARAMETER_SEARCHPATTERN);
                    }
                    /*
                     * Search mails
                     */
                    MailServletInterface mailInterface = mailInterfaceArg;
                    boolean closeMailInterface = false;
                    try {
                        if (mailInterface == null) {
                            mailInterface = MailServletInterface.getInstance(session);
                            closeMailInterface = true;
                        }
                        /*
                         * Pre-Select field writers
                         */
                        final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                        final int userId = session.getUserId();
                        final int contextId = session.getContextId();
                        int orderDir = OrderDirection.ASC.getOrder();
                        if (order != null) {
                            if (order.equalsIgnoreCase(STR_ASC)) {
                                orderDir = OrderDirection.ASC.getOrder();
                            } else if (order.equalsIgnoreCase(STR_DESC)) {
                                orderDir = OrderDirection.DESC.getOrder();
                            } else {
                                throw new MailException(MailException.Code.INVALID_INT_VALUE, PARAMETER_ORDER);
                            }
                        }
                        if ((STR_THREAD.equalsIgnoreCase(sort))) {
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
                        if (closeMailInterface && mailInterface != null) {
                            mailInterface.close(true);
                        }
                    }
                }
            } else {
                final JSONObject searchObject = ((JSONObject) searchValue).getJSONObject(PARAMETER_FILTER);
                /*
                 * Search mails
                 */
                MailServletInterface mailInterface = mailInterfaceArg;
                boolean closeMailInterface = false;
                try {
                    if (mailInterface == null) {
                        mailInterface = MailServletInterface.getInstance(session);
                        closeMailInterface = true;
                    }
                    /*
                     * Pre-Select field writers
                     */
                    final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
                    final int userId = session.getUserId();
                    final int contextId = session.getContextId();
                    int orderDir = OrderDirection.ASC.getOrder();
                    if (order != null) {
                        if (order.equalsIgnoreCase(STR_ASC)) {
                            orderDir = OrderDirection.ASC.getOrder();
                        } else if (order.equalsIgnoreCase(STR_DESC)) {
                            orderDir = OrderDirection.DESC.getOrder();
                        } else {
                            throw new MailException(MailException.Code.INVALID_INT_VALUE, PARAMETER_ORDER);
                        }
                    }
                    final int sortCol = sort == null ? MailListField.RECEIVED_DATE.getField() : Integer.parseInt(sort);
                    it = mailInterface.getMessages(folderId, null, sortCol, orderDir, SearchTermParser.parse(searchObject), true, columns);
                    final int size = it.size();
                    for (int i = 0; i < size; i++) {
                        final MailMessage mail = it.next();
                        final JSONArray arr = new JSONArray();
                        for (final MailFieldWriter writer : writers) {
                            writer.writeField(arr, mail, 0, false, mailInterface.getAccountID(), userId, contextId);
                        }
                        jsonWriter.value(arr);
                    }
                } finally {
                    if (closeMailInterface && mailInterface != null) {
                        mailInterface.close(true);
                    }
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        } finally {
            if (it != null) {
                it.close();
            }
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionPutMailList(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutMailList(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutMailList(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutMailList(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private static final Pattern SPLIT = Pattern.compile(" *, *");

    private final Response actionPutMailList(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        final long start = DEBUG ? System.currentTimeMillis() : ZERO;
        jsonWriter.array();
        try {
            final int[] columns = paramContainer.checkIntArrayParam(PARAMETER_COLUMNS);
            final String[] headers;
            {
                final String tmp = paramContainer.getStringParam(PARAMETER_HEADERS);
                headers = null == tmp ? null : SPLIT.split(tmp, 0);
            }
            /*
             * Pre-Select field writers
             */
            final MailFieldWriter[] writers = MessageWriter.getMailFieldWriter(MailListField.getFields(columns));
            final MailFieldWriter[] headerWriters = null == headers ? null : MessageWriter.getHeaderFieldWriter(headers);
            /*
             * Get map
             */
            final Map<String, List<String>> idMap = fillMapByArray(new JSONArray(body));
            if (idMap.isEmpty()) {
                /*
                 * Request body is an empty JSON array
                 */
                if (LOG.isWarnEnabled()) {
                    LOG.warn("Empty JSON array detected in request body.", new Throwable());
                }
                final Response r = new Response();
                r.setData(EMPTY_JSON_ARR);
                return r;
            }
            /*
             * Proceed
             */
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final int userId = session.getUserId();
                final int contextId = session.getContextId();
                for (final Map.Entry<String, List<String>> entry : idMap.entrySet()) {
                    final MailMessage[] mails = mailInterface.getMessageList(entry.getKey(), toArray(entry.getValue()), columns, headers);
                    final int accountID = mailInterface.getAccountID();
                    for (int i = 0; i < mails.length; i++) {
                        final MailMessage mail = mails[i];
                        if (mail != null) {
                            final JSONArray ja = new JSONArray();
                            for (int j = 0; j < writers.length; j++) {
                                writers[j].writeField(ja, mail, 0, false, accountID, userId, contextId);
                            }
                            if (null != headerWriters) {
                                for (int j = 0; j < headerWriters.length; j++) {
                                    headerWriters[j].writeField(ja, mail, 0, false, accountID, userId, contextId);
                                }
                            }
                            jsonWriter.value(ja);
                        }
                    }
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        if (DEBUG) {
            final long d = System.currentTimeMillis() - start;
            LOG.debug(new StringBuilder(32).append("/ajax/mail?action=list performed in ").append(d).append("msec"));
        }
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    private static String[] toArray(final Collection<String> c) {
        return c.toArray(new String[c.size()]);
    }

    private static final Map<String, List<String>> fillMapByArray(final JSONArray idArray) throws JSONException, MailException {
        final int length = idArray.length();
        if (length <= 0) {
            return Collections.emptyMap();
        }
        final Map<String, List<String>> idMap = newHashMap(4);
        final String parameterFolderId = PARAMETER_FOLDERID;
        final String parameterId = PARAMETER_ID;
        String folder;
        List<String> list;
        {
            final JSONObject idObject = idArray.getJSONObject(0);
            folder = ensureString(parameterFolderId, idObject);
            list = new ArrayList<String>(length);
            idMap.put(folder, list);
            list.add(ensureString(parameterId, idObject));
        }
        for (int i = 1; i < length; i++) {
            final JSONObject idObject = idArray.getJSONObject(i);
            final String fld = ensureString(parameterFolderId, idObject);
            if (!folder.equals(fld)) {
                folder = fld;
                final List<String> tmp = idMap.get(folder);
                if (tmp == null) {
                    list = new ArrayList<String>(length);
                    idMap.put(folder, list);
                } else {
                    list = tmp;
                }
            }
            list.add(ensureString(parameterId, idObject));
        }
        return idMap;
    }

    private static String ensureString(final String key, final JSONObject jo) throws MailException {
        final Object value = jo.opt(key);
        if (null == value || JSONObject.NULL.equals(value)) {
            throw new MailException(MailException.Code.MISSING_PARAMETER, key);
        }
        return value.toString();
    }

    public void actionPutDeleteMails(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutDeleteMails(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutDeleteMails(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutDeleteMails(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutDeleteMails(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        try {
            final boolean hardDelete = STR_1.equals(paramContainer.getStringParam(PARAMETER_HARDDELETE));
            final JSONArray jsonIDs = new JSONArray(body);
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final int length = jsonIDs.length();
                if (length > 0) {
                    final List<MailPath> l = new ArrayList<MailPath>(length);
                    for (int i = 0; i < length; i++) {
                        final JSONObject obj = jsonIDs.getJSONObject(i);
                        final FullnameArgument fa = MailFolderUtility.prepareMailFolderParam(obj.getString(PARAMETER_FOLDERID));
                        l.add(new MailPath(fa.getAccountId(), fa.getFullname(), obj.getString(PARAMETER_ID)));
                    }
                    Collections.sort(l, MailPath.COMPARATOR);
                    String lastFldArg = l.get(0).getFolderArgument();
                    final List<String> arr = new ArrayList<String>(length);
                    for (int i = 0; i < length; i++) {
                        final MailPath current = l.get(i);
                        final String folderArgument = current.getFolderArgument();
                        if (!lastFldArg.equals(folderArgument)) {
                            /*
                             * Delete all collected UIDs til here and reset
                             */
                            final String[] uids = arr.toArray(new String[arr.size()]);
                            mailInterface.deleteMessages(lastFldArg, uids, hardDelete);
                            arr.clear();
                            lastFldArg = folderArgument;
                        }
                        arr.add(current.getMailID());
                    }
                    if (arr.size() > 0) {
                        final String[] uids = arr.toArray(new String[arr.size()]);
                        mailInterface.deleteMessages(lastFldArg, uids, hardDelete);
                    }
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionPutUpdateMail(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mailInterface) throws JSONException {
        ResponseWriter.write(actionPutUpdateMail(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mailInterface), writer);
    }

    private final void actionPutUpdateMail(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutUpdateMail(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutUpdateMail(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailIntefaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.object();
        try {
            final String sourceFolder = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final JSONObject bodyObj = new JSONObject(body);
            final String destFolder = bodyObj.hasAndNotNull(FolderFields.FOLDER_ID) ? bodyObj.getString(FolderFields.FOLDER_ID) : null;
            final Integer colorLabel =
                bodyObj.hasAndNotNull(CommonFields.COLORLABEL) ? Integer.valueOf(bodyObj.getInt(CommonFields.COLORLABEL)) : null;
            final Integer flagBits =
                bodyObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) ? Integer.valueOf(bodyObj.getInt(MailJSONField.FLAGS.getKey())) : null;
            boolean flagVal = false;
            if (flagBits != null) {
                /*
                 * Look for boolean value
                 */
                flagVal =
                    (bodyObj.has(MailJSONField.VALUE.getKey()) && !bodyObj.isNull(MailJSONField.VALUE.getKey()) ? bodyObj.getBoolean(MailJSONField.VALUE.getKey()) : false);
            }

            final Integer setFlags = bodyObj.hasAndNotNull("set_flags") ? Integer.valueOf(bodyObj.getInt("set_flags")) : null;
            final Integer clearFlags = bodyObj.hasAndNotNull("clear_flags") ? Integer.valueOf(bodyObj.getInt("clear_flags")) : null;

            MailServletInterface mailInterface = mailIntefaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }

                final String uid;
                {
                    String tmp = paramContainer.getStringParam(PARAMETER_ID);
                    if (null == tmp) {
                        tmp = paramContainer.getStringParam(PARAMETER_MESSAGE_ID);
                        if (null == tmp) {
                            throw new AjaxException(AjaxException.Code.MISSING_PARAMETER, PARAMETER_ID);
                        }
                        uid = mailInterface.getMailIDByMessageID(sourceFolder, tmp);
                    } else {
                        uid = tmp;
                    }
                }

                String folderId = sourceFolder;
                String mailId = uid;
                if (colorLabel != null) {
                    /*
                     * Update color label
                     */
                    mailInterface.updateMessageColorLabel(sourceFolder, new String[] { uid }, colorLabel.intValue());
                }
                if (flagBits != null) {
                    /*
                     * Update system flags which are allowed to be altered by client
                     */
                    mailInterface.updateMessageFlags(sourceFolder, new String[] { uid }, flagBits.intValue(), flagVal);
                }
                if (setFlags != null) {
                    /*
                     * Add system flags which are allowed to be altered by client
                     */
                    mailInterface.updateMessageFlags(sourceFolder, new String[] { uid }, setFlags.intValue(), true);
                }
                if (clearFlags != null) {
                    /*
                     * Remove system flags which are allowed to be altered by client
                     */
                    mailInterface.updateMessageFlags(sourceFolder, new String[] { uid }, clearFlags.intValue(), false);
                }
                if (destFolder != null) {
                    /*
                     * Perform move operation
                     */
                    mailId = mailInterface.copyMessages(sourceFolder, destFolder, new String[] { uid }, true)[0];
                    folderId = destFolder;
                }
                jsonWriter.key(FolderChildFields.FOLDER_ID).value(folderId);
                jsonWriter.key(DataFields.ID).value(mailId);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endObject();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionPutNewMail(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutNewMail(
            session,
            new SimpleStringProvider(jsonObj.getString(ResponseFields.DATA)),
            ParamContainer.getInstance(jsonObj, EnumComponent.MAIL)), writer);
    }

    private final void actionPutNewMail(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutNewMail(getSessionObject(req), new HTTPRequestStringProvider(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp)), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutNewMail(final ServerSession session, final StringProvider body, final ParamContainer paramContainer) {
        /*
         * Some variables
         */
        final Response response = new Response();
        /*
         * Start response
         */
        JSONValue responseData = null;
        try {
            final String src = paramContainer.checkStringParam(PARAMETER_SRC);
            if (!STR_1.equals(src) && !Boolean.parseBoolean(src)) {
                throw new MailException(MailException.Code.MISSING_PARAMETER, PARAMETER_SRC);
            }
            final String folder = paramContainer.getStringParam(PARAMETER_FOLDERID);
            if (body == null || body.isEmpty()) {
                throw new MailException(MailException.Code.MISSING_PARAMETER, PARAMETER_DATA);
            }
            final int flags = paramContainer.getIntParam(PARAMETER_FLAGS);
            final boolean force;
            {
                String tmp = paramContainer.getStringParam("force");
                if (null == tmp) {
                    force = false;
                } else {
                    tmp = tmp.trim();
                    force = "1".equals(tmp) || Boolean.parseBoolean(tmp);
                }
            }
            /*
             * Get rfc822 bytes and create corresponding mail message
             */
            final InternetAddress[] fromAddresses;
            final ManagedMimeMessage[] managedMessages;
            final MailMessage[] mails;
            {
                final String bodyStr = body.getString();
                final JSONArray array = toJSONArray(bodyStr);
                if (null == array) {
                    fromAddresses = new InternetAddress[1];
                    managedMessages = new ManagedMimeMessage[1];
                    mails = new MailMessage[1];
                    managedMessages[0] = new ManagedMimeMessage(MIMEDefaultSession.getDefaultSession(), bodyStr.getBytes("US-ASCII"));
                    final String fromAddr = managedMessages[0].getHeader(MessageHeaders.HDR_FROM, null);
                    try {
                        if (isEmpty(fromAddr)) {
                            // Add from address
                            fromAddresses[0] = new QuotedInternetAddress(getDefaultSendAddress(session), true);
                            managedMessages[0].setFrom(fromAddresses[0]);
                            mails[0] = MIMEMessageConverter.convertMessage(managedMessages[0]);
                        } else {
                            fromAddresses[0] = new QuotedInternetAddress(fromAddr, true);
                            mails[0] = MIMEMessageConverter.convertMessage(managedMessages[0]);
                        }
                    } catch (final AddressException e) {
                        throw MIMEMailException.handleMessagingException(e);
                    }
                } else {
                    /*
                     * A JSON array
                     */
                    final int len = array.length();
                    fromAddresses = new InternetAddress[len];
                    managedMessages = new ManagedMimeMessage[len];
                    mails = new MailMessage[len];
                    try {
                        QuotedInternetAddress defaultSendAddr = null;
                        for (int i = 0; i < len; i++) {
                            managedMessages[i] =
                                new ManagedMimeMessage(MIMEDefaultSession.getDefaultSession(), array.getString(i).getBytes("US-ASCII"));
                            final String fromAddr = managedMessages[i].getHeader(MessageHeaders.HDR_FROM, null);
                            if (isEmpty(fromAddr)) {
                                // Add from address
                                if (null == defaultSendAddr) {
                                    defaultSendAddr = new QuotedInternetAddress(getDefaultSendAddress(session), true);
                                }
                                fromAddresses[i] = defaultSendAddr;
                                managedMessages[i].setFrom(fromAddresses[i]);
                                mails[i] = MIMEMessageConverter.convertMessage(managedMessages[i]);
                            } else {
                                fromAddresses[i] = new QuotedInternetAddress(fromAddr, true);
                                mails[i] = MIMEMessageConverter.convertMessage(managedMessages[i]);
                            }
                        }
                    } catch (final AddressException e) {
                        throw MIMEMailException.handleMessagingException(e);
                    }
                }
            }
            try {
                /*
                 * Check if "folder" element is present which indicates to save given message as a draft or append to denoted folder
                 */
                if (folder == null) {
                    if (1 == mails.length) {
                        responseData = appendDraft(session, flags, force, fromAddresses[0], mails[0]);
                    } else {
                        final JSONArray respArray = new JSONArray();
                        for (int i = 0; i < mails.length; i++) {
                            respArray.put(appendDraft(session, flags, force, fromAddresses[i], mails[i]));
                        }
                        responseData = respArray;
                    }
                } else {
                    final String[] ids;
                    final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
                    try {
                        ids = mailInterface.appendMessages(folder, mails, force);
                    } finally {
                        mailInterface.close(true);
                    }
                    if (1 == ids.length) {
                        final JSONObject responseObj = new JSONObject();
                        responseObj.put(FolderChildFields.FOLDER_ID, folder);
                        responseObj.put(DataFields.ID, ids[0]);
                        responseData = responseObj;
                    } else {
                        final JSONArray respArray = new JSONArray();
                        for (int i = 0; i < ids.length; i++) {
                            final JSONObject responseObj = new JSONObject();
                            responseObj.put(FolderChildFields.FOLDER_ID, folder);
                            responseObj.put(DataFields.ID, ids[i]);
                            respArray.put(responseObj);
                        }
                        responseData = respArray;
                    }
                }
            } finally {
                for (int i = 0; i < managedMessages.length; i++) {
                    managedMessages[i].cleanUp();
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(responseData == null ? JSONObject.NULL : responseData);
        response.setTimestamp(null);
        return response;
    }

    private JSONObject appendDraft(final ServerSession session, final int flags, final boolean force, final InternetAddress from, final MailMessage m) throws OXException, MailException, JSONException {
        /*
         * Determine the account to transport with
         */
        final int accountId;
        {
            int accId;
            try {
                accId = resolveFrom2Account(session, from, true, !force);
            } catch (final MailException e) {
                if (MailException.Code.NO_TRANSPORT_SUPPORT.getNumber() != e.getDetailNumber()) {
                    // Re-throw
                    throw e;
                }
                LOG.warn(new StringBuilder(128).append(e.getMessage()).append(". Using default account's transport.").toString());
                // Send with default account's transport provider
                accId = MailAccount.DEFAULT_ID;
            }
            accountId = accId;
        }
        /*
         * Missing "folder" element indicates to send given message via default mail account
         */
        final MailTransport transport = MailTransport.getInstance(session, accountId);
        try {
            /*
             * Send raw message source
             */
            final MailMessage sentMail = transport.sendRawMessage(m.getSourceBytes());
            JSONObject responseData = null;
            if (!session.getUserSettingMail().isNoCopyIntoStandardSentFolder()) {
                /*
                 * Copy in sent folder allowed
                 */
                final MailAccess<?, ?> mailAccess = MailAccess.getInstance(session, accountId);
                mailAccess.connect();
                try {
                    final String sentFullname =
                        MailFolderUtility.prepareMailFolderParam(mailAccess.getFolderStorage().getSentFolder()).getFullname();
                    final String[] uidArr;
                    try {
                        /*
                         * Append to default "sent" folder
                         */
                        if (flags != ParamContainer.NOT_FOUND) {
                            sentMail.setFlags(flags);
                        }
                        uidArr = mailAccess.getMessageStorage().appendMessages(sentFullname, new MailMessage[] { sentMail });
                        try {
                            /*
                             * Update cache
                             */
                            MailMessageCache.getInstance().removeFolderMessages(
                                accountId,
                                sentFullname,
                                session.getUserId(),
                                session.getContext().getContextId());
                        } catch (final OXCachingException e) {
                            LOG.error(e.getMessage(), e);
                        }
                    } catch (final MailException e) {
                        if (e.getMessage().indexOf("quota") != -1) {
                            throw new MailException(MailException.Code.COPY_TO_SENT_FOLDER_FAILED_QUOTA, e, new Object[0]);
                        }
                        throw new MailException(MailException.Code.COPY_TO_SENT_FOLDER_FAILED, e, new Object[0]);
                    }
                    if ((uidArr != null) && (uidArr[0] != null)) {
                        /*
                         * Mark appended sent mail as seen
                         */
                        mailAccess.getMessageStorage().updateMessageFlags(sentFullname, uidArr, MailMessage.FLAG_SEEN, true);
                    }
                    /*
                     * Compose JSON object
                     */
                    responseData = new JSONObject();
                    responseData.put(FolderChildFields.FOLDER_ID, MailFolderUtility.prepareFullname(MailAccount.DEFAULT_ID, sentFullname));
                    responseData.put(DataFields.ID, uidArr[0]);
                } finally {
                    mailAccess.close(true);
                }
            }
            return responseData;
        } finally {
            transport.close();
        }
    }

    public void actionPutCopyMail(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mailInterface) throws JSONException {
        ResponseWriter.write(actionPutCopyMail(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mailInterface), writer);
    }

    private final void actionPutCopyMail(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutCopyMail(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutCopyMail(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.object();
        try {
            final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            final String sourceFolder = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final String destFolder = new JSONObject(body).getString(FolderFields.FOLDER_ID);
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final String msgUID = mailInterface.copyMessages(sourceFolder, destFolder, new String[] { uid }, false)[0];
                jsonWriter.key(FolderChildFields.FOLDER_ID).value(destFolder);
                jsonWriter.key(DataFields.ID).value(msgUID);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endObject();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public final void actionPutMoveMailMultiple(final ServerSession session, final JSONWriter writer, final String[] mailIDs, final String sourceFolder, final String destFolder, final MailServletInterface mailInteface) throws JSONException {
        actionPutMailMultiple(session, writer, mailIDs, sourceFolder, destFolder, true, mailInteface);
    }

    public final void actionPutCopyMailMultiple(final ServerSession session, final JSONWriter writer, final String[] mailIDs, final String srcFolder, final String destFolder, final MailServletInterface mailInterface) throws JSONException {
        actionPutMailMultiple(session, writer, mailIDs, srcFolder, destFolder, false, mailInterface);
    }

    public final void actionPutMailMultiple(final ServerSession session, final JSONWriter writer, final String[] mailIDs, final String srcFolder, final String destFolder, final boolean move, final MailServletInterface mailInterfaceArg) throws JSONException {
        try {
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final String[] msgUIDs = mailInterface.copyMessages(srcFolder, destFolder, mailIDs, move);
                if (msgUIDs.length > 0) {
                    final Response response = new Response();
                    for (int k = 0; k < msgUIDs.length; k++) {
                        response.reset();
                        final JSONObject jsonObj = new JSONObject();
                        // DataFields.ID | FolderChildFields.FOLDER_ID
                        jsonObj.put(FolderChildFields.FOLDER_ID, destFolder);
                        jsonObj.put(DataFields.ID, msgUIDs[k]);
                        response.setData(jsonObj);
                        response.setTimestamp(null);
                        ResponseWriter.write(response, writer);
                    }
                } else {
                    final Response response = new Response();
                    response.setData(JSONObject.NULL);
                    response.setTimestamp(null);
                    ResponseWriter.write(response, writer);
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            for (int k = 0; k < mailIDs.length; k++) {
                response.reset();
                response.setException(e);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            final Response response = new Response();
            for (int k = 0; k < mailIDs.length; k++) {
                response.reset();
                response.setException(wrapper);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        }
    }

    public void actionPutStoreFlagsMultiple(final ServerSession session, final JSONWriter writer, final String[] mailIDs, final String folder, final int flagsBits, final boolean flagValue, final MailServletInterface mailInterfaceArg) throws JSONException {
        try {
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                mailInterface.updateMessageFlags(folder, mailIDs, flagsBits, flagValue);
                final Response response = new Response();
                for (int i = 0; i < mailIDs.length; i++) {
                    response.reset();
                    final JSONObject jsonObj = new JSONObject();
                    // DataFields.ID | FolderChildFields.FOLDER_ID
                    jsonObj.put(FolderChildFields.FOLDER_ID, folder);
                    jsonObj.put(DataFields.ID, mailIDs[i]);
                    response.setData(jsonObj);
                    response.setTimestamp(null);
                    ResponseWriter.write(response, writer);
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            for (int i = 0; i < mailIDs.length; i++) {
                response.reset();
                response.setException(e);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            final Response response = new Response();
            for (int i = 0; i < mailIDs.length; i++) {
                response.reset();
                response.setException(wrapper);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        }
    }

    public void actionPutColorLabelMultiple(final ServerSession session, final JSONWriter writer, final String[] mailIDs, final String folder, final int colorLabel, final MailServletInterface mailInterfaceArg) throws JSONException {
        try {
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                mailInterface.updateMessageColorLabel(folder, mailIDs, colorLabel);
                final Response response = new Response();
                for (int i = 0; i < mailIDs.length; i++) {
                    response.reset();
                    final JSONObject jsonObj = new JSONObject();
                    // DataFields.ID | FolderChildFields.FOLDER_ID
                    jsonObj.put(FolderChildFields.FOLDER_ID, folder);
                    jsonObj.put(DataFields.ID, mailIDs[i]);
                    response.setData(jsonObj);
                    response.setTimestamp(null);
                    ResponseWriter.write(response, writer);
                }
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            final Response response = new Response();
            for (int i = 0; i < mailIDs.length; i++) {
                response.reset();
                response.setException(e);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            final Response response = new Response();
            for (int i = 0; i < mailIDs.length; i++) {
                response.reset();
                response.setException(wrapper);
                response.setData(JSONObject.NULL);
                response.setTimestamp(null);
                ResponseWriter.write(response, writer);
            }
        }
    }

    public void actionPutAttachment(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutAttachment(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutAttachment(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutAttachment(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutAttachment(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) throws JSONException {
        /*
         * Some variables
         */
        final Response response = new Response();
        final OXJSONWriter jsonWriter = new OXJSONWriter();
        /*
         * Start response
         */
        jsonWriter.array();
        try {
            final String folderPath = paramContainer.checkStringParam(PARAMETER_FOLDERID);
            final String uid = paramContainer.checkStringParam(PARAMETER_ID);
            final String sequenceId = paramContainer.checkStringParam(PARAMETER_MAILATTCHMENT);
            final String destFolderIdentifier = paramContainer.checkStringParam(PARAMETER_DESTINATION_FOLDER);
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            final InfostoreFacade db = Infostore.FACADE;
            boolean performRollback = false;
            try {
                final Context ctx = session.getContext();
                if (!session.getUserConfiguration().hasInfostore()) {
                    throw new OXPermissionException(new MailException(MailException.Code.NO_MAIL_ACCESS));
                }
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                final MailPart mailPart = mailInterface.getMessageAttachment(folderPath, uid, sequenceId, false);
                if (mailPart == null) {
                    throw new MailException(MailException.Code.NO_ATTACHMENT_FOUND, sequenceId);
                }
                final int destFolderID = Integer.parseInt(destFolderIdentifier);
                {
                    final FolderObject folderObj = new OXFolderAccess(ctx).getFolderObject(destFolderID);
                    final EffectivePermission p = folderObj.getEffectiveUserPermission(session.getUserId(), session.getUserConfiguration());
                    if (!p.isFolderVisible()) {
                        throw new OXFolderException(
                            FolderCode.NOT_VISIBLE,
                            Integer.valueOf(folderObj.getObjectID()),
                            getUserName(session),
                            Integer.valueOf(ctx.getContextId()));
                    }
                    if (!p.canWriteOwnObjects()) {
                        throw new OXFolderException(
                            FolderCode.NO_WRITE_PERMISSION,
                            getUserName(session),
                            getFolderName(folderObj),
                            Integer.valueOf(ctx.getContextId()));
                    }
                }
                /*
                 * Create document's meta data
                 */
                final InfostoreParser parser = new InfostoreParser();
                final DocumentMetadata docMetaData = parser.getDocumentMetadata(body);
                final Set<Metadata> metSet = new HashSet<Metadata>(Arrays.asList(parser.findPresentFields(body)));
                if (!metSet.contains(Metadata.FILENAME_LITERAL)) {
                    docMetaData.setFileName(mailPart.getFileName());
                }
                docMetaData.setFileMIMEType(mailPart.getContentType().toString());
                /*
                 * Since file's size given from IMAP server is just an estimation and therefore does not exactly match the file's size a
                 * future file access via webdav can fail because of the size mismatch. Thus set the file size to 0 to make the infostore
                 * measure the size.
                 */
                docMetaData.setFileSize(0);
                if (!metSet.contains(Metadata.TITLE_LITERAL)) {
                    docMetaData.setTitle(mailPart.getFileName());
                }
                docMetaData.setFolderId(destFolderID);
                /*
                 * Start writing to infostore folder
                 */
                db.startTransaction();
                performRollback = true;
                db.saveDocument(docMetaData, mailPart.getInputStream(), System.currentTimeMillis(), new ServerSessionAdapter(session, ctx));
                db.commit();
            } catch (final Exception e) {
                if (performRollback) {
                    db.rollback();
                }
                throw e;
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
                if (db != null) {
                    db.finish();
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        jsonWriter.endArray();
        response.setData(jsonWriter.getObject());
        response.setTimestamp(null);
        return response;
    }

    public void actionPutReceiptAck(final ServerSession session, final JSONWriter writer, final JSONObject jsonObj, final MailServletInterface mi) throws JSONException {
        ResponseWriter.write(actionPutReceiptAck(session, jsonObj.getString(ResponseFields.DATA), ParamContainer.getInstance(
            jsonObj,
            EnumComponent.MAIL), mi), writer);
    }

    private final void actionPutReceiptAck(final HttpServletRequest req, final HttpServletResponse resp) throws IOException {
        try {
            ResponseWriter.write(actionPutReceiptAck(getSessionObject(req), getBody(req), ParamContainer.getInstance(
                req,
                EnumComponent.MAIL,
                resp), null), resp.getWriter());
        } catch (final JSONException e) {
            final OXJSONException oxe = new OXJSONException(OXJSONException.Code.JSON_WRITE_ERROR, e, new Object[0]);
            LOG.error(oxe.getMessage(), oxe);
            final Response response = new Response();
            response.setException(oxe);
            try {
                ResponseWriter.write(response, resp.getWriter());
            } catch (final JSONException e1) {
                LOG.error(RESPONSE_ERROR, e1);
                sendError(resp);
            }
        }
    }

    private final Response actionPutReceiptAck(final ServerSession session, final String body, final ParamContainer paramContainer, final MailServletInterface mailInterfaceArg) {
        /*
         * Some variables
         */
        final Response response = new Response();
        /*
         * Start response
         */
        try {
            final JSONObject bodyObj = new JSONObject(body);
            final String folderPath = bodyObj.has(PARAMETER_FOLDERID) ? bodyObj.getString(PARAMETER_FOLDERID) : null;
            if (null == folderPath) {
                throw new MailException(MailException.Code.MISSING_PARAM, PARAMETER_FOLDERID);
            }
            final String uid = bodyObj.has(PARAMETER_ID) ? bodyObj.getString(PARAMETER_ID) : null;
            if (null == uid) {
                throw new MailException(MailException.Code.MISSING_PARAM, PARAMETER_ID);
            }
            final String fromAddr =
                bodyObj.has(MailJSONField.FROM.getKey()) && !bodyObj.isNull(MailJSONField.FROM.getKey()) ? bodyObj.getString(MailJSONField.FROM.getKey()) : null;
            MailServletInterface mailInterface = mailInterfaceArg;
            boolean closeMailInterface = false;
            try {
                if (mailInterface == null) {
                    mailInterface = MailServletInterface.getInstance(session);
                    closeMailInterface = true;
                }
                mailInterface.sendReceiptAck(folderPath, uid, fromAddr);
            } finally {
                if (closeMailInterface && mailInterface != null) {
                    mailInterface.close(true);
                }
            }
        } catch (final MailException e) {
            LOG.error(e.getMessage(), e);
            response.setException(e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            response.setException(wrapper);
        }
        /*
         * Close response and flush print writer
         */
        response.setData(JSONObject.NULL);
        response.setTimestamp(null);
        return response;
    }

    private static String checkStringParam(final HttpServletRequest req, final String paramName) throws OXMandatoryFieldException {
        final String paramVal = req.getParameter(paramName);
        if (paramVal == null || paramVal.length() == 0 || STR_NULL.equals(paramVal)) {
            throw new OXMandatoryFieldException(
                EnumComponent.MAIL,
                MailException.Code.MISSING_PARAM.getCategory(),
                MailException.Code.MISSING_PARAM.getNumber(),
                null,
                paramName);
        }
        return paramVal;
    }

    private static String[] checkStringArrayParam(final HttpServletRequest req, final String paramName) throws AbstractOXException {
        final String tmp = req.getParameter(paramName);
        if (tmp == null || tmp.length() == 0 || STR_NULL.equals(tmp)) {
            throw new OXMandatoryFieldException(
                EnumComponent.MAIL,
                MailException.Code.MISSING_PARAM.getCategory(),
                MailException.Code.MISSING_PARAM.getNumber(),
                null,
                paramName);
        }
        return SPLIT.split(tmp, 0);
    }

    /*
     * (non-Javadoc)
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest , javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost(final HttpServletRequest req, final HttpServletResponse resp) throws ServletException, IOException {
        final ServerSession session = getSessionObject(req);
        /*
         * The magic spell to disable caching
         */
        Tools.disableCaching(resp);
        final String actionStr = req.getParameter(PARAMETER_ACTION);
        try {
            final MailServletInterface mailInterface = MailServletInterface.getInstance(session);
            try {
                /*
                 * Set response headers according to html spec
                 */
                resp.setContentType(MIME_TEXT_HTML_CHARSET_UTF_8);
                /*
                 * Append UploadListener instances
                 */
                final Collection<UploadListener> listeners = new ArrayList<UploadListener>(1);
                listeners.add(this);
                /*
                 * Create and fire upload event
                 */
                final UploadEvent uploadEvent = processUpload(req);
                uploadEvent.setParameter(UPLOAD_PARAM_MAILINTERFACE, mailInterface);
                uploadEvent.setParameter(UPLOAD_PARAM_WRITER, resp.getWriter());
                uploadEvent.setParameter(UPLOAD_PARAM_SESSION, session);
                uploadEvent.setParameter(UPLOAD_PARAM_HOSTNAME, req.getServerName());
                uploadEvent.setParameter(UPLOAD_PARAM_PROTOCOL, req.isSecure() ? "https" : "http");
                uploadEvent.setParameter(PARAMETER_ACTION, actionStr);
                fireUploadEvent(uploadEvent, listeners);
            } finally {
                if (mailInterface != null) {
                    try {
                        mailInterface.close(true);
                    } catch (final Exception e) {
                        LOG.error(e.getMessage(), e);
                    }
                }
            }
        } catch (final UploadException e) {
            LOG.error(e.getMessage(), e);
            JSONObject responseObj = null;
            try {
                final Response response = new Response();
                response.setException(e);
                responseObj = ResponseWriter.getJSON(response);
            } catch (final JSONException e1) {
                LOG.error(e1.getMessage(), e1);
            }
            throw new UploadServletException(resp, JS_FRAGMENT.replaceFirst(
                JS_FRAGMENT_JSON,
                responseObj == null ? STR_NULL : Matcher.quoteReplacement(responseObj.toString())).replaceFirst(
                JS_FRAGMENT_ACTION,
                e.getAction() == null ? STR_NULL : e.getAction()), e.getMessage(), e);
        } catch (final AbstractOXException e) {
            LOG.error(e.getMessage(), e);
            JSONObject responseObj = null;
            try {
                final Response response = new Response();
                response.setException(e);
                responseObj = ResponseWriter.getJSON(response);
            } catch (final JSONException e1) {
                LOG.error(e1.getMessage(), e1);
            }
            throw new UploadServletException(resp, JS_FRAGMENT.replaceFirst(
                JS_FRAGMENT_JSON,
                responseObj == null ? STR_NULL : Matcher.quoteReplacement(responseObj.toString())).replaceFirst(
                JS_FRAGMENT_ACTION,
                actionStr == null ? STR_NULL : actionStr), e.getMessage(), e);
        } catch (final Exception e) {
            final AbstractOXException wrapper = getWrappingOXException(e);
            LOG.error(wrapper.getMessage(), wrapper);
            JSONObject responseObj = null;
            try {
                final Response response = new Response();
                response.setException(wrapper);
                responseObj = ResponseWriter.getJSON(response);
            } catch (final JSONException e1) {
                LOG.error(e1.getMessage(), e1);
            }
            throw new UploadServletException(resp, JS_FRAGMENT.replaceFirst(
                JS_FRAGMENT_JSON,
                responseObj == null ? STR_NULL : Matcher.quoteReplacement(responseObj.toString())).replaceFirst(
                JS_FRAGMENT_ACTION,
                actionStr == null ? STR_NULL : actionStr), wrapper.getMessage(), wrapper);
        }
    }

    protected boolean sendMessage(final HttpServletRequest req) {
        return req.getParameter(PARAMETER_ACTION) != null && req.getParameter(PARAMETER_ACTION).equalsIgnoreCase(ACTION_SEND);
    }

    protected boolean appendMessage(final HttpServletRequest req) {
        return req.getParameter(PARAMETER_ACTION) != null && req.getParameter(PARAMETER_ACTION).equalsIgnoreCase(ACTION_APPEND);
    }

    public boolean action(final UploadEvent uploadEvent) throws OXException {
        if (uploadEvent.getAffiliationId() != UploadEvent.MAIL_UPLOAD) {
            return false;
        }
        try {
            final String protocol = (String) uploadEvent.getParameter(UPLOAD_PARAM_PROTOCOL);
            final String serverName = (String) uploadEvent.getParameter(UPLOAD_PARAM_HOSTNAME);
            final PrintWriter writer = (PrintWriter) uploadEvent.getParameter(UPLOAD_PARAM_WRITER);
            final String actionStr = (String) uploadEvent.getParameter(PARAMETER_ACTION);
            try {
                final MailServletInterface mailServletInterface =
                    (MailServletInterface) uploadEvent.getParameter(UPLOAD_PARAM_MAILINTERFACE);
                final String action = uploadEvent.getAction();
                if (ACTION_NEW.equals(action)) {
                    String msgIdentifier = null;
                    {
                        final JSONObject jsonMailObj;
                        {
                            final String json0 = uploadEvent.getFormField(UPLOAD_FORMFIELD_MAIL);
                            if (json0 == null || json0.trim().length() == 0) {
                                throw new MailException(MailException.Code.MISSING_PARAM, UPLOAD_FORMFIELD_MAIL);
                            }
                            jsonMailObj = new JSONObject(json0);
                        }
                        /*
                         * Parse
                         */
                        final ServerSession session = (ServerSession) uploadEvent.getParameter(UPLOAD_PARAM_SESSION);
                        /*
                         * Resolve "From" to proper mail account to select right transport server
                         */
                        final InternetAddress from;
                        try {
                            from = MessageParser.getFromField(jsonMailObj)[0];
                        } catch (final AddressException e) {
                            throw MIMEMailException.handleMessagingException(e);
                        }
                        int accountId;
                        try {
                            accountId = resolveFrom2Account(session, from, true, true);
                        } catch (final MailException e) {
                            if (MailException.Code.NO_TRANSPORT_SUPPORT.getNumber() != e.getDetailNumber()) {
                                // Re-throw
                                throw e;
                            }
                            LOG.warn(new StringBuilder(128).append(e.getMessage()).append(". Using default account's transport.").toString());
                            // Send with default account's transport provider
                            accountId = MailAccount.DEFAULT_ID;
                        }
                        if (jsonMailObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) && (jsonMailObj.getInt(MailJSONField.FLAGS.getKey()) & MailMessage.FLAG_DRAFT) > 0) {
                            /*
                             * ... and save draft
                             */
                            final ComposedMailMessage composedMail =
                                MessageParser.parse4Draft(jsonMailObj, uploadEvent, session, accountId);
                            msgIdentifier = mailServletInterface.saveDraft(composedMail, false, accountId);
                        } else {
                            /*
                             * ... and send message
                             */
                            final ComposedMailMessage[] composedMails =
                                MessageParser.parse4Transport(jsonMailObj, uploadEvent, session, accountId, protocol, serverName);
                            final ComposeType sendType =
                                jsonMailObj.hasAndNotNull(PARAMETER_SEND_TYPE) ? ComposeType.getType(jsonMailObj.getInt(PARAMETER_SEND_TYPE)) : ComposeType.NEW;
                            msgIdentifier = mailServletInterface.sendMessage(composedMails[0], sendType, accountId);
                            for (int i = 1; i < composedMails.length; i++) {
                                mailServletInterface.sendMessage(composedMails[i], sendType, accountId);
                            }
                            /*
                             * Trigger contact collector
                             */
                            try {
                                final ServerUserSetting setting = ServerUserSetting.getDefaultInstance();
                                final int contextId = session.getContextId();
                                final int userId = session.getUserId();
                                if (setting.isIContactCollectionEnabled(contextId, userId).booleanValue() && setting.isContactCollectOnMailTransport(
                                    contextId,
                                    userId).booleanValue()) {
                                    triggerContactCollector(session, composedMails[0]);
                                }
                            } catch (final SettingException e) {
                                LOG.warn("Contact collector could not be triggered.", e);
                            }
                        }
                    }
                    if (msgIdentifier == null) {
                        throw new MailException(MailException.Code.SEND_FAILED_UNKNOWN);
                    }
                    /*
                     * Create JSON response object
                     */
                    final Response response = new Response();
                    response.setData(msgIdentifier);
                    final String jsResponse =
                        JS_FRAGMENT.replaceFirst(JS_FRAGMENT_JSON, Matcher.quoteReplacement(ResponseWriter.getJSON(response).toString())).replaceFirst(
                            JS_FRAGMENT_ACTION,
                            actionStr);
                    writer.write(jsResponse);
                    writer.flush();
                    return true;
                } else if (ACTION_EDIT.equals(action)) {
                    /*
                     * Edit draft
                     */
                    String msgIdentifier = null;
                    {
                        final JSONObject jsonMailObj = new JSONObject(uploadEvent.getFormField(UPLOAD_FORMFIELD_MAIL));
                        final ServerSession session = (ServerSession) uploadEvent.getParameter(UPLOAD_PARAM_SESSION);
                        /*
                         * Resolve "From" to proper mail account
                         */
                        final InternetAddress from;
                        try {
                            from = MessageParser.getFromField(jsonMailObj)[0];
                        } catch (final AddressException e) {
                            throw MIMEMailException.handleMessagingException(e);
                        }
                        int accountId = resolveFrom2Account(session, from, false, true);
                        /*
                         * Check if detected account has drafts
                         */
                        final MailServletInterface msi = mailServletInterface;
                        if (msi.getDraftsFolder(accountId) == null) {
                            if (MailAccount.DEFAULT_ID == accountId) {
                                // Huh... No drafts folder in default account
                                throw new MailException(MailException.Code.FOLDER_NOT_FOUND, "Drafts");
                            }
                            LOG.warn(new StringBuilder(64).append("Mail account ").append(accountId).append(" for user ").append(
                                session.getUserId()).append(" in context ").append(session.getContextId()).append(
                                " has no drafts folder. Saving draft to default account's draft folder."));
                            // No drafts folder in detected mail account; auto-save to default account
                            accountId = MailAccount.DEFAULT_ID;
                        }
                        /*
                         * Parse with default account's transport provider
                         */
                        if (jsonMailObj.hasAndNotNull(MailJSONField.FLAGS.getKey()) && (jsonMailObj.getInt(MailJSONField.FLAGS.getKey()) & MailMessage.FLAG_DRAFT) > 0) {
                            final ComposedMailMessage composedMail =
                                MessageParser.parse4Draft(jsonMailObj, uploadEvent, session, MailAccount.DEFAULT_ID);
                            /*
                             * ... and edit draft
                             */
                            msgIdentifier = msi.saveDraft(composedMail, false, accountId);
                        } else {
                            throw new MailException(MailException.Code.UNEXPECTED_ERROR, "No new message on action=edit");
                        }
                    }
                    if (msgIdentifier == null) {
                        throw new MailException(MailException.Code.SEND_FAILED_UNKNOWN);
                    }
                    /*
                     * Create JSON response object
                     */
                    final Response response = new Response();
                    response.setData(msgIdentifier);
                    final String jsResponse =
                        JS_FRAGMENT.replaceFirst(JS_FRAGMENT_JSON, Matcher.quoteReplacement(ResponseWriter.getJSON(response).toString())).replaceFirst(
                            JS_FRAGMENT_ACTION,
                            actionStr);
                    writer.write(jsResponse);
                    writer.flush();
                    return true;
                } else if (ACTION_APPEND.equals(action)) {
                    // TODO: Editing mail
                    throw new UnsupportedOperationException("APPEND NOT SUPPORTED, YET!");
                }
            } catch (final MailException e) {
                /*
                 * Message could not be sent
                 */
                LOG.error(e.getMessage(), e);
                final Response response = new Response();
                response.setException(e);
                final String jsResponse =
                    JS_FRAGMENT.replaceFirst(JS_FRAGMENT_JSON, Matcher.quoteReplacement(ResponseWriter.getJSON(response).toString())).replaceFirst(
                        JS_FRAGMENT_ACTION,
                        actionStr);
                writer.write(jsResponse);
                writer.flush();
                return true;
            }
            return false;
        } catch (final JSONException e) {
            throw new OXException(new MailException(MailException.Code.JSON_ERROR, e, e.getMessage()));
        }
    }

    public UploadRegistry getRegistry() {
        return this;
    }

    @Override
    protected boolean hasModulePermission(final ServerSession session) {
        return session.getUserConfiguration().hasWebMail();
    }

    private static String getDefaultSendAddress(final ServerSession session) throws OXException {
        try {
            final MailAccountStorageService storageService =
                ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
            return storageService.getDefaultMailAccount(session.getUserId(), session.getContextId()).getPrimaryAddress();
        } catch (final MailAccountException e) {
            throw new OXException(e);
        } catch (final ServiceException e) {
            throw new OXException(e);
        }
    }

    private static int resolveFrom2Account(final ServerSession session, final InternetAddress from, final boolean checkTransportSupport, final boolean checkFrom) throws MailException, OXException {
        /*
         * Resolve "From" to proper mail account to select right transport server
         */
        int accountId;
        try {
            final MailAccountStorageService storageService =
                ServerServiceRegistry.getInstance().getService(MailAccountStorageService.class, true);
            final int user = session.getUserId();
            final int cid = session.getContextId();
            if (null == from) {
                accountId = MailAccount.DEFAULT_ID;
            } else {
                accountId = storageService.getByPrimaryAddress(from.getAddress(), user, cid);
                if (accountId != -1) {
                    accountId = storageService.getByPrimaryAddress(QuotedInternetAddress.toIDN(from.getAddress()), user, cid);
                }
            }
            if (accountId != -1) {
                if (!session.getUserConfiguration().isMultipleMailAccounts() && accountId != MailAccount.DEFAULT_ID) {
                    throw MailAccountExceptionMessages.NOT_ENABLED.create(Integer.valueOf(user), Integer.valueOf(cid));
                }
                if (checkTransportSupport) {
                    final MailAccount account = storageService.getMailAccount(accountId, user, cid);
                    // Check if determined account supports mail transport
                    if (null == account.getTransportServer()) {
                        // Account does not support mail transport
                        throw new MailException(MailException.Code.NO_TRANSPORT_SUPPORT, account.getName(), Integer.valueOf(accountId));
                    }
                }
            }
        } catch (final MailAccountException e) {
            throw new OXException(e);
        } catch (final ServiceException e) {
            throw new OXException(e);
        }
        if (accountId == -1) {
            if (checkFrom && null != from) {
                /*
                 * Check aliases
                 */
                try {
                    final Set<InternetAddress> validAddrs = new HashSet<InternetAddress>(4);
                    final User user = session.getUser();
                    final String[] aliases = user.getAliases();
                    for (final String alias : aliases) {
                        validAddrs.add(new QuotedInternetAddress(alias));
                    }
                    if (!validAddrs.contains(from)) {
                        throw new MailException(MailException.Code.INVALID_SENDER, from.toString());
                    }
                } catch (final AddressException e) {
                    throw MIMEMailException.handleMessagingException(e);
                }
            }
            accountId = MailAccount.DEFAULT_ID;
        }
        return accountId;
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final char[] chars = string.toCharArray();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < chars.length; i++) {
            isWhitespace = Character.isWhitespace(chars[i]);
        }
        return isWhitespace;
    }

    private static interface StringProvider {

        public String getString() throws IOException;

        public boolean isEmpty();
    }

    private static final class SimpleStringProvider implements StringProvider {

        private final String string;

        public SimpleStringProvider(final String string) {
            super();
            this.string = string;
        }

        public String getString() throws IOException {
            return string;
        }

        public boolean isEmpty() {
            return null == string || 0 == string.length();
        }

    }

    // getBody
    private static final class HTTPRequestStringProvider implements StringProvider {

        private final HttpServletRequest req;

        // private String string;

        public HTTPRequestStringProvider(final HttpServletRequest req) {
            super();
            this.req = req;
        }

        public String getString() throws IOException {
            return AJAXServlet.getBody(req);
        }

        public boolean isEmpty() {
            return false;
        }

    }

    private static boolean startsWith(final char startingChar, final String toCheck, final boolean ignoreHeadingWhitespaces) {
        if (null == toCheck) {
            return false;
        }
        final int len = toCheck.length();
        if (len <= 0) {
            return false;
        }
        if (!ignoreHeadingWhitespaces) {
            return startingChar == toCheck.charAt(0);
        }
        int i = 0;
        while (i < len && Character.isWhitespace(toCheck.charAt(i))) {
            i++;
        }
        if (i >= len) {
            return false;
        }
        return startingChar == toCheck.charAt(i);
    }

    private static JSONArray toJSONArray(final String toCheck) {
        if (!startsWith('[', toCheck, true)) {
            return null;
        }
        try {
            return new JSONArray(toCheck);
        } catch (final JSONException e) {
            return null;
        }
    }

    private static String getSimpleName(final String fullname) {
        if (null == fullname) {
            return null;
        }
        final int len = fullname.length();
        int pos = fullname.lastIndexOf('.');
        if (pos >= 0 && pos < len - 1) {
            return fullname.substring(pos + 1);
        }
        pos = fullname.lastIndexOf('/');
        if (pos >= 0 && pos < len - 1) {
            return fullname.substring(pos + 1);
        }
        return fullname;
    }

}
