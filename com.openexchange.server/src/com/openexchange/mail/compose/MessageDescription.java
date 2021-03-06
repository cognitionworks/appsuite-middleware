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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.mail.compose;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import com.openexchange.java.Strings;
import com.openexchange.mail.compose.Message.ContentType;
import com.openexchange.mail.compose.Message.Priority;

/**
 * {@link MessageDescription}
 *
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 * @since v7.10.2
 */
public class MessageDescription {

    private Address from;
    private Address sender;
    private List<Address> to;
    private List<Address> cc;
    private List<Address> bcc;
    private Address replyTo;
    private String subject;
    private String content;
    private ContentType contentType;
    private boolean requestReadReceipt;
    private SharedAttachmentsInfo sharedAttachmentsInfo;
    private List<Attachment> attachments;
    private Meta meta;
    private Security security;
    private Priority priority;
    private ClientToken clientToken;
    private boolean contentEncrypted;
    private Map<String, String> customHeaders;

    private boolean bFrom;
    private boolean bSender;
    private boolean bTo;
    private boolean bCc;
    private boolean bBcc;
    private boolean bReplyTo;
    private boolean bSubject;
    private boolean bContent;
    private boolean bContentType;
    private boolean bRequestReadReceipt;
    private boolean bSharedAttachmentsInfo;
    private boolean bAttachments;
    private boolean bMeta;
    private boolean bSecurity;
    private boolean bPriority;
    private boolean bClientToken;
    private boolean bContentEncrypted;
    private boolean bCustomHeaders;

    /**
     * Initializes a new {@link MessageDescription}.
     */
    public MessageDescription() {
        super();
        priority = Priority.NORMAL;
        sharedAttachmentsInfo = SharedAttachmentsInfo.DISABLED;
        security = Security.DISABLED;
        meta = Meta.META_NEW;
    }

    public boolean seemsEqual(MessageDescription other) {
        // Check flags
        if (other.bContentEncrypted && contentEncrypted != other.contentEncrypted) {
            return false;
        }
        if (other.bContentType && contentType != other.contentType) {
            return false;
        }
        if (other.bPriority && priority != other.priority) {
            return false;
        }
        if (other.bRequestReadReceipt && requestReadReceipt != other.requestReadReceipt) {
            return false;
        }

        // Check addresses
        if (other.bFrom) {
            if (from == null) {
                if (other.from != null) {
                    return false;
                }
            } else if (!from.equals(other.from)) {
                return false;
            }
        }
        if (other.bTo) {
            if (to == null) {
                if (other.to != null) {
                    return false;
                }
            } else if (!to.equals(other.to)) {
                return false;
            }
        }
        if (other.bCc) {
            if (cc == null) {
                if (other.cc != null) {
                    return false;
                }
            } else if (!cc.equals(other.cc)) {
                return false;
            }
        }
        if (other.bBcc) {
            if (bcc == null) {
                if (other.bcc != null) {
                    return false;
                }
            } else if (!bcc.equals(other.bcc)) {
                return false;
            }
        }
        if (other.bReplyTo) {
            if (replyTo == null) {
                if (other.replyTo != null) {
                    return false;
                }
            } else if (!replyTo.equals(other.replyTo)) {
                return false;
            }
        }
        if (other.bSender) {
            if (sender == null) {
                if (other.sender != null) {
                    return false;
                }
            } else if (!sender.equals(other.sender)) {
                return false;
            }
        }

        // Check subject
        if (other.bSubject) {
            if (subject == null) {
                if (other.subject != null) {
                    return false;
                }
            } else if (!subject.equals(other.subject)) {
                return false;
            }
        }

        // Check content
        if (other.bContent) {
            if (content == null) {
                if (other.content != null) {
                    return false;
                }
            } else if (!content.equals(other.content)) {
                return false;
            }
        }

        // Check rest
        if (other.bCustomHeaders) {
            if (customHeaders == null) {
                if (other.customHeaders != null) {
                    return false;
                }
            } else if (!customHeaders.equals(other.customHeaders)) {
                return false;
            }
        }
        if (other.bMeta) {
            if (meta == null) {
                if (other.meta != null) {
                    return false;
                }
            } else if (!meta.equals(other.meta)) {
                return false;
            }
        }
        if (other.bSecurity) {
            if (security == null) {
                if (other.security != null) {
                    return false;
                }
            } else if (!security.equals(other.security)) {
                return false;
            }
        }
        if (other.bSharedAttachmentsInfo) {
            if (sharedAttachmentsInfo == null) {
                if (other.sharedAttachmentsInfo != null) {
                    return false;
                }
            } else if (!sharedAttachmentsInfo.equals(other.sharedAttachmentsInfo)) {
                return false;
            }
        }
        if (other.containsValidClientToken()) {
            if (clientToken == null) {
                if (other.clientToken != null) {
                    return false;
                }
            } else if (!clientToken.equals(other.clientToken)) {
                return false;
            }
        }

        return true;
    }


