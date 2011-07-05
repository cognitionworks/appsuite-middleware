
package com.openexchange.mail.mime;

import com.openexchange.exception.Category;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.mail.MailExceptionCode;

/**
 * For MIME related errors.
 * <p>
 * Taken from {@link OXException}:
 * <p>
 * The detail number range in subclasses generated in mail bundles is supposed to start with 2000 and may go up to 2999.
 * <p>
 * The detail number range in subclasses generated in transport bundles is supposed to start with 3000 and may go up to 3999.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public enum MIMEMailExceptionCode implements OXExceptionCode {

    /**
     * There was an issue in authenticating your E-Mail password. This may be because of a recent password change. To continue please logout
     * now and then log back in with your most current password. (server=%1$s | user=%2$s)
     */
    LOGIN_FAILED("There was an issue in authenticating your E-Mail password. This may be because of a recent password change. " + "To continue please logout now and then log back in with your most current password. (server=%1$s | user=%2$s)", Category.CATEGORY_PERMISSION_DENIED, 1000),
    /**
     * Wrong or missing login data to access mail server %1$s. Error message from mail server: %2$s
     */
    INVALID_CREDENTIALS("Wrong or missing login data to access mail server %1$s. Error message from mail server: %2$s", Category.CATEGORY_PERMISSION_DENIED, 1001),
    /**
     * Wrong or missing login data to access mail server %1$s with login %2$s (user=%3$s, context=%4$s). Error message from mail server:
     * %5$s
     */
    INVALID_CREDENTIALS_EXT("Wrong or missing login data to access mail server %1$s with login %2$s (user=%3$s, context=%4$s). Error message from mail server: %5$s", Category.CATEGORY_PERMISSION_DENIED, INVALID_CREDENTIALS.detailNumber),
    /**
     * Mail folder "%1$s" could not be found.
     */
    FOLDER_NOT_FOUND("Mail folder \"%1$s\" could not be found.", Category.CATEGORY_ERROR, 1002),
    /**
     * Mail folder "%1$s" could not be found on mail server %2$s with login %3$s (user=%4$s, context=%5$s).
     */
    FOLDER_NOT_FOUND_EXT("Mail folder \"%1$s\" could not be found on mail server %2$s with login %3$s (user=%4$s, context=%5$s).", Category.CATEGORY_ERROR, FOLDER_NOT_FOUND.detailNumber),
    /**
     * Folder "%1$s" has been closed due to some reason.<br>
     * Probably your request took too long.
     * <p>
     * This exception is thrown when a method is invoked on a Messaging object and the Folder that owns that object has died due to some
     * reason. Following the exception, the Folder is reset to the "closed" state.
     * </p>
     */
    FOLDER_CLOSED("Folder \"%1$s\" is closed due to some reason.\nProbably your request took too long", Category.CATEGORY_ERROR, 1003),
    /**
     * Folder "%1$s" has been closed on mail server %2$s with login %3$s (user=%4$s, context=%5$s) due to some reason.<br>
     * Probably your request took too long.
     */
    FOLDER_CLOSED_EXT("Folder \"%1$s\" has been closed on mail server %2$s with login %3$s (user=%4$s, context=%5$s) due to some reason.\nProbably your request took too long.", Category.CATEGORY_ERROR, FOLDER_CLOSED.detailNumber),
    /**
     * Illegal write attempt: %1$s
     * <p>
     * The exception thrown when a write is attempted on a read-only attribute of any Messaging object.
     * </p>
     */
    ILLEGAL_WRITE("Illegal write attempt: %1$s", Category.CATEGORY_ERROR, 1004),
    /**
     * Mail(s) could not be found in folder
     * <p>
     * The exception thrown when an invalid method is invoked on an expunged Message. The only valid methods on an expunged Message are
     * <code>isExpunged()</code> and <code>getMessageNumber()</code>.
     * </p>
     */
    MESSAGE_REMOVED(String.format(MailExceptionCode.MAIL_NOT_FOUND.getMessage(), "", ""), MailExceptionCode.MAIL_NOT_FOUND.getCategory(), MailExceptionCode.MAIL_NOT_FOUND.getNumber()),
    /**
     * Method not supported: %1$s
     * <p>
     * The exception thrown when a method is not supported by the implementation
     * </p>
     */
    METHOD_NOT_SUPPORTED("Method not supported: %1$s", Category.CATEGORY_ERROR, 1006),
    /**
     * Session attempts to instantiate a provider that doesn't exist: %1$s
     */
    NO_SUCH_PROVIDER("Session attempts to instantiate a provider that doesn't exist: %1$s", Category.CATEGORY_ERROR, 1007),
    /**
     * Invalid email address %1$s
     */
    INVALID_EMAIL_ADDRESS("Invalid email address %1$s", Category.CATEGORY_USER_INPUT, 1008),
    /**
     * Wrong message header: %1$s
     * <p>
     * The exception thrown due to an error in parsing RFC822 or MIME headers
     * </p>
     */
    PARSE_ERROR("Wrong message header: %1$s", Category.CATEGORY_USER_INPUT, 1009),
    /**
     * An attempt was made to open a read-only folder with read-write "%1$s"
     */
    READ_ONLY_FOLDER("An attempt was made to open a read-only folder with read-write \"%1$s\"", Category.CATEGORY_PERMISSION_DENIED, 1010),
    /**
     * An attempt was made to open a read-only folder with read-write "%1$s" on mail server %2$s with login %3$s (user=%4$s, context=%5$s)
     */
    READ_ONLY_FOLDER_EXT("An attempt was made to open a read-only folder with read-write \"%1$s\" on mail server %2$s with login %3$s (user=%4$s, context=%5$s)", Category.CATEGORY_PERMISSION_DENIED, 1010),
    /**
     * Invalid search expression: %1$s
     */
    SEARCH_ERROR("Invalid search expression: %1$s", Category.CATEGORY_ERROR, 1011),
    /**
     * Message could not be sent because it is too large
     */
    MESSAGE_TOO_LARGE("Message could not be sent because it is too large", Category.CATEGORY_ERROR, 1012),
    /**
     * Message could not be sent to following recipients: %1$s
     * <p>
     * The exception includes those addresses to which the message could not be sent as well as the valid addresses to which the message was
     * sent and valid addresses to which the message was not sent.
     * </p>
     */
    SEND_FAILED("Message could not be sent to the following recipients: %1$s", Category.CATEGORY_USER_INPUT, 1013),
    /**
     * Store already closed: %1$s
     */
    STORE_CLOSED("Store already closed: %1$s", Category.CATEGORY_ERROR, 1014),
    /**
     * Connection closed to mail server %1$s with login %2$s (user=%3$s, context=%4$s): %5$s
     */
    STORE_CLOSED_EXT("Connection closed to mail server %1$s with login %2$s (user=%3$s, context=%4$s): %5$s", STORE_CLOSED.category, STORE_CLOSED.detailNumber),
    /**
     * Could not bind mail connection to local port %1$s
     * <p>
     * Signals that an error occurred while attempting to bind a socket to a local address and port. Typically, the port is in use, or the
     * requested local address could not be assigned.
     * </p>
     */
    BIND_ERROR("Could not bind connection to local port %1$s", Category.CATEGORY_CONFIGURATION, 1015),
    /**
     * Connection was refused or timed out while attempting to connect to remote mail server %1$s for user %2$s.
     * <p>
     * An error occurred while attempting to connect to remote mail server. Typically, the connection was refused remotely (e.g., no process
     * is listening on the remote address/port).
     * </p>
     */
    CONNECT_ERROR("Connection was refused or timed out while attempting to connect to remote server %1$s for user %2$s.", Category.CATEGORY_SERVICE_DOWN, 1016),
    /**
     * Connection was reset
     */
    CONNECTION_RESET("Connection was reset. Please try again.", Category.CATEGORY_TRY_AGAIN, 1017),
    /**
     * No route to host: mail server %1$s cannot be reached
     * <p>
     * Signals that an error occurred while attempting to connect to remote mail server. Typically, the remote mail server cannot be reached
     * because of an intervening firewall, or if an intermediate router is down.
     * </p>
     */
    NO_ROUTE_TO_HOST("No route to host: server (%1$s) cannot be reached", Category.CATEGORY_SERVICE_DOWN, 1018),
    /**
     * Port %1$s was unreachable on remote mail server
     */
    PORT_UNREACHABLE("Port %1$s was unreachable on remote server", Category.CATEGORY_SERVICE_DOWN, 1019),
    /**
     * Connection is broken due to a socket exception on remote mail server: %1$s
     */
    BROKEN_CONNECTION("Connection is broken due to a socket exception on remote server: %1$s", Category.CATEGORY_SERVICE_DOWN, 1020),
    /**
     * A socket error occurred: %1$s
     */
    SOCKET_ERROR("A socket error occurred: %1$s", Category.CATEGORY_ERROR, 1021),
    /**
     * The IP address of host "%1$s" could not be determined
     */
    UNKNOWN_HOST("The IP address of host \"%1$s\" could not be determined", Category.CATEGORY_SERVICE_DOWN, 1022),
    /**
     * Messaging error: %1$s
     */
    MESSAGING_ERROR("Messaging error: %1$s", Category.CATEGORY_ERROR, 1023),
    /**
     * The quota on mail server exceeded.
     */
    QUOTA_EXCEEDED("The quota on mail server exceeded.", Category.CATEGORY_CAPACITY, 1024),
    /**
     * The quota on mail server "%1$s" exceeded with login %2$s (user=%3$s, context=%4$s).
     */
    QUOTA_EXCEEDED_EXT("The quota on mail server \"%1$s\" exceeded with login %2$s (user=%3$s, context=%4$s).", QUOTA_EXCEEDED.category, QUOTA_EXCEEDED.detailNumber),
    /**
     * A command to mail server failed. Server response: %1$s
     */
    COMMAND_FAILED("A command to mail server failed. Server response: %1$s.", Category.CATEGORY_ERROR, 1025),
    /**
     * A command failed on mail server %1$s with login %2$s (user=%3$s, context=%4$s). Server response: %5$s
     */
    COMMAND_FAILED_EXT("A command failed on mail server %1$s with login %2$s (user=%3$s, context=%4$s). Server response: %5$s", COMMAND_FAILED.category, COMMAND_FAILED.detailNumber),
    /**
     * Mail server indicates a bad command. Server response: %1$s
     */
    BAD_COMMAND("Mail server indicates a bad command. Server response: %1$s", Category.CATEGORY_ERROR, 1026),
    /**
     * Bad command indicated by mail server %1$s with login %2$s (user=%3$s, context=%4$s). Server response: %5$s
     */
    BAD_COMMAND_EXT("Bad command indicated by mail server %1$s with login %2$s (user=%3$s, context=%4$s). Server response: %5$s", BAD_COMMAND.category, BAD_COMMAND.detailNumber),
    /**
     * An error in mail server protocol. Error message: %1$s
     */
    PROTOCOL_ERROR("An error in mail server protocol. Error message: %1$s", Category.CATEGORY_ERROR, 1027),
    /**
     * An error in protocol to mail server %1$s with login %2$s (user=%3$s, context=%4$s). Error message: %5$s
     */
    PROTOCOL_ERROR_EXT("An error in protocol to mail server %1$s with login %2$s (user=%3$s, context=%4$s). Error message: %5$s", PROTOCOL_ERROR.category, PROTOCOL_ERROR.detailNumber),
    /**
     * Message could not be sent: %1$s
     */
    SEND_FAILED_MSG("Message could not be sent: %1$s", Category.CATEGORY_ERROR, 1028),
    /**
     * Message cannot be displayed.
     */
    MESSAGE_NOT_DISPLAYED("Message cannot be displayed.", Category.CATEGORY_SERVICE_DOWN, 1029),
    /**
     * Wrong or missing login data to access mail transport server %1$s. Error message from mail transport server: %2$s
     */
    TRANSPORT_INVALID_CREDENTIALS("Wrong or missing login data to access mail transport server %1$s. Error message from mail transport server: %2$s", Category.CATEGORY_PERMISSION_DENIED, 1030),
    /**
     * Wrong or missing login data to access mail transport server %1$s with login %2$s (user=%3$s, context=%4$s). Error message from mail
     * transport server: %5$s
     */
    TRANSPORT_INVALID_CREDENTIALS_EXT("Wrong or missing login data to access mail transport server %1$s with login %2$s (user=%3$s, context=%4$s). Error message from mail transport server: %5$s", Category.CATEGORY_PERMISSION_DENIED, TRANSPORT_INVALID_CREDENTIALS.detailNumber),
    /**
     * Error processing mail server response. The administrator has been informed.
     */
    PROCESSING_ERROR("Error processing mail server response. The administrator has been informed.", Category.CATEGORY_ERROR, 1031),
    /**
     * Error processing %1$s mail server response for login %2$s (user=%3$s, context=%4$s). The administrator has been informed.
     */
    PROCESSING_ERROR_EXT("Error processing %1$s mail server response for login %2$s (user=%3$s, context=%4$s). The administrator has been informed.", Category.CATEGORY_ERROR, PROCESSING_ERROR.detailNumber),
    /**
     * An I/O error occurred: %1$s
     */
    IO_ERROR(MailExceptionCode.IO_ERROR),
    /**
     * I/O error "%1$s" occurred in communication with "%2$s" mail server for login %3$s (user=%4$s, context=%5$s).
     */
    IO_ERROR_EXT(MailExceptionCode.IO_ERROR, "I/O error \"%1$s\" occurred in communication with \"%2$s\" mail server for login %3$s (user=%4$s, context=%5$s)."),
    /**
     * Error processing mail server response. The administrator has been informed. Error message: %1$s
     */
    PROCESSING_ERROR_WE("Error processing mail server response. The administrator has been informed. Error message: %1$s", Category.CATEGORY_ERROR, PROCESSING_ERROR.detailNumber),
    /**
     * Error processing %1$s mail server response for login %2$s (user=%3$s, context=%4$s). The administrator has been informed. Error
     * message: %5$s
     */
    PROCESSING_ERROR_WE_EXT("Error processing %1$s mail server response for login %2$s (user=%3$s, context=%4$s). The administrator has been informed. Error message: %5$s", Category.CATEGORY_ERROR, PROCESSING_ERROR_WE.detailNumber), ;

    private final String message;

    private final int detailNumber;

    private final Category category;

    private final boolean display;

    private MIMEMailExceptionCode(final String message, final Category category, final int detailNumber) {
        this.message = message;
        this.detailNumber = detailNumber;
        this.category = category;
        display = category.getLogLevel().implies(LogLevel.DEBUG);
    }

    private MIMEMailExceptionCode(final MailExceptionCode code, final String message) {
        this.message = message;
        this.detailNumber = code.getNumber();
        this.category = code.getCategory();
        display = category.getLogLevel().implies(LogLevel.DEBUG);
    }

    private MIMEMailExceptionCode(final MailExceptionCode code) {
        this.message = code.getMessage();
        this.detailNumber = code.getNumber();
        this.category = code.getCategory();
        display = category.getLogLevel().implies(LogLevel.DEBUG);
    }

    public String getPrefix() {
        return "MSG";
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
