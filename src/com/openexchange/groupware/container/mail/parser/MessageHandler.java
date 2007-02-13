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

package com.openexchange.groupware.container.mail.parser;

import java.util.Date;
import java.util.Map;

import javax.mail.Flags;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Part;
import javax.mail.internet.InternetAddress;

import com.openexchange.api2.OXException;
import com.openexchange.tools.mail.UUEncodedPart;

/**
 * <p>
 * The <code>MessageHandler</code> interface which declares the
 * <code>handleXXX</code> methods which are invoked by the
 * <code>MessageDumper</code> instance on certain parts of a message.
 * </p>
 * 
 * <p>
 * Each methods returns a boolean value which indicates whether the underlying
 * <code>MessageDumper</code> instance should proceed or quit message
 * traversal after method invocation.
 * </p>
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public interface MessageHandler {

	/**
	 * Handle the 'from' message header
	 */
	public boolean handleFrom(InternetAddress[] fromAddrs) throws OXException;

	/**
	 * Handle the recipient message header indicated through
	 * <code>recipientType</code> which is either
	 * <code>RecipientType.TO</code>, <code>RecipientType.CC</code> or
	 * <code>RecipientType.BCC</code>
	 */
	public boolean handleRecipient(Message.RecipientType recipientType, InternetAddress[] recipientAddrs)
			throws OXException;

	/**
	 * Handle message's subject
	 */
	public boolean handleSubject(String subject) throws OXException;

	/**
	 * Handle message's sent date
	 */
	public boolean handleSentDate(Date sentDate) throws OXException;

	/**
	 * Handle message's received date
	 */
	public boolean handleReceivedDate(Date receivedDate) throws OXException;

	/**
	 * Handle those message headers which cannot be handled through a
	 * <code>handleXXX</code> method
	 */
	public boolean handleHeaders(Map<String, String> headerMap) throws OXException;

	/**
	 * Handle message's system flags (//SEEN, //ANSWERED, ...)
	 */
	public boolean handleSystemFlags(Flags.Flag[] systemFlags) throws OXException;

	/**
	 * Handle message's user flags
	 */
	public boolean handleUserFlags(String[] userFlags) throws OXException;

	/**
	 * Handle a plain text inline part (either <code>text/plain</code> or
	 * <code>text/enriched</code>)
	 */
	public boolean handleInlinePlainText(String plainTextContent, String baseContentType, int size, String fileName,
			String id) throws OXException;

	/**
	 * Handle a UUEncoded plain text inline part
	 */
	public boolean handleInlineUUEncodedPlainText(String decodedTextContent, String baseContentType, int size,
			String fileName, String id) throws OXException;

	/**
	 * Handle a UUEncoded file attachment inline part
	 */
	public boolean handleInlineUUEncodedAttachment(UUEncodedPart part, String id) throws OXException;

	/**
	 * Handle a html inline part (<code>text/html</code>)
	 */
	public boolean handleInlineHtml(String htmlContent, String baseContentType, int size, String fileName, String id)
			throws OXException;

	/**
	 * Handle an attachment part (any non-inline parts and file attachments)
	 * @param fileName TODO
	 */
	public boolean handleAttachment(Part part, boolean isInline, String baseContentType, String fileName, String id) throws OXException;

	/**
	 * Handle special parts. A special part is either of MIME type
	 * <code>message/delivery-status</code>,
	 * <code>message/disposition-notification</code>,
	 * <code>text/rfc822-headers</code>, <code>text/x-vcard</code>,
	 * <code>text/vcard</code>, <code>text/calendar</code> or
	 * <code>text/x-vCalendar</code>
	 */
	public boolean handleSpecialPart(Part part, String baseContentType, String id) throws OXException;

	/**
	 * Handle an image part (<code>image/*</code>)
	 */
	public boolean handleImagePart(Part part, String imageCID, String baseContentType, String id) throws OXException;

	/**
	 * Handle a multipart (<code>multipart/*</code>)
	 */
	public boolean handleMultipart(Multipart mp, int bodyPartCount, String id) throws OXException;

	/**
	 * Handle a nested message (<code>message/rfc822</code>)
	 */
	public boolean handleNestedMessage(Message nestedMsg, String id) throws OXException;
	
	/**
	 * Perform some optional finishing operations
	 * @param msg TODO
	 */
	public void handleMessageEnd(Message msg) throws OXException;

}
