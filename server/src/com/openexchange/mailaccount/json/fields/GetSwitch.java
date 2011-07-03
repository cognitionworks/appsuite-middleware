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

package com.openexchange.mailaccount.json.fields;

import com.openexchange.mailaccount.AttributeSwitch;
import com.openexchange.mailaccount.MailAccountDescription;
import com.openexchange.mailaccount.MailAccountException;

/**
 * {@link GetSwitch}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class GetSwitch implements AttributeSwitch {

    private static final org.apache.commons.logging.Log LOG = com.openexchange.exception.Log.valueOf(org.apache.commons.logging.LogFactory.getLog(GetSwitch.class));

    private final MailAccountDescription desc;

    public GetSwitch(final MailAccountDescription desc) {
        this.desc = desc;
    }

    public Object confirmedHam() {
        return desc.getConfirmedHam();
    }

    public Object confirmedSpam() {
        return desc.getConfirmedSpam();
    }

    public Object drafts() {
        return desc.getDrafts();
    }

    public Object id() {
        return Integer.valueOf(desc.getId());
    }

    public Object login() {
        return desc.getLogin();
    }

    public Object mailURL() throws MailAccountException {
        return desc.generateMailServerURL();
    }

    public Object name() {
        return desc.getName();
    }

    public Object password() {
        return desc.getPassword();
    }

    public Object primaryAddress() {
        return desc.getPrimaryAddress();
    }

    public Object personal() {
        return desc.getPersonal();
    }

    public Object sent() {
        return desc.getSent();
    }

    public Object spam() {
        return desc.getSpam();
    }

    public Object spamHandler() {
        return desc.getSpamHandler();
    }

    public Object transportURL() throws MailAccountException {
        return desc.generateTransportServerURL();
    }

    public Object trash() {
        return desc.getTrash();
    }

    public Object mailPort() {
        return Integer.valueOf(desc.getMailPort());
    }

    public Object mailProtocol() {
        return desc.getMailProtocol();
    }

    public Object mailSecure() {
        return Boolean.valueOf(desc.isMailSecure());
    }

    public Object mailServer() {
        return desc.getMailServer();
    }

    public Object transportPort() {
        return Integer.valueOf(desc.getTransportPort());
    }

    public Object transportProtocol() {
        return desc.getTransportProtocol();
    }

    public Object transportSecure() {
        return Boolean.valueOf(desc.isTransportSecure());
    }

    public Object transportServer() {
        return desc.getTransportServer();
    }

    public Object transportLogin() {
        return desc.getTransportLogin();
    }

    public Object transportPassword() {
        return desc.getTransportPassword();
    }

    public Object unifiedINBOXEnabled() {
        return Boolean.valueOf(desc.isUnifiedINBOXEnabled());
    }

    public Object confirmedHamFullname() {
        return desc.getConfirmedHamFullname();
    }

    public Object confirmedSpamFullname() {
        return desc.getConfirmedSpamFullname();
    }

    public Object draftsFullname() {
        return desc.getDraftsFullname();
    }

    public Object sentFullname() {
        return desc.getSentFullname();
    }

    public Object spamFullname() {
        return desc.getSpamFullname();
    }

    public Object trashFullname() {
        return desc.getTrashFullname();
    }

    public Object pop3DeleteWriteThrough() {
        return Boolean.valueOf(desc.getProperties().get("pop3.deletewt"));
    }

    public Object pop3ExpungeOnQuit() {
        return Boolean.valueOf(desc.getProperties().get("pop3.expunge"));
    }

    public Object pop3RefreshRate() {
        return desc.getProperties().get("pop3.refreshrate");
    }

    public Object pop3Path() {
        return desc.getProperties().get("pop3.path");
    }

    public Object pop3Storage() {
        return desc.getProperties().get("pop3.storage");
    }
    
}
