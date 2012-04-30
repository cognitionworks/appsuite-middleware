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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.imap.thread;

import static com.openexchange.mail.MailServletInterface.mailInterfaceMonitor;
import gnu.trove.set.TIntSet;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.mail.Header;
import javax.mail.MessagingException;
import javax.mail.internet.InternetHeaders;
import com.openexchange.imap.IMAPException;
import com.openexchange.imap.threadsort.MessageId;
import com.openexchange.imap.threadsort.ThreadSortNode;
import com.openexchange.imap.util.ImapUtility;
import com.openexchange.log.Log;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.utils.MimeMessageUtility;
import com.sun.mail.iap.BadCommandException;
import com.sun.mail.iap.CommandFailedException;
import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.protocol.BODY;
import com.sun.mail.imap.protocol.FetchResponse;
import com.sun.mail.imap.protocol.IMAPProtocol;
import com.sun.mail.imap.protocol.IMAPResponse;
import com.sun.mail.imap.protocol.Item;
import com.sun.mail.imap.protocol.RFC822DATA;

/**
 * {@link Threadable}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Threadable {

    /**
     * The logger constant.
     */
    static final org.apache.commons.logging.Log LOG = Log.loggerFor(Threadable.class);

    Threadable next;

    Threadable kid;

    String fullName;

    String subject;

    private long date;

    String messageId;

    String inReplyTo;

    String[] refs;

    int messageNumber;

    String id;

    private String subject2;

    private boolean hasRe;

    /**
     * Initializes a new {@link Threadable}.
     */
    public Threadable() {
        subject = null; // this means "dummy".
    }

    /**
     * Initializes a new {@link Threadable}.
     * 
     * @param next The next element
     * @param subject The subject
     * @param id The identifier
     * @param references The referenced identifiers
     */
    public Threadable(final Threadable next, final String subject, final String id, final String[] references) {
        this.next = next;
        this.subject = subject;
        this.messageId = id;
        this.refs = references;
    }

    public MessageId toMessageId() {
        return new MessageId(messageNumber).setFullName(fullName);
    }

    @Override
    public String toString() {
        if (isDummy()) {
            return "[dummy]";
        }

        String s = "[ " + messageId + ": " + subject + " (";
        if (refs != null) {
            for (int i = 0; i < refs.length; i++) {
                s += " " + refs[i];
            }
        }
        if (date > 0) {
            s += " \"" + new Date(date) + "\"";
        }
        return s + " ) ]";
    }

    private static final Pattern PATTERN_SUBJECT = Pattern.compile("^\\s*(Re|Sv|Vs|Aw|\u0391\u03A0|\u03A3\u03A7\u0395\u03A4|R|Rif|Res|Odp|Ynt)(?:\\[.*?\\]|\\(.*?\\))?:(?:\\s*)(.*)(?:\\s*)", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private void simplifySubject() {
        if (isEmpty(subject)) {
            subject2 = "";
            return;
        }

        // Try by RegEx
        {
            final Matcher m = PATTERN_SUBJECT.matcher(subject);
            if (m.matches()) {
                subject2 = m.group(2);
                return;
            }
        }

        // Start position
        int start = 0;
        final String subject = this.subject.trim();
        final int len = subject.length();
        if (len <= 2) {
            subject2 = subject;
            return;
        }

        boolean done = false;
        while (!done && (start < len)) {
            done = true;

            if (start < (len - 2) && (subject.charAt(start) == 'r' || subject.charAt(start) == 'R') && (subject.charAt(start + 1) == 'e' || subject.charAt(start + 1) == 'E')) {
                if (subject.charAt(start + 2) == ':') {
                    start += 3; // Skip over "Re:"
                    hasRe = true; // yes, we found it.
                    done = false; // keep going.
                } else if (start < (len - 2) && (subject.charAt(start + 2) == '[' || subject.charAt(start + 2) == '(')) {
                    int i = start + 3; // skip over "Re[" or "Re("

                    // Skip forward over digits after the "[" or "(".
                    while (i < len && subject.charAt(i) >= '0' && subject.charAt(i) <= '9') {
                        i++;
                    }

                    // Now ensure that the following thing is "]:" or "):"
                    // Only if it is do we alter `start'.
                    if (i < (len - 1) && (subject.charAt(i) == ']' || subject.charAt(i) == ')') && subject.charAt(i + 1) == ':') {
                        start = i + 2; // Skip over "]:"
                        hasRe = true; // yes, we found it.
                        done = false; // keep going.
                    }
                }
            }

            if (subject2 == "(no subject)") {
                subject2 = "";
            }
        }

        if (start == 0) {
            subject2 = subject;
        } else {
            subject2 = subject.substring(start).trim();
        }
    }

    private static boolean isEmpty(final String string) {
        if (null == string) {
            return true;
        }
        final int len = string.length();
        boolean isWhitespace = true;
        for (int i = 0; isWhitespace && i < len; i++) {
            isWhitespace = Character.isWhitespace(string.charAt(i));
        }
        return isWhitespace;
    }

    void flushSubjectCache() {
        subject2 = null;
    }

    /**
     * Returns each subsequent element in the set of messages of which this IThreadable is the root. Order is unimportant.
     */
    public Enumeration<Threadable> allElements() {
        return new ThreadableEnumeration(this, true);
    }

    /**
     * Returns an object identifying this message. Generally this will be a representation of the contents of the Message-ID header.
     */
    public String messageID() {
        return messageId;
    }

    /**
     * Returns the IDs of the set of messages referenced by this one. This list should be ordered from oldest-ancestor to youngest-ancestor.
     */
    public String[] messageReferences() {
        return refs;
    }

    /**
     * When no references are present, subjects will be used to thread together messages. This method should return a threadable subject:
     * two messages with the same simplifiedSubject will be considered to belong to the same thread. This string should not have `Re:' on
     * the front, and may have been simplified in whatever other ways seem appropriate.
     * <p>
     * This is a String of Unicode characters, and should have had any encodings (such as RFC 2047 charset encodings) removed first.
     * <p>
     * If you aren't interested in threading by subject at all, return null.
     */
    public String simplifiedSubject() {
        if (subject2 == null) {
            simplifySubject();
        }
        return subject2;
    }

    /**
     * Whether the original subject was one that appeared to be a reply (that is, had a `Re:' or some other indicator.) When threading by
     * subject, this property is used to tell whether two messages appear to be siblings, or in a parent/child relationship.
     */
    public boolean subjectIsReply() {
        if (subject2 == null) {
            simplifySubject();
        }
        return hasRe;
    }

    /**
     * Sets the full name
     * 
     * @param fullName The full name to set
     * @return This threadable
     */
    public Threadable setFullName(final String fullName) {
        this.fullName = fullName;
        return this;
    }

    /**
     * Gets the full name
     * 
     * @return The full name
     */
    public String getFullName() {
        return fullName;
    }

    /**
     * Gets the kid
     * 
     * @return The kid
     */
    public Threadable kid() {
        return kid;
    }

    /**
     * Gets the next
     * 
     * @return The next
     */
    public Threadable next() {
        return next;
    }

    /**
     * When the proper thread order has been computed, these two methods will be called on each Threadable in the chain, to set up the
     * proper tree structure.
     */
    public void setNext(final Object next) {
        this.next = (Threadable) next;
        flushSubjectCache();
    }

    public void setChild(final Threadable kid) {
        this.kid = kid;
        flushSubjectCache();
    }

    /**
     * Creates a dummy parent object.
     * <P>
     * With some set of messages, the only way to achieve proper threading is to introduce an element into the tree which represents
     * messages which are not present in the set: for example, when two messages share a common ancestor, but that ancestor is not in the
     * set. This method is used to make a placeholder for those sorts of ancestors. It should return an object which is also a IThreadable.
     * The setNext() and setChild() methods will be used on this placeholder, as either the object or the argument, just as for other
     * elements of the tree.
     */
    public Threadable makeDummy() {
        return new Threadable();
    }

    /**
     * This should return true of dummy messages, false otherwise. It is legal to pass dummy messages in with the list returned by
     * elements(); the isDummy() method is the mechanism by which they are noted and ignored.
     */
    public boolean isDummy() {
        return (subject == null);
    }

    private static final class ThreadableEnumeration implements Enumeration<Threadable> {

        Threadable tail;

        Enumeration<Threadable> kids;

        boolean recursive;

        ThreadableEnumeration(final Threadable thread, final boolean recursive) {
            this.recursive = recursive;
            if (recursive) {
                tail = thread;
            } else {
                tail = thread.kid;
            }
        }

        @Override
        public Threadable nextElement() {
            if (kids != null) {
                // if `kids' is non-null, then we've already returned a node,
                // and we should now go to work on its children.
                final Threadable result = kids.nextElement();
                if (!kids.hasMoreElements()) {
                    kids = null;
                }
                return result;

            } else if (tail != null) {
                // Return `tail', but first note its children, if any.
                // We will descend into them the next time around.
                final Threadable result = tail;
                if (recursive && tail.kid != null) {
                    kids = new ThreadableEnumeration(tail.kid, true);
                }
                tail = tail.next;
                return result;

            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public boolean hasMoreElements() {
            if (tail != null) {
                return true;
            } else if (kids != null && kids.hasMoreElements()) {
                return true;
            } else {
                return false;
            }
        }
    }

    private interface HeaderHandler {

        void handle(Header hdr, Threadable threadable) throws MessagingException;

    }

    static final Map<String, HeaderHandler> HANDLERS = new HashMap<String, HeaderHandler>() {

        {
            put(MessageHeaders.HDR_SUBJECT, new HeaderHandler() {

                @Override
                public void handle(final Header hdr, final Threadable threadable) throws MessagingException {
                    threadable.subject = MimeMessageUtility.decodeMultiEncodedHeader(MimeMessageUtility.checkNonAscii(hdr.getValue()));
                }
            });
            put(MessageHeaders.HDR_REFERENCES, new HeaderHandler() {

                private final Pattern split = Pattern.compile(" +");

                @Override
                public void handle(final Header hdr, final Threadable threadable) throws MessagingException {
                    threadable.refs = split.split(MimeMessageUtility.decodeMultiEncodedHeader(hdr.getValue()));
                }
            });
            put(MessageHeaders.HDR_MESSAGE_ID, new HeaderHandler() {

                @Override
                public void handle(final Header hdr, final Threadable threadable) throws MessagingException {
                    threadable.messageId = MimeMessageUtility.decodeMultiEncodedHeader(hdr.getValue());
                }
            });
            put(MessageHeaders.HDR_IN_REPLY_TO, new HeaderHandler() {

                @Override
                public void handle(final Header hdr, final Threadable threadable) throws MessagingException {
                    threadable.inReplyTo = MimeMessageUtility.decodeMultiEncodedHeader(hdr.getValue());
                }
            });
        }
    };

    public static String toThreadReferences(final Threadable threadable, final TIntSet filter) {
        final StringBuilder sb = new StringBuilder(256);
        toThreadReferences0(threadable, filter, sb);
        return sb.toString();
    }

    private static void toThreadReferences0(final Threadable threadable, final TIntSet filter, final StringBuilder sb) {
        Threadable t = threadable;
        if (null == filter) {
            while (null != t) {
                sb.append('(');
                if (t.messageNumber > 0) {
                    sb.append(t.toMessageId());
                }
                final Threadable kid = t.kid;
                if (null != kid) {
                    if (t.messageNumber > 0) {
                        sb.append(' ');
                    }
                    toThreadReferences0(kid, null, sb);
                }
                final int lastPos = sb.length() - 1;
                if ('(' == sb.charAt(lastPos)) {
                    sb.deleteCharAt(lastPos);
                } else {
                    sb.append(')');
                }
                t = t.next;
            }
        } else {
            while (null != t) {
                if (filter.contains(t.messageNumber)) {
                    sb.append('(');
                    if (t.messageNumber > 0) {
                        sb.append(t.toMessageId());
                    }
                    final Threadable kid = t.kid;
                    if (null != kid) {
                        if (t.messageNumber > 0) {
                            sb.append(' ');
                        }
                        toThreadReferences0(kid, null, sb);
                    }
                    final int lastPos = sb.length() - 1;
                    if ('(' == sb.charAt(lastPos)) {
                        sb.deleteCharAt(lastPos);
                    } else {
                        sb.append(')');
                    }
                } else {
                    final Threadable kid = t.kid;
                    if (null != kid) {
                        toThreadReferences0(kid, filter, sb);
                    }
                }
                t = t.next;
            }
        }
    }

    /**
     * Appends the latter <tt>Threadable</tt> to the first <tt>Threadable</tt> instance.
     * 
     * @param threadable The <tt>Threadable</tt> instance
     * @param toAppend The <tt>Threadable</tt> instance to append
     */
    public static void append(final Threadable threadable, final Threadable toAppend) {
        if (null == threadable) {
            return;
        }
        Threadable t = threadable;
        while (null != t.next) {
            t = t.next;
        }
        t.next = toAppend;
    }

    /**
     * Transforms <tt>Threadable</tt> to list of <tt>ThreadSortNode</tt>s.
     * 
     * @param t The <tt>Threadable</tt> to transform
     * @return The resulting list of <tt>ThreadSortNode</tt>s
     */
    public static List<ThreadSortNode> toNodeList(final Threadable t) {
        if (null == t) {
            return Collections.emptyList();
        }
        final List<ThreadSortNode> list = new LinkedList<ThreadSortNode>();
        fillInList(t, list);
        return list;
    }

    private static void fillInList(final Threadable t, final List<ThreadSortNode> list) {
        Threadable cur = t;
        while (null != cur) {
            final ThreadSortNode node = new ThreadSortNode(cur.toMessageId());
            list.add(node);
            // Check kids
            final Threadable kid = cur.kid;
            if (null != kid) {
                final List<ThreadSortNode> sublist = new LinkedList<ThreadSortNode>();
                fillInList(kid, sublist);
                node.addChildren(sublist);
            }
            // Proceed to next
            cur = cur.next;
        }
    }

    /**
     * Filters from <tt>Threadable</tt> those sub-trees which solely consist of specified <tt>Threadable</tt>s associated with given full
     * name
     * 
     * @param fullName The full name to filter with
     * @param t The <tt>Threadable</tt> instance
     */
    public static Threadable filterFullName(final String fullName, final Threadable t) {
        final List<Threadable> list = new LinkedList<Threadable>();
        {
            Threadable cur = t;
            while (null != cur) {
                list.add(cur);
                cur = cur.next;
            }
        }
        if (list.isEmpty()) {
            return t;
        }
        // Filter
        for (final Iterator<Threadable> iterator = list.iterator(); iterator.hasNext();) {
            final Threadable cur = iterator.next();
            if (checkFullName(fullName, cur)) {
                iterator.remove();
            }
        }
        // Fold
        final Threadable first = list.remove(0);
        {
            Threadable cur = first;
            for (final Threadable threadable : list) {
                cur.next = threadable;
                cur = threadable;
            }
        }
        return first;
    }

    private static boolean checkFullName(final String fullName, final Threadable t) {
        Threadable cur = t;
        while (null != cur) {
            if (t.messageNumber > 0 && !fullName.equals(cur.fullName)) {
                return false;
            }
            final Threadable kid = t.kid;
            if (null != kid) {
                if (!checkFullName(fullName, kid)) {
                    return false;
                }
            }
            cur = cur.next;
        }
        // Solely consists of threadables associated with given full name
        return true;
    }

    /**
     * Gets the <tt>Threadable</tt>s for given IMAP folder.
     * 
     * @param imapFolder The IMAP folders
     * @return The detected <tt>Threadable</tt>s
     * @throws MessagingException If an error occurs
     */
    public static Threadable getAllThreadablesFrom(final IMAPFolder imapFolder) throws MessagingException {
        final int messageCount = imapFolder.getMessageCount();
        if (messageCount <= 0) {
            /*
             * Empty folder...
             */
            return null;
        }
        return (Threadable) (imapFolder.doCommand(new IMAPFolder.ProtocolCommand() {

            @Override
            public Object doCommand(final IMAPProtocol protocol) throws ProtocolException {
                final String command;
                final Response[] r;
                {
                    StringBuilder sb = new StringBuilder(128).append("FETCH ").append(1 == messageCount ? "1" : "1:*").append(" (");
                    final boolean rev1 = protocol.isREV1();
                    if (rev1) {
                        sb.append("BODY.PEEK[HEADER.FIELDS (Subject Message-Id Reference In-Reply-To)]");
                    } else {
                        sb.append("RFC822.HEADER.LINES (Subject Message-Id Reference In-Reply-To)");
                    }
                    sb.append(')');
                    command = sb.toString();
                    sb = null;
                    final long start = System.currentTimeMillis();
                    r = protocol.command(command, null);
                    final long dur = System.currentTimeMillis() - start;
                    if (LOG.isInfoEnabled()) {
                        LOG.info('"' + command + "\" for \"" + imapFolder.getFullName() + "\" took " + dur + "msec.");
                    }
                    mailInterfaceMonitor.addUseTime(dur);
                }
                final int len = r.length - 1;
                final Response response = r[len];
                if (response.isOK()) {
                    try {
                        final List<Threadable> threadables = new ArrayList<Threadable>(messageCount);
                        final String fullName = imapFolder.getFullName();
                        for (int j = 0; j < len; j++) {
                            if ("FETCH".equals(((IMAPResponse) r[j]).getKey())) {
                                final FetchResponse fetchResponse = (FetchResponse) r[j];
                                final InternetHeaders h;
                                {
                                    final InputStream headerStream;
                                    final BODY body = getItemOf(BODY.class, fetchResponse);
                                    if (null == body) {
                                        final RFC822DATA rfc822data = getItemOf(RFC822DATA.class, fetchResponse);
                                        headerStream = null == rfc822data ? null : rfc822data.getByteArrayInputStream();
                                    } else {
                                        headerStream = body.getByteArrayInputStream();
                                    }
                                    if (null == headerStream) {
                                        h = null;
                                    } else {
                                        h = new InternetHeaders();
                                        h.load(headerStream);
                                    }
                                }
                                if (h != null) {
                                    final Threadable t = new Threadable().setFullName(fullName);
                                    t.messageNumber = fetchResponse.getNumber();
                                    for (final Enumeration<?> e = h.getAllHeaders(); e.hasMoreElements();) {
                                        final Header hdr = (Header) e.nextElement();
                                        final HeaderHandler headerHandler = HANDLERS.get(hdr.getName());
                                        if (null != headerHandler) {
                                            headerHandler.handle(hdr, t);
                                        }
                                    }
                                    // Check References and In-Reply-To
                                    if (null != t.refs && null != t.inReplyTo) {
                                        final String[] tmp = t.refs;
                                        t.refs = new String[tmp.length + 1];
                                        System.arraycopy(tmp, 0, t.refs, 0, tmp.length);
                                        t.refs[tmp.length] = t.inReplyTo;
                                    }
                                    threadables.add(t);
                                    r[j] = null;
                                }
                            }
                        }
                        protocol.notifyResponseHandlers(r);
                        final Threadable first = threadables.remove(0);
                        {
                            Threadable cur = first;
                            for (final Threadable threadable : threadables) {
                                cur.next = threadable;
                                cur = threadable;
                            }
                        }
                        return first;
                    } catch (final MessagingException e) {
                        throw new ProtocolException(e.getMessage(), e);
                    }
                } else if (response.isBAD()) {
                    if (ImapUtility.isInvalidMessageset(response)) {
                        return new long[0];
                    }
                    throw new BadCommandException(IMAPException.getFormattedMessage(
                        IMAPException.Code.PROTOCOL_ERROR,
                        command,
                        response.toString() + " (" + imapFolder.getStore().toString() + ")"));
                } else if (response.isNO()) {
                    throw new CommandFailedException(IMAPException.getFormattedMessage(
                        IMAPException.Code.PROTOCOL_ERROR,
                        command,
                        response.toString() + " (" + imapFolder.getStore().toString() + ")"));
                } else {
                    protocol.handleResult(response);
                }
                return null;
            }

        }));
    }

    /**
     * Gets the item associated with given class in specified <i>FETCH</i> response.
     * 
     * @param <I> The returned item's class
     * @param clazz The item class to look for
     * @param fetchResponse The <i>FETCH</i> response
     * @return The item associated with given class in specified <i>FETCH</i> response or <code>null</code>.
     * @see #getItemOf(Class, FetchResponse, String)
     */
    protected static <I extends Item> I getItemOf(final Class<? extends I> clazz, final FetchResponse fetchResponse) {
        final int len = fetchResponse.getItemCount();
        for (int i = 0; i < len; i++) {
            final Item item = fetchResponse.getItem(i);
            if (clazz.isInstance(item)) {
                return clazz.cast(item);
            }
        }
        return null;
    }
}
