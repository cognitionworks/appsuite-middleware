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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.admin.storage.fileStorage;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import com.openexchange.admin.daemons.ClientAdminThread;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.interfaces.OXAuthStorageInterface;
import com.openexchange.admin.tools.GenericChecks;
import com.openexchange.passwordmechs.PasswordMech;

/**
 * Default file implementation for admin auth.
 *
 * @author choeger
 */
public class OXAuthFileStorage extends OXAuthStorageInterface {

    private final static org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(OXAuthFileStorage.class);

    /** */
    public OXAuthFileStorage() {
    }

    /**
     * Authenticates against a textfile
     */
    @Override
    public boolean authenticate(final Credentials authdata) {
        final Credentials master = ClientAdminThread.cache.getMasterCredentials();
        if(master != null && authdata != null &&
           master.getLogin() != null && authdata.getLogin() != null &&
           master.getPassword() != null && authdata.getPassword() != null &&
           master.getLogin().equals(authdata.getLogin())) {
            try {
                PasswordMech passwordMech = PasswordMech.getPasswordMechFor(master.getPasswordMech());
                return GenericChecks.authByMech(master.getPassword(), authdata.getPassword(), passwordMech.getIdentifier());
            } catch (UnsupportedEncodingException | NoSuchAlgorithmException | IllegalArgumentException e) {
                log.error("", e);
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public boolean authenticate(final Credentials authdata, final Context ctx) throws StorageException {
        return false;
    }

    @Override
    public boolean authenticateUser(final Credentials authdata, final Context ctx) throws StorageException {
        return false;
    }

}
