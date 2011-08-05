package com.openexchange.spamhandler.spamassassin.exceptions;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

public enum SpamhandlerSpamassassinExceptionCode implements OXExceptionCode {
    /**
     * Spamd returned wrong exit code "%s"
     */
    WRONG_SPAMD_EXIT("Spamd returned wrong exit code \"%s\"", CATEGORY_ERROR, 3000),

    /**
     * Internal error: Wrong arguments are given to the tell command: "%s"
     */
    WRONG_TELL_CMD_ARGS("Internal error: Wrong arguments are given to the tell command: \"%s\"", CATEGORY_ERROR, 3001),

    /**
     * Error during communication with spamd: "%s"
     */
    COMMUNICATION_ERROR("Error during communication with spamd: \"%s\"", CATEGORY_ERROR, 3002),

    /**
     * Can't handle spam because MailService isn't available
     */
    MAILSERVICE_MISSING("Can't handle spam because MailService isn't available", CATEGORY_ERROR, 3003),

    /**
     * Error while getting spamd provider from service: "%s"
     */
    ERROR_GETTING_SPAMD_PROVIDER("Error while getting spamd provider from service: \"%s\"", CATEGORY_ERROR, 3004);




    final Category category;

    final int detailNumber;

    final String message;

    private SpamhandlerSpamassassinExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
    }

    @Override
    public String getPrefix() {
        return "MSG";
    }

    @Override
    public Category getCategory() {
        return category;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public int getNumber() {
        return detailNumber;
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