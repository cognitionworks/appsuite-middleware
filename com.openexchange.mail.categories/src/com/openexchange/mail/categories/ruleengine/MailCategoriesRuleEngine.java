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

package com.openexchange.mail.categories.ruleengine;

import com.openexchange.exception.OXException;
import com.openexchange.session.Session;

/**
 * {@link MailCategoriesRuleEngine}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.2
 */
public interface MailCategoriesRuleEngine {

    /**
     * Sets the rule. If available the old rule will be overwritten.
     *
     * @param session The user session
     * @param rule The new rule
     * @throws OXException If set operation fails
     */
    public void setRule(Session session, MailCategoryRule rule) throws OXException;

    /**
     * Removes the rule which match the given flag
     *
     * @param session The user session
     * @param flag The mail flag
     * @throws OXException If remove attempt fails
     */
    public void removeRule(Session session, String flag) throws OXException;

    /**
     * Retrieves the current rule of the given category or null if no rule exists.
     *
     * @param session The user session
     * @param flag The mail flag
     * @return The current rule or null
     * @throws OXException If rule cannot be returned
     */
    public MailCategoryRule getRule(Session session, String flag) throws OXException;

    /**
     * Removes the value from all condition headers
     * 
     * @param session The user session
     * @param value The value to remove
     * @param header The mail header name
     * @throws OXException
     */
    public void removeValueFromHeader(Session session, String value, String header) throws OXException;

}
