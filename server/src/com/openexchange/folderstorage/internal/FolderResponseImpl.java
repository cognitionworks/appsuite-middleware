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

package com.openexchange.folderstorage.internal;

import java.util.Collection;
import java.util.Collections;
import com.openexchange.folderstorage.FolderResponse;
import com.openexchange.groupware.AbstractOXException;

/**
 * {@link FolderResponseImpl}
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since Open-Xchange v6.16
 */
public final class FolderResponseImpl<R> implements FolderResponse<R> {

    /**
     * Generates a new {@link FolderResponse}.
     * 
     * @param response The response object
     * @param warnings The warnings
     * @return A new {@link FolderResponse}
     */
    public static <R> FolderResponse<R> newFolderResponse(final R response, final Collection<AbstractOXException> warnings) {
        return new FolderResponseImpl<R>(response, warnings);
    }

    private final R response;

    private final Collection<AbstractOXException> warnings;

    /**
     * Initializes a new {@link FolderResponseImpl}.
     * 
     * @param response The response object
     * @param warnings The warnings
     */
    private FolderResponseImpl(final R response, final Collection<AbstractOXException> warnings) {
        super();
        this.response = response;
        this.warnings = null == warnings ? Collections.<AbstractOXException> emptySet() : warnings;
    }

    public R getResponse() {
        return response;
    }

    public Collection<AbstractOXException> getWarnings() {
        return warnings;
    }

}
