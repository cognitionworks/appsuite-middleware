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

package com.openexchange.mailfilter.ajax.actions;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.jsieve.SieveException;
import org.apache.jsieve.TagArgument;
import org.apache.jsieve.parser.generated.ParseException;
import org.apache.jsieve.parser.generated.TokenMgrError;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.jsieve.commands.ActionCommand;
import com.openexchange.jsieve.commands.IfCommand;
import com.openexchange.jsieve.commands.Rule;
import com.openexchange.jsieve.commands.RuleComment;
import com.openexchange.jsieve.commands.TestCommand;
import com.openexchange.jsieve.commands.TestCommand.Commands;
import com.openexchange.jsieve.export.Capabilities;
import com.openexchange.jsieve.export.SieveHandler;
import com.openexchange.jsieve.export.SieveTextFilter;
import com.openexchange.jsieve.export.SieveTextFilter.ClientRulesAndRequire;
import com.openexchange.jsieve.export.SieveTextFilter.RuleListAndNextUid;
import com.openexchange.jsieve.export.exceptions.OXSieveHandlerException;
import com.openexchange.jsieve.export.exceptions.OXSieveHandlerInvalidCredentialsException;
import com.openexchange.mailfilter.ajax.Credentials;
import com.openexchange.mailfilter.ajax.Parameter;
import com.openexchange.mailfilter.ajax.actions.AbstractRequest.Parameters;
import com.openexchange.mailfilter.ajax.exceptions.OXMailfilterExceptionCode;
import com.openexchange.mailfilter.ajax.json.AbstractObject2JSON2Object;
import com.openexchange.mailfilter.ajax.json.Rule2JSON2Rule;
import com.openexchange.mailfilter.internal.MailFilterProperties;
import com.openexchange.mailfilter.services.MailFilterServletServiceRegistry;
import com.openexchange.session.Session;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;
import com.openexchange.tools.servlet.OXJSONExceptionCodes;

/**
 *
 * @author <a href="mailto:dennis.sieben@open-xchange.org">Dennis Sieben</a>
 */
public class MailfilterAction extends AbstractAction<Rule, MailfilterRequest> {

    private static final Log log = com.openexchange.log.Log.valueOf(LogFactory.getLog(MailfilterAction.class));

    private static final ConcurrentMap<Key, MailfilterAction> INSTANCES = new ConcurrentHashMap<Key, MailfilterAction>();

    /**
     * Gets the {@link MailfilterAction} instance for specified session.
     * 
     * @param session The session
     * @return The appropriate {@link MailfilterAction} instance
     */
    public static MailfilterAction valueFor(final Session session) {
        final Key key = new Key(session.getUserId(), session.getContextId());
        MailfilterAction action = INSTANCES.get(key);
        if (null == action) {
            final MailfilterAction newaction = new MailfilterAction();
            action = INSTANCES.putIfAbsent(key, newaction);
            if (null == action) {
                action = newaction;
            }
        }
        return action;
    }

    /**
     * Removes the {@link MailfilterAction} instance associated with specified session.
     * 
     * @param session The session
     */
    public static void removeFor(final Session session) {
        INSTANCES.remove(new Key(session.getUserId(), session.getContextId()));
    }

    private static final Object[] EMPTY_ARGS = new Object[0];

    private static final class RuleAndPosition {
        private final int position;

        private final Rule rule;

        /**
         * @param rule
         * @param position
         */
        public RuleAndPosition(final Rule rule, final int position) {
            super();
            this.rule = rule;
            this.position = position;
        }

        /**
         * @return the position
         */
        public final int getPosition() {
            return position;
        }

        /**
         * @return the rule
         */
        public final Rule getRule() {
            return rule;
        }

    }

    private static final AbstractObject2JSON2Object<Rule> CONVERTER = new Rule2JSON2Rule();

    private final Object mutex;

    private final String scriptname;

