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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.imap.threader.references;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.java.Strings;
import com.openexchange.mail.MailSortField;
import com.openexchange.mail.dataobjects.MailMessage;
import com.openexchange.mail.utils.MailMessageComparator;

/**
 * {@link Conversation} - Encapsulates a list of messages.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class Conversation {

    /**
     * The default comparator.
     */
    private static final MailMessageComparator COMPARATOR_DESC = new MailMessageComparator(MailSortField.RECEIVED_DATE, true, null);

    /**
     * The default initial capacity - MUST be a power of two.
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 16;

    private final Set<MailMessageWrapper> messages;
    private final Set<String> messageIds;
    private final Set<String> references;

    private boolean messageIdsEmpty;

    private Conversation main;

    /**
     * Initializes a new {@link Conversation}.
     */
    public Conversation() {
        super();
        messages = new HashSet<MailMessageWrapper>(DEFAULT_INITIAL_CAPACITY);
        messageIds = new LinkedHashSet<String>(DEFAULT_INITIAL_CAPACITY << 1, 0.9f);
        references = new LinkedHashSet<String>(DEFAULT_INITIAL_CAPACITY << 1, 0.9f);
        messageIdsEmpty = true;
    }

    /**
     * Initializes a new {@link Conversation}.
     */
    public Conversation(MailMessage message) {
        this();
        addMessage(message);
    }

    /**
     * Initializes a new {@link Conversation}.
     */
    public Conversation(Collection<MailMessage> messages) {
        this();
        if (null != messages) {
            for (MailMessage message : messages) {
                addMessage(message);
            }
        }
    }

    /**
     * Adds given message to this conversation
     *
     * @param message The message to add
     * @return This conversation with message added
     */
    public Conversation addMessage(MailMessage message) {
        if (null != message) {
            addWrapper(new MailMessageWrapper(message));
        }
        return this;
    }

    /**
     * Adds the given {@link MailMessageWrapper} to this conversation
     *
     * @param mmw The {@link MailMessageWrapper}
     */
    private void addWrapper(MailMessageWrapper mmw) {
        Conversation main = getMain();
        if (main.messages.add(mmw)) {
            final MailMessage message = mmw.message;
            final String messageId = message.getMessageId();
            if (null != messageId) {
                if (main.messageIds.add(messageId)) {
                    main.messageIdsEmpty = false;
                }
            }

            final String[] sReferences = message.getReferences();
            if (null != sReferences) {
                for (String sReference : sReferences) {
                    if (null != sReference && Strings.isNotEmpty(sReference)) {
                        main.references.add(sReference);
                    }
                }
            } else {
                String inReplyTo = message.getInReplyTo();
                if (null != inReplyTo && Strings.isNotEmpty(inReplyTo)) {
                    main.references.add(inReplyTo);
                }
            }
        }
    }

    /**
     * Joins this conversation with other conversation.
     *
     * @param other The other conversation to join with
     * @return The main conversation
     */
    public Conversation join(Conversation other) {
        if (null != other) {
            if (this.equals(other)) {
                return this;
            }
            Conversation main = this.main;
            if (null != main) {
                main.join(other);
                return main.getMain();
            }
            
            // now get the other main
            Conversation otherMain = other.getMain();
            // special case, we found ourself, no need to fetch mails
            if (this.equals(otherMain)) {
                return this;
            }
            
            final Set<MailMessageWrapper> messages = otherMain.messages;
            for (MailMessageWrapper mmw : messages) {
                addWrapper(mmw);
            }
            otherMain.setMain(this);
        }
        return this;
    }

    /**
     * Sets the main {@link Conversation}
     *
     * @param main The {@link Conversation} to set as main
     */
    private void setMain(Conversation main) {
        if (this.equals(main)) {
            return;
        }
        Conversation otherMain = getMain();
        if (!this.equals(otherMain)) {
            otherMain.setMain(main);
        }
        this.main = main;
    }

    /**
     * Gets the main {@link Conversation}
     *
     * @return The main {@link Conversation}
     */
    private Conversation getMain() {
        Conversation main = this.main;
        return main != null ? main.getMain() : this;
    }

    /**
     * Returns true if this is the main {@link Conversation}
     * @return <code>true</code> if this is the main  {@link Conversation}, <code>false</code> otherwise
     */
    public boolean isMain() {
        return this.equals(getMain());
    }

    /**
     *Adds the message identifiers from this conversation to given set.
     *
     * @param set The set to add the message identifiers to
     */
    public void addMessageIdsTo(Set<String> set) {
        Conversation main = getMain();
        if (!main.messageIdsEmpty) {
            set.addAll(main.messageIds);
        }
    }

    /**
     * Gets the messages.
     *
     * @return The messages with default sorting
     */
    public List<MailMessage> getMessages() {
        return getMessages(COMPARATOR_DESC);
    }

    /**
     * Gets the messages.
     *
     * @param comparator The comparator used for sorting listed messages
     * @return The messages with given sorting
     */
    public List<MailMessage> getMessages(Comparator<MailMessage> comparator) {
        Conversation main = getMain();
        if (main.messages.isEmpty()) {
            return Collections.emptyList();
        }
        final List<MailMessage> ret = new ArrayList<MailMessage>(main.messages.size());
        for (MailMessageWrapper mmw : main.messages) {
            ret.add(mmw.message);
        }
        Collections.sort(ret, null == comparator ? COMPARATOR_DESC : comparator);
        return ret;
    }

    /**
     * Gets the references
     *
     * @return The references
     */
    public Set<String> getReferences() {
        return getMain().references;
    }

    /**
     * Gets the message ids
     *
     * @return The message ids
     */
    public Set<String> getMessageIds() {
        return getMain().messageIds;
    }

    /**
     * Simple wrapper class for having a message as hash key.
     * <p>
     * The key is built from its identifier and folder full name.
     */
    private static final class MailMessageWrapper {

        final MailMessage message;
        private final int hash;

        /**
         * Initializes a new {@link MailMessageWrapper}.
         *
         * @param message The {@link MailMessage} to wrap
         */
        MailMessageWrapper(MailMessage message) {
            super();
            this.message = message;
            final String id = message.getMailId();
            final String fullName = message.getFolder();
            final int prime = 31;
            int result = 1;
            result = prime * result + ((id == null) ? 0 : id.hashCode());
            result = prime * result + ((fullName == null) ? 0 : fullName.hashCode());
            hash = result;
        }

        @Override
        public int hashCode() {
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof MailMessageWrapper)) {
                return false;
            }
            final MailMessageWrapper other = (MailMessageWrapper) obj;
            if (message.getMailId() == null) {
                if (other.message.getMailId() != null) {
                    return false;
                }
            } else if (!message.getMailId().equals(other.message.getMailId())) {
                return false;
            }
            if (message.getFolder() == null) {
                if (other.message.getFolder() != null) {
                    return false;
                }
            } else if (!message.getFolder().equals(other.message.getFolder())) {
                return false;
            }
            return true;
        }
    } // End of class MailMessageWrapper

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Conversation [messageIds=");
        builder.append(messageIds);
        builder.append(", references=");
        builder.append(references);
        builder.append("]");
        return builder.toString();
    }

}