    public Address getFrom() {
        return from;
    }

    public MessageDescription setFrom(Address from) {
        this.from = from;
        bFrom = true;
        return this;
    }

    public boolean containsFrom() {
        return bFrom;
    }

    public void removeFrom() {
        from = null;
        bFrom = false;
    }

    public Address getSender() {
        return sender;
    }

    public MessageDescription setSender(Address sender) {
        this.sender = sender;
        bSender = true;
        return this;
    }

    public boolean containsSender() {
        return bSender;
    }

    public void removeSender() {
        sender = null;
        bSender = false;
    }

    public List<Address> getTo() {
        return to;
    }

    public MessageDescription setTo(List<Address> to) {
        this.to = to;
        bTo = true;
        return this;
    }

    public MessageDescription addTo(List<Address> to) {
        if (this.to == null) {
            return setTo(to);
        }

        if (to != null) {
            this.to.addAll(to);
        }
        return this;
    }

    public boolean containsTo() {
        return bTo;
    }

    public void removeTo() {
        to = null;
        bTo = false;
    }

    public List<Address> getCc() {
        return cc;
    }

    public MessageDescription setCc(List<Address> cc) {
        this.cc = null == cc ? cc : new LinkedList<>(cc);
        bCc = true;
        return this;
    }

    public MessageDescription addCc(List<Address> cc) {
        if (this.cc == null) {
            return setCc(cc);
        }

        if (cc != null) {
            this.cc.addAll(cc);
        }
        return this;
    }

    public boolean containsCc() {
        return bCc;
    }

    public void removeCc() {
        cc = null;
        bCc = false;
    }

    public List<Address> getBcc() {
        return bcc;
    }

    public MessageDescription setBcc(List<Address> bcc) {
        this.bcc = bcc;
        bBcc = true;
        return this;
    }

    public MessageDescription addBcc(List<Address> bcc) {
        if (this.bcc == null) {
            return setBcc(bcc);
        }

        if (bcc != null) {
            this.bcc.addAll(bcc);
        }
        return this;
    }

    public boolean containsBcc() {
        return bBcc;
    }

    public void removeBcc() {
        bcc = null;
        bBcc = false;
    }

    public Address getReplyTo() {
        return replyTo;
    }

    public MessageDescription setReplyTo(Address replyTo) {
        this.replyTo = replyTo;
        bReplyTo = true;
        return this;
    }

    public boolean containsReplyTo() {
        return bReplyTo;
    }

    public void removeReplyTo() {
        replyTo = null;
        bReplyTo = false;
    }

    public String getSubject() {
        return subject;
    }

    public MessageDescription setSubject(String subject) {
        this.subject = subject;
        bSubject = true;
        return this;
    }

    public boolean containsSubject() {
        return bSubject;
    }

    public void removeSubject() {
        subject = null;
        bSubject = false;
    }

    public String getContent() {
        return content;
    }

    public MessageDescription setContent(String content) {
        this.content = content;
        bContent = true;
        return this;
    }

    public boolean containsContent() {
        return bContent;
    }

    public void removeContent() {
        content = null;
        bContent = false;
    }

    public ContentType getContentType() {
        return contentType;
    }

    public MessageDescription setContentType(ContentType contentType) {
        this.contentType = contentType;
        bContentType = true;
        return this;
    }

    public boolean containsContentType() {
        return bContentType;
    }

    public void removeContentType() {
        contentType = null;
        bContentType = false;
    }

    public boolean isContentEncrypted() {
        return contentEncrypted;
    }

    public MessageDescription setContentEncrypted(boolean contentEncrypted) {
        this.contentEncrypted = contentEncrypted;
        bContentEncrypted = true;
        return this;
    }

    public boolean containsContentEncrypted() {
        return bContentEncrypted;
    }

    public void removeContentEncrypted() {
        contentEncrypted = false;
        bContentEncrypted = false;
    }

    public Map<String, String> getCustomHeaders() {
        return customHeaders;
    }

    public void setCustomHeaders(Map<String, String> customHeaders) {
        this.customHeaders = customHeaders;
        bCustomHeaders = true;
    }

    public void addCustomHeader(String name, String value) {
        if (Strings.isNotEmpty(name) && Strings.isNotEmpty(value)) {
            Map<String, String> customHeaders = this.customHeaders;
            if (customHeaders == null) {
                customHeaders = new LinkedHashMap<>();
                this.customHeaders = customHeaders;
            }
            customHeaders.put(name, value);
            bCustomHeaders = true;
        }
    }

    public boolean containsCustomHeaders() {
        return bCustomHeaders;
    }

    public void removeCustomHeaders() {
        customHeaders = null;
        bCustomHeaders = false;
    }

