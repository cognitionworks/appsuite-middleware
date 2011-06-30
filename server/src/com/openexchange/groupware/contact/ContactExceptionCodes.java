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

package com.openexchange.groupware.contact;

import static com.openexchange.groupware.contact.ContactExceptionMessages.*;
import com.openexchange.exceptions.OXErrorMessage;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.contact.internal.ContactExceptionFactory;

/**
 * {@link ContactExceptionCodes}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public enum ContactExceptionCodes implements OXErrorMessage {

    /** Found a user contact outside global address book in folder %1$d in context %2$d. */
    USER_OUTSIDE_GLOBAL(USER_OUTSIDE_GLOBAL_MSG, Category.CODE_ERROR, 1),
    /** Invalid E-Mail address: '%s'. Please correct the E-Mail address. */
    INVALID_EMAIL(INVALID_EMAIL_MSG, Category.USER_INPUT, 100),
    /** Unable to scale this contact image. Either the file type is not supported or the image is too large. Your mime type is %1$s and your image size is %2$d. The max. allowed image size is %3$d. */
    IMAGE_SCALE_PROBLEM(IMAGE_SCALE_PROBLEM_MSG, Category.USER_INPUT, 101),
    /** You are not allowed to store this contact in a non-contact folder: folder id %1$d in context %2$d with user %3$d */
    NON_CONTACT_FOLDER(NON_CONTACT_FOLDER_MSG, Category.PERMISSION, 103),
    /** You do not have permission to access objects in this folder %1$d in context %2$d with user %3$d */
    NO_ACCESS_PERMISSION(NO_ACCESS_PERMISSION_MSG, Category.PERMISSION, 104),
    /** Got a -1 ID from IDGenerator */
    ID_GENERATION_FAILED(ID_GENERATION_FAILED_MSG, Category.CODE_ERROR, 107),
    /** Unable to scale image down. */
    IMAGE_DOWNSCALE_FAILED(IMAGE_DOWNSCALE_FAILED_MSG, Category.CODE_ERROR, 108),
    /** Invalid SQL Query. */
    SQL_PROBLEM(SQL_PROBLEM_MSG, Category.CODE_ERROR, 109),
    /** Invalid SQL Query: %s */
    AGGREGATING_CONTACTS_NOT_ENABLED(FEATURE_DISABLED_MSG, Category.SUBSYSTEM_OR_SERVICE_DOWN, 110),
    /** You do not have permission to create objects in this folder %1$d in context %2$d with user %3$d */
    NO_CREATE_PERMISSION(NO_CREATE_PERMISSION_MSG, Category.PERMISSION, 112),
    /** Unable to synchronize the old contact with the new changes: Context %1$d Object %2$d */
    LOAD_OLD_CONTACT_FAILED(LOAD_OLD_CONTACT_FAILED_MSG, Category.CODE_ERROR, 116),
    /** You are not allowed to mark this contact as private contact: Context %1$d Object %2$d */
    MARK_PRIVATE_NOT_ALLOWED(MARK_PRIVATE_NOT_ALLOWED_MSG, Category.PERMISSION, 118),
    /** Edit Conflict. Your change cannot be completed because somebody else has made a conflicting change to the same item. Please refresh or synchronize and try again. */
    OBJECT_HAS_CHANGED(OBJECT_HAS_CHANGED_MSG, Category.CONCURRENT_MODIFICATION, 119),
    /** An error occurred: Object id is -1 */
    NEGATIVE_OBJECT_ID(NEGATIVE_OBJECT_ID_MSG, Category.CODE_ERROR, 121),
    /** No changes found. No update requiered. Context %1$d Object %2$d */
    NO_CHANGES(NO_CHANGES_MSG, Category.USER_INPUT, 122),
    /** Contact %1$d not found in context %2$d. */
    CONTACT_NOT_FOUND(CONTACT_NOT_FOUND_MSG, Category.CODE_ERROR, 125),
    /** Unable to save contact image. The image appears to be broken. */
    IMAGE_BROKEN(IMAGE_BROKEN_MSG, Category.USER_INPUT, 136),
    /** Unable to trigger object Events: Context %1$d Folder %2$d */
    TRIGGERING_EVENT_FAILED(TRIGGERING_EVENT_FAILED_MSG, Category.CODE_ERROR, 146),
    /** Unable to pick up a connection from the DBPool */
    INIT_CONNECTION_FROM_DBPOOL(INIT_CONNECTION_FROM_DBPOOL_MSG, Category.SUBSYSTEM_OR_SERVICE_DOWN, 151),
    /** Import failed. Some data entered exceed the database field limit. Please shorten following entries: %1$s Character Limit: %2$s Sent %3$s */
    DATA_TRUNCATION(DATA_TRUNCATION_MSG, Category.USER_INPUT, 154),
    /** The image you tried to attach is not a valid picture. It may be broken or is not a valid file. */
    NOT_VALID_IMAGE(NOT_VALID_IMAGE_MSG, Category.TRY_AGAIN, 158),
    /** Your first name is mandatory. Please enter it. */
    FIRST_NAME_MANDATORY(FIRST_NAME_MANDATORY_MSG, Category.USER_INPUT, 164),
    /** Unable to move this contact because it is marked as private: Context %1$d Object %2$d */
    NO_PRIVATE_MOVE(NO_PRIVATE_MOVE_MSG, Category.PERMISSION, 165),
    /** Your display name is mandatory. Please enter it. */
    DISPLAY_NAME_MANDATORY(DISPLAY_NAME_IN_USE_MSG, Category.USER_INPUT, 166),
    /** The name you entered is not available. Choose another display name. Context %1$d Object %2$d */
    DISPLAY_NAME_IN_USE(DISPLAY_NAME_IN_USE_MSG, Category.TRY_AGAIN, 167),
    /** Bad character in field %2$s. Error: %1$s */
    BAD_CHARACTER(BAD_CHARACTER_MSG, Category.USER_INPUT, 168),
    /** You do not have permission to delete objects from folder %1$d in context %2$d with user %3$d */
    NO_DELETE_PERMISSION(NO_DELETE_PERMISSION_MSG, Category.PERMISSION, 169),
    /** Mime type is not defined. */
    MIME_TYPE_NOT_DEFINED(MIME_TYPE_NOT_DEFINED_MSG, Category.USER_INPUT, 170),
    /** A contact with private flag cannot be stored in a public folder. Folder: %1$d context %2$d user %3$d */
    PFLAG_IN_PUBLIC_FOLDER(PFLAG_IN_PUBLIC_FOLDER_MSG, Category.USER_INPUT, 171),
    /** Image size too large. Image size: %1$d. Max. size: %2$d. */
    IMAGE_TOO_LARGE(IMAGE_TOO_LARGE_MSG, Category.USER_INPUT, 172),
    /** Primary email address in system contact must not be edited: Context %1$d Object %2$d User %3$d */
    NO_PRIMARY_EMAIL_EDIT(NO_PRIMARY_EMAIL_EDIT_MSG, Category.PERMISSION, 173),
    /** The contact %1$d is not located in folder %2$s (%3$d) */
    NOT_IN_FOLDER(NOT_IN_FOLDER_MSG, Category.PERMISSION, 174),
    /** Your last name is mandatory. Please enter it. */
    LAST_NAME_MANDATORY(LAST_NAME_MANDATORY_MSG, Category.USER_INPUT, 175),
    /** You are not allowed to modify contact %1$d in context %2$d. */
    NO_CHANGE_PERMISSION(NO_CHANGE_PERMISSION_MSG, Category.PERMISSION, 176),
    /** Unable to load objects. Context %1$d User %2$d */
    LOAD_OBJECT_FAILED(LOAD_OBJECT_FAILED_MSG, Category.CODE_ERROR, 252),
    /** User contacts can not be deleted. */
    NO_USER_CONTACT_DELETE(NO_USER_CONTACT_DELETE_MSG, Category.PERMISSION, 260),
    /** Number of documents attached to this contact is below zero. You can not remove any more attachments. */
    TOO_FEW_ATTACHMENTS(TOO_FEW_ATTACHMENTS_MSG, Category.USER_INPUT, 400),
    /** Need at least a ContactObject and a value to set %s */
    TOO_FEW_ATTRIBUTES(TOO_FEW_ATTRIBUTES_MSG, Category.CODE_ERROR, 500),
    /** Could not convert given string %1$s to a date. */
    DATE_CONVERSION_FAILED(DATE_CONVERSION_FAILED_MSG, Category.CODE_ERROR, 600),
    /** Could not convert given object %1$s to a date when setting %2$s. */
    CONV_OBJ_2_DATE_FAILED(CONV_OBJ_2_DATE_FAILED_MSG, Category.CODE_ERROR, 700),
    /** Need at least a ContactObject to get the value of %s */
    CONTACT_OBJECT_MISSING(CONTACT_OBJECT_MISSING_MSG, Category.CODE_ERROR, 800),
    /** In order to accomplish the search, %1$d or more characters are required. */
    TOO_FEW_SEARCH_CHARS(TOO_FEW_SEARCH_CHARS_MSG, Category.USER_INPUT, 1000);

    private String message;
    private Category category;
    private int number;

    private ContactExceptionCodes(String message, Category category, int number) {
        this.message = message;
        this.category = category;
        this.number = number;
    }

    public int getDetailNumber() {
        return number;
    }

    public String getMessage() {
        return message;
    }

    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    public Category getCategory() {
        return category;
    }

    public ContactException create(Object... args) {
        return ContactExceptionFactory.getInstance().create(this, args);
    }

    public ContactException create(Throwable cause, Object... args) {
        return ContactExceptionFactory.getInstance().create(this, cause, args);
    }
}
