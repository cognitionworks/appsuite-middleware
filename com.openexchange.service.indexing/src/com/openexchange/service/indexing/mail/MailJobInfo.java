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

package com.openexchange.service.indexing.mail;



/**
 * {@link MailJobInfo} - Provides necessary information for performing a mail job.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailJobInfo {

    /**
     * The context identifier
     */
    public final int contextId;

    /**
     * The user identifier.
     */
    public final int userId;

    /**
     * The account identifier.
     */
    public final int accountId;

    /**
     * The primary password.
     */
    public final String primaryPassword;

    /**
     * The login.
     */
    public final String login;

    /**
     * The password.
     */
    public final String password;

    /**
     * The server's host name or IP address.
     */
    public final String server;

    /**
     * The port.
     */
    public final int port;

    /**
     * Whether a secure connection shall be established.
     */
    public final boolean secure;

    /**
     * Initializes a new {@link MailJobInfo}.
     */
    protected MailJobInfo(final Builder builder) {
        super();
        this.accountId = builder.accountId;
        this.contextId = builder.contextId;
        this.userId = builder.userId;
        this.primaryPassword = builder.primaryPassword;
        this.login = builder.login;
        this.password = builder.password;
        this.server = builder.server;
        this.port = builder.port;
        this.secure = builder.secure;
    }

    

    @Override
    public String toString() {
        final StringBuilder builder2 = new StringBuilder(32);
        builder2.append("{contextId=").append(contextId).append(", userId=").append(userId).append(", accountId=").append(
            accountId).append(", ");
        if (login != null) {
            builder2.append("login=").append(login).append(", ");
        }
        if (server != null) {
            builder2.append("server=").append(server).append(", ");
        }
        builder2.append("port=").append(port).append(", secure=").append(secure).append('}');
        return builder2.toString();
    }


    /**
     * Initializes a new {@link Builder}.
     * <p>
     * Convenience method for <code>new MailJobInfo.Builder(userId, contextId)</code>.
     * 
     * @param userId The user identifier
     * @param contextId The context identifier
     * @return The new builder
     */
    public static Builder newBuilder(final int userId, final int contextId) {
        return new Builder(userId, contextId);
    }

    /**
     * Builds a {@link MailJobInfo} instance.
     */
    public static final class Builder {
        protected final int contextId;
        protected final int userId;
        protected int accountId;
        protected String primaryPassword;
        protected String login;
        protected String password;
        protected String server;
        protected int port;
        protected boolean secure;

        /**
         * Initializes a new {@link Builder}.
         * 
         * @param userId The user identifier
         * @param contextId The context identifier
         */
        public Builder(final int userId, final int contextId) {
            super();
            this.userId = userId;
            this.contextId = contextId;
        }

        /**
         * Builds a {@link MailJobInfo} instance.
         * 
         * @return The {@link MailJobInfo} instance
         */
        public MailJobInfo build() {
            return new MailJobInfo(this);
        }

        public Builder accountId(final int accountId) {
            this.accountId = accountId;
            return this;
        }

        public Builder primaryPassword(final String primaryPassword) {
            this.primaryPassword = primaryPassword;
            return this;
        }

        public Builder login(final String login) {
            this.login = login;
            return this;
        }

        public Builder password(final String password) {
            this.password = password;
            return this;
        }

        public Builder server(final String server) {
            this.server = server;
            return this;
        }

        public Builder port(final int port) {
            this.port = port;
            return this;
        }

        public Builder secure(final boolean secure) {
            this.secure = secure;
            return this;
        }

    }

}
