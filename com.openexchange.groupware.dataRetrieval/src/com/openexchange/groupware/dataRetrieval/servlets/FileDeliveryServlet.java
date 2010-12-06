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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.groupware.dataRetrieval.servlets;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.ajax.helper.DownloadUtility;
import com.openexchange.ajax.helper.DownloadUtility.CheckedDownload;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.dataRetrieval.Constants;
import com.openexchange.groupware.dataRetrieval.DataProvider;
import com.openexchange.groupware.dataRetrieval.FileMetadata;
import com.openexchange.groupware.dataRetrieval.config.Configuration;
import com.openexchange.groupware.dataRetrieval.registry.DataProviderRegistry;
import com.openexchange.groupware.dataRetrieval.services.Services;
import com.openexchange.session.RandomTokenContainer;
import com.openexchange.tools.io.IOTools;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link FileDeliveryServlet}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class FileDeliveryServlet extends HttpServlet {
    private static final Log LOG = LogFactory.getLog(FileDeliveryServlet.class);
    
    public static RandomTokenContainer<Map<String, Object>> PARAM_MAP = null;
    public static DataProviderRegistry DATA_PROVIDERS = null;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        Configuration configuration = Services.getConfiguration();
        String token = req.getParameter("token");
        if(token == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing parameter 'token");
        }
        Map<String, Object> parameters = PARAM_MAP.get(token);
        if(parameters == null) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        String id = (String) parameters.get(Constants.DATA_PROVDER_KEY);
        ServerSession session = (ServerSession) parameters.get(Constants.SESSION_KEY);
        
        if(configuration.hasExpired((Long)parameters.get(Constants.CREATED))) {
            resp.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        
        DataProvider provider = null;
        Object state = null;
        try {
            provider = DATA_PROVIDERS.getProvider(id);
            
            state = provider.start();
            
            FileMetadata metadata = provider.retrieveMetadata(state, parameters, session);
            
            InputStream stream = provider.retrieve(state, parameters, session);
            stream = setHeaders(stream, metadata, req, resp);
            
            IOTools.copy(stream, resp.getOutputStream());
            if(configuration.expiresAfterAccess()) {
                PARAM_MAP.remove(token);
            }
        } catch (Exception e) {
            LOG.error(e.getMessage(), e);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            if(state != null && provider != null) {
                provider.close(state);
            }
        }
    }

    private InputStream setHeaders(InputStream stream, FileMetadata metadata, HttpServletRequest req, HttpServletResponse resp) throws AbstractOXException {
        if(metadata.getSize() > 0) {
            resp.setContentLength((int) metadata.getSize());
        }
        
        if(metadata.getType() != null) {
            resp.setContentType(metadata.getType());
        }
        
        return stream;
    }
    
}
