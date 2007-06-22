

package com.openexchange.admin.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Filestore;
import com.openexchange.admin.rmi.exceptions.ProgrammErrorException;
import com.openexchange.admin.rmi.exceptions.StorageException;
import com.openexchange.admin.storage.interfaces.OXContextStorageInterface;
import com.openexchange.admin.tools.ShellExecutor.ArrayOutput;

/**
 * 
 * @author d7
 * 
 */
public class FilestoreDataMover implements Callable<Void> {

    private final static Log log = LogFactory.getLog(FilestoreDataMover.class);

    private String src = null;

    private String dst = null;

    private Context ctx = null;

    private Filestore dstStore = null;

    /**
     * @throws IOException
     * 
     */
    public FilestoreDataMover(final String src, final String dst, final Context ctx, final Filestore dstStore) throws IOException {
        if (!new File(this.src).exists()) {
            throw new IOException("Source does not exist");
        }
        this.src = src;
        this.dst = dst;
        this.ctx = ctx;
        this.dstStore = dstStore;
    }

    /**
     * 
     * get Size as long (bytes) from the source dir
     * 
     * @param source
     * @return
     */
    public long getSize(final String source) {
        return FileUtils.sizeOfDirectory(new File(source));
    }

    /**
     * 
     * get the list of files to copy from the source dir
     * 
     * @param source
     * @return
     */
    @SuppressWarnings("unchecked")
    public ArrayList<String> getFileList(final String source) {
        final Collection<File> listFiles = FileUtils.listFiles(new File(source), null, true);
        final ArrayList<String> retval = new ArrayList<String>();
        for (final File file : listFiles) {
            retval.add(new StringBuilder(file.getPath()).append(File.pathSeparatorChar).append(file.getName()).toString());
        }
        return retval;
    }

    /**
     * start the copy (rsync)
     * 
     * @throws StorageException
     * @throws InterruptedException
     * @throws IOException
     * @throws ProgrammErrorException
     */
    public void copy() throws StorageException, IOException, InterruptedException, ProgrammErrorException {
        // if context store does not yet exist, which might be possible,
        // just change the configdb
        final ArrayOutput output = new ShellExecutor().executeprocargs(new String[] { "/opt/open-xchange/libexec/sync.sh", this.src, this.dst });
        if (0 != output.exitstatus) {
            throw new ProgrammErrorException("Wrong exit status. Exit status was: " + output.exitstatus + " Stderr was: \n" + output.errOutput.toString() + '\n' + "and stdout was: \n" + output.stdOutput.toString());
        }
        FileUtils.deleteDirectory(new File(this.src));

        final OXContextStorageInterface oxcox = OXContextStorageInterface.getInstance();
        oxcox.changeStorageData(ctx, dstStore);
        oxcox.enable(ctx);
    }

    /**
     * starting the thread
     * 
     * @throws StorageException
     * @throws InterruptedException
     * @throws IOException
     * @throws ProgrammErrorException
     * 
     */
    public Void call() throws StorageException, IOException, InterruptedException, ProgrammErrorException {
        try {
            copy();
        } catch (final StorageException e) {
            log.error(e);
            throw e;
        } catch (final IOException e) {
            log.error(e);
            throw e;
        } catch (final InterruptedException e) {
            log.error(e);
            throw e;
        } catch (final ProgrammErrorException e) {
            log.error(e);
            throw e;
        }
        return null;
    }
}