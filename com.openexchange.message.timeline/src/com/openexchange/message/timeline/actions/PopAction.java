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

package com.openexchange.message.timeline.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.json.JSONArray;
import org.json.JSONException;
import com.openexchange.ajax.requesthandler.AJAXRequestResult;
import com.openexchange.exception.OXException;
import com.openexchange.message.timeline.Message;
import com.openexchange.message.timeline.MessageTimelineManagement;
import com.openexchange.message.timeline.MessageTimelineRequest;
import com.openexchange.server.ServiceLookup;


/**
 * {@link PopAction} - The 'pop' action.
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public final class PopAction extends AbstractMessageTimelineAction {

    /**
     * Initializes a new {@link PopAction}.
     */
    public PopAction(final ServiceLookup services, final Map<String, AbstractMessageTimelineAction> actions) {
        super(services, actions);
    }

    @Override
    protected AJAXRequestResult perform(final MessageTimelineRequest msgTimelineRequest) throws OXException, JSONException {
        // Check client identifier
        final String client = checkClient(msgTimelineRequest);

        // Get appropriate queue(s)
        final List<Message> messages = new ArrayList<Message>(16);
        if ("*".equals(client) || "all".equalsIgnoreCase(client)) {
            final List<BlockingQueue<Message>> queues = MessageTimelineManagement.getInstance().getQueuesFor(msgTimelineRequest.getSession());
            for (final BlockingQueue<Message> queue : queues) {
                queue.drainTo(messages);
            }
        } else {
            final BlockingQueue<Message> queue = MessageTimelineManagement.getInstance().getQueueFor(msgTimelineRequest.getSession(), client);
            queue.drainTo(messages);
        }

        // Sort according to time stamp
        Collections.sort(messages);

        // Output as JSON array
        final JSONArray jArray = new JSONArray(messages.size());
        for (final Message m : messages) {
            jArray.put(m.jsonValue);
        }
        return new AJAXRequestResult(jArray, "json");
    }

    @Override
    public String getAction() {
        return "pop";
    }

}
