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

package com.openexchange.folderstorage;

import java.util.Date;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.session.Session;

/**
 * {@link StorageParameters} - The storage parameters to perform a certain storage operation.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface StorageParameters {

    /**
     * Gets the context.
     * 
     * @return The context
     */
    Context getContext();

    /**
     * Gets the user.
     * 
     * @return The user
     */
    User getUser();

    /**
     * Gets the session.
     * 
     * @return The session
     */
    Session getSession();

    /**
     * Gets the optional decorator.
     * 
     * @return The decorator or <code>null</code>
     */
    FolderServiceDecorator getDecorator();

    /**
     * Sets the decorator.
     * 
     * @param decorator The decorator
     */
    void setDecorator(FolderServiceDecorator decorator);

    /**
     * Gets a <b>copy</b> of the requestor's last-modified time stamp.
     * 
     * @return A <b>copy</b> of the requestor's last-modified time stamp or <code>null</code>
     */
    Date getTimeStamp();

    /**
     * Sets the requestor's last-modified time stamp.
     * <p>
     * <b>Note</b>: Given time stamp is copied if not <code>null</code>.
     * 
     * @param timeStamp The requestor's last-modified time stamp or <code>null</code> to remove
     */
    void setTimeStamp(Date timeStamp);

    /**
     * Gets the parameter bound to given name.
     * 
     * @param folderType The folder type
     * @param name The parameter name
     * @return The parameter bound to given name
     */
    Object getParameter(FolderType folderType, String name);

    /**
     * Puts given parameter. Any existing parameters bound to given name are replaced. A <code>null</code> value means to remove the
     * parameter.
     * <p>
     * A <code>null</code> value removes the parameter.
     * 
     * @param folderType The folder type
     * @param name The parameter name
     * @param value The parameter value
     */
    void putParameter(FolderType folderType, String name, Object value);

    /**
     * (Atomically) Puts given parameter only if the specified name is not already associated with a value.
     * <p>
     * A <code>null</code> value is not permitted.
     * 
     * @param folderType The folder type
     * @param name The parameter name
     * @param value The parameter value
     * @throws IllegalArgumentException If value is <code>null</code>
     * @return <code>true</code> if put was successful; otherwise <code>false</code>
     */
    boolean putParameterIfAbsent(FolderType folderType, String name, Object value);

}
