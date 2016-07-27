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

package com.openexchange.reseller.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.DatabaseService;
import com.openexchange.exception.OXException;
import com.openexchange.reseller.ResellerService;
import com.openexchange.reseller.data.ResellerAdmin;
import com.openexchange.reseller.data.Restriction;
import com.openexchange.reseller.osgi.Services;

/**
 * {@link ResellerServiceImpl}
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.8.3
 */
public class ResellerServiceImpl implements ResellerService {

    private static final String DATABASE_COLUMN_VALUE = "value";

    private static final String DATABASE_COLUMN_NAME = "name";

    private static final String DATABASE_COLUMN_ID = "rid";

    private static final Logger log = LoggerFactory.getLogger(ResellerServiceImpl.class);
    private DatabaseService dbService;
    private static ResellerServiceImpl instance = null;

    public static ResellerServiceImpl getInstance() {
        if (instance == null) {
            instance = new ResellerServiceImpl();
        }
        return instance;
    }

    private ResellerServiceImpl() {
        dbService = Services.getService(DatabaseService.class);
    }

    @Override
    public ResellerAdmin getReseller(int cid) throws OXException {
        Connection con = dbService.getReadOnly();
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            prep = con.prepareStatement("SELECT sid FROM context2subadmin WHERE cid=?");
            prep.setInt(1, cid);
            rs = prep.executeQuery();
            if (!rs.next()) {
                throw ResellerExceptionCodes.NO_RESELLER_FOUND_FOR_CTX.create(cid);
            }
            return getData(new ResellerAdmin[] { new ResellerAdmin(rs.getInt(1)) })[0];
        } catch (final SQLException e) {
            log.error("", e);
            throw new OXException(e);
        } finally {
            dbService.backReadOnly(con);
            try {
                if (rs != null) {
                    rs.close();
                }
                if (prep != null) {
                    prep.close();
                }
            } catch (SQLException e) {
                //ignore
            }
        }
    }

    private ResellerAdmin[] getData(final ResellerAdmin[] admins) throws SQLException, OXException {
        Connection con = null;
        PreparedStatement prep = null;
        ResultSet rs = null;
        try {
            final ArrayList<ResellerAdmin> ret = new ArrayList<>();
            con = dbService.getReadOnly();
            for (final ResellerAdmin adm : admins) {
                ResellerAdmin newadm = new ResellerAdmin(adm.getId(), adm.getName());
                String query = "SELECT * FROM subadmin WHERE sid=?";
                prep = con.prepareStatement(query);
                prep.setInt(1, adm.getId());
                rs = prep.executeQuery();
                if (!rs.next()) {
                    throw ResellerExceptionCodes.NO_RESELLER_FOUND.create(adm.getId());
                }
                newadm.setName(rs.getString(DATABASE_COLUMN_NAME));
                newadm.setId(rs.getInt("sid"));
                newadm.setParentId(rs.getInt("pid"));
                newadm.setDisplayname(rs.getString("displayName"));
                newadm.setPassword(rs.getString("password"));
                newadm.setPasswordMech(rs.getString("passwordMech"));

                rs.close();
                prep.close();

                newadm = getRestrictionDataForAdmin(newadm, con);

                ret.add(newadm);
            }
            return ret.toArray(new ResellerAdmin[ret.size()]);
        } finally {
            dbService.backReadOnly(con);
            if (rs != null) {
                rs.close();
            }
            if (prep != null) {
                prep.close();
            }
        }
    }

    private ResellerAdmin getRestrictionDataForAdmin(final ResellerAdmin admin, final Connection con) throws SQLException {
        final PreparedStatement prep = con.prepareStatement("SELECT subadmin_restrictions.rid,sid,name,value FROM subadmin_restrictions INNER JOIN restrictions ON subadmin_restrictions.rid=restrictions.rid WHERE sid=?");
        if (admin.getParentId() > 0) {
            prep.setInt(1, admin.getParentId());
        } else {
            prep.setInt(1, admin.getId());
        }
        final ResultSet rs = prep.executeQuery();

        final HashSet<Restriction> res = new HashSet<>();
        while (rs.next()) {
            final Restriction r = new Restriction();
            r.setId(rs.getInt(DATABASE_COLUMN_ID));
            r.setName(rs.getString(DATABASE_COLUMN_NAME));
            r.setValue(rs.getString(DATABASE_COLUMN_VALUE));
            if (admin.getParentId() > 0 && r.getName().equals(Restriction.SUBADMIN_CAN_CREATE_SUBADMINS)) {
                continue;
            }
            res.add(r);
        }
        if (res.size() > 0) {
            admin.setRestrictions(res.toArray(new Restriction[res.size()]));
        }
        rs.close();
        prep.close();
        return admin;
    }

}
