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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.find.mail;

import com.openexchange.i18n.LocalizableStrings;


/**
 * Mail-specific strings are potentially displayed in client applications and
 * should therefore be localized.
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 * @since v7.6.0
 */
public class MailStrings implements LocalizableStrings {

    // Search in emails
    public static final String FACET_GLOBAL = "%1$s <i>in emails</i>";

    // Search in mail field subject.
    public static final String FACET_SUBJECT = "%1$s <i>in subject</i>";

    // Search in mail field text.
    public static final String FACET_MAIL_TEXT = "%1$s <i>in mail text</i>";

    // Search in folders.
    public static final String FACET_FOLDERS = "Folder";

    // Search in senders and recipients.
    public static final String FACET_SENDER_AND_RECIPIENT = "Sender/Recipient";

    // Search in for sender.
    public static final String FACET_SENDER = "Sender";

    // Search in for recipient.
    public static final String FACET_RECIPIENT = "Recipient";

    // Search criteria time
    public static final String FACET_TIME = "Time";

    // Search mails from last week
    public static final String LAST_WEEK = "last week";

    // Search mails from last month
    public static final String LAST_MONTH = "last month";

    // Search mails from last year
    public static final String LAST_YEAR = "last year";

}
