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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.realtime.atmosphere.impl.stanza.builder;

import org.json.JSONObject;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.packet.ID;
import com.openexchange.realtime.packet.IQ;

/**
 * {@link IQBuilder} - Parse an atmosphere client's IQ message and build a IQ Stanza from it by adding the recipients ID.
 * 
 * @author <a href="mailto:marc.arens@open-xchange.com">Marc Arens</a>
 */
public class IQBuilder extends StanzaBuilder<IQ> {

    /**
     * Create a new IQBuilder
     * Initializes a new {@link IQBuilder}.
     * 
     * @param from the sender's ID, must not be null
     * @param json the sender's message, must not be null
     * @throws IllegalArgumentException if from or json are null
     */
    public IQBuilder(ID from, JSONObject json) {
        if (from == null || json == null) {
            throw new IllegalArgumentException();
        }
        this.from = from;
        this.json = json;
        this.stanza = new IQ();
    }

    @Override
    public IQ build() throws OXException {
        basics();
        type();
        return stanza;
    }

    /**
     * Check for the obligatory type key of IQ Stanzas in the received json and set the value in the Stanza 
     * @throws OXException if the type key is missing
     */
    private void type() throws OXException {
        throw new UnsupportedOperationException("Not implemented yet!");
//        String type = json.optString("type");
//        if (type == null) {
//            throw AtmosphereExceptionCode.MISSING_KEY.create("type", json);
//        }
//        for (IQ.Type t : IQ.Type.values()) {
//            if (t.name().equalsIgnoreCase(type)) {
//                stanza.setType(t);
//                break;
//            }
//        }
    }
    
}
