
package com.openexchange.groupware.contexts.impl;

import com.openexchange.exception.Category;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Error codes for context exceptions.
 */
public enum ContextExceptionCodes implements OXExceptionCode {
    /**
     * Mailadmin for a context is missing.
     */
    NO_MAILADMIN("Cannot resolve mailadmin for context %d.", Category.CATEGORY_CONFIGURATION, 1),
    /**
     * Cannot find context %d.
     */
    NOT_FOUND("Cannot find context %d.", Category.CATEGORY_CONFIGURATION, 2),
    /**
     * No connection to database.
     */
    NO_CONNECTION("Cannot get connection to database.", Category.CATEGORY_SERVICE_DOWN, 5),
    /**
     * SQL problem: %1$s.
     */
    SQL_ERROR("SQL problem: %1$s.", Category.CATEGORY_ERROR, 6),
    /**
     * Updating database ... Try again later.
     */
    UPDATE("Updating database ... Try again later.", Category.CATEGORY_TRY_AGAIN, 7),
    /**
     * Problem initializing the cache.
     */
    CACHE_INIT("Problem initializing the cache.", Category.CATEGORY_CONFIGURATION, 8),
    /**
     * Cannot remove object %s from cache.
     */
    CACHE_REMOVE("Cannot remove object %s from cache.", Category.CATEGORY_ERROR, 9),
    /**
     * Cannot find context "%s".
     */
    NO_MAPPING("Cannot find context \"%s\".", Category.CATEGORY_ERROR, 10);

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
    private final int number;

    private final boolean display;

    /**
     * Default constructor.
     * 
     * @param message message.
     * @param category category.
     * @param number detail number.
     */
    private ContextExceptionCodes(final String message, final Category category, final int number) {
        this.message = message;
        this.category = category;
        this.number = number;
        display = category.getLogLevel().implies(LogLevel.DEBUG);
    }

    @Override
    public String getPrefix() {
        return "CTX";
    }

    /**
     * @return the message
     */
    @Override
    public String getMessage() {
        return message;
    }

    /**
     * @return the category
     */
    @Override
    public Category getCategory() {
        return category;
    }

    /**
     * @return the number
     */
    @Override
    public int getNumber() {
        return number;
    }

    @Override
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
