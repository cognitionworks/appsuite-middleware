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



package com.openexchange.event.impl;

import java.util.Date;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.Contact;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.tasks.Task;
import com.openexchange.session.Session;

/**
 * EventObject
 *
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 */
public class EventObject {

	private final int module;
	private final int action;
	private final Object obj;
	private final Session session;
	private final Date creationDate;
	private boolean noDelay;

	public EventObject(final Appointment obj, final int action, final Session session) {
	    super();
		this.obj = obj;
        this.module = Types.APPOINTMENT;
        this.action = action;
        this.session = session;
        creationDate = new Date();
	}

	public EventObject(final Task obj, final int action, final Session session) {
        super();
		this.obj = obj;
        this.module = Types.TASK;
        this.action = action;
        this.session = session;
        creationDate = new Date();
	}

	public EventObject(final Contact obj, final int action, final Session session) {
        super();
		this.obj = obj;
        this.module = Types.CONTACT;
        this.action = action;
        this.session = session;
        creationDate = new Date();
	}

	public EventObject(final FolderObject obj, final int action, final Session session) {
        super();
		this.obj = obj;
        this.module = Types.FOLDER;
        this.action = action;
        this.session = session;
        creationDate = new Date();
	}

	public EventObject(final DocumentMetadata obj, final int action, final Session session) {
        super();
		this.obj = obj;
        this.module = Types.INFOSTORE;
        this.action = action;
        this.session = session;
        creationDate = new Date();
	}

	/**
     * Sets the <code>no-delay</code> flag
     *
     * @param noDelay The <code>no-delay</code> flag to set
     * @return This instance
     */
    public EventObject setNoDelay(boolean noDelay) {
        this.noDelay = noDelay;
        return this;
    }

    /**
     * Gets the <code>no-delay</code> flag
     *
     * @return The <code>no-delay</code> flag
     */
    public boolean isNoDelay() {
        return noDelay;
    }

	public int getModule() {
		return module;
	}

	public int getAction() {
		return action;
	}

	public Object getObject() {
		return obj;
	}

	public Session getSessionObject() {
		return session;
	}

	public Date getCreationDate() {
		return creationDate;
	}

	@Override
	public String toString() {
		return new StringBuilder()
		.append("MODULE=")
		.append(module)
		.append(",ACTION=")
		.append(action).toString();
	}
}
