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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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
/*
 * $Id$
 */
package com.openexchange.admin.rmi.impl;

import java.rmi.RemoteException;
import java.util.ArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import com.openexchange.admin.daemons.AdminDaemon;
import com.openexchange.admin.plugins.OXUserPluginInterface;
import com.openexchange.admin.rmi.BasicAuthenticator;
import com.openexchange.admin.rmi.OXLoginInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.admin.storage.interfaces.OXUserStorageInterface;

public class OXLogin extends BasicAuthenticator implements OXLoginInterface{ 
    
    private BundleContext context = null;
    private final Log log = LogFactory.getLog(this.getClass());    
    
    public OXLogin(final BundleContext context) throws RemoteException {
        super();
        this.context = context;
        if (log.isInfoEnabled()) {
            log.info("Class loaded: " + this.getClass().getName());
        }
        
    }


    public void login(Context ctx, Credentials auth) 
        throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException {
        if(ctx==null || auth ==null){
            throw new InvalidDataException();
        }
        
        doUserAuthentication(auth, ctx);
        
    }


    public void login(Credentials auth) 
        throws RemoteException, StorageException, InvalidCredentialsException, InvalidDataException {
        
        if(auth ==null){
            throw new InvalidDataException();
        }
        
        
        
    }


    public User login2User(Context ctx, Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException {
        if(ctx==null || auth ==null){
            throw new InvalidDataException();
        }
        doUserAuthentication(auth, ctx);
        
        final OXToolStorageInterface tools = OXToolStorageInterface.getInstance();
        int user_id = tools.getUserIDByUsername(ctx, auth.getLogin());
        tools.isContextAdmin(ctx, user_id);
        User retval = new User(user_id);
        retval.setUsername(auth.getLogin());
        
        final OXUserStorageInterface oxu = OXUserStorageInterface.getInstance();
        
        User[] retusers = oxu.getData(ctx, new User[]{retval});        
        
        final ArrayList<Bundle> bundles = AdminDaemon.getBundlelist();
        for (final Bundle bundle : bundles) {
            final String bundlename = bundle.getSymbolicName();
            if (Bundle.ACTIVE==bundle.getState()) {
                final ServiceReference[] servicereferences = bundle.getRegisteredServices();
                if (null != servicereferences) {
                    for (final ServiceReference servicereference : servicereferences) {
                        final Object property = servicereference.getProperty("name");
                        if (null != property && property.toString().equalsIgnoreCase("oxuser")) {
                            final OXUserPluginInterface oxuserplugin = (OXUserPluginInterface) this.context.getService(servicereference);
                            if (log.isDebugEnabled()) {
                                log.debug("Calling getData for plugin: " + bundlename);
                            }
                            retusers = oxuserplugin.getData(ctx, retusers, auth);
                        }
                    }
                }
            }
        }
        
        return retusers[0];
        
    }
    
}