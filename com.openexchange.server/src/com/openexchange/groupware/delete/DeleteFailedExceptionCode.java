package com.openexchange.groupware.delete;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionStrings;

/**
 * Error code enumeration for a failed delete event.
 */
public enum DeleteFailedExceptionCode {
    /**
     * Unknown delete event type: %1$d
     */
    UNKNOWN_TYPE("Unknown delete event type: %1$d", Category.CATEGORY_ERROR, 1),
    /**
     * A SQL error occurred: %1$s
     */
    SQL_ERROR("A SQL error occurred: %1$s", Category.CATEGORY_ERROR, 2),
    /**
     * An error occurred: %1$s
     */
    ERROR("An error occurred: %1$s", Category.CATEGORY_ERROR, 3);

    private final String message;

    private final Category category;

    private final int detailNumber;

    private DeleteFailedExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        this.detailNumber = detailNumber;
    }

    /**
     * Creates an {@link OXException} instance using this error code.
     *
     * @return The newly created {@link OXException} instance.
     */
    public OXException create() {
        return create(new Object[0]);
    }

    /**
     * Creates an {@link OXException} instance using this error code.
     *
     * @param logArguments The arguments for log message.
     * @return The newly created {@link OXException} instance.
     */
    public OXException create(final Object... logArguments) {
        return create(null, logArguments);
    }

    private static final String PREFIX = "DEL";

    /**
     * Creates an {@link OXException} instance using this error code.
     *
     * @param cause The initial cause for {@link OXException}
     * @param logArguments The arguments for log message.
     * @return The newly created {@link OXException} instance.
     */
    public OXException create(final Throwable cause, final Object... logArguments) {
        return new OXException(detailNumber, OXExceptionStrings.MESSAGE, cause).setPrefix(PREFIX).addCategory(category).setLogMessage(
            message,
            logArguments);
    }
}