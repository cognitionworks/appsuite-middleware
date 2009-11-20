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

package com.openexchange.jsieve;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.jsieve.exceptions.OXSieveHandlerException;
import com.openexchange.jsieve.exceptions.OXSieveHandlerInvalidCredentialsException;

/**
 * This class is used to deal with the communication with sieve. For a description of the communication system to sieve see
 * {@see <a href="http://www.ietf.org/internet-drafts/draft-martin-managesieve-07.txt">http://www.ietf.org/internet-drafts/draft-martin-managesieve-07.txt</a>}
 * 
 * @author <a href="mailto:dennis.sieben@open-xchange.com">Dennis Sieben</a>
 */
public class SieveHandler {

    private final static String CRLF = "\r\n";

    private final static String SIEVE_OK = "OK";

    private final static String SIEVE_NO = "NO";

    private final static String SIEVE_AUTH = "AUTHENTICATE ";

    private final static String SIEVE_AUTH_FAILD = "NO \"Authentication Error\"";

    private final static String SIEVE_AUTH_LOGIN_USERNAME = "{12}" + CRLF + "VXNlcm5hbWU6";

    private final static String SIEVE_AUTH_LOGIN_PASSWORD = "{12}" + CRLF + "UGFzc3dvcmQ6";

    private final static String SIEVE_PUT = "PUTSCRIPT ";

    private final static String SIEVE_ACTIVE = "SETACTIVE ";

    private final static String SIEVE_DEACTIVE = "SETACTIVE \"\"" + CRLF;

    private final static String SIEVE_DELETE = "DELETESCRIPT ";

    private final static String SIEVE_LIST = "LISTSCRIPTS" + CRLF;

    private final static String SIEVE_GET_SCRIPT = "GETSCRIPT ";

    private final static String SIEVE_LOGOUT = "LOGOUT" + CRLF;

    private static final int UNDEFINED = -1;

    private static final int OK = 0;

    private static final int NO = 1;

    /*-
     * Member section
     */

    private boolean AUTH = false;

    private String sieve_user = null;

    private String sieve_auth = null;

    private String sieve_auth_passwd = null;

    private String sieve_host = "127.0.0.1";

    private int sieve_host_port = 2000;

    private Capabilities capa = null;

    private Socket s_sieve = null;

    private BufferedReader bis_sieve = null;

    private BufferedOutputStream bos_sieve = null;

    private static Log log = LogFactory.getLog(SieveHandler.class);

    private long mStart;

    private long mEnd;

    /**
     * SieveHandler use socket-connection to manage sieve-scripts.<br>
     * <br>
     * Important: Don't forget to close the SieveHandler!
     * 
     * @param userName
     * @param passwd
     * @param host
     * @param port
     */
    public SieveHandler(final String userName, final String passwd, final String host, final int port) {
        sieve_user = userName;
        sieve_auth = userName;
        sieve_auth_passwd = passwd;
        sieve_host = host;
        sieve_host_port = port;
    }

    public SieveHandler(final String userName, final String authUserName, final String authUserPasswd, final String host, final int port) {
        sieve_user = userName;
        sieve_auth = authUserName;
        sieve_auth_passwd = authUserPasswd;
        sieve_host = host;
        sieve_host_port = port;

    }

    private void measureStart() {
        this.mStart = System.currentTimeMillis();
    }

    private void measureEnd(final String method) {
        this.mEnd = System.currentTimeMillis();
        if (log.isDebugEnabled()) {
            log.debug("SieveHandler." + method + "() took " + (this.mEnd - this.mStart) + "ms to perform");
        }
    }

