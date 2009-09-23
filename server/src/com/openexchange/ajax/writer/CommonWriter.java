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

package com.openexchange.ajax.writer;

import java.util.TimeZone;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import com.openexchange.ajax.fields.CommonFields;
import com.openexchange.groupware.container.CommonObject;

/**
 * {@link CommonWriter} - Writer for common fields
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */

public class CommonWriter extends DataWriter {

	/**
	 * Initializes a new {@link CommonWriter}
	 * 
	 * @param tz
	 *            The user time zone
	 * @param jsonwriter
	 *            The JSON writer to write to
	 */
	protected CommonWriter(final TimeZone tz, final JSONWriter jsonwriter) {
		super(tz, jsonwriter);
	}

	/**
	 * Writes common field from given {@link CommonObject} instance to specified
	 * {@link JSONObject}
	 * 
	 * @param commonObj
	 *            The common object
	 * @param jsonObj
	 *            The JSON object
	 * @throws JSONException
	 *             If a JSON error occurs
	 */
	public void writeCommonFields(final CommonObject commonObj, final JSONObject jsonObj) throws JSONException {
		if (commonObj.containsObjectID()) {
			writeParameter(CommonFields.ID, commonObj.getObjectID(), jsonObj);
		}

		if (commonObj.containsCreatedBy()) {
			writeParameter(CommonFields.CREATED_BY, commonObj.getCreatedBy(), jsonObj);
		}

		if (commonObj.containsCreationDate()) {
			writeParameter(CommonFields.CREATION_DATE, commonObj.getCreationDate(), timeZone, jsonObj);
		}

		if (commonObj.containsModifiedBy()) {
			writeParameter(CommonFields.MODIFIED_BY, commonObj.getModifiedBy(), jsonObj);
		}

		if (commonObj.containsLastModified()) {
			writeParameter(CommonFields.LAST_MODIFIED, commonObj.getLastModified(), timeZone, jsonObj);
			writeParameter(CommonFields.LAST_MODIFIED_UTC, commonObj.getLastModified(), jsonObj);
		}

		if (commonObj.containsParentFolderID()) {
			writeParameter(CommonFields.FOLDER_ID, commonObj.getParentFolderID(), jsonObj);
		}

		if (commonObj.containsCategories()) {
			writeParameter(CommonFields.CATEGORIES, commonObj.getCategories(), jsonObj);
		}

		if (commonObj.containsLabel()) {
			writeParameter(CommonFields.COLORLABEL, commonObj.getLabel(), jsonObj);
		}

		if (commonObj.containsPrivateFlag()) {
			writeParameter(CommonFields.PRIVATE_FLAG, commonObj.getPrivateFlag(), jsonObj);
		}

		if (commonObj.containsNumberOfAttachments()) {
			writeParameter(CommonFields.NUMBER_OF_ATTACHMENTS, commonObj.getNumberOfAttachments(), jsonObj);
		}
	}
}
