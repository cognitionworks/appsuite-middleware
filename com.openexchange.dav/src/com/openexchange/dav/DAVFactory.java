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

package com.openexchange.dav;

import static com.openexchange.dav.DAVTools.getExternalPath;
import static com.openexchange.dav.DAVTools.removePathPrefixFromPath;
import static com.openexchange.dav.DAVTools.removePrefixFromPath;
import static com.openexchange.dav.DAVTools.startsWithPrefix;
import com.openexchange.config.cascade.ConfigViewFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.java.Strings;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.session.SessionHolder;
import com.openexchange.tools.session.ServerSessionAdapter;
import com.openexchange.user.User;
import com.openexchange.webdav.protocol.Protocol;
import com.openexchange.webdav.protocol.WebdavPath;
import com.openexchange.webdav.protocol.helpers.AbstractWebdavFactory;

/**
 * {@link DAVFactory}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.8.1
 */
public abstract class DAVFactory extends AbstractWebdavFactory implements SessionHolder, ServiceLookup {

    public static final WebdavPath ROOT_URL = new WebdavPath();

    private final Protocol protocol;
    private final SessionHolder sessionHolder;
    private final ServiceLookup services;

    /**
     * Initializes a new {@link DAVFactory}.
     *
     * @param protocol The protocol
     * @param services A service lookup reference
     * @param sessionHolder The session holder to use
     */
    public DAVFactory(Protocol protocol, ServiceLookup services, SessionHolder sessionHolder) {
        super();
        this.protocol = protocol;
        this.sessionHolder = sessionHolder;
        this.services = services;
    }

    @Override
    public Protocol getProtocol() {
        return protocol;
    }

    @Override
    public Context getContext() {
        return sessionHolder.getContext();
    }

    @Override
    public Session getSessionObject() {
        return sessionHolder.getSessionObject();
    }

    @Override
    public Session getSession() {
        return getSessionObject();
    }

    @Override
    public User getUser() {
        return sessionHolder.getUser();
    }

    public UserConfiguration getUserConfiguration() {
        return ServerSessionAdapter.valueOf(getSession(), getContext(), getUser()).getUserConfiguration();
    }

    @Override
    public <S> S getService(Class<? extends S> clazz) {
        return services.getService(clazz);
    }

    @Override
    public <S> S getOptionalService(Class<? extends S> clazz) {
        return services.getOptionalService(clazz);
    }

    public <S> S requireService(Class<? extends S> clazz) throws OXException {
        S service = services.getService(clazz);
        if (null == service) {
            throw ServiceExceptionCode.absentService(clazz);
        }
        return service;
    }

    /**
     * Gets a value indicating whether the supplied WebDAV path denotes the root path or not.
     *
     * @param url The WebDAV path to check
     * @return <code>true</code> if it's the root path, <code>false</code>, otherwise
     */
    protected boolean isRoot(WebdavPath url) {
        return 0 == url.size();
    }

    /**
     * Sanitizes the supplied WebDAV path by stripping the implicit URL prefix of the DAV servlet this factory is responsible for.
     *
     * @param url The WebDAV path to sanitize
     * @return The sanitized path
     */
    protected WebdavPath sanitize(WebdavPath url) {
        ConfigViewFactory configViewFactory = getService(ConfigViewFactory.class);

        /*
         * Build relative URLs
         */
        String prefix = removePathPrefixFromPath(configViewFactory, getURLPrefix());
        String urlPath = removePathPrefixFromPath(configViewFactory, url.toString());

        /*
         * Remove the path this factory is responsible for, if present
         */
        if (Strings.isNotEmpty(prefix) && false == prefix.equals("/") && startsWithPrefix(urlPath, prefix)) {
            return new WebdavPath(removePrefixFromPath(prefix, urlPath));
        }
        return new WebdavPath(urlPath);
    }

    /**
     * Gets the URL prefix of the DAV servlet this factory is responsible for.
     *
     * @return The URL prefix, e.g. <code>/principals/users/</code>
     */
    public abstract String getURLPrefix();

    /**
     * Gets the full qualified path this factory is responsible for.
     *
     * @param path The relative path of factory is responsible for.
     * @return The full qualified path considering configuration.
     */
    protected String getURLPrefix(String path) {
        return getExternalPath(getService(ConfigViewFactory.class), path);
    }

}
