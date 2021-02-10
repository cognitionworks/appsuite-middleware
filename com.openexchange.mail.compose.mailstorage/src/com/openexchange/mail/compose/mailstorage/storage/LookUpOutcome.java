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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.mail.compose.mailstorage.storage;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import com.openexchange.mail.MailPath;

/**
 * {@link LookUpOutcome} - The result for a look-up of draft mails associated with a composition space.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.5
 */
public class LookUpOutcome {

    /** The empty look-up result */
    public static final LookUpOutcome EMPTY = new LookUpOutcome(Collections.emptyMap(), Collections.emptyMap());

    private final Map<MailPath, UUID> draftPath2CompositionSpaceId;
    private final Map<MailPath, UUID> duplicateCompositionSpaces;

    /**
     * Initializes a new {@link LookUpOutcome}.
     *
     * @param draftPath2compositionSpaceId The mapping of draft path to composition space identifier
     * @param duplicateCompositionSpaces The paths of those draft messages which are considered as duplicates of a composition space
     */
    public LookUpOutcome(Map<MailPath, UUID> draftPath2compositionSpaceId, Map<MailPath, UUID> duplicateCompositionSpaces) {
        super();
        this.draftPath2CompositionSpaceId = draftPath2compositionSpaceId;
        this.duplicateCompositionSpaces = duplicateCompositionSpaces;
    }

    /**
     * Gets the mapping of draft path to composition space identifier.
     *
     * @return The mapping of draft path to composition space identifier
     */
    public Map<MailPath, UUID> getDraftPath2CompositionSpaceId() {
        return draftPath2CompositionSpaceId;
    }

    /**
     * Gets the paths of those draft messages which are considered as duplicates of a composition space.
     *
     * @return The paths of those draft messages which are considered as duplicates of a composition space
     */
    public Map<MailPath, UUID> getDuplicateCompositionSpaces() {
        return duplicateCompositionSpaces;
    }

}
