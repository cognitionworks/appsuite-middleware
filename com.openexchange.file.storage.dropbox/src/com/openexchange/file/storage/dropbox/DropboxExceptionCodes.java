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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.file.storage.dropbox;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 * {@link DropboxExceptionCodes} - Enumeration of all errors.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum DropboxExceptionCodes implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR("An error occurred: %1$s", Category.CATEGORY_ERROR, 1),
    /**
     * A Dropbox error occurred: %1$s
     */
    DROPBOX_ERROR(DropboxExceptionMessages.DROPBOX_ERROR_MSG, Category.CATEGORY_ERROR, 2),
    /**
     * Invalid Dropbox URL: %1$s
     */
    INVALID_DROPBOX_URL("Invalid Dropbox URL: %1$s", DropboxExceptionMessages.INVALID_DROPBOX_URL_MSG, Category.CATEGORY_ERROR, 3),
    /**
     * Dropbox URL does not denote a directory: %1$s
     */
    NOT_A_FOLDER("Dropbox URL does not denote a directory: %1$s", DropboxExceptionMessages.NOT_A_FOLDER_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * The Dropbox resource does not exist: %1$s
     */
    NOT_FOUND("he Dropbox resource does not exist: %1$s", DropboxExceptionMessages.NOT_FOUND_MSG, Category.CATEGORY_ERROR, 5),
    /**
     * Update denied for Dropbox resource: %1$s
     */
    UPDATE_DENIED("Update denied for Dropbox resource: %1$s", DropboxExceptionMessages.UPDATE_DENIED_MSG, Category.CATEGORY_ERROR, 6),
    /**
     * Delete denied for Dropbox resource: %1$s
     */
    DELETE_DENIED("Delete denied for Dropbox resource: %1$s", DropboxExceptionMessages.DELETE_DENIED_MSG, Category.CATEGORY_ERROR, 7),
    /**
     * Dropbox URL does not denote a file: %1$s
     */
    NOT_A_FILE("Dropbox URL does not denote a file: %1$s", DropboxExceptionMessages.NOT_A_FILE_MSG, Category.CATEGORY_ERROR, 8),
    /**
     * Missing file name.
     */
    MISSING_FILE_NAME("Missing file name.", DropboxExceptionMessages.MISSING_FILE_NAME_MSG, Category.CATEGORY_ERROR, 12),
    /**
     * Versioning not supported by Dropbox file storage.
     */
    VERSIONING_NOT_SUPPORTED("Versioning not supported by Dropbox file storage.", DropboxExceptionMessages.VERSIONING_NOT_SUPPORTED_MSG, Category.CATEGORY_ERROR, 13),
    /**
     * Missing configuration for account "%1$s".
     */
    MISSING_CONFIG("Missing configuration for account \"%1$s\".", DropboxExceptionMessages.MISSING_CONFIG_MSG, Category.CATEGORY_CONFIGURATION, 14),
    /**
     * Bad or expired access token. Need to re-authenticate user.
     */
    UNLINKED_ERROR("Bad or expired access token. Need to re-authenticate user.", DropboxExceptionMessages.UNLINKED_ERROR_MSG, Category.CATEGORY_CONFIGURATION, 15),

    ;

    private final Category category;

    private final int detailNumber;

    private final String message;
    
    private final String displayMessage;

    private DropboxExceptionCodes(final String message, final Category category, final int detailNumber) {
        this(message, null, category, detailNumber);
    }
    
    private DropboxExceptionCodes(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        this.detailNumber = detailNumber;
        this.displayMessage = (displayMessage == null) ? OXExceptionStrings.MESSAGE : displayMessage;
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getNumber() {
        return detailNumber;
    }

    @Override
    public String getPrefix() {
        return "DROPBOX";
    }

    @Override
    public boolean equals(final OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @return The newly created {@link OXException} instance
     */
    public OXException create() {
        return OXExceptionFactory.getInstance().create(this, new Object[0]);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Object... args) {
        return OXExceptionFactory.getInstance().create(this, (Throwable) null, args);
    }

    /**
     * Creates a new {@link OXException} instance pre-filled with this code's attributes.
     *
     * @param cause The optional initial cause
     * @param args The message arguments in case of printf-style message
     * @return The newly created {@link OXException} instance
     */
    public OXException create(final Throwable cause, final Object... args) {
        return OXExceptionFactory.getInstance().create(this, cause, args);
    }

    /* (non-Javadoc)
     * @see com.openexchange.exception.DisplayableOXExceptionCode#getDisplayMessage()
     */
    @Override
    public String getDisplayMessage() {
        return displayMessage;
    }
}
