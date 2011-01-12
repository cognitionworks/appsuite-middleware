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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.config.cascade.impl;

import com.openexchange.config.cascade.BasicProperty;
import com.openexchange.config.cascade.ConfigProperty;
import com.openexchange.tools.strings.StringParser;

/**
 * {@link CoercingConfigProperty}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class CoercingConfigProperty<T> implements ConfigProperty<T> {

    private Class<T> type;

    private BasicProperty delegate;

    private StringParser parser;

    public CoercingConfigProperty(Class<T> type, BasicProperty delegate, StringParser parser) {
        super();
        this.type = type;
        this.delegate = delegate;
        this.parser = parser;
    }

    public T get() {
        String value = delegate.get();
        return parse(value, type);
    }

    private <S> S parse(String value, Class<S> s) {
        if (value == null) {
            return null;
        }

        S parsed = parser.parse(value, s);
        if (parsed == null) {
            // TOOD: Throw Exception
            return null;
        }
        return parsed;
    }

    public String get(String metadataName) {
        return delegate.get(metadataName);
    }

    public <M> M get(String metadataName, Class<M> m) {
        return parse(delegate.get(metadataName), m);
    }

    public boolean isDefined() {
        return delegate.isDefined();
    }

    public void set(T value) {
        delegate.set(value.toString()); // We assume good toString methods that allow reparsing
    }

    public <M> void set(String metadataName, M value) {
        delegate.set(metadataName, value.toString());
    }

}
