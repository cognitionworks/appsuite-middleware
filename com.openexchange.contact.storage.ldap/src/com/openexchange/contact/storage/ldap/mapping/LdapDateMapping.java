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

package com.openexchange.contact.storage.ldap.mapping;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import com.openexchange.contact.storage.ldap.LdapExceptionCodes;
import com.openexchange.contact.storage.ldap.id.LdapIDResolver;
import com.openexchange.contact.storage.ldap.internal.LdapResult;
import com.openexchange.exception.OXException;

/**
 *
 * {@link LdapDateMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class LdapDateMapping extends LdapMapping<Date> {

    private static final ThreadLocal<DateFormat> DATE_FORMAT_IN = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    private static final ThreadLocal<DateFormat> DATE_FORMAT_OUT = new ThreadLocal<DateFormat>() {

        @Override
        protected DateFormat initialValue() {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMddHHmmss'.0Z'");
            dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            return dateFormat;
        }
    };

    @Override
    public Date get(LdapResult result, LdapIDResolver idResolver) throws OXException {
        String value = (String)super.getValue(result);
        if (null != value) {
            try {
                return DATE_FORMAT_IN.get().parse(value);
            } catch (ParseException e) {
                throw LdapExceptionCodes.ERROR_GETTING_ATTRIBUTE.create(e, e.getMessage());
            }
        }
        return null;
    }

    @Override
    public String encode(Date value, LdapIDResolver idResolver) throws OXException {
        return null != value ? DATE_FORMAT_OUT.get().format(value) : null;
    }

}