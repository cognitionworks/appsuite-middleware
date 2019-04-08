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

package com.openexchange.serialization.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import com.openexchange.serialization.ClassResolver;

/**
 * {@link FilteringObjectInputStream} prevents invalid deserialization by using a blacklist
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.2
 */
public class FilteringObjectInputStream extends ObjectInputStream {

    /** Simple class to delay initialization until needed */
    private static class LoggerHolder {
        static final Logger LOG = org.slf4j.LoggerFactory.getLogger(FilteringObjectInputStream.class);
    }

    private final SerializationFilteringConfig config;
    private final Set<ClassResolver> classResolvers;

    /**
     * Initializes a new {@link FilteringObjectInputStream}.
     *
     * @param in The {@link InputStream}
     * @param optContext An optional context object from which class loading has to be done
     * @param optClassResolver The class resolver or <code>null</code>
     * @param config The configuration to use
     * @throws IOException If an I/O error occurs
     */
    FilteringObjectInputStream(InputStream in, Object optContext, ClassResolver optClassResolver, SerializationFilteringConfig config) throws IOException {
        super(in);
        Set<ClassResolver> classResolvers = new LinkedHashSet<ClassResolver>(8);
        if (null != optClassResolver) {
            classResolvers.add(optClassResolver);
        }
        if (null != optContext) {
            classResolvers.add(new ClassLoaderClassResolver(optContext.getClass().getClassLoader()));
        }
        this.classResolvers = classResolvers;
        this.config = config;
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass input) throws IOException, ClassNotFoundException {
        String name = input.getName();

        for (Pattern blackPattern : config.getBlacklist()) {
            if (blackPattern.matcher(name).find()) {
                LoggerHolder.LOG.error("Blocked by blacklist '{}'. Match found for '{}'", blackPattern.pattern(), name);
                throw new InvalidClassException(name, "Class blocked from deserialization (blacklist)");
            }
        }

        for (ClassResolver classResolver : classResolvers) {
            try {
                Class<?> clazz = classResolver.resolveClass(name);
                if (clazz != null) {
                    classResolvers.add(new ClassLoaderClassResolver(clazz.getClassLoader()));
                    return clazz;
                }
            } catch (@SuppressWarnings("unused") Exception e) {
                // Ignore
            }
        }

        return super.resolveClass(input);

    }

}
