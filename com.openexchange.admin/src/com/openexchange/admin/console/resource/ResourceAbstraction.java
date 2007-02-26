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
package com.openexchange.admin.console.resource;

import org.apache.commons.cli.Option;

import com.openexchange.admin.console.BasicCommandlineOptions;

public abstract class ResourceAbstraction extends BasicCommandlineOptions {
    
    protected static final String _OPT_NAME_LONG = "name";
    protected static final String _OPT_NAME_SHORT = "n";
    protected static final String _OPT_DISPNAME_LONG = "displayname";
    protected static final String _OPT_DISPNAME_SHORT = "d";
    protected static final String _OPT_DESCRIPTION_LONG = "description";
    protected static final String _OPT_DESCRIPTION_SHORT = "D";
    protected static final String _OPT_AVAILABLE_SHORT = "a";
    protected static final String _OPT_AVAILABLE_LONG = "available";
    protected static final String _OPT_EMAIL_SHORT = "e";
    protected static final String _OPT_EMAIL_LONG = "email";
    protected static final String _OPT_RESOURCEID_SHORT = "i";
    protected static final String _OPT_RESOURCEID_LONG = "resourceid";
    
    
    protected Option getDisplayNameOption(){
        return getShortLongOpt( _OPT_DISPNAME_SHORT,_OPT_DISPNAME_LONG,"The resource display name",true, true);        
    }
    
    protected Option getNameOption(){
        return getShortLongOpt( _OPT_NAME_SHORT,_OPT_NAME_LONG,"The resource name",true, true); 
    }
    
    protected Option getAvailableOption(){
        return getShortLongOpt( _OPT_AVAILABLE_SHORT,_OPT_AVAILABLE_LONG,"Toggle resource availability",false, false);                
    }
    
    protected Option getDescriptionOption(){
        return  getShortLongOpt(_OPT_DESCRIPTION_SHORT,_OPT_DESCRIPTION_LONG,"Description of this resource", true, false);        
    }
    
    protected Option getEmailOption(){
        return getShortLongOpt(_OPT_EMAIL_SHORT,_OPT_EMAIL_LONG,"Email of this resource", true, true); 
    }
    
    protected Option getIdOption(){
        return getShortLongOpt(_OPT_RESOURCEID_SHORT,_OPT_RESOURCEID_LONG,"Id of this resource", true, true); 
    }

}
