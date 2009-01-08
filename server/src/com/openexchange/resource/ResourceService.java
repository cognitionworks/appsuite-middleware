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

package com.openexchange.resource;

import java.util.Date;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;

/**
 * {@link ResourceService} - This service defines the API to the resource component.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface ResourceService {

    /**
     * Gets the resource identified by specified ID located in given context.
     * 
     * @param resourceId The unique identifier of the resource to return.
     * @param context The context.
     * @return The data object of the resource.
     * @throws ResourceException If the resource can't be found or an exception appears while reading it.
     */
    public Resource getResource(int resourceId, Context context) throws ResourceException;

    /**
     * Searches all resources which identifier matches the given pattern.
     * 
     * @param pattern The identifier of all returned resources will match this pattern.
     * @param context The context.
     * @return a string array with the resource identifiers. If no identifiers match, an empty array will be returned.
     * @throws ResourceException If an exception occurs while reading from the underlying persistent storage.
     */
    public Resource[] searchResources(String pattern, Context context) throws ResourceException;

    /**
     * Searches all resources which email address matches the given pattern.
     * 
     * @param pattern The email address pattern to search for
     * @param context The context
     * @return An array of {@link Resource resources} whose email address matches the given pattern.
     * @throws ResourceException If searching for resources fails
     */
    public abstract Resource[] searchResourcesByMail(String pattern, Context context) throws ResourceException;

    /**
     * This method returns resources that have been modified since the given timestamp.
     * 
     * @param modifiedSince timestamp after that the resources have been modified.
     * @param context The context.
     * @return an array of resources.
     * @throws ResourceException If an error occurs.
     */
    public abstract Resource[] listModified(Date modifiedSince, Context context) throws ResourceException;

    /**
     * Creates a resource.
     * 
     * @param user The user in whose name the insertion takes place
     * @param ctx The context.
     * @param resource The resource to create.
     * @throws ResourceException If resource insertion fails
     */
    public void create(User user, Context ctx, Resource resource) throws ResourceException;

    /**
     * Updates a resource.
     * 
     * @param user The user in whose name the update takes place
     * @param ctx The context.
     * @param resource The resource to update.
     * @param clientLastModified The client last-modified timestamp; may be <code>null</code> to omit timestamp comparison
     * @throws ResourceException If resource update fails
     */
    public void update(User user, Context ctx, Resource resource, Date clientLastModified) throws ResourceException;

    /**
     * Deletes a resource.
     * 
     * @param user The user in whose name the deletion takes place
     * @param ctx The context.
     * @param resource The resource to delete.
     * @param clientLastModified The client last-modified timestamp; may be <code>null</code> to omit timestamp comparison
     * @throws ResourceException If resource deletion fails
     */
    public void delete(User user, Context ctx, Resource resource, Date clientLastModified) throws ResourceException;
}
