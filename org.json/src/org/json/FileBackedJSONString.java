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

package org.json;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * {@link FileBackedJSONString} - A JSON string backed by a file.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.0
 */
public interface FileBackedJSONString extends JSONString, Closeable, CharSequence {

    /**
     * Gets the optional temporary file.
     * <p>
     * If {@link #isInMemory()} signals <code>true</code>, then this method will return <code>null</code>, and the content should rather be obtained by {@link #getBuffer()}.
     *
     * @return The temporary file or <code>null</code>
     * @see #isInMemory()
     */
    File getTempFile();

    /**
     * Writes a single character.
     *
     * @param c The character to be written
     * @throws IOException If an I/O error occurs
     */
    void write(int c) throws IOException;

    /**
     * Writes an array of characters.
     *
     * @param cbuf The characters to be written
     * @throws IOException If an I/O error occurs
     */
    void write(char cbuf[]) throws IOException;

    /**
     * Writes a portion of an array of characters.
     *
     * @param cbuf The array of characters
     * @param off The offset from which to start writing characters
     * @param len The number of characters to write
     * @throws IOException If an I/O error occurs
     */
    void write(char cbuf[], int off, int len) throws IOException;

    /**
     * Writes a string.
     *
     * @param str The string to be written
     * @throws IOException If an I/O error occurs
     */
    void write(String str) throws IOException;

    /**
     * Flushes this instance.
     *
     * @exception IOException If an I/O error occurs
     */
    void flush() throws IOException;

}
