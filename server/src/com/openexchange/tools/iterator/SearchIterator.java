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

package com.openexchange.tools.iterator;

import com.openexchange.api2.OXException;
import com.openexchange.groupware.AbstractOXException;

/**
 * {@link SearchIterator} - An extended iterator over a collection.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public interface SearchIterator<T> {

    SearchIterator<?> EMPTY_ITERATOR = new SearchIterator<Object>() {

        public boolean hasNext() {
            return false;
        }

        public Object next() throws SearchIteratorException, OXException {
            return null;
        }

        public void close() throws SearchIteratorException {
            // Not applicable
        }

        public int size() {
            return 0;
        }

        public boolean hasSize() {
            return true;
        }

        public void addWarning(final AbstractOXException warning) {
            // Not applicable
        }

        public AbstractOXException[] getWarnings() {
            return null;
        }

        public boolean hasWarnings() {
            return false;
        }

    };

    /**
     * Returns <code>true</code> if the iteration has more elements. (In other words, returns <code>true</code> if {@link #next()} would
     * return an element.)
     * 
     * @return <code>true</code> if the iterator has more elements; otherwise <code>false</code>
     */
    boolean hasNext();

    /**
     * Returns the next element in the iteration. Calling this method repeatedly until the {@link #hasNext()} method returns
     * <code>false</code> will return each element in the underlying collection exactly once.
     * 
     * @return The next element in the iteration.
     * @exception SearchIteratorException If next element cannot be returned
     * @throws OXException If next element cannot be returned
     */
    T next() throws SearchIteratorException, OXException;

    /**
     * Closes the search iterator
     * 
     * @throws SearchIteratorException If closing the search iterator fails
     */
    void close() throws SearchIteratorException;

    /**
     * This iterator's size
     * 
     * @return The size
     */
    int size();

    /**
     * Indicates if this iterator's size is accessible via {@link #size()}
     * 
     * @return <code>true</code> if this iterator's size is accessible via {@link #size()}; otherwise <code>false</code>
     */
    boolean hasSize();

    /**
     * Indicates if this iterator has warnings
     * 
     * @return <code>true</code> if this iterator has warnings; otherwise <code>false</code>
     */
    boolean hasWarnings();

    /**
     * Adds specified warning to this iterator's warnings
     * 
     * @param warning The warning to add
     */
    void addWarning(AbstractOXException warning);

    /**
     * Gets the iterator's warnings as an array
     * 
     * @return The iterator's warnings as an array or <code>null</code>
     */
    AbstractOXException[] getWarnings();

}
