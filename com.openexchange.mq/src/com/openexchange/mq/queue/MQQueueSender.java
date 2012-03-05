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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.mq.queue;

import java.io.Serializable;
import com.openexchange.exception.OXException;
import com.openexchange.mq.MQCloseable;

/**
 * {@link MQQueueSender} - A queue sender intended to be re-used. Invoke {@link #close()} method when done.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface MQQueueSender extends MQCloseable {

    /**
     * Sends a message containing a <code>java.lang.String</code>.
     * 
     * @param text The <code>java.lang.String</code> to send
     * @throws OXException If send operation fails
     */
    public void sendTextMessage(String text) throws OXException;

    /**
     * Sends a message containing a <code>java.lang.String</code>.
     * 
     * @param text The <code>java.lang.String</code> to send
     * @param priority The priority (<code>4</code> is default); range from 0 (lowest) to 9 (highest)
     * @throws OXException If send operation fails
     */
    public void sendTextMessage(String text, int priority) throws OXException;

    /**
     * Sends a message containing a {@link Serializable serializable} Java object.
     * 
     * @param object The serializable object to send
     * @throws OXException If send operation fails
     */
    public void sendObjectMessage(Serializable object) throws OXException;

    /**
     * Sends a message containing a {@link Serializable serializable} Java object.
     * 
     * @param object The serializable object to send
     * @param priority The priority (<code>4</code> is default); range from 0 (lowest) to 9 (highest)
     * @throws OXException If send operation fails
     */
    public void sendObjectMessage(Serializable object, int priority) throws OXException;

    /**
     * Sends a message containing <code>byte</code>s.
     * 
     * @param bytes The <code>byte</code> array to send
     * @throws OXException If send operation fails
     */
    public void sendBytesMessage(byte[] bytes) throws OXException;

    /**
     * Sends a message containing <code>byte</code>s.
     * 
     * @param bytes The <code>byte</code> array to send
     * @param priority The priority (<code>4</code> is default); range from 0 (lowest) to 9 (highest)
     * @throws OXException If send operation fails
     */
    public void sendBytesMessage(byte[] bytes, int priority) throws OXException;

}
