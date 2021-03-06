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

package com.openexchange.login;

import static com.openexchange.ajax.AJAXServlet.localeFrom;
import static com.openexchange.ajax.requesthandler.AJAXRequestDataBuilder.request;
import static com.openexchange.login.LoginRampUpConfig.getConfig;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.openexchange.ajax.requesthandler.AJAXRequestData;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.ajax.requesthandler.Dispatcher;
import com.openexchange.ajax.requesthandler.Dispatchers;
import com.openexchange.ajax.tools.JSONCoercion;
import com.openexchange.ajax.writer.ResponseWriter;
import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptions;
import com.openexchange.java.Strings;
import com.openexchange.json.OXJSONWriter;
import com.openexchange.mail.MailExceptionCode;
import com.openexchange.osgi.ExceptionUtils;
import com.openexchange.server.ServiceLookup;
import com.openexchange.threadpool.AbstractTrackableTask;
import com.openexchange.threadpool.ThreadPoolService;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link DefaultAppSuiteLoginRampUp} - The default ramp-up implementation.
 * <p>
 * Additional/extended contributions may be added through overriding {@link #getExtendedContributions()} method.
 * <p>
 * <div style="background-color:#FFDDDD; padding:6px; margin:0px;">
 * <b>Note</b><br>
 * The passed {@link ServiceLookup} instance needs to track the following singleton services:
 * <ul>
 * <li>com.openexchange.threadpool.ThreadPoolService</li>
 * <li>com.openexchange.ajax.requesthandler.Dispatcher</li>
 * </ul>
 * </div>
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class DefaultAppSuiteLoginRampUp implements LoginRampUpService {

    private static final String ERROR_DURING_RAMP_UP = "Error during {} ramp-up";
    /** The logger constant */
    static final Logger LOG = org.slf4j.LoggerFactory.getLogger(DefaultAppSuiteLoginRampUp.class);

    /** The known ramp-up keys */
    public static enum RampUpKey {

        SERVER_CONFIG("serverConfig"),
        JSLOBS("jslobs"),
        OAUTH("oauth"),
        FOLDER("folder"),
        FOLDER_LIST("folderlist"),
        USER("user"),
        ACCOUNTS("accounts"),
        ERRORS("errors"),

        ;

        final String key;
        private RampUpKey(String key) {
            this.key = key;
        }
    }

    /** Specifies the verbosity for the <code>"jslobs"</code> ramp-up contribution */
    public static enum JSlobContributionVerbosity {
        /**
         * The <code>"jslobs"</code> ramp-up contribution only contains minimal information for <code>io.ox/core</code> (e.g. theme, language)
         */
        MINIMAL,
        /**
         * The (default) <code>"jslobs"</code> ramp-up contribution contains full information for <code>io.ox/core</code>,
         * <code>io.ox/files</code>, <code>io.ox/mail</code> and so on.
         */
        FULL,
        ;
    }

    public static interface Contribution {

        /**
         * Gets the key of this contribution.
         * <p>
         * Must not be one of {@link RampUpKey reserved keys}.
         *
         * @return The key
         */
        String getKey();

        /**
         * Gets the result object.
         *
         * @param session The session providing user information
         * @param loginRequest The login request
         * @return The result object
         * @throws OXException If result object cannot be returned
         */
        Object getResult(ServerSession session, AJAXRequestData loginRequest) throws OXException;
    }

    public static abstract class AbstractDispatcherContribution implements Contribution {

        private final Dispatcher ox;

        /**
         * Initializes a new {@link DefaultAppSuiteLoginRampUp.AbstractDispatcherContribution}.
         */
        protected AbstractDispatcherContribution(Dispatcher ox) {
            super();
            this.ox = ox;
        }

        @Override
        public Object getResult(ServerSession session, AJAXRequestData loginRequest) throws OXException {
            if (false == isApplicable(session, loginRequest, ox)) {
                return null;
            }

            AJAXRequestResult requestResult = null;
            Exception exc = null;
            try {
                AJAXRequestData requestData = request().session(session).module(getModule()).action(getAction()).params(getParams()).format("json").build(loginRequest);
                requestResult = ox.perform(requestData, null, session);
                return requestResult.getResultObject();
            } catch (OXException | RuntimeException x) {
                // Omit result on error. Let the UI deal with this
                exc = x;
                throw x;
            } finally {
                Dispatchers.signalDone(requestResult, exc);
            }
        }

        /**
         * Checks if this contribution is applicable.
         *
         * @param session The session
         * @param loginRequest The AJAX login request
         * @param ox The dispatcher service
         * @return <code>true</code> if applicable; otherwise <code>false</code>
         */
        protected abstract boolean isApplicable(ServerSession session, AJAXRequestData loginRequest, Dispatcher ox);

        /**
         * Gets the parameters to pass to dispatcher action
         *
         * @return The parameters
         */
        protected abstract String[] getParams();

        /**
         * Gets the action to call
         *
         * @return The action
         */
        protected abstract String getAction();

        /**
         * Gets the dispatcher module
         *
         * @return The module
         */
        protected abstract String getModule();

    }

    // ------------------------------------------------------------------------------------------------------------------------------------------------------ //

    /** The service look-up */
    protected final ServiceLookup services;

    private final EnumSet<RampUpKey> acceptedKeys;
    private final List<RampUpKey> keys;
    private final Set<String> reserved;

    /**
     * Initializes a new {@link DefaultAppSuiteLoginRampUp}.
     *
     * @param services The service look-up
     */
    protected DefaultAppSuiteLoginRampUp(ServiceLookup services) {
        this(RampUpKey.values(), services);
    }

    /**
     * Initializes a new {@link DefaultAppSuiteLoginRampUp}.
     *
     * @param acceptedKeys The optional accepted ramp-up keys. If <code>null</code> is specified, all keys are accepted.
     * @param services The service look-up
     */
    protected DefaultAppSuiteLoginRampUp(RampUpKey[] acceptedKeys, ServiceLookup services) {
        super();
        this.services = services;

        RampUpKey[] values = acceptedKeys;
        if (null == values) {
            // Accept all available keys
            values = RampUpKey.values();
        }
        ImmutableList.Builder<RampUpKey> keys = ImmutableList.builder();
        ImmutableSet.Builder<String> reserved = ImmutableSet.builder();
        for (RampUpKey rampUpKey : values) {
            if (RampUpKey.ERRORS != rampUpKey) {
                keys.add(rampUpKey);
            }
            reserved.add(Strings.asciiLowerCase(rampUpKey.key));
        }
        this.keys = keys.build();
        this.acceptedKeys = EnumSet.copyOf(this.keys);
        this.reserved = reserved.build();
    }

    /**
     * Gets the infix to use when looking up config settings.
     * <p>
     * Typically the client identifier to which this ramp-up applies.
     *
     * @return The infix or <code>null</code>
     */
    protected String getConfigInfix() {
        return null;
    }

    /**
     * Gets the desired verbosity for the <code>"jslobs"</code> ramp-up contribution.
     *
     * @return The JSlob verbosity
     */
    protected JSlobContributionVerbosity getJSlobVerbosity() {
        return JSlobContributionVerbosity.FULL;
    }

    /**
     * Checks whether the given ramp-up key is accepted for being delivered to the client.
     *
     * @param key The ramp-up key to check
     * @return <code>true</code> if accepted; otherwise <code>false</code> is the key should be discarded
     */
    private boolean accept(RampUpKey key) {
        return acceptedKeys.contains(key);
    }

    static void handleException(OXException e, String key, ConcurrentMap<String, OXException> errors) {
        if (OXExceptions.isCategory(Category.CATEGORY_PERMISSION_DENIED, e)) {
            LOG.debug("Permission error during {} ramp-up", key, e);
        } else if (OXExceptions.isCategory(Category.CATEGORY_USER_INPUT, e)) {
            LOG.debug(ERROR_DURING_RAMP_UP, key, e);
        } else {
            LOG.error(ERROR_DURING_RAMP_UP, key, e);
        }

        // Check for special mail error that standard folders cannot be created due to an "over quota" error
        if (MailExceptionCode.DEFAULT_FOLDER_CHECK_FAILED_OVER_QUOTA.equals(e)) {
            errors.putIfAbsent(e.getErrorCode(), e);
        }
    }

    static void handleException(Exception e, String key) {
        LOG.error(ERROR_DURING_RAMP_UP, key, e);
    }

    /**
     * Gets extended contributions
     *
     * @return The extended contributions
     */
    protected Collection<Contribution> getExtendedContributions() {
        return Collections.emptyList();
    }

    /**
     * Performs specified request.
     *
     * @param rampUpKey The ramp-up key for which the request is executed
     * @param info Optional additional information
     * @param requestData The request to perform
     * @param session The session providing user data
     * @param ox The dispatcher instance to use
     * @param thresholdMillis The execution time threshold in milliseconds since when to track executions
     * @return The result
     * @throws OXException If performing the request fails
     */
    protected AJAXRequestResult performDispatcherRequest(RampUpKey rampUpKey, String info, AJAXRequestData requestData, ServerSession session, Dispatcher ox, long thresholdMillis) throws OXException {
        if (thresholdMillis < 0) {
            return ox.perform(requestData, null, session);
        }

        // Track execution time
        long st = System.currentTimeMillis();
        AJAXRequestResult requestResult = ox.perform(requestData, null, session);
        long dur = System.currentTimeMillis() - st;
        if (dur >= thresholdMillis) {
            if (null == info) {
                LOG.debug("Ramp-up call \"{}\" took {}msec for session {}", rampUpKey.key, Long.valueOf(dur), session.getSessionID());
            } else {
                Object infoToLog = new StringBuilder(info.length() + 1).append(' ').append(info);
                LOG.debug("Ramp-up call \"{}\"{} took {}msec for session {}", rampUpKey.key, infoToLog, Long.valueOf(dur), session.getSessionID());
            }
        }
        return requestResult;
    }

    @Override
    public JSONObject getContribution(final ServerSession session, final AJAXRequestData loginRequest) throws OXException {
        int numberOfKeys = keys.size();

        ConcurrentMap<String, Future<Object>> rampUps = new ConcurrentHashMap<String, Future<Object>>(numberOfKeys);
        final ConcurrentMap<String, OXException> errors = new ConcurrentHashMap<String, OXException>(numberOfKeys);
        ThreadPoolService threads = services.getService(ThreadPoolService.class);

        final Dispatcher ox = services.getService(Dispatcher.class);
        final LoginRampUpConfig config = getConfig(getConfigInfix());
        final long thresholdMillis = LOG.isDebugEnabled() ? config.debugThresholdMillis : -1;

        if (accept(RampUpKey.FOLDER_LIST)) {
            rampUps.put(RampUpKey.FOLDER_LIST.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.folderlistDisabled) {
                        return null;
                    }

                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        JSONObject folderlist = new JSONObject(2);
                        AJAXRequestData requestData = request().session(session).module("folders").action("list").params("parent", "1", "tree", "0", "altNames", "true", "timezone", "UTC", "columns", "1,2,3,4,5,6,20,23,51,52,300,301,302,304,305,306,307,308,309,310,311,312,313,314,315,316,317,318,319,321,3010,3020,3030,3050,3201,3203,3204,3205,3220").format("json").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.FOLDER_LIST, null, requestData, session, ox, thresholdMillis);
                        folderlist.put("1", requestResult.getResultObject());
                        return folderlist;
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.FOLDER_LIST.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.FOLDER_LIST.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }
                    return null;
                }
            }));
        }

        if (accept(RampUpKey.FOLDER)) {
            rampUps.put(RampUpKey.FOLDER.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.folderDisabled) {
                        return null;
                    }
                    if (null == session.getUserPermissionBits() || false == session.getUserPermissionBits().hasWebMail()) {
                        return null;
                    }
                    JSONObject folder = new JSONObject(3);
                    {
                        AJAXRequestResult requestResult = null;
                        Exception exc = null;
                        try {
                            AJAXRequestData requestData = request().session(session).module("folders").action("get").params("id", "1", "tree", "1", "altNames", "true", "timezone", "UTC").format("json").build(loginRequest);
                            requestResult = performDispatcherRequest(RampUpKey.FOLDER, "\"private-folder\"", requestData, session, ox, thresholdMillis);
                            folder.put("1", requestResult.getResultObject());
                        } catch (OXException x) {
                            // Omit result on error. Let the UI deal with this
                            exc = x;
                            handleException(x, RampUpKey.FOLDER.key, errors);
                        } catch (RuntimeException x) {
                            // Omit result on error. Let the UI deal with this
                            exc = x;
                            handleException(x, RampUpKey.FOLDER.key);
                        } finally {
                            Dispatchers.signalDone(requestResult, exc);
                        }
                    }
                    {
                        AJAXRequestResult requestResult = null;
                        Exception exc = null;
                        try {
                            AJAXRequestData requestData = request().session(session).module("folders").action("get").params("id", "default0/INBOX", "tree", "1", "altNames", "true", "timezone", "UTC").format("json").build(loginRequest);
                            requestResult = performDispatcherRequest(RampUpKey.FOLDER, "\"INBOX\"", requestData, session, ox, thresholdMillis);
                            folder.put("default0/INBOX", requestResult.getResultObject());
                        } catch (OXException x) {
                            // Omit result on error. Let the UI deal with this
                            exc = x;
                            handleException(x, RampUpKey.FOLDER.key, errors);
                        } catch (RuntimeException x) {
                            // Omit result on error. Let the UI deal with this
                            exc = x;
                            handleException(x, RampUpKey.FOLDER.key);
                        } finally {
                            Dispatchers.signalDone(requestResult, exc);
                        }
                    }
                    return folder;
                }
            }));
        }

        if (accept(RampUpKey.JSLOBS)) {
            final JSlobContributionVerbosity jslobVerbosity = getJSlobVerbosity();
            rampUps.put(RampUpKey.JSLOBS.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.jslobsDisabled) {
                        return null;
                    }

                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        JSONObject jslobs = new JSONObject();
                        AJAXRequestData requestData;
                        switch (jslobVerbosity) {
                            case MINIMAL:
                                requestData = request().session(session).module("jslob").action("list").data(new JSONArray(Arrays.asList("io.ox/core")), "json").format("json").build(loginRequest);
                                break;
                            case FULL:
                                /* fall-through */
                            default:
                                requestData = request().session(session).module("jslob").action("list").data(new JSONArray(Arrays.asList("io.ox/core", "io.ox/core/updates", "io.ox/mail", "io.ox/contacts", "io.ox/calendar", "io.ox/caldav", "io.ox/files", "io.ox/tours", "io.ox/mail/emoji", "io.ox/tasks", "io.ox/office")), "json").format("json").build(loginRequest);
                                break;
                        }
                        requestResult = performDispatcherRequest(RampUpKey.JSLOBS, null, requestData, session, ox, thresholdMillis);
                        JSONArray lobs = (JSONArray) requestResult.getResultObject();
                        if (null != lobs) {
                            for (int i = 0, size = lobs.length(); i < size; i++) {
                                JSONObject lob = lobs.getJSONObject(i);
                                jslobs.put(lob.getString("id"), lob);
                            }
                        }
                        return jslobs;
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.JSLOBS.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.JSLOBS.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }
                    return null;
                }
            }));
        }

        if (accept(RampUpKey.SERVER_CONFIG)) {
            rampUps.put(RampUpKey.SERVER_CONFIG.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.serverConfigDisabled) {
                        return null;
                    }

                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("apps/manifests").action("config").format("json").hostname(loginRequest.getHostname()).build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.SERVER_CONFIG, null, requestData, session, ox, thresholdMillis);
                        return requestResult.getResultObject();
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.SERVER_CONFIG.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.SERVER_CONFIG.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }
                    return null;
                }
            }));
        }

        if (accept(RampUpKey.OAUTH)) {
            rampUps.put(RampUpKey.OAUTH.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.oauthDisabled) {
                        return null;
                    }
                    if (session.isAnonymous() || session.getUser().isGuest()) {
                        return null;
                    }

                    JSONObject oauth = new JSONObject(3);
                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("oauth/services").action("all").format("json").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.OAUTH, "\"available-services\"", requestData, session, ox, thresholdMillis);
                        oauth.put("services", requestResult.getResultObject());
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }

                    requestResult = null;
                    exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("oauth/accounts").action("all").format("json").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.OAUTH, "\"available-accounts\"", requestData, session, ox, thresholdMillis);
                        oauth.put("accounts", requestResult.getResultObject());
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }

                    requestResult = null;
                    exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("recovery/secret").action("check").format("json").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.OAUTH, "\"password-check\"", requestData, session, ox, thresholdMillis);
                        oauth.put("secretCheck", requestResult.getResultObject());
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.OAUTH.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }

                    return oauth;
                }
            }));
        }

        if (accept(RampUpKey.USER)) {
            rampUps.put(RampUpKey.USER.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.userDisabled) {
                        return null;
                    }

                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("user").action("get").params("timezone", "utc", "id", Integer.toString(session.getUserId())).format("json").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.USER, null, requestData, session, ox, thresholdMillis);
                        return requestResult.getResultObject();
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.USER.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.USER.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }
                    return null;
                }
            }));
        }

        if (accept(RampUpKey.ACCOUNTS)) {
            rampUps.put(RampUpKey.ACCOUNTS.key, threads.submit(new AbstractTrackableTask<Object>() {

                @Override
                public Object call() throws Exception {
                    if (config.accountsDisabled) {
                        return null;
                    }

                    AJAXRequestResult requestResult = null;
                    Exception exc = null;
                    try {
                        AJAXRequestData requestData = request().session(session).module("account").action("all").format("json").params("columns", "all").build(loginRequest);
                        requestResult = performDispatcherRequest(RampUpKey.ACCOUNTS, null, requestData, session, ox, thresholdMillis);
                        return requestResult.getResultObject();
                    } catch (OXException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.ACCOUNTS.key, errors);
                    } catch (RuntimeException x) {
                        // Omit result on error. Let the UI deal with this
                        exc = x;
                        handleException(x, RampUpKey.ACCOUNTS.key);
                    } finally {
                        Dispatchers.signalDone(requestResult, exc);
                    }
                    return null;
                }

            }));
        }

        // Check for extended contributions
        List<String> extendedKeys = null;
        {
            Collection<Contribution> contributions = getExtendedContributions();
            if (null != contributions && false == contributions.isEmpty()) {
                extendedKeys = new ArrayList<>(contributions.size());
                for (final Contribution contribution : contributions) {
                    if (reserved.contains(Strings.asciiLowerCase(contribution.getKey()))) {
                        // Illegal key... Ignore
                    } else {
                        rampUps.put(contribution.getKey(), threads.submit(new AbstractTrackableTask<Object>() {

                            @Override
                            public Object call() throws Exception {
                                try {
                                    return contribution.getResult(session, loginRequest);
                                } catch (OXException x) {
                                    // Omit result on error. Let the UI deal with this
                                    handleException(x, contribution.getKey(), errors);
                                } catch (RuntimeException x) {
                                    // Omit result on error. Let the UI deal with this
                                    handleException(x, contribution.getKey());
                                }
                                return null;
                            }
                        }));
                        extendedKeys.add(contribution.getKey());
                    }
                }
            }
        }

        try {
            // Grab tasks to complete in preserved order
            List<RampUpFuture> taskCompletions = new LinkedList<>();
            for (RampUpKey rampUpKey : keys) {
                Future<Object> taskCompletion = rampUps.get(rampUpKey.key);
                if (null != taskCompletion) {
                    taskCompletions.add(new RampUpFuture(rampUpKey, taskCompletion));
                }
            }

            if (null != extendedKeys) {
                for (String extendedKey : extendedKeys) {
                    Future<Object> taskCompletion = rampUps.get(extendedKey);
                    if (null != taskCompletion) {
                        taskCompletions.add(new RampUpFuture(extendedKey, taskCompletion));
                    }
                }
            }

            // Let tasks contribute to JSON object
            JSONObject jo = new JSONObject(numberOfKeys);
            Iterator<RampUpFuture> tasksToWaitFor = taskCompletions.iterator();
            RampUpFuture last = null;
            try {
                while (tasksToWaitFor.hasNext()) {
                    RampUpFuture rampUpTask = tasksToWaitFor.next();
                    last = rampUpTask;
                    Object value = rampUpTask.get();
                    jo.put(rampUpTask.getKey(), JSONCoercion.coerceToJSON(value));
                }
            } catch (InterruptedException e) {
                // Ramp-up interrupted... Cancel remaining tasks
                if (null != last) {
                    last.cancel(true);
                }
                while (tasksToWaitFor.hasNext()) {
                    RampUpFuture rampUpTask = tasksToWaitFor.next();
                    rampUpTask.cancel(true);
                }

                // Keep interrupted status
                Thread.currentThread().interrupt();
                LOG.debug("Ramp-up has been intentionally interrupted", e);
            }

            // Add errors (if any)
            int numErrors = errors.size();
            if (numErrors > 0) {
                Locale locale = localeFrom(session);
                JSONObject jErrors = new JSONObject(numErrors);
                for (Map.Entry<String, OXException> errorEntry : errors.entrySet()) {
                    JSONObject jError = new JSONObject(8);
                    ResponseWriter.writeException(errorEntry.getValue(), new OXJSONWriter(jError), locale);
                    jErrors.put(errorEntry.getKey(), jError);
                }
                jo.put(RampUpKey.ERRORS.key, jErrors);
            }
            return jo;
        } catch (Throwable t) {
            ExceptionUtils.handleThrowable(t);
            LOG.warn("Failed ramp-up", t);
            return new JSONObject();
        }
    }

    private static final class RampUpFuture {

        private final String key;
        private final Future<Object> rampUpTask;

        RampUpFuture(RampUpKey rampUpKey, Future<Object> rampUpTask) {
            super();
            this.key = rampUpKey.key;
            this.rampUpTask = rampUpTask;
        }

        RampUpFuture(String key, Future<Object> rampUpTask) {
            super();
            this.key = key;
            this.rampUpTask = rampUpTask;
        }

        String getKey() {
            return key;
        }

        boolean cancel(boolean mayInterruptIfRunning) {
            return rampUpTask.cancel(mayInterruptIfRunning);
        }

        Object get() throws InterruptedException, ExecutionException {
            return rampUpTask.get();
        }
    }

}
