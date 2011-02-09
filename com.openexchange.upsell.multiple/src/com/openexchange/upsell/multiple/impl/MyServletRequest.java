package com.openexchange.upsell.multiple.impl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URLEncoder;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.mail.Address;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.httpclient.URIException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONException;
import org.json.JSONObject;
import com.openexchange.admin.rmi.OXContextInterface;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.NoSuchContextException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.api2.OXException;
import com.openexchange.config.ConfigurationService;
import com.openexchange.groupware.AbstractOXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserStorage;
import com.openexchange.mail.MailException;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.TextBodyMailPart;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.server.ServiceException;
import com.openexchange.session.Session;
import com.openexchange.tools.servlet.AjaxException;
import com.openexchange.upsell.multiple.api.UrlGeneratorException;
import com.openexchange.upsell.multiple.api.UrlService;
import com.openexchange.upsell.multiple.osgi.MyServiceRegistry;

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


/**
 * 
 * Servlet to trigger upsell actions like email or URL redirect.
 * 
 */
public final class MyServletRequest  {

	private final Session sessionObj;
	private User user;	
	private User admin;	
	private final Context ctx;
	private ConfigurationService configservice;
	
	
	private static final Log LOG = LogFactory.getLog(MyServletRequest.class);
	
	// HTTP API methods/parameters
	public static final String ACTION_GET_CONFIGURED_METHOD = "get_method"; // action to retrieve configured upsell method
	public static final String ACTION_GET_STATIC_REDIRECT_URL_METHOD = "get_static_redirect_url"; // 
	public static final String ACTION_GET_EXTERNAL_REDIRECT_URL_METHOD = "get_external_redirect_url"; // 
	public static final String ACTION_TRIGGER_UPSELL_EMAIL = "send_upsell_email"; // 
	public static final String ACTION_DOWNGRADE = "change_context_permissions"; // 
	
	
	// config options
	private static final String PROPERTY_METHOD_EXTERNAL_SHOP_API_URL = "com.openexchange.upsell.multiple.method.external.shop_api_url";
	private static final String PROPERTY_METHOD_STATIC_SHOP_REDIR_URL = "com.openexchange.upsell.multiple.method.static.shop_redir_url";
	private static final String PROPERTY_METHOD = "com.openexchange.upsell.multiple.method"; // one of: external, static, email, direct
	
	// email options
	private static final String PROPERTY_METHOD_EMAIL_ADDRESS = "com.openexchange.upsell.multiple.method.email.address";
	private static final String PROPERTY_METHOD_EMAIL_SUBJECT = "com.openexchange.upsell.multiple.method.email.subject";
	private static final String PROPERTY_METHOD_EMAIL_TEMPLATE = "com.openexchange.upsell.multiple.method.email.template";
	private static final String PROPERTY_METHOD_EMAIL_OXUSER_TEMPLATE = "com.openexchange.upsell.multiple.method.email.oxuser.template";
	private static final String PROPERTY_METHOD_EMAIL_OXUSER_SUBJECT_TEMPLATE = "com.openexchange.upsell.multiple.method.email.oxuser.template_subject";
	private static final String PROPERTY_METHOD_EMAIL_OXUSER_ENABLED = "com.openexchange.upsell.multiple.method.email.oxuser.enabled";
	
	// RMI API options
	private static final String PROPERTY_RMI_HOST = "com.openexchange.upsell.multiple.rmi.host";
	private static final String PROPERTY_RMI_MASTERADMIN = "com.openexchange.upsell.multiple.rmi.masteradmin";
	private static final String PROPERTY_RMI_MASTERADMIN_PWD = "com.openexchange.upsell.multiple.rmi.masteradmin.pass";
	private static final String PROPERTY_RMI_DOWNGRADE_NAME = "com.openexchange.upsell.multiple.rmi.downgrade.accessname";
	
	public MyServletRequest(final Session sessionObj, final Context ctx) throws OXException, ServiceException {		
		
		
		
		this.sessionObj = sessionObj;
		this.ctx = ctx;
		try {
			// load user for data
			this.user = UserStorage.getInstance().getUser(sessionObj.getUserId(), ctx);
			
			// load admin for custom data like redirect url
			this.admin = UserStorage.getInstance().getUser(this.ctx.getMailadmin(), ctx);
			
		} catch (final LdapException e) {
			LOG.error(e.getMessage(), e);
			throw new OXException(e);
		}		
		
		// init config 
		this.configservice = MyServiceRegistry.getServiceRegistry().getService(ConfigurationService.class,true); 
	}
	
