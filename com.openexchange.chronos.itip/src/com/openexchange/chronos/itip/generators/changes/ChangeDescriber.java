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

package com.openexchange.chronos.itip.generators.changes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.itip.generators.Sentence;
import com.openexchange.chronos.itip.generators.TypeWrapper;
import com.openexchange.chronos.itip.generators.changes.generators.ChangeDescriptionProvider;
import com.openexchange.chronos.itip.tools.ITipEventUpdate;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;

/**
 * {@link ChangeDescriber}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class ChangeDescriber {

    private final ChangeDescriptionGenerator[] generators;

    /**
     * 
     * Initializes a new {@link ChangeDescriber} with all available {@link ChangeDescriptionGenerator}.
     *
     */
    public ChangeDescriber() {
        this(ChangeDescriptionProvider.getGenerators());
    }

    public ChangeDescriber(ChangeDescriptionGenerator... generators) {
        this.generators = generators;
    }

    public List<String> getChanges(Context ctx, Event original, Event update, ITipEventUpdate diff, TypeWrapper wrapper, Locale locale, TimeZone timezone, int recipientUserId) throws OXException {
        if (diff == null) {
            return Collections.emptyList();
        }

        List<String> changeDescriptions = new ArrayList<String>(generators.length);
        for (ChangeDescriptionGenerator generator : generators) {
            if (diff.containsAnyChangeOf(generator.getFields())) {
                List<Sentence> descriptions = generator.getDescriptions(ctx, original, update, diff, locale, timezone, recipientUserId);
                if (null != descriptions) {
                    for (Sentence changeDescription : descriptions) {
                        changeDescriptions.add(changeDescription.getMessage(wrapper, locale));
                    }
                }
            }
        }
        return changeDescriptions;
    }
}
