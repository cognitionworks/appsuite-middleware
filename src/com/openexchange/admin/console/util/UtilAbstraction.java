package com.openexchange.admin.console.util;

import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.BasicCommandlineOptions;
import com.openexchange.admin.console.CmdLineParser.Option;

/**
 * 
 * @author d7,cutmasta
 *
 */
public class UtilAbstraction extends BasicCommandlineOptions {
    
    //  Setting default values for some options
    protected final static String HOSTNAME_DEFAULT = "localhost";
    protected final static String USER_DEFAULT = "openexchange";
    protected final static int MAXUNITS_DEFAULT = 1000;
    protected final static int CLUSTER_WEIGHT_DEFAULT = 100;
    protected final static int POOL_HARD_LIMIT_DEFAULT = 0;
    protected final static int POOL_INITIAL_DEFAULT = 2;
    protected final static int POOL_MAX_DEFAULT = 100;
    protected final static String DRIVER_DEFAULT = "com.mysql.jdbc.Driver";
//    protected final static String STORE_PATH_DEFAULT = "file:///tmp/filestore";
    protected final static long STORE_SIZE_DEFAULT = 1000;
    protected final static int STORE_MAX_CTX_DEFAULT = 5000;
    
    //  Setting names for options
    protected final static char OPT_NAME_SEARCH_PATTERN_SHORT = 's';
    protected final static String OPT_NAME_SEARCH_PATTERN_LONG = "searchpattern";
    protected final static char OPT_NAME_DBNAME_SHORT='n';
    protected final static String OPT_NAME_DBNAME_LONG="name";
    protected final static char OPT_NAME_HOSTNAME_SHORT='h';
    protected final static String OPT_NAME_HOSTNAME_LONG="hostname";
    protected final static char OPT_NAME_DB_USERNAME_SHORT='u';
    protected final static String OPT_NAME_DB_USERNAME_LONG="dbuser";
    protected final static char OPT_NAME_DB_PASSWD_SHORT='p';
    protected final static String OPT_NAME_DB_PASSWD_LONG="dbpasswd";
    protected final static char OPT_NAME_IS_MASTER_SHORT='m';
    protected final static String OPT_NAME_IS_MASTER_LONG="master";
    protected final static char OPT_NAME_MASTER_ID_SHORT='i';
    protected final static String OPT_NAME_MASTER_ID_LONG="masterid";
    protected final static char OPT_NAME_WEIGHT_SHORT='w';
    protected final static String OPT_NAME_WEIGHT_LONG="dbweight";
    protected final static char OPT_NAME_MAX_UNITS_SHORT='x';
    protected final static String OPT_NAME_MAX_UNITS_LONG="maxunit";
    protected final static char OPT_NAME_DBPARAM_SHORT='b';
    protected final static String OPT_NAME_DBPARAM_LONG="dbparam";
    protected final static char OPT_NAME_POOL_HARDLIMIT_SHORT='l';
    protected final static String OPT_NAME_POOL_HARDLIMIT_LONG="poolhardlimit";
    protected final static char OPT_NAME_POOL_INITIAL_SHORT='o';
    protected final static String OPT_NAME_POOL_INITIAL_LONG="poolinitial";
    protected final static char OPT_NAME_POOL_MAX_SHORT='a';
    protected final static String OPT_NAME_POOL_MAX_LONG="poolmax";
    protected final static char OPT_NAME_DB_DRIVER_SHORT='d';
    protected final static String OPT_NAME_DB_DRIVER_LONG="dbdriver";
    protected final static char OPT_NAME_STORE_FILESTORE_ID_SHORT = 'i';
    protected final static String OPT_NAME_STORE_FILESTORE_ID_LONG = "id";
    protected final static char OPT_NAME_STORE_PATH_SHORT = 't';
    protected final static String OPT_NAME_STORE_PATH_LONG = "storepath";
    protected final static char OPT_NAME_STORE_SIZE_SHORT = 's';
    protected final static String OPT_NAME_STORE_SIZE_LONG = "storesize";
    protected final static char OPT_NAME_STORE_MAX_CTX_SHORT = 'x';
    protected final static String OPT_NAME_STORE_MAX_CTX_LONG = "maxcontexts";
    
    /**
     * 
     * @return Options containing adminuser,adminpass Option objects.
     */
    @Override
    protected void setDefaultCommandLineOptions(final AdminParser parser){          
        
        getAdminUserOption(parser);
        getAdminPassOption(parser);        
        
    }
    
    protected void setDatabaseNameOption(final AdminParser parser,final boolean required){
        this.databaseNameOption = setShortLongOpt(parser, OPT_NAME_DBNAME_SHORT,OPT_NAME_DBNAME_LONG,"Name of the database",true, required); 
    }
    
