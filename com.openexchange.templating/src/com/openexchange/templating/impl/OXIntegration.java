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

package com.openexchange.templating.impl;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.infostore.DocumentMetadata;
import com.openexchange.groupware.infostore.InfostoreFacade;
import com.openexchange.groupware.infostore.database.impl.DocumentMetadataImpl;
import com.openexchange.groupware.infostore.utils.Metadata;
import com.openexchange.server.impl.OCLPermission;
import com.openexchange.templating.TemplateErrorMessage;
import com.openexchange.tools.iterator.SearchIterator;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.oxfolder.OXFolderManager;
import com.openexchange.tools.session.ServerSession;


/**
 * {@link OXIntegration}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 *
 */
public class OXIntegration implements OXFolderHelper, OXInfostoreHelper {

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(OXIntegration.class));

    private static final String TEMPLATE_FOLDER_NAME = "OXMF Templates";

    private final InfostoreFacade infostore;

    public OXIntegration(final InfostoreFacade infostore) {
        this.infostore = infostore;
    }

    public FolderObject createPrivateTemplateFolder(final ServerSession session) throws OXException {
        final OXFolderManager manager = OXFolderManager.getInstance(session);
        final OXFolderAccess access = getFolderAccess(session);

        final FolderObject parent = access.getDefaultFolder(session.getUserId(), FolderObject.INFOSTORE);

        final FolderObject fo = new FolderObject();
        fo.setParentFolderID(parent.getObjectID());
        fo.setFolderName(TEMPLATE_FOLDER_NAME);

        final OCLPermission adminPermission = new OCLPermission();
        adminPermission.setAllObjectPermission(OCLPermission.READ_ALL_OBJECTS, OCLPermission.WRITE_ALL_OBJECTS, OCLPermission.DELETE_ALL_OBJECTS);
        adminPermission.setFolderAdmin(true);
        adminPermission.setFolderPermission(OCLPermission.ADMIN_PERMISSION);
        adminPermission.setEntity(session.getUserId());

        fo.setPermissions(Arrays.asList(adminPermission));

        fo.setModule(FolderObject.INFOSTORE);
        fo.setType(FolderObject.PUBLIC);
        return manager.createFolder(fo, true, System.currentTimeMillis());
    }

    public FolderObject getGlobalTemplateFolder(final ServerSession session) throws OXException {
        return findTemplatesSubfolder(FolderObject.SYSTEM_PUBLIC_INFOSTORE_FOLDER_ID, getFolderAccess(session), session.getContext());
    }

    public FolderObject getPrivateTemplateFolder(final ServerSession session) throws OXException {
        final OXFolderAccess access = getFolderAccess(session);
        final FolderObject privateInfostoreFolder = access.getDefaultFolder(session.getUserId(), FolderObject.INFOSTORE);
        return findTemplatesSubfolder(privateInfostoreFolder, access, session.getContext());
    }

    private OXFolderAccess getFolderAccess(final ServerSession session) {
        return new OXFolderAccess(session.getContext());
    }

    private FolderObject findTemplatesSubfolder(final int folderId, final OXFolderAccess access, final Context ctx) throws OXException {
        final FolderObject folderObject = access.getFolderObject(folderId);
        return findTemplatesSubfolder(folderObject, access, ctx);
    }

    private FolderObject findTemplatesSubfolder(final FolderObject folderObject, final OXFolderAccess access, final Context ctx) throws OXException {
        try {
            for(final int id : folderObject.getSubfolderIds(true, ctx)) {
                final FolderObject child = access.getFolderObject(id);
                if(child.getFolderName().equals(TEMPLATE_FOLDER_NAME)) {
                    return child;
                }
            }
        } catch (final SQLException e) {
            throw TemplateErrorMessage.SQLException.create(e);
        }
        return null;
    }

    public String findTemplateInFolder(final ServerSession session, final FolderObject folder, final String name) throws OXException {
        final SearchIterator<DocumentMetadata> iterator = infostore.getDocuments(folder.getObjectID(), new Metadata[]{Metadata.ID_LITERAL, Metadata.TITLE_LITERAL, Metadata.FILENAME_LITERAL}, session.getContext(), session.getUser(), session.getUserConfiguration()).results();
        BufferedReader reader = null;
        try {
            final DocumentMetadataMatcher matcher = new DocumentMetadataMatcher(name);
            while(iterator.hasNext() && !matcher.hasPerfectMatch()) {
                matcher.propose(iterator.next());
            }
            final DocumentMetadata metadata = matcher.getBestMatch();

            if(metadata == null) {
                return null;
            }

            final InputStream is = infostore.getDocument(metadata.getId(), InfostoreFacade.CURRENT_VERSION, session.getContext(), session.getUser(), session.getUserConfiguration());

            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            final StringBuilder builder = new StringBuilder();
            String line = null;
            while((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } catch (final UnsupportedEncodingException e) {
            LOG.fatal(e.getMessage(), e);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            throw TemplateErrorMessage.IOException.create(e);
        } finally {
            if(reader != null) {
                try {
                    reader.close();
                } catch (final IOException e) {
                }
            }
            if(iterator != null){
                iterator.close();
            }
        }
        return null;
    }

    public void storeTemplateInFolder(final ServerSession session, final FolderObject folder, final String name, final String templateText) throws OXException {
        final DocumentMetadata metadata = new DocumentMetadataImpl();
        metadata.setFileName(name);
        metadata.setTitle(name);
        metadata.setFolderId(folder.getObjectID());
        metadata.setVersionComment("Created as copy from default template");
        metadata.setFileMIMEType("text/plain");

        try {
            infostore.saveDocument(metadata, new ByteArrayInputStream(templateText.getBytes("UTF-8")), InfostoreFacade.NEW, session);
        } catch (final UnsupportedEncodingException e) {
            LOG.fatal(e.getMessage(), e);
        }
    }

    public List<String> getNames(final ServerSession session, final FolderObject folder, final String ... filter) throws OXException {
    	final HashSet<String> sieve = new HashSet<String>(Arrays.asList(filter));

        final SearchIterator<DocumentMetadata> iterator = infostore.getDocuments(folder.getObjectID(), new Metadata[]{Metadata.FILENAME_LITERAL, Metadata.CATEGORIES_LITERAL}, session.getContext(), session.getUser(), session.getUserConfiguration()).results();
        final List<String> names = new ArrayList<String>(30);
        while(iterator.hasNext()) {
            final DocumentMetadata doc = iterator.next();
            Set<String> categories = null;

            if(doc.getCategories() != null && doc.getCategories().length() > 0){
                categories = new HashSet<String>(Arrays.asList(doc.getCategories().split("\\s*,\\s*")));
            }

			if(categories == null || categories.containsAll(sieve)) {
				names.add(doc.getFileName());
			}
        }
        return names;
    }

}
