/*
 *
 *    OPEN-XCHANGE legal information
 *
 *    All intellectual property rights in the Software are protected by
 *    international copyright laws.
 *
 *
 *    In some countries OX, OX Open-Xchange, open xchange and OXtender
 *    as well as the corresponding Logos OX Open-Xchange and OX are registered
 *    trademarks of the Open-Xchange, Inc. group of companies.
 *    The use of the Logos is not covered by the GNU General Public License.
 *    Instead, you are allowed to use these Logos according to the terms and
 *    conditions of the Creative Commons License, Version 2.5, Attribution,
 *    Non-commercial, ShareAlike, and the interpretation of the term
 *    Non-commercial applicable to the aforementioned license is published
 *    on the web site http://www.open-xchange.com/EN/legal/index.html.
 *
 *    Please make sure that third-party modules and libraries are used
 *    according to their respective licenses.
 *
 *    Any modifications to this package must retain all copyright notices
 *    of the original copyright holder(s) for the original code used.
 *
 *    After any such modifications, the original and derivative code shall remain
 *    under the copyright of the copyright holder(s) and/or original author(s)per
 *    the Attribution and Assignment Agreement that can be located at
 *    http://www.open-xchange.com/EN/developer/. The contributing author shall be
 *    given Attribution for the derivative code and a license granting use.
 *
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
 *     Mail: info@open-xchange.com
 *
 *
 *     This program is free software; you can redistribute it and/or modify it
 *     under the terms of the GNU General Public License, Version 2 as published
 *     by the Free Software Foundation.
 *
 *     This program is distributed in the hope that it will be useful, but
 *     WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *     or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *     for more details.
 *
 *     You should have received a copy of the GNU General Public License along
 *     with this program; if not, write to the Free Software Foundation, Inc., 59
 *     Temple Place, Suite 330, Boston, MA 02111-1307 USA
 *
 */

package com.openexchange.imap.command;

import java.io.InputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Header;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.UIDFolder;
import javax.mail.Message.RecipientType;
import javax.mail.internet.InternetHeaders;
import com.openexchange.imap.IMAPCommandsCollection;
import com.openexchange.mail.MailException;
import com.openexchange.mail.MailListField;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.ExtendedMimeMessage;
import com.openexchange.mail.mime.MIMEMailException;
import com.openexchange.mail.mime.MIMETypes;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.utils.MIMEMessageUtility;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.BODYSTRUCTURE;
import com.sun.mail.imap.protocol.ENVELOPE;
import com.sun.mail.imap.protocol.FLAGS;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.INTERNALDATE;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.imap.protocol.RFC822DATA;
import com.sun.mail.imap.protocol.RFC822SIZE;
import com.sun.mail.imap.protocol.UID;

