package com.openexchange.group;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Error codes for the ldap exception.
 */
public enum GroupExceptionCodes implements OXExceptionCode {
    /**
     * A database connection Cannot be obtained.
     */
    NO_CONNECTION("Cannot get database connection.", Category.CATEGORY_SERVICE_DOWN, 1),
    /**
     * SQL Problem: "%1$s".
     */
    SQL_ERROR("SQL Problem: \"%1$s\"", Category.CATEGORY_ERROR, 2),
    /**
     * No group given.
     */
    NULL("No group given.", Category.CATEGORY_ERROR, 3),
    /**
     * The mandatory field %1$s is not defined.
     */
    MANDATORY_MISSING("The mandatory field %1$s is not defined.", Category.CATEGORY_USER_INPUT, 4),
    /**
     * The simple name contains this not allowed characters: "%s".
     */
    NOT_ALLOWED_SIMPLE_NAME("The simple name contains this not allowed " + "characters: \"%1$s\".", Category.CATEGORY_USER_INPUT, 5),
    /**
     * Another group with same identifier name exists: %1$d.
     */
    DUPLICATE("Another group with same identifier name exists: %1$d.", Category.CATEGORY_USER_INPUT, 6),
    /**
     * Group contains a not existing member %1$d.
     */
    NOT_EXISTING_MEMBER("Group contains a not existing member %1$d.", Category.CATEGORY_USER_INPUT, 7),
    /**
     * Group contains invalid data: "%1$s".
     */
    INVALID_DATA("Group contains invalid data: \"%1$s\".", Category.CATEGORY_USER_INPUT, 8),
    /**
     * You are not allowed to create groups.
     */
    NO_CREATE_PERMISSION("You are not allowed to create groups.", Category.CATEGORY_PERMISSION_DENIED, 9),
    /**
     * Edit Conflict. Your change cannot be completed because somebody else has made a conflicting change to the same item. Please
     * refresh or synchronize and try again.
     */
    MODIFIED("Edit Conflict. Your change cannot be completed because somebody else has made a conflicting change to the same item. " + "Please refresh or synchronize and try again.", Category.CATEGORY_CONFLICT, 10),
    /**
     * You are not allowed to change groups.
     */
    NO_MODIFY_PERMISSION("You are not allowed to change groups.", Category.CATEGORY_PERMISSION_DENIED, 11),
    /**
     * You are not allowed to delete groups.
     */
    NO_DELETE_PERMISSION("You are not allowed to delete groups.", Category.CATEGORY_PERMISSION_DENIED, 12),
    /**
     * Group "%1$s" can not be deleted.
     */
    NO_GROUP_DELETE("Group \"%1$s\" can not be deleted.", Category.CATEGORY_USER_INPUT, 13),
    /**
     * Group "%1$s" can not be changed.
     */
    NO_GROUP_UPDATE("Group \"%1$s\" can not be changed.", Category.CATEGORY_USER_INPUT, 14);

    /**
     * Message of the exception.
     */
    private final String message;

    /**
     * Category of the exception.
     */
    private final Category category;

    /**
     * Detail number of the exception.
     */
    private final int detailNumber;

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private GroupExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    public String getPrefix() {
        return "GRP";
    }

    public Category getCategory() {
        return category;
    }

    public int getNumber() {
        return detailNumber;
    }

    public String getMessage() {
        return message;
    }
    
    public boolean equals(final OXException e) {
        return getPrefix().equals(e.getPrefix()) && e.getCode() == getNumber();
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