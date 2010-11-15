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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.groupware.dataRetrieval;

import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.dataRetrieval.exception.DataRetrievalExceptionFactory;
import com.openexchange.groupware.AbstractOXException.Category;

/**
 * {@link DataRetrievalExceptionCodes} - Enumeration of all {@link DataRetrievalException}s.
 * 
 * @author francisco.laguna@open-xchange.com
 */
public enum DataRetrievalExceptionCodes implements OXErrorMessage {

    /**
     * An error occurred: %1$s
     */
    UNEXPECTED_ERROR(DataRetrievalExceptionMessages.UNEXPECTED_ERROR_MSG, Category.CODE_ERROR, 1),
    UNKNOWN_DATA_PROVIDER(DataRetrievalExceptionMessages.UNKNOWN_DATA_PROVIDER_MSG, Category.CODE_ERROR, 2),
    MISSING_PARAMETER(DataRetrievalExceptionMessages.MISSING_PARAMETER_MSG, Category.CODE_ERROR, 3);
    ;

    private final Category category;

    private final int detailNumber;

    private final String message;

    private DataRetrievalExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
    }

    public Category getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

    public int getDetailNumber() {
        return detailNumber;
    }

    public String getHelp() {
        return null;
    }

    private static final Object[] EMPTY = new Object[0];

    /**
     * Creates a new file storage exception of this error type with no message arguments.
     * 
     * @return A new twitter exception
     */
    public DataRetrievalException create() {
        return DataRetrievalExceptionFactory.getInstance().create(this, EMPTY);
    }

    /**
     * Creates a new file storage exception of this error type with specified message arguments.
     * 
     * @param messageArgs The message arguments
     * @return A new twitter exception
     */
    public DataRetrievalException create(final Object... messageArgs) {
        return DataRetrievalExceptionFactory.getInstance().create(this, messageArgs);
    }

    /**
     * Creates a new file storage exception of this error type with specified cause and message arguments.
     * 
     * @param cause The cause
     * @param messageArgs The message arguments
     * @return A new twitter exception
     */
    public DataRetrievalException create(final Throwable cause, final Object... messageArgs) {
        return DataRetrievalExceptionFactory.getInstance().create(this, cause, messageArgs);
    }
}
