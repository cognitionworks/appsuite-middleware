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

package com.openexchange.password.mechanism.impl.algorithm;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import com.openexchange.java.Charsets;

/**
 * {@link SHACrypt}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a> - moved from global & update to new versions
 * @since v7.10.2
 */
public enum SHACrypt {

    // -------------------------------------------------------------------

    /**
     * SHA-1 algorithm.
     *
     * @deprecated Use SHA-256 to generate new password instead
     */
    @Deprecated
    SHA1("{SHA}", "SHA"),
    /**
     * SHA-256 algorithm
     */
    SHA256("{SHA-256}", "SHA-256"),
    /**
     * SHA-512 algorithm.
     * Note: Might not run on all JVMs.
     *
     * @see <a href="https://docs.oracle.com/javase/8/docs/api/java/security/MessageDigest.html">MessageDigest</a>
     *
     */
    SHA512("{SHA-512}", "SHA-512"),
    ;

    private final String identifier;
    private final String algorithm;

    private SHACrypt(String lIdentifier, String algorithm) {
        this.identifier = lIdentifier;
        this.algorithm = algorithm;
    }

    public String makeSHAPasswd(String raw) throws NoSuchAlgorithmException {
        return makeSHAPasswd(raw, null);
    }

    public String makeSHAPasswd(String raw, byte[] salt) throws NoSuchAlgorithmException {
        final MessageDigest sha = MessageDigest.getInstance(algorithm);
        if (null != salt) {
            sha.update(salt);
        }
        sha.update(raw.getBytes(com.openexchange.java.Charsets.UTF_8));
        final byte[] hash = sha.digest();
        return Charsets.toAsciiString(org.apache.commons.codec.binary.Base64.encodeBase64(hash));
    }

    /**
     * Gets the identifier
     *
     * @return The identifier
     */
    public String getIdentifier() {
        return identifier;
    }

}
