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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.calendar;

import static com.openexchange.java.Autoboxing.I;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.calendar.storage.ParticipantStorage;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.configuration.ServerConfig.Property;
import com.openexchange.database.provider.SimpleDBProvider;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.AttachmentException;
import com.openexchange.groupware.attach.Attachments;
import com.openexchange.groupware.calendar.CalendarConfig;
import com.openexchange.groupware.calendar.CalendarDataObject;
import com.openexchange.groupware.calendar.CalendarFolderObject;
import com.openexchange.groupware.calendar.OXCalendarExceptionCodes;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.UserParticipant;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.impl.DBPool;
import com.openexchange.tools.StringCollection;
import com.openexchange.tools.iterator.SearchIterator;

/**
 * CachedCalendarIterator
 * @author <a href="mailto:martin.kauss@open-xchange.org">Martin Kauss</a>
 */

public class CachedCalendarIterator implements SearchIterator<CalendarDataObject> {

    private static final Log LOG = com.openexchange.exception.Log.valueOf(LogFactory.getLog(CachedCalendarIterator.class));

	private final List<OXException> warnings;
    private final List<CalendarDataObject> list;
    private final SearchIterator<CalendarDataObject> non_cached_iterator;
    private final boolean cache;
    private int counter;
    private boolean closed;
    private final Context c;
    private int[][] oids;
    private int cc;
    private boolean oxonfe;
    protected final int uid;

    public static boolean CACHED_ITERATOR_FAST_FETCH = true;
    public static int MAX_PRE_FETCH = 20;
    private int pre_fetch;

    private CalendarFolderObject visibleFolders;

    public CachedCalendarIterator(final CalendarFolderObject visibleFolders, final SearchIterator<CalendarDataObject> non_cached_iterator, final Context c, final int uid) throws OXException {
    	this.warnings =  new ArrayList<OXException>(2);
    	list = new ArrayList<CalendarDataObject>(16);
    	this.visibleFolders = visibleFolders;
        this.non_cached_iterator = non_cached_iterator;
        this.c = c;
        this.uid = uid;
        cache = ServerConfig.getBoolean(Property.PrefetchEnabled);
        CACHED_ITERATOR_FAST_FETCH = CalendarConfig.isCACHED_ITERATOR_FAST_FETCH();
        MAX_PRE_FETCH = CalendarConfig.getMAX_PRE_FETCH();
        if (cache) {
            fillCachedResultSet();
        }
    }

    public CachedCalendarIterator(final SearchIterator<CalendarDataObject> non_cached_iterator, final Context c, final int uid) throws OXException {
        this(null, non_cached_iterator, c, uid);
    }

    public CachedCalendarIterator(final SearchIterator<CalendarDataObject> non_cached_iterator, final Context c, final int uid, final int[][] oids) throws OXException {
    	this.warnings =  new ArrayList<OXException>(2);
    	if (non_cached_iterator.hasWarnings()) {
    		warnings.addAll(Arrays.asList(non_cached_iterator.getWarnings()));
    	}
    	list = new ArrayList<CalendarDataObject>(16);
        this.non_cached_iterator = non_cached_iterator;
        this.c = c;
        this.uid = uid;
        this.oids = oids;
        cache = ServerConfig.getBoolean(Property.PrefetchEnabled);
        CACHED_ITERATOR_FAST_FETCH = CalendarConfig.isCACHED_ITERATOR_FAST_FETCH();
        MAX_PRE_FETCH = CalendarConfig.getMAX_PRE_FETCH();
        if (cache) {
            fillCachedResultSet();
        }
    }

    public boolean hasNext() throws OXException {
        if (!cache) {
            return non_cached_iterator.hasNext();
        }
        if (!list.isEmpty() && counter < list.size()) {
            return true;
        }
        if (oids != null && cc < oids.length) {
		    oxonfe = true;
		    return true;
		}
        return false;
    }

    public CalendarDataObject next() throws OXException {
        if (!oxonfe) {
            if (!cache) {
                if (oids != null) {
                    final CalendarDataObject cdao = non_cached_iterator.next();
                    cc++;
                    return cdao;
                }
				return non_cached_iterator.next();
            }
            if (hasNext()) {
                if (oids != null) {
                    final CalendarDataObject cdao = getPreFilledResult();
                    cc++;
                    return cdao;
                }
				return getPreFilledResult();
            }
        } else {
            cc++;
        }
        return null;
    }

