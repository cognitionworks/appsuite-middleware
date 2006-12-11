package com.openexchange.groupware.attach.actions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.openexchange.groupware.attach.AttachmentListener;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.attach.impl.FireAttachedEventAction;
import com.openexchange.groupware.tx.UndoableAction;

public class FireAttachedEventActionTest extends AbstractAttachmentEventActionTest {

	private MockAttachmentListener listener = new MockAttachmentListener();
	
	@Override
	protected UndoableAction getAction() throws Exception {
		FireAttachedEventAction fireAttached = new FireAttachedEventAction();
		fireAttached.setAttachments(getAttachments());
		fireAttached.setContext(getContext());
		fireAttached.setUser(getUser());
		fireAttached.setUserConfiguration(null);
		fireAttached.setWriteConnection(null);
		List<AttachmentListener> listeners = new ArrayList<AttachmentListener>();
		listeners.add(listener);
		fireAttached.setAttachmentListeners(listeners);
		fireAttached.setSource(getAttachmentBase());
		return fireAttached;
	}

	@Override
	protected void verifyPerformed() throws Exception {
		List<AttachmentMetadata> m = listener.getAttached();
		Map<Integer, AttachmentMetadata> attachmentMap = new HashMap<Integer, AttachmentMetadata>();
		Set<AttachmentMetadata> attachmentSet = new HashSet<AttachmentMetadata>();
		
		for(AttachmentMetadata att : getAttachments()) {
			attachmentMap.put(att.getId(),att);
			attachmentSet.add(att);
		}
		
		for(AttachmentMetadata attached : m) {
			AttachmentMetadata orig = attachmentMap.get(attached.getId());
			assertEquals(orig, attached);
			assertTrue(attachmentSet.remove(attached));
		}
		assertTrue(attachmentSet.isEmpty());
		
		listener.clear();
	}

	@Override
	protected void verifyUndone() throws Exception {
		Set<Integer> ids = new HashSet<Integer>();
		for(AttachmentMetadata m : getAttachments()) {
			ids.add(m.getId());
		}
		for(int id : listener.getDetached()) {
			assertTrue(ids.remove(id));
		}
		assertTrue(ids.isEmpty());
	}	

}
