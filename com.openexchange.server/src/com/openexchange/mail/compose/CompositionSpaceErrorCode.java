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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.mail.compose;

import com.openexchange.exception.Category;
import com.openexchange.exception.DisplayableOXExceptionCode;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;

/**
 *
 * {@link CompositionSpaceErrorCode}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since 7.10.1
 */
public enum CompositionSpaceErrorCode implements DisplayableOXExceptionCode {

    /**
     * An error occurred: %1$s
     */
    ERROR("An error occurred: %1$s", null, CATEGORY_ERROR, 1),
    /**
     * An SQL error occurred: %1$s
     */
    SQL_ERROR("An SQL error occurred: %1$s", null, CATEGORY_ERROR, 2),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", null, CATEGORY_ERROR, 3),
    /**
     * Unable to access the file storage
     */
    FILESTORE_DOWN("Unable to access the file storage", null, CATEGORY_ERROR, 4),
    /**
     * Found no suitable attachment storage
     */
    NO_ATTACHMENT_STORAGE("Found no suitable attachment storage", null, CATEGORY_ERROR, 5),
    /**
     * Found no such resource in attachment storage for identifier: %1$s
     */
    NO_SUCH_ATTACHMENT_RESOURCE("Found no such resource in attachment storage for identifier: %1$s", null, CATEGORY_ERROR, 6),
    /**
     * Found no such composition space for identifier: %1$s
     */
    NO_SUCH_COMPOSITION_SPACE("Found no such composition space for identifier: %1$s", null, CATEGORY_ERROR, 7),
    /**
     * The operation cannot be performed because composed message is not a reply.
     */
    NO_REPLY_FOR("The operation cannot be performed because composed message is not a reply.", CompositionSpaceExceptionMessages.NO_REPLY_FOR_MSG, CATEGORY_USER_INPUT, 8),
    /**
     * Found no such attachment %1$s in composition space %2$s
     */
    NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE("Found no such attachment %1$s in composition space %2$s", CompositionSpaceExceptionMessages.NO_SUCH_ATTACHMENT_IN_COMPOSITION_SPACE_MSG, CATEGORY_USER_INPUT, 9),
    /**
     * Concurrent Update Exception.
     */
    CONCURRENT_UPDATE("Concurrent Update Exception.", null, CATEGORY_ERROR, 10),
    /**
     * Maximum number of composition spaces is reached: %1$s
     */
    MAX_NUMBER_OF_COMPOSITION_SPACE_REACHED("Maximum number of composition spaces is reached: %1$s", CompositionSpaceExceptionMessages.MAX_NUMBER_OF_COMPOSITION_SPACE_REACHED_MSG, CATEGORY_USER_INPUT, 11),
    /**
     * Found no suitable key storage
     */
    NO_KEY_STORAGE("Found no suitable key storage", null, CATEGORY_ERROR, 12),
    /**
     * Found no suitable key for composition space %1$s
     */
    MISSING_KEY("Found no suitable key for composition space %1$s", CompositionSpaceExceptionMessages.MISSING_KEY_MSG, CATEGORY_TRY_AGAIN, 13),
    /**
     * Composition space could not be opened
     */
    OPEN_FAILED("Composition space could not be opened", null, CATEGORY_ERROR, 14),
    /**
     * The entered subject is too long. Please use a shorter one.
     */
    SUBJECT_TOO_LONG("The entered subject is too long. Please use a shorter one.", CompositionSpaceExceptionMessages.SUBJECT_TOO_LONG_MSG, CATEGORY_USER_INPUT, 15),
    /**
     * The entered "From" address is too long. Please use a shorter one.
     */
    FROM_TOO_LONG("The entered \"From\" address is too long.", CompositionSpaceExceptionMessages.FROM_TOO_LONG_MSG, CATEGORY_USER_INPUT, 16),
    /**
     * The entered "Sender" address is too long. Please use a shorter one.
     */
    SENDER_TOO_LONG("The entered \"Sender\" address is too long.", CompositionSpaceExceptionMessages.SENDER_TOO_LONG_MSG, CATEGORY_USER_INPUT, 17),
    /**
     * The entered "To" addresses are too long. Please use a shorter one.
     */
    TO_TOO_LONG("The entered \"To\" addresses are too long.", CompositionSpaceExceptionMessages.TO_TOO_LONG_MSG, CATEGORY_USER_INPUT, 18),
    /**
     * The entered "Cc" addresses are too long. Please use a shorter one.
     */
    CC_TOO_LONG("The entered \"Cc\" addresses are too long.", CompositionSpaceExceptionMessages.CC_TOO_LONG_MSG, CATEGORY_USER_INPUT, 19),
    /**
     * The entered "Bcc" addresses are too long. Please use a shorter one.
     */
    BCC_TOO_LONG("The entered \"Bcc\" addresses are too long.", CompositionSpaceExceptionMessages.BCC_TOO_LONG_MSG, CATEGORY_USER_INPUT, 20),
    /**
     * The entered "Reply-To" address is too long. Please use a shorter one.
     */
    REPLY_TO_TOO_LONG("The entered \"Reply-To\" address is too long.", CompositionSpaceExceptionMessages.REPLY_TO_TOO_LONG_MSG, CATEGORY_USER_INPUT, 21),

    ;

    private static final String PREFIX = "MSGCS";

    private String message;
    private String displayMessage;
    private Category category;
    private int number;

    private CompositionSpaceErrorCode(String message, String displayMessage, Category category, int number) {
        this.message = message;
        this.displayMessage = displayMessage != null ? displayMessage : OXExceptionStrings.MESSAGE;
        this.category = category;
        this.number = number;
    }

    @Override
    public int getNumber() {
        return number;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String getDisplayMessage() {
        return displayMessage;
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
    public boolean equals(OXException e) {
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
