package com.openexchange.groupware;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.ContextStorage;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;

import junit.framework.TestCase;

public class IDGeneratorTest extends TestCase {

    /**
     * Logger.
     */
    private static final Log LOG = LogFactory.getLog(IDGeneratorTest.class);

    private static final int TYPE = Types.DOCUMENT;

    private static final int THREADS = 10;

    private static final int TIME = 20;
    
    private transient Context context;
    
    protected void setUp() throws Exception {
        super.setUp();
        Init.initDB();
        context = ContextStorage.getInstance().getContext("defaultcontext");
    }

    /*
     * Test method for 'com.openexchange.groupware.IDGenerator.getId(Context, int)'
     */
    public void testGetId() throws Throwable {
        Connection con = DBPool.pickupWriteable(context);
        try {
            final Statement stmt = con.createStatement();
            try {
                stmt.execute("CREATE TABLE id_generator_test (cid INT4 UNSIGNED NOT NULL, id INT4 UNSIGNED NOT NULL, PRIMARY KEY (cid,id))");
            } catch (SQLException e) {
                LOG.fatal("Error while creating test table.", e);
                fail("Error while creating test table.");
            }
            stmt.close();
        } finally {
            DBPool.closeWriterSilent(context, con);
            con = null;
        }

        Inserter[] tester = new Inserter[THREADS];
        Thread[] threads = new Thread[tester.length];
        for (int i = 0; i < tester.length; i++) {
            tester[i] = new Inserter();
            threads[i] = new Thread(tester[i]);
            threads[i].start();
        }
        Thread.sleep(TIME * 1000);
        for (int i = 0; i < tester.length; i++) {
            tester[i].run = false;
        }
        for (int i = 0; i < tester.length; i++) {
            threads[i].join();
        }
        
        con = DBPool.pickup(context);
        try {
            final Statement stmt = con.createStatement();
            final ResultSet result = stmt.executeQuery(
                "SELECT count(*) FROM id_generator_test");
            int rows = 0;
            if (result.next()) {
                rows = result.getInt(1);
            }
            result.close();
            LOG.info("Inserted " + ((float) rows / TIME / THREADS) + " rows.");
            stmt.close();
        } finally {
            DBPool.closeWriterSilent(context, con);
            con = null;
        }
            
        con = DBPool.pickup(context);
        try {
            final Statement stmt = con.createStatement();
            try {
                stmt.execute("DROP TABLE id_generator_test");
            } catch (SQLException e) {
                LOG.fatal("Error while dropping table.", e);
            }
            stmt.close();
        } finally {
            DBPool.closeWriterSilent(context, con);
        }
    }

    private class Inserter implements Runnable {

        boolean run = true;
        
        public void run() {
            Connection con = null;
            try {
                con = DBPool.pickupWriteable(context);
            } catch (DBPoolingException e) {
                LOG.error("Can't get writable database connection.", e);
                return;
            }
            try {
                con.setAutoCommit(false);
                final PreparedStatement insert = con.prepareStatement(
                    "INSERT INTO id_generator_test (cid, id) VALUES (?, ?)");
                while (run) {
                    final int ident = IDGenerator.getId(context, TYPE, con);
                    insert.setInt(1, context.getContextId());
                    insert.setInt(2, ident);
                    insert.executeUpdate();
                }
                insert.close();
                con.commit();
            } catch (SQLException e) {
                try {
                    con.rollback();
                } catch (SQLException e1) {
                    LOG.fatal("Error while rollback.", e);
                }
                LOG.fatal("Error while getting ID and inserting.", e);
                fail(e.getMessage());
            } finally {
                try {
                    con.setAutoCommit(true);
                } catch (SQLException e) {
                    LOG.fatal("Error while setting autocommit true.", e);
                }
                DBPool.closeWriterSilent(context, con);
            }
        }
    }
}
