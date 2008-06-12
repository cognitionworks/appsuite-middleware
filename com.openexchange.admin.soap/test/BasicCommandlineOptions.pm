=begin
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
=cut
package BasicCommandlineOptions;
use Data::Dumper;
use strict;

=begin
 * This abstract class contains all the common options between all command line tools
 * @author cutmasta,d7
 *
=cut

our $dividechar = ' ';

=begin
 Used when username/password credentials were not correct!
=cut
our $SYSEXIT_INVALID_CREDENTIALS=101;

=begin
 Used when the requested context does not exists on the server!
=cut
our $SYSEXIT_NO_SUCH_CONTEXT=102;

=begin
     * Used when wrong data was sent to the server!
=cut
our $SYSEXIT_INVALID_DATA=103;

=begin
 Used when an option is missing to execute the cmd tool!
=cut
our $SYSEXIT_MISSING_OPTION=104;
    
=begin
 Used when a communication problem was encountered
=cut
our $SYSEXIT_COMMUNICATION_ERROR =105;
    
=begin
 Used when a storage problem was encountered on the server!
=cut
our $SYSEXIT_SERVERSTORAGE_ERROR =106;
    
=begin
 Used when a remote server problem was encountered !
=cut
our $SYSEXIT_REMOTE_ERROR =107;
   
=begin
    * Used when an user does not exists
=cut
our $SYSEXIT_NO_SUCH_USER =108;
   
=begin
    * Used when an unknown option was passed to the cmd tool!
=cut
our $SYSEXIT_ILLEGAL_OPTION_VALUE=109;
   
=begin
    * Used when a context already exists
=cut
our $SYSEXIT_CONTEXT_ALREADY_EXISTS=110;
   
=begin
    * Used when an unknown option was passed to the cmd tool!
=cut
our $SYSEXIT_UNKNOWN_OPTION=111;
   
=begin
     * Used when a group does not exists
=cut
our $SYSEXIT_NO_SUCH_GROUP =112;
    
=begin
     * Used when a resource does not exists
=cut
our $SYSEXIT_NO_SUCH_RESOURCE =113;

our $DEFAULT_CONTEXT=1;
our $OPT_NAME_CONTEXT_SHORT='c';
our $OPT_NAME_CONTEXT_LONG="contextid";
our $OPT_NAME_CONTEXT_NAME_SHORT='N';
our $OPT_NAME_CONTEXT_NAME_LONG="contextname";
our $OPT_NAME_CONTEXT_NAME_DESCRIPTION="context name";
our $OPT_NAME_CONTEXT_DESCRIPTION="The id of the context";
our $OPT_NAME_ADMINUSER_SHORT='A';
our $OPT_NAME_ADMINUSER_LONG="adminuser";
our $OPT_NAME_ADMINPASS_SHORT='P';
our $OPT_NAME_ADMINPASS_LONG="adminpass";
our $OPT_NAME_ADMINPASS_DESCRIPTION="Admin password";
our $OPT_NAME_ADMINUSER_DESCRIPTION="Admin username";
our $OPT_NAME_SEARCHPATTERN_LONG = "searchpattern";
our $OPT_NAME_SEARCHPATTERN = 's';
    
our $OPT_NAME_CSVOUTPUT_LONG = "csv";
our $OPT_NAME_CSVOUTPUT_DESCRIPTION = "Format output to csv";

our @ENV_OPTIONS = ( "RMI_HOSTNAME", "COMMANDLINE_TIMEZONE", "COMMANDLINE_DATEFORMAT");
our $RMI_HOSTNAME ="rmi://localhost:1099/";
our $COMMANDLINE_TIMEZONE ="GMT";
our $COMMANDLINE_DATEFORMAT ="yyyy-MM-dd";
    
    

=begin
    protected Option contextOption = null;
    protected Option contextNameOption = null;
    protected Option adminUserOption = null;
    protected Option adminPassOption = null;
    protected Option searchOption = null;
    protected Option csvOutputOption = null;
=cut
  
=begin
my $test = new BasicCommandlineOptions();
my $columns = [ "id", "name", "displayname", "email", "available"];
my $row1 = [ "4", "test", "test", "xyz\@bla.de", "true" ];
my $row2 = [ "4", "test", "test", "xyz\@bla.de", "true" ];
my $data = [ $row1, $row2 ];
$test->doCSVOutput($columns, $data);
# Used for right error output
#    protected Integer ctxid = null;
    
