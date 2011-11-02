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

package com.openexchange.chat;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Enumeration about all {@link OXException}s.
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public enum ChatExceptionCodes implements OXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    ERROR(ChatExceptionMessages.ERROR_MSG, Category.CATEGORY_ERROR, 1),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR(ChatExceptionMessages.IO_ERROR_MSG, Category.CATEGORY_ERROR, 2),
    /**
     * Method is not supported.
     */
    UNSUPPORTED_OPERATION(ChatExceptionMessages.UNSUPPORTED_OPERATION_MSG, Category.CATEGORY_ERROR, 3),
    /**
     * No account found with identifier "%1$s".
     */
    ACCOUNT_NOT_FOUND(ChatExceptionMessages.ACCOUNT_NOT_FOUND_MSG, Category.CATEGORY_ERROR, 4),
    /**
     * Invalid presence packet.
     */
    INVALID_PRESENCE_PACKET(ChatExceptionMessages.INVALID_PRESENCE_PACKET_MSG, Category.CATEGORY_ERROR, 5),
    /**
     * A chat with identifier "%1$s" already exists.
     */
    CHAT_ALREADY_EXISTS(ChatExceptionMessages.CHAT_ALREADY_EXISTS_MSG, Category.CATEGORY_ERROR, 6),
    /**
     * Chat member "%1$s" already exists in chat "%2$s".
     */
    CHAT_MEMBER_ALREADY_EXISTS(ChatExceptionMessages.CHAT_MEMBER_ALREADY_EXISTS_MSG, Category.CATEGORY_ERROR, 7),
    /**
     * No chat found with identifier "%1$s".
     */
    CHAT_NOT_FOUND(ChatExceptionMessages.CHAT_NOT_FOUND_MSG, Category.CATEGORY_ERROR, 8),
    /**
     * Unknown chat service: %1$s
     */
    UNKNOWN_CHAT_SERVICE(ChatExceptionMessages.UNKNOWN_CHAT_SERVICE_MSG, Category.CATEGORY_ERROR, 9),
    /**
     * No message found with identifier "%1$s" in chat "%2$s".
     */
    MESSAGE_NOT_FOUND(ChatExceptionMessages.MESSAGE_NOT_FOUND_MSG, Category.CATEGORY_ERROR, 10),
    /**
     * No member found with identifier "%1$s".
     */
    MEMBER_NOT_FOUND(ChatExceptionMessages.MEMBER_NOT_FOUND_MSG, Category.CATEGORY_ERROR, 11),

    ;

    private static final String PREFIX = "CHAT";

    private final Category category;

    private final int number;

    private final String message;


    private ChatExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        number = detailNumber;
        this.category = category;
    }

    @Override
    public String getPrefix() {
        return PREFIX;
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
        return number;
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
