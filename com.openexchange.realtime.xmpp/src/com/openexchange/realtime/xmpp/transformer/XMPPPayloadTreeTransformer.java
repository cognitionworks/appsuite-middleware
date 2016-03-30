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

package com.openexchange.realtime.xmpp.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.openexchange.exception.OXException;
import com.openexchange.realtime.payload.PayloadElement;
import com.openexchange.realtime.payload.PayloadTree;
import com.openexchange.realtime.payload.PayloadTreeNode;
import com.openexchange.realtime.util.ElementPath;
import com.openexchange.server.ServiceLookup;

/**
 * {@link XMPPPayloadTreeTransformer}
 * 
 * @author <a href="mailto:martin.herfurth@open-xchange.com">Martin Herfurth</a>
 */
public class XMPPPayloadTreeTransformer {

    public static AtomicReference<ServiceLookup> SERVICES = new AtomicReference<ServiceLookup>();

    public static Map<ElementPath, XMPPPayloadElementTransformer> transformers = new HashMap<ElementPath, XMPPPayloadElementTransformer>();

    public PayloadTree incoming(PayloadTree payloadTree) throws OXException {
        PayloadTreeNode root = payloadTree.getRoot();
        return new PayloadTree(incoming(root));
    }

    private PayloadTreeNode incoming(PayloadTreeNode root) throws OXException {
        PayloadElement converted = transformers.get(root.getElementPath()).incoming(root.getPayloadElement());
        PayloadElement transformedElement = new PayloadElement(converted);

        PayloadTreeNode retval = new PayloadTreeNode(transformedElement);

        if (root.hasChildren()) {
            for (PayloadTreeNode child : root.getChildren()) {
                retval.addChild(incoming(child));
            }
        }

        return retval;
    }

    public PayloadTree outgoing(PayloadTree payloadTree) throws OXException {
        PayloadTreeNode root = payloadTree.getRoot();
        return new PayloadTree(outgoing(root));
    }

    private PayloadTreeNode outgoing(PayloadTreeNode root) throws OXException {
        PayloadElement converted = transformers.get(root.getElementPath()).outgoing(root.getPayloadElement());
        PayloadElement transformedElement = new PayloadElement(converted);

        PayloadTreeNode retval = new PayloadTreeNode(transformedElement);

        if (root.hasChildren()) {
            for (PayloadTreeNode child : root.getChildren()) {
                retval.addChild(outgoing(child));
            }
        }

        return retval;
    }

}
