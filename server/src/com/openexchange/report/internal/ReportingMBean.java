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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.report.internal;

import static com.openexchange.java.Autoboxing.I;
import static com.openexchange.java.Autoboxing.L;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.DynamicMBean;
import javax.management.InvalidAttributeValueException;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.ReflectionException;
import javax.management.RuntimeOperationsException;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenMBeanAttributeInfo;
import javax.management.openmbean.OpenMBeanAttributeInfoSupport;
import javax.management.openmbean.OpenMBeanInfoSupport;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import javax.management.openmbean.TabularDataSupport;
import javax.management.openmbean.TabularType;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.openexchange.context.ContextService;
import com.openexchange.database.DBPoolingException;
import com.openexchange.databaseold.Database;
import com.openexchange.groupware.calendar.Constants;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextException;
import com.openexchange.groupware.ldap.User;
import com.openexchange.groupware.ldap.UserException;
import com.openexchange.groupware.userconfiguration.UserConfiguration;
import com.openexchange.groupware.userconfiguration.UserConfigurationException;
import com.openexchange.server.ServiceException;
import com.openexchange.server.services.ServerServiceRegistry;
import com.openexchange.tools.sql.DBUtils;
import com.openexchange.user.UserService;
import com.openexchange.userconf.UserConfigurationService;

/**
 * {@link ReportingMBean}
 *
 * @author <a href="mailto:marcus.klein@open-xchange.com">Marcus Klein</a>
 */
public class ReportingMBean implements DynamicMBean {

    private static final Log LOG = LogFactory.getLog(ReportingMBean.class);

    private String[] totalNames = { "contexts", "users" };

    private CompositeType totalRow;

    private TabularType totalType;

    private String[] moduleAccessCombinationNames = { "module access combination", "users" };

    private String[] detailNames = { "identifier", "users", "age", "created", "mappings", "module access combinations" };

    private CompositeType detailRow;

    private TabularType moduleAccessCombinationsType;

    private TabularType detailType;

    private MBeanInfo mbeanInfo;

    private CompositeType moduleAccessPermission;

    /**
     * Initializes a new {@link ReportingMBean}.
     */
    public ReportingMBean() {
        super();
        mbeanInfo = buildMBeanInfo();
    }

