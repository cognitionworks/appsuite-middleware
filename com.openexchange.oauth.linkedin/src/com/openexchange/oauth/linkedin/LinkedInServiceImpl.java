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

package com.openexchange.oauth.linkedin;

import java.util.LinkedList;
import java.util.List;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.scribe.builder.ServiceBuilder;
import org.scribe.builder.api.LinkedInApi;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.container.Contact;
import com.openexchange.oauth.OAuthAccount;
import com.openexchange.oauth.OAuthServiceMetaData;
import com.openexchange.oauth.linkedin.osgi.Activator;
import com.openexchange.session.Session;

/**
 * {@link LinkedInServiceImpl}
 *
 * @author <a href="mailto:karsten.will@open-xchange.com">Karsten Will</a>
 * @author <a href="mailto:tobias.prinz@open-xchange.com">Tobias Prinz</a>
 */
public class LinkedInServiceImpl implements LinkedInService{

    private static final String PERSONAL_FIELDS = "id,first-name,last-name,phone-numbers,headline,im-accounts,twitter-accounts,date-of-birth,main-address,picture-url,positions,industry";
    private static final String RELATION_TO_VIEWER = "relation-to-viewer:(connections:(person:(id,first-name,last-name,picture-url,headline)))";
	private static final String PERSONAL_FIELD_QUERY = ":("+PERSONAL_FIELDS+")";
	private static final String CONNECTIONS_URL = "http://api.linkedin.com/v1/people/~/connections:(id,first-name,last-name,phone-numbers,im-accounts,twitter-accounts,date-of-birth,main-address,picture-url,positions)";
    private static final String IN_JSON = "?format=json";

    private Activator activator;

    private static final Log LOG = com.openexchange.log.Log.valueOf(LogFactory.getLog(LinkedInServiceImpl.class));


    public LinkedInServiceImpl(final Activator activator) {
        this.activator = activator;
    }

    public Activator getActivator() {
        return activator;
    }

    public void setActivator(final Activator activator) {
        this.activator = activator;
    }


    public Response performRequest(final Session session, final int user, final int contextId, final int accountId, final Verb method, final String url) {
        final OAuthServiceMetaData linkedInMetaData = new OAuthServiceMetaDataLinkedInImpl(activator);

        final OAuthService service = new ServiceBuilder().provider(LinkedInApi.class).apiKey(linkedInMetaData.getAPIKey()).apiSecret(
            linkedInMetaData.getAPISecret()).build();

        OAuthAccount account = null;
        try {
            final com.openexchange.oauth.OAuthService oAuthService = activator.getOauthService();
            account = oAuthService.getAccount(accountId, session, user, contextId);
        } catch (final OXException e) {
            LOG.error(e);
        }

        final Token accessToken = new Token(account.getToken(), account.getSecret());
        final OAuthRequest request = new OAuthRequest(method, url);
        service.signRequest(accessToken, request);
        return request.send();
    }


	private JSONObject extractJson(final Response response) throws OXException {
		JSONObject json;
		try {
			json = new JSONObject(response.getBody());
		} catch (final JSONException e) {
			LOG.error(e);
			throw OXException.general("Could not parse JSON: " + response.getBody()); //TODO: Different exception - wasn't this supposed to get easier with the rewrite?
		}
        return json;
	}


	protected List<String> extractIds(final Response response) throws OXException{
		List<String> result = new LinkedList<String>();
		try {
			final JSONObject json = new JSONObject(response.getBody());
			final JSONArray ids = json.getJSONArray("values");
			result = extractIds(ids);
		} catch (final JSONException e) {

		}
		return result;
	}

	protected List<String> extractIds(final JSONArray connections) throws OXException{
		final List<String> result = new LinkedList<String>();
		try {
			for(int i = 0, max = connections.length(); i < max; i++){
				result.add(connections.getJSONObject(i).getString("id"));
			}
		} catch (final JSONException e) {

		}
		return result;
	}


    @Override
    public String getAccountDisplayName(final Session session, final int user, final int contextId, final int accountId) {
        String displayName="";
        try {
            final com.openexchange.oauth.OAuthService oAuthService = activator.getOauthService();
            final OAuthAccount account = oAuthService.getAccount(accountId, session, user, contextId);
            displayName = account.getDisplayName();
        } catch (final OXException e) {
            LOG.error(e);
        }
        return displayName;
    }


    @Override
    public List<Contact> getContacts(final Session session, final int user, final int contextId, final int accountId) {
    	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, CONNECTIONS_URL);
    	final LinkedInXMLParser parser = new LinkedInXMLParser();
        final List<Contact> contacts = parser.parseConnections(response.getBody());
        return contacts;
    }


	@Override
	public JSONObject getProfileForId(final String id, final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/id="+id+PERSONAL_FIELD_QUERY;
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
    	return extractJson(response);
	}


	@Override
	public JSONObject getRelationToViewer(final String id, final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/id="+id+":(relation-to-viewer)";
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
    	final JSONObject relations = extractJson(response);
    	return relations;
	}

	@Override
	public JSONObject getConnections(final Session session, final int user, final int contextId,	final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/~/connections"+PERSONAL_FIELD_QUERY;
		final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
		return extractJson(response);
	}


	@Override
	public List<String> getUsersConnectionsIds(final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/~/connections:(id)";
		final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
		return extractIds(response);
	}

	public JSONObject getFullProfileById(final String id, final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/id="+id+":("+RELATION_TO_VIEWER+","+PERSONAL_FIELDS+")";
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
    	final JSONObject data = extractJson(response);
    	return data;
	}

    @Override
    public JSONObject getFullProfileByEMail(final List<String> email, final Session session, final int user, final int contextId, final int accountId) throws OXException {

		String uri = null;
		if (email.size() == 1) {
			uri = "http://api.linkedin.com/v1/people/email="+email.get(0)+":("+RELATION_TO_VIEWER+","+PERSONAL_FIELDS+")";
		} else {
			final StringBuilder b = new StringBuilder("http://api.linkedin.com/v1/people::(");
			for(final String s : email) {
				b.append("email=").append(s).append(',');
			}
			b.setLength(b.length()-1);
			b.append("):(").append(RELATION_TO_VIEWER).append(',').append(PERSONAL_FIELDS).append(')');
			uri = b.toString();
		}
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri + IN_JSON);
    	final JSONObject data = extractJson(response);
    	return data;
    }

	@Override
	public JSONObject getNetworkUpdates(final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/~/network/updates" + IN_JSON + "&type=CONN";
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri);
    	final JSONObject data = extractJson(response);
    	return data;
	}

	@Override
	public JSONObject getMessageInbox(final Session session, final int user, final int contextId, final int accountId) throws OXException {
		final String uri = "http://api.linkedin.com/v1/people/~/mailbox:(id,folder,from:(person:(id,first-name,last-name,picture-url,headline)),recipients:(person:(id,first-name,last-name,picture-url,headline)),subject,short-body,last-modified,timestamp,mailbox-item-actions,body)?message-type=message-connections,invitation-request,invitation-reply,inmail-direct-connection&format=json";
	   	final Response response = performRequest(session, user, contextId, accountId, Verb.GET, uri);
    	final JSONObject data = extractJson(response);
    	System.out.println(data);
    	return data;
	}

}
