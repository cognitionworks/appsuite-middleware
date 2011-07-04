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

package com.openexchange.file.storage.webdav;

import com.openexchange.exceptions.LocalizableStrings;

/**
 * {@link WebDAVOXExceptionMessages} - Exception messages for {@link WebDAVOXException} that needs to be translated.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class WebDAVOXExceptionMessages implements LocalizableStrings {

    // An error occurred: %1$s
    public static final String UNEXPECTED_ERROR_MSG = "An error occurred: %1$s";

    // A HTTP error occurred: %1$s
    public static final String HTTP_ERROR_MSG = "A HTTP error occurred: %1$s";

    // A DAV error occurred: %1$s
    public static final String DAV_ERROR_MSG = "A DAV error occurred: %1$s";

    // The resource is not a directory: %1$s
    public static final String NOT_A_FOLDER_MSG = "The resource is not a directory: %1$s";

    // Invalid date property: %1$s
    public static final String INVALID_DATE_PROPERTY_MSG = "Invalid date property: %1$s";

    // Invalid property "%1$s". Should be "%2$s", but is not.
    public static final String INVALID_PROPERTY_MSG = "Invalid property \"%1$s\". Should be \"%2$s\", but is not.";

    // Directory "%1$s" must not be deleted.
    public static final String DELETE_DENIED_MSG = "Directory \"%1$s\" must not be deleted.";

    // Directory "%1$s" must not be updated.
    public static final String UPDATE_DENIED_MSG = "Directory \"%1$s\" must not be updated.";

    // Invalid or missing credentials to access WebDAV server "%1$s".
    public static final String INVALID_CREDS_MSG = "Invalid or missing credentials to access WebDAV server \"%1$s\".";

    // The resource is not a file: %1$s
    public static final String NOT_A_FILE_MSG = "The resource is not a file: %1$s";

    // Versioning not supported by WebDAV.
    public static final String VERSIONING_NOT_SUPPORTED_MSG = "Versioning not supported by WebDAV.";

    // Missing file name.
    public static final String MISSING_FILE_NAME_MSG = "Missing file name.";

    /**
     * Initializes a new {@link WebDAVOXExceptionMessages}.
     */
    private WebDAVOXExceptionMessages() {
        super();
    }

}
