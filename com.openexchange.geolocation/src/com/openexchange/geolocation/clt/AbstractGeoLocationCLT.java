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

package com.openexchange.geolocation.clt;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import com.openexchange.auth.rmi.RemoteAuthenticator;
import com.openexchange.cli.AbstractRmiCLI;
import com.openexchange.cli.ConsoleSpinner;
import com.openexchange.geolocation.GeoLocationRMIService;
import com.openexchange.java.Charsets;
import com.openexchange.java.Strings;

/**
 * {@link AbstractGeoLocationCLT}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 * @since v7.10.2
 */
public abstract class AbstractGeoLocationCLT extends AbstractRmiCLI<Void> {

    protected static final String ABSTRACT_USAGE = "-u <database-user> [-a <database-password>] [-g <group>] [-k] [-o <output-directory>] [--http-proxy-host <http-proxy-host> --http-proxy-port <http-proxy-port> --https-proxy-host <https-proxy-host> --https-proxy-port <https-proxy-port>]" + BASIC_MASTER_ADMIN_USAGE;

    /**
     * The extraction working directory
     */
    private static final String DEFAULT_EXTRACT_DIRECTORY = File.separator + "tmp";

    /**
     * Value of '-o'
     */
    private String extractDirectory = DEFAULT_EXTRACT_DIRECTORY;
    /**
     * Value of '-g'
     */
    private String dbGroup = "default";
    /**
     * Value of '-u'
     */
    private String dbUser;
    /**
     * Value of '-a'
     */
    private String dbPassword;
    /**
     * Value of '-d'
     */
    private DatabaseVersion databaseVersion;
    /**
     * The absolute path of the downloaded file
     */
    private String downloadFilePath;
    /**
     * Influenced by '-k'
     */
    private boolean keep = false;

    private boolean importMode = false;

    private final String tables;
    private String usage;
    private String footer;

    /**
     * Initialises a new {@link AbstractGeoLocationCLT}.
     * 
     * @param a comma separated list with the database tables
     */
    public AbstractGeoLocationCLT(String tables, String usage, String footer) {
        super();
        this.tables = tables;
        this.usage = usage;
        this.footer = footer;
    }

