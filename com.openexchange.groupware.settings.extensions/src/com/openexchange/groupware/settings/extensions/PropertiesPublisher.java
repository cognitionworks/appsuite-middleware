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
package com.openexchange.groupware.settings.extensions;

import com.openexchange.groupware.settings.*;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.session.Session;

import java.util.*;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author Francisco Laguna <francisco.laguna@open-xchange.com>
 */
public class PropertiesPublisher {

    private ServicePublisher services;
    private Properties properties = new Properties();
    Map<Object, StaticValue> items = new HashMap<Object, StaticValue>();

    private final Log LOG = LogFactory.getLog(PropertiesPublisher.class);

    public void publish(final Properties properties) {

        final Set<Object> previousKeys = new HashSet<Object>(this.properties.keySet());

        for(final Object key : properties.keySet()) {
            final String pathString = key.toString();
            final String[] path = pathString.split("/");
            final String value = properties.getProperty(key.toString());


            if(previousKeys.contains(key)) {
                previousKeys.remove(key);
                final StaticValue prefItem = items.get(key);
                LOG.debug("Updating property "+key+" with value "+value);
                setValue(prefItem, value);                
            } else {
                final StaticValue prefItem = new StaticValue(path);
                setValue(prefItem, value);
                LOG.debug("Publishing property "+key+" with value "+value);
                publish( prefItem );
                items.put(key, prefItem);
            }
        }

        for(final Object key : previousKeys) {
            LOG.debug("Removing property "+key);
            remove(items.get(key));
            items.remove(key);
        }

        this.properties = properties;

    }

    public void setServicePublisher(final ServicePublisher publisher) {
        services = publisher;    
    }

    private void setValue(final StaticValue prefItem, final String value) {
        boolean valueSet = false;
        if(isMultiValue(value)) {
            try {
                prefItem.setValue(parseMultiValue(value), true);
                valueSet = true;
            } catch (final IllegalArgumentException x) {
                // IGNORE set as single value
                LOG.warn(x.getMessage(), x);
            } catch (final ArrayIndexOutOfBoundsException x) {
                // IGNORE set as single value
                LOG.warn(x.getMessage(), x);
            }
        }
        if(!valueSet) {
            // if all else fails, assume single value
            prefItem.setValue(applyEscapes(value), false);
        }
    }

    private String applyEscapes(final String value) {
        final StringBuilder bob = new StringBuilder();
        boolean escape = false;
        for(final char c : value.toCharArray()) {
            switch(c) {
                case '\\' :
                    if(escape) {
                        bob.append('\\');
                        escape = false;
                    } else {
                        escape = true;
                    }
                    break;
                default:
                    if(escape) {
                        escape = false;
                    }
                    bob.append(c);
                    break;
            }
        }
        return bob.toString();
    }

    private Object[] parseMultiValue(final String value) {

        return new MultiValueParser(value).array();
    }

    private boolean isMultiValue(final String value) {
        return value.matches("^\\s*\\[.*");
    }


    private void publish(final PreferencesItemService service) {
        services.publishService(PreferencesItemService.class, service);   
    }

    private void remove(final PreferencesItemService service) {
        services.removeService(PreferencesItemService.class, service);
    }


    private static final class StaticValue implements PreferencesItemService {

        private final String[] path;
        private Object value;
        private boolean multiple;

        public StaticValue(final String[] path) {
            this.path = path;
        }

        public String[] getPath() {
            return path;
        }

        public IValueHandler getSharedValue() {
            return new ReadOnlyValue(){

                public void getValue(final Session session, final Context ctx, final User user, final UserConfiguration userConfig, final Setting setting) throws SettingException {
                    StaticValue.this.setValue(setting);
                }

                public boolean isAvailable(final UserConfiguration userConfig) {
                    return true;
                }
            };
        }

        public void setValue(final Object value, final boolean multiple) {
            this.value = value;
            this.multiple = multiple;
        }

        public void setValue(final Setting setting) {
            if(multiple) {
                for(final Object component : (Object[]) value) {
                    setting.addMultiValue(component);
                }
            } else {
                setting.setSingleValue(value);
            }
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            final StaticValue that = (StaticValue) o;

            if (!Arrays.equals(path, that.path)) return false;

            return true;
        }

        @Override
        public String toString() {
            final StringBuilder bob = new StringBuilder();
            bob.append("Path: ").append(Arrays.asList(path));
            if(multiple) {
                bob.append("Multiple: ").append(Arrays.asList((Object[])value));
            } else {
                bob.append("Single: ").append(value);
            }
            return bob.toString();
        }

        @Override
        public int hashCode() {
            int result;
            result = (path != null ? Arrays.hashCode(path) : 0);
            result = 31 * result + (value != null ? value.hashCode() : 0);
            result = 31 * result + (multiple ? 1 : 0);
            return result;
        }
    }

    private class MultiValueParser {
        List<Object> components = new ArrayList<Object>();
        int index = 0;
        private final char[] stream;

        public MultiValueParser(final String value) {
            stream = value.toCharArray();
        }

        public Object[] array() {
            while(lookahead(' ')) { skip(); }
            consume('[');

            while(!eol()) { components.add(value()); }

            return components.toArray(new Object[components.size()]);
        }

        private Object value() {
            final StringBuilder bob = new StringBuilder();
            boolean escape = false;
            while(!eol()) {
                switch(next()) {
                    case '\\' :
                        if(escape) {
                            bob.append('\\');
                            escape = false;
                        } else {
                            escape = true;
                        }
                        skip();
                        break;
                    case ',' :
                        skip();
                        if(escape) {
                            bob.append(',');
                            escape = false;
                        } else {
                            return bob.toString().trim();
                        }
                        break;
                    case ']' :
                        skip();
                        if(escape) {
                            bob.append(']');
                            escape = false;
                        } else {
                            return bob.toString().trim();
                        }
                        break;
                    default :
                        if(escape) {
                            escape = false;
                        }
                        bob.append(next());
                        skip();
                        break;

                }
            }
            throw new IllegalArgumentException("This doesn't look like a multivalue");
        }

        private void consume(final char c) {
            if(!lookahead(c)) {
                throw new IllegalArgumentException("This doesn't look like a multivalue");
            }
            skip();
        }

        private void skip() {
            index++;
        }

        private boolean lookahead(final char c) {
            return stream[index] == c;
        }

        private char next() {
            return stream[index];
        }

        private boolean eol() {
            return index >= stream.length;
        }

    }
}
