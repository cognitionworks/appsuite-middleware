package com.openexchange.groupware.userconfiguration;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionStrings;

/**
 * The error code enumeration for user configurations.
 */
public enum UserConfigurationExceptionCodes {

    /**
     * A SQL error occurred: %1$s
     */
    SQL_ERROR("A SQL error occurred: %1$s", Category.CATEGORY_ERROR, 1),
    /**
     * A DBPooling error occurred
     */
    DBPOOL_ERROR("A DBPooling error occurred", Category.CATEGORY_ERROR, 2),
    /**
     * Configuration for user %1$s could not be found in context %2$d
     */
    NOT_FOUND("Configuration for user %1$s could not be found in context %2$d", Category.CATEGORY_ERROR, 3),
    /**
     * Missing property %1$s in system.properties.
     */
    MISSING_SETTING("Missing property %1$s in system.properties.", Category.CATEGORY_CONFIGURATION, 4),
    /**
     * Class %1$s can not be found.
     */
    CLASS_NOT_FOUND("Class %1$s can not be found.", Category.CATEGORY_CONFIGURATION, 5),
    /**
     * Instantiating the class failed.
     */
    INSTANTIATION_FAILED("Instantiating the class failed.", Category.CATEGORY_ERROR, 6),
    /**
     * Cache initialization failed. Region: %1$s
     */
    CACHE_INITIALIZATION_FAILED("Cache initialization failed. Region: %1$s", Category.CATEGORY_ERROR, 7),
    /**
     * User configuration could not be put into cache: %1$s
     */
    CACHE_PUT_ERROR("User configuration could not be put into cache: %1$s", Category.CATEGORY_ERROR, 8),
    /**
     * User configuration cache could not be cleared: %1$s
     */
    CACHE_CLEAR_ERROR("User configuration cache could not be cleared: %1$s", Category.CATEGORY_ERROR, 9),
    /**
     * User configuration could not be removed from cache: %1$s
     */
    CACHE_REMOVE_ERROR("User configuration could not be removed from cache: %1$s", Category.CATEGORY_ERROR, 9),
    /**
     * Mail settings for user %1$s could not be found in context %2$d
     */
    MAIL_SETTING_NOT_FOUND("Mail settings for user %1$s could not be found in context %2$d", Category.CATEGORY_ERROR, 10);

    private final String message;

    private final int detailNumber;

    private final Category category;

    private UserConfigurationExceptionCodes(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
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

    private static final String PREFIX = "USS";

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
