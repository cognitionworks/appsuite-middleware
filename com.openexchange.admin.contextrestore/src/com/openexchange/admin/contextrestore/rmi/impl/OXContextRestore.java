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

package com.openexchange.admin.contextrestore.rmi.impl;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.Flushable;
import java.io.IOException;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import com.openexchange.admin.contextrestore.dataobjects.UpdateTaskEntry;
import com.openexchange.admin.contextrestore.dataobjects.UpdateTaskInformation;
import com.openexchange.admin.contextrestore.dataobjects.VersionInformation;
import com.openexchange.admin.contextrestore.osgi.Activator;
import com.openexchange.admin.contextrestore.rmi.OXContextRestoreInterface;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException;
import com.openexchange.admin.contextrestore.rmi.exceptions.OXContextRestoreException.Code;
import com.openexchange.admin.contextrestore.rmi.impl.OXContextRestore.Parser.PoolIdSchemaAndVersionInfo;
import com.openexchange.admin.contextrestore.storage.interfaces.OXContextRestoreStorageInterface;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.DatabaseUpdateException;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.rmi.impl.BasicAuthenticator;
import com.openexchange.admin.rmi.impl.OXCommonImpl;
import com.openexchange.admin.storage.interfaces.OXToolStorageInterface;
import com.openexchange.log.LogFactory;

/**
 * This class contains the implementation of the API defined in {@link OXContextRestoreInterface}
 *
 * @author <a href="mailto:dennis.sieben@open-xchange.com">Dennis Sieben</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>: Bugfix 20044
 */
public class OXContextRestore extends OXCommonImpl implements OXContextRestoreInterface {

