package com.openexchange.calendar.itip;

import com.openexchange.api2.AppointmentSQLInterface;
import com.openexchange.calendar.api.CalendarFeature;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.server.ServiceLookup;
import com.openexchange.session.Session;
import com.openexchange.user.UserService;

public class ITipFeature implements CalendarFeature {

	private static final String ITIP = "itip";

	private ServiceLookup services;
	
	
	public ITipFeature(ServiceLookup services) {
		super();
		this.services = services;
	}



	public String getId() {
		return ITIP;
	}

	
	
	public AppointmentSQLInterface wrap(AppointmentSQLInterface delegate,
			Session session) throws OXException {
		
		return new ITipConsistencyCalendar(delegate, session, services);
	}

}
