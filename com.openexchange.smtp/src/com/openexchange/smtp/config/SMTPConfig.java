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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.smtp.config;

import java.net.URI;
import java.net.URISyntaxException;
import javax.mail.internet.IDNA;
import com.openexchange.exception.OXException;
import com.openexchange.mail.api.MailCapabilities;
import com.openexchange.mail.transport.config.ITransportProperties;
import com.openexchange.mail.transport.config.TransportConfig;
import com.openexchange.smtp.SMTPExceptionCode;
import com.openexchange.tools.net.URIDefaults;
import com.openexchange.tools.net.URIParser;

/**
 * {@link SMTPConfig} - The SMTP configuration.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class SMTPConfig extends TransportConfig {

    private static final String PROTOCOL_SMTP_SECURE = "smtps";

    /*
     * +++++++++ User-specific fields +++++++++
     */

    private boolean secure;

    private int smtpPort;

    private String smtpServer;

    private ISMTPProperties transportProperties;

    /**
     * Default constructor
     */
    public SMTPConfig() {
        super();
    }

    @Override
    public MailCapabilities getCapabilities() {
        return MailCapabilities.EMPTY_CAPS;
    }

    /**
     * Gets the smtpPort
     *
     * @return the smtpPort
     */
    @Override
    public int getPort() {
        return smtpPort;
    }

    /**
     * Gets the smtpServer
     *
     * @return the smtpServer
     */
    @Override
    public String getServer() {
        return smtpServer;
    }

    @Override
    public boolean isSecure() {
        return secure;
    }

    @Override
    protected void parseServerURL(final String serverURL) throws OXException {
        final URI uri;
        try {
            uri = URIParser.parse(serverURL, URIDefaults.SMTP);
        } catch (final URISyntaxException e) {
            throw SMTPExceptionCode.URI_PARSE_FAILED.create(e, serverURL);
        }
        secure = PROTOCOL_SMTP_SECURE.equals(uri.getScheme());
        smtpServer = uri.getHost();
        smtpPort = uri.getPort();
    }

    @Override
    public void setPort(final int smtpPort) {
        this.smtpPort = smtpPort;
    }

    @Override
    public void setSecure(final boolean secure) {
        this.secure = secure;
    }

    @Override
    public void setServer(final String smtpServer) {
        this.smtpServer = null == smtpServer ? null : IDNA.toUnicode(smtpServer);
    }

    @Override
    public ITransportProperties getTransportProperties() {
        return transportProperties;
    }

    public ISMTPProperties getSMTPProperties() {
        return transportProperties;
    }

    @Override
    public void setTransportProperties(final ITransportProperties transportProperties) {
        this.transportProperties = (ISMTPProperties) transportProperties;
    }
}
