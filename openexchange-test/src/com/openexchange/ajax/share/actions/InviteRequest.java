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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.ajax.share.actions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.framework.AJAXRequest;
import com.openexchange.ajax.framework.AbstractAJAXParser;
import com.openexchange.ajax.framework.Header;
import com.openexchange.share.ShareTarget;
import com.openexchange.share.recipient.ShareRecipient;


/**
 * {@link InviteRequest}
 *
 * @author <a href="mailto:steffen.templin@open-xchange.com">Steffen Templin</a>
 */
public class InviteRequest implements AJAXRequest<InviteResponse> {

    private final boolean failOnError;

    private final List<ShareTarget> targets = new ArrayList<ShareTarget>();

    private final List<ShareRecipient> recipients = new ArrayList<ShareRecipient>();
    
    private String message;

    public InviteRequest() {
        this(Collections.<ShareTarget>emptyList(), Collections.<ShareRecipient>emptyList(), true);
    }

    public InviteRequest(List<ShareTarget> targets, List<ShareRecipient> recipients) {
        this(targets, recipients, true);
    }

    public InviteRequest(List<ShareTarget> targets, List<ShareRecipient> recipients, boolean failOnError) {
        this(targets, recipients, failOnError, null);
    }
    
    public InviteRequest(List<ShareTarget> targets, List<ShareRecipient> recipients, boolean failOnError, String message) {
        super();
        this.targets.addAll(targets);
        this.recipients.addAll(recipients);
        this.failOnError = failOnError;
        this.message = message;
    }

    public void addTarget(ShareTarget target) {
        targets.add(target);
    }

    public void addRecipient(ShareRecipient recipient) {
        recipients.add(recipient);
    }
    
    /**
     * Gets the message
     *
     * @return The message
     */
    public String getMessage() {
        return message;
    }

    /**
     * Sets the message
     *
     * @param message The message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public Method getMethod() {
        return Method.PUT;
    }

    @Override
    public String getServletPath() {
        return "/ajax/share/management";
    }

    @Override
    public Parameter[] getParameters() throws IOException, JSONException {
        return new Parameter[] { new URLParameter(AJAXServlet.PARAMETER_ACTION, "invite") };
    }

    @Override
    public AbstractAJAXParser<? extends InviteResponse> getParser() {
        return new AbstractAJAXParser<InviteResponse>(failOnError) {
            @Override
            protected InviteResponse createResponse(Response response) throws JSONException {
                return new InviteResponse(response);
            }
        };
    }

    @Override
    public Object getBody() throws IOException, JSONException {
        JSONObject json = new JSONObject();
        json.put("targets", ShareWriter.writeTargets(targets));
        json.put("recipients", ShareWriter.writeRecipients(recipients));
        if(message != null) {
            json.put("message", message);
        }
        return json;
    }

    @Override
    public Header[] getHeaders() {
        return NO_HEADER;
    }

}
