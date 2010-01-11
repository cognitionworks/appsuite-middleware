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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.report.client.impl;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.List;
import javax.net.ssl.HttpsURLConnection;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.report.client.configuration.ReportConfiguration;
import com.openexchange.report.client.container.ContextDetail;
import com.openexchange.report.client.container.ContextModuleAccessCombination;
import com.openexchange.report.client.container.Total;

public class TransportHandler {
	
	private static final String REPORT_SERVER_URL = "activation.open-xchange.com";
	
	private static final String REPORT_SERVER_CLIENT_AUTHENTICATION_STRING = "rhadsIsAgTicOpyodNainPacloykAuWyribZydkarbEncherc4";
	
	private static final String POST_CLIENT_AUTHENTICATION_STRING_KEY = "clientauthenticationstring";
	
	private static final String POST_LICENSE_KEYS_KEY = "license_keys";

	private static final String POST_METADATA_KEY = "client_information";
	
	private static final String URL_ENCODING = "UTF-8";
	
	public TransportHandler() {	}
	
    protected void sendReport(List<Total> totals, List<ContextDetail> contextDetails, String[] versions) throws IOException, JSONException {
    	JSONObject metadata = buildJSONObject(totals, contextDetails, versions);
    	
    	ReportConfiguration reportConfiguration = new ReportConfiguration();

        StringBuffer report = new StringBuffer();
        report.append(POST_CLIENT_AUTHENTICATION_STRING_KEY);
        report.append("=");
        report.append(URLEncoder.encode(REPORT_SERVER_CLIENT_AUTHENTICATION_STRING, URL_ENCODING));
        report.append("&");
        report.append(POST_LICENSE_KEYS_KEY);
        report.append("=");
        report.append(URLEncoder.encode(reportConfiguration.getLicenseKeys(), URL_ENCODING));
        report.append("&");
        report.append(POST_METADATA_KEY);
        report.append("=");
        report.append(URLEncoder.encode(metadata.toString(), URL_ENCODING));

        HttpsURLConnection httpsURLConnection = (HttpsURLConnection) new URL("https://"+REPORT_SERVER_URL+"/").openConnection();
        httpsURLConnection.setUseCaches(false);
        httpsURLConnection.setDoOutput(true);
        httpsURLConnection.setDoInput(true);
        httpsURLConnection.setRequestMethod("POST");
        httpsURLConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        DataOutputStream stream = new DataOutputStream(httpsURLConnection.getOutputStream());
        stream.writeBytes(report.toString());
        stream.flush();
        stream.close();

        if (httpsURLConnection.getResponseCode() != HttpsURLConnection.HTTP_OK) {
            throw new MalformedURLException("Problem contacting report server: " + httpsURLConnection.getResponseCode());
        }
        BufferedReader in = new BufferedReader(new InputStreamReader(httpsURLConnection.getInputStream()));
        String buffer = "";
        while ((buffer = in.readLine()) != null) {
            System.out.println(new StringBuilder().append(REPORT_SERVER_URL).append(" said: ").append(buffer).toString());
        }
        in.close();
    }

    
    private JSONObject buildJSONObject(List<Total> totals, List<ContextDetail> contextDetails, String[] versions) throws JSONException {
    	JSONObject retval = new JSONObject();

    	JSONObject total = new JSONObject();
    	JSONObject detail = new JSONObject();
    	JSONObject version = new JSONObject();

    	for (Total tmp : totals) {
    		total.put("contexts", tmp.getContexts());
    		total.put("users", tmp.getUsers());
    	}
    	
    	for (ContextDetail tmp : contextDetails) {
    		JSONObject contextDetailObjectJSON = new JSONObject();
    		contextDetailObjectJSON.put("id", tmp.getId());
    		contextDetailObjectJSON.put("age", tmp.getAge());
    		contextDetailObjectJSON.put("created", tmp.getCreated());
    		
    		JSONObject moduleAccessCombinations = new JSONObject();
    		for (ContextModuleAccessCombination moduleAccessCombination : tmp.getModuleAccessCombinations()) {
    			JSONObject moduleAccessCombinationJSON = new JSONObject();
    			moduleAccessCombinationJSON.put("mac", moduleAccessCombination.getUserAccessCombination());
    			moduleAccessCombinationJSON.put("users", moduleAccessCombination.getUserCount());
    			moduleAccessCombinations.put(moduleAccessCombination.getUserAccessCombination(), moduleAccessCombinationJSON);
    		}
    		contextDetailObjectJSON.put("macs", moduleAccessCombinations);
    		detail.put(tmp.getId(), contextDetailObjectJSON);
    	}
    	
    	version.put("admin", versions[0]);
    	version.put("groupware", versions[1]);
    	    	
    	retval.put("total", total);
    	retval.put("detail", detail);
    	retval.put("version", version);

    	return retval;
    }
	
}