    /**
     * Use this function to initialize the connection. It will get the welcome messages from the server, parse the capabilities and login
     * the user.
     * 
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     * @throws OXSieveHandlerInvalidCredentialsException
     */
    public void initializeConnection() throws IOException, OXSieveHandlerException, UnsupportedEncodingException, OXSieveHandlerInvalidCredentialsException {
        measureStart();
        s_sieve = new Socket();
        /*
         * Connect with a connect-timeout of 30sec
         */
        s_sieve.connect(new InetSocketAddress(sieve_host, sieve_host_port), 30000);
        /*
         * Set timeout to 30sec
         */
        s_sieve.setSoTimeout(30000);
        bis_sieve = new BufferedReader(new InputStreamReader(s_sieve.getInputStream(), "UTF-8"));
        bos_sieve = new BufferedOutputStream(s_sieve.getOutputStream());

        if (!getServerWelcome()) {
            throw new OXSieveHandlerException("No welcome from server", sieve_host, sieve_host_port);
        }
        log.debug("Got welcome from sieve");
        measureEnd("getServerWelcome");
        /*
         * Capabilities read; further communication dependent on capabilities
         */
        measureStart();
        List<String> sasl = capa.getSasl();
        measureEnd("capa.getSasl");

        final boolean issueTLS = capa.getStarttls().booleanValue();

        final StringBuilder commandBuilder = new StringBuilder(64);

        if (issueTLS) {
            /*-
             * Switch to TLS and re-fetch capabilities
             *
             *
             * Send STARTTLS
             * 
             * C: STARTTLS
             * S: OK
             * <TLS negotiation, further commands are under TLS layer>
             * S: "IMPLEMENTATION" "Example1 ManageSieved v001"
             * S: "SASL" "PLAIN"
             * S: "SIEVE" "fileinto vacation"
             * S: OK
             */
            measureStart();
            bos_sieve.write(commandBuilder.append("STARTTLS").append(CRLF).toString().getBytes("UTF-8"));
            bos_sieve.flush();
            measureEnd("startTLS");
            commandBuilder.setLength(0);
            /*
             * Expect OK
             */
            while (true) {
                final String temp = bis_sieve.readLine();
                if (null == temp) {
                    throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
                } else if (temp.startsWith(SIEVE_OK)) {
                    break;
                } else if (temp.startsWith(SIEVE_AUTH_FAILD)) {
                    throw new OXSieveHandlerException("can't auth to SIEVE ", sieve_host, sieve_host_port);
                }
            }
            /*
             * Switch to TLS
             */
            s_sieve = SocketFetcher.startTLS(s_sieve, sieve_host);
            bis_sieve = new BufferedReader(new InputStreamReader(s_sieve.getInputStream(), "UTF-8"));
            bos_sieve = new BufferedOutputStream(s_sieve.getOutputStream());
            /*
             * Fire CAPABILITY command but only for cyrus that is not sieve draft conform to sent CAPABILITY response again directly as
             * response for the STARTTLS command.
             */
            if (capa.getImplementation().matches("^Cyrus.*v([0-1]\\.[0-9]|2\\.[0-2]).*$") || capa.getImplementation().startsWith("NEMESIS")) {
	            measureStart();
	            bos_sieve.write(commandBuilder.append("CAPABILITY").append(CRLF).toString().getBytes("UTF-8"));
	            bos_sieve.flush();
	            measureEnd("capability");
	            commandBuilder.setLength(0);
            }
            /*
             * Read capabilities
             */
            measureStart();
            if (!getServerWelcome()) {
                throw new OXSieveHandlerException("No TLS negotiation from server", sieve_host, sieve_host_port);
            }
            measureEnd("tlsNegotiation");
            sasl = capa.getSasl();
        }
        /*
         * Check for PLAIN authentication support
         */
        if (null == sasl || !sasl.contains("PLAIN")) {
            throw new OXSieveHandlerException(
                new StringBuilder(64).append("The server doesn't suppport PLAIN authentication over a ").append(
                    issueTLS ? "TLS" : "plain-text").append(" connection.").toString(),
                sieve_host,
                sieve_host_port);
        }
        measureStart();
        if (!selectAuth("PLAIN", commandBuilder)) {
            throw new OXSieveHandlerInvalidCredentialsException("Authentication failed");
        }
        log.debug("Authentication to sieve successful");
        measureEnd("selectAuth");
    }

