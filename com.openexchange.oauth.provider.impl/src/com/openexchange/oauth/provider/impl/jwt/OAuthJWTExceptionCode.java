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

package com.openexchange.oauth.provider.impl.jwt;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * {@link OAuthJWTExceptionCode} - defines exception codes for {@link OAuthJWTExceptionCode}
 *
 * @author <a href="mailto:sebastian.lutz@open-xchange.com">Sebastian Lutz</a>
 * @since v7.10.5
 */
public enum OAuthJWTExceptionCode implements OXExceptionCode {

    /**
     * The JWT is rejected because the issuer is invalid: '%1$s'
     */
    IVALID_ISSUER("JWT validation failed because of invalid issuer: '%1$s'", Category.CATEGORY_ERROR, 1),

    /**
     * Unable to load client name from JWT: '%1$s'"
     */
    UNALBLE_TO_LOAD_CLIENT("Unable to load client name from JWT: '%1$s'", Category.CATEGORY_ERROR, 2),

    /**
     * Unable to load valid scope from JWT claims: '%1$s'"
     */
    UNABLE_TO_LOAD_VALID_SCOPE("Unable to load valid scope from claim: '%1$s'", Category.CATEGORY_ERROR, 3),

    /**
     * JWT validation failed because of internal errors: '%1$s'
     */
    JWT_VALIDATON_FAILED("JWT validation failed because of internal errors: '%1$s'", Category.CATEGORY_ERROR, 4),

    /**
     * Unable to parse JWT claim: '%1$s'
     */
    UNABLE_TO_PARSE_CLAIM("Unable to parse claim: '%1$s'", Category.CATEGORY_ERROR, 5);

    private final String message;
    private final String displayMessage;
    private final int detailNumber;
    private final Category category;

    /**
     * Initializes a new {@link OAuthJWTExceptionCode}.
     * 
     * @param message
     * @param category
     * @param detailNumber
     */
    private OAuthJWTExceptionCode(final String message, final Category category, final int detailNumber) {
        this(message, null, category, detailNumber);
    }

    /**
     * Initializes a new {@link OAuthJWTExceptionCode}.
     * 
     * @param message
     * @param displayMessage
     * @param category
     * @param detailNumber
     */
    private OAuthJWTExceptionCode(final String message, final String displayMessage, final Category category, final int detailNumber) {
        this.message = message;
        this.displayMessage = displayMessage;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    @Override
    public boolean equals(OXException e) {
        return OXExceptionFactory.getInstance().equals(this, e);
    }

    @Override
    public int getNumber() {
        return this.detailNumber;
    }

    @Override
    public Category getCategory() {
        return this.category;
    }

    @Override
    public String getPrefix() {
        return "OAUTH_JWT";
    }

    @Override
    public String getMessage() {
        return this.message;
    }

    public String getDisplaymessage() {
        return this.displayMessage;
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
