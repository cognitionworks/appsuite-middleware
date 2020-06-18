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

package com.openexchange.mail.compose;

import com.openexchange.i18n.LocalizableStrings;

/**
 *
 * {@link CompositionSpaceExceptionMessages}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.10.1
 */
public class CompositionSpaceExceptionMessages implements LocalizableStrings {

    /**
     * Initializes a new {@link CompositionSpaceExceptionMessages}.
     */
    private CompositionSpaceExceptionMessages() {
        super();
    }

    // This error message is returned to the user in case he/she wants to apply a change to an in-compose message,
    // which is only applicable in case a reply is generated, but in-compose message is not a reply
    public static final String NO_REPLY_FOR_MSG = "The operation cannot be performed because composed message is not a reply";

    // Found no such attachment %1$s in composition space %2$s
    public static final String NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE_MSG = "No such attachment";

    // Attachments must not be shared
    public static final String SHARED_ATTACHMENTS_NOT_ALLOWED_MSG = "Attachments must not be shared";

    // A user must not create any new composition spaces
    public static final String MAX_NUMBER_OF_COMPOSITION_SPACE_REACHED_MSG = "Maximum number of composition spaces is reached. Please terminate existing open spaces in order to open new ones.";

    // Missing key which is required to decrypt the content of a composition space
    public static final String MISSING_KEY_MSG = "Found no suitable key for composition space. Please re-compose your E-Mail.";

    // The user entered a very long subject, which cannot be stored due to data truncation
    public static final String SUBJECT_TOO_LONG_MSG = "The entered subject is too long. Please use a shorter one.";

    // The user entered a very long From address, which cannot be stored due to data truncation
    public static final String FROM_TOO_LONG_MSG = "The entered \"from\" address is too long.";

    // The user entered a very long Sender address, which cannot be stored due to data truncation
    public static final String SENDER_TOO_LONG_MSG = "The entered \"sender\" address is too long.";

    // The user entered a very long To addresses, which cannot be stored due to data truncation
    public static final String TO_TOO_LONG_MSG = "The entered \"to\" addresses are too long.";

    // The user entered a very long Cc addresses, which cannot be stored due to data truncation
    public static final String CC_TOO_LONG_MSG = "The entered \"cc\" addresses are too long.";

    // The user entered a very long Bcc addresses, which cannot be stored due to data truncation
    public static final String BCC_TOO_LONG_MSG = "The entered \"bcc\" addresses are too long.";

    // The user entered a very long Reply-To address, which cannot be stored due to data truncation
    public static final String REPLY_TO_TOO_LONG_MSG = "The entered \"Reply-To\" address is too long.";;
}