    /**
     * Default constructor.
     */
    public MailfilterAction() {
        super();
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(
                ConfigurationService.class);
        scriptname = config.getProperty(MailFilterProperties.Values.SCRIPT_NAME.property);
        mutex = new Object();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONObject actionConfig(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveHandler sieveHandler = connectRight(credentials);
            // First fetch configuration:
            JSONObject tests = null;
            try {
                sieveHandler.initializeConnection();
                final Capabilities capabilities = sieveHandler.getCapabilities();
                final ArrayList<String> sieve = capabilities.getSieve();
                tests = getTestAndActionObjects(sieve);
            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final JSONException e) {
                throw OXMailfilterExceptionCode.JSON_ERROR.create(e, e.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
            return tests;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void actionDelete(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveTextFilter sieveTextFilter = new SieveTextFilter(credentials);
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = sieveHandler.getActiveScript();
                final String script = sieveHandler.getScript(activeScript);
                final RuleListAndNextUid rulesandid = sieveTextFilter.readScriptFromString(script);
                final ClientRulesAndRequire clientrulesandrequire =
                    sieveTextFilter.splitClientRulesAndRequire(rulesandid.getRulelist(), null, rulesandid.isError());
                final String body = request.getBody();
                final JSONObject json = new JSONObject(body);

                final ArrayList<Rule> rules = clientrulesandrequire.getRules();
                final RuleAndPosition deletedrule =
                    getRightRuleForUniqueId(rules, getUniqueId(json), credentials.getRightUsername(), credentials.getContextString());
                rules.remove(deletedrule.getPosition());
                final String writeback = sieveTextFilter.writeback(clientrulesandrequire);
                writeScript(sieveHandler, activeScript, writeback);
            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final ParseException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final SieveException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e, EMPTY_ARGS);
            } catch (final TokenMgrError error) {
                throw OXMailfilterExceptionCode.LEXICAL_ERROR.create(error, error.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected JSONArray actionList(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Parameters parameters = request.getParameters();
            final Credentials credentials = request.getCredentials();
            final SieveTextFilter sieveTextFilter = new SieveTextFilter(credentials);
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String script = sieveHandler.getScript(sieveHandler.getActiveScript());
                if (log.isDebugEnabled()) {
                    log.debug("The following sieve script will be parsed:\n" + script);
                }
                final RuleListAndNextUid readScriptFromString = sieveTextFilter.readScriptFromString(script);
                final ClientRulesAndRequire clientrulesandrequire =
                    sieveTextFilter.splitClientRulesAndRequire(
                        readScriptFromString.getRulelist(),
                        parameters.getParameter(Parameter.FLAG),
                        readScriptFromString.isError());
                final ArrayList<Rule> clientrules = clientrulesandrequire.getRules();

                changeOutgoingVacationRule(clientrules);

                return CONVERTER.write(clientrules.toArray(new Rule[clientrules.size()]));
            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final ParseException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final SieveException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e, EMPTY_ARGS);
            } catch (final TokenMgrError error) {
                throw OXMailfilterExceptionCode.LEXICAL_ERROR.create(error, error.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected int actionNew(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveTextFilter sieveTextFilter = new SieveTextFilter(credentials);
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = sieveHandler.getActiveScript();
                final String script = sieveHandler.getScript(activeScript);
                final RuleListAndNextUid rules = sieveTextFilter.readScriptFromString(script);

                final ClientRulesAndRequire clientrulesandrequire =
                    sieveTextFilter.splitClientRulesAndRequire(rules.getRulelist(), null, rules.isError());

                final String body = request.getBody();
                final JSONObject json = new JSONObject(body);
                final Rule newrule = CONVERTER.parse(json);

                changeIncomingVacationRule(newrule);

                // Now find the right position inside the array
                int position = newrule.getPosition();
                final ArrayList<Rule> clientrules = clientrulesandrequire.getRules();
                if (position >= clientrules.size()) {
                    throw OXMailfilterExceptionCode.POSITION_TOO_BIG.create();
                }
                final int nextuid = rules.getNextuid();
                setUidInRule(newrule, nextuid);
                if (-1 != position) {
                    clientrules.add(position, newrule);
                } else {
                    clientrules.add(newrule);
                    position = clientrules.size() - 1;
                }
                final String writeback = sieveTextFilter.writeback(clientrulesandrequire);
                if (log.isDebugEnabled()) {
                    log.debug("The following sieve script will be written:\n" + writeback);
                }
                writeScript(sieveHandler, activeScript, writeback);

                return nextuid;
            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final ParseException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final SieveException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_READ_ERROR.create(e, e.getMessage());
            } catch (final TokenMgrError error) {
                throw OXMailfilterExceptionCode.LEXICAL_ERROR.create(error, error.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    @Override
    protected void actionReorder(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveTextFilter sieveTextFilter = new SieveTextFilter(credentials);
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = sieveHandler.getActiveScript();
                final String script = sieveHandler.getScript(activeScript);
                final RuleListAndNextUid rules = sieveTextFilter.readScriptFromString(script);

                final ClientRulesAndRequire clientrulesandrequire =
                    sieveTextFilter.splitClientRulesAndRequire(rules.getRulelist(), null, rules.isError());

                final String body = request.getBody();
                final JSONArray json = new JSONArray(body);

                final ArrayList<Rule> clientrules = clientrulesandrequire.getRules();
                for (int i = 0; i < json.length(); i++) {
                    final int uniqueid = json.getInt(i);
                    final RuleAndPosition rightRule =
                        getRightRuleForUniqueId(
                            clientrules,
                            Integer.valueOf(uniqueid),
                            credentials.getRightUsername(),
                            credentials.getContextString());
                    final int position = rightRule.getPosition();
                    clientrules.remove(position);
                    clientrules.add(i, rightRule.getRule());
                }

                final String writeback = sieveTextFilter.writeback(clientrulesandrequire);
                writeScript(sieveHandler, activeScript, writeback);

            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final ParseException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final SieveException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e, EMPTY_ARGS);
            } catch (final TokenMgrError error) {
                throw OXMailfilterExceptionCode.LEXICAL_ERROR.create(error, error.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void actionUpdate(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveTextFilter sieveTextFilter = new SieveTextFilter(credentials);
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = fixParsingError(sieveHandler.getActiveScript());
                final String script = fixParsingError(sieveHandler.getScript(activeScript));
                final RuleListAndNextUid rules = sieveTextFilter.readScriptFromString(script);

                final ClientRulesAndRequire clientrulesandrequire =
                    sieveTextFilter.splitClientRulesAndRequire(rules.getRulelist(), null, rules.isError());

                final String body = request.getBody();
                final JSONObject json = new JSONObject(body);
                final Integer uniqueid = getUniqueId(json);

                final ArrayList<Rule> clientrules = clientrulesandrequire.getRules();
                if (null != uniqueid) {
                    // First get the right rule which should be modified...
                    final RuleAndPosition rightRule =
                        getRightRuleForUniqueId(clientrules, uniqueid, credentials.getRightUsername(), credentials.getContextString());
                    CONVERTER.parse(rightRule.getRule(), json);
                    changeIncomingVacationRule(rightRule.getRule());
                } else {
                    throw OXMailfilterExceptionCode.ID_MISSING.create();
                }

                final String writeback = sieveTextFilter.writeback(clientrulesandrequire);
                if (log.isDebugEnabled()) {
                    log.debug("The following sieve script will be written:\n" + writeback);
                }
                writeScript(sieveHandler, activeScript, writeback);

            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                handleParsingException(e, credentials);
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final ParseException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final SieveException e) {
                throw OXMailfilterExceptionCode.SIEVE_ERROR.create(e, e.getMessage());
            } catch (final JSONException e) {
                throw OXJSONExceptionCodes.JSON_BUILD_ERROR.create(e, EMPTY_ARGS);
            } catch (final TokenMgrError error) {
                throw OXMailfilterExceptionCode.LEXICAL_ERROR.create(error, error.getMessage());
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    @Override
    protected void actionDeleteScript(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = sieveHandler.getActiveScript();

                writeScript(sieveHandler, activeScript, "");

            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    @Override
    protected String actionGetScript(final MailfilterRequest request) throws OXException {
        synchronized (mutex) {
            final Credentials credentials = request.getCredentials();
            final SieveHandler sieveHandler = connectRight(credentials);
            try {
                sieveHandler.initializeConnection();
                final String activeScript = sieveHandler.getActiveScript();
                return sieveHandler.getScript(activeScript);
            } catch (final UnsupportedEncodingException e) {
                throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
            } catch (final IOException e) {
                throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, sieveHandler.getSieveHost(), Integer.valueOf(sieveHandler.getSievePort()));
            } catch (final OXSieveHandlerException e) {
                throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(
                    e,
                    e.getSieveHost(),
                    Integer.valueOf(e.getSieveHostPort()),
                    credentials.getRightUsername(),
                    credentials.getContextString());
            } catch (final OXSieveHandlerInvalidCredentialsException e) {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(e, EMPTY_ARGS);
            } catch (final NumberFormatException nfe) {
                throw OXMailfilterExceptionCode.NAN.create(nfe, getNANString(nfe));
            } catch (final RuntimeException re) {
                throw OXMailfilterExceptionCode.PROBLEM.create(re, re.getMessage());
            } finally {
                if (null != sieveHandler) {
                    try {
                        sieveHandler.close();
                    } catch (final UnsupportedEncodingException e) {
                        throw OXMailfilterExceptionCode.UNSUPPORTED_ENCODING.create(e, EMPTY_ARGS);
                    } catch (final IOException e) {
                        throw OXMailfilterExceptionCode.IO_CONNECTION_ERROR.create(e, EMPTY_ARGS);
                    }
                }
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Rule2JSON2Rule getConverter() {
        return new Rule2JSON2Rule();
    }

    // protected so that we can test this
    protected String getRightPassword(final ConfigurationService config, final Credentials creds) throws OXException {
        final String passwordsrc = config.getProperty(MailFilterProperties.Values.SIEVE_PASSWORDSRC.property);
        if (MailFilterProperties.PasswordSource.SESSION.name.equals(passwordsrc)) {
            return creds.getPassword();
        } else if (MailFilterProperties.PasswordSource.GLOBAL.name.equals(passwordsrc)) {
            final String masterpassword = config.getProperty(MailFilterProperties.Values.SIEVE_MASTERPASSWORD.property);
            if (null == masterpassword || masterpassword.length() == 0) {
                throw OXMailfilterExceptionCode.NO_MASTERPASSWORD_SET.create();
            }
            return masterpassword;
        } else {
            throw OXMailfilterExceptionCode.NO_VALID_PASSWORDSOURCE.create();
        }
    }

    private SieveHandler connectRight(final Credentials creds) throws OXException {
        final SieveHandler sieveHandler;
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(
                ConfigurationService.class);

        final String logintype = config.getProperty(MailFilterProperties.Values.SIEVE_LOGIN_TYPE.property);
        final int sieve_port;
        final String sieve_server;
        User storageUser = null;
        if (MailFilterProperties.LoginTypes.GLOBAL.name.equals(logintype)) {
            sieve_server = config.getProperty(MailFilterProperties.Values.SIEVE_SERVER.property);
            if (null == sieve_server) {
                throw OXMailfilterExceptionCode.PROPERTY_ERROR.create(MailFilterProperties.Values.SIEVE_SERVER.property);
            }
            try {
                sieve_port = Integer.parseInt(config.getProperty(MailFilterProperties.Values.SIEVE_PORT.property));
            } catch (final RuntimeException e) {
                throw OXMailfilterExceptionCode.PROPERTY_ERROR.create(e, MailFilterProperties.Values.SIEVE_PORT.property);
            }
        } else if (MailFilterProperties.LoginTypes.USER.name.equals(logintype)) {
            storageUser = UserStorage.getStorageUser(creds.getUserid(), creds.getContextid());
            if (null != storageUser) {
                final String mailServerURL = storageUser.getImapServer();
                final URI uri;
                try {
                    uri = URIParser.parse(mailServerURL, URIDefaults.IMAP);
                } catch (final URISyntaxException e) {
                    throw OXMailfilterExceptionCode.NO_SERVERNAME_IN_SERVERURL.create(e, mailServerURL);
                }
                sieve_server = uri.getHost();
                try {
                    sieve_port = Integer.parseInt(config.getProperty(MailFilterProperties.Values.SIEVE_PORT.property));
                } catch (final RuntimeException e) {
                    throw OXMailfilterExceptionCode.PROPERTY_ERROR.create(e,
                            MailFilterProperties.Values.SIEVE_PORT.property);
                }
            } else {
                throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create("Could not get a valid user object for uid "
                        + creds.getUserid() + " and contextid " + creds.getContextid());
            }
        } else {
            throw OXMailfilterExceptionCode.NO_VALID_LOGIN_TYPE.create();
        }
        /*
         * Get SIEVE_AUTH_ENC property
         */
        final String authEnc = config.getProperty(MailFilterProperties.Values.SIEVE_AUTH_ENC.property, MailFilterProperties.Values.SIEVE_AUTH_ENC.def);
        /*
         * Establish SieveHandler
         */
        final String credsrc = config.getProperty(MailFilterProperties.Values.SIEVE_CREDSRC.property);
        if (MailFilterProperties.CredSrc.SESSION.name.equals(credsrc) || MailFilterProperties.CredSrc.SESSION_FULL_LOGIN.name.equals(credsrc)) {
            final String username = creds.getUsername();
            final String authname = creds.getAuthname();
            final String password = getRightPassword(config, creds);
            if (null != username) {
                sieveHandler = new SieveHandler(username, authname, password, sieve_server, sieve_port, authEnc);
            } else {
                sieveHandler = new SieveHandler(authname, password, sieve_server, sieve_port, authEnc);
            }
        } else if (MailFilterProperties.CredSrc.IMAP_LOGIN.name.equals(credsrc)) {
            final String authname;
            if (null != storageUser) {
                authname = storageUser.getImapLogin();
            } else {
                storageUser = UserStorage.getStorageUser(creds.getUserid(), creds.getContextid());
                if (null != storageUser) {
                    authname = storageUser.getImapLogin();
                } else {
                    throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create(
                            "Could not get a valid user object for uid " + creds.getUserid() + " and contextid "
                                    + creds.getContextid());
                }
            }
            final String username = creds.getUsername();
            final String password = getRightPassword(config, creds);
            if (null != username) {
                sieveHandler = new SieveHandler(username, authname, password, sieve_server, sieve_port, authEnc);
            } else {
                sieveHandler = new SieveHandler(authname, password, sieve_server, sieve_port, authEnc);
            }
            } else if (MailFilterProperties.CredSrc.MAIL.name.equals(credsrc)) {
                final String authname;
                if (null != storageUser) {
                    authname = storageUser.getMail();
                } else {
                    storageUser = UserStorage.getStorageUser(creds.getUserid(), creds.getContextid());
                    if (null != storageUser) {
                        authname = storageUser.getMail();
                    } else {
                        throw OXMailfilterExceptionCode.INVALID_CREDENTIALS.create("Could not get a valid user object for uid " + creds.getUserid() + " and contextid " + creds.getContextid());
                    }
                }
                final String username = creds.getUsername();
                final String password = getRightPassword(config, creds);
                if (null != username) {
                    sieveHandler = new SieveHandler(username, authname, password, sieve_server, sieve_port, authEnc);
                } else {
                    sieveHandler = new SieveHandler(authname, password, sieve_server, sieve_port, authEnc);
                }
        } else {
            throw OXMailfilterExceptionCode.NO_VALID_CREDSRC.create();
        }
        return sieveHandler;
    }

    private void changeIncomingVacationRule(final Rule newrule) throws SieveException {
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String vacationdomains = config.getProperty(MailFilterProperties.Values.VACATION_DOMAINS.property);

        if (null != vacationdomains && 0 != vacationdomains.length()) {
            final IfCommand ifCommand = newrule.getIfCommand();
            final RuleComment ruleComment = newrule.getRuleComment();
            if (null != ruleComment && null != ruleComment.getFlags() && ruleComment.getFlags().contains("vacation") && ActionCommand.Commands.VACATION.equals(ifCommand.getActioncommands().get(0).getCommand())) {
                final List<Object> argList = new ArrayList<Object>();
                argList.add(Rule2JSON2Rule.createTagArg("is"));
                argList.add(Rule2JSON2Rule.createTagArg("domain"));

                final ArrayList<String> header = new ArrayList<String>();
                header.add("From");

                final String[] split = vacationdomains.split(",");

                argList.add(header);
                argList.add(Arrays.asList(split));
                final TestCommand testcommand = ifCommand.getTestcommand();
                final Commands command = testcommand.getCommand();
                final TestCommand newTestCommand = new TestCommand(Commands.ADDRESS, argList, new ArrayList<TestCommand>());
                if (Commands.TRUE.equals(command)) {
                    // No test until now
                    ifCommand.setTestcommand(newTestCommand);
                } else {
                    // Found other tests
                    final ArrayList<TestCommand> arrayList = new ArrayList<TestCommand>();
                    arrayList.add(newTestCommand);
                    arrayList.add(testcommand);
                    ifCommand.setTestcommand(new TestCommand(Commands.ALLOF, new ArrayList<Object>(), arrayList));
                }
            }
        }
    }

    private void changeOutgoingVacationRule(final ArrayList<Rule> clientrules) throws SieveException {
        final ConfigurationService config = MailFilterServletServiceRegistry.getServiceRegistry().getService(ConfigurationService.class);
        final String vacationdomains = config.getProperty(MailFilterProperties.Values.VACATION_DOMAINS.property);

        if (null != vacationdomains && 0 != vacationdomains.length()) {
            for (final Rule rule : clientrules) {
                final IfCommand ifCommand = rule.getIfCommand();
                final RuleComment ruleComment = rule.getRuleComment();
                if (null != ruleComment && null != ruleComment.getFlags() && ruleComment.getFlags().contains("vacation") && ActionCommand.Commands.VACATION.equals(ifCommand.getActioncommands().get(0).getCommand())) {
                    final TestCommand testcommand = ifCommand.getTestcommand();
                    if (Commands.ADDRESS.equals(testcommand.getCommand())) {
                        // Test command found now check if it's the right one...
                        if (checkOwnVacation(testcommand.getArguments())) {
                            ifCommand.setTestcommand(new TestCommand(TestCommand.Commands.TRUE, new ArrayList<Object>(), new ArrayList<TestCommand>()));
                        }
                    } else if (Commands.ALLOF.equals(testcommand.getCommand())) {
                        // In this case we find "our" rule at the first place
                        final List<TestCommand> testcommands = testcommand.getTestcommands();
                        if (null != testcommands && testcommands.size() > 1) {
                            final TestCommand testCommand2 = testcommands.get(0);
                            if (checkOwnVacation(testCommand2.getArguments())) {
                                // now remove...
                                if (2 == testcommands.size()) {
                                    // If this is one of two convert the rule
                                    ifCommand.setTestcommand(testcommands.get(1));
                                } else if (testcommands.size() > 2) {
                                    // If we have more than one just remove it...
                                    testcommands.remove(0);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private boolean checkOwnVacation(final List<Object> arguments) {
        return null != arguments
            && null != arguments.get(0) && arguments.get(0) instanceof TagArgument && ":is".equals(((TagArgument)arguments.get(0)).getTag())
            && null != arguments.get(1) && arguments.get(1) instanceof TagArgument && ":domain".equals(((TagArgument)arguments.get(1)).getTag())
            && null != arguments.get(2) && arguments.get(2) instanceof List<?> && "From".equals(((List<?>)arguments.get(2)).get(0));
    }

    private JSONArray getActionArray(final ArrayList<String> sieve) {
        final JSONArray actionarray = new JSONArray();
        for (final ActionCommand.Commands command : ActionCommand.Commands.values()) {
            final List<String> required = command.getRequired();
            if (required.isEmpty()) {
                actionarray.put(command.getJsonname());
            } else {
                for (final String req : required) {
                    if (sieve.contains(req)) {
                        actionarray.put(command.getJsonname());
                        break;
                    }
                }
            }
        }
        return actionarray;
    }

    private RuleAndPosition getRightRuleForUniqueId(final ArrayList<Rule> clientrules, final Integer uniqueid,
            final String userName, final String contextStr) throws OXException {
        for (int i = 0; i < clientrules.size(); i++) {
            final Rule rule = clientrules.get(i);
            if (uniqueid.intValue() == rule.getUniqueId()) {
                return new RuleAndPosition(rule, i);
            }
        }
        throw OXMailfilterExceptionCode.NO_SUCH_ID.create(uniqueid, userName, contextStr);
    }

    // private int getIndexOfRightRuleForUniqueId(final ArrayList<Rule>
    // clientrules, Integer uniqueid) throws OXException {
    // for (int i = 0; i < clientrules.size(); i++) {
    // final Rule rule = clientrules.get(i);
    // if (uniqueid == rule.getUniqueId()) {
    // return i;
    // }
    // }
    // throw new OXException(Code.NO_SUCH_ID);
    // }

    /**
     * Fills up the config object
     *
     * @param sieve
     *            A list of sieve capabilities
     * @return
     * @throws JSONException
     */
    private JSONObject getTestAndActionObjects(final ArrayList<String> sieve) throws JSONException {
        final JSONObject retval = new JSONObject();
        retval.put("tests", getTestArray(sieve));
        retval.put("actioncommands", getActionArray(sieve));
        return retval;
    }

    private JSONArray getTestArray(final ArrayList<String> sieve) throws JSONException {
        final JSONArray testarray = new JSONArray();
        for (final TestCommand.Commands command : TestCommand.Commands.values()) {
            final JSONObject object = new JSONObject();
            if (null == command.getRequired() || sieve.contains(command.getRequired())) {
                final JSONArray comparison = new JSONArray();
                object.put("test", command.getCommandname());
                final List<String[]> jsonMatchTypes = command.getJsonMatchTypes();
                if (null != jsonMatchTypes) {
                    for (final String[] matchtype : jsonMatchTypes) {
                        final String value = matchtype[0];
                        if ("".equals(value) || sieve.contains(value)) {
                            comparison.put(matchtype[1]);
                        }
                    }
                }
                object.put("comparison", comparison);
                testarray.put(object);
            }
        }
        return testarray;
    }

    private Integer getUniqueId(final JSONObject json) throws OXException {
        if (json.has("id") && !json.isNull("id")) {
            try {
                return Integer.valueOf(json.getInt("id"));
            } catch (final JSONException e) {
                throw OXMailfilterExceptionCode.ID_MISSING.create();
            }
        }
        throw OXMailfilterExceptionCode.MISSING_PARAMETER.create("id");
    }

    private String fixParsingError(final String script) {
        final String pattern = ":addresses\\s+:";
        return script.replaceAll(pattern, ":addresses \"\" :");
    }

    private void setUidInRule(final Rule newrule, final int uid) {
        final RuleComment name = newrule.getRuleComment();
        if (null != name) {
            name.setUniqueid(uid);
        } else {
            newrule.setRuleComments(new RuleComment(uid));
        }
    }

    /**
     * Used to perform checks to set the right script name when writing
     *
     * @param sieveHandler
     * @param activeScript
     * @param writeback
     * @throws OXSieveHandlerException
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    private void writeScript(final SieveHandler sieveHandler, final String activeScript, final String writeback)
            throws OXSieveHandlerException, IOException, UnsupportedEncodingException {
        final StringBuilder commandBuilder = new StringBuilder(64);

        if (null != activeScript && activeScript.equals(this.scriptname)) {
            sieveHandler.setScript(activeScript, writeback.getBytes("UTF-8"), commandBuilder);
            sieveHandler.setScriptStatus(activeScript, true, commandBuilder);
        } else {
            sieveHandler.setScript(this.scriptname, writeback.getBytes("UTF-8"), commandBuilder);
            sieveHandler.setScriptStatus(this.scriptname, true, commandBuilder);
        }
    }

    private static String getNANString(final NumberFormatException nfe) {
        final String msg = nfe.getMessage();
        if (msg != null && msg.startsWith("For input string: \"")) {
            return msg.substring(19, msg.length() - 1);
        }
        return msg;
    }

    /**
     * The SIEVE parser is not very expressive when it comes to exceptions.
     * This method analyses an exception message and throws a more detailed
     * one if possible.
     */
    private void handleParsingException(final OXSieveHandlerException e, final Credentials credentials) throws OXException{
        final String message = e.toString();

        if(message.contains("unexpected SUBJECT")) {
            throw OXMailfilterExceptionCode.EMPTY_MANDATORY_FIELD.create(e, "ADDRESS (probably)");
        }
        if(message.contains("address ''")) {
            throw OXMailfilterExceptionCode.EMPTY_MANDATORY_FIELD.create(e, "ADDRESS");
        }

        throw OXMailfilterExceptionCode.SIEVE_COMMUNICATION_ERROR.create(e, e.getSieveHost(), Integer.valueOf(e
            .getSieveHostPort()), credentials.getRightUsername(), credentials.getContextString());
    }

    private static final class Key {

        private final int cid;

        private final int user;

        private final int hash;

        public Key(final int user, final int cid) {
            super();
            this.user = user;
            this.cid = cid;
            final int prime = 31;
            int result = 1;
            result = prime * result + cid;
            result = prime * result + user;
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(final Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof Key)) {
                return false;
            }
            final Key other = (Key) obj;
            if (cid != other.cid) {
                return false;
            }
            if (user != other.user) {
                return false;
            }
            return true;
        }

    } // End of class Key

}