    protected void setSearchOption(final AdminParser parser){
        this.searchOption = setShortLongOpt(parser, OPT_NAME_SEARCH_PATTERN_SHORT,OPT_NAME_SEARCH_PATTERN_LONG,"Search/List pattern!",true, false);
    }
    
    protected void setDatabaseHostnameOption(final AdminParser parser,final boolean required){
        this.hostnameOption =  setShortLongOpt(parser, OPT_NAME_HOSTNAME_SHORT,OPT_NAME_HOSTNAME_LONG,"Hostname of the server",true, required); 
    }
    
    protected void setDatabaseUsernameOption(final AdminParser parser,final boolean required){
        this.databaseUsernameOption = setShortLongOpt(parser, OPT_NAME_DB_USERNAME_SHORT,OPT_NAME_DB_USERNAME_LONG,"Name of the user for the database",true, required);
    }
    
    protected void setDatabaseDriverOption(final AdminParser parser,final boolean required){
        this.databaseDriverOption = setShortLongOpt(parser, OPT_NAME_DB_DRIVER_SHORT,OPT_NAME_DB_DRIVER_LONG,"The driver to be used for the database",true, required);
    }
    
    protected void setDatabasePasswdOption(final AdminParser parser,final boolean required){
        this.databasePasswdOption = setShortLongOpt(parser, OPT_NAME_DB_PASSWD_SHORT,OPT_NAME_DB_PASSWD_LONG,"Password for the database",true, required);
    }
    
    protected void setDatabaseIsMasterOption(final AdminParser parser,final boolean required){
        this.databaseIsMasterOption = setShortLongOpt(parser, OPT_NAME_IS_MASTER_SHORT,OPT_NAME_IS_MASTER_LONG,"Set this if the registered database is the master",false,required);
    }
    protected void setDatabaseMasterIDOption(final AdminParser parser,final boolean required){
        this.databaseMasterIDOption = setShortLongOpt(parser, OPT_NAME_MASTER_ID_SHORT,OPT_NAME_MASTER_ID_LONG,"If this database isn't the master give the id of the master here",true,required);
    }
    protected void setDatabaseWeightOption(final AdminParser parser,final boolean required){
        this.databaseWeightOption = setShortLongOpt(parser, OPT_NAME_WEIGHT_SHORT,OPT_NAME_WEIGHT_LONG,"The db weight for this database",true,required);
    }
    protected void setDatabaseMaxUnitsOption(final AdminParser parser,final boolean required){
        this.maxUnitsOption = setShortLongOpt(parser, OPT_NAME_MAX_UNITS_SHORT,OPT_NAME_MAX_UNITS_LONG,"The maximum number of units in this database",true,required);
    }
    protected void setDatabasePoolHardlimitOption(final AdminParser parser,final boolean required){
//      FIXME: choeger Enter right description here        
        this.poolHardlimitOption = setShortLongOpt(parser, OPT_NAME_POOL_HARDLIMIT_SHORT,OPT_NAME_POOL_HARDLIMIT_LONG,"Db pool hardlimit",true,required);
    }
    protected void setDatabasePoolInitialOption(final AdminParser parser,final boolean required){
        this.poolInitialOption = setShortLongOpt(parser, OPT_NAME_POOL_INITIAL_SHORT,OPT_NAME_POOL_INITIAL_LONG,"Db pool initial",true,required);
    }
    protected void setDatabasePoolMaxOption(final AdminParser parser,final boolean required){
        this.poolMaxOption = setShortLongOpt(parser, OPT_NAME_POOL_MAX_SHORT,OPT_NAME_POOL_MAX_LONG,"Db pool max",true,required);
    }
    
    protected void setDatabaseParatmeterOption(final AdminParser parser,final boolean required){
        this.poolMaxOption = setShortLongOpt(parser, OPT_NAME_DBPARAM_SHORT,OPT_NAME_DBPARAM_LONG,"Parameter for the database",true,required);
    }
    
    // for all tools
    protected Option searchOption = null;
    // for database tools
    protected Option databaseNameOption = null;
    protected Option hostnameOption = null;
    protected Option databaseUsernameOption = null;
    protected Option databaseDriverOption = null;
    protected Option databasePasswdOption = null;
    protected Option databaseIsMasterOption = null;
    protected Option databaseMasterIDOption = null;
    protected Option databaseWeightOption = null;    
    protected Option maxUnitsOption = null;
    protected Option databaseParameterOption = null;
    protected Option poolHardlimitOption = null;
    protected Option poolInitialOption = null;
    protected Option poolMaxOption = null;
    

}
