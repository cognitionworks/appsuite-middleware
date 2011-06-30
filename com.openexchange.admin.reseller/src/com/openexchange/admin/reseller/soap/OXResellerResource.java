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
package com.openexchange.admin.reseller.soap;

import java.rmi.ConnectException;
import java.rmi.RemoteException;

import com.openexchange.admin.reseller.soap.dataobjects.ResellerContext;
import com.openexchange.admin.rmi.OXResourceInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.Resource;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.DuplicateExtensionException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.NoSuchResourceException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.soap.OXSOAPRMIMapper;

/**
 * SOAP Service implementing RMI Interface OXResourceInterface
 * 
 * @author choeger
 *
 */
/*
 * Note: cannot implement interface OXResourceInterface because method
 * overloading is not supported
 */
public class OXResellerResource extends OXSOAPRMIMapper {

    public OXResellerResource() throws RemoteException {
        super(OXResourceInterface.class);
    }

    private void changeWrapper(final ResellerContext ctx, final Resource res, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        ((OXResourceInterface)rmistub).change(cin, res, auth);
    }
    /**
     * Same as {@link OXResourceInterface#change(Context, Resource, Credentials)}
     * 
     * @param ctx
     * @param res
     * @param auth
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws NoSuchResourceException
     * @throws DuplicateExtensionException 
     */
    public void change(final ResellerContext ctx, final Resource res, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException, DuplicateExtensionException {
        reconnect();
        try {
            changeWrapper(ctx, res, auth);
        } catch( ConnectException e) {
            reconnect(true);
            changeWrapper(ctx, res, auth);
        }
    }

    private Resource createWrapper(final ResellerContext ctx, final Resource res, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        return ((OXResourceInterface)rmistub).create(cin, res, auth);
    }
    /**
     * Same as {@link OXResourceInterface#create(Context, Resource, Credentials)}
     * 
     * @param ctx
     * @param res
     * @param auth
     * @return
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws DuplicateExtensionException 
     */
    public Resource create(final ResellerContext ctx, final Resource res, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException, DuplicateExtensionException {        
        reconnect();
        try {
            return createWrapper(ctx, res, auth);
        } catch( ConnectException e) {
            reconnect(true);
            return createWrapper(ctx, res, auth);
        }
    }

    private void deleteWrapper(final ResellerContext ctx, final Resource res, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        ((OXResourceInterface)rmistub).delete(cin, res, auth);
    }
    /**
     * Same as {@link OXResourceInterface#delete(Context, Resource, Credentials)}
     * 
     * @param ctx
     * @param res
     * @param auth
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws NoSuchResourceException
     * @throws DuplicateExtensionException 
     */
    public void delete(final ResellerContext ctx, final Resource res, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException, NoSuchResourceException, DuplicateExtensionException {
        reconnect();
        try {
            deleteWrapper(ctx, res, auth);
        } catch( ConnectException e) {
            reconnect(true);
            deleteWrapper(ctx, res, auth);
        }
    }

    private Resource getDataWrapper(final ResellerContext ctx, final Resource res, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException, NoSuchResourceException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        return ((OXResourceInterface)rmistub).getData(cin, res, auth);
    }
    /**
     * Same as {@link OXResourceInterface#getData(Context, Resource, Credentials)}
     * 
     * @param ctx
     * @param res
     * @param auth
     * @return
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws NoSuchResourceException
     * @throws DuplicateExtensionException 
     */
    public Resource getData(final ResellerContext ctx, final Resource res, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException, NoSuchResourceException, DuplicateExtensionException {
        reconnect();
        try {
            return getDataWrapper(ctx, res, auth);
        } catch( ConnectException e) {
            reconnect(true);
            return getDataWrapper(ctx, res, auth);
        }
    }

    private Resource[] getMultipleDataWrapper(final ResellerContext ctx, final Resource[] resources, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchResourceException, DatabaseUpdateException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        return ((OXResourceInterface)rmistub).getData(cin, resources, auth);
    }
    /**
     * Same as {@link OXResourceInterface#getData(Context, Resource[], Credentials)}
     * 
     * @param ctx
     * @param resources
     * @param auth
     * @return
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws NoSuchResourceException
     * @throws DatabaseUpdateException
     * @throws DuplicateExtensionException 
     */
    public Resource[] getMultipleData(final ResellerContext ctx, final Resource[] resources, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, NoSuchResourceException, DatabaseUpdateException, DuplicateExtensionException {
        reconnect();
        try {
            return getMultipleDataWrapper(ctx, resources, auth);
        } catch( ConnectException e) {
            reconnect(true);
            return getMultipleDataWrapper(ctx, resources, auth);
        }
    }

    private Resource[] listWrapper(final ResellerContext ctx, final String pattern, final Credentials auth) throws DuplicateExtensionException, RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException, InvalidDataException, DatabaseUpdateException {
        Context cin = ResellerContextUtil.resellerContext2Context(ctx);
        return ((OXResourceInterface)rmistub).list(cin, pattern, auth);
    }
    /**
     * Same as {@link OXResourceInterface#list(Context, String, Credentials)}
     * 
     * @param ctx
     * @param pattern
     * @param auth
     * @return
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws DuplicateExtensionException 
     */
    public Resource[] list(final ResellerContext ctx, final String pattern, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException, DuplicateExtensionException {
        reconnect();
        try {
            return listWrapper(ctx, pattern, auth);
        } catch( ConnectException e) {
            reconnect(true);
            return listWrapper(ctx, pattern, auth);
        }
    }

    /**
     * Same as {@link OXResourceInterface#listAll(Context, Credentials)}
     * 
     * @param ctx
     * @param auth
     * @return
     * @throws RemoteException
     * @throws StorageException
     * @throws InvalidCredentialsException
     * @throws NoSuchContextException
     * @throws InvalidDataException
     * @throws DatabaseUpdateException
     * @throws DuplicateExtensionException 
     */
    public Resource[] listAll(final ResellerContext ctx, final Credentials auth) throws RemoteException, StorageException, InvalidCredentialsException, NoSuchContextException,InvalidDataException, DatabaseUpdateException, DuplicateExtensionException {
        reconnect();
        try {
            return list(ctx, "*", auth);
        } catch( ConnectException e) {
            reconnect(true);
            return list(ctx, "*", auth);
        }
    }

}
