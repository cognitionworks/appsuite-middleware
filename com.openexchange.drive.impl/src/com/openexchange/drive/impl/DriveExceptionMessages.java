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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.drive.impl;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link DriveExceptionMessages}
 *
 * Translatable messages for {@link DriveExceptionCodes}.
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public final class DriveExceptionMessages implements LocalizableStrings {

    public static final String PATH_NOT_FOUND_MSG = "The path \"%1$s\" was not found. Please retry the synchronization.";
    public static final String FILE_NOT_FOUND_MSG = "The file \"%1$s\" was not found at \"%2$s\". Please retry the synchronization.";
    public static final String INVALID_FILE_OFFSET_MSG = "Unable to access the file at the requested position. Please retry the synchronization.";
    public static final String FILEVERSION_NOT_FOUND_MSG = "The file version \"%1$s\" was not found at \"%3$s\". Please retry the synchronization.";
    public static final String UPLOADED_FILE_CHECKSUM_ERROR_MSG = "Integrity checks failed for uploaded file \"%2$s\". Please try again.";
    public static final String DIRECTORYVERSION_NOT_FOUND_MSG = "The directory version \"%1$s\" was not found. Please retry the synchronization.";
    public static final String NO_DELETE_FILE_PERMISSION_MSG = "You are not allowed to delete the file \"%1$s\" at \"%2$s\".";
    public static final String NO_CREATE_FILE_PERMISSION_MSG = "You are not allowed to create files at \"%1$s\".";
    public static final String NO_MODIFY_FILE_PERMISSION_MSG = "You are not allowed to modify the file \"%1$s\" at \"%2$s\".";
    public static final String NO_DELETE_DIRECTORY_PERMISSION_MSG = "You are not allowed to delete the directory \"%1$s\".";
    public static final String NO_CREATE_DIRECTORY_PERMISSION_MSG = "You are not allowed to create directories at \"%1$s\".";
    public static final String QUOTA_REACHED_MSG = "The allowed quota is reached.";
    public static final String INVALID_PATH_MSG = "The path \"%1$s\" is invalid.";
    public static final String INVALID_FILENAME_MSG = "The file name \"%1$s\" is invalid.";
    public static final String IGNORED_FILENAME_MSG = "The file name \"%1$s\" is ignored.";
    public static final String CONFLICTING_PATH_MSG = "The path \"%1$s\" is conflicting.";
    public static final String INVALID_FILEVERSION_MSG = "The file version \"%1$s\" is invalid.";
    public static final String INVALID_DIRECTORYVERSION_MSG = "The directory version \"%1$s\" is invalid.";
    public static final String LONG_POLLING_NOT_AVAILABLE_MSG = "Push updates are not available.";
    public static final String SERVER_BUSY_MSG = "The server is too busy at the moment - please try again later.";
    public static final String IGNORED_PATH_MSG = "The path \"%1$s\" is ignored.";
    public static final String CONFLICTING_FILENAME_MSG = "The file name \"%1$s\" is conflicting.";
    public static final String REPEATED_SYNC_PROBLEMS_MSG = "The directory \"%1$s\" caused repeated synchronization problems.";
    public static final String CLIENT_OUTDATED_MSG = "The client application you're using is outdated and no longer supported - please upgrade to a newer version.";
    public static final String CLIENT_VERSION_UPDATE_AVAILABLE_MSG = "A newer version of your client application is available for download.";
    public static final String INVALID_PATTERN_MSG = "The pattern \"%1$s\" is invalid. Please use a valid pattern and try again.";
    public static final String LEVEL_CONFLICTING_FILENAME_MSG = "The file name \"%1$s\" is conflicting with an equally named directory at \"%2$s\".";
    public static final String LEVEL_CONFLICTING_PATH_MSG = "The path \"%1$s\" is conflicting with an equally named file at \"%2$s\".";

    /**
     * Prevent instantiation.
     */
    private DriveExceptionMessages() {
        super();
    }
}
