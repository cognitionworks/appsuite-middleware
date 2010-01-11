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

package com.openexchange.control.console;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXAuthenticator;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXConnectorServer;
import javax.management.remote.JMXPrincipal;
import javax.management.remote.JMXServiceURL;
import javax.security.auth.Subject;
import org.apache.commons.codec.binary.Base64;
import com.openexchange.control.console.internal.ConsoleException;
import com.openexchange.control.console.internal.ValueObject;
import com.openexchange.control.console.internal.ValuePairObject;
import com.openexchange.control.console.internal.ValueParser;

/**
 * {@link AbstractConsoleHandler} - Abstract super class for console handlers.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractConsoleHandler {

    /**
     * The default parameter for host, port, login, and password: <code>&quot;-h&quot;</code>, <code>&quot;-p&quot;</code>,
     * <code>&quot;-l&quot;</code>, and <code>&quot;-pw&quot;</code>.
     */
    static final String[] DEFAULT_PARAMETER = { "-h", "-p", "-l", "-pw" };

    protected String jmxHost = "localhost";

    protected int jmxPort = 9999;

    protected String jmxLogin;

    protected String jmxPassword;

    protected JMXConnector jmxConnector;

    protected ObjectName objectName;

    protected MBeanServerConnection mBeanServerConnection;

    protected ValueParser valueParser;

    protected final void init(final String args[]) throws ConsoleException {
        init(args, false);
    }

    protected final void init(final String args[], final boolean noArgs) throws ConsoleException {
        if (!noArgs && args.length == 0) {
            showHelp();
            exit();
        } else {
            try {
                valueParser = new ValueParser(args, getParameter());
                final ValueObject[] valueObjects = valueParser.getValueObjects();
                for (int a = 0; a < valueObjects.length; a++) {
                    if (valueObjects[a].getValue().equals("-help") || valueObjects[a].getValue().equals("--help")) {
                        showHelp();
                        exit();
                    }
                }

                final ValuePairObject[] valuePairObjects = valueParser.getValuePairObjects();
                for (int a = 0; a < valuePairObjects.length; a++) {
                    if ("-h".equals(valuePairObjects[a].getName())) {
                        jmxHost = valuePairObjects[a].getValue();
                    } else if ("-p".equals(valuePairObjects[a].getName())) {
                        jmxPort = Integer.parseInt(valuePairObjects[a].getValue());
                    } else if ("-l".equals(valuePairObjects[a].getName())) {
                        jmxLogin = valuePairObjects[a].getValue();
                    } else if ("-pw".equals(valuePairObjects[a].getName())) {
                        jmxPassword = valuePairObjects[a].getValue();
                    }
                }

                initJMX(jmxHost, jmxPort, jmxLogin, jmxPassword);
            } catch (final Exception exc) {
                throw new ConsoleException(exc);
            }
        }
    }

    protected final void initJMX(final String jmxHost, final int jmxPort, final String jmxLogin, final String jmxPassword) throws Exception {
        final JMXServiceURL url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + jmxHost + ":" + jmxPort + "/server");

        final Map<String, Object> environment;
        if (jmxLogin == null || jmxPassword == null) {
            environment = null;
        } else {
            environment = new HashMap<String, Object>(1);
            environment.put(JMXConnectorServer.AUTHENTICATOR, new AbstractConsoleJMXAuthenticator(new String[] { jmxLogin, jmxPassword }));
        }

        jmxConnector = JMXConnectorFactory.connect(url, environment);

        mBeanServerConnection = jmxConnector.getMBeanServerConnection();

        objectName = ObjectName.getInstance("com.openexchange.control", "name", "Control");
    }

    protected abstract void showHelp();

    protected abstract void exit();

    protected abstract String[] getParameter();

    protected ObjectName getObjectName() {
        return objectName;
    }

    protected MBeanServerConnection getMBeanServerConnection() {
        return mBeanServerConnection;
    }

    protected ValueParser getParser() {
        return valueParser;
    }

    protected final void close() throws ConsoleException {
        try {
            if (jmxConnector != null) {
                jmxConnector.close();
            }
        } catch (final Exception exc) {
            throw new ConsoleException(exc);
        }
    }

    private static final class AbstractConsoleJMXAuthenticator implements JMXAuthenticator {

        private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AbstractConsoleJMXAuthenticator.class);

        private static volatile Charset US_ASCII;

        private static Charset getUSASCII() {
            if (US_ASCII == null) {
                synchronized (AbstractConsoleJMXAuthenticator.class) {
                    if (US_ASCII == null) {
                        US_ASCII = Charset.forName("US-ASCII");
                    }
                }
            }
            return US_ASCII;
        }

        private final String[] credentials;

        public AbstractConsoleJMXAuthenticator(final String[] credentials) {
            super();
            this.credentials = new String[credentials.length];
            System.arraycopy(credentials, 0, this.credentials, 0, credentials.length);
        }

        public Subject authenticate(final Object credentials) {
            if (!(credentials instanceof String[])) {
                if (credentials == null) {
                    throw new SecurityException("Credentials required");
                }
                throw new SecurityException("Credentials should be String[]");
            }
            final String[] creds = (String[]) credentials;
            if (creds.length != 2) {
                throw new SecurityException("Credentials should have 2 elements");
            }
            /*
             * Perform authentication
             */
            final String username = creds[0];
            final String password = creds[1];
            if ((this.credentials[0].equals(username)) && (this.credentials[1].equals(makeSHAPasswd(password)))) {
                return new Subject(true, Collections.singleton(new JMXPrincipal(username)), Collections.EMPTY_SET, Collections.EMPTY_SET);
            }
            throw new SecurityException("Invalid credentials");

        }

        private static String makeSHAPasswd(final String raw) {
            MessageDigest md;

            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (final NoSuchAlgorithmException e) {
                LOG.error(e.getMessage(), e);
                return raw;
            }

            final byte[] salt = {};

            md.reset();
            try {
                md.update(raw.getBytes("UTF-8"));
            } catch (final UnsupportedEncodingException e) {
                /*
                 * Cannot occur
                 */
                LOG.error(e.getMessage(), e);
            }
            md.update(salt);

            final String ret = getUSASCII().decode(ByteBuffer.wrap(Base64.encodeBase64(md.digest()))).toString();

            return ret;
        }

    }
}
