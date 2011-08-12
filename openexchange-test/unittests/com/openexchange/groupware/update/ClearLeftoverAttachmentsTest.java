package com.openexchange.groupware.update;

import com.openexchange.exception.OXException;
import java.io.ByteArrayInputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.openexchange.groupware.attach.AttachmentBase;
import com.openexchange.groupware.attach.impl.AttachmentBaseImpl;
import com.openexchange.groupware.attach.impl.AttachmentImpl;
import com.openexchange.groupware.filestore.FilestoreStorage;
import com.openexchange.groupware.update.tasks.ClearLeftoverAttachmentsUpdateTask;
import com.openexchange.tools.file.FileStorage;

public class ClearLeftoverAttachmentsTest extends UpdateTest {
    private AttachmentBase attachmentBase;

    private static final int OFFSET = 3;

    private final List<AttachmentImpl> attachments = new ArrayList<AttachmentImpl>();

    @Override
	public void setUp() throws Exception {
        super.setUp();

        attachmentBase = new AttachmentBaseImpl(getProvider());
        attachmentBase.setTransactional(true);
        attachmentBase.startTransaction();

    }

    @Override
	public void tearDown() throws Exception {
        for(final AttachmentImpl attachment : attachments) {
            try {
                attachmentBase.detachFromObject(22,22,22,new int[]{attachment.getId()}, session, ctx, user, null);
            } catch (final OXException x) {}
        }
        super.tearDown();
    }

    public void testFixSchema() throws OXException, SQLException {
        createAttachments();
        resetSequenceCounter();
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
        assertNoLeftoversInDatabase();
        assertRemovedFiles();
    }

    public void testRunMultipleTimesNonDestructively() throws OXException, SQLException {
        createAttachments();
        resetSequenceCounter();
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
    }

    public void testIgnoreMissingFiles() throws OXException, SQLException {
        createAttachments();
        resetSequenceCounter();
        removeSomeFiles();
        new ClearLeftoverAttachmentsUpdateTask().perform(schema, existing_ctx_id);
        assertNoLeftoversInDatabase();
        assertRemovedFiles();
    }

    private void createAttachments() throws OXException {
        final AttachmentImpl original  = new AttachmentImpl();
        original.setAttachedId(22);
        original.setComment("");
        original.setCreatedBy(user_id);
        original.setCreationDate(new Date());
        original.setFolderId(22);
        original.setModuleId(22);
        original.setFileMIMEType("text/plain");
        original.setFilename("blupp.txt");

        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);
        createCopy(original);


    }

    private void createCopy(final AttachmentImpl original) throws OXException {
        final AttachmentImpl copy = new AttachmentImpl(original);
        attachmentBase.attachToObject(copy,new ByteArrayInputStream(new byte[10]),session,ctx,user,null);
        attachments.add(copy);
    }

    private void resetSequenceCounter() throws SQLException, OXException {
        exec("UPDATE sequence_attachment SET id = ? WHERE cid = ?",attachments.get(OFFSET).getId(), existing_ctx_id);
    }

    private void removeSomeFiles() throws OXException, OXException {
        final FileStorage fs = FileStorage.getInstance(FilestoreStorage.createURI(ctx));
        fs.deleteFile(attachments.get(OFFSET+2).getFileId());
        fs.deleteFile(attachments.get(OFFSET+3).getFileId());
    }

    private void assertNoLeftoversInDatabase() throws SQLException, OXException {
        assertNoResults("SELECT 1 FROM prg_attachment JOIN sequence_attachment ON prg_attachment.cid = sequence_attachment.cid WHERE prg_attachment.id > sequence_attachment.id AND prg_attachment.cid = ?",existing_ctx_id);
    }

    private void assertRemovedFiles() throws OXException, OXException {
        final FileStorage fs = FileStorage.getInstance(FilestoreStorage.createURI(ctx));
        for(int i = OFFSET+1; i < attachments.size(); i++) {
            try {
                fs.getFile(attachments.get(i).getFileId());
                assertFalse("File of attachment "+i+" was not deleted",true);
            } catch (final OXException x) {
                assertTrue(true); // Specific enough?
            }
        }
    }
}