    public final void close() throws OXException {
        if (closed) {
            return;
        }
        closed = true;
        non_cached_iterator.close();
    }

    public int size() {
        return non_cached_iterator.size();
    }

    public void addWarning(final OXException warning) {
		warnings.add(warning);
	}

	public OXException[] getWarnings() {
		return warnings.isEmpty() ? null : warnings.toArray(new OXException[warnings.size()]);
	}

	public boolean hasWarnings() {
		return !warnings.isEmpty();
	}

    private final void fillCachedResultSet() throws OXException {
        try {
            while (non_cached_iterator.hasNext()) {
                list.add(non_cached_iterator.next());
            }
        } finally {
            close();
        }
    }

    private final CalendarDataObject getPreFilledResult() throws OXException {
        Connection readcon = null;
        CalendarDataObject cdao;
        try {
            cdao = list.get(counter++);
            if (CACHED_ITERATOR_FAST_FETCH && pre_fetch < counter) {
			    if ((cdao.fillParticipants() || cdao.fillUserParticipants() || cdao.fillFolderID() || cdao.fillConfirmations())) {
			        readcon = DBPool.pickup(c);
			    }
			
			    int mn = MAX_PRE_FETCH;
			    if (mn+pre_fetch > list.size()) {
			        mn = list.size()%MAX_PRE_FETCH;
			    }
			    final int arr[] = new int[mn];
			
			    for (int a = 0; a < mn; a++) {
			        final CalendarDataObject temp = list.get(pre_fetch++);
			        arr[a] = temp.getObjectID();
			    }
			
			    final String sqlin = StringCollection.getSqlInString(arr);
			    if  (cdao.fillUserParticipants() || cdao.fillFolderID()) {
			        try {
			            new CalendarMySQL().getUserParticipantsSQLIn(visibleFolders, list, readcon, c.getContextId(), uid, sqlin);
			        } catch (final SQLException ex) {
			            throw OXCalendarExceptionCodes.UNEXPECTED_EXCEPTION.create(ex, I(202));
			        }
			    }
			
			    if (cdao.fillParticipants()) {
			        try {
			            new CalendarMySQL().getParticipantsSQLIn(list, readcon, cdao.getContextID(), sqlin);
			        } catch (final SQLException ex) {
			            throw OXCalendarExceptionCodes.UNEXPECTED_EXCEPTION.create(ex, I(203));
			        }
			    }
			    if (cdao.fillConfirmations()) {
			        ParticipantStorage.getInstance().selectExternal(c, readcon, list, arr);
			    }
			    if (cdao.isFillLastModifiedOfNewestAttachment()) {
			        setAttachmentLastModified(c, readcon, list, arr);
			    }
			}

            if (cdao.fillFolderID()) {
                cdao.setGlobalFolderID(cdao.getEffectiveFolderId());
            }

            // Security check for bug 10836
            if (CACHED_ITERATOR_FAST_FETCH && (cdao.getFolderType() == FolderObject.PRIVATE || cdao.getFolderType() == FolderObject.SHARED)) {
            	final UserParticipant up[] = cdao.getUsers();
            	boolean found = false;
            	for (int a = 0; a < up.length; a++) {
            		if (up[a].getPersonalFolderId() == cdao.getParentFolderID()) {
            			found = true;
            			break;
            		}
            	}
            	if (!found) {
            		throw OXCalendarExceptionCodes.LOAD_PERMISSION_EXCEPTION_5.create(I(cdao.getObjectID()));
            	}
            }

        } finally {
            if (readcon != null) {
                DBPool.push(c, readcon);
            }
            close();
        }
        return cdao;
    }

    private static void setAttachmentLastModified(final Context ctx, final Connection readcon, final List<CalendarDataObject> list, final int[] arr) {
        final AttachmentBase attachmentBase = Attachments.getInstance(new SimpleDBProvider(readcon, null));
        final Map<Integer, Date> dates;
        try {
            dates = attachmentBase.getNewestCreationDates(ctx, Types.APPOINTMENT, arr);
        } catch (final AttachmentException e) {
            LOG.error(e.getMessage(), e);
            return;
        }
        for (final CalendarDataObject cdao : list) {
            final Date date = dates.get(I(cdao.getObjectID()));
            if (null != date) {
                cdao.setLastModifiedOfNewestAttachment(date);
            }
        }
    }
}
