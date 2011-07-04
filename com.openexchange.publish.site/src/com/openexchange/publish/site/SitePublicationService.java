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
 *     Copyright (C) 2004-2011 Open-Xchange, Inc.
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

package com.openexchange.publish.site;

import static com.openexchange.publish.site.FormStrings.FORM_LABEL_DISPLAY_NAME;
import static com.openexchange.publish.site.FormStrings.FORM_LABEL_LINK;
import static com.openexchange.publish.site.FormStrings.TARGET_NAME_INFOSTORE;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import com.openexchange.datatypes.genericonf.DynamicFormDescription;
import com.openexchange.datatypes.genericonf.FormElement;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.publish.Publication;
import com.openexchange.publish.PublicationException;
import com.openexchange.publish.PublicationService;
import com.openexchange.publish.PublicationTarget;
import com.openexchange.publish.helpers.AbstractPublicationService;
import com.openexchange.publish.helpers.SecurityStrategy;

/**
 * {@link SitePublicationService}
 * 
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class SitePublicationService extends AbstractPublicationService implements PublicationService {

    public static final String DISPLAY_NAME = "displayName";

    public static final String URL = "url";

    public static final String SECRET = "secret";

    public static final String ID = "com.openexchange.publish.site";

    public static final String INFOSTORE = "infostore";

    private PublicationTarget target;

    private Random random = new Random();

    public SitePublicationService() {
        super();
        target = new PublicationTarget();
        DynamicFormDescription form = new DynamicFormDescription();

        form.add(FormElement.input(DISPLAY_NAME, FORM_LABEL_DISPLAY_NAME, true, null));
        form.add(FormElement.link(URL, FORM_LABEL_LINK, false, null));

        target.setDisplayName(TARGET_NAME_INFOSTORE);
        target.setId(ID);
        target.setModule(INFOSTORE);
        target.setFormDescription(form);
        target.setPublicationService(this);
    }

    @Override
    protected SecurityStrategy getSecurityStrategy() {
        return FOLDER_ADMIN_ONLY;
    }

    @Override
    public PublicationTarget getTarget() throws PublicationException {
        return target;
    }

    private static SitePublicationService INSTANCE = null;

    public static SitePublicationService getInstance() {
        return INSTANCE != null ? INSTANCE : (INSTANCE = new SitePublicationService());
    }

    public Publication getPublication(Context ctx, String secret) throws PublicationException {
        Map<String, Object> query = new HashMap<String, Object>();
        query.put(SECRET, secret);

        Collection<Publication> result = getStorage().search(ctx, getTarget().getId(), query);
        if (result.isEmpty()) {
            return null;
        }

        return result.iterator().next();
    }

    @Override
    public void beforeCreate(Publication publication) throws PublicationException {
        super.beforeCreate(publication);
        publication.getConfiguration().remove(URL);
        addSecret(publication, null);
    }

    @Override
    public void beforeUpdate(Publication publication) throws PublicationException {
        super.beforeUpdate(publication);
        Publication oldPublication = loadInternally(publication.getContext(), publication.getId());
        addSecret(publication, oldPublication);
        removeSecret(publication);
    }

    @Override
    public void modifyOutgoing(Publication publication) throws PublicationException {
        super.modifyOutgoing(publication);

        Map<String, Object> configuration = publication.getConfiguration();

        StringBuilder urlBuilder = new StringBuilder(Constants.PUBLICATION_ROOT_URL);
        String secret = (String) configuration.get(SECRET);
        urlBuilder.append('/').append(publication.getContext().getContextId()).append('/').append(secret).append('/');


        publication.getConfiguration().put(URL, urlBuilder.toString());

        publication.getConfiguration().remove(SECRET);

        publication.setDisplayName((String) publication.getConfiguration().get(DISPLAY_NAME));
    }

    private void removeSecret(Publication publication) {
        publication.getConfiguration().put(SECRET, null);
    }

    private void addSecret(Publication publication, Publication oldPublication) {

        String secret = null;
        if (oldPublication != null) {
            secret = (String) oldPublication.getConfiguration().get(SECRET);
        }
        if (secret == null) {
            long l1 = random.nextLong();
            long l2 = random.nextLong();

            secret = Long.toHexString(l1) + Long.toHexString(l2);
        }

        publication.getConfiguration().put(SECRET, secret);

    }

}
