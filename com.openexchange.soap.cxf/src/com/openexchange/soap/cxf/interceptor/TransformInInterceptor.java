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

package com.openexchange.soap.cxf.interceptor;

import java.io.InputStream;
import java.util.List;
import java.util.Map;
import javax.xml.stream.XMLStreamReader;
import org.apache.cxf.interceptor.StaxInInterceptor;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageUtils;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.staxutils.transform.TransformUtils;


/**
 * {@link TransformInInterceptor}
 *
 * @author <a href="mailto:thorben.betten@open-xchange.com">Thorben Betten</a>
 */
public class TransformInInterceptor extends AbstractPhaseInterceptor<Message> {

    private List<String> inDropElements;
    private Map<String, String> inElementsMap;
    private Map<String, String> inAppendMap;
    private boolean blockOriginalReader = true;
    private String contextPropertyName;

    public TransformInInterceptor() {
        this(Phase.POST_STREAM);
        addBefore(StaxInInterceptor.class.getName());
    }

    public TransformInInterceptor(String phase) {
        super(phase);
    }

    public TransformInInterceptor(String phase, List<String> after) {
        super(phase);
        if (after != null) {
            addAfter(after);
        }
    }

    public TransformInInterceptor(String phase, List<String> before, List<String> after) {
        this(phase, after);
        if (before != null) {
            addBefore(before);
        }
    }

    @Override
    public void handleMessage(Message message) {
        if (contextPropertyName != null
            && !MessageUtils.getContextualBoolean(message, contextPropertyName, false)) {
            return;
        }
        XMLStreamReader reader = message.getContent(XMLStreamReader.class);
        InputStream is = message.getContent(InputStream.class);

        XMLStreamReader transformReader = createTransformReaderIfNeeded(reader, is);
        if (transformReader != null) {
            message.setContent(XMLStreamReader.class, transformReader);
            message.removeContent(InputStream.class);
        }

    }

    protected XMLStreamReader createTransformReaderIfNeeded(XMLStreamReader reader, InputStream is) {
        return TransformUtils.createTransformReaderIfNeeded(reader, is,
                                                            inDropElements,
                                                            inElementsMap,
                                                            inAppendMap,
                                                            blockOriginalReader);
    }

    public void setInAppendElements(Map<String, String> inElements) {
        this.inAppendMap = inElements;
    }

    public void setInDropElements(List<String> dropElementsSet) {
        this.inDropElements = dropElementsSet;
    }

    public void setInTransformElements(Map<String, String> inElements) {
        this.inElementsMap = inElements;
    }

    public void setBlockOriginalReader(boolean blockOriginalReader) {
        this.blockOriginalReader = blockOriginalReader;
    }

    public void setContextPropertyName(String propertyName) {
        contextPropertyName = propertyName;
    }

}
