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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.ajax.contact.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.openexchange.ajax.framework.CommonAllRequest;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.search.Order;

/**
 * Contains the data for an appointment all request.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 * @author <a href="mailto:ben.pahne@open-xchange.org">Ben Pahne</a>
 */
public class AllRequest extends CommonAllRequest {

    public static final int[] GUI_COLUMNS = new int[] {
    	ContactObject.OBJECT_ID,
    	ContactObject.FOLDER_ID
    };

    public static final int GUI_SORT = ContactObject.SUR_NAME;

    public static final Order GUI_ORDER = Order.ASCENDING;

    @Deprecated
    public AllRequest(final int folderId, final int[] columns, final Date start,
                      final Date end) {
        this(folderId, columns);
    }

    @Deprecated
    public AllRequest(final int folderId, final int[] columns, final Date start,
                      final Date end, final boolean recurrenceMaster) {
        this(folderId, columns);
    }
    
    /**
     * Default constructor.
     */
    public AllRequest(final int folderId, final int[] columns) {
        super(AbstractContactRequest.URL, folderId, addGUIColumns(columns),
            0, null, true);
    }

    private static int[] addGUIColumns(final int[] columns) {
        final List<Integer> list = new ArrayList<Integer>();
        for (int i = 0; i < columns.length; i++) {
            list.add(Integer.valueOf(columns[i]));
        }
        // Move GUI_COLUMNS to end unless already in there.
        for (int i = 0; i < GUI_COLUMNS.length; i++) {
            final Integer column = Integer.valueOf(GUI_COLUMNS[i]);
            if (!list.contains(column)) {
                list.add(column);
            }
        }
        final int[] retval = new int[list.size()];
        for (int i = 0; i < retval.length; i++) {
            retval[i] = list.get(i).intValue();
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AllParser getParser() {
        return new AllParser(isFailOnError(), getColumns());
    }
}
