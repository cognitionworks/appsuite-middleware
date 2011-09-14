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

package com.openexchange.webdav.protocol;

import org.apache.webdav.lib.WebdavException;
import com.openexchange.exception.Category;
import com.openexchange.exception.LogLevel;
import com.openexchange.exception.OXException;
import com.openexchange.exception.OXExceptionCode;
import com.openexchange.exception.OXExceptionFactory;
import com.openexchange.exception.OXExceptionStrings;
import com.openexchange.groupware.EnumComponent;

/**
 * {@link WebdavProtocolException} - Indicates a WebDAV/XML protocol error.
 * <p>
 * This is a subclass of {@link WebdavException}, therefore its error codes start at <code>1000</code>.
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class WebdavProtocolException extends OXException implements WebdavStatus<Object> {

    public static enum Code implements OXExceptionCode {

        /**
         * A WebDAV error occurred.
         */
        GENERAL_ERROR("A WebDAV error occurred.", CATEGORY_ERROR, 1000),
        /**
         * The folder %s doesn't exist.
         */
        FOLDER_NOT_FOUND("The folder %s doesn't exist.", CATEGORY_ERROR, 1001),
        /**
         * The directory already exists.
         */
        DIRECTORY_ALREADY_EXISTS("The directory already exists.", CATEGORY_ERROR, 1002),
        /**
         * No write permission.
         */
        NO_WRITE_PERMISSION("No write permission.", CATEGORY_PERMISSION_DENIED, 1003),
        /**
         * File &quot;%1$s&quot; already exists
         */
        FILE_ALREADY_EXISTS("File \"%1$s\" already exists.", CATEGORY_ERROR, 1004),
        /**
         * Collections must not have bodies.
         */
        NO_BODIES_ALLOWED("Collections must not have bodies.", CATEGORY_ERROR, 1005),
        /**
         * File "%1$s" does not exist.
         */
        FILE_NOT_FOUND("File \"%1$s\" does not exist.", CATEGORY_ERROR, 1006),
        /**
         * "%1$s" is a directory.
         */
        FILE_IS_DIRECTORY("\"%1$s\" is a directory.", CATEGORY_ERROR, 1007);

        private final String message;

        private final int detailNumber;

        private final Category category;

        private Code(final String message, final Category category, final int detailNumber) {
            this.message = message;
            this.detailNumber = detailNumber;
            this.category = category;
        }

        @Override
        public String getPrefix() {
            return EnumComponent.WEBDAV.getAbbreviation();
        }

        @Override
        public Category getCategory() {
            return category;
        }

        @Override
        public int getNumber() {
            return detailNumber;
        }

        @Override
        public String getMessage() {
            return message;
        }

        @Override
        public boolean equals(final OXException e) {
            return OXExceptionFactory.getInstance().equals(this, e);
        }

        /**
         * Creates a new {@link WebdavProtocolException} instance pre-filled with this code's attributes.
         *
         * @return The newly created {@link WebdavProtocolException} instance
         */
        public WebdavProtocolException create(final WebdavPath url, final int status) {
            return create(url, status);
        }

        /**
         * Creates a new {@link WebdavProtocolException} instance pre-filled with this code's attributes.
         * @param args The message arguments in case of printf-style message
         *
         * @return The newly created {@link WebdavProtocolException} instance
         */
        public WebdavProtocolException create(final WebdavPath url, final int status, final Object... args) {
            return create(url, status, null, args);
        }

        /**
         * Creates a new {@link WebdavProtocolException} instance pre-filled with this code's attributes.
         * @param cause The optional initial cause
         * @param args The message arguments in case of printf-style message
         *
         * @return The newly created {@link WebdavProtocolException} instance
         */
        public WebdavProtocolException create(final WebdavPath url, final int status, final Throwable cause, final Object... args) {
            final Category category = getCategory();
            final WebdavProtocolException ret;
            if (category.getLogLevel().implies(LogLevel.DEBUG)) {
                ret = new WebdavProtocolException(status, url, getNumber(), getMessage(), cause, args);
            } else {
                ret =
                    new WebdavProtocolException(
                        status,
                        url,
                        getNumber(),
                        Category.EnumType.TRY_AGAIN.equals(category.getType()) ? OXExceptionStrings.MESSAGE_RETRY : OXExceptionStrings.MESSAGE,
                        cause,
                        new Object[0]);
                ret.setLogMessage(getMessage(), args);
            }
            ret.addCategory(category);
            ret.setPrefix(getPrefix());
            return ret;
        }
    }

    public static OXException generalError(final Throwable t, final WebdavPath url, final int status) {
        return Code.GENERAL_ERROR.create(url, status, t);
    }

    public static OXException generalError(final WebdavPath url, final int status) {
        return Code.GENERAL_ERROR.create(url, status);
    }

    private static final long serialVersionUID = 617401197355575125L;

    private final int status;

    private final transient WebdavPath url;

    /**
     * No direct instantiation.
     */
    protected WebdavProtocolException(final int status, final WebdavPath url, final int code, final String displayMessage, final Throwable cause, final Object... displayArgs) {
        super(code, displayMessage, cause, displayArgs);
        this.status = status;
        this.url = url;
    }

    public WebdavProtocolException(final WebdavPath url, final int status, final OXException e) {
        super(e);
        this.status = status;
        this.url = url;
    }

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public WebdavPath getUrl() {
        return url;
    }

    @Override
    public Object getAdditional() {
        return null;
    }

    @Override
    public String toString() {
        final String msg = super.toString();
        return new StringBuilder(msg.length() + 64).append(msg).append(' ').append(getUrl()).append(' ').append(getStatus()).toString();
    }

}
