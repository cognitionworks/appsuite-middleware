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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.mail.config;

import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.EnumComponent;
import com.openexchange.exception.OXException;
import com.openexchange.mail.MailExceptionCode;

/**
 * {@link MailConfigException} - Errors related to mail configuration
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class MailConfigException extends OXException {

    /**
     * Serial version UID
     */
    private static final long serialVersionUID = -5676002376855401186L;

    /**
     * Initializes a new {@link MailConfigException}
     * 
     * @param cause The initial error
     */
    public MailConfigException(final AbstractOXException cause) {
        super(cause);
    }

    /**
     * Constructs a new exception with the given detail message and cause.
     */
    public MailConfigException(final String message, final Throwable cause) {
        super(
            EnumComponent.MAIL,
            MailExceptionCode.CONFIG_ERROR.getCategory(),
            MailExceptionCode.CONFIG_ERROR.getNumber(),
            MailExceptionCode.CONFIG_ERROR.getMessage(),
            cause);
        super.setMessageArgs(message);
    }

    /**
     * Constructs a new exception with the given detail message.
     */
    public MailConfigException(final String message) {
        super(
            EnumComponent.MAIL,
            MailExceptionCode.CONFIG_ERROR.getCategory(),
            MailExceptionCode.CONFIG_ERROR.getNumber(),
            MailExceptionCode.CONFIG_ERROR.getMessage(),
            null);
        super.setMessageArgs(message);
    }

    /**
     * Constructs a new exception from the given <code>Exception</code> instance.
     */
    public MailConfigException(final Exception e) {
        super(
            EnumComponent.MAIL,
            MailExceptionCode.CONFIG_ERROR.getCategory(),
            MailExceptionCode.CONFIG_ERROR.getNumber(),
            MailExceptionCode.CONFIG_ERROR.getMessage(),
            e);
        super.setMessageArgs(e.getMessage());
    }

}
