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

package com.openexchange.chronos.alarm.sms.osgi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.chronos.alarm.message.AlarmNotificationService;
import com.openexchange.chronos.alarm.sms.SMSNotificationService;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.i18n.TranslatorFactory;
import com.openexchange.osgi.HousekeepingActivator;
import com.openexchange.regional.RegionalSettingsService;
import com.openexchange.sms.SMSServiceSPI;
import com.openexchange.user.UserService;

/**
 * {@link SMSAlarmActivator}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.1
 */
public class SMSAlarmActivator extends HousekeepingActivator{

    private static final Logger LOG = LoggerFactory.getLogger(SMSAlarmActivator.class);

    @Override
    protected Class<?>[] getNeededServices() {
        return new Class[] { UserService.class, TranslatorFactory.class, LeanConfigurationService.class, RegionalSettingsService.class };
    }

    @Override
    protected Class<?>[] getOptionalServices() {
        return new Class[] { SMSServiceSPI.class };
    }

    @Override
    protected void startBundle() throws Exception {
        TranslatorFactory translatorFactory = getServiceSafe(TranslatorFactory.class);
        UserService userService = getServiceSafe(UserService.class);
        LeanConfigurationService leanConfigurationService = getServiceSafe(LeanConfigurationService.class);
        registerService(AlarmNotificationService.class, new SMSNotificationService(this, translatorFactory, userService, leanConfigurationService));
        LOG.info("Successfully started bundle "+this.context.getBundle().getSymbolicName());
    }

    @Override
    protected void stopBundle() throws Exception {
        super.stopBundle();
        LOG.info("Successfully stopped bundle "+this.context.getBundle().getSymbolicName());
    }

}
