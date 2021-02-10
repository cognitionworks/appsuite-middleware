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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.contact.provider;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link ContactsProviderExceptionMessages}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.5
 */
public class ContactsProviderExceptionMessages implements LocalizableStrings {

    /**
     * The requested contact account was not found.
     */
    public static final String ACCOUNT_NOT_FOUND_MSG = "The requested contact account was not found.";
    /**
     * The operation could not be completed due to missing capabilities.
     */
    public static final String MISSING_CAPABILITY_MSG = "The operation could not be completed due to missing capabilities.";
    /**
     * The supplied folder is not supported. Please select a valid folder and try again.
     */
    public static final String UNSUPPORTED_FOLDER_MSG = "The supplied folder is not supported. Please select a valid folder and try again.";
    /**
     * The contacts provider '%1$s' is not available.
     */
    public static final String PROVIDER_NOT_AVAILABLE_MSG = "The contacts provider '%1$s' is not available.";
    /**
     * The requested operation is not supported for contacts provider '%1$s'.
     */
    public static final String UNSUPPORTED_OPERATION_FOR_PROVIDER_MSG = "The requested operation is not supported for contacts provider '%1$s'.";
    /**
     * The operation could not be completed due to a concurrent modification. Please reload the data and try again.
     */
    public static final String CONCURRENT_MODIFICATION_MSG = "The operation could not be completed due to a concurrent modification. Please reload the data and try again.";
    /**
     * The requested contact was not found.
     */
    public static final String CONTACT_NOT_FOUND_MSG = "The requested contact was not found.";
    /**
     * The requested folder was not found.
     */
    public static final String FOLDER_NOT_FOUND_MSG = "The requested folder was not found.";
    /**
     * The field '%1$s' is mandatory. Please supply a valid value and try again.
     */
    public static final String MANDATORY_FIELD_MSG = "The field '%1$s' is mandatory. Please supply a valid value and try again.";
}
