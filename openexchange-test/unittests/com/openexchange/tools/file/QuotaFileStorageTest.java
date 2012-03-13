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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.tools.file;

import com.openexchange.exception.OXException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.net.URI;
import java.sql.Connection;
import java.util.Random;
import junit.framework.TestCase;
import com.openexchange.database.DatabaseService;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.contexts.impl.ContextImpl;
import com.openexchange.test.DelayedInputStream;
import com.openexchange.tools.RandomString;
import com.openexchange.tools.file.external.FileStorage;
import com.openexchange.tools.file.internal.DBQuotaFileStorage;
import com.openexchange.tools.file.internal.LocalFileStorage;

public class QuotaFileStorageTest extends TestCase {

    private FileStorage fs;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }

    public void testBasic() throws Exception{
        // Taken from FileStorageTest
        final File tempFile = File.createTempFile("filestorage", ".tmp");

        tempFile.deleteOnExit();

        tempFile.delete();

        fs = new LocalFileStorage(new URI("file:"+tempFile.getAbsolutePath()));
        final TestQuotaFileStorage quotaStorage = new TestQuotaFileStorage(new ContextImpl(1), fs, new DummyDatabaseService());

        quotaStorage.setQuota(10000);
        // And again, some lines from the original test
        final String fileContent = RandomString.generateLetter(100);
        final ByteArrayInputStream bais = new ByteArrayInputStream(fileContent
            .getBytes(com.openexchange.java.Charsets.UTF_8));

        final String id = quotaStorage.saveNewFile(bais);

        assertEquals(fileContent.getBytes(com.openexchange.java.Charsets.UTF_8).length, quotaStorage.getUsage());
        assertEquals(fileContent.getBytes(com.openexchange.java.Charsets.UTF_8).length, quotaStorage.getFileSize(id));


        quotaStorage.deleteFile(id);

        assertEquals(0,quotaStorage.getUsage());
        rmdir(tempFile);
    }

    public void testFull() throws Exception{
        final File tempFile = File.createTempFile("filestorage", ".tmp");
        tempFile.deleteOnExit();

        tempFile.delete();

        fs = new LocalFileStorage(new URI("file://"+tempFile.getAbsolutePath()));
        final TestQuotaFileStorage quotaStorage = new TestQuotaFileStorage(new ContextImpl(1), fs, new DummyDatabaseService());
        quotaStorage.setQuota(10000);

        final String fileContent = RandomString.generateLetter(100);

        quotaStorage.setQuota(fileContent.getBytes(com.openexchange.java.Charsets.UTF_8).length-2);

        try {
            final ByteArrayInputStream bais = new ByteArrayInputStream(fileContent.getBytes(com.openexchange.java.Charsets.UTF_8));
            quotaStorage.saveNewFile(bais);
            fail("Managed to exceed quota");
        } catch (final OXException x) {
            assertTrue(true);
        }
        rmdir(tempFile);
    }

    public void testExclusiveLock() throws Exception{
        final File tempFile = File.createTempFile("filestorage", ".tmp");
        tempFile.deleteOnExit();

        tempFile.delete();

        fs = new LocalFileStorage(new URI("file://"+tempFile.getAbsolutePath()));
        final TestQuotaFileStorage quotaStorage = new TestQuotaFileStorage(new ContextImpl(1), fs, new DummyDatabaseService());
        quotaStorage.setQuota(10000);
        quotaStorage.setUsage(5000);

        final Thread[] threads = new Thread[100];
        for(int i = 0; i < threads.length; i++) {
            threads[i] = new AddAndRemoveThread(50,quotaStorage);
        }

        for(final Thread thread : threads) { thread.start(); }
        for(final Thread thread : threads) { thread.join(); }

        assertEquals(5000, quotaStorage.getUsage());
        rmdir(tempFile);
    }

    public void testConcurrentLock() throws Exception  {
        final File tempFile = File.createTempFile("filestorage", ".tmp");
        tempFile.deleteOnExit();

        tempFile.delete();

        fs = new LocalFileStorage(new URI("file://"+tempFile.getAbsolutePath()));
        final TestQuotaFileStorage quotaStorage = new TestQuotaFileStorage(new ContextImpl(1), fs, new DummyDatabaseService());
        quotaStorage.setQuota(10000);

        final int size = 1000;
        final int tests = 2;
        final long delay = 6000;

        quotaStorage.setQuota(size*tests);
        quotaStorage.setUsage(0);



        final SaveFileThread[] saveFiles = new SaveFileThread[tests];

        for(int i = 0; i < saveFiles.length; i++) {
            final DelayedInputStream is = new DelayedInputStream(new ByteArrayInputStream(new byte[size]), delay);
            saveFiles[i] = new SaveFileThread(is, quotaStorage);
            saveFiles[i].start();
        }


        for(int i = 0; i < saveFiles.length; i++) {
            saveFiles[i].join();
            if(saveFiles[i].getException() != null) {
                saveFiles[i].getException().printStackTrace();
                assertTrue(false);
            }
        }

        assertFalse(new File(tempFile,".lock").exists());
        rmdir(tempFile);
    }

    public static final class TestQuotaFileStorage extends DBQuotaFileStorage {

        public TestQuotaFileStorage(final Context ctx, final FileStorage fs, final DatabaseService dbs) throws OXException {
            super(ctx, fs, dbs);
        }

        private long usage;
        private long quota;

        public void setQuota(final long quota){
            this.quota = quota;
        }

        @Override
        public long getQuota() {
            return quota;
        }

        @Override
        public long getUsage() {
            return usage;
        }

        protected void setUsage(final long usage) {
            this.usage = usage;
        }

        @Override
        protected boolean incUsage(final long added) {
            boolean full = false;
            if (this.usage + added <= this.quota) {
                this.usage += added;
            } else {
                full = true;
            }
            return full;
        }

        @Override
        protected void decUsage(final long removed) {
            this.usage -= removed;
        }
    }

    private static final class AddAndRemoveThread extends Thread{
        private final int counter;
        private final FileStorage fs;
        private final byte[] bytes = new byte[10];
        private final Random r = new Random();

        public AddAndRemoveThread(final int counter, final FileStorage fs) {
            super();
            this.counter = counter;
            this.fs = fs;
        }

        @Override
        public void run() {
            for(int i = 0; i < counter; i++) {
                try {
                    final int w = r.nextInt(200);
                    final String id = fs.saveNewFile(new ByteArrayInputStream(bytes));
                    Thread.sleep(w);
                    fs.deleteFile(id);
                } catch (final Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static final class SaveFileThread extends Thread {
        private final InputStream data;
        private final FileStorage fs;
        private Exception exception;

        public SaveFileThread(final InputStream data, final FileStorage fs){
            this.data = data;
            this.fs = fs;
        }

        public Exception getException(){ return exception; }

        @Override
        public void run() {
            try {
                fs.saveNewFile(data);
            } catch (final OXException e) {
                exception = e;
            }
        }
    }

    static final class DummyDatabaseService implements DatabaseService {

        @Override
        public void back(final int poolId, final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backForUpdateTask(final int contextId, final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backReadOnly(final Context ctx, final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backReadOnly(final int contextId, final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backWritable(final Context ctx, final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backWritable(final int contextId, final Connection con) {
            // Nothing to do.
        }

        @Override
        public Connection get(final int poolId, final String schema) {
            return null;
        }

        @Override
        public int[] getContextsInSameSchema(final int contextId) {
            return null;
        }

        @Override
        public Connection getForUpdateTask(final int contextId) {
            return null;
        }

        @Override
        public Connection getReadOnly(final Context ctx) {
            return null;
        }

        @Override
        public Connection getReadOnly(final int contextId) {
            return null;
        }

        @Override
        public String getSchemaName(final int contextId) {
            return null;
        }

        @Override
        public Connection getWritable(final Context ctx) {
            return null;
        }

        @Override
        public Connection getWritable(final int contextId) {
            return null;
        }

        @Override
        public int getWritablePool(final int contextId) {
            return 0;
        }

        @Override
        public void invalidate(final int contextId) {
            // Nothing to do.
        }

        @Override
        public void backReadOnly(final Connection con) {
            // Nothing to do.
        }

        @Override
        public void backWritable(final Connection con) {
            // Nothing to do.
        }

        @Override
        public Connection getReadOnly() {
            return null;
        }

        @Override
        public Connection getWritable() {
            return null;
        }

        @Override
        public int[] listContexts(final int poolId) {
            return null;
        }

        @Override
        public Connection getNoTimeout(final int poolId, final String schema) {
            return null;
        }

        @Override
        public void backNoTimeoout(final int poolId, final Connection con) {
            // Nothing to do
        }

        @Override
        public int getServerId() {
            return 0;
        }
    }

    private static void rmdir(final File tempFile) {
        if (tempFile.isDirectory()) {
            for (final File f : tempFile.listFiles()) {
                rmdir(f);
            }
        }
        tempFile.delete();
    }
}
