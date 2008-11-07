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

package com.openexchange.mail;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.openexchange.mail.api.MailAccess;
import com.openexchange.mail.api.MailConfig;
import com.openexchange.server.ServerTimer;

/**
 * {@link MailAccessWatcher} - Keeps track of connected instances of
 * {@link MailAccess} and allows a forced close if connection time exceeds
 * allowed time
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class MailAccessWatcher {

	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory
			.getLog(MailAccessWatcher.class);

	private static final ConcurrentMap<MailAccess<?, ?>, Long> mailAccesses = new ConcurrentHashMap<MailAccess<?, ?>, Long>();

	private static final AtomicBoolean initialized = new AtomicBoolean();

	private static WatcherTask watcherTask;

	/**
	 * Initializes and starts mail connection watcher if not done, yet
	 */
	static void init() {
		if (!initialized.get()) {
			synchronized (initialized) {
				if (initialized.get()) {
					return;
				}
				if (MailConfig.isWatcherEnabled()) {
					/*
					 * Start task
					 */
					watcherTask = new WatcherTask(mailAccesses, LOG);
					ServerTimer.getTimer().schedule(watcherTask, 1000, MailConfig.getWatcherFrequency());
					initialized.set(true);
					if (LOG.isInfoEnabled()) {
						LOG.info("Mail connection watcher successfully established and ready for tracing");
					}
				}
			}
		}
	}

	/**
	 * Stops mail connection watcher if currently running
	 */
	static void stop() {
		if (initialized.get()) {
			synchronized (initialized) {
				if (!initialized.get()) {
					return;
				}
				if (MailConfig.isWatcherEnabled()) {
					watcherTask.cancel();
					ServerTimer.getTimer().purge();
					mailAccesses.clear();
					initialized.set(false);
					if (LOG.isInfoEnabled()) {
						LOG.info("Mail connection watcher successfully stopped");
					}
				}
			}
		}
	}

	/**
	 * Prevent instantiation
	 */
	private MailAccessWatcher() {
		super();
	}

	/**
	 * Adds specified mail access to this watcher's tracing if not already added
	 * before. If already present its timestamp is updated.
	 * <p>
	 * Watcher is established if not running, yet
	 * 
	 * @param mailAccess
	 *            The mail access to add
	 */
	public static void addMailAccess(final MailAccess<?, ?> mailAccess) {
		if (!initialized.get()) {
			LOG.error("Mail connection watcher is not running. Aborting addMailAccess()");
			return;
		}
		/*
		 * Insert or update timestamp
		 */
		mailAccesses.put(mailAccess, Long.valueOf(System.currentTimeMillis()));
	}

	/**
	 * Removes specified mail access from this watcher's tracing
	 * 
	 * @param mailAccess
	 *            The mail access to remove
	 */
	public static void removeMailAccess(final MailAccess<?, ?> mailAccess) {
		if (!initialized.get()) {
			LOG.error("Mail connection watcher is not running. Aborting removeMailAccess()");
			return;
		}
		mailAccesses.remove(mailAccess);
	}

	private static final String INFO_PREFIX = "UNCLOSED MAIL CONNECTION AFTER #N#msec:\n";

	private static final String INFO_PREFIX2 = "CLOSING MAIL CONNECTION BY WATCHER:\n";

	private static final String INFO_PREFIX3 = "\n\tDONE";

	private static class WatcherTask extends TimerTask {

		private final ConcurrentMap<MailAccess<?, ?>, Long> mailAccessMap;

		private final org.apache.commons.logging.Log logger;

		public WatcherTask(final ConcurrentMap<MailAccess<?, ?>, Long> mailAccesses,
				final org.apache.commons.logging.Log logger) {
			super();
			this.mailAccessMap = mailAccesses;
			this.logger = logger;
		}

		@Override
		public void run() {
			try {
				final StringBuilder sb = new StringBuilder(512);
				final List<MailAccess<?, ?>> exceededCons = new ArrayList<MailAccess<?, ?>>();
				for (final Iterator<Entry<MailAccess<?, ?>, Long>> iter = mailAccessMap.entrySet().iterator(); iter
						.hasNext();) {
					final Entry<MailAccess<?, ?>, Long> e = iter.next();
					if (e.getKey().isConnectedUnsafe()) {
						if ((System.currentTimeMillis() - e.getValue().longValue()) > MailConfig.getWatcherTime()) {
							sb.setLength(0);
							logger.info(sb.append(
									INFO_PREFIX.replaceFirst("#N#", String.valueOf(MailConfig.getWatcherTime())))
									.append(e.getKey().getTrace()).toString());
							exceededCons.add(e.getKey());
						}
					} else {
						/*
						 * Remove closed connection from watcher
						 */
						iter.remove();
					}
				}
				if (!exceededCons.isEmpty()) {
					/*
					 * Remove/Close exceeded accesses
					 */
					final int n = exceededCons.size();
					for (int i = 0; i < n; i++) {
						final MailAccess<?, ?> mailAccess = exceededCons.get(i);
						try {
							if (MailConfig.isWatcherShallClose()) {
								sb.setLength(0);
								sb.append(INFO_PREFIX2).append(mailAccess.toString());
								mailAccess.close(false);
								sb.append(INFO_PREFIX3);
								logger.info(sb.toString());
							}
						} finally {
							mailAccessMap.remove(mailAccess);
						}
					}
				}
			} catch (final Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}

}
