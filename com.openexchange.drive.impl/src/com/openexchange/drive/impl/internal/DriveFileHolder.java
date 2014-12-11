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
 *     Copyright (C) 2004-2013 Open-Xchange, Inc.
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

package com.openexchange.drive.impl.internal;

import java.io.IOException;
import java.io.InputStream;
import com.openexchange.ajax.fileholder.IFileHolder;
import com.openexchange.drive.impl.internal.throttle.BucketInputStream;
import com.openexchange.exception.OXException;
import com.openexchange.java.Streams;

/**
 * {@link DriveFileHolder}
 *
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public class DriveFileHolder implements IFileHolder {

    private final InputStream stream;
    private final String contentType;
    private final String name;

    /**
     * Initializes a new {@link DriveFileHolder}.
     *
     * @param session The sync session
     * @param stream The underlying stream
     * @param name The filename
     * @param contentType The content-type, or <code>null</code> if unknown
     */
    public DriveFileHolder(SyncSession session, InputStream stream, String name, String contentType) {
        this(session, stream, name, contentType, true);
    }

    public DriveFileHolder(SyncSession session, InputStream stream, String name, String contentType, boolean throttled) {
        super();
        this.contentType = null != contentType ? contentType : "application/octet-stream";
        this.name = name;
        if (throttled) {
            this.stream = new BucketInputStream(stream, session.getServerSession());
        } else {
            this.stream = stream;
        }
    }

    @Override
    public boolean repetitive() {
        return false;
    }

    @Override
    public void close() throws IOException {
        Streams.close(stream);
    }

    @Override
    public InputStream getStream() throws OXException {
        return stream;
    }

    @Override
    public long getLength() {
        return -1;
    }

    @Override
    public String getContentType() {
        return contentType;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDisposition() {
        return null;
    }

    @Override
    public String getDelivery() {
        return "download";
    }

}
