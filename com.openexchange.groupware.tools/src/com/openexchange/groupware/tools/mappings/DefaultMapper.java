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

package com.openexchange.groupware.tools.mappings;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import com.openexchange.exception.OXException;
import com.openexchange.tools.arrays.Arrays;

/**
 * {@link DefaultMapper} - Abstract {@link Mapping} implementation.
 *
 * @param <O> the type of the object
 * @param <E> the enum type for the fields
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public abstract class DefaultMapper<O, E extends Enum<E>> implements Mapper<O, E> {

	@Override
	public Mapping<? extends Object, O> get(final E field) throws OXException {
		final Mapping<? extends Object, O> mapping = opt(field);
		if (null == mapping) {
			throw OXException.notFound(field.toString());
		}
		return mapping;
	}

    @Override
    public Mapping<? extends Object, O> opt(final E field) {
        if (null == field) {
            throw new IllegalArgumentException("field");
        }
        return getMappings().get(field);
    }

	@Override
	public void mergeDifferences(final O original, final O update) throws OXException {
		if (null == original) {
			throw new IllegalArgumentException("original");
		}
		if (null == update) {
			throw new IllegalArgumentException("update");
		}
		for (final Mapping<? extends Object, O> mapping : getMappings().values()) {
			if (mapping.isSet(update)) {
				mapping.copy(update, original);
			}
		}
	}

    @Override
    public O getDifferences(final O original, final O update) throws OXException {
        if (null == original) {
            throw new IllegalArgumentException("original");
        }
        if (null == update) {
            throw new IllegalArgumentException("update");
        }
        final O delta = newInstance();
        for (final Mapping<? extends Object, O> mapping : getMappings().values()) {
            if (mapping.isSet(update) && (false == mapping.isSet(original) || false == mapping.equals(original, update))) {
                mapping.copy(update, delta);
            }
        }
        return delta;
    }

    @Override
    public E[] getDifferentFields(O original, O update) throws OXException {
        if (null == original) {
            throw new IllegalArgumentException("original");
        }
        if (null == update) {
            throw new IllegalArgumentException("update");
        }
        Set<E> differentFields = new HashSet<E>();
        for (Entry<E, ? extends Mapping<? extends Object, O>> entry : getMappings().entrySet()) {
            Mapping<? extends Object, O> mapping = entry.getValue();
            if (mapping.isSet(update) && (false == mapping.isSet(original) || false == mapping.equals(original, update))) {
                differentFields.add(entry.getKey());
            }
        }
        return differentFields.toArray(newArray(differentFields.size()));
    }

    @Override
    public Set<E> getDifferentFields(O original, O update, boolean considerUnset, E... ignoredFields) throws OXException {
        if (null == original) {
            throw new IllegalArgumentException("original");
        }
        if (null == update) {
            throw new IllegalArgumentException("update");
        }
        Set<E> differentFields = new HashSet<E>();
        for (Entry<E, ? extends Mapping<? extends Object, O>> entry : getMappings().entrySet()) {
            if (null != ignoredFields && Arrays.contains(ignoredFields, entry.getKey())) {
                continue;
            }
            Mapping<? extends Object, O> mapping = entry.getValue();
            if (mapping.isSet(update) && ((considerUnset || mapping.isSet(original)) && false == mapping.equals(original, update))) {
                differentFields.add(entry.getKey());
            }
        }
        return differentFields;
    }

	@Override
	public E[] getAssignedFields(final O object) {
		if (null == object) {
			throw new IllegalArgumentException("object");
		}
		final Set<E> setFields = new HashSet<E>();
		for (final Entry<E, ? extends Mapping<? extends Object, O>> entry : getMappings().entrySet()) {
			if (entry.getValue().isSet(object)) {
				setFields.add(entry.getKey());
			}
		}
		return setFields.toArray(newArray(setFields.size()));
	}

    @Override
    public O copy(O from, O to, E... fields) throws OXException {
        if (null == from) {
            throw new IllegalArgumentException("from");
        }
        if (null == to) {
            to = newInstance();
        }
        if (null == fields) {
            for (Mapping<? extends Object, O> mapping : getMappings().values()) {
                if (mapping.isSet(from)) {
                    mapping.copy(from, to);
                }
            }
        } else {
            for (E field : fields) {
                Mapping<? extends Object, O> mapping = get(field);
                if (mapping.isSet(from)) {
                    mapping.copy(from, to);
                }
            }
        }
        return to;
    }

    @Override
    public List<O> copy(List<O> objects, E... fields) throws OXException {
        if (null == objects) {
            return null;
        }
        List<O> copiedObjects = new ArrayList<O>(objects.size());
        for (O object : objects) {
            copiedObjects.add(copy(object, newInstance(), fields));
        }
        return copiedObjects;
    }

	/**
	 * Gets the mappings for all possible values of the underlying enum.
	 *
	 * @return the mappings
	 */
	protected abstract EnumMap<E, ? extends Mapping<? extends Object, O>> getMappings();

}