    public Object getAttribute(String attribute) throws AttributeNotFoundException, MBeanException, ReflectionException {
        if (attribute == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("Attribute name cannot be null"),
                "Cannot call getAttributeInfo with null attribute name");
        }
        final ContextService contextService;
        final UserService userService;
        final UserConfigurationService configurationService;
        try {
            contextService = ServerServiceRegistry.getInstance().getService(ContextService.class, true);
            userService = ServerServiceRegistry.getInstance().getService(UserService.class, true);
            configurationService = ServerServiceRegistry.getInstance().getService(UserConfigurationService.class, true);
        } catch (ServiceException e) {
            LOG.error(e.getMessage(), e);
            throw new MBeanException(new Exception(e.getMessage()));
        }
        if ("Total".equals(attribute)) {
            TabularDataSupport total = new TabularDataSupport(totalType);
            try {
                List<Integer> allContextIds = contextService.getAllContextIds();
                int userCount = 0;
                for (Integer contextId : allContextIds) {
                    userCount += userService.listAllUser(contextService.getContext(contextId.intValue())).length;
                }
                CompositeDataSupport value = new CompositeDataSupport(totalRow, totalNames, new Object[] { I(allContextIds.size()), I(userCount) });
                total.put(value);
            } catch (OpenDataException e) {
                LOG.error(e.getMessage(), e);
            } catch (ContextException e) {
                LOG.error(e.getMessage(), e);
            } catch (UserException e) {
                LOG.error(e.getMessage(), e);
            }
            return total;
        } else
        if ("Detail".equals(attribute)) {
            return generateDetailTabular(contextService, userService, configurationService);
        }
        throw new AttributeNotFoundException("Cannot find " + attribute + " attribute ");
    }

    private TabularDataSupport generateDetailTabular(ContextService contextService, UserService userService, UserConfigurationService configService) {
        TabularDataSupport detail = new TabularDataSupport(detailType);
        try {
            List<Integer> allContextIds = contextService.getAllContextIds();
            for (Integer contextId : allContextIds) {
                Context context = contextService.getContext(contextId.intValue());
                UserConfiguration[] configs = getUserConfigurations(userService, configService, context);
                Date created = getContextCreated(context);
                StringBuilder sb = new StringBuilder();
                for (String loginInfo : context.getLoginInfo()) {
                    sb.append(loginInfo);
                    sb.append(',');
                }
                sb.setLength(sb.length() - 1);
                TabularDataSupport moduleAccessCombinations = new TabularDataSupport(moduleAccessCombinationsType);
                consolidateAccessCombinations(configs, moduleAccessCombinations);
                CompositeDataSupport value = new CompositeDataSupport(detailRow, detailNames, new Object[] {
                    contextId, I(configs.length), calcAge(created), created, sb.toString(), moduleAccessCombinations });
                detail.put(value);
            }
        } catch (ContextException e) {
            LOG.error(e.getMessage(), e);
        } catch (OpenDataException e) {
            LOG.error(e.getMessage(), e);
        } catch (UserException e) {
            LOG.error(e.getMessage(), e);
        } catch (UserConfigurationException e) {
            LOG.error(e.getMessage(), e);
        } catch (Throwable t) {
            LOG.error(t.getMessage(), t);
        }
        return detail;
    }

    private void consolidateAccessCombinations(UserConfiguration[] configs, TabularDataSupport moduleAccessCombinations) throws OpenDataException {
        Map<Integer, Integer> combinations = new HashMap<Integer, Integer>();
        for (UserConfiguration config : configs) {
            Integer accessCombination = I(config.getPermissionBits());
            Integer users = combinations.get(accessCombination);
            if (null == users) {
                users = I(1);
            } else {
                users = I(users.intValue() + 1);
            }
            combinations.put(accessCombination, users);
        }
        for (Map.Entry<Integer, Integer> entry : combinations.entrySet()) {
            moduleAccessCombinations.put(new CompositeDataSupport(moduleAccessPermission, moduleAccessCombinationNames, new Object[] {
                entry.getKey(), entry.getValue() }));
        }
    }

    private UserConfiguration[] getUserConfigurations(UserService userService, UserConfigurationService configurationService, Context ctx) throws UserException, UserConfigurationException {
        User[] users = userService.getUser(ctx);
        UserConfiguration[] configurations = configurationService.getUserConfiguration(ctx, users);
        return configurations;
    }

    private Long calcAge(Date created) {
        return L((System.currentTimeMillis() - created.getTime()) / Constants.MILLI_DAY);
    }

    private Date getContextCreated(Context ctx) {
        final Connection con;
        try {
            con = Database.get(ctx, false);
        } catch (DBPoolingException e) {
            LOG.error("Unable to get database connection.", e);
            return new Date(0);
        }
        PreparedStatement stmt = null;
        ResultSet result = null;
        try {
            stmt = con.prepareStatement("SELECT c.creating_date FROM prg_contacts c JOIN user_setting_admin a ON c.cid=a.cid AND c.userid=a.user WHERE a.cid=?");
            stmt.setInt(1, ctx.getContextId());
            result = stmt.executeQuery();
            if (result.next()) {
                return new Date(result.getLong(1));
            }
        } catch (SQLException e) {
            LOG.error("SQL problem.", e);
        } finally {
            DBUtils.closeSQLStuff(result, stmt);
            Database.back(ctx, false, con);
        }
        return new Date(0);
    }

    public AttributeList getAttributes(String[] attributes) {
        if (attributes == null) {
            throw new RuntimeOperationsException(
                new IllegalArgumentException("attributes can not be null"),
                "Cannot call getAttributes with null attribute names");
        }
        AttributeList resultList = new AttributeList();
        if (attributes.length == 0)
            return resultList;
        for (int i = 0; i < attributes.length; i++) {
            try {
                Object value = getAttribute(attributes[i]);
                resultList.add(new Attribute(attributes[i], value));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return (resultList);
    }

    public MBeanInfo getMBeanInfo() {
        return mbeanInfo;
    }

    public Object invoke(String actionName, Object[] params, String[] signature) throws MBeanException, ReflectionException {
        throw new RuntimeOperationsException(
            new UnsupportedOperationException("invoke is not supported"),
            "The method invoke is not supported.");
    }

    public void setAttribute(Attribute attribute) throws AttributeNotFoundException, InvalidAttributeValueException, MBeanException, ReflectionException {
        throw new AttributeNotFoundException("No attribute can be set in this MBean");
    }

    public AttributeList setAttributes(AttributeList attributes) {
        return new AttributeList();
    }

    private final MBeanInfo buildMBeanInfo() {
        try {
            String[] totalDescriptions = { "Number of contexts", "Number of users" };
            OpenType[] totalTypes = { SimpleType.INTEGER, SimpleType.INTEGER };
            totalRow = new CompositeType("Total row", "A total row", totalNames, totalDescriptions, totalTypes);
            totalType = new TabularType("Total", "Total view", totalRow, totalNames);

            String[] moduleAccessCombinationDescriptions = { "Integer value of the module access combination", "number of users configured with this module access combination" };
            OpenType[] moduleAccessCombinationTypes = { SimpleType.INTEGER, SimpleType.INTEGER };
            moduleAccessPermission = new CompositeType("Module access permission", "A module access combination and the number of users having it", moduleAccessCombinationNames, moduleAccessCombinationDescriptions, moduleAccessCombinationTypes);
            moduleAccessCombinationsType = new TabularType("Module access permission combinations", "The different access combinations used in this context", moduleAccessPermission, new String[] { "module access combination" });

            String[] detailDescriptions = { "Context identifier", "Number of users", "Context age in days", "Date and time of context creation", "Login mappings", "Module access permission combinations" };
            OpenType[] detailTypes = { SimpleType.INTEGER, SimpleType.INTEGER, SimpleType.LONG, SimpleType.DATE, SimpleType.STRING, moduleAccessCombinationsType };
            detailRow = new CompositeType("Detail row", "A detail row", detailNames, detailDescriptions, detailTypes);
            detailType = new TabularType("Detail", "Detail view", detailRow, new String[] { "identifier" });

            OpenMBeanAttributeInfo totalAttribute = new OpenMBeanAttributeInfoSupport("Total", "Total contexts and users.", totalType, true, false, false);
            OpenMBeanAttributeInfo detailAttribute = new OpenMBeanAttributeInfoSupport("Detail", "Detailed report about contexts and users", detailType, true, false, false);
            return new OpenMBeanInfoSupport(this.getClass().getName(), "Context and user reporting.", new OpenMBeanAttributeInfo[] { totalAttribute, detailAttribute }, null, null, null);
        } catch (OpenDataException e) {
            LOG.error(e.getMessage(), e);
        }
        return null;
    }
}
