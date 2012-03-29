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

package com.openexchange.solr.internal;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.Types;
import com.openexchange.solr.SolrCore;
import com.openexchange.solr.SolrCoreConfigService;
import com.openexchange.solr.SolrCoreConfiguration;
import com.openexchange.solr.SolrCoreIdentifier;
import com.openexchange.solr.SolrCoreStore;
import com.openexchange.solr.SolrExceptionCodes;
import com.openexchange.solr.SolrProperties;


/**
 * {@link SolrCoreConfigServiceImpl}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class SolrCoreConfigServiceImpl implements SolrCoreConfigService {
    
    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(SolrCoreConfigServiceImpl.class));
    
    private final SolrIndexMysql indexMysql;
    

    public SolrCoreConfigServiceImpl() {
        super();
        indexMysql = SolrIndexMysql.getInstance();
    }
    
    @Override
    public List<SolrCoreStore> getAllStores() throws OXException {
        return indexMysql.getCoreStores();
    }

    @Override
    public int registerCoreStore(final SolrCoreStore store) throws OXException {
        return indexMysql.createCoreStoreEntry(store);
    }

    @Override
    public void modifyCoreStore(final SolrCoreStore store) throws OXException {
        indexMysql.updateCoreStoreEntry(store);
    }

    @Override
    public void unregisterCoreStore(final int storeId) throws OXException {
        indexMysql.removeCoreStoreEntry(storeId);
    }
    
    @Override
    public boolean createCoreEnvironment(final int cid, final int uid, final int module) throws OXException {
        if (indexMysql.createCoreEntry(cid, uid, module)) {
            final SolrCore solrCore = indexMysql.getSolrCore(cid, uid, module);
            final SolrCoreStore store = indexMysql.getCoreStore(solrCore.getStore());
            
            final ConfigurationService config = Services.getService(ConfigurationService.class);
            final URI storeUri = store.getUri();
            final File storeDir = new File(storeUri);
            if (!storeDir.exists()) {
                throw SolrExceptionCodes.CORE_STORE_NOT_EXISTS_ERROR.create(storeDir.getPath());
            }
            
            final String solrHome = config.getProperty(SolrProperties.PROP_SOLR_HOME);      
            final SolrCoreIdentifier identifier = new SolrCoreIdentifier(cid, uid, module);
            final SolrCoreConfiguration coreConfig = new SolrCoreConfiguration(storeUri, identifier);
            final String coreDirPath = coreConfig.getCoreDirPath();
            final File coreDir = new File(coreDirPath);     
            if (coreDir.exists()) {
                LOG.warn("Core directory " + coreDir.getPath() + " already exists. Checking consistency...");
                if (structureIsConsistent(solrHome, coreConfig, module)) {
                    LOG.warn("Core directory " + coreDir.getPath() + " seems to be consistent.");
                    return true;
                }
                /*
                 * If we got here, database and file system seem to be inconsistent.
                 */
                throw SolrExceptionCodes.INSTANCE_DIR_EXISTS.create(coreDirPath);
            }
            
            if (coreDir.mkdir()) {                          
                final File skelDir = getSkelDir(solrHome, module);
                final File confDir = new File(coreConfig.getConfigDirPath());
                final File dataDir = new File(coreConfig.getDataDirPath());
                if (confDir.mkdir() && dataDir.mkdir()) {
                    /*
                     * We successfully created the cores directory structure.
                     * Now we have to copy the necessary configuration files.
                     */
                    try {
                        FileUtils.copyDirectory(skelDir, confDir, false);
                        return true;
                    } catch (IOException e) {
                        LOG.error("Could not initialize core configuration directory with contents from skel. Trying to roll back.");
                    }
                }
            }            

            if (coreDir.exists() && !FileUtils.deleteQuietly(coreDir)) {
                LOG.error("Deleting core directory during rollback failed.");
            }
            
            indexMysql.removeCoreEntry(cid, uid, module);
            return false;
        }
        
        return false;
    }
    
    private boolean structureIsConsistent(final String solrHome, final SolrCoreConfiguration coreConfig, final int module) throws OXException {
        final File coreDir = new File(coreConfig.getCoreDirPath());
        if (isReadableAndWritableDirectory(coreDir)) {
            final File[] files = coreDir.listFiles(new FilenameFilter() {
                
                @Override
                public boolean accept(final File dir, final String name) {
                    if (dir.equals(coreDir)) {
                        if (name.equals(coreConfig.getConfigDirName()) || name.equals(coreConfig.getDataDirName())) {
                            return true;
                        }
                    }

                    return false;
                }
            });
            
            if (files.length == 2) {
                for (final File file : files) {
                    if (!isReadableAndWritableDirectory(file)) {
                        return false;
                    }
                }
            }
            
            final File skelDir = getSkelDir(solrHome, module);
            final File confDir = new File(coreDir, coreConfig.getConfigDirName());
            final String[] confFileNames = confDir.list();
            for (final String fileName : skelDir.list()) {
                if (!ArrayUtils.contains(confFileNames, fileName)) {
                    return false;
                }
                
                try {
                    final long skelChecksum = FileUtils.checksumCRC32(new File(skelDir, fileName));
                    final long confChecksum = FileUtils.checksumCRC32(new File(confDir, fileName));
                    if (skelChecksum != confChecksum) {
                        return false;
                    }
                } catch (IOException e) {
                    return false;
                }
            }
            
            return true;
        }
        
        return false;
    }
    
    private boolean isReadableAndWritableDirectory(final File file) {
        return file.exists() && file.isDirectory() && file.canRead() && file.canWrite();
    }
    
    private File getSkelDir(final String solrHome, int module) throws OXException {
        final String moduleDir;
        switch (module) {
        
        case Types.EMAIL:
            moduleDir = "mail";
            break;
            
        case Types.APPOINTMENT:
            moduleDir = "calendar";
            break;
            
        case Types.CONTACT:
            moduleDir = "contacts";
            break;
            
        case Types.TASK:
            moduleDir = "tasks";
            break;
            
        case Types.INFOSTORE:
            moduleDir = "infostore";
            break;
            
        default:
            throw SolrExceptionCodes.UNKNOWN_MODULE.create(module);
        
        }
        
        final String skelPath = solrHome + IOUtils.DIR_SEPARATOR + "skel" + IOUtils.DIR_SEPARATOR + moduleDir;
        final File skelDir = new File(skelPath);
        if (skelDir.exists()) {
            return skelDir;
        }
        
        throw SolrExceptionCodes.FILE_NOT_EXISTS_ERROR.create(skelPath);
    }
    
    @Override
    public void removeCoreEnvironment(final int cid, final int uid, final int module) throws OXException {
        final SolrCore solrCore = indexMysql.getSolrCore(cid, uid, module);
        final SolrCoreStore store = indexMysql.getCoreStore(solrCore.getStore());
        final URI storeUri = store.getUri();
        final SolrCoreIdentifier identifier = new SolrCoreIdentifier(cid, uid, module);
        final SolrCoreConfiguration config = new SolrCoreConfiguration(storeUri, identifier);
         
        final String coreDirPath = config.getCoreDirPath();
        final File coreDir = new File(coreDirPath);
        if (coreDir.exists()) {
            try {
                FileUtils.forceDelete(coreDir);
            } catch (IOException e) {
                throw new OXException(e);
            } finally {
                indexMysql.removeCoreEntry(cid, uid, module);
            }
        }
    }
    
    @Override
    public boolean coreEnvironmentExists(int contextId, int userId, int module) throws OXException {
        return indexMysql.coreEntryExists(contextId, userId, module);
    }
}
