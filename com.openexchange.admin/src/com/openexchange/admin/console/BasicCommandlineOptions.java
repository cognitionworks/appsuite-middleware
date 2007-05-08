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
package com.openexchange.admin.console;

import java.rmi.NotBoundException;
import java.util.ArrayList;

import com.openexchange.admin.console.CmdLineParser.Option;


/**
 * 
 * @author cutmasta,d7
 *
 */
public abstract class BasicCommandlineOptions {
    /**
     * Used when username/password credentials were not correct!
     */
    public static final int SYSEXIT_INVALID_CREDENTIALS=101;
    /**
     * Used when the requested context does not exists on the server!
     */
    public static final int SYSEXIT_NO_SUCH_CONTEXT=102;
    /**
     * Used when wrong data was sent to the server!
     */
    public static final int SYSEXIT_INVALID_DATA=103;
    /**
     * Used when an option is missing to execute the cmd tool!
     */
    public static final int SYSEXIT_MISSING_OPTION=104;
    
    
  
    /**
     * Used when a communication problem was encountered
     */
    public static final int SYSEXIT_COMMUNICATION_ERROR =105;
    
    /**
     * Used when a storage problem was encountered on the server!
     */
    public static final int SYSEXIT_SERVERSTORAGE_ERROR =106;
    
    /**
    * Used when a remote server problem was encountered !
    */
    public static final int SYSEXIT_REMOTE_ERROR =107;
   
   /**
    * Used when an user does not exists
    */
    public static final int SYSEXIT_NO_SUCH_USER =108;
   
   /**
    * Used when an unknown option was passed to the cmd tool!
    */
    public static final int SYSEXIT_ILLEGAL_OPTION_VALUE=109;
   
   /**
    * Used when a context aklready exists
    */
    public static final int SYSEXIT_CONTEXT_ALREADY_EXISTS=110;
   
   /**
    * Used when an unknown option was passed to the cmd tool!
    */
    public static final int SYSEXIT_UNKNOWN_OPTION=111;
   
    
    protected static final int DEFAULT_CONTEXT=1;
    protected static final char OPT_NAME_CONTEXT_SHORT='c';
    protected static final String OPT_NAME_CONTEXT_LONG="contextid";
    protected static final char OPT_NAME_CONTEXT_NAME_SHORT='N';
    protected static final String OPT_NAME_CONTEXT_NAME_LONG="contextname";
    protected static final String OPT_NAME_CONTEXT_NAME_DESCRIPTION="The name of the context";
    protected static final String OPT_NAME_CONTEXT_DESCRIPTION="The id of the context";
    protected static final char OPT_NAME_ADMINUSER_SHORT='A';
    protected static final String OPT_NAME_ADMINUSER_LONG="adminuser";
    protected static final char OPT_NAME_ADMINPASS_SHORT='P';
    protected static final String OPT_NAME_ADMINPASS_LONG="adminpass";
    protected static final String OPT_NAME_ADMINPASS_DESCRIPTION="Admin password";
    protected static final String OPT_NAME_ADMINUSER_DESCRIPTION="Admin username";
    protected static final String OPT_NAME_SEARCHPATTERN_LONG = "searchpattern";
    protected static final char OPT_NAME_SEARCHPATTERN = 's';
    
    protected static final String OPT_NAME_CSVOUTPUT_LONG = "csv";
    protected static final String OPT_NAME_CSVOUTPUT_DESCRIPTION = "Format output to csv";
    
    protected static String RMI_HOSTNAME ="rmi://localhost";
    
    protected Option contextOption = null;
    protected Option adminUserOption = null;
    protected Option adminPassOption = null;
    protected Option searchOption = null;
    protected Option csvOutputOption = null;
    
    protected final static void printServerResponse(final String msg){
        System.err.println("Server response:\n "+msg);    
    }
    
    protected final static void printNotBoundResponse(final NotBoundException nbe){
        printServerResponse("RMI module "+nbe.getMessage()+" not available on server");
    }
    
    protected final static void printError(final String msg){
        System.err.println("Error:\n "+msg+"\n");    
    }
    
    protected final static void printInvalidInputMsg(final String msg){
        System.err.println("Invalid input detected: "+msg);    
    }    
    
    
    /**
     * Prints out the given data as csv output.
     * The first ArrayList contains the columns which describe the following data lines.<br><br>
     * 
     * Example output:<br><br>
     * username,email,mycolumn<br>
     * testuser,test@test.org,mycolumndata<br>
     * 
     * @param columns
     * @param data
     */
    protected final static void doCSVOutput(final ArrayList<String> columns, final ArrayList<ArrayList<String>> data){
        if(columns!=null && data!=null){
            
            // first prepare the columns line
            StringBuilder sb = new StringBuilder();
            for (final String column_entry : columns) {
                sb.append(column_entry);
                sb.append(",");
            }
            if(sb.length()>0){
                // remove last ","
                sb.deleteCharAt(sb.length()-1);
            }
            
            // print the columns line
            System.out.println(sb.toString());            
            
            // now prepare all data lines
            for (final ArrayList<String> data_list : data) {
                sb = new StringBuilder();
                for (final String data_column : data_list) {
                    if(data_column!=null){
                        sb.append("\"");
                        sb.append(data_column);
                        sb.append("\"");
                    }
                    sb.append(",");
                }
                if(sb.length()>0){
                    // remove trailing ","
                    sb.deleteCharAt(sb.length()-1);
                }
                // print out data line with linessbreak
                System.out.println(sb.toString());
            }
        }
    }
    