=begin
     * 
=cut
sub new {
	my ($inPkg) = @_;
	my $self = {};
	$self->{'basisUrl'} = "http://127.0.0.1/servlet/axis2/services/";
	$self->{'serviceNs'} = "http://soap.admin.openexchange.com";
	$self->{'Context'} = SOAP::Data->type("Context")->value(\SOAP::Data->value(SOAP::Data->name("id" => "111")));
	$self->{'creds'} = SOAP::Data->type("Credentials")->value(\SOAP::Data->value(SOAP::Data->name("login" => "oxadmin"),SOAP::Data->name("password" => "secret")));
	
	foreach my $opt(@ENV_OPTIONS) {
		# Call setEnvConfigOption(opt); here
	}
        
	bless $self, $inPkg;
    return $self;
}

=begin
    public static final Hashtable<String,String> getEnvOptions() {
        Hashtable<String, String> opts = new Hashtable<String, String>();
        for( final String opt : ENV_OPTIONS ) {
            try {
                Field f = BasicCommandlineOptions.class.getDeclaredField(opt);
                opts.put(opt, (String)f.get(null));
            } catch (SecurityException e) {
                System.err.println("unable to get commandline option \""+opt+"\"");
                e.printStackTrace();
            } catch (NoSuchFieldException e) {
                System.err.println("unable to get commandline option \""+opt+"\"");
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                System.err.println("unable to get commandline option \""+opt+"\"");
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                System.err.println("unable to get commandline option \""+opt+"\"");
                e.printStackTrace();
            }
        }
        return opts;
    }
    
    private final void setEnvConfigOption(final String opt) {
        final String property = System.getProperties().getProperty(opt);
        final String env = System.getenv(opt);
        String setOpt = null;
        if (null != env && env.trim().length()>0) {
            setOpt = env;
        } else if (null != property && property.trim().length()>0) {
            setOpt = property;
        }
        if( setOpt != null ) {
            if( opt.equals("RMI_HOSTNAME") ) {
                setRMI_HOSTNAME(setOpt);
            } else {
                try {
                    Field f = BasicCommandlineOptions.class.getDeclaredField(opt);
                    f.set(this, setOpt);
                } catch (SecurityException e) {
                    System.err.println("unable to set commandline option for \""+opt+"\" to \"" + setOpt+ "\"");
                    e.printStackTrace();
                } catch (NoSuchFieldException e) {
                    System.err.println("unable to set commandline option for \""+opt+"\" to \"" + setOpt+ "\"");
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    System.err.println("unable to set commandline option for \""+opt+"\" to \"" + setOpt+ "\"");
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    System.err.println("unable to set commandline option for \""+opt+"\" to \"" + setOpt+ "\"");
                    e.printStackTrace();
                }
            }
        }
    }
=cut

=begin
    protected final void printServerException(final Exception e, final AdminParser parser){
        String output = "";
        final String msg = e.getMessage();
        if( parser != null && parser.checkNoNewLine() ) {
            if( msg != null ) {
                output = "Server response: " + msg.replace("\n", "");
            } else {
                output += e.toString();
                for(final StackTraceElement ste : e.getStackTrace() ) {
                    output += ": " + ste.toString().replace("\n", "");
                }
            }
        } else {
            if( msg != null ) {
                output = "Server response:\n "+msg;
            } else {
                output += e.toString() + "\n";
                for (final StackTraceElement ste : e.getStackTrace()) {
                    output += "\tat " + ste.toString() + "\n";
                }
            }
        }
        System.err.println(output);
    }
=cut
    
=begin
    protected final void printError(final String msg, final AdminParser parser){
        String output = null;
        if( parser == null ) {
            output = msg;
        } else {
            if (parser.checkNoNewLine()) {
                output =  msg.replace("\n", "");
            } else {
                output = msg;
            }
        }
        System.err.println("Error: " + output);
    }
    
    protected final void printServerResponse(final String msg){
        System.err.println("Server response:\n "+msg+"\n");    
    }

    protected final void printInvalidInputMsg(final String msg){
        System.err.println("Invalid input detected: "+msg);    
    }
=cut
    