    /**
     * Safely closes specified {@link Closeable} instance.
     *
     * @param toClose The {@link Closeable} instance
     */
    protected static void close(final Closeable toClose) {
        if (null != toClose) {
            try {
                toClose.close();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Safely flushes specified {@link Flushable} instance.
     *
     * @param toFlush The {@link Flushable} instance
     */
    protected static void flush(final Flushable toFlush) {
        if (null != toFlush) {
            try {
                toFlush.flush();
            } catch (final Exception e) {
                // Ignore
            }
        }
    }

    /**
     * Parser for MySQL dump files.
     */
    public static class Parser {

        public class PoolIdSchemaAndVersionInfo {

            private final int poolId;
            private final int contextId;
            private final String schema;
            private final String fileName;
            private VersionInformation versionInformation;
            private UpdateTaskInformation updateTaskInformation;

            protected PoolIdSchemaAndVersionInfo(final String fileName, final int contextId, int poolId, String schema, VersionInformation versionInformation, UpdateTaskInformation updateTaskInformation) {
                super();
                this.fileName = fileName;
                this.contextId = contextId;
                this.poolId = poolId;
                this.schema = schema;
                this.versionInformation = versionInformation;
                this.updateTaskInformation = updateTaskInformation;
            }

            public String getFileName() {
                return fileName;
            }

            public int getContextId() {
                return contextId;
            }

            public final int getPoolId() {
                return poolId;
            }

            public final String getSchema() {
                return schema;
            }

            public final VersionInformation getVersionInformation() {
                return versionInformation;
            }

            public final void setVersionInformation(VersionInformation versionInformation) {
                this.versionInformation = versionInformation;
            }

            public UpdateTaskInformation getUpdateTaskInformation() {
                return updateTaskInformation;
            }

            public void setUpdateTaskInformation(UpdateTaskInformation updateTaskInformation) {
                this.updateTaskInformation = updateTaskInformation;
            }

        }

        private final static Pattern database = Pattern.compile("^.*?\\s+Database:\\s+`?([^` ]*)`?.*$");

        private final static Pattern table = Pattern.compile("^Table\\s+structure\\s+for\\s+table\\s+`([^`]*)`.*$");

        private final static Pattern cidpattern = Pattern.compile(".*`cid`.*");

        private final static Pattern engine = Pattern.compile("^\\).*ENGINE=.*.*$");

        private final static Pattern foreignkey =
            Pattern.compile("^\\s+CONSTRAINT.*FOREIGN KEY\\s+\\(`([^`]*)`(?:,\\s+`([^`]*)`)*\\)\\s+REFERENCES `([^`]*)`.*$");

        private final static Pattern datadump = Pattern.compile("^Dumping\\s+data\\s+for\\s+table\\s+`([^`]*)`.*$");

        private final static Pattern insertIntoVersion =
            Pattern.compile("^INSERT INTO `version` VALUES \\((?:([^\\),]*),)(?:([^\\),]*),)(?:([^\\),]*),)(?:([^\\),]*),)([^\\),]*)\\).*$");

        /**
         * Starts parsing named MySQL dump file
         *
         * @param cid The context identifier
         * @param fileName The name of the MySQL dump file
         * @return The information object for parsed MySQL dump file
         * @throws IOException If an I/O error occurs
         * @throws OXContextRestoreException If a context restore error occurs
         */
        public PoolIdSchemaAndVersionInfo start(final int cid, final String fileName) throws IOException, OXContextRestoreException {
            int c;
            int state = 0;
            int oldstate = 0;
            int cidpos = -1;
            String tableName = null;
            // Set if a database is found in which the search for cid should be done
            boolean furthersearch = true;
            // Defines if we have found a contextserver2pool table
            boolean searchcontext = false;
            // boolean searchdbpool = false;
            int poolId = -1;
            String schema = null;
            VersionInformation versionInformation = null;
            UpdateTaskInformation updateTaskInformation = null;

            final BufferedReader in = new BufferedReader(new FileReader(fileName));
            BufferedWriter bufferedWriter = null;
            try {
                while ((c = in.read()) != -1) {
                    if (0 == state && c == '-') {
                        state = 1; // Started comment line
                        continue;
                    } else if (1 == state) {
                        if (c == '-') {
                            state = 2; // Read comment prefix "--"
                            continue;
                        }
                        // Not a comment prefix; an interpretable line
                        state = oldstate;
                        continue;
                    } else if (2 == state) {
                        if (c == ' ') { // Comment line: "-- " + <rest-of-line>
                            final String readLine = in.readLine();
                            final Matcher dbmatcher = database.matcher(readLine);
                            final Matcher tablematcher = table.matcher(readLine);
                            final Matcher datadumpmatcher = datadump.matcher(readLine);

                            if (dbmatcher.matches()) {
                                // Database found
                                final String databasename = dbmatcher.group(1);
                                if ("mysql".equals(databasename) || "information_schema".equals(databasename)) {
                                    furthersearch = false;
                                } else {
                                    furthersearch = true;
                                }
                                LOG.info("Database: " + databasename);
                                if (null != bufferedWriter) {
                                    bufferedWriter.append("/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;\n");
                                    bufferedWriter.flush();
                                    bufferedWriter.close();
                                }

                                final String file = "/tmp/" + databasename + ".txt";
                                bufferedWriter = new BufferedWriter(new FileWriter(file));
                                bufferedWriter.append("/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;\n");
                                // Reset values
                                cidpos = -1;
                                state = 0;
                                oldstate = 0;
                            } else if (furthersearch && tablematcher.matches()) {
                                // Table found
                                tableName = tablematcher.group(1);
                                LOG.info("Table: " + tableName);
                                cidpos = -1;
                                oldstate = 0;
                                state = 3;
                            } else if (furthersearch && datadumpmatcher.matches()) {
                                // Content found
                                LOG.info("Dump found");
                                if ("updateTask".equals(tableName)) {
                                    // One or more entries for 'updateTask' table
                                    if (null == updateTaskInformation) {
                                        updateTaskInformation = new UpdateTaskInformation();
                                    }
                                    searchAndCheckUpdateTask(in, cid, updateTaskInformation);
                                }
                                if ("version".equals(tableName)) {
                                    // The version table is quite small so it is safe to read the whole line here:
                                    versionInformation = searchAndCheckVersion(in);
                                }
                                if ("context_server2db_pool".equals(tableName)) {
                                    searchcontext = true;
                                }
                                // if ("db_pool".equals(table_name)) {
                                // // As the table in the dump are sorted alphabetically it's safe to
                                // // assume that we have the pool id here
                                // searchdbpool = true;
                                // }
                                state = 5;
                                oldstate = 0;
                            } else {
                                state = 0;
                                oldstate = 0;
                            }
                            continue;
                        }
                        // Reset to old state
                        state = oldstate;
                    } else if (3 == state && c == 'C') {
                        final String creatematchpart = "REATE";
                        state = returnRightStateToString(in, creatematchpart, 4, 3);
                        continue;
                    } else if (3 == state && c == '-') {
                        oldstate = 3;
                        state = 1;
                        continue;
                    } else if (4 == state && c == '(') {
                        cidpos = cidsearch(in);
                        LOG.info("Cid pos: " + cidpos);
                        state = 0;
                        continue;
                    } else if (5 == state && c == 'I') {
                        state = returnRightStateToString(in, "NSERT", 6, 5);
                        continue;
                    } else if (5 == state && c == '-') {
                        oldstate = 5;
                        state = 1;
                    } else if (6 == state && c == '(') {
                        LOG.info("Insert found and cid=" + cidpos);
                        // Now we search for matching cids and write them to the tmp file
                        if (searchcontext && null != bufferedWriter) {
                            final String value[] =
                                searchAndWriteMatchingCidValues(in, bufferedWriter, cidpos, Integer.toString(cid), tableName, true, true);
                            if (value.length >= 2) {
                                try {
                                    poolId = Integer.parseInt(value[1]);
                                } catch (final NumberFormatException e) {
                                    throw new OXContextRestoreException(Code.COULD_NOT_CONVERT_POOL_VALUE);
                                }
                                schema = value[2];
                                // } else if (searchdbpool) {
                                // final String value[] = searchAndWriteMatchingCidValues(in, bufferedWriter, 1, Integer.toString(pool_id),
                                // table_name, true, false);
                                // searchdbpool = false;
                                // System.out.println(Arrays.toString(value));
                            } else {
                                throw new OXContextRestoreException(Code.CONTEXT_NOT_FOUND_IN_POOL_MAPPING);
                            }
                        } else if (null != bufferedWriter) {
                            // Here we should only search if a fitting db was found and thus the writer was set
                            searchAndWriteMatchingCidValues(in, bufferedWriter, cidpos, Integer.toString(cid), tableName, false, true);
                        }
                        searchcontext = false;
                        oldstate = 0;
                        state = 5;
                    }
                    // Reset state machine at the end of the line if we are in the first two states
                    if (3 > state && c == '\n') {
                        state = 0;
                        continue;
                    }
                }
            } finally {
                flush(bufferedWriter);
                close(bufferedWriter);
                close(in);
            }
            //if (null == updateTaskInformation) {
            //    throw new OXContextRestoreException(Code.NO_UPDATE_TASK_INFORMATION_FOUND);
            // }
            return new PoolIdSchemaAndVersionInfo(fileName, cid, poolId, schema, versionInformation, updateTaskInformation);
        }

        /**
         * @param in
         * @return
         * @throws IOException
         */
        private VersionInformation searchAndCheckVersion(final BufferedReader in) throws IOException {
            String readLine2 = in.readLine();
            while ((readLine2 = in.readLine()) != null && !readLine2.equals("--")) {
                final Matcher matcher = insertIntoVersion.matcher(readLine2);
                if (matcher.matches()) {
                    final int version = Integer.parseInt(matcher.group(1));
                    final int locked = Integer.parseInt(matcher.group(2));
                    final int gw_compatible = Integer.parseInt(matcher.group(3));
                    final int admin_compatible = Integer.parseInt(matcher.group(4));
                    final String server = matcher.group(5);

                    return new VersionInformation(
                        admin_compatible,
                        gw_compatible,
                        locked,
                        server.substring(1, server.length() - 1),
                        version);
                }
            }
            return null;
        }

        private final static String REGEX_VALUE = "([^\\),]*)";
        private final static Pattern insertIntoUpdateTaskValues =
            Pattern.compile("\\((?:" + REGEX_VALUE + ",)(?:" + REGEX_VALUE + ",)(?:" + REGEX_VALUE + ",)" + REGEX_VALUE + "\\)");

        private UpdateTaskInformation searchAndCheckUpdateTask(final BufferedReader in, final int contextId, final UpdateTaskInformation updateTaskInformation) throws IOException {
            StringBuilder insert = null;
            String line;
            {
                boolean eoi = false;
                while (!eoi && (line = in.readLine()) != null && !line.startsWith("--")) {
                    if (null == insert) {
                        if (line.startsWith("INSERT INTO `updateTask` VALUES ")) {
                            // Start collecting lines
                            insert = new StringBuilder(2048);
                            insert.append(line);
                        }
                    } else {
                        insert.append(line);
                        if (line.endsWith(");")) {
                            eoi = true;
                        }
                    }
                }
            }
            if (null != insert) {
                final Matcher matcher = insertIntoUpdateTaskValues.matcher(insert.substring(32));
                insert = null;
                while (matcher.find()) {
                    final UpdateTaskEntry updateTaskEntry = new UpdateTaskEntry();
                    final int contextId2 = Integer.parseInt(matcher.group(1));
                    if (contextId2 <= 0 || contextId2 == contextId) {
                        updateTaskEntry.setContextId(contextId2);
                        updateTaskEntry.setTaskName(matcher.group(2));
                        updateTaskEntry.setSuccessful((Integer.parseInt(matcher.group(3)) > 0));
                        updateTaskEntry.setLastModified(Long.parseLong(matcher.group(4)));
                        updateTaskInformation.add(updateTaskEntry);
                    }
                }
            }
            return updateTaskInformation;
        }

        /**
         * @param in
         * @param bufferedWriter
         * @param valuepos The position of the value inside the value row
         * @param value The value itself
         * @param table_name
         * @param readall If the rest of the row should be returned as string array after a match or not
         * @param contextsearch
         * @throws IOException
         */
        private String[] searchAndWriteMatchingCidValues(final BufferedReader in, final Writer bufferedWriter, final int valuepos, final String value, final String table_name, boolean readall, boolean contextsearch) throws IOException {
            final StringBuilder currentValues = new StringBuilder();
            currentValues.append("(");
            final StringBuilder lastpart = new StringBuilder();
            int c = 0;
            int counter = 1;
            // If we are inside a string '' or not
            boolean instring = false;
            // If we are inside a dataset () or not
            boolean indatarow = true;
            // Have we found the value we searched for?
            boolean found = false;
            // Is this the first time we found the value
            boolean firstfound = true;
            // Are we in escapted mode
            boolean escapted = false;
            // Used for only escaping one char
            boolean firstescaperun = false;
            // Used to leave the loop
            boolean continuation = true;
            final ArrayList<String> retval = new ArrayList<String>();
            while ((c = in.read()) != -1 && continuation) {
                if (firstescaperun && escapted) {
                    escapted = false;
                    firstescaperun = false;
                }
                if (escapted) {
                    firstescaperun = true;
                }
                switch (c) {
                case '(':
                    if (!indatarow) {
                        indatarow = true;
                        currentValues.setLength(0);
                        currentValues.append('(');
                    } else {
                        currentValues.append((char) c);
                    }
                    break;
                case ')':
                    if (indatarow) {
                        if (!instring) {
                            if (counter == valuepos) {
                                if (lastpart.toString().equals(value)) {
                                    found = true;
                                }
                            } else if (readall && found) {
                                retval.add(lastpart.toString());
                            }
                            lastpart.setLength(0);
                            indatarow = false;
                            if (found && contextsearch) {
                                if (firstfound) {
                                    bufferedWriter.write("INSERT INTO `");
                                    bufferedWriter.write(table_name);
                                    bufferedWriter.write("` VALUES ");
                                    firstfound = false;
                                } else {
                                    bufferedWriter.write(",");
                                }

                                bufferedWriter.write(currentValues.toString());
                                bufferedWriter.write(")");
                                bufferedWriter.flush();
                                found = false;
                            }
                        }
                        currentValues.append((char) c);
                    }
                    break;
                case ',':
                    if (indatarow) {
                        if (!instring) {
                            if (counter == valuepos) {
                                if (lastpart.toString().equals(value)) {
                                    found = true;
                                }
                            } else if (readall && found) {
                                retval.add(lastpart.toString());
                            }
                            counter++;
                            lastpart.setLength(0);
                        }
                        currentValues.append((char) c);
                    } else {
                        // New datarow comes
                        counter = 1;
                    }
                    break;
                case '\'':
                    if (indatarow) {
                        if (!instring) {
                            instring = true;
                        } else {
                            if (!escapted) {
                                instring = false;
                            }
                        }
                        currentValues.append((char) c);
                    }
                    break;
                case '\\':
                    if (indatarow) {
                        if (instring && !escapted) {
                            escapted = true;
                        }
                        currentValues.append((char) c);
                    }
                    break;
                case ';':
                    if (!indatarow) {
                        if (!firstfound && contextsearch) {
                            // End of VALUES part
                            bufferedWriter.write(";");
                            bufferedWriter.write("\n");
                        }
                        continuation = false;
                    } else {
                        currentValues.append((char) c);
                    }
                    break;
                default:
                    if (indatarow) {
                        lastpart.append((char) c);
                        currentValues.append((char) c);
                    }
                    break;
                }
            }
            return retval.toArray(new String[retval.size()]);
        }

        private int returnRightStateToString(final BufferedReader in, final String string, int successstate, int failstate) throws IOException {
            final int length = string.length();
            char[] arr = new char[length];
            int i;
            if ((i = in.read(arr)) != -1 && length == i) {
                if (string.equals(new String(arr))) {
                    return successstate;
                }
                return failstate;
            }
            // File at the end or no more chars
            return -1;
        }

        /**
         * Searches for the cid and returns the line number in which is was found, after this method the reader's position is behind the
         * create structure
         *
         * @param in
         * @return
         * @throws IOException
         */
        private int cidsearch(final BufferedReader in) throws IOException {
            String readLine;
            readLine = in.readLine();
            int columnpos = 0;
            boolean found = false;
            while (null != readLine) {
                final Matcher cidmatcher = cidpattern.matcher(readLine);
                final Matcher enginematcher = engine.matcher(readLine);
                // Now searching for cid text...
                if (cidmatcher.matches()) {
                    final List<String> searchingForeignKey = searchingForeignKey(in);
                    LOG.info("Foreign Keys: " + searchingForeignKey);
                    found = true;
                    break;
                } else if (enginematcher.matches()) {
                    break;
                }
                columnpos++;
                readLine = in.readLine();
            }
            if (!found) {
                return -1;
            }
            return columnpos;
        }

        private List<String> searchingForeignKey(final BufferedReader in) throws IOException {
            String readLine;
            readLine = in.readLine();
            List<String> foreign_keys = null;
            while (null != readLine) {
                final Matcher matcher = foreignkey.matcher(readLine);
                final Matcher enginematcher = engine.matcher(readLine);
                if (matcher.matches()) {
                    foreign_keys = get_foreign_keys(matcher);
                } else if (enginematcher.matches()) {
                    return foreign_keys;
                }
                readLine = in.readLine();
            }
            return null;
        }

        private List<String> get_foreign_keys(Matcher matcher) {
            final ArrayList<String> retval = new ArrayList<String>();
            final int groupCount = matcher.groupCount();
            for (int i = 1; i < groupCount; i++) {
                final String group = matcher.group(i);
                if (null != group) {
                    retval.add(group);
                }
            }
            return retval;
        }

    }

    protected final static Log LOG = LogFactory.getLog(OXContextRestore.class);

    private final BasicAuthenticator basicauth;

    public OXContextRestore() throws StorageException {
        super();
        basicauth = new BasicAuthenticator();
    }

    @Override
    public String restore(final Context ctx, final String[] fileNames, final Credentials auth, final boolean dryrun) throws InvalidDataException, InvalidCredentialsException, StorageException, OXContextRestoreException, DatabaseUpdateException {
        try {
            doNullCheck(ctx, fileNames);
            for (final String filename : fileNames) {
                doNullCheck(filename);
            }
        } catch (final InvalidDataException e) {
            LOG.error("One of the arguments for restore is null", e);
            throw e;
        }

        try {
            basicauth.doAuthentication(auth);
        } catch (final InvalidCredentialsException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }

        final Parser parser = new Parser();
        LOG.info("Context: " + ctx);
        LOG.info("Filenames: " + java.util.Arrays.toString(fileNames));

        try {
            VersionInformation versionInfo = null;
            UpdateTaskInformation updateTaskInfo = null;
            PoolIdSchemaAndVersionInfo result = null;
            for (final String fileName : fileNames) {
                final PoolIdSchemaAndVersionInfo infoObject = parser.start(ctx.getId().intValue(), fileName);
                final VersionInformation versionInformation = infoObject.getVersionInformation();
                final UpdateTaskInformation updateTaskInformation = infoObject.getUpdateTaskInformation();
                final String schema = infoObject.getSchema();
                final int pool_id = infoObject.getPoolId();
                if (null != versionInformation) {
                    versionInfo = versionInformation;
                }
                if (null != updateTaskInformation) {
                    updateTaskInfo = updateTaskInformation;
                }
                if (null != schema && -1 != pool_id) {
                    result = infoObject;
                }
            }
            if (null == result) {
                throw new OXContextRestoreException(Code.NO_CONFIGDB_FOUND);
            }

            final OXContextRestoreStorageInterface instance = OXContextRestoreStorageInterface.getInstance();
            result.setVersionInformation(versionInfo);
            result.setUpdateTaskInformation(updateTaskInfo);
            instance.checkVersion(result);

            final OXContextInterface contextInterface = Activator.getContextInterface();

            final OXToolStorageInterface storage = OXToolStorageInterface.getInstance();
            if (storage.existsContext(ctx)) {
                try {
                    contextInterface.delete(ctx, auth);
                } catch (final NoSuchContextException e) {
                    // As we check for the existence beforehand this exception should never occur. Nevertheless we will log this
                    LOG.fatal("FATAL:" + e.getMessage(), e);
                }
            }
            if (dryrun) {
                return "Done nothing (dry run)";
            }
            // We have to do the exists check beforehand otherwise you'll find a stack trace in the logs
            return instance.restorectx(ctx, result);
        } catch (final StorageException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final FileNotFoundException e) {
            LOG.error(e.getMessage(), e);
            throw new OXContextRestoreException(Code.FILE_NOT_FOUND, e);
        } catch (final IOException e) {
            LOG.error(e.getMessage(), e);
            throw new OXContextRestoreException(Code.IO_EXCEPTION, e);
        } catch (final SQLException e) {
            LOG.error(e.getMessage(), e);
            throw new OXContextRestoreException(Code.DATABASE_OPERATION_ERROR, e, e.getMessage());
        } catch (final OXContextRestoreException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final RuntimeException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        } catch (final DatabaseUpdateException e) {
            LOG.error(e.getMessage(), e);
            throw e;
        }
    }

}
