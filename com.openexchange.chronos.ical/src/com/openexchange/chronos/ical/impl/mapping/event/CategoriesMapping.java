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
 *     Copyright (C) 2004-2020 Open-Xchange, Inc.
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

package com.openexchange.chronos.ical.impl.mapping.event;

import java.util.ArrayList;
import java.util.List;
import biweekly.component.VEvent;
import biweekly.property.Categories;
import com.openexchange.chronos.Event;
import com.openexchange.chronos.ical.ICalParameters;
import com.openexchange.chronos.ical.impl.mapping.AbstractICalMapping;
import com.openexchange.exception.OXException;

/**
 * {@link CategoriesMapping}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 * @since v7.10.0
 */
public class CategoriesMapping extends AbstractICalMapping<VEvent, Event> {

	@Override
	public void export(Event event, VEvent vEvent, ICalParameters parameters, List<OXException> warnings) {
		List<String> categories = event.getCategories();
		if (null == categories || 0 == categories.size()) {
			vEvent.removeProperties(biweekly.property.Categories.class);
		} else {
			vEvent.removeProperties(biweekly.property.Categories.class); // TODO: better merge?
			vEvent.setProperty(new Categories(categories));
		}
	}

	@Override
	public void importICal(VEvent vEvent, Event event, ICalParameters parameters, List<OXException> warnings) {
		List<Categories> properties = vEvent.getCategories();
		if (null == properties || 0 == properties.size()) {
			event.setCategories(null);
		} else {
			List<String> categories = new ArrayList<String>(properties.size());
			for (biweekly.property.Categories property : properties) {
			    if (null != property.getValues()) {
			        categories.addAll(property.getValues());
			    }
			}
			event.setCategories(categories);
		}
	}

}
