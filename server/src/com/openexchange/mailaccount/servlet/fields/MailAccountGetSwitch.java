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

package com.openexchange.mailaccount.servlet.fields;

import com.openexchange.mailaccount.AttributeSwitch;
import com.openexchange.mailaccount.MailAccount;

/**
 * {@link MailAccountGetSwitch}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class MailAccountGetSwitch implements AttributeSwitch {

    private final MailAccount account;

    public MailAccountGetSwitch(final MailAccount desc) {
        account = desc;
    }

    public Object confirmedHam() {
        return account.getConfirmedHam();
    }

    public Object confirmedSpam() {
        return account.getConfirmedSpam();
    }

    public Object drafts() {
        return account.getDrafts();
    }

    public Object id() {
        return Integer.valueOf(account.getId());
    }

    public Object login() {
        return account.getLogin();
    }

    public Object mailURL() {
        return account.generateMailServerURL();
    }

    public Object name() {
        return account.getName();
    }

    public Object password() {
        return account.getPassword();
    }

    public Object primaryAddress() {
        return account.getPrimaryAddress();
    }
    
    public Object personal() {
        return account.getPersonal();
    }

    public Object sent() {
        return account.getSent();
    }

    public Object spam() {
        return account.getSpam();
    }

    public Object spamHandler() {
        return account.getSpamHandler();
    }

    public Object transportURL() {
        return account.generateTransportServerURL();
    }

    public Object trash() {
        return account.getTrash();
    }

    public Object mailPort() {
        return Integer.valueOf(account.getMailPort());
    }

    public Object mailProtocol() {
        return account.getMailProtocol();
    }

    public Object mailSecure() {
        return Boolean.valueOf(account.isMailSecure());
    }

    public Object mailServer() {
        return account.getMailServer();
    }

    public Object transportPort() {
        return Integer.valueOf(account.getTransportPort());
    }

    public Object transportProtocol() {
        return account.getTransportProtocol();
    }

    public Object transportSecure() {
        return Boolean.valueOf(account.isTransportSecure());
    }

    public Object transportServer() {
        return account.getTransportServer();
    }

    public Object transportLogin() {
        return account.getTransportLogin();
    }

    public Object transportPassword() {
        return account.getTransportPassword();
    }

    public Object unifiedINBOXEnabled() {
        return Boolean.valueOf(account.isUnifiedINBOXEnabled());
    }

    public Object confirmedHamFullname() {
        return account.getConfirmedHamFullname();
    }

    public Object confirmedSpamFullname() {
        return account.getConfirmedSpamFullname();
    }

    public Object draftsFullname() {
        return account.getDraftsFullname();
    }

    public Object sentFullname() {
        return account.getSentFullname();
    }

    public Object spamFullname() {
        return account.getSpamFullname();
    }

    public Object trashFullname() {
        return account.getTrashFullname();
    }

    public Object pop3DeleteWriteThrough() {
        return account.getProperties().get("pop3.deletewt");
    }

    public Object pop3ExpungeOnQuit() {
        return account.getProperties().get("pop3.expunge");
    }

    public Object pop3RefreshRate() {
        return account.getProperties().get("pop3.refreshrate");
    }

    public Object pop3Path() {
        return account.getProperties().get("pop3.path");
    }

    public Object pop3Storage() {
        return account.getProperties().get("pop3.storage");
    }
}
