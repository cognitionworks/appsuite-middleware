package com.openexchange.passwordchange.servlet;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

public enum PasswordChangeServletExceptionCode implements OXExceptionCode {

    /**
     * A JSON error occurred: %1$s
     */
    JSON_ERROR("A JSON error occurred: %1$s", CATEGORY_ERROR, 1),
    /**
     * Invalid locale string: %1$s
     */
    INVALID_LOCALE_STR("Invalid locale string: %1$s", CATEGORY_ERROR, 2),
    /**
     * Unsupported action %1$s via %2$s request
     */
    UNSUPPORTED_ACTION("Unsupported action %1$s via %2$s request", CATEGORY_ERROR, 3),
    /**
     * Missing parameter %1$s
     */
    MISSING_PARAM("Missing parameter %1$s", CATEGORY_ERROR, 4);

    private final String message;

    private final int detailNumber;

    private final Category category;

    private PasswordChangeServletExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
    }
    
    public String getPrefix() {
        return "PSW";
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