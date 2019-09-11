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
 *    trademarks of the OX Software GmbH. group of companies.
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

package com.openexchange.mail.compose.impl.attachment;

import java.util.concurrent.atomic.AtomicReference;
import org.osgi.framework.BundleContext;
import com.openexchange.capabilities.CapabilityService;
import com.openexchange.capabilities.CapabilitySet;
import com.openexchange.exception.OXException;
import com.openexchange.mail.compose.AttachmentStorage;
import com.openexchange.mail.compose.AttachmentStorageService;
import com.openexchange.mail.compose.CompositionSpaceErrorCode;
import com.openexchange.mail.compose.CompositionSpaceStorageService;
import com.openexchange.mail.compose.impl.NonCryptoCompositionSpaceStorageService;
import com.openexchange.mail.compose.impl.attachment.security.CryptoAttachmentStorage;
import com.openexchange.mail.compose.security.CompositionSpaceKeyStorageService;
import com.openexchange.osgi.RankingAwareNearRegistryServiceTracker;
import com.openexchange.server.ServiceExceptionCode;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;


/**
 * {@link AttachmentStorageServiceImpl}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 * @since v7.10.2
 */
public class AttachmentStorageServiceImpl extends RankingAwareNearRegistryServiceTracker<AttachmentStorage> implements AttachmentStorageService {

    private final ServiceLookup services;
    private final CompositionSpaceKeyStorageService keyStorageService;
    private final AtomicReference<CompositionSpaceStorageService> compositionSpaceStorage;

    /**
     * Initializes a new {@link AttachmentStorageServiceImpl}.
     */
    public AttachmentStorageServiceImpl(CompositionSpaceKeyStorageService keyStorageService, ServiceLookup services, BundleContext context) {
        super(context, AttachmentStorage.class);
        this.services = services;
        this.keyStorageService = keyStorageService;
        compositionSpaceStorage = new AtomicReference<>();
    }

    /**
     * Sets the composition space storage instance.
     *
     * @param compositionSpaceStorage The composition space storage instance
     */
    public void setCompositionSpaceStorageService(CompositionSpaceStorageService compositionSpaceStorage) {
        if (!(compositionSpaceStorage instanceof NonCryptoCompositionSpaceStorageService)) {
            throw new IllegalArgumentException("Composition space storage instance must not be crypto-aware");
        }

        this.compositionSpaceStorage.set(compositionSpaceStorage);
    }

    @Override
    public AttachmentStorage getAttachmentStorageFor(Session session) throws OXException {
        // Obtain user's capabilities
        CapabilitySet capabilities = getCapabilitySet(session);

        // Find suitable attachment storage
        for (AttachmentStorage attachmentStorage : this) {
            if (attachmentStorage.isApplicableFor(capabilities, session)) {
                return new CryptoAttachmentStorage(attachmentStorage, compositionSpaceStorage.get(), keyStorageService, services);
            }
        }
        throw CompositionSpaceErrorCode.NO_ATTACHMENT_STORAGE.create();
    }

    private CapabilitySet getCapabilitySet(Session session) throws OXException {
        CapabilityService capabilityService = services.getOptionalService(CapabilityService.class);
        if (null == capabilityService) {
            throw ServiceExceptionCode.absentService(CapabilityService.class);
        }
        return capabilityService.getCapabilities(session);
    }

}
