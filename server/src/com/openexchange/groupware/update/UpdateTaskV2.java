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

package com.openexchange.groupware.update;

import com.openexchange.groupware.AbstractOXException;

/**
 * Second generation of update tasks.
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public interface UpdateTaskV2 extends UpdateTask {

    static final int NO_VERSION = Schema.NO_VERSION;

    /**
     * Performs the database schema upgrade.
     * @param params Interface carrying some useful parameters for performing the update. This is a parameter interface to be extendable for
     * future requirements without breaking the API.
     * @throws AbstractOXException should be thrown if the update fails. Then it can be tried to execute this task again.
     */
    void perform(PerformParameters params) throws AbstractOXException;

    /**
     * This method is used to determine the order when executing update tasks. Check VERY carefully what update tasks must be run before
     * your task can run. For all currently existing update task the dependency returns always the previous update task to remain the same
     * order as with the versions.
     * @return a string array containing the update tasks that must be run before running this one. You may return an empty array if you can
     * not discover any dependencies. Never return <code>null</code>.
     */
    String[] getDependencies();

    /**
     * Defines the attributes of a database update task. Please read the corresponding java documentation for the interfaces and enums to
     * get an understandig for the attributes.
     * @return the attributes.
     */
    TaskAttributes getAttributes();
}
