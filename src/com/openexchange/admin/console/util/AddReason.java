package com.openexchange.admin.console.util;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.AdminParser.MissingOptionException;
import com.openexchange.admin.console.CmdLineParser.IllegalOptionValueException;
import com.openexchange.admin.console.CmdLineParser.Option;
import com.openexchange.admin.console.CmdLineParser.UnknownOptionException;
import com.openexchange.admin.rmi.OXUtilInterface;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.MaintenanceReason;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.StorageException;

/**
 * 
 * @author d7,cutmasta
 *
 */
public class AddReason extends UtilAbstraction {
    

    private final static char OPT_NAME_REASON_TEXT_SHORT = 'r';

    private final static String OPT_NAME_REASON_TEXT_LONG = "reasontext";

    private Option reasonTextOption = null;

    public AddReason(final String[] args2) {
    
        final AdminParser parser = new AdminParser("addreason");
    
        setOptions(parser);
    
        try {
            
            parser.ownparse(args2);
    
            final Credentials auth = new Credentials((String)parser.getOptionValue(this.adminUserOption),(String)parser.getOptionValue(this.adminPassOption));
            
            // get rmi ref
            final OXUtilInterface oxutil = (OXUtilInterface) Naming.lookup(RMI_HOSTNAME +OXUtilInterface.RMI_NAME);
    
            final MaintenanceReason reason = new MaintenanceReason((String)parser.getOptionValue(this.reasonTextOption));
    
            System.out.println(oxutil.addMaintenanceReason(reason, auth));
            
            sysexit(0);
        } catch (final java.rmi.ConnectException neti) {
            printError(neti.getMessage());
            sysexit(SYSEXIT_COMMUNICATION_ERROR);
        } catch (final java.lang.NumberFormatException num) {
            printInvalidInputMsg("Ids must be numbers!");
            sysexit(1);
        } catch (final MalformedURLException e) {
            printServerResponse(e);
            sysexit(1);
        } catch (final RemoteException e) {
            printServerResponse(e);
            sysexit(SYSEXIT_REMOTE_ERROR);
        } catch (final NotBoundException e) {
            printNotBoundResponse(e);
            sysexit(1);
        } catch (final StorageException e) {
            printServerResponse(e);
            sysexit(SYSEXIT_SERVERSTORAGE_ERROR);
        } catch (final InvalidCredentialsException e) {
            printServerResponse(e);
            sysexit(SYSEXIT_INVALID_CREDENTIALS);
        } catch (final InvalidDataException e) {
            printServerResponse(e);
            sysexit(SYSEXIT_INVALID_DATA);
        } catch (final IllegalOptionValueException e) {            
            printError("Illegal option value : " + e.getMessage());
            parser.printUsage();
            sysexit(SYSEXIT_ILLEGAL_OPTION_VALUE);
        } catch (final UnknownOptionException e) {
            printError("Unrecognized options on the command line: " + e.getMessage());
            parser.printUsage();
            sysexit(SYSEXIT_UNKNOWN_OPTION);
        } catch (final MissingOptionException e) {
            printError(e.getMessage());
            parser.printUsage();
            sysexit(SYSEXIT_MISSING_OPTION);
        }
    
    }

    public static void main(final String args[]) {
        new AddReason(args);
    }

    private void setOptions(final AdminParser parser) {
        
        setDefaultCommandLineOptions(parser);

        this.reasonTextOption = setShortLongOpt(parser, OPT_NAME_REASON_TEXT_SHORT,OPT_NAME_REASON_TEXT_LONG,"the text for the added reason",true, true);
                
    }
}
