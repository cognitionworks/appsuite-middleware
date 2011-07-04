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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.publish.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.publish.Publication;
import com.openexchange.publish.PublicationDataLoaderService;
import com.openexchange.publish.PublicationException;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserConfigurationService;

/**
 * {@link InfostoreDocumentLoader}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class InfostoreDocumentLoader implements PublicationDataLoaderService {

    private InfostoreFacade infostore;

    private UserService users;

    private UserConfigurationService userConfigs;

    /**
     * Initializes a new {@link InfostoreDocumentLoader}.
     * 
     * @param infostoreFacade
     */
    public InfostoreDocumentLoader(InfostoreFacade infostoreFacade, UserService userService, UserConfigurationService userConfigService) {
        super();
        this.infostore = infostoreFacade;
        this.users = userService;
        this.userConfigs = userConfigService;
    }

    public Collection<? extends Object> load(Publication publication) throws PublicationException {
        ArrayList<InputStream> documents = new ArrayList<InputStream>();
        try {
            InputStream document = infostore.getDocument(
                Integer.parseInt(publication.getEntityId()),
                InfostoreFacade.CURRENT_VERSION,
                publication.getContext(),
                loadUser(publication.getContext(), publication.getUserId()),
                loadUserConfiguration(publication.getContext(), publication.getUserId()));
            documents.add(document);
        } catch (OXException e) {
            throw new PublicationException(e);
        }
        return documents;
    }

    protected User loadUser(Context ctx, int userId) throws PublicationException {
        try {
            return users.getUser(userId, ctx);
        } catch (UserException e) {
            throw new PublicationException(e);
        }
    }

    protected UserConfiguration loadUserConfiguration(Context ctx, int userId) throws PublicationException {
        try {
            return userConfigs.getUserConfiguration(userId, ctx);
        } catch (UserConfigurationException e) {
            throw new PublicationException(e);
        }
    }

}
