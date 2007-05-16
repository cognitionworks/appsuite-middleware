package com.openexchange.webdav.action;

import com.openexchange.groupware.infostore.utils.InfostoreConfigUtils;
import com.openexchange.sessiond.SessionHolder;

public class OXWebdavPutAction extends WebdavPutAction {
	
	private SessionHolder sessionHolder;

	@Override
	public long getMaxSize() {
		long maxSize = InfostoreConfigUtils.determineRelevantUploadSizePerFile(sessionHolder.getSessionObject().getUserConfiguration());
		System.out.println("================\n\n\t\t"+maxSize+"\n\n==================");
		if(maxSize < 1) {
			return -1;
		}
		
		return maxSize;
	}
	
	public void setSessionHolder(SessionHolder holder){
		this.sessionHolder = holder;
	}
}
