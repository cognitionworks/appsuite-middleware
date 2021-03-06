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

package com.openexchange.mail.search;

import static com.openexchange.mail.utils.StorageUtility.getAllAddresses;
import java.util.Collection;
import javax.mail.FetchProfile;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.search.RecipientStringTerm;
import com.openexchange.exception.OXException;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.mime.utils.MimeMessageUtility;

/**
 * {@link CcTerm}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class CcTerm extends AbstractAddressTerm {

    private static final long serialVersionUID = 4340549452409949555L;

    /**
     * Initializes a new {@link CcTerm}
     */
    public CcTerm(String pattern) {
        super(pattern);
    }

    @Override
    public void accept(SearchTermVisitor visitor) {
        visitor.visit(this);
    }

    /**
     * @return The pattern of the cc address
     */
    @Override
    public String getPattern() {
        return addr;
    }

    @Override
    public void addMailField(Collection<MailField> col) {
        col.add(MailField.CC);
    }

    @Override
    public boolean matches(MailMessage mailMessage) {
        if (containsWildcard()) {
            return toRegex(addr).matcher(getAllAddresses(mailMessage.getCc())).find();
        }
        return (Strings.asciiLowerCase(getAllAddresses(mailMessage.getCc())).indexOf(getLowerCaseAddr()) >= 0);
    }

    @Override
    public boolean matches(Message msg) throws OXException {
        try {
            /*
             * Get plain headers
             */
            final String[] headers = msg.getHeader("Cc");
            if (null == headers || headers.length == 0) {
                return false;
            }
            /*
             * Parse addresses
             */
            final InternetAddress[] addresses = MimeMessageUtility.parseAddressList(MimeMessageUtility.decodeMultiEncodedHeader(headers[0]), false, false);
            if (containsWildcard()) {
                return toRegex(addr).matcher(getAllAddresses(addresses)).find();
            }
            return (Strings.asciiLowerCase(getAllAddresses(addresses)).indexOf(getLowerCaseAddr()) >= 0);
        } catch (MessagingException e) {
            org.slf4j.LoggerFactory.getLogger(CcTerm.class).warn("Error during search.", e);
            return false;
        }
    }

    @Override
    public javax.mail.search.SearchTerm getJavaMailSearchTerm() {
        return new RecipientStringTerm(Message.RecipientType.CC, addr);
    }

    @Override
    public javax.mail.search.SearchTerm getNonWildcardJavaMailSearchTerm() {
        return new RecipientStringTerm(Message.RecipientType.CC, getNonWildcardPart(addr));
    }

    @Override
    public void contributeTo(FetchProfile fetchProfile) {
        if (!fetchProfile.contains(FetchProfile.Item.ENVELOPE)) {
            fetchProfile.add(FetchProfile.Item.ENVELOPE);
        }
    }

    @Override
    public boolean isAscii() {
        return isAscii(addr);
    }

    @Override
    public boolean containsWildcard() {
        return null == addr ? false : addr.indexOf('*') >= 0 || addr.indexOf('?') >= 0;
    }
}
