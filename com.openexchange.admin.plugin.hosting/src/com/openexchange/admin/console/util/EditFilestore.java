package com.openexchange.admin.console.util;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
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
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.exceptions.InvalidCredentialsException;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;
import com.openexchange.admin.rmi.exceptions.StorageException;

/**
 * 
 * @author d7,cutmasta
 *
 */
public class EditFilestore extends UtilAbstraction {    

    public EditFilestore(final String[] args2) {

        AdminParser parser = new AdminParser("editFileStore");

       setOptions(parser);

        try {
            
            parser.ownparse(args2);

            final Credentials auth = new Credentials((String)parser.getOptionValue(adminUserOption),(String)parser.getOptionValue(adminPassOption));
            
            // get rmi ref
            final OXUtilInterface oxutil = (OXUtilInterface) Naming.lookup(OXUtilInterface.RMI_NAME);

            final Filestore fstore = new Filestore();
            final String filestore_id = (String)parser.getOptionValue(filestoreIdOption);
            final String store_path = (String)parser.getOptionValue(filestorePathOption);
            
            String store_size = String.valueOf(STORE_SIZE_DEFAULT);
            if(parser.getOptionValue(filestoreSizeOption)!=null){
                store_size = (String)parser.getOptionValue(filestoreSizeOption);
            }
            String store_max_ctx = String.valueOf(STORE_MAX_CTX_DEFAULT);
            if(parser.getOptionValue(filestoreMaxContextsOption)!=null){
                store_max_ctx = (String)parser.getOptionValue(filestoreMaxContextsOption);
            }            

            fstore.setId(Integer.parseInt(filestore_id));
            final java.net.URI uri = new java.net.URI(store_path);
            fstore.setUrl(uri.toString());
            new java.io.File(uri.getPath()).mkdir();

            if (null != store_size) {
                fstore.setSize(Long.parseLong(store_size));
            } else {
                fstore.setSize(STORE_SIZE_DEFAULT);
            }
            fstore.setMaxContexts(testStringAndGetIntOrDefault(store_max_ctx, STORE_MAX_CTX_DEFAULT));
            
            oxutil.changeFilestore(fstore, auth);
        } catch (final java.rmi.ConnectException neti) {
            printError(neti.getMessage());
            System.exit(1);
        } catch (final java.lang.NumberFormatException num) {
            printInvalidInputMsg("Ids must be numbers!");      
        } catch (final MalformedURLException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (final RemoteException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (final NotBoundException e) {
            printNotBoundResponse(e);
            System.exit(1);
        } catch (final StorageException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (final InvalidCredentialsException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (final InvalidDataException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (URISyntaxException e) {
            printServerResponse(e.getMessage());
            System.exit(1);
        } catch (IllegalOptionValueException e) {            
            printError("Illegal option value : " + e.getMessage());
            parser.printUsage();
            System.exit(1);
        } catch (UnknownOptionException e) {
            printError("Unrecognized options on the command line: " + e.getMessage());
            parser.printUsage();
            System.exit(1);
        } catch (MissingOptionException e) {
            printError(e.getMessage());
            parser.printUsage();
            System.exit(1);
        }
    }

    public static void main(final String args[]) {
        new EditFilestore(args);
    }

    private void setOptions(AdminParser parser) {
        setDefaultCommandLineOptions(parser);

        filestoreIdOption = setShortLongOpt(parser, OPT_NAME_STORE_FILESTORE_ID_SHORT,OPT_NAME_STORE_FILESTORE_ID_LONG,"The id of the filestore which should be changed",true, true);
        
        filestorePathOption = setShortLongOpt(parser, OPT_NAME_STORE_PATH_SHORT,OPT_NAME_STORE_PATH_LONG,"Path to store filestore contents",true, true);
        
        filestoreSizeOption = setShortLongOpt(parser, OPT_NAME_STORE_SIZE_SHORT,OPT_NAME_STORE_SIZE_LONG,"The maximum size of the filestore",true, false);
        
        filestoreMaxContextsOption = setShortLongOpt(parser, OPT_NAME_STORE_MAX_CTX_SHORT,OPT_NAME_STORE_MAX_CTX_LONG,"the maximum number of contexts",true, false);
        
    }
    
    private Option filestoreIdOption = null;
    private Option filestorePathOption = null;
    private Option filestoreSizeOption = null;
    private Option filestoreMaxContextsOption = null;

}