	public Object action(final String action, final JSONObject jsonObject, final HttpServletRequest http_request) throws AbstractOXException, JSONException {
		Object retval = null;
		
		// Host/UI URL from where the request came, needed for different types of shops per domain/branding
		String request_src_hostname = http_request.getServerName();
		
		if(action.equalsIgnoreCase(ACTION_GET_CONFIGURED_METHOD)){
			// return configur�ed upsell method
			retval = actionGetUpsellMethod(jsonObject);
		}else if(action.equalsIgnoreCase(ACTION_GET_STATIC_REDIRECT_URL_METHOD)){
			// return static redirect URL containing all needed parameters
				retval = actionGetStaticRedirectURL(jsonObject,request_src_hostname);
		}else if(action.equalsIgnoreCase(ACTION_GET_EXTERNAL_REDIRECT_URL_METHOD)){
			// return the generated URL from the external system
			retval = actionGetExternalRedirectURL(jsonObject,request_src_hostname);
		}else if(action.equalsIgnoreCase(ACTION_DOWNGRADE)){
			// downgrade the context which is within this users session
			retval = actionUpDownGradeContext(jsonObject);
		}else if(action.equalsIgnoreCase(ACTION_TRIGGER_UPSELL_EMAIL)){
			// trigger and send email with all need params to configured email addy
			// UI must send feature, upsell package and hostname
			retval = actionTriggerEmailUpsell(jsonObject,request_src_hostname);
		}else {
			throw new AjaxException(AjaxException.Code.UnknownAction, action);
		}
		
		return retval;
	}
	
