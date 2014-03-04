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

package com.openexchange.find.tasks;

import java.util.HashMap;
import java.util.Map;
import com.openexchange.find.facet.FacetType;
import com.openexchange.java.Strings;

/**
 * {@link TasksFacetType} - Facet types for the drive module.
 *
 * @author <a href="mailto:felix.marx@open-xchange.com">Felix Marx</a>
 */
public enum TasksFacetType implements FacetType {

    TASK_PARTICIPANTS(TasksStrings.FACET_TASK_PARTICIPANTS),
    TASK_FOLDERS(TasksStrings.FACET_TASK_FOLDERS),
    TASK_SUBJECT(TasksStrings.FACET_TASK_SUBJECT),
    TASK_DESCRIPTION(TasksStrings.FACET_TASK_DESCRIPTION),
    TASK_LOCATION(TasksStrings.FACET_TASK_LOCATION),
    TASK_ATTACHMENT_NAME(TasksStrings.FACET_TASK_ATTACHMENT_NAME),
    TASK_TYPE(TasksStrings.FACET_TASK_TYPE),
    TASK_STATUS(TasksStrings.FACET_TASK_STATUS), ;

    private static final Map<String, TasksFacetType> typesById = new HashMap<String, TasksFacetType>();
    static {
        for (TasksFacetType type : values()) {
            typesById.put(type.getId(), type);
        }
    }

    private final String displayName;

    private TasksFacetType(final String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String getId() {
        return toString().toLowerCase();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public boolean isFieldFacet() {
        return false;
    }

    @Override
    public boolean appliesOnce() {
        return false;
    }

    /**
     * Gets a {@link TasksFacetType} by its id.
     * @return The type or <code>null</code>, if the id is invalid.
     */
    public static TasksFacetType getById(String id) {
        if (Strings.isEmpty(id)) {
            return null;
        }

        return typesById.get(id);
    }

}