    /**
     * Upload this byte[] as sieve script
     * 
     * @param script_name
     * @param script
     * @param commandBuilder
     * @throws OXSieveHandlerException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public void setScript(final String script_name, final byte[] script, final StringBuilder commandBuilder) throws OXSieveHandlerException, IOException, UnsupportedEncodingException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("Script upload not possible. Auth first.", sieve_host, sieve_host_port);
        }

        if (script == null) {
            throw new OXSieveHandlerException("Script upload not possible. No Script", sieve_host, sieve_host_port);
        }

        final String put = commandBuilder.append(SIEVE_PUT).append('\"').append(script_name).append("\" {").append(script.length).append(
            "+}").append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(put.getBytes("UTF-8"));
        bos_sieve.write(script);

        bos_sieve.write(CRLF.getBytes("UTF-8"));
        bos_sieve.flush();

        final StringBuilder sb = new StringBuilder();
        final String actualline = bis_sieve.readLine();
        if (null != actualline && actualline.startsWith(SIEVE_OK)) {
            return;
        } else if (null != actualline && actualline.startsWith("NO ")) {
            final String answer = actualline.substring(3);
            final Pattern p = Pattern.compile("^\\{([^\\}]*)\\}.*$");
            final Matcher matcher = p.matcher(answer);
            if (matcher.matches()) {
                final String group = matcher.group(1);
                final int octetsToRead = Integer.parseInt(group);
                final char[] buf = new char[octetsToRead];
                final int octetsRead = bis_sieve.read(buf, 0, octetsToRead);
                if (octetsRead == octetsToRead) {
                    sb.append(buf);
                } else {
                    sb.append(buf, 0, octetsRead);
                }
                sb.append(CRLF);
            } else {
                sb.append(answer);
                sb.append(CRLF);
            }
            throw new OXSieveHandlerException(sb.toString(), sieve_host, sieve_host_port);
        } else {
            throw new OXSieveHandlerException("Unknown error occured", sieve_host, sieve_host_port);
        }
    }

    /**
     * Activate/Deactivate sieve script. Is status is true, activate this script.
     * 
     * @param script_name
     * @param status
     * @param commandBuilder
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     */
    public void setScriptStatus(final String script_name, final boolean status, final StringBuilder commandBuilder) throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (status) {
            activate(script_name, commandBuilder);
        } else {
            deactivate(script_name);
        }
    }

    /**
     * Get the sieveScript, if a script doesn't exists a byte[] with a size of 0 is returned
     * 
     * @param script_name
     * @return the read script
     * @throws OXSieveHandlerException
     * @throws UnsupportedEncodingException
     * @throws IOException
     */
    public String getScript(final String script_name) throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (!AUTH) {
            throw new OXSieveHandlerException("Get script not possible. Auth first.", sieve_host, sieve_host_port);
        }
        final StringBuilder sb = new StringBuilder(32);
        final String get = sb.append(SIEVE_GET_SCRIPT).append('"').append(script_name).append('"').append(CRLF).toString();
        bos_sieve.write(get.getBytes("UTF-8"));
        bos_sieve.flush();
        sb.setLength(0);
        /*-
         * If the script does not exist the server MUST reply with a NO response. Upon success a string with the contents of the script is
         * returned followed by a OK response.
         * 
         * Example:
         * 
         * C: GETSCRIPT "myscript"
         * S: {54+}
         * S: #this is my wonderful script
         * S: reject "I reject all";
         * S:
         * S: OK
         */
        {
            final String firstLine = bis_sieve.readLine();
            if (null == firstLine) {
                // End of the stream reached
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            final int[] parsed = parseFirstLine(firstLine);
            final int respCode = parsed[0];
            if (NO == respCode || OK == respCode) {
                /*
                 * Either received a NO or an OK which indicates end of response. In both cases no script is availabe.
                 */
                return "";
            }
            sb.ensureCapacity(parsed[1]);
        }
        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                if (sb.length() >= 2) {
                    // We have to strip off the last trailing CRLF...
                    return sb.substring(0, sb.length() - 2);
                }
                return sb.toString();
            }
            sb.append(temp);
            sb.append(CRLF);
        }
        /*-
         * 
         * 
        boolean firstread = true;
        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                // We have to strip off the last trailing CRLF...
                return sb.substring(0, sb.length() - 2);
            } else if (temp.startsWith(SIEVE_NO)) {
                return "";
            }
            // The first line contains the length of the following byte set, we don't need this
            // information here and so strip it off...
            if (firstread) {
                firstread = false;
            } else {
                sb.append(temp);
                sb.append(CRLF);
            }
        }
         */
    }

    /**
     * Get the list of sieveScripts
     * 
     * @return List of scripts
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     */
    public ArrayList<String> getScriptList() throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("List scripts not possible. Auth first.", sieve_host, sieve_host_port);
        }

        final String active = SIEVE_LIST;
        bos_sieve.write(active.getBytes("UTF-8"));
        bos_sieve.flush();

        final ArrayList<String> list = new ArrayList<String>();
        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                return list;
            }
            if (temp.startsWith(SIEVE_NO)) {
                throw new OXSieveHandlerException("Sieve has no script list", sieve_host, sieve_host_port);
            }
            // Here we strip off the leading and trailing " and the ACTIVE at the
            // end if it occurs. We want a list of the script names only
            final String scriptname = temp.substring(temp.indexOf('\"') + 1, temp.lastIndexOf('\"'));
            list.add(scriptname);
        }

    }

    /**
     * Get the list of active sieve scripts
     * 
     * @return List of scripts
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     */
    public String getActiveScript() throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("List scripts not possible. Auth first.", sieve_host, sieve_host_port);
        }

        final String active = SIEVE_LIST;
        bos_sieve.write(active.getBytes("UTF-8"));
        bos_sieve.flush();

        String scriptname = null;
        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                return scriptname;
            }
            if (temp.startsWith(SIEVE_NO)) {
                throw new OXSieveHandlerException("Sieve has no script list", sieve_host, sieve_host_port);
            }

            if (temp.matches(".*ACTIVE")) {
                scriptname = temp.substring(temp.indexOf('\"') + 1, temp.lastIndexOf('\"'));
            }
        }

    }

    /**
     * Remove the sieve script. If the script is active it is deactivated before removing
     * 
     * @param script_name
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     */
    public void remove(final String script_name) throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("Delete a script not possible. Auth first.", sieve_host, sieve_host_port);
        }
        if (null == script_name) {
            throw new OXSieveHandlerException("Script can't be removed", sieve_host, sieve_host_port);
        }

        final StringBuilder commandBuilder = new StringBuilder(64);

        setScriptStatus(script_name, false, commandBuilder);

        final String delete = commandBuilder.append(SIEVE_DELETE).append("\"").append(script_name).append("\"").append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(delete.getBytes("UTF-8"));
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                return;
            } else if (temp.startsWith(SIEVE_NO)) {
                throw new OXSieveHandlerException("Script can't be removed", sieve_host, sieve_host_port);
            }
        }
    }

    /**
     * Close socket-connection to sieve
     * 
     * @throws IOException
     * @throws UnsupportedEncodingException
     */
    public void close() throws IOException, UnsupportedEncodingException {
        if (null != bos_sieve) {
            bos_sieve.write(SIEVE_LOGOUT.getBytes("UTF-8"));
            bos_sieve.flush();
        }
        if (null != s_sieve) {
            s_sieve.close();
        }
    }

    private boolean getServerWelcome() throws UnknownHostException, IOException, OXSieveHandlerException {
        capa = new Capabilities();

        while (true) {
            final String test = bis_sieve.readLine();
            if (null == test) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (test.startsWith(SIEVE_OK)) {
                return true;
            } else if (test.startsWith(SIEVE_NO)) {
                AUTH = false;
                return false;
            } else {
                parseCAPA(test);
            }
        }
    }

    private boolean authPLAIN(final StringBuilder commandBuilder) throws IOException, UnsupportedEncodingException {
        final String to64 = commandBuilder.append(sieve_user).append('\0').append(sieve_auth).append('\0').append(sieve_auth_passwd).toString();
        commandBuilder.setLength(0);

        final String user_auth_pass_64 = commandBuilder.append(convertStringToBase64(to64)).append(CRLF).toString();
        commandBuilder.setLength(0);

        final String auth_mech_string = commandBuilder.append(SIEVE_AUTH).append("\"PLAIN\" ").toString();
        commandBuilder.setLength(0);

        final String user_size = commandBuilder.append("{").append((user_auth_pass_64.length() - 2)).append("+}").append(CRLF).toString();
        commandBuilder.setLength(0);

        // We don't need to specify an encoding here because all strings contain only ASCII Text
        bos_sieve.write(auth_mech_string.getBytes());
        bos_sieve.write(user_size.getBytes());
        bos_sieve.write(user_auth_pass_64.getBytes());
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null != temp) {
                if (temp.startsWith(SIEVE_OK)) {
                    AUTH = true;
                    return true;
                } else if (temp.startsWith(SIEVE_NO)) {
                    AUTH = false;
                    return false;
                }
            } else {
                AUTH = false;
                return false;
            }
        }
    }

    // FIXME: Not tested yet
    private boolean authLOGIN(final StringBuilder commandBuilder) throws IOException, OXSieveHandlerException, UnsupportedEncodingException {

        final String auth_mech_string = commandBuilder.append(SIEVE_AUTH).append("\"LOGIN\"").append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(auth_mech_string.getBytes("UTF-8"));
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.endsWith(SIEVE_AUTH_LOGIN_USERNAME)) {
                break;
            } else if (temp.endsWith(SIEVE_AUTH_FAILD)) {
                throw new OXSieveHandlerException("can't auth to SIEVE ", sieve_host, sieve_host_port);
            }
        }

        final String user64 = commandBuilder.append(convertStringToBase64(sieve_auth)).append(CRLF).toString();
        commandBuilder.setLength(0);

        final String user_size = commandBuilder.append('{').append((user64.length() - 2)).append("+}").append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(user_size.getBytes("UTF-8"));
        bos_sieve.write(user64.getBytes("UTF-8"));
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.endsWith(SIEVE_AUTH_LOGIN_PASSWORD)) {
                break;
            } else if (temp.endsWith(SIEVE_AUTH_FAILD)) {
                throw new OXSieveHandlerException("can't auth to SIEVE ", sieve_host, sieve_host_port);
            }
        }

        final String pass64 = commandBuilder.append(convertStringToBase64(sieve_auth_passwd)).append(CRLF).toString();
        commandBuilder.setLength(0);

        final String pass_size = commandBuilder.append('{').append((pass64.length() - 2)).append("+}").append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(pass_size.getBytes("UTF-8"));
        bos_sieve.write(pass64.getBytes("UTF-8"));
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                AUTH = true;
                return true;
            } else if (temp.startsWith(SIEVE_AUTH_FAILD)) {
                throw new OXSieveHandlerException("can't auth to SIEVE ", sieve_host, sieve_host_port);
            }
        }
    }

    private void activate(final String sieve_script_name, final StringBuilder commandBuilder) throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("Activate a script not possible. Auth first.", sieve_host, sieve_host_port);
        }

        final String active = commandBuilder.append(SIEVE_ACTIVE).append('\"').append(sieve_script_name).append('\"').append(CRLF).toString();
        commandBuilder.setLength(0);

        bos_sieve.write(active.getBytes("UTF-8"));
        bos_sieve.flush();

        while (true) {
            final String temp = bis_sieve.readLine();
            if (null == temp) {
                throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
            }
            if (temp.startsWith(SIEVE_OK)) {
                return;
            } else if (temp.startsWith(SIEVE_NO)) {
                throw new OXSieveHandlerException("Error while activating script: " + sieve_script_name, sieve_host, sieve_host_port);
            }
        }
    }

    private void deactivate(final String sieve_script_name) throws OXSieveHandlerException, UnsupportedEncodingException, IOException {
        if (AUTH == false) {
            throw new OXSieveHandlerException("Deactivate a script not possible. Auth first.", sieve_host, sieve_host_port);
        }

        boolean scriptactive = false;
        if (sieve_script_name.equals(getActiveScript())) {
            scriptactive = true;
        }

        if (scriptactive) {
            bos_sieve.write(SIEVE_DEACTIVE.getBytes("UTF-8"));
            bos_sieve.flush();

            while (true) {
                final String temp = bis_sieve.readLine();
                if (null == temp) {
                    throw new OXSieveHandlerException("Communication to SIEVE server aborted. ", sieve_host, sieve_host_port);
                }
                if (temp.startsWith(SIEVE_OK)) {
                    return;
                } else if (temp.startsWith(SIEVE_NO)) {
                    throw new OXSieveHandlerException("Error while deactivating script: " + sieve_script_name, sieve_host, sieve_host_port);
                }
            }
        }
    }

    /**
     * @param auth_mech
     * @return
     * @throws IOException
     * @throws UnsupportedEncodingException
     * @throws OXSieveHandlerException
     */
    private boolean selectAuth(final String auth_mech, final StringBuilder commandBuilder) throws IOException, UnsupportedEncodingException, OXSieveHandlerException {
        if (auth_mech.equals("PLAIN")) {
            return authPLAIN(commandBuilder);
        } else if (auth_mech.equals("LOGIN")) {
            return authLOGIN(commandBuilder);
        }
        return false;
    }

    private void parseCAPA(final String line) {
        final String starttls = "\"STARTTLS\"";
        final String implementation = "\"IMPLEMENTATION\"";
        final String sieve = "\"SIEVE\"";
        final String sasl = "\"SASL\"";

        String temp = line;

        if (temp.startsWith(starttls)) {
            temp = temp.substring(starttls.length());
            capa.setStarttls(Boolean.TRUE);
        } else if (temp.startsWith(implementation)) {
            temp = temp.substring(implementation.length());
            temp = temp.substring(temp.indexOf('\"') + 1);
            temp = temp.substring(0, temp.indexOf('\"'));

            capa.setImplementation(temp);
        } else if (temp.startsWith(sieve)) {
            temp = temp.substring(sieve.length());
            temp = temp.substring(temp.indexOf("\"") + 1);
            temp = temp.substring(0, temp.indexOf("\""));

            final StringTokenizer st = new StringTokenizer(temp);
            while (st.hasMoreTokens()) {
                capa.addSieve(st.nextToken());
            }
        } else if (temp.startsWith(sasl)) {
            temp = temp.substring(sasl.length());
            temp = temp.substring(temp.indexOf("\"") + 1);
            temp = temp.substring(0, temp.indexOf("\""));

            final StringTokenizer st = new StringTokenizer(temp);
            while (st.hasMoreTokens()) {
                capa.addSasl(st.nextToken().toUpperCase());
            }
        }
    }

    /**
     * @param toConvert
     * @return Base64String
     * @throws UnsupportedEncodingException
     */
    private String convertStringToBase64(final String toConvert) throws UnsupportedEncodingException {
        final String converted = com.openexchange.tools.encoding.Base64.encode(toConvert.getBytes("UTF-8"));
        return converted.replaceAll("(\\r)?\\n", "");
    }

    /**
     * Parses the first line of a SIEVE response.
     * <p>
     * Examples:<br>
     * &nbsp;<code>{54+}</code><br>
     * &nbsp;<code>No {31+}</code><br>
     * 
     * @param firstLine The first line
     * @return An array of <code>int</code> with length 2. The first position holds the response code if any available ({@link #NO} or
     *         {@link #OK}), otherwise {@link #UNDEFINED}. The second position holds the number of octets of a following literal or
     *         {@link #UNDEFINED} if no literal is present.
     */
    private static int[] parseFirstLine(final String firstLine) {
        if (null == firstLine) {
            return null;
        }
        final int[] retval = new int[2];
        retval[0] = UNDEFINED;
        retval[1] = UNDEFINED;
        // Check for starting "NO" or "OK"
        final char[] chars = firstLine.toCharArray();
        int index = 0;
        if ('N' == chars[index] && 'O' == chars[index + 1]) {
            retval[0] = NO;
            index += 2;
        } else if ('O' == chars[index] && 'K' == chars[index + 1]) {
            retval[0] = OK;
            index += 2;
        }
        // Check for a literal
        if (index < chars.length) {
            char c;
            while ((index < chars.length) && (((c = chars[index]) == ' ') || (c == '\t'))) {
                index++;
            }
            if (index < chars.length && '{' == chars[index]) {
                // A literal
                retval[1] = parseLiteralLength(readString(index, chars));
            }
        }

        return retval;
    }

    private static final Pattern PAT_LIT_LEN = Pattern.compile("\\{([0-9]+)(\\+?)\\}");

    private static int parseLiteralLength(final String respLen) {
        final Matcher matcher = PAT_LIT_LEN.matcher(respLen);
        if (matcher.matches()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (final NumberFormatException e) {
                log.error(e.getMessage(), e);
                return -1;
            }
        }
        return -1;
    }

    private static String readString(final int index, final char[] chars) {
        final int size = chars.length;
        if (index >= size) {
            // already at end of response
            return null;
        }
        // Read until delimiter reached
        final int start = index;
        int i = index;
        char c;
        while ((i < size) && ((c = chars[i]) != ' ') && (c != '\r') && (c != '\n') && (c != '\t')) {
            i++;
        }
        return toString(chars, start, i);
    }

    /**
     * Convert the chars within the specified range of the given byte array into a {@link String}. The range extends from <code>start</code>
     * till, but not including <code>end</code>.
     */
    private static String toString(final char[] chars, final int start, final int end) {
        final int size = end - start;
        final char[] theChars = new char[size];
        for (int i = 0, j = start; i < size;) {
            theChars[i++] = (chars[j++]);
        }
        return new String(theChars);
    }

    /**
     * Gets the capabilities.
     * 
     * @return The capabilities
     */
    public Capabilities getCapabilities() {
        return this.capa;
    }

}
