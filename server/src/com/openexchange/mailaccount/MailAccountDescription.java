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

package com.openexchange.mailaccount;

import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import com.openexchange.exception.OXException;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;
import com.openexchange.tools.net.URITools;

/**
 * {@link MailAccountDescription} - Container object describing a mail account to insert/update.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailAccountDescription implements Serializable {

    private static final org.apache.commons.logging.Log LOG =
        com.openexchange.log.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(MailAccountDescription.class));

    private static final long serialVersionUID = -2443656355399068302L;

    private int id;

    private String login;

    private String password;

    private String transportLogin;

    private String transportPassword;

    private String name;

    private String primaryAddress;

    private String personal;

    private String replyTo;

    private String spamHandler;

    private String trash;

    private String sent;

    private String drafts;

    private String spam;

    private String confirmedSpam;

    private String confirmedHam;

    private boolean defaultFlag;

    private String mailServer;

    private int mailPort;

    private String mailProtocol;

    private boolean mailSecure;

    private String transportServer;

    private int transportPort;

    private String transportProtocol;

    private boolean transportSecure;

    private String mailServerUrl;

    private String transportUrl;

    private boolean unifiedINBOXEnabled;

    private String trashFullname;

    private String sentFullname;

    private String draftsFullname;

    private String spamFullname;

    private String confirmedSpamFullname;

    private String confirmedHamFullname;

    private Map<String, String> properties;

    /**
     * Initializes a new {@link MailAccountDescription}.
     */
    public MailAccountDescription() {
        super();
        properties = Collections.emptyMap();
        transportPort = 25;
        mailPort = 143;
        transportProtocol = "smtp";
        mailProtocol = "imap";
        id = -1;
    }

    /**
     * Gets the ID.
     *
     * @return The ID or <code>-1</code> if not set
     */
    public int getId() {
        return id;
    }

    /**
     * Gets the login.
     *
     * @return The login
     */
    public String getLogin() {
        return login;
    }

    /**
     * Gets the account name.
     *
     * @return The account name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets clear, non-encrypted password.
     *
     * @return The clear, non-encrypted password.
     */
    public String getPassword() {
        return password;
    }

    /**
     * Gets the primary email address.
     *
     * @return The primary email address
     */
    public String getPrimaryAddress() {
        return primaryAddress;
    }

    /**
     * Gets the personal.
     *
     * @return The personal
     */
    public String getPersonal() {
        return personal;
    }

    /**
     * Gets the reply-to address
     *
     * @return The reply-to address
     */
    public String getReplyTo() {
        return replyTo;
    }

    /**
     * Gets the ID
     *
     * @return The ID
     */
    public void setId(final int id) {
        this.id = id;
    }

    /**
     * Sets the login.
     *
     * @param login The login
     */
    public void setLogin(final String login) {
        this.login = login;
    }

    /**
     * Sets the clear-text password (which is stored encrypted).
     *
     * @param password The clear-text password
     */
    public void setPassword(final String password) {
        this.password = password;
    }

    /**
     * Gets the mail server name.
     * <p>
     * The mail server name can either be a machine name, such as "<code>java.sun.com</code>", or a textual representation of its IP
     * address.
     *
     * @return The mail server name
     */
    public String getMailServer() {
        return mailServer;
    }

    /**
     * Gets the mail server port.
     *
     * @return The mail server port
     */
    public int getMailPort() {
        return mailPort;
    }

    /**
     * Gets the mail server protocol.
     *
     * @return The mail server protocol
     */
    public String getMailProtocol() {
        return mailProtocol;
    }

    /**
     * Checks if a secure connection to mail server shall be established.
     *
     * @return <code>true</code> if a secure connection to mail server shall be established; otherwise <code>false</code>
     */
    public boolean isMailSecure() {
        return mailSecure;
    }

    /**
     * Sets the mail server name.
     *
     * @param mailServer The mail server name to set
     */
    public void setMailServer(final String mailServer) {
        mailServerUrl = null;
        this.mailServer = mailServer;
    }

    /**
     * Sets the mail server port.
     *
     * @param mailPort The mail server port to set
     */
    public void setMailPort(final int mailPort) {
        mailServerUrl = null;
        this.mailPort = mailPort;
    }

    /**
     * Sets the mail server protocol.
     *
     * @param mailProtocol The mail server protocol to set
     */
    public void setMailProtocol(final String mailProtocol) {
        mailServerUrl = null;
        this.mailProtocol = mailProtocol;
    }

    /**
     * Sets if a secure connection to mail server shall be established.
     *
     * @param mailSecure <code>true</code> if a secure connection to mail server shall be established; otherwise <code>false</code>
     */
    public void setMailSecure(final boolean mailSecure) {
        mailServerUrl = null;
        this.mailSecure = mailSecure;
    }

    /**
     * Gets the transport server name.
     * <p>
     * The transport server name can either be a machine name, such as "<code>java.sun.com</code>", or a textual representation of its IP
     * address.
     *
     * @return The transport server name
     */
    public String getTransportServer() {
        return transportServer;
    }

    /**
     * Gets the transport server port.
     *
     * @return The transport server port
     */
    public int getTransportPort() {
        return transportPort;
    }

    /**
     * Gets the transport server protocol.
     *
     * @return The transport server protocol
     */
    public String getTransportProtocol() {
        return transportProtocol;
    }

    /**
     * Checks if a secure connection to transport server shall be established.
     *
     * @return <code>true</code> if a secure connection to transport server shall be established; otherwise <code>false</code>
     */
    public boolean isTransportSecure() {
        return transportSecure;
    }

    /**
     * Sets the transport server name.
     *
     * @param transportServer The transport server name to set
     */
    public void setTransportServer(final String transportServer) {
        transportUrl = null;
        this.transportServer = transportServer;
    }

    /**
     * Sets the transport server port
     *
     * @param transportPort The transport server port to set
     */
    public void setTransportPort(final int transportPort) {
        transportUrl = null;
        this.transportPort = transportPort;
    }

    /**
     * Sets the transport server protocol
     *
     * @param transportProtocol The transport server protocol to set
     */
    public void setTransportProtocol(final String transportProtocol) {
        transportUrl = null;
        this.transportProtocol = transportProtocol;
    }

    /**
     * Sets if a secure connection to transport server shall be established.
     *
     * @param mailSecure <code>true</code> if a secure connection to transport server shall be established; otherwise <code>false</code>
     */
    public void setTransportSecure(final boolean transportSecure) {
        transportUrl = null;
        this.transportSecure = transportSecure;
    }

    /**
     * Generates the mail server URL.
     *
     * @return The generated mail server URL
     * @throws OXException If mail server URL is invalid
     */
    public String generateMailServerURL() throws OXException {
        if (null != mailServerUrl) {
            return mailServerUrl;
        }
        if (isEmpty(mailServer)) {
            return null;
        }
        try {
            return mailServerUrl = URITools.generateURI(mailSecure ? mailProtocol + 's' : mailProtocol, mailServer, mailPort).toString();
        } catch (final URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(mailProtocol);
            if (mailSecure) {
                sb.append('s');
            }
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(
                e,
                sb.append("://").append(mailServer).append(':').append(mailPort).toString());
        }
    }

    /**
     * Parses specified mail server URL
     *
     * @param mailServerURL The mail server URL to parse
     * @throws OXException If URL cannot be parsed
     */
    public void parseMailServerURL(final String mailServerURL) throws OXException {
        if (null == mailServerURL) {
            setMailServer((String) null);
            return;
        }
        try {
            setMailServer(URIParser.parse(mailServerURL, URIDefaults.IMAP));
        } catch (final URISyntaxException e) {
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, mailServerURL);
            // TODO method needs to throw the following exception. But that needs a global changing of a mass of code. Doing fallback
            // instead now.
            // throw MailAccountExceptionFactory.getInstance().create(MailAccountExceptionMessages.URI_PARSE_FAILED, e, mailServerURL);
            // final String[] tmp = MailConfig.parseProtocol(mailServerURL);
            // final String prot;
            // final Object[] parsed;
            // if (tmp != null) {
            // prot = tmp[0];
            // parsed = parseServerAndPort(tmp[1], getMailPort());
            // } else {
            // prot = getMailProtocol();
            // parsed = parseServerAndPort(mailServerURL, getMailPort());
            // }
            // if (prot.endsWith("s")) {
            // setMailSecure(true);
            // setMailProtocol(prot.substring(0, prot.length() - 1));
            // } else {
            // setMailProtocol(prot);
            // }
            // setMailServer(parsed[0].toString());
            // setMailPort(((Integer) parsed[1]).intValue());
        }
    }

    public void setMailServer(final URI mailServer) {
        if (null == mailServer) {
            // Parse like old parser to prevent problems.
            setMailServer("");
        } else {
            final String protocol = mailServer.getScheme();
            if (protocol.endsWith("s")) {
                setMailSecure(true);
                setMailProtocol(protocol.substring(0, protocol.length() - 1));
            } else {
                setMailSecure(false);
                setMailProtocol(protocol);
            }
            setMailServer(mailServer.getHost());
            setMailPort(mailServer.getPort());
        }
    }

    /**
     * Parses specified transport server URL
     *
     * @param mailServerURL The transport server URL to parse
     * @throws OXException If URL cannot be parsed
     */
    public void parseTransportServerURL(final String transportServerURL) throws OXException {
        if (null == transportServerURL) {
            setTransportServer((String) null);
            return;
        }
        try {
            setTransportServer(URIParser.parse(transportServerURL, URIDefaults.SMTP));
        } catch (final URISyntaxException e) {
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(e, transportServerURL);
        }
    }

    public void setTransportServer(final URI transportServer) {
        if (null == transportServer) {
            // Parse like old parser to prevent problems.
            setTransportServer("");
        } else {
            final String protocol = transportServer.getScheme();
            if (protocol.endsWith("s")) {
                setTransportSecure(true);
                setTransportProtocol(protocol.substring(0, protocol.length() - 1));
            } else {
                setTransportSecure(false);
                setTransportProtocol(protocol);
            }
            setTransportServer(URITools.getHost(transportServer));
            setTransportPort(transportServer.getPort());
        }
    }

    /**
     * Generates transport server URL
     *
     * @return The transport server URL
     * @throws OXException If URL cannot be parsed
     */
    public String generateTransportServerURL() throws OXException {
        if (null != transportUrl) {
            return transportUrl;
        }
        if (isEmpty(transportServer)) {
            return null;
        }
        final String protocol = transportSecure ? transportProtocol + 's' : transportProtocol;
        try {
            return transportUrl = URITools.generateURI(protocol, transportServer, transportPort).toString();
        } catch (final URISyntaxException e) {
            final StringBuilder sb = new StringBuilder(32);
            sb.append(transportProtocol);
            if (transportSecure) {
                sb.append('s');
            }
            throw MailAccountExceptionCodes.INVALID_HOST_NAME.create(
                e,
                sb.append("://").append(transportServer).append(':').append(mailPort).toString());
        }
    }

    /**
     * Sets the account name.
     *
     * @param name The account name.
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Sets the primary email address.
     *
     * @param primaryAddress The primary email address
     */
    public void setPrimaryAddress(final String primaryAddress) {
        this.primaryAddress = primaryAddress;
    }

    /**
     * Sets the personal.
     *
     * @param personal The personal
     */
    public void setPersonal(final String personal) {
        this.personal = personal;
    }

    /**
     * Sets the reply-to address
     *
     * @param replyTo The reply-to address
     */
    public void setReplyTo(final String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * Gets the name for default drafts folder.
     *
     * @return The name for default drafts folder
     */
    public String getDrafts() {
        return drafts;
    }

    /**
     * Gets the name for default sent folder.
     *
     * @return The name for default sent folder
     */
    public String getSent() {
        return sent;
    }

    /**
     * Gets the name for default spam folder.
     *
     * @return The name for default spam folder
     */
    public String getSpam() {
        return spam;
    }

    /**
     * Gets the name for default trash folder.
     *
     * @return The name for default trash folder
     */
    public String getTrash() {
        return trash;
    }

    /**
     * Gets the name for default confirmed-ham folder.
     *
     * @return The name for default confirmed-ham folder
     */
    public String getConfirmedHam() {
        return confirmedHam;
    }

    /**
     * Gets the name for default confirmed-spam folder.
     *
     * @return The name for default confirmed-spam folder
     */
    public String getConfirmedSpam() {
        return confirmedSpam;
    }

    /**
     * Gets the spam handler name.
     *
     * @return The spam handler name
     */
    public String getSpamHandler() {
        return spamHandler;
    }

    /**
     * Sets the name for default trash folder.
     *
     * @param trash The name for default trash folder
     */
    public void setTrash(final String trash) {
        this.trash = trash;
    }

    /**
     * Sets the name for default sent folder.
     *
     * @param sent The name for default sent folder
     */
    public void setSent(final String sent) {
        this.sent = sent;
    }

    /**
     * Sets the name for default drafts folder.
     *
     * @param drafts The name for default drafts folder
     */
    public void setDrafts(final String drafts) {
        this.drafts = drafts;
    }

    /**
     * Sets the name for default spam folder.
     *
     * @param spam The name for spam trash folder
     */
    public void setSpam(final String spam) {
        this.spam = spam;
    }

    /**
     * Sets the name for default confirmed-spam folder.
     *
     * @param confirmedSpam The name for default confirmed-spam folder
     */
    public void setConfirmedSpam(final String confirmedSpam) {
        this.confirmedSpam = confirmedSpam;
    }

    /**
     * Sets the name for default confirmed-ham folder.
     *
     * @param confirmedHam The name for default confirmed-ham folder
     */
    public void setConfirmedHam(final String confirmedHam) {
        this.confirmedHam = confirmedHam;
    }

    /**
     * Sets the spam handler name.
     *
     * @param spamHandler The spam handler name
     */
    public void setSpamHandler(final String spamHandler) {
        this.spamHandler = spamHandler;
    }

    /**
     * Checks if mail account denotes the default mail account.
     *
     * @return <code>true</code> if mail account denotes the default mail account; otherwise <code>false</code>
     */
    public boolean isDefaultFlag() {
        return defaultFlag;
    }

    /**
     * Sets whether mail account denotes the default mail account.
     *
     * @param defaultFlag <code>true</code> if mail account denotes the default mail account; otherwise <code>false</code>
     */
    public void setDefaultFlag(final boolean defaultFlag) {
        this.defaultFlag = defaultFlag;
    }

    /**
     * Gets the optional transport login.
     *
     * @return The optional transport login
     */
    public String getTransportLogin() {
        return transportLogin;
    }

    /**
     * Gets the optional transport password.
     *
     * @return The optional transport password
     */
    public String getTransportPassword() {
        return transportPassword;
    }

    /**
     * Sets the optional transport login.
     *
     * @param transportLogin The optional transport login
     */
    public void setTransportLogin(final String transportLogin) {
        this.transportLogin = transportLogin;
    }

    /**
     * Sets the optional transport password.
     *
     * @param transportLogin The optional transport password
     */
    public void setTransportPassword(final String transportPassword) {
        this.transportPassword = transportPassword;
    }

    /**
     * Checks if this mail account is enabled for Unified INBOX.
     *
     * @return <code>true</code> if this mail account is enabled for Unified INBOX; otherwise <code>false</code>
     */
    public boolean isUnifiedINBOXEnabled() {
        return unifiedINBOXEnabled;
    }

    /**
     * Sets if this mail account is enabled for Unified INBOX.
     *
     * @param unifiedINBOXEnabled <code>true</code> if this mail account is enabled for Unified INBOX; otherwise <code>false</code>
     */
    public void setUnifiedINBOXEnabled(final boolean unifiedINBOXEnabled) {
        this.unifiedINBOXEnabled = unifiedINBOXEnabled;
    }

    /**
     * Gets the trash fullname
     *
     * @return The trash fullname
     */
    public String getTrashFullname() {
        return trashFullname;
    }

    /**
     * Sets the trash fullname
     *
     * @param trashFullname The trash fullname to set
     */
    public void setTrashFullname(final String trashFullname) {
        this.trashFullname = trashFullname;
    }

    /**
     * Gets the sent fullname
     *
     * @return The sent fullname
     */
    public String getSentFullname() {
        return sentFullname;
    }

    /**
     * Sets the sent fullname
     *
     * @param sentFullname The sent fullname to set
     */
    public void setSentFullname(final String sentFullname) {
        this.sentFullname = sentFullname;
    }

    /**
     * Gets the drafts fullname
     *
     * @return The drafts fullname
     */
    public String getDraftsFullname() {
        return draftsFullname;
    }

    /**
     * Sets the drafts fullname
     *
     * @param draftsFullname The drafts fullname to set
     */
    public void setDraftsFullname(final String draftsFullname) {
        this.draftsFullname = draftsFullname;
    }

    /**
     * Gets the spam fullname
     *
     * @return The spam fullname
     */
    public String getSpamFullname() {
        return spamFullname;
    }

    /**
     * Sets the spam fullname
     *
     * @param spamFullname The spam fullname to set
     */
    public void setSpamFullname(final String spamFullname) {
        this.spamFullname = spamFullname;
    }

    /**
     * Gets the confirmed-spam fullname
     *
     * @return The confirmed-spam fullname
     */
    public String getConfirmedSpamFullname() {
        return confirmedSpamFullname;
    }

    /**
     * Sets the confirmed-spam fullname
     *
     * @param confirmedSpamFullname The confirmed-spam fullname to set
     */
    public void setConfirmedSpamFullname(final String confirmedSpamFullname) {
        this.confirmedSpamFullname = confirmedSpamFullname;
    }

    /**
     * Gets the confirmed-ham fullname
     *
     * @return The confirmed-ham fullname
     */
    public String getConfirmedHamFullname() {
        return confirmedHamFullname;
    }

    /**
     * Sets the confirmed-ham fullname
     *
     * @param confirmedHamFullname The confirmed-ham fullname to set
     */
    public void setConfirmedHamFullname(final String confirmedHamFullname) {
        this.confirmedHamFullname = confirmedHamFullname;
    }

    /**
     * Gets the properties
     *
     * @return The properties
     */
    public Map<String, String> getProperties() {
        if (properties.isEmpty()) {
            return Collections.emptyMap();
        }
        final Map<String, String> clone = new HashMap<String, String>(properties.size());
        clone.putAll(properties);
        return clone;
    }

    /**
     * Sets the properties
     *
     * @param properties The properties to set
     */
    public void setProperties(final Map<String, String> properties) {
        if (null == properties) {
            this.properties = Collections.emptyMap();
        } else if (properties.isEmpty()) {
            this.properties = Collections.emptyMap();
        } else {
            this.properties = new HashMap<String, String>(properties.size());
            this.properties.putAll(properties);
        }
    }

    /**
     * Adds specified name-value-pair to properties.
     *
     * @param name The property name
     * @param value The property value
     */
    public void addProperty(final String name, final String value) {
        if (properties.isEmpty()) {
            properties = new HashMap<String, String>();
        }
        properties.put(name, value);
    }

    private static Object[] parseServerAndPort(final String server, final int defaultPort) {
        final int pos = server.indexOf(':');
        if (pos == -1) {
            return new Object[] { server, Integer.valueOf(defaultPort) };
        }
        int port;
        try {
            port = Integer.parseInt(server.substring(pos + 1));
        } catch (final NumberFormatException e) {
            LOG.warn("Unable to parse port out of URL: " + server + ". Using default port instead: " + defaultPort, e);
            port = defaultPort;
        }
        return new Object[] { server.subSequence(0, pos), Integer.valueOf(port) };
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

}
