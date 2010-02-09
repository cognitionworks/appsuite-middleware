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

package com.openexchange.folderstorage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * {@link FolderServiceDecorator} - The decorator for {@link FolderService}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class FolderServiceDecorator {

    private TimeZone timeZone;

    private Locale locale;

    private List<ContentType> allowedContentTypes;

    /**
     * Initializes a new {@link FolderServiceDecorator}.
     */
    public FolderServiceDecorator() {
        super();
        allowedContentTypes = Collections.<ContentType> emptyList();
    }

    /**
     * Gets the list of allowed content types or an empty list if all are allowed.
     * 
     * @return The list of allowed content types
     */
    public List<ContentType> getAllowedContentTypes() {
        return allowedContentTypes;
    }

    /**
     * Sets the list of allowed content types or an empty list if all are allowed.
     * 
     * @param allowedContentTypes The list of allowed content types
     * @return This decorator with allowed content types applied
     */
    public FolderServiceDecorator setAllowedContentTypes(final List<ContentType> allowedContentTypes) {
        this.allowedContentTypes =
            (null == allowedContentTypes || allowedContentTypes.isEmpty()) ? Collections.<ContentType> emptyList() : new ArrayList<ContentType>(
                allowedContentTypes);
        return this;
    }

    /**
     * Gets the time zone.
     * 
     * @return The time zone or <code>null</code>
     */
    public TimeZone getTimeZone() {
        return timeZone;
    }

    /**
     * Sets the time zone.
     * 
     * @param timeZone The time zone to set
     * @return This decorator with time zone applied
     */
    public FolderServiceDecorator setTimeZone(final TimeZone timeZone) {
        this.timeZone = timeZone;
        return this;
    }

    /**
     * Gets the locale.
     * 
     * @return The locale or <code>null</code>
     */
    public Locale getLocale() {
        return locale;
    }

    /**
     * Sets the locale.
     * 
     * @param locale The locale to set
     * @return This decorator with locale applied
     */
    public FolderServiceDecorator setLocale(final Locale locale) {
        this.locale = locale;
        return this;
    }

}