=begin
     * Prints out the given data as csv output.
     * The first ArrayList contains the columns which describe the following data lines.<br><br>
     * 
     * Example output:<br><br>
     * username,email,mycolumn<br>
     * testuser,test@test.org,mycolumndata<br>
     * 
     * @param columns
     * @param data
     * @throws InvalidDataException 
=cut
sub doCSVOutput {
	my $inSelf = shift;
	my $columns = shift;
	my $data = shift;
	
	#print Dumper($columns);
	#print Dumper($data);
	#my ($inSelf, @columns, @data) = @_;
    # first prepare the columns line
    # StringBuilder sb = new StringBuilder();
    my $sb = "";
    foreach my $column_entry(@$columns) {
    	$sb .= $column_entry;
    	$sb .= ",";
    }
    if (length $sb > 0) {
		#remove last ","
		chop($sb);
    } 
        
    # print the columns line
    print $sb."\n";
    
    if (defined($columns) && scalar(@$columns) != 0 && defined($data) && scalar(@$data) != 0) {
    	# Check if each row in data is != 0
    	foreach my $ref (@$data) {
    		if (scalar(@$ref) == 0) {
    			throw InvalidDataException("One of the data rows is null");
    		}
    	}
    	if (scalar(@$columns) != scalar(@{@$data[0]})) {
  			throw InvalidDataException("Number of columnnames and number of columns in data object must be the same");
    	}
        # now prepare all data lines
        foreach my $data_list (@$data) {
        	my $sb = "";
        	foreach my $data_column (@$data_list) {
        		if (defined($data_column)) {
        			$sb .= "\"".$data_column."\"";
        		}
        		$sb .= ",";
        	}
        	if (length $sb > 0) {
        		# remove trailing ","
        		chop($sb);
        	}
        	print $sb."\n";
        }
    }
}
=begin    
    protected final void setRMI_HOSTNAME(final String rmi_hostname) {       
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
    
    protected final Option setSettableBooleanLongOpt(final AdminParser admp, final String longopt, final String argdescription, final String description, final boolean hasarg, final boolean required, final boolean extended) {
        
        final Option retval = admp.addSettableBooleanOption(longopt, argdescription, description, required, hasarg, extended);
        return retval;
    }

    protected final Option setShortLongOpt(final AdminParser admp,final char shortopt, final String longopt, final String argdescription, final String description, final boolean required) {
        final Option retval = admp.addOption(shortopt, longopt, argdescription, description, required);       
        return retval;
    }

    protected final Option setShortLongOpt(final AdminParser admp,final char shortopt, final String longopt, final String description, final boolean hasarg, final NeededQuadState required) {
        final Option retval = admp.addOption(shortopt,longopt, longopt, description, required, hasarg);       
        return retval;
    }
    
    protected final Option setShortLongOptWithDefault(final AdminParser admp,final char shortopt, final String longopt, final String description, final String defaultvalue, final boolean hasarg, final NeededQuadState required) {
        final StringBuilder desc = new StringBuilder();
        desc.append(description);
        desc.append(". Default: ");
        desc.append(defaultvalue);
        
        return setShortLongOpt(admp, shortopt, longopt, desc.toString(), hasarg, required);
    }

    protected final Option setShortLongOptWithDefault(final AdminParser admp,final char shortopt, final String longopt, final String argdescription, final String description, final String defaultvalue, final boolean required) {
        final StringBuilder desc = new StringBuilder();
        desc.append(description);
        desc.append(". Default: ");
        desc.append(defaultvalue);
        
        return setShortLongOpt(admp, shortopt, longopt, argdescription, desc.toString(), required);
    }

    protected final void setContextOption(final AdminParser admp, final NeededQuadState needed) {
        this.contextOption = setShortLongOpt(admp,OPT_NAME_CONTEXT_SHORT, OPT_NAME_CONTEXT_LONG, OPT_NAME_CONTEXT_DESCRIPTION, true, needed);        
    }
    
    protected final void setContextNameOption(final AdminParser admp, final NeededQuadState needed) {
        this.contextNameOption = setShortLongOpt(admp,OPT_NAME_CONTEXT_NAME_SHORT, OPT_NAME_CONTEXT_NAME_LONG, OPT_NAME_CONTEXT_NAME_DESCRIPTION, true, needed);
    }
    
    protected void setAdminPassOption(final AdminParser admp) {
        this.adminPassOption = setShortLongOpt(admp,OPT_NAME_ADMINPASS_SHORT, OPT_NAME_ADMINPASS_LONG, OPT_NAME_ADMINPASS_DESCRIPTION, true, NeededQuadState.possibly);
    }
    
    protected final void setCSVOutputOption(final AdminParser admp) {
        this.csvOutputOption = setLongOpt(admp, OPT_NAME_CSVOUTPUT_LONG, OPT_NAME_CSVOUTPUT_DESCRIPTION, false, false);
    }
    
    protected void setAdminUserOption(final AdminParser admp) {
        this.adminUserOption= setShortLongOpt(admp,OPT_NAME_ADMINUSER_SHORT, OPT_NAME_ADMINUSER_LONG, OPT_NAME_ADMINUSER_DESCRIPTION, true, NeededQuadState.possibly);
    }
    
    protected final void setSearchPatternOption(final AdminParser admp){
        this.searchOption = setShortLongOpt(admp,OPT_NAME_SEARCHPATTERN, OPT_NAME_SEARCHPATTERN_LONG, "The search pattern which is used for listing", true, NeededQuadState.notneeded);
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

    protected final boolean testStringAndGetBooleanOrDefault(final String test, final boolean defaultvalue) {
        if (null != test) {
            return Boolean.parseBoolean(test);
        } else {
            return defaultvalue;
        }
    }
=cut
=begin
     * 
=cut
=begin
    protected void setDefaultCommandLineOptions(final AdminParser admp){
        setContextOption(admp, NeededQuadState.needed);
        setAdminUserOption(admp); 
        setAdminPassOption(admp);
    }

    
    protected final void setDefaultCommandLineOptionsWithoutContextID(final AdminParser parser) {          
        setAdminUserOption(parser);
        setAdminPassOption(parser);
    }

    protected void sysexit(final int exitcode) {
        // see http://java.sun.com/j2se/1.5.0/docs/guide/rmi/faq.html#leases
        System.gc();
        System.runFinalization();
        // 
        System.exit(exitcode);
    }

    protected final Context contextparsing(final AdminParser parser) {
        final Context ctx = new Context();
    
        if (parser.getOptionValue(this.contextOption) != null) {
            ctxid = Integer.parseInt((String) parser.getOptionValue(this.contextOption));
            ctx.setId(ctxid);
        }
        return ctx;
    }

    protected final Credentials credentialsparsing(final AdminParser parser) {
        final Credentials auth = new Credentials((String) parser.getOptionValue(this.adminUserOption), (String) parser.getOptionValue(this.adminPassOption));
        return auth;
    }
=cut
=begin
     * Strips a string to the given size and adds the given lastmark to it to signal that the string is longer
     * than specified
     * 
     * @param test
     * @param length
     * @return
=cut
=begin
    private String stripString(final String text, final int length, final String lastmark) {
        if (null != text && text.length() > length) {
            final int stringlength = length - lastmark.length();
            return new StringBuffer(text.substring(0, stringlength)).append(lastmark).toString();
        } else if (text == null) {
            return "";
        } else {
            return text;
        }
    }
=cut
=begin
     * This method takes an array of objects and format them in one comma-separated string
     * 
     * @param objects
     * @return
=cut
=begin
    protected String getObjectsAsString(final Object[] objects) {
        final StringBuilder sb = new StringBuilder();
        if (objects != null && objects.length > 0) {
            for (final Object id : objects) {
                sb.append(id);
                sb.append(",");
            }
            sb.deleteCharAt(sb.length() - 1);
            
            return sb.toString();
        } else {
            return "";
        }
        
    }

    private final int longestLine(final ArrayList<ArrayList<String>> data, final String[] columnnames, final int column) {
        //long start = System.currentTimeMillis();
        int max = columnnames[column].length();
        for(int row=0; row<data.size(); row++) {
            final String value = data.get(row).get(column);
            if( value != null ) {
                final int curLength = data.get(row).get(column).length();
                if( curLength > max ) {
                    max = curLength;
                }
            }
        }
        //System.out.println("calc took " + (System.currentTimeMillis()-start) + "ms");
        return max;
    }
   
    protected void doOutput(final String[] columnsizesandalignments, final String[] columnnames, final ArrayList<ArrayList<String>> data) throws InvalidDataException {
        if (columnsizesandalignments.length != columnnames.length) {
            throw new InvalidDataException("The sizes of columnsizes and columnnames aren't equal");
        }
        final int[] columnsizes = new int[columnsizesandalignments.length];
        final char[] alignments = new char[columnsizesandalignments.length];
        final StringBuilder formatsb = new StringBuilder();
        for (int i = 0; i < columnsizesandalignments.length; i++) {
            // fill up part
            try {
                columnsizes[i] = Integer.parseInt(columnsizesandalignments[i].substring(0, columnsizesandalignments[i].length() - 1));
            } catch (final NumberFormatException e) {
                // there's no number, so use longest line as alignment value
                columnsizes[i] = longestLine(data,columnnames,i);
            }            
            alignments[i] = columnsizesandalignments[i].charAt(columnsizesandalignments[i].length() - 1);

            // check part
            if (columnnames[i].length() > columnsizes[i]) {
                throw new InvalidDataException("Columnsize for column " + columnnames[i] + " is too small for columnname");
            }

            // formatting part
            formatsb.append("%");
            if (alignments[i] == 'l') {
                formatsb.append('-');
            }
            formatsb.append(columnsizes[i]);
            formatsb.append('s');
            formatsb.append(dividechar);
        }
        formatsb.deleteCharAt(formatsb.length() - 1);
        formatsb.append('\n');
        System.out.format(formatsb.toString(), (Object[]) columnnames);
        for (final ArrayList<String> row : data) {
            if (row.size() != columnsizesandalignments.length) {
                throw new InvalidDataException("The size of one of the rows isn't correct");
            }
            final Object[] outputrow = new Object[columnsizesandalignments.length];
            for (int i = 0; i < columnsizesandalignments.length; i++) {
                final String value = row.get(i);
                outputrow[i] = stripString(value, columnsizes[i], "~");
            }
            System.out.format(formatsb.toString(), outputrow);
        }
    }

    @SuppressWarnings("unchecked")
    protected final void printExtensionsError(final ExtendableDataObject obj) {
        //+ loop through extensions and check for errors       
        if (obj != null && obj.getAllExtensionsAsHash() != null) {
            for (final OXCommonExtension obj_extension : obj.getAllExtensionsAsHash().values()) {
                if (obj_extension.getExtensionError() != null) {
                    printServerResponse(obj_extension.getExtensionError());
                }
            }
        }
    }

    protected final NeededQuadState convertBooleantoTriState(boolean needed) {
        if (needed) {
            return NeededQuadState.needed;
        } else {
            return NeededQuadState.notneeded;
        }
    }

    // We have to serve this 2nd method here because String.valueOf return "null" as String
    // instead of null from an Integer null object. So we have to deal with this situation
    // ourself
    protected String nameOrIdSetInt(final Integer id, final String name, final String objectname) throws MissingOptionException {
        if (null == id) {
            return nameOrIdSet(null, name, objectname);
        } else {
            return nameOrIdSet(String.valueOf(id), name, objectname);
        }
    }

    protected String nameOrIdSet(final String id, final String name, final String objectname) throws MissingOptionException {
        String successtext;
        // Through the order of this checks we archive that the id is preferred over the name
        if (null == id) {
            if (null == name) {
                throw new MissingOptionException("Either " + objectname + "name or " + objectname + "id must be given");
            } else {
                successtext = name;
            }
        } else {
            successtext = String.valueOf(id);
        }
        return successtext;
    }
}
=cut

sub fetch_results {
    my $self = shift;
    my $fields = shift;
    my $results = shift;
    
    my @data;
    
    for my $result (@$results) {
        my @row;
        foreach my $field (@$fields) {
             push (@row, $result->{$field});
        }
        
    #    for my $key ( keys %$result ) {
    #        print $key."\n";
    #    }
        push (@data, \@row);
    } 
    
    return @data;
}

sub fault_output {
    my $self = shift;
    my $s = shift;
    printf("Code: %s\nString: %s\nDetail: %s\nActor: %s\n",
        $s->faultcode(), $s->faultstring(), Dumper($s->faultdetail()), $s->faultactor() );
}
