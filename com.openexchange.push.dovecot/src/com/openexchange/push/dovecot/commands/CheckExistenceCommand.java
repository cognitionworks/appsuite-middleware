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

package com.openexchange.push.dovecot.commands;

import static com.openexchange.imap.util.ImapUtility.prepareImapCommandForLogging;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.log.LogProperties;
import com.openexchange.session.Session;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ParsingException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPFolder.ProtocolCommand;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;

/**
 * {@link CheckExistenceCommand} - Issues the special GETMETADATA command to check for a registered push listener at Dovecot.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.0
 */
public class CheckExistenceCommand implements ProtocolCommand {

    private static final Logger LOGGER = LoggerFactory.getLogger(CheckExistenceCommand.class);

    private final Session session;
    private final IMAPFolder imapFolder;

    /**
     * Initializes a new {@link CheckExistenceCommand}.
     *
     * @param imapFolder The IMAP folder
     * @param session The associated user session
     */
    public CheckExistenceCommand(IMAPFolder imapFolder, Session session) {
        super();
        this.session = session;
        this.imapFolder = imapFolder;
    }

    @Override
    public Object doCommand(IMAPProtocol protocol) throws ProtocolException {
        // Craft IMAP command
        String command;
        {
            StringBuilder cmdBuilder = new StringBuilder(32).append("GETMETADATA \"\" (");

            // Append path for Dovecot HTTP-Notify plug-in
            cmdBuilder.append("/private/vendor/vendor.dovecot/http-notify");

            // URL
            /*-
             * Currently not needed as statically configured in Dovecot plug-in
             *
            if (null != uri) {
                cmdBuilder.append('\t').append("url=").append(uri);
            }
            */

            // Auth data
            /*-
             * Currently not needed as statically configured in Dovecot plug-in
             *
            if (Strings.isNotEmpty(authLogin) && Strings.isNotEmpty(authPassword)) {
                cmdBuilder.append('\t').append("auth=basic:").append(authLogin).append(':').append(authPassword);
            }
            */

            // Closing parenthesis
            cmdBuilder.append(")");
            command = cmdBuilder.toString();
        }

        // Issue command
        Response[] r = IMAPCommandsCollection.performCommand(protocol, command);
        int mlen = r.length - 1;
        Response response = r[mlen];
        if (response.isOK()) {
            // Read response
            Boolean retval = Boolean.FALSE;
            for (int i = 0; i < mlen; i++) {
                if ((r[i] instanceof IMAPResponse)) {
                    IMAPResponse ir = (IMAPResponse) r[i];
                    /*-
                     *
                     * ABNF (https://tools.ietf.org/html/rfc5464#section-4.2):
                     *   metadata-resp = "METADATA" SP mailbox SP
                     *                   (entry-values / entry-list)
                     *                   ; empty string for mailbox implies
                     *                   ; server annotation.
                     *
                     *   entry-values  = "(" entry-value *(SP entry-value) ")"
                     *
                     *   entry-list    = entry *(SP entry)
                     *                   ; list of entries used in unsolicited
                     *                   ; METADATA response
                     *
                     *   entry         = astring
                     *                   ; slash-separated path to entry
                     *                   ; MUST NOT contain "*" or "%"*
                     *
                     *   entry-value   = entry SP value
                     *
                     *   value         = nstring / literal8
                     *
                     * Example:
                     *
                     *   * METADATA "" (/private/vendor/vendor.dovecot/http-notify {8}
                     *   user=3@2)
                     */
                    if (ir.keyEquals("METADATA")) {
                        // mailbox
                        ir.readAtomString();

                        while (ir.peekByte() != '(' && ir.peekByte() != 0) {
                            ir.readByte(); // Discard
                        }

                        // not unsolicited, so we expect entry-values
                        if (ir.readByte() != '(') {
                            throw new ParsingException("parse error in METADATA");
                        }

                        // Expect "/private/vendor/vendor.dovecot/http-notify" as entry
                        String entry = ir.readAtomString();
                        if ("/private/vendor/vendor.dovecot/http-notify".equals(entry)) {
                            // Expect "user=" + <user> + "@" + <context> as value
                            String expectedValue = new StringBuilder(16).append("user=").append(session.getUserId()).append('@').append(session.getContextId()).toString();
                            // nstring - seems like JavaMail doesn't support literal8 anyway?
                            String value = ir.readString();
                            if (value == null) {
                                LOGGER.debug("Did not find valid registration for push notifications for {} using: {}", imapFolder.getStore(), command);
                            } else if (expectedValue.equals(value)) {
                                LOGGER.debug("Found valid registration for push notifications for {} using: {}", imapFolder.getStore(), command);
                                retval = Boolean.TRUE;
                            } else {
                                LOGGER.warn("Found invalid registration for push notifications for {} using: {}. Expected \"{}\" but is \"{}\".", imapFolder.getStore(), command, expectedValue, value);
                            }
                        }
                        r[i] = null;
                    }
                }
            }
            protocol.notifyResponseHandlers(r);
            return retval;
        } else if (response.isBAD()) {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            if (null != imapFolder) {
                LogProperties.putProperty(LogProperties.Name.MAIL_FULL_NAME, imapFolder.getFullName());
            }
            throw new BadCommandException(response);
        } else if (response.isNO()) {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            if (null != imapFolder) {
                LogProperties.putProperty(LogProperties.Name.MAIL_FULL_NAME, imapFolder.getFullName());
            }
            throw new CommandFailedException(response);
        } else {
            LogProperties.putProperty(LogProperties.Name.MAIL_COMMAND, prepareImapCommandForLogging(command));
            protocol.handleResult(response);
        }
        return Boolean.FALSE;
    }
}