/**
 * {@link FetchIMAPCommand} - performs a prefetch of messages in given folder with only those fields set that need to be present for display
 * and sorting. A corresponding instance of <code>javax.mail.FetchProfile</code> is going to be generated from given fields.
 * <p>
 * This method avoids calling JavaMail's fetch() methods which implicitly requests whole message envelope (FETCH 1:* (ENVELOPE INTERNALDATE
 * RFC822.SIZE)) when later working on returned <code>javax.mail.Message</code> objects.
 * </p>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FetchIMAPCommand extends AbstractIMAPCommand<Message[]> {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(FetchIMAPCommand.class);

    private static interface SeqNumFetcher {

        public int getNextSeqNum(int messageIndex);

        public int getIndexOf(int value);
    }

    private static class MsgSeqNumFetcher implements SeqNumFetcher {

        private final SeqNumFetcher delegate;

        public MsgSeqNumFetcher(final Message[] msgs) {
            /*
             * Create array from messages' sequence numbers
             */
            final int[] arr = new int[msgs.length];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = msgs[i].getMessageNumber();
            }
            /*
             * Create delegate
             */
            this.delegate = new IntSeqNumFetcher(arr);
        }

        public int getNextSeqNum(final int index) {
            return delegate.getNextSeqNum(index);
        }

        public int getIndexOf(final int value) {
            return delegate.getIndexOf(value);
        }
    }

    private static class IntSeqNumFetcher implements SeqNumFetcher {

        private final int[] arr;

        public IntSeqNumFetcher(final int[] arr) {
            this.arr = arr;
        }

        public int getNextSeqNum(final int index) {
            return arr[index];
        }

        public int getIndexOf(final int value) {
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == value) {
                    return i;
                }
            }
            return -1;
        }
    }

    private final char separator;

    private String[] args;

    private final String command;

    private SeqNumFetcher seqNumFetcher;

    private boolean uid;

    private int length;

    private int index;

    private ExtendedMimeMessage[] retval;

    private final boolean loadBody;

    private boolean determineAttachmentByHeader;

    /**
     * Initializes a new {@link FetchIMAPCommand}.
     * 
     * @param imapFolder - the IMAP folder
     * @param isRev1 Whether IMAP server has <i>IMAP4rev1</i> capability or not
     * @param arr - the source array (either <code>long</code> UIDs, <code>int</code> SeqNums or instances of <code>Message</code>)
     * @param fp - the fetch profile
     * @param isSequential - whether the source array values are sequential
     * @param keepOrder - whether to keep or to ignore given order through parameter <code>arr</code>; only has effect if parameter
     *            <code>arr</code> is of type <code>Message[]</code> or <code>int[]</code>
     * @throws MessagingException
     */
    public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final Object arr, final FetchProfile fp, final boolean isSequential, final boolean keepOrder) throws MessagingException {
        this(imapFolder, isRev1, arr, fp, isSequential, keepOrder, false);
    }

    /**
     * Initializes a new {@link FetchIMAPCommand}.
     * 
     * @param imapFolder - the IMAP folder
     * @param isRev1 Whether IMAP server has <i>IMAP4rev1</i> capability or not
     * @param arr - the source array (either <code>long</code> UIDs, <code>int</code> SeqNums or instances of <code>Message</code>)
     * @param fp - the fetch profile
     * @param isSequential - whether the source array values are sequential
     * @param keepOrder - whether to keep or to ignore given order through parameter <code>arr</code>; only has effect if parameter
     *            <code>arr</code> is of type <code>Message[]</code> or <code>int[]</code>
     * @param loadBody <code>true</code> to load complete messages' bodies; otherwise <code>false</code>
     * @throws MessagingException
     */
    public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final Object arr, final FetchProfile fp, final boolean isSequential, final boolean keepOrder, final boolean loadBody) throws MessagingException {
        super(imapFolder);
        if (imapFolder.getMessageCount() == 0) {
            returnDefaultValue = true;
        }
        this.loadBody = loadBody;
        this.separator = imapFolder.getSeparator();
        command = getFetchCommand(isRev1, fp, loadBody);
        set(arr, isSequential, keepOrder);
    }

    /**
     * Apply a new numeric argument to this IMAP <i>FETCH</i> command
     * 
     * @param arr - the source array (either <code>long</code> UIDs, <code>int</code> SeqNums or instances of <code>Message</code>)
     * @param isSequential whether the source array values are sequential
     * @param keepOrder whether to keep or to ignore given order through parameter <code>arr</code>; only has effect if parameter
     *            <code>arr</code> is of type <code>Message[]</code> or <code>int[]</code>
     * @throws MessagingException
     */
    public void set(final Object arr, final boolean isSequential, final boolean keepOrder) throws MessagingException {
        if (null == arr) {
            returnDefaultValue = true;
        } else {
            createArgs(arr, isSequential, keepOrder);
        }
        retval = new ExtendedMimeMessage[length];
        index = 0;
    }

    /**
     * Sets whether detection if message contains attachment is performed by "Content-Type" header only.
     * <p>
     * If <code>true</code> a message is considered to contain attachments if its "Content-Type" header equals "multipart/mixed".
     * 
     * @param determineAttachmentByHeader <code>true</code> to detect if message contains attachment is performed by "Content-Type" header
     *            only; otherwise <code>false</code>
     * @return This FETCH IMAP command with value applied
     */
    public FetchIMAPCommand setDetermineAttachmentyHeader(final boolean determineAttachmentByHeader) {
        this.determineAttachmentByHeader = determineAttachmentByHeader;
        return this;
    }

    private static final int LENGTH = 9; // "FETCH <nums> (<command>)"

    private static final int LENGTH_WITH_UID = 13; // "UID FETCH <nums> (<command>)"

    private void createArgs(final Object arr, final boolean isSequential, final boolean keepOrder) throws MessagingException {
        if (arr instanceof int[]) {
            final int[] seqNums = (int[]) arr;
            uid = false;
            length = seqNums.length;
            if (0 == length) {
                returnDefaultValue = true;
            } else {
                args = isSequential ? new String[] { new StringBuilder(32).append(seqNums[0]).append(':').append(
                    seqNums[seqNums.length - 1]).toString() } : IMAPNumArgSplitter.splitSeqNumArg(
                    seqNums,
                    keepOrder,
                    LENGTH + command.length());
                seqNumFetcher = keepOrder ? new IntSeqNumFetcher(seqNums) : null;
            }
        } else if (arr instanceof long[]) {
            if (keepOrder) {
                /*
                 * Turn UIDs to corresponding sequence number to initialize seqNumFetcher which keeps track or proper order
                 */
                final int[] seqNums = IMAPCommandsCollection.uids2SeqNums(imapFolder, (long[]) arr);
                uid = false;
                length = seqNums.length;
                if (0 == length) {
                    returnDefaultValue = true;
                } else {
                    args = isSequential ? new String[] { new StringBuilder(32).append(seqNums[0]).append(':').append(
                        seqNums[seqNums.length - 1]).toString() } : IMAPNumArgSplitter.splitSeqNumArg(
                        seqNums,
                        true,
                        LENGTH + command.length());
                    seqNumFetcher = new IntSeqNumFetcher(seqNums);
                }
            } else {
                final long[] uids = (long[]) arr;
                uid = true;
                length = uids.length;
                if (0 == length) {
                    returnDefaultValue = true;
                } else {
                    args = isSequential ? new String[] { new StringBuilder(32).append(uids[0]).append(':').append(uids[uids.length - 1]).toString() } : IMAPNumArgSplitter.splitUIDArg(
                        uids,
                        false,
                        LENGTH_WITH_UID + command.length());
                    seqNumFetcher = null;
                }
            }
        } else if (arr instanceof Message[]) {
            final Message[] msgs = (Message[]) arr;
            uid = false;
            length = msgs.length;
            if (0 == length) {
                returnDefaultValue = true;
            } else {
                args = isSequential ? new String[] { new StringBuilder(64).append(msgs[0].getMessageNumber()).append(':').append(
                    msgs[msgs.length - 1].getMessageNumber()).toString() } : IMAPNumArgSplitter.splitMessageArg(
                    msgs,
                    keepOrder,
                    LENGTH + command.length());
                seqNumFetcher = keepOrder ? new MsgSeqNumFetcher(msgs) : null;
            }
        } else {
            throw new MessagingException(new StringBuilder("Invalid array type! ").append(arr.getClass().getName()).toString());
        }
    }

    /**
     * Constructor to fetch all messages of given folder
     * <p>
     * <b>Note</b>: Ensure that denoted folder is not empty through {@link IMAPFolder#getMessageCount()}.
     * 
     * @param imapFolder - the IMAP folder
     * @param isRev1 Whether IMAP server has <i>IMAP4rev1</i> capability or not
     * @param fp - the fetch profile
     * @param fetchLen - the total message count
     * @throws MessagingException If a messaging error occurs
     */
    public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final FetchProfile fp, final int fetchLen) throws MessagingException {
        this(imapFolder, isRev1, fp, fetchLen, false);
    }

    /**
     * Constructor to fetch all messages of given folder
     * <p>
     * <b>Note</b>: Ensure that denoted folder is not empty through {@link IMAPFolder#getMessageCount()}.
     * 
     * @param imapFolder - the IMAP folder
     * @param isRev1 Whether IMAP server has <i>IMAP4rev1</i> capability or not
     * @param fp - the fetch profile
     * @param fetchLen - the total message count
     * @param loadBody <code>true</code> to load complete messages' bodies; otherwise <code>false</code>
     * @throws MessagingException If a messaging error occurs
     */
    public FetchIMAPCommand(final IMAPFolder imapFolder, final boolean isRev1, final FetchProfile fp, final int fetchLen, final boolean loadBody) throws MessagingException {
        super(imapFolder);
        if (imapFolder.getMessageCount() == 0) {
            returnDefaultValue = true;
        }
        this.loadBody = loadBody;
        this.separator = imapFolder.getSeparator();
        if (0 == fetchLen) {
            returnDefaultValue = true;
        }
        args = AbstractIMAPCommand.ARGS_ALL;
        uid = false;
        length = fetchLen;
        command = getFetchCommand(isRev1, fp, loadBody);
        retval = new ExtendedMimeMessage[length];
        index = 0;
    }

    @Override
    protected String getDebugInfo(final int argsIndex) {
        final StringBuilder sb = new StringBuilder(command.length() + 64);
        if (uid) {
            sb.append("UID ");
        }
        sb.append("FETCH ");
        final String arg = this.args[argsIndex];
        if (arg.length() > 32) {
            final int pos = arg.indexOf(',');
            if (pos == -1) {
                sb.append("...");
            } else {
                sb.append(arg.substring(0, pos)).append(",...,").append(arg.substring(arg.lastIndexOf(',') + 1));
            }
        } else {
            sb.append(arg);
        }
        sb.append(" (").append(command).append(')');
        return sb.toString();
    }

    @Override
    protected boolean addLoopCondition() {
        return (index < length);
    }

    @Override
    protected String[] getArgs() {
        return args;
    }

    @Override
    protected String getCommand(final int argsIndex) {
        final StringBuilder sb = new StringBuilder(args[argsIndex].length() + 64);
        if (uid) {
            sb.append("UID ");
        }
        sb.append("FETCH ");
        sb.append(args[argsIndex]);
        sb.append(" (").append(command).append(')');
        return sb.toString();
    }

    private static final ExtendedMimeMessage[] EMPTY_ARR = new ExtendedMimeMessage[0];

    @Override
    protected Message[] getDefaultValue() {
        return EMPTY_ARR;
    }

    @Override
    protected Message[] getReturnVal() throws MessagingException {
        if (index < length) {
            String server = imapFolder.getStore().toString();
            int pos = server.indexOf('@');
            if (pos >= 0 && ++pos < server.length()) {
                server = server.substring(pos);
            }
            throw new MessagingException(
                new StringBuilder(32).append("Expected ").append(length).append(" FETCH responses but got ").append(index).append(
                    " from IMAP folder \"").append(imapFolder.getFullName()).append("\" on server \"").append(server).append("\".").toString());
        }
        return retval;
    }

    @Override
    protected void handleResponse(final Response currentReponse) throws MessagingException {
        /*
         * Response is null or not a FetchResponse
         */
        if (!FetchResponse.class.isInstance(currentReponse)) {
            return;
        }
        final FetchResponse fetchResponse = (FetchResponse) currentReponse;
        int seqnum;
        final int pos;
        if (null == seqNumFetcher) {
            seqnum = fetchResponse.getNumber();
            pos = index;
        } else {
            seqnum = seqNumFetcher.getNextSeqNum(index);
            if (seqnum == fetchResponse.getNumber()) {
                pos = index;
            } else {
                /*
                 * Assign to current response's sequence number
                 */
                seqnum = fetchResponse.getNumber();
                /*
                 * Look-up position
                 */
                pos = seqNumFetcher.getIndexOf(seqnum);
                if (pos == -1) {
                    throw new MessagingException("Unexpected sequence number in untagged FETCH response: " + seqnum);
                }
            }
        }
        index++;
        final ExtendedMimeMessage msg = new ExtendedMimeMessage(imapFolder.getFullName(), separator, seqnum);
        boolean error = false;
        try {
            final int itemCount = fetchResponse.getItemCount();
            for (int j = 0; j < itemCount; j++) {
                final Item item = fetchResponse.getItem(j);
                FetchItemHandler itemHandler = MAP.get(item.getClass());
                if (null == itemHandler) {
                    itemHandler = getItemHandlerByItem(item, loadBody);
                }
                if (null == itemHandler) {
                    if (LOG.isWarnEnabled()) {
                        LOG.warn("Unknown FETCH item: " + item.getClass().getName());
                    }
                } else {
                    itemHandler.handleItem(item, msg, LOG);
                }
            }
            if (determineAttachmentByHeader) {
                final String cts = msg.getHeader(MessageHeaders.HDR_CONTENT_TYPE, null);
                if (null != cts) {
                    msg.setHasAttachment(new ContentType(cts).isMimeType("multipart/mixed"));
                }
            }
        } catch (final MessagingException e) {
            /*
             * Discard corrupt message
             */
            final MailException imapExc = MIMEMailException.handleMessagingException(e);
            LOG.error(new StringBuilder(128).append("Message #").append(msg.getMessageNumber()).append(" discarded: ").append(
                imapExc.getMessage()).toString(), imapExc);
            error = true;
        } catch (final MailException e) {
            /*
             * Discard corrupt message
             */
            LOG.error(new StringBuilder(128).append("Message #").append(msg.getMessageNumber()).append(" discarded: ").append(
                e.getMessage()).toString(), e);
            error = true;
        }
        if (!error) {
            retval[pos] = msg;
        }
    }

    private static final Set<Integer> ENV_FIELDS;

    static {
        ENV_FIELDS = new HashSet<Integer>(6);
        /*
         * The Envelope is an aggregation of the common attributes of a Message: From, To, Cc, Bcc, ReplyTo, Subject and Date.
         */
        ENV_FIELDS.add(Integer.valueOf(MailListField.FROM.getField()));
        ENV_FIELDS.add(Integer.valueOf(MailListField.TO.getField()));
        ENV_FIELDS.add(Integer.valueOf(MailListField.CC.getField()));
        ENV_FIELDS.add(Integer.valueOf(MailListField.BCC.getField()));
        ENV_FIELDS.add(Integer.valueOf(MailListField.SUBJECT.getField()));
        ENV_FIELDS.add(Integer.valueOf(MailListField.SENT_DATE.getField()));
        /*-
         * Discard the two extra fetch profile items contained in JavaMail's ENVELOPE constant: RFC822.SIZE and INTERNALDATE
         * ENV_FIELDS.add(Integer.valueOf(MailListField.RECEIVED_DATE.getField()));
         * ENV_FIELDS.add(Integer.valueOf(MailListField.SIZE.getField()));
         */
    }

    /*-
     * private static void addFetchItem(final FetchProfile fp, final int field) {
        if (field == MailListField.ID.getField()) {
            fp.add(UIDFolder.FetchProfileItem.UID);
        } else if (field == MailListField.ATTACHMENT.getField()) {
            fp.add(FetchProfile.Item.CONTENT_INFO);
        } else if (field == MailListField.FROM.getField()) {
            fp.add(MessageHeaders.HDR_FROM);
        } else if (field == MailListField.TO.getField()) {
            fp.add(MessageHeaders.HDR_TO);
        } else if (field == MailListField.CC.getField()) {
            fp.add(MessageHeaders.HDR_CC);
        } else if (field == MailListField.BCC.getField()) {
            fp.add(MessageHeaders.HDR_BCC);
        } else if (field == MailListField.SUBJECT.getField()) {
            fp.add(MessageHeaders.HDR_SUBJECT);
        } else if (field == MailListField.SIZE.getField()) {
            fp.add(IMAPFolder.FetchProfileItem.SIZE);
        } else if (field == MailListField.SENT_DATE.getField()) {
            fp.add(MessageHeaders.HDR_DATE);
        } else if (field == MailListField.FLAGS.getField()) {
            if (!fp.contains(FetchProfile.Item.FLAGS)) {
                fp.add(FetchProfile.Item.FLAGS);
            }
        } else if (field == MailListField.DISPOSITION_NOTIFICATION_TO.getField()) {
            fp.add(MessageHeaders.HDR_DISP_NOT_TO);
        } else if (field == MailListField.PRIORITY.getField()) {
            fp.add(MessageHeaders.HDR_X_PRIORITY);
        } else if (field == MailListField.COLOR_LABEL.getField()) {
            if (!fp.contains(FetchProfile.Item.FLAGS)) {
                fp.add(FetchProfile.Item.FLAGS);
            }
        } else if ((field == MailListField.FLAG_SEEN.getField()) && !fp.contains(FetchProfile.Item.FLAGS)) {
            fp.add(FetchProfile.Item.FLAGS);
        }
    }
     */

    /*-
     * private static FetchItemHandler[] createItemHandlers(final int itemCount, final FetchResponse f,
            final boolean loadBody) {
        final FetchItemHandler[] itemHandlers = new FetchItemHandler[itemCount];
        for (int j = 0; j < itemCount; j++) {
            final Item item = f.getItem(j);
            FetchItemHandler h = MAP.get(item.getClass());
            if (null == h) {
                // Try through instanceof checks
                if ((item instanceof RFC822DATA) || (item instanceof BODY)) {
                    if (loadBody) {
                        h = BODY_ITEM_HANDLER;
                    } else {
                        h = HEADER_ITEM_HANDLER;
                    }
                } else if (item instanceof UID) {
                    h = UID_ITEM_HANDLER;
                } else if (item instanceof INTERNALDATE) {
                    h = INTERNALDATE_ITEM_HANDLER;
                } else if (item instanceof Flags) {
                    h = FLAGS_ITEM_HANDLER;
                } else if (item instanceof ENVELOPE) {
                    h = ENVELOPE_ITEM_HANDLER;
                } else if (item instanceof RFC822SIZE) {
                    h = SIZE_ITEM_HANDLER;
                } else if (item instanceof BODYSTRUCTURE) {
                    h = BODYSTRUCTURE_ITEM_HANDLER;
                }
            }
            itemHandlers[j] = h;
        }
        return itemHandlers;
    }
     */

    private static FetchItemHandler getItemHandlerByItem(final Item item, final boolean loadBody) {
        if ((item instanceof RFC822DATA) || (item instanceof BODY)) {
            if (loadBody) {
                return BODY_ITEM_HANDLER;
            }
            return HEADER_ITEM_HANDLER;
        } else if (item instanceof UID) {
            return UID_ITEM_HANDLER;
        } else if (item instanceof INTERNALDATE) {
            return INTERNALDATE_ITEM_HANDLER;
        } else if (item instanceof Flags) {
            return FLAGS_ITEM_HANDLER;
        } else if (item instanceof ENVELOPE) {
            return ENVELOPE_ITEM_HANDLER;
        } else if (item instanceof RFC822SIZE) {
            return SIZE_ITEM_HANDLER;
        } else if (item instanceof BODYSTRUCTURE) {
            return BODYSTRUCTURE_ITEM_HANDLER;
        } else {
            return null;
        }
    }

    private static interface FetchItemHandler {

        /**
         * Handles given <code>com.sun.mail.imap.protocol.Item</code> instance and applies it to given message.
         * 
         * @param item The item to handle
         * @param msg The message to apply to
         * @param logger The logger
         * @throws MessagingException If a messaging error occurs
         * @throws MailException If a mail error occurs
         */
        public abstract void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MessagingException, MailException;
    }

    private static final class HeaderFetchItemHandler implements FetchItemHandler {

        // private static interface HeaderHandler {
        //
        // /**
        // * Handles given header value and applies it to given message.
        // *
        // * @param hdrValue The header value
        // * @param msg The message to apply to
        // * @throws MessagingException If a messaging error occurs
        // * @throws MailException If a mail error occurs
        // */
        // public void handleHeader(String hdrValue, ExtendedMimeMessage msg) throws MessagingException, MailException;
        // }

        // static final MailDateFormat mailDateFormat = new MailDateFormat();

        // private final Map<String, HeaderHandler> hdrHandlers;

        public HeaderFetchItemHandler() {
            super();
            /*-
             * 
            this.hdrHandlers = new HashMap<String, HeaderHandler>();
            hdrHandlers.put(MessageHeaders.HDR_FROM, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.addFrom(InternetAddress.parse(hdrValue, false));
                    } catch (final AddressException e) {
                        msg.setHeader(MessageHeaders.HDR_FROM, hdrValue);
                    }
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_TO, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.setRecipients(RecipientType.TO, InternetAddress.parse(hdrValue, false));
                    } catch (final AddressException e) {
                        msg.setHeader(MessageHeaders.HDR_TO, hdrValue);
                    }
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_CC, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.setRecipients(RecipientType.CC, InternetAddress.parse(hdrValue, false));
                    } catch (final AddressException e) {
                        msg.setHeader(MessageHeaders.HDR_CC, hdrValue);
                    }
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_BCC, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.setRecipients(RecipientType.BCC, InternetAddress.parse(hdrValue, false));
                    } catch (final AddressException e) {
                        msg.setHeader(MessageHeaders.HDR_BCC, hdrValue);
                    }
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_REPLY_TO, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.setReplyTo(InternetAddress.parse(hdrValue, true));
                    } catch (final AddressException e) {
                        msg.setHeader(MessageHeaders.HDR_REPLY_TO, hdrValue);
                    }
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_SUBJECT, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    msg.setHeader(MessageHeaders.HDR_SUBJECT, hdrValue);
                }
            });
            hdrHandlers.put(MessageHeaders.HDR_DATE, new HeaderHandler() {

                public void handleHeader(final String hdrValue, final ExtendedMimeMessage msg) throws MessagingException {
                    try {
                        msg.setSentDate(mailDateFormat.parse(hdrValue));
                    } catch (final ParseException e) {
                        throw new MessagingException(e.getMessage(), e);
                    }
                }
            });
             */
        }

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MessagingException, MailException {
            final InternetHeaders h;
            {
                final InputStream headerStream;
                if (item instanceof BODY) {
                    /*
                     * IMAP4rev1
                     */
                    headerStream = ((BODY) item).getByteArrayInputStream();
                } else {
                    /*
                     * IMAP4
                     */
                    headerStream = ((RFC822DATA) item).getByteArrayInputStream();
                }
                h = new InternetHeaders();
                if (null == headerStream) {
                    if (logger.isDebugEnabled()) {
                        logger.debug(new StringBuilder(32).append("Cannot retrieve headers from message #").append(msg.getMessageNumber()).append(
                            " in folder ").append(msg.getFullname()).toString());
                    }
                } else {
                    h.load(headerStream);
                }
            }
            for (final Enumeration<?> e = h.getAllHeaders(); e.hasMoreElements();) {
                final Header hdr = (Header) e.nextElement();
                msg.setHeader(hdr.getName(), hdr.getValue());
                /*-
                 * 
                final HeaderHandler hdrHandler = hdrHandlers.get(hdr.getName());
                if (hdrHandler == null) {
                    msg.setHeader(hdr.getName(), hdr.getValue());
                } else {
                    hdrHandler.handleHeader(hdr.getValue(), msg);
                }
                 */
            }
        }

    } // End of HeaderFetchItemHandler

    private static final String MULTI_SUBTYPE_MIXED = "MIXED";

    /*-
     * ++++++++++++++ Item handlers ++++++++++++++
     */

    private static final FetchItemHandler FLAGS_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MessagingException {
            msg.setFlags((Flags) item, true);
        }
    };

    private static final FetchItemHandler ENVELOPE_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MessagingException {
            final ENVELOPE env = (ENVELOPE) item;
            msg.addFrom(env.from);
            msg.setRecipients(RecipientType.TO, env.to);
            msg.setRecipients(RecipientType.CC, env.cc);
            msg.setRecipients(RecipientType.BCC, env.bcc);
            msg.setReplyTo(env.replyTo);
            msg.setHeader(MessageHeaders.HDR_IN_REPLY_TO, env.inReplyTo);
            msg.setHeader(MessageHeaders.HDR_MESSAGE_ID, env.messageId);

            final String subject;
            if (env.subject == null) {
                subject = "";
            } else {
                final char[] chars = env.subject.toCharArray();
                final StringBuilder sb = new StringBuilder(chars.length);
                int i = 0;
                while (i < chars.length) {
                    final char c = chars[i];
                    if ('\t' != c && '\r' != c && '\n' != c) {
                        if (' ' == c && (i + 1) < chars.length && ' ' == chars[i + 1]) {
                            i++;
                        }
                        sb.append(c);
                    }
                    i++;
                }
                subject = MIMEMessageUtility.decodeMultiEncodedHeader(sb.toString());
            }
            msg.setSubject(subject, MailProperties.getInstance().getDefaultMimeCharset());

            msg.setSentDate(env.date);
        }
    };

    private static final FetchItemHandler INTERNALDATE_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) {
            msg.setReceivedDate(((INTERNALDATE) item).getDate());
        }
    };

    private static final FetchItemHandler SIZE_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) {
            msg.setSize(((RFC822SIZE) item).size);
        }
    };

    private static final FetchItemHandler BODYSTRUCTURE_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MailException {
            final BODYSTRUCTURE bs = (BODYSTRUCTURE) item;
            msg.setBodystructure(bs);
            final StringBuilder sb = new StringBuilder();
            sb.append(bs.type).append('/').append(bs.subtype);
            if (bs.cParams != null) {
                sb.append(bs.cParams);
            }
            try {
                msg.setContentType(new ContentType(sb.toString()));
            } catch (final MailException e) {
                if (logger.isWarnEnabled()) {
                    logger.warn(e.getMessage(), e);
                }
                msg.setContentType(new ContentType(MIMETypes.MIME_DEFAULT));
            }
            msg.setHasAttachment(bs.isMulti() && (MULTI_SUBTYPE_MIXED.equalsIgnoreCase(bs.subtype) || MIMEMessageUtility.hasAttachments(bs)));
        }
    };

    private static final FetchItemHandler UID_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) {
            msg.setUid(((UID) item).uid);
        }
    };

    private static final FetchItemHandler BODY_ITEM_HANDLER = new FetchItemHandler() {

        public void handleItem(final Item item, final ExtendedMimeMessage msg, final org.apache.commons.logging.Log logger) throws MessagingException, MailException {
            final InputStream msgStream;
            if (item instanceof RFC822DATA) {
                /*
                 * IMAP4
                 */
                msgStream = ((RFC822DATA) item).getByteArrayInputStream();
            } else {
                /*
                 * IMAP4rev1
                 */
                msgStream = ((BODY) item).getByteArrayInputStream();
            }
            if (null == msgStream) {
                if (logger.isWarnEnabled()) {
                    logger.warn(new StringBuilder(32).append("Cannot retrieve body from message #").append(msg.getMessageNumber()).append(
                        " in folder ").append(msg.getFullname()).toString());
                }
            } else {
                msg.parseStream(msgStream);
            }
        }
    };

    private static final FetchItemHandler HEADER_ITEM_HANDLER = new HeaderFetchItemHandler();

    private static final Map<Class<? extends Item>, FetchItemHandler> MAP;

    static {
        MAP = new HashMap<Class<? extends Item>, FetchItemHandler>(8);
        MAP.put(UID.class, UID_ITEM_HANDLER);
        MAP.put(INTERNALDATE.class, INTERNALDATE_ITEM_HANDLER);
        MAP.put(FLAGS.class, FLAGS_ITEM_HANDLER);
        MAP.put(ENVELOPE.class, ENVELOPE_ITEM_HANDLER);
        MAP.put(RFC822SIZE.class, SIZE_ITEM_HANDLER);
        MAP.put(BODYSTRUCTURE.class, BODYSTRUCTURE_ITEM_HANDLER);
        MAP.put(INTERNALDATE.class, INTERNALDATE_ITEM_HANDLER);
    }

    /*-
     * ++++++++++++++ End of item handlers ++++++++++++++
     */

    /**
     * Turns given fetch profile into FETCH items to craft a FETCH command.
     * 
     * @param isRev1 Whether IMAP protocol is revision 1 or not
     * @param fp The fetch profile to convert
     * @param loadBody <code>true</code> if message body should be loaded; otherwise <code>false</code>
     * @return The FETCH items to craft a FETCH command
     */
    private static String getFetchCommand(final boolean isRev1, final FetchProfile fp, final boolean loadBody) {
        final StringBuilder command = new StringBuilder(128);
        final boolean envelope;
        if (fp.contains(FetchProfile.Item.ENVELOPE)) {
            if (loadBody) {
                command.append("INTERNALDATE");
                envelope = false;
            } else {
                command.append("ENVELOPE INTERNALDATE RFC822.SIZE");
                envelope = true;
            }
        } else {
            command.append("INTERNALDATE");
            envelope = false;
        }
        if (fp.contains(FetchProfile.Item.FLAGS)) {
            command.append(" FLAGS");
        }
        if (fp.contains(FetchProfile.Item.CONTENT_INFO)) {
            command.append(" BODYSTRUCTURE");
        }
        if (fp.contains(UIDFolder.FetchProfileItem.UID)) {
            command.append(" UID");
        }
        boolean allHeaders = false;
        if (fp.contains(IMAPFolder.FetchProfileItem.HEADERS) && !loadBody) {
            allHeaders = true;
            if (isRev1) {
                command.append(" BODY.PEEK[HEADER]");
            } else {
                command.append(" RFC822.HEADER");
            }
        }
        if (!envelope && fp.contains(IMAPFolder.FetchProfileItem.SIZE)) {
            command.append(" RFC822.SIZE");
        }
        /*
         * If we're not fetching all headers, fetch individual headers
         */
        if (!allHeaders && !loadBody) {
            final String[] hdrs = fp.getHeaderNames();
            if (hdrs.length > 0) {
                command.append(' ');
                if (isRev1) {
                    command.append("BODY.PEEK[HEADER.FIELDS (");
                } else {
                    command.append("RFC822.HEADER.LINES (");
                }
                command.append(hdrs[0]);
                for (int i = 1; i < hdrs.length; i++) {
                    command.append(' ');
                    command.append(hdrs[i]);
                }
                if (isRev1) {
                    command.append(")]");
                } else {
                    command.append(')');
                }
            }
        }
        if (loadBody) {
            /*
             * Load full message
             */
            if (isRev1) {
                command.append(" BODY.PEEK[]");
            } else {
                command.append(" RFC822");
            }
        }
        return command.toString();
    }

    /**
     * Strips BODYSTRUCTURE item from given fetch profile.
     * 
     * @param fetchProfile The fetch profile
     * @return The fetch profile with BODYSTRUCTURE item stripped
     */
    public static final FetchProfile getSafeFetchProfile(final FetchProfile fetchProfile) {
        if (fetchProfile.contains(FetchProfile.Item.CONTENT_INFO)) {
            final FetchProfile newFetchProfile = new FetchProfile();
            newFetchProfile.add("Content-Type");
            if (!fetchProfile.contains(UIDFolder.FetchProfileItem.UID)) {
                newFetchProfile.add(UIDFolder.FetchProfileItem.UID);
            }
            final javax.mail.FetchProfile.Item[] items = fetchProfile.getItems();
            for (final javax.mail.FetchProfile.Item item : items) {
                if (!FetchProfile.Item.CONTENT_INFO.equals(item)) {
                    newFetchProfile.add(item);
                }
            }
            final String[] names = fetchProfile.getHeaderNames();
            for (final String name : names) {
                newFetchProfile.add(name);
            }
            return newFetchProfile;
        }
        return fetchProfile;
    }

}
