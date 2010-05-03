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

package com.sun.mail.pop3;

import java.io.IOException;
import javax.mail.MessagingException;

/**
 * {@link POP3Prober} - Probes support of <code><small>UIDL</small></code> and <code><small>TOP</small></code> POP3 commands.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class POP3Prober {

    private final Protocol protocol;

    /**
     * Initializes a new {@link POP3Prober}.
     * 
     * @param pop3Store The connected POP3 store
     * @param pop3Folder The POP3 folder to check with
     * @throws IOException If initialization fails
     * @throws MessagingException If initialization fails
     */
    public POP3Prober(final POP3Store pop3Store, final POP3Folder pop3Folder) throws IOException, MessagingException {
        super();
        protocol = (0 == pop3Folder.getMessageCount() ? null : pop3Store.getPort(pop3Folder));
    }

    /**
     * Probes the <code><small>UIDL</small></code> command.
     * 
     * @return <code>true</code> if <code><small>UIDL</small></code> command is supported; otherwise <code>false</code>
     */
    public boolean probeUIDL() {
        if (null == protocol) {
            /*
             * Nothing to probe present
             */
            return true;
        }
        try {
            return protocol.uidl(new String[1]);
        } catch (final IOException e) {
            return false;
        }
    }

    /**
     * Probes the <code><small>TOP</small></code> command.
     * 
     * @return <code>true</code> if <code><small>TOP</small></code> command is supported; otherwise <code>false</code>
     */
    public boolean probeTOP() {
        if (null == protocol) {
            /*
             * Nothing to probe present
             */
            return true;
        }
        try {
            return (null != protocol.top(1, 1));
        } catch (final IOException e) {
            return false;
        }
    }

}
