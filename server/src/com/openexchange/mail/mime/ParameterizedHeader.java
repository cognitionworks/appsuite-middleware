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

package com.openexchange.mail.mime;

import java.io.Serializable;
import java.util.Iterator;

/**
 * {@link ParameterizedHeader} - Super class for headers which can hold a
 * parameter list such as <code>Content-Type</code>.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public abstract class ParameterizedHeader implements Serializable {

	/**
	 * Serial version UID
	 */
	private static final long serialVersionUID = -1094716342843794294L;

	protected ParameterList parameterList;

	/**
	 * Initializes a new {@link ParameterizedHeader}
	 */
	protected ParameterizedHeader() {
		super();
	}

	/**
	 * Adds specified value to given parameter name. If existing, the parameter
	 * is treated as a contiguous parameter according to RFC2231.
	 * 
	 * @param key
	 *            The parameter name
	 * @param value
	 *            The parameter value to add
	 */
	public void addParameter(final String key, final String value) {
		parameterList.addParameter(key, value);
	}

	/**
	 * Sets the given parameter. Existing value is overwritten.
	 * 
	 * @param key
	 *            The parameter name
	 * @param value
	 *            The parameter value
	 */
	public void setParameter(final String key, final String value) {
		parameterList.setParameter(key, value);
	}

	/**
	 * Gets specified parameter's value
	 * 
	 * @param key
	 *            The parameter name
	 * @return The parameter's value or <code>null</code> if not existing
	 */
	public String getParameter(final String key) {
		return parameterList.getParameter(key);
	}

	/**
	 * Removes specified parameter and returns its value
	 * 
	 * @param key
	 *            The parameter name
	 * @return The parameter's value or <code>null</code> if not existing
	 */
	public String removeParameter(final String key) {
		return parameterList.removeParameter(key);
	}

	/**
	 * Checks if parameter is present
	 * 
	 * @param key
	 *            the parameter name
	 * @return <code>true</code> if parameter is present; otherwise
	 *         <code>false</code>
	 */
	public boolean containsParameter(final String key) {
		return parameterList.containsParameter(key);
	}

	/**
	 * Gets all parameter names wrapped in an {@link Iterator}
	 * 
	 * @return All parameter names wrapped in an {@link Iterator}
	 */
	public Iterator<String> getParameterNames() {
		return parameterList.getParameterNames();
	}

	/**
	 * Prepares parameterized header's string representation:
	 * <ol>
	 * <li>Trims starting/ending whitespace characters</li>
	 * <li>Replaces all " = " with "="</li>
	 * <li>Removes ending ";" character</li>
	 * </ol>
	 * 
	 * @param paramHdrArg
	 *            The parameterized header string argument
	 * @return The prepared parameterized header's string.
	 */
	protected static final String prepareParameterizedHeader(final String paramHdrArg) {
		String paramHdr = paramHdrArg.trim().replaceAll("\\s*=\\s*", "=");
		if (paramHdr.length() > 0) {
			final int lastPos = paramHdr.length() - 1;
			if (paramHdr.charAt(lastPos) == ';') {
				paramHdr = paramHdr.substring(0, lastPos);
			}
		}
		return paramHdr;
	}
}
