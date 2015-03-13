
package com.openexchange.database.migration.internal;

import java.sql.Connection;
import liquibase.Liquibase;
import liquibase.database.Database;
import liquibase.database.DatabaseConnection;
import liquibase.database.core.MySQLDatabase;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.DatabaseException;
import liquibase.exception.LiquibaseException;
import liquibase.resource.CompositeResourceAccessor;
import liquibase.resource.ResourceAccessor;
import org.osgi.framework.FrameworkUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.openexchange.database.migration.DBMigrationExceptionCodes;
import com.openexchange.database.migration.resource.accessor.BundleResourceAccessor;
import com.openexchange.exception.OXException;

public class LiquibaseHelper {

    private static final Logger LOG = LoggerFactory.getLogger(LiquibaseHelper.class);

    public static final String LIQUIBASE_NO_DEFINED_CONTEXT = "";

    /**
     * Prepares a new liquibase instance for the given file location. The instance is initialized with a writable non-timeout connection to
     * the underlying database.
     *
     * @param connection The database connection to use
     * @param fileLocation The file location
     * @param accessor Needed to access the given file
     * @return The initialized liquibase instance
     */
    public static Liquibase prepareLiquibase(Connection connection, String fileLocation, ResourceAccessor accessor) throws LiquibaseException, OXException {
        MySQLDatabase database = new MySQLDatabase();
        database.setConnection(new JdbcConnection(connection));
        return new Liquibase(fileLocation, LiquibaseHelper.prepareResourceAccessor(accessor), database);
    }

    /**
     * All liquibase locks are released and the underlying connection is closed.
     *
     * @param liquibase The liquibase instance. If <code>null</code>, calling this method has no effect.
     * @return The underlying database connection, or <code>null</code> if not available
     * @throws OXException If an error occurs while releasing the locks
     */
    public static Connection cleanUpLiquibase(Liquibase liquibase) throws OXException {
        if (liquibase != null) {
            try {
                liquibase.forceReleaseLocks();
            } catch (LiquibaseException liquibaseException) {
                throw DBMigrationExceptionCodes.LIQUIBASE_ERROR.create(liquibaseException);
            } finally {
                Database database = liquibase.getDatabase();
                if (database != null) {
                    DatabaseConnection connectionWrapper = database.getConnection();
                    if (connectionWrapper != null) {
                        try {
                            return  ((JdbcConnection) connectionWrapper).getUnderlyingConnection();
                        } catch (ClassCastException e) {
                            LOG.warn("An unexpected connection instance was passed, it will be closed manually.", e);
                            try {
                                connectionWrapper.close();
                            } catch (DatabaseException d) {
                                LOG.error("Could not close unknown connection instance!", d);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    /**
     * Prepares a {@link CompositeResourceAccessor} containing the given {@link ResourceAccessor} and one for this bundle.
     *
     * @param provided The {@link ResourceAccessor} provided by service users
     * @return A {@link CompositeResourceAccessor}
     */
    public static ResourceAccessor prepareResourceAccessor(ResourceAccessor provided) {
        return new CompositeResourceAccessor(provided, new BundleResourceAccessor(FrameworkUtil.getBundle(LiquibaseHelper.class)));
    }

}
