package com.openexchange.configjump;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Error codes for login exceptions.
 */
public enum ConfigJumpExceptionCode implements OXExceptionCode {
    /**
     * Unknown problem: "%s".
     */
    UNKNOWN("Unknown problem: \"%s\".", CATEGORY_ERROR, 1),
    /**
     * Too few (%d) login attributes.
     */
    MISSING_ATTRIBUTES("Too few (%d) login attributes.", CATEGORY_USER_INPUT, 2),
    /**
     * Problem while communicating with external authorization.
     */
    COMMUNICATION("Problem while communicating with external " + "authorization.", CATEGORY_SERVICE_DOWN, 3),
    /**
     * Instantiating the class failed.
     */
    INSTANTIATION_FAILED("Instantiating the class failed.", CATEGORY_ERROR, 4),
    /**
     * Class %1$s can not be found.
     */
    CLASS_NOT_FOUND("Class %1$s can not be found.", CATEGORY_CONFIGURATION, 5),
    /**
     * Missing property %1$s in system.properties.
     */
    MISSING_SETTING("Missing property %1$s in system.properties.", CATEGORY_CONFIGURATION, 6),
    /**
     * URL "%s" is malformed.
     */
    MALFORMED_URL("URL \"%s\" is malformed.", CATEGORY_ERROR, 7),
    /**
     * Link is not implemented.
     */
    NOT_IMPLEMENTED("Extras link is not implemented.", CATEGORY_CONFIGURATION, 8);

    /**
     * Message of the exception.
     */
    final String message;

    /**
     * Category of the exception.
     */
    final Category category;

    /**
     * Detail number of the exception.
     */
    final int number;

    /**
     * Default constructor.
     *
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private ConfigJumpExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        number = detailNumber;
    }

    public String getPrefix() {
        return "LGI";
    }

    /**
     * @return the category.
     */
    public Category getCategory() {
        return category;
    }

    /**
     * @return the message.
     */
    public String getMessage() {
        return message;
    }

    /**
     * @return the number.
     */
    public int getNumber() {
        return number;
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