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

package com.openexchange.sessiond.impl;

import java.util.UUID;

import com.openexchange.sessiond.exception.SessiondException;

/**
 * {@link UUIDSessionIdGenerator} - The session ID generator based on
 * {@link UUID#randomUUID()}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class UUIDSessionIdGenerator extends SessionIdGenerator {

	/**
	 * Initializes a new {@link UUIDSessionIdGenerator}
	 */
	public UUIDSessionIdGenerator() {
		super();
	}

	@Override
	public String createSessionId(final String userId, final String data) throws SessiondException {
		return randomUUID();
	}

	@Override
	public String createSecretId(final String userId, final String data) throws SessiondException {
		return randomUUID();
	}

	@Override
	public String createRandomId() throws SessiondException {
		return randomUUID();
	}

	/**
	 * Generates a UUID using {@link UUID#randomUUID()} and removes all dashes;
	 * e.g.:<br>
	 * <i>a5aa65cb-6c7e-4089-9ce2-b107d21b9d15</i> would be
	 * <i>a5aa65cb6c7e40899ce2b107d21b9d15</i>
	 * 
	 * @return A UUID string
	 */
	private static String randomUUID() {
		final StringBuilder s = new StringBuilder(36).append(UUID.randomUUID());
		s.deleteCharAt(23);
		s.deleteCharAt(18);
		s.deleteCharAt(13);
		s.deleteCharAt(8);
		return s.toString();
	}
}
