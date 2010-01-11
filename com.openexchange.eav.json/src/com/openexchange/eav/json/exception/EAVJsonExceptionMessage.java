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

package com.openexchange.eav.json.exception;

import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.AbstractOXException.Category;

/**
 * @author <a href="mailto:martin.herfurth@open-xchange.org">Martin Herfurth</a>
 */
public enum EAVJsonExceptionMessage implements OXErrorMessage {
    JSONException(1, Category.CODE_ERROR, EAVJsonExceptionStrings.JSONException, null),
    InvalidType(2, Category.USER_INPUT, EAVJsonExceptionStrings.InvalidType, null),
    DifferentTypesInArray(3, Category.USER_INPUT, EAVJsonExceptionStrings.DifferentTypesInArray, null),
    InvalidTreeStructure(4, Category.USER_INPUT, EAVJsonExceptionStrings.InvalidTreeStructure, null),
    IOException(5, Category.INTERNAL_ERROR, EAVJsonExceptionStrings.IOException, null), 
    MissingParameter(6, Category.USER_INPUT, EAVJsonExceptionStrings.MISSING_PARAMETER, "Please supply all mandatory parameters"),
    InvalidLoadBinaries(7, Category.USER_INPUT, EAVJsonExceptionStrings.INVALID_LOAD_BINARIES, "Please supply a list of paths in the loadBinaries metadatum"),
    ConflictingParameters(8, Category.USER_INPUT, EAVJsonExceptionStrings.CONFLICTING_PARAMETERS, "Please supply only one of the parameters"), 
    UnknownAction(9, Category.USER_INPUT, EAVJsonExceptionStrings.UNKNOWN_ACTION, "Please check the spelling of the action parameter"), 
    BinariesInTreesMustBeBase64Encoded(10, Category.USER_INPUT, EAVJsonExceptionStrings.BINARIES_IN_TREE_MUST_BE_BASE64_ENCODED, ""), 
    UnknownBinaryEncoding(11, Category.USER_INPUT, EAVJsonExceptionStrings.UNKNOWN_BINARY_ENCODING, "Please specify one of 'raw' and 'base64'")
    ;

    private int detailNumber;
    private Category category;
    private String message;
    private String help;

    private EAVJsonExceptionMessage(int detailNumber, Category category, String message, String help) {
        this.detailNumber = detailNumber;
        this.category = category;
        this.message = message;
        this.help = help;
    }
    
    public Category getCategory() {
        return category;
    }

    public int getDetailNumber() {
        return detailNumber;
    }

    public String getHelp() {
        return help;
    }

    public String getMessage() {
        return message;
    }
    
    public static final EAVJsonExceptionFactory EXCEPTIONS = new EAVJsonExceptionFactory();
    
    public EAVJsonException create(final Throwable cause, final Object...args) {
        return EXCEPTIONS.create(this,cause, args);
    }
    
    public EAVJsonException create(final Object...args) {
        return EXCEPTIONS.create(this,args);
    }

}
