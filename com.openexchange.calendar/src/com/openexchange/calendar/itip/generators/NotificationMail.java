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
 *     Copyright (C) 2004-2014 Open-Xchange, Inc.
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

package com.openexchange.calendar.itip.generators;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.openexchange.ajax.fields.AppointmentFields;
import com.openexchange.calendar.AppointmentDiff;
import com.openexchange.calendar.AppointmentDiff.FieldUpdate;
import com.openexchange.calendar.CalendarField;
import com.openexchange.calendar.itip.ITipRole;
import com.openexchange.data.conversion.ical.itip.ITipMessage;
import com.openexchange.data.conversion.ical.itip.ITipMethod;
import com.openexchange.groupware.attach.AttachmentMetadata;
import com.openexchange.groupware.container.Appointment;
import com.openexchange.groupware.container.CalendarObject;
import com.openexchange.groupware.container.Participant;
import com.openexchange.groupware.notify.State.Type;


/**
 * {@link NotificationMail}
 *
 * @author <a href="mailto:francisco.laguna@open-xchange.com">Francisco Laguna</a>
 */
public class NotificationMail {
    private ITipMessage itipMessage;

    private String templateName;
    private String text;
    private String html;
    private String subject;

    private Appointment original;
    private Appointment appointment;
    private AppointmentDiff diff;


    private NotificationParticipant sender;
    private NotificationParticipant recipient;
    private NotificationParticipant organizer;
    private NotificationParticipant principal;
    private NotificationParticipant sharedCalendarOwner;

    private List<NotificationParticipant> participants;
    private List<NotificationParticipant> resources;

    private NotificationParticipant actor;

    private final List<AttachmentMetadata> attachments = new ArrayList<AttachmentMetadata>();

	private Type stateType;

	private boolean attachmentUpdate;

	private boolean sortedParticipants;
	
	private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(NotificationMail.class);

    public ITipMessage getMessage() {
        return itipMessage;
    }

