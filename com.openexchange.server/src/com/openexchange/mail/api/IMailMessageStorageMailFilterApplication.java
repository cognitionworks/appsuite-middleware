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

package com.openexchange.mail.api;

import java.util.List;
import com.openexchange.exception.OXException;
import com.openexchange.mail.dataobjects.MailFilterResult;
import com.openexchange.mail.search.SearchTerm;

/**
 * {@link IMailMessageStorageMailFilterApplication} - Extends basic message storage by applying a mail filter expression to a mailbox' messages.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface IMailMessageStorageMailFilterApplication extends IMailMessageStorage {

    /**
     * Indicates if applying mail filters are supported.
     *
     * @return <code>true</code> if supported; otherwise <code>false</code>
     * @throws OXException If check fails
     */
    boolean isMailFilterApplicationSupported() throws OXException;

    /**
     * Applies the given mail filter script to the messages contained in folder specified by full name.
     *
     * @param fullName the folder full name
     * @param mailFilterScript The mail filter script to apply
     * @param searchTerm The search term to filter the messages to which the mail filter script is supposed to be applied; may be <code>null</code> to apply to all messages
     * @param acceptOkFilterResults <code>true</code> in case OK filter results should be kept; otherwise <code>false</code> to discard them (and only return filter results of either ERRORS or WARNINGS)
     * @return The filter results
     * @throws OXException If an error occurs
     */
    List<MailFilterResult> applyMailFilterScript(String fullName, String mailFilterScript, SearchTerm<?> searchTerm, boolean acceptOkFilterResults) throws OXException;

}
