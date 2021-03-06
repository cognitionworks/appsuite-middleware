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

package com.openexchange.net.ssl.exception;

import com.openexchange.i18n.LocalizableStrings;

/**
 * {@link SSLExceptionMessages}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.8.3
 */
public class SSLExceptionMessages implements LocalizableStrings {

    // The certificate for domain '%2$s' is untrusted.
    public final static String UNTRUSTED_CERTIFICATE_MSG = "The certificate for domain '%2$s' is untrusted.";

    // The certificate for domain '%2$s' is untrusted. You can change your general trust level in the settings.
    public final static String UNTRUSTED_CERT_USER_CONFIG_MSG = "The certificate for domain '%2$s' is untrusted. You can change your general trust level in the settings.";

    // The certificate is not trusted by the user.
    public final static String USER_DOES_NOT_TRUST_CERTIFICATE = "The certificate with is not trusted by the user.";

    // The root certificate issued by '%2$s' is not trusted
    public final static String UNTRUSTED_ROOT_CERTIFICATE = "The root certificate issued by '%2$s' is not trusted";

    // The certificate is self-signed
    public final static String SELF_SIGNED_CERTIFICATE = "The certificate is self-signed";

    // The certificate is expired
    public final static String CERTIFICATE_IS_EXPIRED = "The certificate is expired";

    // The common name for the certificate is invalid
    public final static String INVALID_COMMON_NAME = "The common name for the certificate is invalid";

    // The root authority for the certificate is untrusted
    public final static String UNTRUSTED_ROOT_AUTHORITY = "The root authority for the certificate is untrusted";

    // The certificate is using a weak algorithm
    public final static String WEAK_ALGORITHM = "The certificate is using a weak algorithm";

    // The certificate was revoked
    public final static String CERTIFICATE_REVOKED = "The certificate was revoked";

    private SSLExceptionMessages() {
        super();
    }

}