    public void setMessage(ITipMessage itipMessage) {
        this.itipMessage = itipMessage;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getHtml() {
        return html;
    }

    public void setHtml(String html) {
        this.html = html;
    }

    public Appointment getOriginal() {
        return original;
    }

    public void setOriginal(Appointment original) {
        this.original = original;
    }

    public Appointment getAppointment() {
        return appointment;
    }

    public void setAppointment(Appointment updated) {
        this.appointment = updated;
    }

    public AppointmentDiff getDiff() {
        if (diff == null && original != null && appointment != null) {
            diff = AppointmentDiff.compare(original, appointment, NotificationMailGenerator.DEFAULT_SKIP);
        }
        return diff;
    }

    public NotificationParticipant getRecipient() {
        return recipient;
    }

    public void setRecipient(NotificationParticipant recipient) {
        this.recipient = recipient;
    }

    public void setSender(NotificationParticipant sender) {
        this.sender = sender;
    }

    public NotificationParticipant getSender() {
        return sender;
    }

    public String getTemplateName() {
        return templateName;
    }

    public void setTemplateName(String templateName) {
        this.templateName = templateName;
    }


    public NotificationParticipant getOrganizer() {
        return organizer;
    }


    public void setOrganizer(NotificationParticipant organizer) {
        this.organizer = organizer;
    }

    public NotificationParticipant getPrincipal() {
    	if (principal == null) {
    		return principal = organizer;
    	}
		return principal;
	}

	public void setPrincipal(NotificationParticipant principal) {
		this.principal = principal;
	}

	public NotificationParticipant getOnBehalfOf() {
		if (isAboutActorsStateChangeOnly()) {
			return actor;
		}

		if (sharedCalendarOwner != null) {
			return sharedCalendarOwner;
		}

		return sender;
	}

	public NotificationParticipant getSharedCalendarOwner() {
		return sharedCalendarOwner;
	}

	public void setSharedCalendarOwner(NotificationParticipant sharedCalendarOwner) {
		this.sharedCalendarOwner = sharedCalendarOwner;
	}

	public boolean actionIsDoneOnBehalfOfAnother() {
		if (getActor().hasRole(ITipRole.PRINCIPAL)) {
			return false;
		}
		return !getActor().equals(getOnBehalfOf());
	}

	public boolean actionIsDoneOnMyBehalf() {
		if (isAboutActorsStateChangeOnly()) {
			return false;
		}
		if (actor.hasRole(ITipRole.PRINCIPAL)) {
			return false;
		}

		return recipient.equals(principal) ||recipient.equals(sharedCalendarOwner);
	}

	public void setParticipants(List<NotificationParticipant> recipients) {
		sortedParticipants = false;
        this.participants = recipients;
    }

    public List<NotificationParticipant> getParticipants() {
    	if (!sortedParticipants) {
    		Collections.sort(participants, new Comparator<NotificationParticipant>() {

				@Override
                public int compare(NotificationParticipant p1,
						NotificationParticipant p2) {
					return p1.getDisplayName().compareTo(p2.getDisplayName());
				}

    		});
    	}
    	return participants;
    }

    public void setResources(List<NotificationParticipant> resources) {
        this.resources = resources;
    }

    public List<NotificationParticipant> getResources() {
        return resources;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setAttachmentUpdate(boolean attachmentUpdate) {
		this.attachmentUpdate = attachmentUpdate;
	}

    public boolean isAttachmentUpdate() {
		return attachmentUpdate;
	}

    public boolean _shouldBeSent() {
    	if (endsInPast(appointment)) {
    		return false;
    	}
        LOG.debug("1: User: {}, {} {}", recipient.getEmail(), recipient.getConfiguration().forceCancelMails(), isCancelMail());
    	if (recipient.getConfiguration().forceCancelMails() && isCancelMail()) {
    		return true;
    	}
    	if (appointment != null) {
            LOG.debug("2: User: {}, {} {}", recipient.getEmail(), appointment.containsNotification(), appointment.getNotification());
        	if (appointment.containsNotification() && !appointment.getNotification()) {
        		return false;
        	}
    	}
    	if (appointment != null) {
            LOG.debug("3: User: {}, {} {}", recipient.getEmail(), stateType.name(), endsInPast(appointment));
        	if (stateType.equals(Type.NEW) && endsInPast(appointment)) {
        	    return false;
        	}
    	}
    	if (appointment != null && original != null) {
            LOG.debug("4: User: {}, {} {}", recipient.getEmail(), stateType.name(), isNotWorthUpdateNotification(original, appointment));
        	if (stateType.equals(Type.MODIFIED)  && isNotWorthUpdateNotification(original, appointment)) {
        	    return false;
        	}
    	}
        LOG.debug("5: User: {}, {}", recipient.getEmail(), stateType.name());
    	if (appointment != null && stateType.equals(Type.DELETED)) {
    	    return false;
    	}
        LOG.debug("6: User: {}, {}", recipient.getEmail(), anInterestingFieldChanged());
    	if (! anInterestingFieldChanged()) {
    		return false;
    	}
        LOG.debug("7: User: {}, {} {}", recipient.getEmail(), stateType.name(), onlyPseudoChangesOnParticipants());
        if (stateType == Type.MODIFIED && onlyPseudoChangesOnParticipants()) {
            return false;
        }
        LOG.debug("8: User: {}, {}", recipient.getEmail(), getRecipient().getConfiguration().sendITIP());
        if (getRecipient().getConfiguration().sendITIP() && itipMessage != null) {
            return true;
        }
        LOG.debug("9: User: {}, {}", recipient.getEmail(), getRecipient().getConfiguration().interestedInChanges());
        if (!getRecipient().getConfiguration().interestedInChanges()) {
            return false;
        }
        LOG.debug("10: User: {}, {} {}", recipient.getEmail(), getRecipient().getConfiguration().interestedInStateChanges(), isAboutStateChangesOnly(true));
        if (!getRecipient().getConfiguration().interestedInStateChanges() && isAboutStateChangesOnly(true)) {
            return false;
        }
        LOG.debug("11: User: {}", recipient.getEmail());
        return true;
    }

    public boolean shouldBeSent() {
        if (endsInPast(appointment)) {
            return false;
        }
        if (recipient.getConfiguration().forceCancelMails() && isCancelMail()) {
            return true;
        }
        if (appointment != null && appointment.containsNotification() && !appointment.getNotification()) {
            return false;
        }
        if (appointment != null && stateType.equals(Type.NEW) && endsInPast(appointment)) {
            return false;
        }
        if (appointment != null && original != null && stateType.equals(Type.MODIFIED)
                                && isNotWorthUpdateNotification(original, appointment)) {
            return false;
        }
        if (appointment != null && stateType.equals(Type.DELETED)) {
            return false;
        }
        if (! anInterestingFieldChanged()) {
            return false;
        }
        if (stateType == Type.MODIFIED && onlyPseudoChangesOnParticipants()) {
            return false;
        }
        if (getRecipient().getConfiguration().sendITIP() && itipMessage != null) {
            return true;
        }
        if (!getRecipient().getConfiguration().interestedInChanges()) {
            return false;
        }
        if (!getRecipient().getConfiguration().interestedInStateChanges() && isAboutStateChangesOnly(true)) {
            return false;
        }
        return true;
    }

    private boolean onlyPseudoChangesOnParticipants() {
        AppointmentDiff appDiff = getDiff();
        if (appDiff == null) {
            return false;
        }
        boolean onlyParticipantsChanged = appDiff.exactlyTheseChanged(CalendarField.getByColumn(CalendarObject.PARTICIPANTS).getJsonName());
        if (onlyParticipantsChanged) {
            FieldUpdate participantUpdate = appDiff.getUpdateFor(CalendarField.getByColumn(CalendarObject.PARTICIPANTS).getJsonName());
            Participant[] oldParticipants = (Participant[]) participantUpdate.getOriginalValue();
            Participant[] newParticipants = (Participant[]) participantUpdate.getNewValue();

            for (Participant pOld : oldParticipants) {
                if (pOld.getType() == Participant.RESOURCE) {
                    boolean found = false;
                    for (Participant pNew : newParticipants) {
                        if (pNew.getType() == Participant.RESOURCE && pNew.getIdentifier() == pOld.getIdentifier()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }

            for (Participant pNew : newParticipants) {
                if (pNew.getType() == Participant.RESOURCE) {
                    boolean found = false;
                    for (Participant pOld : oldParticipants) {
                        if (pOld.getType() == Participant.RESOURCE && pOld.getIdentifier() == pNew.getIdentifier()) {
                            found = true;
                            break;
                        }
                    }
                    if (!found) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    private boolean isNotWorthUpdateNotification(final Appointment original, final Appointment modified) {
        if (original.containsRecurrenceType() && original.getRecurrenceType() != CalendarObject.NO_RECURRENCE) {
            if (endsInPast(original)) {
                return endsInPast(modified);
            } else {
                if (modified.isException()) {
                    return endsInPast(modified);
                } else {
                    return false;
                }
            }
        } else {
            if (endsInPast(original)) {
                return endsInPast(modified);
            } else {
                return false;
            }
        }
    }

    private boolean endsInPast(final Appointment appointment) {
        final Date now = new Date();
        Date endDate;
        Date until;
        if (original == null || appointment.getEndDate().after(original.getEndDate())) {
            endDate = appointment.getEndDate();
        } else {
            endDate = original.getEndDate();
        }

        if (original == null || appointment.getUntil().after(original.getUntil())) {
            until = appointment.getUntil();
        } else {
            until = original.getUntil();
        }

        if (appointment.isException() || appointment.getRecurrenceType() == CalendarObject.NO_RECURRENCE) {
            return endDate.before(now);
        } else {
            if (until == null) {
                return false;
            }

            return until.before(now);
        }
    }

	private boolean isCancelMail() {
		return itipMessage != null && itipMessage.getMethod() == ITipMethod.CANCEL;
	}


	private static final Set<String> FIELDS_TO_REPORT = new HashSet<String>(Arrays.asList(
			AppointmentFields.LOCATION,
			AppointmentFields.FULL_TIME,
			AppointmentFields.TIMEZONE,
			AppointmentFields.RECURRENCE_START,
			AppointmentFields.TITLE,
			AppointmentFields.START_DATE,
			AppointmentFields.END_DATE,
			AppointmentFields.NOTE,
			AppointmentFields.RECURRENCE_DATE_POSITION,
			AppointmentFields.RECURRENCE_POSITION,
			AppointmentFields.RECURRENCE_TYPE,
			AppointmentFields.DAYS,
			AppointmentFields.DAY_IN_MONTH,
			AppointmentFields.MONTH,
			AppointmentFields.INTERVAL,
			AppointmentFields.UNTIL,
			AppointmentFields.RECURRENCE_CALCULATOR,
			AppointmentFields.PARTICIPANTS,
			AppointmentFields.USERS,
			AppointmentFields.CONFIRMATIONS
		));

    private boolean anInterestingFieldChanged() {
    	if (getDiff() == null) {
    		return true;
    	}
    	if (isAttachmentUpdate()) {
    		return true;
    	}

    	return getDiff().anyFieldChangedOf(FIELDS_TO_REPORT);
    }

	public boolean isAboutStateChangesOnly(boolean log) {
        if (getDiff() == null) {
            if (log) {
                LOG.debug("12: User: {}", recipient.getEmail());
            }
            return false;
        }

        if (isAttachmentUpdate()) {
            if (log) {
                LOG.debug("13: User: {}", recipient.getEmail());
            }
        	return false;
        }

        if (log) {
            LOG.debug("14: User: {} {}", recipient.getEmail(), diff.getDifferingFieldNames());
        }
        return diff.isAboutStateChangesOnly();
    }

	public boolean isAboutActorsStateChangeOnly() {
    	if (!isAboutStateChangesOnly(false)) {
    		return false;
    	}
		return diff.isAboutCertainParticipantsStateChangeOnly(actor.getIdentifier()+"");
	}

	public boolean someoneElseChangedPrincipalsState() {
		if (actor.getIdentifier() == getPrincipal().getIdentifier()) {
			return false;
		}
		return diff.isAboutCertainParticipantsStateChangeOnly(getPrincipal().getIdentifier()+"");
	}

    private boolean isAboutRecipientsStateChangeOnly() {
    	if (!isAboutStateChangesOnly(false)) {
    		return false;
    	}
		return diff.isAboutCertainParticipantsStateChangeOnly(recipient.getEmail());
	}

    public void setActor(NotificationParticipant actor) {
        this.actor = actor;
    }

    public NotificationParticipant getActor() {
        return actor;
    }

	public Type getStateType() {
		return stateType;
	}

	public void setStateType(Type stateType) {
		this.stateType = stateType;
	}

    public void addAttachment(AttachmentMetadata attachment) {
    	attachments.add(attachment);
    }

    public List<AttachmentMetadata> getAttachments() {
		return attachments;
	}


}
