package com.openexchange.tools.iterator;

import com.openexchange.exception.Category;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionStrings;

/**
 * The {@link SearchIterator} error code enumeration.
 */
public enum SearchIteratorExceptionCodes {

    /**
     * A SQL error occurred: %1$s
     */
    SQL_ERROR("A SQL error occurred: %1$s", Category.CATEGORY_ERROR, 1),
    /**
     * A DBPool error occurred: %1$s
     */
    DBPOOLING_ERROR("A DBPool error occurred: 1$%s", Category.CATEGORY_ERROR, 2),
    /**
     * Operation not allowed on a closed SearchIterator
     */
    CLOSED("Operation not allowed on a closed SearchIterator", Category.CATEGORY_ERROR, 3),
    /**
     * Mapping for %1$d not implemented
     */
    NOT_IMPLEMENTED("Mapping for %1$d not implemented", Category.CATEGORY_ERROR, 4),

    /**
     * FreeBusyResults calculation problem with oid: %1$d
     */
    CALCULATION_ERROR("FreeBusyResults calculation problem with oid: %1$d", Category.CATEGORY_ERROR, 5),
    /**
     * Invalid constructor argument. Instance of %1$s not supported
     */
    INVALID_CONSTRUCTOR_ARG("Invalid constructor argument. Instance of %1$s not supported", Category.CATEGORY_ERROR, 6),
    /**
     * No such element.
     */
    NO_SUCH_ELEMENT("No such element.", Category.CATEGORY_ERROR, 7),
    /**
     * An unexpected error occurred: %1$s
     */
    UNEXPECTED_ERROR("An unexpected error occurred: %1$s", Category.CATEGORY_ERROR, 8);

    private final String message;

    private final int detailNumber;

    private final Category category;

    private final boolean display;

    private SearchIteratorExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.category = category;
        this.detailNumber = detailNumber;
        display = LogLevel.DEBUG.equals(category.getLogLevel());
    }

    public Category getCategory() {
        return category;
    }

    public int getDetailNumber() {
        return detailNumber;
    }

    public String getMessage() {
        return message;
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

    private static final String PREFIX = "FLD";

    /**
     * Creates an {@link OXException} instance using this error code.
     *
     * @param cause The initial cause for {@link OXException}
     * @param arguments The arguments for message.
     * @return The newly created {@link OXException} instance.
     */
    public OXException create(final Throwable cause, final Object... arguments) {
        final OXException ret;
        if (display) {
            ret = new OXException(detailNumber, message, cause, arguments);
        } else {
            ret = new OXException(detailNumber, OXExceptionStrings.MESSAGE, cause);
            ret.setLogMessage(message, arguments);
        }
        return ret.setPrefix(PREFIX).addCategory(category);
    }
}