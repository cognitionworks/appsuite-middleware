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

package com.openexchange.consistency;

import static com.openexchange.java.Autoboxing.I;
import org.apache.commons.collections.keyvalue.MultiKey;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.user.User;

/**
 * {@link EntityImpl}
 *
 * @author <a href="mailto:ioannis.chouklis@open-xchange.com">Ioannis Chouklis</a>
 */
public class EntityImpl implements Entity {

    private static final long serialVersionUID = -4838502301287665200L;

    private final EntityType type;
    private final MultiKey identifier;
    private final Context context;
    private final User user;

    /**
     * Initializes a new {@link EntityImpl} of type {@link EntityType#Context context}.
     *
     * @param context The context
     */
    public EntityImpl(Context context) {
        this(new MultiKey(new Object[] { I(context.getContextId()) }), context, null, EntityType.Context);
    }

    /**
     * Initializes a new {@link EntityImpl} of type {@link EntityType#User user}.
     *
     * @param context The context
     * @param user The user
     */
    public EntityImpl(Context context, User user) {
        this(new MultiKey(I(context.getContextId()), I(user.getId())), context, user, EntityType.User);
    }

    /**
     * Initialises a new {@link EntityImpl}.
     */
    private EntityImpl(MultiKey identifier, Context context, User user, EntityType type) {
        super();
        this.identifier = identifier;
        this.type = type;
        this.context = context;
        this.user = user;
    }

    @Override
    public MultiKey getId() {
        return identifier;
    }

    @Override
    public EntityType getType() {
        return type;
    }

    @Override
    public Context getContext() {
        return context;
    }

    @Override
    public User getUser() {
        return user;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append("Entity [type=").append(type).append(", identifier=").append(identifier).append("]");
        return builder.toString();
    }

}
