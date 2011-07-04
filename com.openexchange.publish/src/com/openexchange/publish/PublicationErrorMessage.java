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

package com.openexchange.publish;

import static com.openexchange.publish.PublicationExceptionMessages.ACCESS_DENIED_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.ID_GIVEN_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.NO_LOADER_FOUND_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.PARSE_EXCEPTION_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.PUBLICATION_NOT_FOUND_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.SQL_EXCEPTION_MSG;
import static com.openexchange.publish.PublicationExceptionMessages.UNIQUENESS_CONSTRAINT_VIOLATION;
import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.AbstractOXException.Category;

/**
 * {@link PublicationErrorMessage}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public enum PublicationErrorMessage implements OXErrorMessage {

    /**
     * A SQL Error occurred.
     */
    SQLException(Category.CODE_ERROR, 1, "Please try again later.", SQL_EXCEPTION_MSG),
    /**
     * A parsing error occurred: %1$s.
     */
    ParseException(Category.CODE_ERROR, 2, "Provide well-formed HTML.", PARSE_EXCEPTION_MSG),
    /**
     * Could not load publications of type %1$s
     */
    NoLoaderFound(Category.CODE_ERROR, 3, "Only publish supported document types", NO_LOADER_FOUND_MSG),
    /**
     * Can not save a given ID.
     */
    IDGiven(Category.CODE_ERROR, 4, "Do not set a ID when saving a publication", ID_GIVEN_MSG),
    /**
     * Cannot find the publication site (according ID and Context).
     */
    PublicationNotFound(Category.USER_INPUT, 5, "Provide a valid id.", PUBLICATION_NOT_FOUND_MSG),
    UniquenessConstraintViolation(Category.USER_INPUT, 6, "Choose a different value", UNIQUENESS_CONSTRAINT_VIOLATION),
    AccessDenied(Category.PERMISSION, 7, "Try again when you have the correct permissions", ACCESS_DENIED_MSG);

    private Category category;
    private int errorCode;
    private String help;
    private String message;
    
    public static final PublicationExceptionFactory EXCEPTIONS = new PublicationExceptionFactory();
    
    private PublicationErrorMessage(final Category category, final int errorCode, final String help, final String message) {
        this.category = category;
        this.errorCode = errorCode;
        this.help = help;
        this.message = message;
    }
    
    public Category getCategory() {
        return category;
    }

    public int getDetailNumber() {
        return errorCode;
    }

    public String getHelp() {
        return help;
    }

    public String getMessage() {
        return message;
    }
    
    public PublicationException create(final Throwable cause, final Object...args) {
        return EXCEPTIONS.create(this,cause, args);
    }
    
    public PublicationException create(final Object...args) {
        return EXCEPTIONS.create(this,args);
    }
}