    protected final static void setRMI_HOSTNAME(final String rmi_hostname) {       
        String host = rmi_hostname;
        if(!host.startsWith("rmi://")){
            host = "rmi://"+host;
        }
        if(!host.endsWith("/")){
            host = host+"/";
        }
        RMI_HOSTNAME = host;
        
    }
   
    protected final Option setLongOpt(final AdminParser admp, final String longopt, final String description, final boolean hasarg, final boolean required) {
        
        final Option retval = admp.addOption(longopt, longopt, description, required,hasarg);
//        //OptionBuilder.withLongOpt( longopt ).withDescription( description ).withValueSeparator( '=' ).create();
//        if (hasarg) {
//            retval.hasArg();
//        }
//        retval.setRequired(required);
        return retval;
    }

    protected final Option setLongOpt(final AdminParser admp, final String longopt, final String description, final boolean hasarg, final boolean required, final boolean extended) {
        
        final Option retval = admp.addOption(longopt, longopt, description, required, hasarg, extended);
        return retval;
    }

    protected final Option setLongOpt(final AdminParser admp, final String longopt, final String argdescription, final String description, final boolean hasarg, final boolean required, final boolean extended) {
        
        final Option retval = admp.addOption(longopt, argdescription, description, required, hasarg, extended);
        return retval;
    }

    protected final Option setShortLongOpt(final AdminParser admp,final char shortopt, final String longopt, final String argdescription, final String description, final boolean required) {
        final Option retval = admp.addOption(shortopt, longopt, argdescription, description, required);       
        return retval;
    }

    protected final Option setShortLongOpt(final AdminParser admp,final char shortopt, final String longopt, final String description, final boolean hasarg, final boolean required) {
        final Option retval = admp.addOption(shortopt,longopt, longopt, description, required,hasarg);       
        return retval;
    }
    
    protected final Option setShortLongOptWithDefault(final AdminParser admp,final char shortopt, final String longopt, final String description, final String defaultvalue, final boolean hasarg, final boolean required) {
        final StringBuilder desc = new StringBuilder();
        desc.append(description);
        desc.append(". Default: ");
        desc.append(defaultvalue);
        
        return setShortLongOpt(admp, shortopt, longopt, desc.toString(), hasarg, required);
    }

    protected final Option getContextOption(final AdminParser admp) {
        this.contextOption = setShortLongOpt(admp,OPT_NAME_CONTEXT_SHORT, OPT_NAME_CONTEXT_LONG, OPT_NAME_CONTEXT_DESCRIPTION, true, false);        
//        retval.setArgName("Context ID");
        return this.contextOption;
    }
    
    protected final Option getContextNameOption(final AdminParser admp) {
        final Option retval = setShortLongOpt(admp,OPT_NAME_CONTEXT_NAME_SHORT, OPT_NAME_CONTEXT_NAME_LONG, OPT_NAME_CONTEXT_NAME_DESCRIPTION, true, false);
//        retval.setArgName("Context Name");
        return retval;
    }
    
    protected final Option getAdminPassOption(final AdminParser admp) {
        this.adminPassOption = setShortLongOpt(admp,OPT_NAME_ADMINPASS_SHORT, OPT_NAME_ADMINPASS_LONG, OPT_NAME_ADMINPASS_DESCRIPTION, true, true);
//        retval.setArgName("Admin password");
        return this.adminPassOption;
    }
    
    protected final void setCSVOutputOption(final AdminParser admp) {
        this.csvOutputOption = setLongOpt(admp, OPT_NAME_CSVOUTPUT_LONG, OPT_NAME_CSVOUTPUT_DESCRIPTION, false, false);
    }
    
    protected final Option getAdminUserOption(final AdminParser admp) {
        this.adminUserOption= setShortLongOpt(admp,OPT_NAME_ADMINUSER_SHORT, OPT_NAME_ADMINUSER_LONG, OPT_NAME_ADMINUSER_DESCRIPTION, true, true);
//        retval.setArgName("Admin username");
        return this.adminUserOption;
    }
    
    protected final void setSearchPatternOption(final AdminParser admp){
        this.searchOption = setShortLongOpt(admp,OPT_NAME_SEARCHPATTERN, OPT_NAME_SEARCHPATTERN_LONG, "The search pattern which is used for listing", true, false);
    }

//    protected final Option addArgName(final Option option, final String argname) {
//        final Option retval = option;
////        retval.setArgName(argname);
//        return retval;
//    }
    
    @Deprecated
    protected final Option addDefaultArgName(final AdminParser admp,final Option option) {
//        return addArgName(option, option.getLongOpt(admp));
        // FIXME
        return null;
    }

    protected final int testStringAndGetIntOrDefault(final String test, final int defaultvalue) throws NumberFormatException {
        if (null != test) {
            return Integer.parseInt(test);
        } else {
            return defaultvalue;
        }
    }
    
    protected final String testStringAndGetStringOrDefault(final String test, final String defaultvalue) {
        if (null != test) {
            return test;
        } else {
            return defaultvalue;
        }
    }

    /**
     * 
     * @return Options containing context,adminuser,adminpass Option objects.
     */
    protected void setDefaultCommandLineOptions(final AdminParser admp){
        
        final Option[] options = new Option[3];
        this.contextOption = getContextOption(admp);
        this.adminUserOption = getAdminUserOption(admp); 
        this.adminPassOption = getAdminPassOption(admp);
        
        options[0] = this.contextOption;
        options[1] = this.adminUserOption;
        options[2] = this.adminPassOption;
    }
}
