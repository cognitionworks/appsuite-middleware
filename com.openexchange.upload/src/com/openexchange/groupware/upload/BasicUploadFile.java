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

package com.openexchange.groupware.upload;

/**
 * {@link BasicUploadFile} - The basic interface for an uploaded file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface BasicUploadFile {

    /**
     * Gets the file's field name in multipart upload.
     *
     * @return The file's field name in multipart upload.
     */
    String getFieldName();

    /**
     * Gets the file's content type.
     *
     * @return The file's content type.
     */
    String getContentType();

    /**
     * Gets the value of the optional <code>"Content-Id"</code> header.
     *
     * @return The value of the <code>"Content-Id"</code> header or <code>null</code>
     */
    String getContentId();

    /**
     * Gets the file name as given through upload form.
     * <p>
     * The file name possible contains the full path on sender's file system and may be encoded as well; e.g.<br>
     * <code>l=C3=B6l=C3=BCl=C3=96=C3=96=C3=96.txt</code> or <code>C:\MyFolderOnDisk\myfile.dat</code>
     * <p>
     * To ensure to deal with the expected file name call {@link #getPreparedFileName()}.
     *
     * @see #getPreparedFileName()
     * @return The file name.
     */
    String getFileName();

    /**
     * Gets the prepared file name; meaning prepending path and encoding information omitted.
     *
     * @return The prepared file name
     */
    String getPreparedFileName();

    /**
     * Gets the file size in bytes.
     *
     * @return The file size in bytes or <code>-1</code> if unknown
     */
    long getSize();

}
