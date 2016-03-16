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

package com.openexchange.tools.versit.old;

import java.io.IOException;
import com.openexchange.tools.versit.Property;
import com.openexchange.tools.versit.StringScanner;
import com.openexchange.tools.versit.VersitException;

public class OldPriorityPropertyDefinition extends OldIntegerPropertyDefinition {

    public OldPriorityPropertyDefinition(final String[] paramNames, final OldParamDefinition[] params) {
        super(paramNames, params);
    }

    @Override
    protected Object parseValue(final Property property, final StringScanner s) throws IOException {
        final int[] mapping = { 1, 3, 5, 7, 9 };
        final int prio = Integer.parseInt(s.getRest());
        if (prio < 1 || prio > 5) {
            throw new VersitException(s, "Invalid priority: " + prio);
        }
        return Integer.valueOf(mapping[prio - 1]);
    }

    @Override
    protected String writeValue(final Property property, final Object value) {
        final String[] mapping = { "1", "1", "2", "2", "3", "4", "4", "5", "5" };
        final int prio = ((Integer) value).intValue();
        if (prio < 1 || prio > 9) {
            return "0";
        }
        return mapping[prio - 1];
    }

}
