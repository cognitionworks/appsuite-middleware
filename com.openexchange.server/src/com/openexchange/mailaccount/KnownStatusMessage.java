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

package com.openexchange.mailaccount;

import com.openexchange.i18n.LocalizableStrings;


/**
 * {@link KnownStatusMessage}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.8.3
 */
public class KnownStatusMessage implements LocalizableStrings {

    /**
     * Initializes a new {@link KnownStatusMessage}.
     */
    private KnownStatusMessage() {
        super();
    }

    // The message advertising that everything is fine with checked account
    public static final String MESSAGE_OK = "All fine";

    // The message advertising that authentication against referenced mail account does not work or stopped working
    public static final String MESSAGE_INVALID_CREDENTIALS = "The entered credential or authentication information does not work or are no longer accepted by provider. Please change them.";

    // The message advertising that affected account is broken and needs to be re-created
    public static final String MESSAGE_RECREATION_NEEDED = "Account is broken and needs to be re-created";

    // The message advertising that affected account has been disabled by administrator
    public static final String MESSAGE_DISABLED = "Account is disabled.";

    // The message advertising that affected account is currently in setup phase and does not yet reflect up-to-date information
    public static final String MESSAGE_IN_SETUP = "Account is currently being set-up.";
    
    // The message advertising that the affected account is facing an SSL problem
    public static final String MESSAGE_SSL_ERROR = "There was an SSL problem.";

    // The message advertising that the status of the account could not be determined.
    public static final String MESSAGE_UNKNOWN = "The account status could not be determined.";

    // The message advertising that the affected account is not supported.
    public static final String MESSAGE_UNSUPPORTED = "The account is not supported.";

    // The message advertising that the affected account cannot be accessed.
    public static final String MESSAGE_INACCESSIBLE = "The account cannot be accessed.";

}
