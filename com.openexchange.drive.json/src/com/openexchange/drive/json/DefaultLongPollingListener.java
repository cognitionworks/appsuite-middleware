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
 *     Copyright (C) 2016-2020 OX Software GmbH.
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

package com.openexchange.drive.json;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.openexchange.drive.DriveSession;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;

/**
 * {@link DefaultLongPollingListener}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class DefaultLongPollingListener implements LongPollingListener {

    protected DriveSession driveSession;

    /**
     * Initializes a new {@link DefaultLongPollingListener}.
     *
     * @param driveSession The drive session
     */
    protected DefaultLongPollingListener(DriveSession driveSession) {
        super();
        this.driveSession = driveSession;
    }

    @Override
    public DriveSession getSession() {
        return driveSession;
    }

    @Override
    public boolean matches(String tokenRef) {
        String token = extractToken(driveSession);
        return null == tokenRef ? null == token : tokenRef.equals(token) || tokenRef.equals(getMD5(token));
    }

    private static String extractToken(DriveSession driveSession) {
        if (null != driveSession && null != driveSession.getServerSession()) {
            return (String)driveSession.getServerSession().getParameter(DriveSession.PARAMETER_PUSH_TOKEN);
        }
        return null;
    }

    private static String getMD5(String string) {
        if (Strings.isEmpty(string)) {
            return string;
        }
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            byte[] digest = md5.digest(string.getBytes(Charsets.UTF_8));
            return new BigInteger(1, digest).toString(16);
        } catch (NoSuchAlgorithmException e) {
            return null; // ignore
        }
    }

}