    /**
     * Parses the database version from the provided '-d' flag and returns the DatabaseVersion
     * 
     * @return returns the {@link DatabaseVersion}
     * @throws IllegalArgumentException if no valid database version can be parsed
     */
    protected abstract DatabaseVersion parseDatabaseVersion(CommandLine cmd);

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractRmiCLI#addOptions(org.apache.commons.cli.Options)
     */
    @Override
    protected void addOptions(Options options) {
        options.addOption(createArgumentOption("o", "output-directory", "output-directory", "The output directory, i.e. where the downloaded and extracted files will be placed. If absent it will fall-back to '/tmp'", false));
        options.addOption(createArgumentOption("u", "database-user", "database-user", "The database user for importing the data.", true));
        options.addOption(createArgumentOption("a", "database-password", "database-password", "The database password for importing the data.", false));
        options.addOption(createArgumentOption("g", "database-group", "group", "The global database group. If absent it falls-back to 'default'", false));
        options.addOption(createSwitch("k", "keep", "Keeps the temporary files produced from this command line tool (zip archives, downloaded and extracted files).", false));
        options.addOption(createArgumentOption("d", "database-version", "database-version", "The database version identifier to download and import. The import mode is affected by this switch. Be sure to supply the correct version for the CSV you are importing.", false));
        options.addOption(createArgumentOption(null, "http-proxy-host", "http-proxy-host", "The IP or hostname of the HTTP proxy", false));
        options.addOption(createArgumentOption(null, "http-proxy-port", "http-proxy-port", "The port of the HTTP proxy", false));
        options.addOption(createArgumentOption(null, "https-proxy-host", "https-proxy-host", "The IP or hostname of the HTTPs proxy", false));
        options.addOption(createArgumentOption(null, "https-proxy-port", "https-proxy-port", "The port of the HTTPs proxy", false));
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractCLI#checkOptions(org.apache.commons.cli.CommandLine)
     */
    @Override
    protected void checkOptions(CommandLine cmd) {
        if (cmd.hasOption('o')) {
            extractDirectory = cmd.getOptionValue('o');
            File f = new File(extractDirectory);
            if (false == f.exists()) {
                System.out.println("The specified output directory '" + extractDirectory + "' does not exist. We will try to create it");
                try {
                    f.mkdirs();
                } catch (Exception e) {
                    System.out.println("Unable to create output directory '" + extractDirectory + "': " + e.getMessage());
                    System.exit(1);
                    return;
                }
            }
        }
        if (cmd.hasOption("http-proxy-host")) {
            System.setProperty("http.proxyHost", cmd.getOptionValue("http-proxy-host"));
        }
        if (cmd.hasOption("http-proxy-port")) {
            System.setProperty("http.proxyPort", cmd.getOptionValue("http-proxy-port"));
        }
        if (cmd.hasOption("https-proxy-host")) {
            System.setProperty("https.proxyHost", cmd.getOptionValue("https-proxy-host"));
        }
        if (cmd.hasOption("https-proxy-port")) {
            System.setProperty("https.proxyPort", cmd.getOptionValue("https-proxy-port"));
        }
        if (cmd.hasOption('u')) {
            dbUser = cmd.getOptionValue('u');
        }
        if (cmd.hasOption('a')) {
            dbPassword = cmd.getOptionValue('a');
        }
        if (cmd.hasOption('g')) {
            dbGroup = cmd.getOptionValue('g');
        }
        databaseVersion = parseDatabaseVersion(cmd);
        keep = cmd.hasOption('k');
        downloadFilePath = cmd.getOptionValue('i');
        if (downloadFilePath != null && false == downloadFilePath.isEmpty()) {
            importMode = true;
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractAdministrativeCLI#requiresAdministrativePermission()
     */
    @Override
    protected Boolean requiresAdministrativePermission() {
        return Boolean.TRUE;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractRmiCLI#administrativeAuth(java.lang.String, java.lang.String, org.apache.commons.cli.CommandLine, com.openexchange.auth.rmi.RemoteAuthenticator)
     */
    @Override
    protected void administrativeAuth(String login, String password, CommandLine cmd, RemoteAuthenticator authenticator) throws RemoteException {
        authenticator.doAuthentication(login, password);
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractCLI#getName()
     */
    @Override
    protected String getName() {
        return usage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see com.openexchange.cli.AbstractCLI#getFooter()
     */
    @Override
    protected String getFooter() {
        return footer;
    }

    /////////////////////////////////////// GETTERS //////////////////////////////

    /**
     * Gets the dbGroup
     *
     * @return The dbGroup
     */
    public String getDbGroup() {
        return dbGroup;
    }

    /**
     * Gets the dbUser
     *
     * @return The dbUser
     */
    public String getDbUser() {
        return dbUser;
    }

    /**
     * Gets the dbPassword
     *
     * @return The dbPassword
     */
    public String getDbPassword() {
        return dbPassword;
    }

    /**
     * Gets the keep
     *
     * @return The keep
     */
    public boolean isKeep() {
        return keep;
    }

    /**
     * Gets the importMode
     *
     * @return The importMode
     */
    public boolean isImportMode() {
        return importMode;
    }

    /**
     * Gets the extractDirectory
     *
     * @return The extractDirectory
     */
    public String getExtractDirectory() {
        return extractDirectory;
    }

    /**
     * Gets the databaseVersion
     *
     * @return The databaseVersion
     */
    public DatabaseVersion getDatabaseVersion() {
        return databaseVersion;
    }

    //////////////////////////////////////// HELPERS ///////////////////////////////

    /**
     * Checks whether the specified array of <code>filePaths</code> is either empty
     * or <code>null</code> and if one of those is, then terminate the JVM with
     * error code <code>1</code> to indicate an error.
     * 
     * @param filePaths The file paths to check.
     */
    protected void checkDatabaseFilePaths(String... filePaths) {
        for (String f : filePaths) {
            if (Strings.isEmpty(f)) {
                System.out.println("No viable database file was found in the extracted files.");
                System.exit(1);
                return;
            }
        }
    }

    /**
     * Imports the data into the 'global' database
     * 
     * @param the RMI hostname of the node on which to query the name of the global database
     * @param environment The execution environment
     */
    protected void importDatabase(String rmiHostName, String importStatements) throws Exception {
        GeoLocationRMIService rmiService = getRmiStub(rmiHostName, GeoLocationRMIService.RMI_NAME);
        String dbName = rmiService.getGlobalDatabaseName(dbGroup);

        String[] executionEnvironment = { "mysql", "-u", dbUser, "-p" + dbPassword, dbName, "-e", importStatements };
        ConsoleSpinner cs = new ConsoleSpinner("Importing data to schema '" + dbName + "' in table(s) '" + tables + "'...", null);
        runProcess(executionEnvironment, cs);
    }

    /**
     * Runs the process with the specified execution environment
     * 
     * @param executionEnvironment The execution environment, i.e. the executable with all its arguments
     */
    protected void runProcess(String[] executionEnvironment, ConsoleSpinner consoleSpinner) {
        if (executionEnvironment == null || executionEnvironment.length == 0) {
            System.out.println("The execution environment is empty.");
            System.exit(1);
            return;
        }
        try {
            consoleSpinner.start();
            ProcessBuilder processBuilder = new ProcessBuilder(executionEnvironment);
            Process runtimeProcess = processBuilder.start();
            int processComplete = runtimeProcess.waitFor();
            if (processComplete == 0) {
                consoleSpinner.stop("OK");
                return;
            }
            // The inputStream of the runtimeProcess might also contain 
            // hints and/or errors, so we make sure to print those as well.
            consoleSpinner.stop("FAILED");
            printErrors(runtimeProcess.getInputStream());
            printErrors(runtimeProcess.getErrorStream());
            System.exit(1);
            return;
        } catch (IOException e) {
            if (e.getMessage().contains("No such file or directory")) {
                System.out.println("\nERROR: Couldn't find the '" + executionEnvironment[0] + "' executable. Ensure that '" + executionEnvironment[0] + "' is installed and in your $PATH");
                System.exit(127);
                return;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            System.exit(1);
            return;
        }
    }

    /**
     * Prints any errors that were encountered during processing
     * 
     * @param inputStream the {@link InputStream} that holds the errors
     * @throws IOException if an I/O error is occurred
     */
    private void printErrors(InputStream inputStream) throws IOException {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(inputStream, Charsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                System.out.println(line);
            }
        }
    }
}
