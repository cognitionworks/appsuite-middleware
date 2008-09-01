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

package com.openexchange.groupware.upload.impl;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;

/**
 * {@link UploadException} - Indicates an error during an upload.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public class UploadException extends AbstractOXException {

	private static final long serialVersionUID = 8590042770250274015L;

	private final String action;

	public static enum UploadCode {
		/**
		 * File upload failed: %1$s
		 */
		UPLOAD_FAILED("File upload failed: %1$s", Category.INTERNAL_ERROR, 1),
		/**
		 * Missing affiliation id
		 */
		MISSING_AFFILIATION_ID("Missing affiliation id", Category.CODE_ERROR, 2),
		/**
		 * Unknown action value: %1$s
		 */
		UNKNOWN_ACTION_VALUE("Unknown action value: %1$s", Category.CODE_ERROR, 3),
		/**
		 * Header "content-type" does not indicate multipart content
		 */
		NO_MULTIPART_CONTENT("Header \"content-type\" does not indicate multipart content", Category.CODE_ERROR, 4),
		/**
		 * Request rejected because its size (%1$d) exceeds the maximum
		 * configured size of %2$d
		 */
		MAX_UPLOAD_SIZE_EXCEEDED(
				"Request rejected because its size (%1$d) exceeds the maximum configured size of %2$d",
				Category.USER_INPUT, 5),
		/**
		 * Missing parameter %1$s
		 */
		MISSING_PARAM("Missing parameter %1$s", Category.CODE_ERROR, 6),
		/**
		 * Unknown module: %1$d
		 */
		UNKNOWN_MODULE("Unknown module: %1$d", Category.CODE_ERROR, 7),
		/**
		 * An uploaded file referenced by %1$s could not be found
		 */
		UPLOAD_FILE_NOT_FOUND("An uploaded file referenced by %1$s could not be found", Category.USER_INPUT, 8),
		/**
		 * Invalid action value: %1$s
		 */
		INVALID_ACTION_VALUE("Invalid action value: %1$s", Category.CODE_ERROR, 9),
		/**
		 * Upload file with id %1$s could not be found
		 */
		FILE_NOT_FOUND("Upload file with id %1$s could not be found", Category.CODE_ERROR, 10),
		/**
		 * Upload file's content type "%1$s" does not fit to given file filter
		 * "%2$s"
		 */
		INVALID_FILE_TYPE("Upload file's content type \"%1$s\" does not fit to given file filter \"%2$s\"",
				Category.CODE_ERROR, 11);

		private final String message;

		private final Category category;

		private final int detailNumber;

		private UploadCode(final String message, final Category category, final int detailNumber) {
			this.message = message;
			this.category = category;
			this.detailNumber = detailNumber;
		}

		public final Category getCategory() {
			return category;
		}

		public final int getNumber() {
			return detailNumber;
		}

		public final String getMessage() {
			return message;
		}
	}

	public UploadException(final AbstractOXException cause, final String action) {
		super(cause);
		this.action = action;
	}

	public UploadException(final UploadCode uploadCode, final String action, final Throwable cause) {
		super(EnumComponent.UPLOAD, uploadCode.category, uploadCode.detailNumber, uploadCode.message, cause);
		super.setMessageArgs(cause.getMessage());
		this.action = action;
	}

	public UploadException(final UploadCode uploadCode, final String action, final Object... messageArgs) {
		super(EnumComponent.UPLOAD, uploadCode.category, uploadCode.detailNumber, uploadCode.message, null);
		super.setMessageArgs(messageArgs);
		this.action = action;
	}

	public UploadException(final UploadCode uploadCode, final String action) {
		super(EnumComponent.UPLOAD, uploadCode.category, uploadCode.detailNumber, uploadCode.message, null);
		super.setMessageArgs(new Object[0]);
		this.action = action;
	}

	public String getAction() {
		return action;
	}

}
