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

package com.openexchange.service.messaging;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link MessagingServiceExceptionCodes} - Enumeration of all {@link OXException}s.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.22
 */
public enum MessagingServiceExceptionCodes implements OXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR(MessagingServiceExceptionMessages.UNEXPECTED_ERROR_MSG, CATEGORY_ERROR, 1),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR(MessagingServiceExceptionMessages.IO_ERROR_MSG, CATEGORY_ERROR, 2),
    /**
     * The IP address of host %1$s could not be determined.
     */
    UNKNOWN_HOST(MessagingServiceExceptionMessages.UNKNOWN_HOST_MSG, CATEGORY_ERROR, 3),
    /**
     *
     */
    INT_TOO_BIG(MessagingServiceExceptionMessages.INT_TOO_BIG_MSG, CATEGORY_ERROR, 4),
    /**
     * Missing previous truncated message package(s).
     */
    MISSING_PREV_PACKAGE(MessagingServiceExceptionMessages.MISSING_PREV_PACKAGE_MSG, CATEGORY_ERROR, 5),
    /**
     * Conflicting truncated message package(s).
     */
    CONFLICTING_TRUNCATED_PACKAGES(MessagingServiceExceptionMessages.CONFLICTING_TRUNCATED_PACKAGES_MSG, CATEGORY_ERROR, 6),
    /**
     * Missing or wrong magic bytes: %1$s
     */
    BROKEN_MAGIC_BYTES(MessagingServiceExceptionMessages.BROKEN_MAGIC_BYTES_MSG, CATEGORY_ERROR, 7),
    /**
     * Unknown prefix code: %1$s
     */
    UNKNOWN_PREFIX_CODE(MessagingServiceExceptionMessages.UNKNOWN_PREFIX_CODE_MSG, CATEGORY_ERROR, 8),
    /**
     * Invalid message package
     */
    INVALID_MSG_PACKAGE(MessagingServiceExceptionMessages.INVALID_MSG_PACKAGE_MSG, CATEGORY_ERROR, 9),
    /**
     * Unparseable string.
     */
    UNPARSEABLE_STRING(MessagingServiceExceptionMessages.UNPARSEABLE_STRING_MSG, CATEGORY_ERROR, 10),
    /**
     * Invalid quoted-printable encoding.
     */
    INVALID_QUOTED_PRINTABLE(MessagingServiceExceptionMessages.INVALID_QUOTED_PRINTABLE_MSG, CATEGORY_ERROR, 11),
    /**
     * Messaging server socket could not be bound to port %1$d. Probably another process is already listening on this port.
     */
    BIND_ERROR(MessagingServiceExceptionMessages.BIND_ERROR_MSG, CATEGORY_ERROR, 12),

    ;

    private final Category category;

    private final int detailNumber;

    private final String message;

    private MessagingServiceExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
    }

    @Override
    public String getPrefix() {
        return "MESSAGING-SERVICE";
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

    public String getHelp() {
        return null;
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
}
