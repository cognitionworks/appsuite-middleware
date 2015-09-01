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
 *     Copyright (C) 2004-2015 Open-Xchange, Inc.
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

package com.openexchange.oauth.provider.impl.notification;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import javax.activation.DataHandler;
import javax.mail.MessagingException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.servlet.http.HttpServletRequest;
import org.apache.http.client.utils.URIBuilder;
import com.openexchange.config.ConfigurationService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.html.HtmlService;
import com.openexchange.i18n.Translator;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.mail.dataobjects.compose.ComposeType;
import com.openexchange.mail.dataobjects.compose.ComposedMailMessage;
import com.openexchange.mail.dataobjects.compose.ContentAwareComposedMailMessage;
import com.openexchange.mail.mime.ContentType;
import com.openexchange.mail.mime.MessageHeaders;
import com.openexchange.mail.mime.MimeDefaultSession;
import com.openexchange.mail.mime.datasource.MessageDataSource;
import com.openexchange.mail.transport.MailTransport;
import com.openexchange.mail.transport.TransportProvider;
import com.openexchange.mail.transport.TransportProviderRegistry;
import com.openexchange.notification.BasicNotificationTemplate;
import com.openexchange.notification.BasicNotificationTemplate.FooterImage;
import com.openexchange.notification.FullNameBuilder;
import com.openexchange.oauth.provider.client.Client;
import com.openexchange.oauth.provider.exceptions.OAuthProviderExceptionCodes;
import com.openexchange.oauth.provider.impl.osgi.Services;
import com.openexchange.oauth.provider.impl.tools.URLHelper;
import com.openexchange.serverconfig.NotificationMailConfig;
import com.openexchange.serverconfig.ServerConfig;
import com.openexchange.serverconfig.ServerConfigService;
import com.openexchange.session.Session;
import com.openexchange.templating.OXTemplate;
import com.openexchange.templating.TemplateService;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link OAuthMailNotificationService}
 *
 * @author <a href="mailto:jan.bauerdick@open-xchange.com">Jan Bauerdick</a>
 * @since v7.8.0
 */
public class OAuthMailNotificationService {

    private final TransportProvider transportProvider;

    public OAuthMailNotificationService() {
        super();
        transportProvider = TransportProviderRegistry.getTransportProvider("smtp");
    }

    public void sendNotification(ServerSession serverSession, Client client, HttpServletRequest request) throws OXException {
        try {
            ComposedMailMessage mail = buildNewExternalApplicationMail(serverSession, client, request);
            MailTransport transport = transportProvider.createNewNoReplyTransport(serverSession.getContextId());
            transport.sendMailMessage(mail, ComposeType.NEW, mail.getTo());
        } catch (UnsupportedEncodingException | MessagingException | URISyntaxException e) {
            throw OAuthProviderExceptionCodes.UNEXPECTED_ERROR.create(e);
        }
    }

