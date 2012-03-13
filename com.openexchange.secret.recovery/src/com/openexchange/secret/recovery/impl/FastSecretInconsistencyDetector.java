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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.secret.recovery.impl;

import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.crypto.CryptoService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.secret.SecretService;
import com.openexchange.secret.recovery.EncryptedItemDetectorService;
import com.openexchange.secret.recovery.SecretInconsistencyDetector;
import com.openexchange.secret.recovery.SecretMigrator;
import com.openexchange.tools.session.ServerSession;
import com.openexchange.user.UserService;

/**
 * {@link FastSecretInconsistencyDetector}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FastSecretInconsistencyDetector implements SecretInconsistencyDetector, SecretMigrator {

    private static final Log LOG = LogFactory.getLog(FastSecretInconsistencyDetector.class);

    private static final String PROPERTY = "com.openexchange.secret.recovery.fast.token";

    private final SecretService secretService;

    private final CryptoService cryptoService;

    private final UserService userService;

    private final EncryptedItemDetectorService detector;
    
    public FastSecretInconsistencyDetector(final SecretService secretService, final CryptoService cryptoService, final UserService userService, final EncryptedItemDetectorService detector) {
        this.secretService = secretService;
        this.cryptoService = cryptoService;
        this.userService = userService;
        this.detector = detector;
    }

    private static final String testString = "supercalifragilisticexplialidocious";

    @Override
    public String isSecretWorking(final ServerSession session) throws OXException {
        final String secret = secretService.getSecret(session);
        final Set<String> token = session.getUser().getAttributes().get(PROPERTY);
        if (token == null || token.isEmpty()) {
            saveNewToken(session.getUser(), secret, session.getContext());
            return null;
        }

        if (canDecrypt(token.iterator().next(), secret)) {
            return null;
        }
        
        if (detector.hasEncryptedItems(session)) {
            return "Could not decrypt token";
        }
        saveNewToken(session.getUser(), secret, session.getContext());
        return null;
    }

    private boolean canDecrypt(final String next, final String secret) {
        try {
            return cryptoService.decrypt(next, secret).equals(testString);
        } catch (final OXException e) {
            return false;
        }
    }

    private void saveNewToken(final User user, final String secret, final Context context) {
        try {
            userService.setAttribute(PROPERTY, cryptoService.encrypt(testString, secret), user.getId(), context);
        } catch (final OXException e) {
            LOG.error(e.getMessage(), e);
        }
    }

    @Override
    public void migrate(final String oldSecret, final String newSecret, final ServerSession session) throws OXException {
        saveNewToken(session.getUser(), newSecret, session.getContext());
    }

}
