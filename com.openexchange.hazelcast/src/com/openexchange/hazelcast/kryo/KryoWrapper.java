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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.hazelcast.kryo;

import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * {@link KryoWrapper} - Wraps any arbitrary object intend to be serialzed/deserialized using <a
 * href="http://code.google.com/p/kryo/">Kryo</a>.<br>
 * <img src="http://kryo.googlecode.com/svn/wiki/kryo-logo.jpg" width="88" height="37">
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class KryoWrapper implements Serializable {

    private static final ThreadLocal<ClassLoader> CLASS_LOADER = new ThreadLocal<ClassLoader>();

    /**
     * Sets thread-local class loader.
     *
     * @param classLoader The class loader
     */
    public static void setClassLoader(final Class<?> clazz) {
        CLASS_LOADER.set(clazz.getClassLoader());
    }

    /**
     * Sets thread-local class loader.
     *
     * @param classLoader The class loader
     */
    public static void setClassLoader(final ClassLoader classLoader) {
        CLASS_LOADER.set(classLoader);
    }

    /**
     * Discards previously set thread-local class loader.
     */
    public static void unsetClassLoader() {
        CLASS_LOADER.set(null);
    }

    private static final long serialVersionUID = 8626996938143001455L;

    private final byte[] bytes;

    /**
     * Initializes a new {@link KryoWrapper}.
     *
     * @param target The object to serialize with Kryo
     */
    public KryoWrapper(final Object target) {
        super();
        bytes = KryoSerializer.write(target, CLASS_LOADER.get());
    }

    /**
     * Classes that need to designate a replacement when an instance of it is read from the stream should implement this special method with
     * the exact signature.
     *
     * <PRE>
     * ANY-ACCESS-MODIFIER Object readResolve() throws ObjectStreamException;
     * </PRE>
     *
     * @return The deserialized object
     * @throws ObjectStreamException If deserialization fails
     */
    private Object readResolve() throws ObjectStreamException {
        return KryoSerializer.read(bytes, CLASS_LOADER.get());
    }

}
