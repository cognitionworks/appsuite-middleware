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
 *     Copyright (C) 2004-2006 Open-Xchange, Inc.
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

package com.openexchange.groupware.container;

/**
 * {@link ExternalUserParticipant} - Represents an external user participant.
 * 
 * @author <a href="mailto:sebastian.kauss@open-xchange.com">Sebastian Kauss</a>
 */
public class ExternalUserParticipant implements Participant, Comparable<Participant> {

    private int id = NO_ID;

    private String displayName;

    private String emailaddress;

    private boolean ignoreNotification;

    /**
     * Default constructor.
     * 
     * @param emailAddress The unique email address of the external participant.
     */
    public ExternalUserParticipant(final String emailAddress) {
        super();
        emailaddress = emailAddress;
    }

    /**
     * @deprecated Use {@link #ExternalUserParticipant(String)}.
     */
    @Deprecated
    public ExternalUserParticipant() {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public void setIdentifier(final int id) {
        this.id = id;
    }

    /**
     * {@inheritDoc}
     */
    public int getIdentifier() {
        return id;
    }

    /**
     * {@inheritDoc}
     */
    public void setDisplayName(final String displayName) {
        this.displayName = displayName;
    }

    /**
     * {@inheritDoc}
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * {@inheritDoc}
     */
    public String getEmailAddress() {
        return emailaddress;
    }

    /**
     * {@inheritDoc}
     */
    public void setEmailAddress(final String emailaddress) {
        this.emailaddress = emailaddress;
    }

    /**
     * {@inheritDoc}
     */
    public int getType() {
        return EXTERNAL_USER;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + EXTERNAL_USER;
        result = prime * result + id;
        result = prime * result + ((emailaddress == null) ? 0 : emailaddress.hashCode());
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof ExternalUserParticipant)) {
            return false;
        }
        final ExternalUserParticipant other = (ExternalUserParticipant) obj;
        if (emailaddress == null) {
            if (other.emailaddress != null) {
                return false;
            }
        } else if (!emailaddress.equals(other.emailaddress)) {
            return false;
        }
        if (id != other.id) {
            return false;
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public int compareTo(final Participant part) {
        final int retval;
        if (EXTERNAL_USER == part.getType()) {
            if (null == emailaddress) {
                if (null == part.getEmailAddress()) {
                    retval = 0;
                } else {
                    retval = -1;
                }
            } else {
                if (null == part.getEmailAddress()) {
                    retval = 1;
                } else {
                    retval = emailaddress.compareTo(part.getEmailAddress());
                }
            }
        } else {
            retval = Integer.valueOf(EXTERNAL_USER).compareTo(Integer.valueOf(part.getType()));
        }
        return retval;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("ExternalUserParticipant,mail:");
        sb.append(emailaddress);
        return sb.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

    public Participant getClone() throws CloneNotSupportedException {
        return (Participant) clone();
    }

    public boolean isIgnoreNotification() {
        return ignoreNotification;
    }

    public void setIgnoreNotification(final boolean ignoreNotification) {
        this.ignoreNotification = ignoreNotification;
    }
}
