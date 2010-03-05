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

package com.openexchange.folderstorage.internal;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import com.openexchange.folderstorage.FolderServiceDecorator;
import com.openexchange.folderstorage.FolderType;
import com.openexchange.folderstorage.StorageParameters;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link StorageParametersImpl} - Implementation of {@link StorageParameters}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class StorageParametersImpl implements StorageParameters {

    private final ServerSession session;

    private FolderServiceDecorator decorator;

    private final User user;

    private final int userId;

    private final Context context;

    private final int contextId;

    private final ConcurrentMap<FolderType, ConcurrentMap<String, Object>> parameters;

    private Date timeStamp;

    private Thread usingThread;

    private StackTraceElement[] trace;

    /**
     * Initializes a new {@link List} from given session.
     * 
     * @param session The session
     */
    public StorageParametersImpl(final ServerSession session) {
        super();
        this.session = session;
        user = session.getUser();
        userId = user.getId();
        context = session.getContext();
        contextId = context.getContextId();
        parameters = new ConcurrentHashMap<FolderType, ConcurrentMap<String, Object>>();
    }

    /**
     * Initializes a new {@link List} from given user-context-pair.
     * 
     * @param user The user
     * @param context The context
     */
    public StorageParametersImpl(final User user, final Context context) {
        super();
        session = null;
        this.user = user;
        userId = user.getId();
        this.context = context;
        contextId = context.getContextId();
        parameters = new ConcurrentHashMap<FolderType, ConcurrentMap<String, Object>>();
    };

    private ConcurrentMap<String, Object> getFolderTypeMap(final FolderType folderType, final boolean createIfAbsent) {
        ConcurrentMap<String, Object> m = parameters.get(folderType);
        if (createIfAbsent && null == m) {
            final ConcurrentMap<String, Object> inst = new ConcurrentHashMap<String, Object>();
            m = parameters.putIfAbsent(folderType, inst);
            if (null == m) {
                m = inst;
            }
        }
        return m;
    }

    public Context getContext() {
        return context;
    }

    public <P> P getParameter(final FolderType folderType, final String name) {
        final Map<String, Object> m = getFolderTypeMap(folderType, false);
        if (null == m) {
            return null;
        }
        try {
            @SuppressWarnings("unchecked") final P retval = (P) m.get(name);
            return retval;
        } catch (final ClassCastException e) {
            /*
             * Wrong type
             */
            return null;
        }
    }

    public Session getSession() {
        return session;
    }

    public User getUser() {
        return user;
    }

    public void putParameter(final FolderType folderType, final String name, final Object value) {
        if (null == value) {
            final Map<String, Object> m = getFolderTypeMap(folderType, false);
            if (null == m) {
                return;
            }
            m.remove(name);
        } else {
            final Map<String, Object> m = getFolderTypeMap(folderType, true);
            m.put(name, value);
        }
    }

    public boolean putParameterIfAbsent(final FolderType folderType, final String name, final Object value) {
        if (null == value) {
            throw new IllegalArgumentException("value is null");
        }
        final ConcurrentMap<String, Object> m = getFolderTypeMap(folderType, true);
        return (null == m.putIfAbsent(name, value));
    }

    public Date getTimeStamp() {
        return null == timeStamp ? null : new Date(timeStamp.getTime());
    }

    public void setTimeStamp(final Date timeStamp) {
        this.timeStamp = null == timeStamp ? null : new Date(timeStamp.getTime());
    }

    public FolderServiceDecorator getDecorator() {
        return decorator;
    }

    public void setDecorator(final FolderServiceDecorator decorator) {
        this.decorator = decorator;
    }

    public int getContextId() {
        return contextId;
    }

    public int getUserId() {
        return userId;
    }

    public void markCommitted() {
        usingThread = Thread.currentThread();
        /*
         * This is faster than Thread.getStackTrace() since a native method is used to fill thread's stack trace
         */
        trace = new Throwable().getStackTrace();
    }

    /**
     * Gets the trace of the thread that lastly obtained this access.
     * <p>
     * This is useful to detect certain threads which uses an access for a long time
     * 
     * @return the trace of the thread that lastly obtained this access
     */
    public String getCommittedTrace() {
        final StringBuilder sBuilder = new StringBuilder(512);
        sBuilder.append(toString());
        sBuilder.append("\nStorage parameters committed at: ").append('\n');
        /*
         * Start at index 2
         */
        final String delim = "\tat ";
        for (int i = 2; i < trace.length; i++) {
            sBuilder.append(delim).append(trace[i]).append('\n');
        }
        if ((null != usingThread) && usingThread.isAlive()) {
            sBuilder.append("Currently using thread: ").append(usingThread.getName()).append('\n');
            /*
             * Only possibility to get the current working position of a thread.
             */
            final StackTraceElement[] trace = usingThread.getStackTrace();
            sBuilder.append(delim).append(trace[0]);
            final String prefix = "\n\tat ";
            for (int i = 1; i < trace.length; i++) {
                sBuilder.append(prefix).append(trace[i]);
            }
        }
        return sBuilder.toString();
    }

}
