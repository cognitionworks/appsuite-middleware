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

package com.openexchange.groupware.update.tools.console;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ReflectionException;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.TabularData;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXServiceURL;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.MissingArgumentException;
import org.apache.commons.cli.MissingOptionException;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import com.openexchange.groupware.update.tools.Constants;
import com.openexchange.tools.console.TableWriter;
import com.openexchange.tools.console.TableWriter.ColumnFormat;
import com.openexchange.tools.console.TableWriter.ColumnFormat.Align;

/**
 * {@link ListExecutedTasksCLT}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ListExecutedTasksCLT {

    private static final Options toolkitOptions;

    static {
        toolkitOptions = new Options();
        toolkitOptions.addOption(new Option("h", "help", false, "Prints a help text."));
        Option schemaOption = new Option("n", "name", true, "A valid schema name.");
        schemaOption.setRequired(true);
        schemaOption.setType(String.class);
        toolkitOptions.addOption(schemaOption);
        Option portOption = new Option("p", "port", true, "The optional JMX port (default:9999)");
        portOption.setType(Integer.TYPE);
        toolkitOptions.addOption(portOption);
        Option loginOption = new Option("l", "login", true, "The optional JMX login (if JMX has authentication enabled)");
        loginOption.setType(String.class);
        toolkitOptions.addOption(loginOption);
        Option passwdOption = new Option("s", "password", true, "The optional JMX password (if JMX has authentication enabled)");
        passwdOption.setType(String.class);
        toolkitOptions.addOption(passwdOption);
    }

    public ListExecutedTasksCLT() {
        super();
    }

    public static void main(String[] args) {
        final CommandLineParser parser = new PosixParser();
        final CommandLine cmd;
        try {
            cmd = parser.parse(toolkitOptions, args);
        } catch (MissingOptionException e) {
            printHelp();
            System.err.println("Option " + e.getMessage() + " must be defined.");
            System.exit(1);
            return;
        } catch (MissingArgumentException e) {
            printHelp();
            System.err.println(e.getMessage());
            System.exit(1);
            return;
        } catch (ParseException e) {
            e.printStackTrace();
            System.err.println("Unable to parse command line: " + e.getMessage());
            printHelp();
            System.exit(1);
            return;
        }
        if (cmd.hasOption('h')) {
            printHelp();
            System.exit(0);
        }

        final String schemaName = cmd.getOptionValue('n');
        int port = 9999;
        String val = cmd.getOptionValue('p');
        if (null != val) {
            try {
                port = Integer.parseInt(val.trim());
            } catch (NumberFormatException e) {
                System.err.println("Port parameter value \"" + val + "\" is not a number.");
                printHelp();
                System.exit(1);
            }
            if (port < 1 || port > 65535) {
                System.err.println("Port parameter value " + port + " is out of range. Valid range is from 1 to 65535.");
                printHelp();
                System.exit(1);
            }
        }
        final String jmxLogin = cmd.getOptionValue('l');
        final String jmxPassword = cmd.getOptionValue('s');

        final Map<String, Object> environment;
        if (jmxLogin == null || jmxPassword == null) {
            environment = null;
        } else {
            environment = new HashMap<String, Object>(1);
            environment.put(JMXConnectorServer.AUTHENTICATOR, new JMXAuthenticatorImpl(jmxLogin, jmxPassword));
        }
        final JMXServiceURL url;
        try {
            url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://localhost:" + port + "/server");
        } catch (MalformedURLException e) {
            System.err.println("URL to connect to server is invalid: " + e.getMessage());
            System.exit(1);
            return;
        }
        try {
            final JMXConnector jmxConnector = JMXConnectorFactory.connect(url, environment);
            try {
                final MBeanServerConnection mbsc = jmxConnector.getMBeanServerConnection();
                TabularData taskList = (TabularData) mbsc.invoke(Constants.OBJECT_NAME, "listExecutedTasks", new Object[] { schemaName }, null);
                writeTasks(taskList);
            } finally {
                jmxConnector.close();
            }
        } catch (InstanceNotFoundException e) {
            System.err.println("Instance is not available: " + e.getMessage());
            System.exit(1);
        } catch (MBeanException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        } catch (ReflectionException e) {
            System.err.println("Problem with reflective type handling: " + e.getMessage());
            System.exit(1);
        } catch (IOException e) {
            System.err.println("Unable to communicate with the server: " + e.getMessage());
            System.exit(1);
        }
    }

    private static final ColumnFormat[] FORMATS = {
        new ColumnFormat(Align.LEFT), new ColumnFormat(Align.LEFT), new ColumnFormat(Align.LEFT) };

    private static final String[] COLUMNS = { "taskName", "successful", "lastModified" };

    private static void writeTasks(TabularData taskList) {
        List<List<Object>> data = new ArrayList<List<Object>>();
        List<Object> valuesList = new ArrayList<Object>(COLUMNS.length);
        for (String column : COLUMNS) {
            valuesList.add(column);
        }
        data.add(valuesList);
        for (Object tmp : taskList.values()) {
            CompositeData composite = (CompositeData) tmp;
            valuesList = new ArrayList<Object>(COLUMNS.length);
            for (String column : COLUMNS) {
                valuesList.add(composite.get(column));
            }
            data.add(valuesList);
        }
        new TableWriter(System.out, FORMATS, data).write();
    }

    private static void printHelp() {
        final HelpFormatter helpFormatter = new HelpFormatter();
        helpFormatter.printHelp("listExecutedTasks", toolkitOptions);
    }
}
