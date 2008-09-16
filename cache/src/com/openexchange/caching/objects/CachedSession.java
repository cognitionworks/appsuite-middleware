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

package com.openexchange.caching.objects;

import java.io.Serializable;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * {@link CachedSession} - Holding cache-able informations of a session.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class CachedSession implements Serializable {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -8392075484894016494L;

	private final String loginName;

	private final String password;

	private final int contextId;

	private final int userId;

	private final String sessionId;

	private final String secret;

	private final String randomToken;

	private final String localIp;

	private final String login;

	private final Map<String, Serializable> parameters;

	private boolean markedAsRemoved;

	/**
	 * Initializes a new {@link CachedSession}
	 * 
	 * @param userId
	 *            The user ID
	 * @param loginName
	 *            The login name
	 * @param password
	 *            The password
	 * @param contextId
	 *            The context ID
	 * @param sessionId
	 *            The session ID
	 * @param secret
	 *            The secret (cookie identifier)
	 * @param randomToken
	 *            The random token
	 * @param localIp
	 *            The local IP
	 * @param login
	 *            The full login; e.g. <code>test@foo</code>
	 * @param parameters
	 *            The session's parameters
	 */
	public CachedSession(final int userId, final String loginName, final String password, final int contextId,
			final String sessionId, final String secret, final String randomToken, final String localIp,
			final String login, final Map<String, Object> parameters) {
		super();
		this.userId = userId;
		this.loginName = loginName;
		this.password = password;
		this.sessionId = sessionId;
		this.secret = secret;
		this.randomToken = randomToken;
		this.localIp = localIp;
		this.contextId = contextId;
		this.login = login;
		final Map<String, Serializable> tmpparameters = new HashMap<String, Serializable>(parameters.size());
		/*
		 * Only fill with serializable objects
		 */
		for (final Iterator<Map.Entry<String, Object>> iterator = parameters.entrySet().iterator(); iterator.hasNext();) {
			final Map.Entry<String, Object> entry = iterator.next();
			final Object value = entry.getValue();
			if (Serializable.class.isInstance(value) && value.getClass().getName().startsWith("java.")) {
				tmpparameters.put(entry.getKey(), (Serializable) entry.getValue());
			}
		}
		this.parameters = Collections.unmodifiableMap(tmpparameters);
	}

	/**
	 * Gets the loginName
	 * 
	 * @return the loginName
	 */
	public String getLoginName() {
		return loginName;
	}

	/**
	 * Gets the full login incl. context information; e.g <code>test@foo</code>
	 * 
	 * @return The full login
	 */
	public String getLogin() {
		return login;
	}

	/**
	 * Gets the password
	 * 
	 * @return the password
	 */
	public String getPassword() {
		return password;
	}

	/**
	 * Gets the contextId
	 * 
	 * @return the contextId
	 */
	public int getContextId() {
		return contextId;
	}

	/**
	 * Gets the userId
	 * 
	 * @return the userId
	 */
	public int getUserId() {
		return userId;
	}

	/**
	 * Gets the sessionId
	 * 
	 * @return the sessionId
	 */
	public String getSessionId() {
		return sessionId;
	}

	/**
	 * Gets the secret
	 * 
	 * @return the secret
	 */
	public String getSecret() {
		return secret;
	}

	/**
	 * Gets the randomToken
	 * 
	 * @return the randomToken
	 */
	public String getRandomToken() {
		return randomToken;
	}

	/**
	 * Gets the localIp
	 * 
	 * @return the localIp
	 */
	public String getLocalIp() {
		return localIp;
	}

	/**
	 * Gets the parameters
	 * 
	 * @return the parameters
	 */
	public Map<String, Serializable> getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		final StringBuilder sb = new StringBuilder(256);
		sb.append(super.toString()).append("\nuserId=").append(userId);
		sb.append(" loginName=").append(loginName);
		sb.append(" password=").append(password);
		sb.append(" sessionId=").append(sessionId);
		sb.append(" secret=").append(secret);
		sb.append(" randomToken=").append(randomToken);
		sb.append(" localIp=").append(localIp);
		sb.append(" contextId=").append(contextId);
		sb.append("\nparameters=").append(parameters.toString());
		return sb.toString();
	}

	/**
	 * Checks if this cached session has been marked as removed
	 * 
	 * @return <code>true</code> if this cached session has been marked as
	 *         removed; otherwise <code>false</code>
	 */
	public boolean isMarkedAsRemoved() {
		return markedAsRemoved;
	}

	/**
	 * Sets this cached session's marked-as-removed flag
	 * 
	 * @param markedAsRemoved
	 *            <code>true</code> to mark this cached session as removed;
	 *            otherwise <code>false</code>
	 */
	public void setMarkedAsRemoved(final boolean markedAsRemoved) {
		this.markedAsRemoved = markedAsRemoved;
	}

}
