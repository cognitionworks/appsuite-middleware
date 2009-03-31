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

package com.openexchange.smtp.config;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import com.openexchange.mail.api.MailConfig;
import com.openexchange.mail.config.MailProperties;
import com.openexchange.mail.mime.MIMEDefaultSession;
import com.openexchange.mail.mime.MIMESessionPropertyNames;

/**
 * {@link SMTPSessionProperties} - Default properties for an SMTP session
 * established via <code>JavaMail</code> API
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * 
 */
public final class SMTPSessionProperties {

	private static Properties sessionProperties;

	private static final AtomicBoolean initialized = new AtomicBoolean();

	private static final String STR_TRUE = "true";

	private static final String STR_FALSE = "false";

	/**
	 * No instantiation
	 */
	private SMTPSessionProperties() {
		super();
	}

	/**
	 * Creates a <b>cloned</b> version of default SMTP session properties
	 * 
	 * @return a cloned version of default SMTP session properties
	 */
	public static Properties getDefaultSessionProperties() {
		if (!initialized.get()) {
			synchronized (initialized) {
				if (null == sessionProperties) {
					initializeSMTPProperties();
					initialized.set(true);
				}
			}
		}
		return (Properties) sessionProperties.clone();
	}

	/**
	 * Resets default SMTP session properties
	 */
	public static void resetDefaultSessionProperties() {
		if (initialized.get()) {
			synchronized (initialized) {
				if (null != sessionProperties) {
					sessionProperties = null;
					initialized.set(false);
				}
			}
		}
	}

	/**
	 * This method can only be exclusively accessed
	 */
	private static void initializeSMTPProperties() {
		/*
		 * Define imap properties
		 */
		MIMEDefaultSession.getDefaultSession();
		sessionProperties = ((Properties) (System.getProperties().clone()));
		/*
		 * Set some global JavaMail properties
		 */
		if (!sessionProperties.containsKey(MIMESessionPropertyNames.PROP_MAIL_MIME_BASE64_IGNOREERRORS)) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_MIME_BASE64_IGNOREERRORS, STR_TRUE);
			System.getProperties().put(MIMESessionPropertyNames.PROP_MAIL_MIME_BASE64_IGNOREERRORS, STR_TRUE);
		}
		if (!sessionProperties.containsKey(MIMESessionPropertyNames.PROP_ALLOWREADONLYSELECT)) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_ALLOWREADONLYSELECT, STR_TRUE);
			System.getProperties().put(MIMESessionPropertyNames.PROP_ALLOWREADONLYSELECT, STR_TRUE);
		}
		if (!sessionProperties.containsKey(MIMESessionPropertyNames.PROP_MAIL_MIME_ENCODEEOL_STRICT)) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_MIME_ENCODEEOL_STRICT, STR_TRUE);
			System.getProperties().put(MIMESessionPropertyNames.PROP_MAIL_MIME_ENCODEEOL_STRICT, STR_TRUE);
		}
		if (!sessionProperties.containsKey(MIMESessionPropertyNames.PROP_MAIL_MIME_DECODETEXT_STRICT)) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_MIME_DECODETEXT_STRICT, STR_FALSE);
			System.getProperties().put(MIMESessionPropertyNames.PROP_MAIL_MIME_DECODETEXT_STRICT, STR_FALSE);
		}
		if (!sessionProperties.containsKey(MIMESessionPropertyNames.PROP_MAIL_MIME_CHARSET)) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_MIME_CHARSET, MailProperties.getInstance().getDefaultMimeCharset());
			System.getProperties().put(MIMESessionPropertyNames.PROP_MAIL_MIME_CHARSET,
					MailProperties.getInstance().getDefaultMimeCharset());
		}
		if (SMTPConfig.getSmtpLocalhost() != null) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_SMTPLOCALHOST, SMTPConfig.getSmtpLocalhost());
		}
		if (SMTPConfig.getSmtpTimeout() > 0) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_SMTP_TIMEOUT, String.valueOf(SMTPConfig
					.getSmtpTimeout()));
		}
		if (SMTPConfig.getSmtpConnectionTimeout() > 0) {
			sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_SMTP_CONNECTIONTIMEOUT, String.valueOf(SMTPConfig
					.getSmtpConnectionTimeout()));
		}
		sessionProperties.put(MIMESessionPropertyNames.PROP_MAIL_SMTP_AUTH, SMTPConfig.isSmtpAuth() ? STR_TRUE
				: STR_FALSE);
		if (MailProperties.getInstance().getJavaMailProperties() != null) {
			/*
			 * Overwrite current JavaMail-Specific properties with the ones
			 * defined in javamail.properties
			 */
			sessionProperties.putAll(MailProperties.getInstance().getJavaMailProperties());
		}
	}
}
