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

package com.openexchange.ajax.importexport.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import com.openexchange.ajax.AJAXServlet;
import com.openexchange.ajax.importexport.actions.AbstractImportRequest.Action;
import com.openexchange.java.Strings;

/**
 *
 * @author <a href="mailto:marcus@open-xchange.org">Marcus Klein</a>
 */
public class ICalExportRequest extends AbstractExportRequest<ICalExportResponse> {

    private final int type;
    private final String body;

    public ICalExportRequest(final int folderId) {
        this(folderId, -1);
    }
    
    public ICalExportRequest(final int folderId, final int type) {
        super(Action.ICal, folderId);
        this.type = type;     
        this.body = "";
    }            
    
    public ICalExportRequest(final int folderId, final int type, String body) {
        super(Action.ICal, folderId);
        this.type = type;   
        this.body = body;
    }
    
    @Override
    public Parameter[] getParameters() {
        com.openexchange.ajax.framework.AJAXRequest.Parameter[] parameters = super.getParameters();
        if (type != -1) {
            parameters = parametersToAdd(new Parameter(AJAXServlet.PARAMETER_TYPE, this.type), parameters);
        }
        if (this.getFolderId() < 0) {
            parameters = parametersToRemove(AJAXServlet.PARAMETER_FOLDERID, parameters);
        }
        if (Strings.isNotEmpty(body)) {
            parameters = parametersToAdd(new Parameter("body", body), parameters);
        }
        return parameters;
    }

    @Override
    public ICalExportParser getParser() {
        return new ICalExportParser(true);
    }
    
    private com.openexchange.ajax.framework.AJAXRequest.Parameter[] parametersToAdd(Parameter parameter, Parameter[] parameters) {
        Parameter[] newParameters = new Parameter[parameters.length + 1];
        System.arraycopy(parameters, 0, newParameters, 0, parameters.length);
        newParameters[newParameters.length - 1] = parameter;
        return newParameters;
    }
    
    private com.openexchange.ajax.framework.AJAXRequest.Parameter[] parametersToRemove(String parameter, Parameter[] parameters) {
        List<Parameter> list = Arrays.asList(parameters);
        List<Parameter> newList = new ArrayList<Parameter>();
        for(Parameter param : list){
            if (!param.getName().equals(parameter)){
                newList.add(param);
            }
        }
        return newList.toArray(new Parameter[newList.size()]);
    }
}