    public boolean isRequestReadReceipt() {
        return requestReadReceipt;
    }

    public MessageDescription setRequestReadReceipt(boolean requestReadReceipt) {
        this.requestReadReceipt = requestReadReceipt;
        bRequestReadReceipt = true;
        return this;
    }

    public boolean containsRequestReadReceipt() {
        return bRequestReadReceipt;
    }

    public void removeRequestReadReceipt() {
        requestReadReceipt = false;
        bRequestReadReceipt = false;
    }

    public SharedAttachmentsInfo getSharedAttachmentsInfo() {
        return sharedAttachmentsInfo;
    }

    public MessageDescription setsharedAttachmentsInfo(SharedAttachmentsInfo sharedAttachmentsInfo) {
        this.sharedAttachmentsInfo = sharedAttachmentsInfo;
        bSharedAttachmentsInfo = true;
        return this;
    }

    public boolean containsSharedAttachmentsInfo() {
        return bSharedAttachmentsInfo;
    }

    public void removeSharedAttachmentsInfo() {
        sharedAttachmentsInfo = SharedAttachmentsInfo.DISABLED;
        bSharedAttachmentsInfo = false;
    }

    public List<Attachment> getAttachments() {
        return attachments;
    }

    public MessageDescription setAttachments(List<? extends Attachment> attachments) {
        if (attachments == null) {
            this.attachments = null;
        } else {
            this.attachments = new ArrayList<>(attachments.size());
            for (Attachment attachment : attachments) {
                this.attachments.add(attachment);
            }
        }
        bAttachments = true;
        return this;
    }

    public boolean containsAttachments() {
        return bAttachments;
    }

    public void removeAttachments() {
        attachments = null;
        bAttachments = false;
    }

    public Meta getMeta() {
        return meta;
    }

    public MessageDescription setMeta(Meta meta) {
        this.meta = meta;
        bMeta = true;
        return this;
    }

    public boolean containsMeta() {
        return bMeta;
    }

    public void removeMeta() {
        meta = null;
        bMeta = false;
    }

    public Security getSecurity() {
        return security;
    }

    public MessageDescription setSecurity(Security security) {
        this.security = security;
        bSecurity = true;
        return this;
    }

    public boolean containsSecurity() {
        return bSecurity;
    }

    /**
     * Checks if security object has been set before and its value is not <code>null</code>.
     *
     * @return <code>true</code> if previously set and not <code>null</code>; otherwise <code>false</code>
     */
    public boolean containsNotNullSecurity() {
        return bSecurity && security != null;
    }

    public void removeSecurity() {
        security = null;
        bSecurity = false;
    }

    public Priority getPriority() {
        return priority;
    }

    public MessageDescription setPriority(Priority priority) {
        this.priority = priority;
        bPriority = true;
        return this;
    }

    public boolean containsPriority() {
        return bPriority;
    }

    public void removePriority() {
        priority = null;
        bPriority = false;
    }

    /**
     * Gets the client token
     *
     * @return The client token
     */
    public ClientToken getClientToken() {
        return clientToken;
    }

    public MessageDescription setClientToken(ClientToken clientToken) {
        this.clientToken = clientToken;
        bClientToken = true;
        return this;
    }

    public boolean containsClientToken() {
        return bClientToken;
    }

    public boolean containsValidClientToken() {
        return clientToken != null && clientToken.isPresent();
    }

    public void removeClientToken() {
        clientToken = null;
        bClientToken = false;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("[");
        if (from != null) {
            builder.append("from=").append(from).append(", ");
        }
        if (sender != null) {
            builder.append("sender=").append(sender).append(", ");
        }
        if (to != null) {
            builder.append("to=").append(to).append(", ");
        }
        if (cc != null) {
            builder.append("cc=").append(cc).append(", ");
        }
        if (bcc != null) {
            builder.append("bcc=").append(bcc).append(", ");
        }
        if (subject != null) {
            builder.append("subject=").append(subject).append(", ");
        }
        if (content != null) {
            builder.append("content=").append(content).append(", ");
        }
        if (contentType != null) {
            builder.append("contentType=").append(contentType).append(", ");
        }
        builder.append("requestReadReceipt=").append(requestReadReceipt).append(", ");
        if (sharedAttachmentsInfo != null) {
            builder.append("sharedAttachmentsInfo=").append(sharedAttachmentsInfo).append(", ");
        }
        if (attachments != null) {
            builder.append("attachments=").append(attachments).append(", ");
        }
        if (meta != null) {
            builder.append("meta=").append(meta).append(", ");
        }
        if (security != null) {
            builder.append("security=").append(security).append(", ");
        }
        if (priority != null) {
            builder.append("priority=").append(priority).append(", ");
        }
        if (clientToken != null) {
            builder.append("clientToken=").append(clientToken);
        }
        builder.append("]");
        return builder.toString();
    }

}
