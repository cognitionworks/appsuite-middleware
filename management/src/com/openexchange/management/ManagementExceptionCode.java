package com.openexchange.management;

import com.openexchange.exception.Category;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;

/**
 * Management error codes.
 */
public enum ManagementExceptionCode implements OXExceptionCode {

    /**
     * MBean registration denied: ManagementAgent is not running.
     */
    NOT_RUNNING("MBean registration denied: ManagementAgent is not running.", CATEGORY_CONFIGURATION, 1),
    /**
     * Malformed object name: %1$s
     */
    MALFORMED_OBJECT_NAME("Malformed object name: %1$s", CATEGORY_ERROR, 2),
    /**
     * Not compliant MBean: %1$s
     */
    NOT_COMPLIANT_MBEAN("Not compliant MBean: %1$s", CATEGORY_ERROR, 3),
    /**
     * MBean registration error: %1$s
     */
    MBEAN_REGISTRATION("MBean registration error: %1$s", CATEGORY_ERROR, 4),
    /**
     * MBean already exists: %1$s.
     */
    ALREADY_EXISTS("MBean already exists: %1$s", CATEGORY_ERROR, 5),
    /**
     * MBean not found: %1$s.
     */
    NOT_FOUND("MBean not found: %1$s", CATEGORY_ERROR, 6),
    /**
     * Malformed URL: %1$s
     */
    MALFORMED_URL("Malformed URL: %1$s", CATEGORY_ERROR, 7),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR("An I/O error occurred: %1$s", CATEGORY_ERROR, 8),
    /**
     * Unknown host error: %1$s
     */
    UNKNOWN_HOST_ERROR("Unknown host error: %1$s", CATEGORY_ERROR, 9),
    /**
     * Remote error: %1$s
     */
    REMOTE_ERROR("Remote error: %1$s", CATEGORY_ERROR, 9),
    /**
     * A JMX connector is already bound to URL %1$s.
     */
    JMX_URL_ALREADY_BOUND("A JMX connector is already bound to URL %1$s.", CATEGORY_ERROR, 10);

    private final String message;

    private final int detailNumber;

    private final Category category;

    private ManagementExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
    }
    
    public String getPrefix() {
        return "JMX";
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