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

package com.openexchange.tools.ajp13;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * A request handler pool to hold pre-initialized instances of <code>AJPv13RequestHandler</code>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 *
 */
public class AJPv13RequestHandlerPool {
	
	private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AJPv13RequestHandlerPool.class);

	private static final int REQUEST_HANDLER_POOL_SIZE = AJPv13Config.getAJPRequestHandlerPoolSize();

	private static final BlockingQueue<AJPv13RequestHandler> REQUEST_HANDLER_POOL = new ArrayBlockingQueue<AJPv13RequestHandler>(
			REQUEST_HANDLER_POOL_SIZE);

	private static boolean initialized;
	
	private AJPv13RequestHandlerPool() {
		super();
	}

	public static boolean isInitialized() {
		return initialized;
	}

	public static void initPool() {
		if (!initialized) {
			for (int i = 0; i < REQUEST_HANDLER_POOL_SIZE; i++) {
				REQUEST_HANDLER_POOL.add(new AJPv13RequestHandler());
			}
			initialized = true;
			LOG.info("AJPv13-RequestHandler-Pool initialized with " + REQUEST_HANDLER_POOL_SIZE);
		}
	}
	
	public static void resetPool() {
		REQUEST_HANDLER_POOL.clear();
		initialized = false;
	}

	/**
	 * Fetches an existing instance from pool or creates & returns a new one.
	 * The given conenction is then assigned to the request handler instance.
	 */
	public static AJPv13RequestHandler getRequestHandler(final AJPv13Connection ajpCon) {
		AJPv13RequestHandler reqHandler = REQUEST_HANDLER_POOL.poll();
		if (reqHandler == null) {
			reqHandler = new AJPv13RequestHandler();
		}
		reqHandler.setAJPConnection(ajpCon);
		return reqHandler;
	}

	/**
	 * Puts back the given request handler instance into pool if space
	 * available. Otherwise it's going to be discarded.
	 */
	public static boolean putRequestHandler(final AJPv13RequestHandler reqHandler) {
		if (reqHandler == null) {
			return false;
		}
		return REQUEST_HANDLER_POOL.offer(reqHandler);
	}

}
