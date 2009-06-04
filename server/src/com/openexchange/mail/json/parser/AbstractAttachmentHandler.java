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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.mail.json.parser;

import java.util.ArrayList;
import java.util.List;
import com.openexchange.configuration.ConfigurationException;
import com.openexchange.configuration.ServerConfig;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.mail.MailException;
import com.openexchange.mail.dataobjects.MailPart;
import com.openexchange.mail.usersetting.UserSettingMail;
import com.openexchange.mail.usersetting.UserSettingMailStorage;
import com.openexchange.session.Session;
import com.openexchange.tools.session.ServerSession;

/**
 * {@link AbstractAttachmentHandler} - An abstract {@link IAttachmentHandler attachment handler}.
 * 
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public abstract class AbstractAttachmentHandler implements IAttachmentHandler {

    private static final org.apache.commons.logging.Log LOG = org.apache.commons.logging.LogFactory.getLog(AbstractAttachmentHandler.class);

    protected final List<MailPart> attachments;

    protected final boolean doAction;

    protected final long uploadQuota;

    protected final long uploadQuotaPerFile;

    /**
     * Initializes a new {@link AbstractAttachmentHandler}.
     * 
     * @param session The session providing needed user information
     * @throws MailException If initialization fails
     */
    public AbstractAttachmentHandler(final Session session) throws MailException {
        super();
        attachments = new ArrayList<MailPart>(4);
        try {
            final UserSettingMail usm;
            if (session instanceof ServerSession) {
                usm = ((ServerSession) session).getUserSettingMail();
            } else {
                usm = UserSettingMailStorage.getInstance().getUserSettingMail(session.getUserId(), session.getContextId());
            }
            if (usm.getUploadQuota() >= 0) {
                this.uploadQuota = usm.getUploadQuota();
            } else {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Upload quota is less than zero. Using global server property \"MAX_UPLOAD_SIZE\" instead.");
                }
                long tmp;
                try {
                    tmp = ServerConfig.getInteger(ServerConfig.Property.MAX_UPLOAD_SIZE);
                } catch (final ConfigurationException e) {
                    LOG.error(e.getMessage(), e);
                    tmp = 0;
                }
                this.uploadQuota = tmp;
            }
            this.uploadQuotaPerFile = usm.getUploadQuotaPerFile();
            doAction = ((uploadQuotaPerFile > 0) || (uploadQuota > 0));
        } catch (final UserConfigurationException e) {
            throw new MailException(e);
        }
    }
}
