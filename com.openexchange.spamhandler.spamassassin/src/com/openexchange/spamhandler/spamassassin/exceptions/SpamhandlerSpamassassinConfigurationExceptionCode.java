package com.openexchange.spamhandler.spamassassin.exceptions;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Error codes for permission exceptions.
 * @author <a href="mailto:dennis.sieben@open-xchange.org">Dennis Sieben</a>
 */
public enum SpamhandlerSpamassassinConfigurationExceptionCode implements OXExceptionCode {
    /**
     * The given value for mode "%s" is not a possible one
     */
    MODE_TYPE_WRONG("The given value for mode \"%s\" is not a possible one", CATEGORY_CONFIGURATION, 1),

    /**
     * The parameter "%s" is not set in the property file
     */
    PARAMETER_NOT_SET("The parameter \"%s\" is not set in property file", CATEGORY_CONFIGURATION, 2),

    /**
     * The parameter "%s" must be set in the property file if spamd is true
     */
    PARAMETER_NOT_SET_SPAMD("The parameter \"%s\" must be set in the property file if spamd is true", CATEGORY_CONFIGURATION, 3),

    /**
     * The parameter "%s" must be an integer value but is "%s"
     */
    PARAMETER_NO_INTEGER("The parameter \"%s\" must be an integer value but is \"%s\"", CATEGORY_CONFIGURATION, 4),

    /**
     * The parameter "userSource" must be set in the property file if spamd is true
     */
    USERSOURCE_NOT_SET("The parameter \"userSource\" must be set in the property file if spamd is true", CATEGORY_CONFIGURATION, 5),

    /**
     * The given value for userSource "%s" is not a possible one
     */
    USERSOURCE_WRONG("The given value for userSource \"%s\" is not a possible one", CATEGORY_CONFIGURATION, 6),

    /**
     * The parameter "%s" must be an long value but is "%s"
     */
    PARAMETER_NO_LONG("The parameter \"%s\" must be an long value but is \"%s\"", CATEGORY_CONFIGURATION, 7);


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
     * @param message message.
     * @param category category.
     * @param detailNumber detail number.
     */
    private SpamhandlerSpamassassinConfigurationExceptionCode(final String message, final Category category,
        final int detailNumber) {
        this.message = message;
        this.category = category;
        this.number = detailNumber;
    }

    public String getPrefix() {
        return "MSG";
    }

    public Category getCategory() {
        return category;
    }

    public String getMessage() {
        return message;
    }

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