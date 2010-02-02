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

package com.openexchange.authentication.ldap;

import java.util.Properties;
import javax.naming.AuthenticationException;
import javax.naming.Context;
import javax.naming.InvalidNameException;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.authentication.Authenticated;
import com.openexchange.authentication.AuthenticationService;
import com.openexchange.authentication.LoginException;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.authentication.LoginInfo;
import com.openexchange.tools.ssl.TrustAllSSLSocketFactory;

/**
 * This class implements the login by using an LDAP for authentication.
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class LDAPAuthentication implements AuthenticationService {

    private enum PropertyNames {
        BASE_DN("baseDN"),
        UID_ATTRIBUTE("uidAttribute"),
        LDAP_RETURN_FIELD("ldapReturnField"),
        SUBTREE_SEARCH("subtreeSearch"),
        SEARCH_FILTER("searchFilter"),
        BIND_DN("bindDN"),
        BIND_DN_PASSWORD("bindDNPassword");
        
        public String name;

        private PropertyNames(String name) {
            this.name = name;
        }
    }

    private static final Log LOG = LogFactory.getLog(LDAPAuthentication.class);

    /**
     * Properties for the JNDI context.
     */
    private final Properties props;

    /**
     * attribute name and base DN.
     */
    private String uidAttribute, baseDN, ldapReturnField, searchFilter, bindDN, bindDNPassword;

    private boolean subtreeSearch;
    
    /**
     * Default constructor.
     * @throws LoginException if setup fails.
     */
    public LDAPAuthentication(Properties props) throws LoginException {
        super();
        this.props = props;
        init();
    }

    /**
     * {@inheritDoc}
     */
    public Authenticated handleLoginInfo(LoginInfo loginInfo) throws LoginException {
        final String[] splitted = split(loginInfo.getUsername());
        final String uid = splitted[1];
        final String password = loginInfo.getPassword();
        if ("".equals(uid) || "".equals(password)) {
            throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
        }
        final String returnstring = bind(uid, password);
        return new Authenticated() {
            public String getContextInfo() {
                return splitted[0];
            }
            public String getUserInfo() {
                return null == returnstring ? splitted[1] : returnstring;
            }
        };
    }

    /**
     * Tries to bind.
     * @param uid login name.
     * @param password password.
     * @throws LoginException if some problem occurs.
     */
    private String bind(String uid, String password) throws LoginException {
        LdapContext context = null;
        String dn = null;
        try {
            if( subtreeSearch ) {
                // get user dn from user
                final Properties aprops = (Properties)props.clone();
                aprops.put(LdapContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
                if( bindDN != null && bindDN.length() > 0 ) {
                    LOG.debug("Using bindDN=" + bindDN);
                    aprops.put(Context.SECURITY_PRINCIPAL, bindDN);
                    aprops.put(Context.SECURITY_CREDENTIALS, bindDNPassword);
                } else {
                    aprops.put(Context.SECURITY_AUTHENTICATION, "none");
                }
                context = new InitialLdapContext(aprops, null);
                final String filter = "(&" + searchFilter + "(" + uidAttribute + "=" + uid + "))";
                LOG.debug("Using filter=" + filter);
                LOG.debug("BaseDN      =" + baseDN);
                SearchControls cons = new SearchControls();
                cons.setSearchScope(SearchControls.SUBTREE_SCOPE);
                cons.setCountLimit(0);
                cons.setReturningAttributes(new String[]{"dn"});
                NamingEnumeration<SearchResult> res = context.search(baseDN, filter, cons);
                if( res.hasMoreElements() ) {
                    dn = res.nextElement().getNameInNamespace();
                    if( res.hasMoreElements() ) {
                        final String errortext = "Found more then one user with " + uidAttribute + "=" + uid;
                        LOG.error(errortext);
                        throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
                    }
                } else {
                    final String errortext = "No user found with " + uidAttribute + "=" + uid;
                    LOG.error(errortext);
                    throw LoginExceptionCodes.INVALID_CREDENTIALS.create();
                }
                context.close();
            } else {
                dn = uidAttribute + "=" + uid + "," + baseDN;
            }
            context = new InitialLdapContext(props, null);
            context.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
            context.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
            context.reconnect(null);
            if (null != ldapReturnField && ldapReturnField.length() > 0) {
                final Attributes userDnAttributes = context.getAttributes(dn);
                final Attribute attribute = userDnAttributes.get(ldapReturnField);
                return (String) attribute.get();
            }
            return null;
        } catch (InvalidNameException e) {
            LOG.error("Login failed for dn " + dn + ":",e);
            throw LoginExceptionCodes.INVALID_CREDENTIALS.create(e);
        } catch (AuthenticationException e) {
            LOG.error("Login failed for dn " + dn + ":",e);
            throw LoginExceptionCodes.INVALID_CREDENTIALS.create(e);
        } catch (NamingException e) {
            LOG.error(e.getMessage(), e);
            throw LoginExceptionCodes.COMMUNICATION.create(e);
        } finally {
            try {
                if( context != null ) {
                    context.close();
                }
            } catch (NamingException e) {
                LOG.error(e.getMessage(), e);
            }
        }
    }

    /**
     * Initializes the properties for the ldap authentication.
     * @throws LoginException if configuration fails.
     */
    private void init() throws LoginException {
        props.put(LdapContext.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");

        if (!props.containsKey(PropertyNames.UID_ATTRIBUTE.name)) {
            throw LoginExceptionCodes.MISSING_PROPERTY.create(PropertyNames.UID_ATTRIBUTE.name);
        }
        uidAttribute = props.getProperty(PropertyNames.UID_ATTRIBUTE.name);

        if (!props.containsKey(PropertyNames.BASE_DN.name)) {
            throw LoginExceptionCodes.MISSING_PROPERTY.create(PropertyNames.BASE_DN.name);
        }
        baseDN = props.getProperty(PropertyNames.BASE_DN.name);

        final String url = props.getProperty(LdapContext.PROVIDER_URL);
        if (null == url) {
            throw LoginExceptionCodes.MISSING_PROPERTY.create(LdapContext.PROVIDER_URL);
        } else if (url.startsWith("ldaps")) {
            props.put("java.naming.ldap.factory.socket", TrustAllSSLSocketFactory.class.getName());
        }

        this.ldapReturnField = props.getProperty(PropertyNames.LDAP_RETURN_FIELD.name);

        if (!props.containsKey(PropertyNames.SUBTREE_SEARCH.name)) {
            throw LoginExceptionCodes.MISSING_PROPERTY.create(PropertyNames.SUBTREE_SEARCH.name);
        }
        subtreeSearch = Boolean.parseBoolean(props.getProperty(PropertyNames.SUBTREE_SEARCH.name));

        if (!props.containsKey(PropertyNames.SEARCH_FILTER.name)) {
            throw LoginExceptionCodes.MISSING_PROPERTY.create(PropertyNames.SEARCH_FILTER.name);
        }
        searchFilter = props.getProperty(PropertyNames.SEARCH_FILTER.name);

        bindDN = props.getProperty(PropertyNames.BIND_DN.name);
        bindDNPassword = props.getProperty(PropertyNames.BIND_DN_PASSWORD.name);
    }

    /**
     * Splits user name and context.
     * @param loginInfo combined information separated by an @ sign.
     * @return a string array with context and user name (in this order).
     * @throws LoginException if no separator is found.
     */
    private String[] split(String loginInfo) {
        return split(loginInfo, '@');
    }

    /**
     * Splits user name and context.
     * @param loginInfo combined information separated by an @ sign.
     * @param character for splitting user name and context.
     * @return a string array with context and user name (in this order).
     * @throws LoginException if no separator is found.
     */
    private String[] split(String loginInfo, char separator) {
        final int pos = loginInfo.lastIndexOf(separator);
        final String[] splitted = new String[2];
        if (-1 == pos) {
            splitted[1] = loginInfo;
            splitted[0] = "defaultcontext";
        } else {
            splitted[1] = loginInfo.substring(0, pos);
            splitted[0] = loginInfo.substring(pos + 1);
        }
        return splitted;
    }
}