	private Object actionUpDownGradeContext(JSONObject json) throws MyServletException {
		
		try {
		
			String upsell_plan = getFromConfig(PROPERTY_RMI_DOWNGRADE_NAME); // fallback if not set
			
			if(json.has("upsell_plan")){
				upsell_plan = json.getString("upsell_plan");
			}
			
			final OXContextInterface iface = (OXContextInterface)Naming.lookup("rmi://"+getFromConfig(PROPERTY_RMI_HOST)+"/"+OXContextInterface.RMI_NAME);
		
			com.openexchange.admin.rmi.dataobjects.Context bla = new com.openexchange.admin.rmi.dataobjects.Context(this.sessionObj.getContextId());
			
			Credentials authcreds = new Credentials(getFromConfig(PROPERTY_RMI_MASTERADMIN),getFromConfig(PROPERTY_RMI_MASTERADMIN_PWD));
			
			iface.getAccessCombinationName(bla, authcreds);
			LOG.info("Current access combination name for context "+this.ctx.getContextId()+": "+iface.getAccessCombinationName(bla, authcreds));
			
			// update the level of the context
			iface.changeModuleAccess(bla,upsell_plan, authcreds);
			
			// get updated level to debug if it was correctly set
			iface.getAccessCombinationName(bla, authcreds);
			LOG.info("Updated access combination name for context "+this.ctx.getContextId()+" to: "+iface.getAccessCombinationName(bla, authcreds));
			
		} catch (MalformedURLException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (RemoteException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (NotBoundException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (StorageException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (InvalidCredentialsException e) {
			LOG.error("Invalid credentials supplied for OX API",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (NoSuchContextException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (InvalidDataException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (JSONException e) {
			LOG.error("Error changing context",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		} catch (ServiceException e) {
			LOG.error("Error changing context. Mandatory configuration option not found",e);
			throw new MyServletException(MyServletException.Code.API_COMMUNICATION_ERROR,e.getMessage());
		}
		
		

		
		
		return null;
	}

	private Object actionGetExternalRedirectURL(JSONObject jsonObject,String request_src_hostname) {
		
		
		return null;
	}
	
	
	
	/**
	 * 
	 * Send an upsell mail to configured email address with configured/parsed body and subject
	 * 
	 * @param jsonObject
	 * @param request_src_hostname
	 * @return
	 * @throws MyServletException
	 */
	private Object actionTriggerEmailUpsell(JSONObject jsonObject,String request_src_hostname) throws MyServletException {
		
		try {
			
			String email_addy_ox_user = this.user.getMail();
			String email_addy_provider = getFromConfig(PROPERTY_METHOD_EMAIL_ADDRESS);
			String subject = getFromConfig(PROPERTY_METHOD_EMAIL_SUBJECT);
			
				
			// load mail body template if exists
			String mailbody_provider = getTemplateContent(getFromConfig(PROPERTY_METHOD_EMAIL_TEMPLATE),false);
			if(mailbody_provider==null){
				mailbody_provider = subject;
			}
			
			subject = parseText(subject, jsonObject, false); // replace stuff for easier processing at customer
			mailbody_provider = parseText(mailbody_provider, jsonObject, false); // replace stuff in mail template
			
			// send mail to provider email addy
			sendUpsellEmail(email_addy_provider, email_addy_ox_user, mailbody_provider, subject);
			
			
			// prepare/send email to enduser if configured
			if(getFromConfig(PROPERTY_METHOD_EMAIL_OXUSER_ENABLED)!=null && 
			   getFromConfig(PROPERTY_METHOD_EMAIL_OXUSER_ENABLED).equalsIgnoreCase("true")){
								
				// first try to load i18n version, if not found, try to load generic one
				String oxuser_subject = getTemplateContent(getFromConfig(PROPERTY_METHOD_EMAIL_OXUSER_SUBJECT_TEMPLATE),true);
				
				
				if(oxuser_subject==null){
					oxuser_subject = subject; // fallback to general subject
				}else{
					oxuser_subject = parseText(oxuser_subject, jsonObject, false); // parse infos into the templates
				}
				
				
				String oxuser_body = getTemplateContent(getFromConfig(PROPERTY_METHOD_EMAIL_OXUSER_TEMPLATE),true);
				
				if(oxuser_body==null){
					oxuser_body = mailbody_provider; // fallback to general mailbody
				}else{
					oxuser_body = parseText(oxuser_body, jsonObject, false); // parse infos into the templates
				}
				
				sendUpsellEmail(email_addy_ox_user, email_addy_ox_user, oxuser_body, oxuser_subject);
				if(LOG.isDebugEnabled()){
					LOG.debug("Sent upsell request email to enduser with email address:"+email_addy_ox_user);
				}
			}
			
		} catch (ServiceException e) {
			LOG.error("Error reading mandatory configuration parameters for sending upsell email",e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());
		} catch (URIException e) {
			LOG.error("Error parsing upsell email text",e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());
		} catch (UnsupportedEncodingException e) {
			LOG.error("Error parsing upsell email text",e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());
		} catch (JSONException e) {
			LOG.error("Error processing upsell email text",e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());
		}
		
		return null;
	}
	
	
	private String getTemplateContent(String fulltemplatepath,boolean i18n){
		
		
		if (!i18n) {
			File templateFile = new File(fulltemplatepath);
			if (templateFile.exists() && templateFile.canRead() && templateFile.isFile()) {
				LOG.debug("Found and now using the upsell mail template at "+ fulltemplatepath);
				return getFileContents(templateFile);
			} else {
				LOG.error("Could not find an upsell mail template at "
						+ fulltemplatepath + ", using "
						+ PROPERTY_METHOD_EMAIL_SUBJECT + " as mail body.");
				return null;
			}
		} else {
			// load with langcode extension
			File templateFile_i18n = new File(fulltemplatepath + "_"+ this.user.getPreferredLanguage());
			File templateFile = new File(fulltemplatepath);
			// first try to load i18n file, then the fallback file
			if (templateFile_i18n.exists() && templateFile_i18n.canRead()
					&& templateFile_i18n.isFile()) {
				LOG.debug("Found and now using the i18n upsell mail template at "+ templateFile_i18n.getPath());
				return getFileContents(templateFile_i18n);
			} else if (templateFile.exists() && templateFile.canRead()
					&& templateFile.isFile()) {
				LOG.debug("Found and now using the upsell mail template at "+ templateFile.getPath());
				return getFileContents(templateFile);
			} else {
				LOG.error("Could not find an i18n upsell mail template with base path "+ fulltemplatepath);
				return null;
			}
		}
		
	}

	

	/**
	 * 
	 * Return the parsed URL to the UI to which it should redirect 
	 * 
	 * @param jsonObject
	 * @return
	 * @throws ServiceException
	 * @throws JSONException
	 */
	private Object actionGetStaticRedirectURL(JSONObject jsonObject,String request_src_hostname) throws ServiceException, JSONException {
		JSONObject jsonResponseObject = new JSONObject();
		
		// Default implementation to generate the redirect URL
		// this checks for configured url in file or configured url in admin user attributes
		String STATIC_URL_RAW = getRedirectURL();
				
		
		try {
			
			String url = parseText(STATIC_URL_RAW,jsonObject,true);
			
			
			// now check for custom implementations of the URL
            final UrlService urlservice = MyServiceRegistry.getServiceRegistry().getService(UrlService.class);
            UrlService provider = null;
            if (null != urlservice) {
            	if(LOG.isDebugEnabled()){
                	LOG.debug("Found URLGenerator service. Using it now to generate redirect Upsell URL instead of default.");
                }
                // We have a special service providing login information, so we use that one...
                try {
                	// pass the parameters to the external implementation
                    url = urlservice.generateUrl(getParameterMap(jsonObject),this.sessionObj,this.user,this.admin,this.ctx);
                    if(LOG.isDebugEnabled()){
                    	LOG.debug("Using custom redirect URL from URLGenerator service. URL: "+url);
                    }
                } catch (final UrlGeneratorException e) {
                	LOG.error("Fatal error occured, generating redirect URL from custom implementation failed!", e);
                }
            }

            jsonResponseObject.put("upsell_static_redirect_url",url); // parsed url with all parameter
		} catch (URIException e) {
			LOG.error("Error encoding static redirect URL", e);
		} catch (UnsupportedEncodingException e) {
			LOG.error("Error encoding static redirect URL", e);
		}		
		
		return jsonResponseObject;
	}
	
	/**
	 * If context has special login mapping "UPSELL_DIRECT_URL||<URL>" we use this URL instead of configured one.
	 * @return
	 * @throws ServiceException 
	 */
	private String getRedirectURL() throws ServiceException{
		
		String STATIC_URL_RAW = getFromConfig(PROPERTY_METHOD_STATIC_SHOP_REDIR_URL);
		int contextId = this.ctx.getContextId();
		
		if(LOG.isDebugEnabled()){
			LOG.debug("Admin user attributes for context "+contextId+" : "+this.admin.getAttributes().toString());
		}
		
		if(this.admin.getAttributes().containsKey("com.openexchange.upsell/url")){
			Set urlset = this.admin.getAttributes().get("com.openexchange.upsell/url");
			STATIC_URL_RAW = (String) urlset.iterator().next();
			STATIC_URL_RAW += "src=ox&user=_USER_&invite=_INVITE_&mail=_MAIL_&purchase_type=_PURCHASE_TYPE_&login=_LOGIN_&imaplogin=_IMAPLOGIN_&clicked_feat=_CLICKED_FEATURE_&upsell_plan=_UPSELL_PLAN_&cid=_CID_&lang=_LANG_";
			if(LOG.isDebugEnabled()){
				LOG.debug("Parsed UPSELL URL from context "+contextId+" and admin user attributes: "+STATIC_URL_RAW);
			}
		}else{
			if(LOG.isDebugEnabled()){
				LOG.debug("Parsed UPSELL URL from configuration for context: "+contextId);
			}
		}
		
		return STATIC_URL_RAW;
	}
	
	private String parseText(String raw_text, JSONObject json,boolean url_encode_it) throws JSONException, URIException, UnsupportedEncodingException{
		Map<String, String> bla = getParameterMap(json);
		Set a = bla.keySet();
		Iterator itr = a.iterator();
		
		// loop through all params and replace
		while (itr.hasNext()) {
			
			String map_key = (String) itr.next();
			String map_val = bla.get(map_key).toString();
			if(url_encode_it){
				map_val = URLEncoder.encode(map_val,"UTF-8");
			}
			// replace the placeholder with values
			raw_text = raw_text.replaceAll(map_key, map_val);
			
		}
		
		return raw_text;
	}
	
	private void sendUpsellEmail(String to, String from,String text,String subject) throws MyServletException{
		try {		
			InternetAddress fromAddress = new InternetAddress(from, true);

			final com.openexchange.mail.transport.TransportProvider provider = com.openexchange.mail.transport.TransportProviderRegistry.getTransportProviderBySession(this.sessionObj, 0);

			ComposedMailMessage msg = provider.getNewComposedMailMessage(this.sessionObj, this.ctx);
			msg.setSubject(subject);
			msg.addFrom(fromAddress);
			msg.addTo(new InternetAddress(to));
			
			
			final TextBodyMailPart textPart = provider.getNewTextBodyPart(text);
			msg.setBodyPart(textPart);			
			msg.setContentType("text/plain");

			final MailTransport transport = MailTransport.getInstance(this.sessionObj);
			try {
				transport.sendMailMessage(msg, com.openexchange.mail.dataobjects.compose.ComposeType.NEW, new Address[] { new InternetAddress(to) });
				LOG.info("Upsell request from user "+this.sessionObj.getLogin()+" (cid:"+this.ctx.getContextId()+")  was sent to "+to+"");
			} finally {
				transport.close();
			}
			
			
		} catch (MailException e) {
			LOG.error("Couldn't send provisioning mail", e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());
		} catch (AddressException e) {
			LOG.error("Target email address cannot be parsed",e);
			throw new MyServletException(MyServletException.Code.EMAIL_COMMUNICATION_ERROR,e.getMessage());

		}
		
	}
	
	/**
	 * Method for generating a map with all needed parameters
	 * 
	 * @param jsondata - Data from UI to fill feature which was clicked and what upsell plan user wants to buy
	 * @return
	 * @throws JSONException 
	 */
	private Map<String, String> getParameterMap(JSONObject jsondata) throws JSONException{
		
		Map<String, String> bla = new HashMap<String, String>();
		
		bla.put(UrlService.MAP_ATTR_USER,this.sessionObj.getUserlogin()); // users username 
		bla.put(UrlService.MAP_ATTR_PWD,this.sessionObj.getPassword()); // password
		bla.put(UrlService.MAP_ATTR_MAIL,this.user.getMail()); // users email addy
		bla.put(UrlService.MAP_ATTR_LOGIN,this.sessionObj.getLogin()); // users full login from UI mask
		bla.put(UrlService.MAP_ATTR_IMAP_LOGIN,this.user.getImapLogin()); // imap login 
		bla.put(UrlService.MAP_ATTR_CID,""+ctx.getContextId()); // context id
		bla.put(UrlService.MAP_ATTR_USERID,""+this.sessionObj.getUserId()); // user id 
		bla.put(UrlService.MAP_ATTR_LANGUAGE,""+this.user.getPreferredLanguage()); // user id 
		
		if(jsondata!=null && jsondata.has("purchase_type")){
			bla.put(UrlService.MAP_ATTR_PURCHASE_TYPE,jsondata.getString("purchase_type"));
		}
		
		if(jsondata!=null && jsondata.has("invite")){
			bla.put(UrlService.MAP_ATTR_INVITE,jsondata.getString("invite"));
		}
		
		if(jsondata!=null && jsondata.has("feature_clicked")){
			bla.put(UrlService.MAP_ATTR_CLICKED_FEATURE,jsondata.getString("feature_clicked")); // the feature the user clicked on like calender, infostore, mobility etc.

		}
		if(jsondata!=null && jsondata.has("upsell_plan")){
			bla.put(UrlService.MAP_ATTR_UPSELL_PLAN,jsondata.getString("upsell_plan")); //the upsell package the user wants to buy
		}
		
		
		return bla;
		
	}
	
	static public String getFileContents(File file) {
        StringBuilder stringBuilder = new StringBuilder();
        try {
            BufferedReader input = new BufferedReader(new FileReader(file));
            try {
                String line = null;
                while ((line = input.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append(System.getProperty("line.separator"));
                }
            } finally {
                input.close();
            }
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
        return stringBuilder.toString();
    }
	
	
	
	/**
	 * 
	 * Return configured method of upsell plugin to handle actions different in UI.
	 * 
	 * @param jsonObject
	 * @return
	 * @throws ServiceException
	 * @throws JSONException
	 */
	private Object actionGetUpsellMethod(JSONObject jsonObject) throws ServiceException, JSONException {
		JSONObject jsonResponseObject = new JSONObject();
		
		jsonResponseObject.put("upsell_method",getFromConfig(PROPERTY_METHOD)); // send method
		
		return jsonResponseObject;
	}

	
	private String getFromConfig(String key) throws ServiceException{		
		return this.configservice.getProperty(key); 
	}
	
//	private static final HttpClient HTTPCLIENT;
	//
//	    static {
//	            MultiThreadedHttpConnectionManager manager = new MultiThreadedHttpConnectionManager();
//	            HttpConnectionManagerParams params = manager.getParams();
//	            params.setMaxConnectionsPerHost(HostConfiguration.ANY_HOST_CONFIGURATION, 23);
//	            HTTPCLIENT = new HttpClient(manager);
//	    }
	

}
