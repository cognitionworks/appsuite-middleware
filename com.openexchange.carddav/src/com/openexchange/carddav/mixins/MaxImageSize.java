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

package com.openexchange.carddav.mixins;


import static com.openexchange.java.Autoboxing.L;
import static com.openexchange.java.Autoboxing.l;
import com.openexchange.carddav.CarddavProtocol;
import com.openexchange.carddav.GroupwareCarddavFactory;
import com.openexchange.config.lean.DefaultProperty;
import com.openexchange.config.lean.LeanConfigurationService;
import com.openexchange.config.lean.Property;
import com.openexchange.exception.OXException;
import com.openexchange.webdav.protocol.helpers.SingleXMLPropertyMixin;

/**
 * {@link MaxImageSize}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class MaxImageSize extends SingleXMLPropertyMixin {

    private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(MaxImageSize.class);
    private static final Property MAX_IMAGE_SIZE_PROPERTY = DefaultProperty.valueOf("max_image_size", L(4194304));

	private final GroupwareCarddavFactory factory;

    /**
     * Initializes a new {@link MaxImageSize}.
     *
     * @param factory A reference to the CardDAV factory
     */
    public MaxImageSize(GroupwareCarddavFactory factory) {
        super(CarddavProtocol.CARD_NS.getURI(), "max-image-size");
        this.factory = factory;
    }

    @Override
    protected String getValue() {
        long maxSize;
        try {
            maxSize = factory.getServiceSafe(LeanConfigurationService.class).getLongProperty(MAX_IMAGE_SIZE_PROPERTY);
        } catch (OXException e) {
            maxSize = l(MAX_IMAGE_SIZE_PROPERTY.getDefaultValue(Long.class));
            LOG.warn("error reading value for \"{}\", falling back to {}.", MAX_IMAGE_SIZE_PROPERTY, L(maxSize), e);
        }
        return String.valueOf(maxSize);
    }

}
