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

package com.openexchange.logback.extensions.logstash;

import java.util.Stack;
import org.xml.sax.Attributes;
import ch.qos.logback.core.joran.action.Action;
import ch.qos.logback.core.joran.spi.ActionException;
import ch.qos.logback.core.joran.spi.InterpretationContext;

/**
 * {@link CustomFieldsAction}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class CustomFieldsAction extends Action {

    /**
     * Initialises a new {@link CustomFieldsAction}.
     */
    public CustomFieldsAction() {
        super();
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.qos.logback.core.joran.action.Action#begin(ch.qos.logback.core.joran.spi.InterpretationContext, java.lang.String, org.xml.sax.Attributes)
     */
    @Override
    public void begin(InterpretationContext ic, String name, Attributes attributes) throws ActionException {
        Stack<Object> objectStack = ic.getObjectStack();
        for (Object o : objectStack) {
            if (o instanceof LogstashEncoder) {
                LogstashEncoder encoder = (LogstashEncoder) o;
                encoder.addCustomField(new CustomField(attributes.getValue("name"), attributes.getValue("value")));
                break;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see ch.qos.logback.core.joran.action.Action#end(ch.qos.logback.core.joran.spi.InterpretationContext, java.lang.String)
     */
    @Override
    public void end(InterpretationContext ic, String name) throws ActionException {
        // no-op
    }
}
