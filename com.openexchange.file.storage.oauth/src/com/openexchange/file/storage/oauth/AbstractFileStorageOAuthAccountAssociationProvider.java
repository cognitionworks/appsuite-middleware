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
 *     Copyright (C) 2018-2020 OX Software GmbH
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

package com.openexchange.file.storage.oauth;

import static com.openexchange.java.Autoboxing.I;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.exception.OXException;
import com.openexchange.file.storage.FileStorageAccount;
import com.openexchange.oauth.OAuthUtil;
import com.openexchange.oauth.association.OAuthAccountAssociation;
import com.openexchange.oauth.association.spi.OAuthAccountAssociationProvider;
import com.openexchange.session.Session;

/**
 * {@link AbstractFileStorageOAuthAccountAssociationProvider}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.1
 */
public abstract class AbstractFileStorageOAuthAccountAssociationProvider implements OAuthAccountAssociationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractFileStorageOAuthAccountAssociationProvider.class);

    private final AbstractOAuthFileStorageService storageService;

    /**
     * Initialises a new {@link AbstractFileStorageOAuthAccountAssociationProvider}.
     */
    public AbstractFileStorageOAuthAccountAssociationProvider(AbstractOAuthFileStorageService filestorageService) {
        super();
        this.storageService = filestorageService;
    }

    @Override
    public Collection<OAuthAccountAssociation> getAssociationsFor(int accountId, Session session) throws OXException {
        Collection<OAuthAccountAssociation> associations = null;
        List<FileStorageAccount> accounts = storageService.getAccounts(session);
        for (FileStorageAccount fileStorageAccount : accounts) {
            try {
                if (OAuthUtil.getAccountId(fileStorageAccount.getConfiguration()) != accountId) {
                    continue;
                }
            } catch (IllegalArgumentException e) {
                LOGGER.debug("No association found between file storage account {} and oauth account {} for user {} in context {}.", fileStorageAccount.getId(), I(accountId), I(session.getUserId()), I(session.getContextId()), e);
                continue;
            }
            if (null == associations) {
                associations = new LinkedList<>();
            }
            associations.add(createAssociation(accountId, fileStorageAccount, session.getUserId(), session.getContextId()));
        }

        return null == associations ? Collections.<OAuthAccountAssociation> emptyList() : associations;
    }

    /**
     * Creates a new {@link OAuthAccountAssociation} for the specified {@link FileStorageAccount}
     *
     * @param accountId The OAuthAccount identifier
     * @param account The file storage account
     * @param userId The user identifier
     * @param contextId The context identifier
     */
    public abstract OAuthAccountAssociation createAssociation(int accountId, FileStorageAccount account, int userId, int contextId);
}
