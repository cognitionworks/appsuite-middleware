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

package com.openexchange.groupware.update.internal;

import java.util.Date;
import java.util.UUID;
import com.openexchange.groupware.update.ExecutedTask;

/**
 * {@link ExecutedTaskImpl}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ExecutedTaskImpl implements ExecutedTask {

    private final String taskName;
    private final boolean successful;
    private final Date lastModified;
    private final UUID uuid;

    /**
     * Initializes a new {@link ExecutedTaskImpl}.
     * 
     * @param lastModified when the task has been executed lately
     * @param successful if the task was executed successfully
     * @param taskName full class name of the update task
     * @param uuid the {@link UUID} of the update task
     */
    public ExecutedTaskImpl(String taskName, boolean successful, Date lastModified, UUID uuid) {
        super();
        this.taskName = taskName;
        this.successful = successful;
        this.lastModified = lastModified;
        this.uuid = uuid;
    }

    @Override
    public Date getLastModified() {
        return lastModified;
    }

    @Override
    public String getTaskName() {
        return taskName;
    }

    @Override
    public boolean isSuccessful() {
        return successful;
    }

    @Override
    public UUID getUUID() {
        return uuid;
    }

    @Override
    public int compareTo(final ExecutedTask o) {
        if (null == o) {
            return 1;
        }
        final Date thisLastModified = lastModified;
        final Date otherLastModified = o.getLastModified();
        if (null == thisLastModified) {
            if (null == otherLastModified) {
                return 0; // Both null
            }
            return -1; // Other is not null
        }
        if (null == otherLastModified) {
            return 1; // Other is null
        }
        return thisLastModified.compareTo(otherLastModified);
    }
}
