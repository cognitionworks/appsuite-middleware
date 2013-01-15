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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.mq.topic.impl;

import java.io.ByteArrayOutputStream;
import javax.jms.BytesMessage;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import javax.jms.TextMessage;
import javax.jms.Topic;
import javax.jms.TopicSubscriber;
import com.openexchange.exception.OXException;
import com.openexchange.java.UnsynchronizedByteArrayOutputStream;
import com.openexchange.mq.MQExceptionCodes;
import com.openexchange.mq.topic.MQTopicListener;
import com.openexchange.mq.topic.MQTopicSyncSubscriber;

/**
 * {@link MQTopicSyncSubscriberImpl} - An asynchronous topic subscriber intended to be re-used. It subscribes specified {@link MQTopicListener
 * listener} to given topic.
 * <p>
 * Invoke {@link #close()} method when done.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MQTopicSyncSubscriberImpl extends MQTopicResource implements MQTopicSyncSubscriber {

    protected TopicSubscriber topicSubscriber;

    /**
     * Initializes a new {@link MQTopicSyncSubscriberImpl}.
     *
     * @param topicName The name of topic to subscribe from
     * @throws OXException If initialization fails
     */
    public MQTopicSyncSubscriberImpl(final String topicName) throws OXException {
        super(topicName, null);
    }

    @Override
    protected boolean isTransacted() {
        return false;
    }

    @Override
    protected int getAcknowledgeMode() {
        return Session.AUTO_ACKNOWLEDGE;
    }

    @Override
    protected synchronized void initResource(final Topic topic, final Object ignore) throws JMSException {
        topicSubscriber = topicSession.createSubscriber(topic);
        topicConnection.start();
    }

    @Override
    public String receiveText() throws OXException {
        try {
            final Message message = topicSubscriber.receive();
            if (!(message instanceof TextMessage)) {
                return null;
            }
            return ((TextMessage) message).getText();
        } catch (final JMSException e) {
            throw MQExceptionCodes.JMS_ERROR.create(e, e.getMessage());
        }
    }

    @Override
    public String receiveText(final long timeout) throws OXException {
        try {
            final Message message = topicSubscriber.receive(timeout);
            if (!(message instanceof TextMessage)) {
                return null;
            }
            return ((TextMessage) message).getText();
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    @Override
    public String receiveTextNoWait() throws OXException {
        try {
            final Message message = topicSubscriber.receiveNoWait();
            if (!(message instanceof TextMessage)) {
                return null;
            }
            return ((TextMessage) message).getText();
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    /*-
     * ----------------------- Receive Java object methods --------------------------
     */

    @Override
    public Object receiveObject() throws OXException {
        try {
            final Message message = topicSubscriber.receive();
            if (!(message instanceof ObjectMessage)) {
                return null;
            }
            return ((ObjectMessage) message).getObject();
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    @Override
    public Object receiveObject(final long timeout) throws OXException {
        try {
            final Message message = topicSubscriber.receive(timeout);
            if (!(message instanceof ObjectMessage)) {
                return null;
            }
            return ((ObjectMessage) message).getObject();
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    @Override
    public Object receiveObjectNoWait() throws OXException {
        try {
            final Message message = topicSubscriber.receiveNoWait();
            if (!(message instanceof ObjectMessage)) {
                return null;
            }
            return ((ObjectMessage) message).getObject();
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    /*-
     * ----------------------- Receive bytes methods --------------------------
     */

    @Override
    public byte[] receiveBytes() throws OXException {
        try {
            final Message message = topicSubscriber.receive();
            if (!(message instanceof BytesMessage)) {
                return null;
            }
            return readBytesFrom((BytesMessage) message);
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    @Override
    public byte[] receiveBytes(final long timeout) throws OXException {
        try {
            final Message message = topicSubscriber.receive(timeout);
            if (!(message instanceof BytesMessage)) {
                return null;
            }
            return readBytesFrom((BytesMessage) message);
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    @Override
    public byte[] receiveBytesNoWait() throws OXException {
        try {
            final Message message = topicSubscriber.receiveNoWait();
            if (!(message instanceof BytesMessage)) {
                return null;
            }
            return readBytesFrom((BytesMessage) message);
        } catch (final JMSException e) {
            throw MQExceptionCodes.handleJMSException(e);
        }
    }

    private static byte[] readBytesFrom(final BytesMessage bytesMessage) throws JMSException {
        final ByteArrayOutputStream out = new UnsynchronizedByteArrayOutputStream(4096);
        final int buflen = 2048;
        final byte[] buf = new byte[buflen];
        for (int read; (read = bytesMessage.readBytes(buf, buflen)) > 0;) {
            out.write(buf, 0, read);
        }
        return out.toByteArray();
    }

}
