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

package com.openexchange.chronos.scheduling.changes.impl.desc;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import com.openexchange.chronos.EventField;
import com.openexchange.chronos.scheduling.changes.Description;
import com.openexchange.chronos.scheduling.changes.Sentence;
import com.openexchange.chronos.scheduling.changes.impl.SentenceImpl;

/**
 * {@link DefaultDescription}
 *
 * @author <a href="mailto:daniel.becker@open-xchange.com">Daniel Becker</a>
 * @since v7.10.3
 */
public class DefaultDescription implements Description {

    private final List<EventField> fields;
    private final List<Sentence> sentences;

    /**
     * Initializes a new {@link DefaultDescription}.
     *
     * @param sentence The sentence
     * @param field The field
     */
    public DefaultDescription(SentenceImpl sentence, EventField field) {
        this(Collections.singletonList(sentence), Collections.singletonList(field));
    }

    /**
     * Initializes a new {@link DefaultDescription}.
     *
     * @param sentences The sentences
     * @param field The field
     */
    public DefaultDescription(List<SentenceImpl> sentences, EventField field) {
        this(sentences, Collections.singletonList(field));
    }

    /**
     * Initializes a new {@link DefaultDescription}.
     *
     * @param sentences The sentences
     * @param fields The fields
     */
    public DefaultDescription(List<SentenceImpl> sentences, List<EventField> fields) {
        super();
        this.sentences = new ArrayList<>(sentences);
        this.fields = fields;
    }

    @Override
    public List<EventField> getChangedFields() {
        return fields;
    }

    @Override
    public List<Sentence> getSentences() {
        return sentences;
    }

}