    private ComposedMailMessage buildNewExternalApplicationMail(ServerSession session, Client client, HttpServletRequest request) throws OXException, UnsupportedEncodingException, MessagingException, URISyntaxException {
        User user = session.getUser();
        TemplateService templateService = Services.requireService(TemplateService.class);
        Translator translator = Services.requireService(TranslatorFactory.class).translatorFor(user.getLocale());
        ServerConfigService serverConfigService = Services.requireService(ServerConfigService.class);
        String hostname = URLHelper.getHostname(request);
        int contextId = session.getContextId();
        ServerConfig serverConfig = serverConfigService.getServerConfig(hostname, user.getId(), contextId);
        String subject = translator.translate(NotificationStrings.SUBJECT);
        subject = String.format(subject, client.getName());
        String salutation = translator.translate(NotificationStrings.SALUTATION);
        String userName = FullNameBuilder.buildFullName(user, translator);
        salutation = String.format(salutation, userName);
        String appConnected = translator.translate(NotificationStrings.APP_CONNECTED);
        appConnected = String.format(appConnected, getLogin(session, user), client.getName());

        // text substitutions
        Map<String, Object> vars = new HashMap<String, Object>();
        vars.put("salutation", salutation);
        vars.put("appConnected", appConnected);
        vars.put("revokeAccess", translator.translate(NotificationStrings.REVOKE_ACCESS));
        vars.put("gotoSettings", translator.translate(NotificationStrings.GO_TO_SETTINGS));
        vars.put("settingsURL", getSettingsUrl(request));

        // style substitutions
        NotificationMailConfig mailConfig = serverConfig.getNotificationMailConfig();
        BasicNotificationTemplate basicTemplate = BasicNotificationTemplate.newInstance(mailConfig);
        basicTemplate.applyStyle(vars);
        FooterImage footerImage = basicTemplate.applyFooter(vars);

        OXTemplate template = templateService.loadTemplate("notify.oauthprovider.accessgranted.html.tmpl");
        StringWriter writer = new StringWriter();
        template.process(vars, writer);

        ComposedMailMessage mail;
        if (footerImage == null) {
            // no image, no multipart
            mail = transportProvider.getNewComposedMailMessage(session, session.getContext());
            mail.setSubject(subject);
            mail.setHeader("Auto-Submitted", "auto-generated");
            mail.setBodyPart(transportProvider.getNewTextBodyPart(writer.toString()));
        } else {
            MimeBodyPart htmlPart = new MimeBodyPart();
            ContentType ct = new ContentType();
            ct.setPrimaryType("text");
            ct.setSubType("html");
            ct.setCharsetParameter("UTF-8");
            String contentType = ct.toString();
            HtmlService htmlService = Services.requireService(HtmlService.class);
            String conformContent = htmlService.getConformHTML(writer.toString(), "UTF-8");
            htmlPart.setDataHandler(new DataHandler(new MessageDataSource(conformContent, ct)));
            htmlPart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, contentType);

            MimeBodyPart imagePart = new MimeBodyPart();
            imagePart.setDisposition("inline; filename=\"" + footerImage.getFileName() + "\"");
            imagePart.setHeader(MessageHeaders.HDR_CONTENT_TYPE, footerImage.getContentType() + "; name=\"" + footerImage.getFileName() + "\"");
            imagePart.setContentID("<" + footerImage.getContentId() + ">");
            imagePart.setHeader("X-Attachment-Id", footerImage.getContentId());
            imagePart.setDataHandler(new DataHandler(new MessageDataSource(footerImage.getData(), footerImage.getContentType())));

            MimeMultipart multipart = new MimeMultipart("related");
            multipart.addBodyPart(htmlPart);
            multipart.addBodyPart(imagePart);

            MimeMessage mimeMessage = new MimeMessage(MimeDefaultSession.getDefaultSession());
            mimeMessage.setSubject(subject, "UTF-8");
            mimeMessage.setHeader("Auto-Submitted", "auto-generated");
            mimeMessage.setContent(multipart);
            mimeMessage.saveChanges();
            mail = new ContentAwareComposedMailMessage(mimeMessage, contextId);
        }

        InternetAddress recipient = new InternetAddress(user.getMail(), userName);
        mail.addRecipient(recipient);
        mail.addTo(recipient);
        return mail;
    }

    private static String getLogin(Session session, User user) {
        String login = session.getLogin();
        if (login == null) {
            login = session.getLoginName();
        }

        if (login == null) {
            login = user.getLoginInfo();
        }

        if (login == null) {
            login = user.getMail();
        }

        if (login == null) {
            login = "";
        }

        return login;
    }

    private String getSettingsUrl(HttpServletRequest request) throws OXException, URISyntaxException {
        String uiWebPath = Services.requireService(ConfigurationService.class).getProperty("com.openexchange.UIWebPath", "/appsuite");
        URI settingsURI = new URIBuilder()
            .setScheme("https")
            .setHost(URLHelper.getHostname(request))
            .setPath(uiWebPath)
            .setFragment("!&app=io.ox/settings&folder=virtual/settings/external/apps")
            .build();
        return settingsURI.toString();
    }

}
