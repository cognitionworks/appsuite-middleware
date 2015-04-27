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

package com.openexchange.contact.storage.rdb.mbean;

import java.util.List;
import javax.management.MBeanException;
import com.openexchange.exception.OXException;

/**
 * {@link ContactStorageMBean}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface ContactStorageMBean {

    static final String DOMAIN = "com.openexchange.contact";
    static final String NAME = "RDB Contact Storage Toolkit";

    /**
     * De-duplicates contacts in a folder.
     *
     * @param contextID The context ID
     * @param folderID The folder ID
     * @param limit The maximum number of contacts to process, or <code>0</code> for no limits
     * @param dryRun <code>true</code> to analyze the folder for duplicates only, without actually performing the deduplication,
     *               <code>false</code>, otherwise
     * @return The identifiers of the contacts identified (and deleted in case <code>dryRun</code> is <code>false</code>) as duplicates
     * @throws OXException
     */
    int[] deduplicateContacts(int contextID, int folderID, long limit, boolean dryRun);

    /**
     * Checks the user aliases for completeness; e.g. do contain primaryMail, Email1 and defaultSenderAddress.
     *
     * @param optContextId The optiona context identifier
     * @param dryRun <code>true</code> to just check for incomplete aliases, but does not modify anything; otherwise <code>false</code>
     * @return The context-wise listed identifiers of the users with incomplete aliases; first position of each array is always the context identifier
     * @throws MBeanException If operation fails for any reason
     */
    List<List<Integer>> checkUserAliases(int optContextId, boolean dryRun) throws MBeanException;

}
