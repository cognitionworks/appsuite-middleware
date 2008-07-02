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



package com.openexchange.tools.versit.valuedefinitions.rfc2425;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Pattern;

import com.openexchange.tools.versit.Property;
import com.openexchange.tools.versit.StringScanner;
import com.openexchange.tools.versit.ValueDefinition;
import com.openexchange.tools.versit.VersitException;
import com.openexchange.tools.versit.values.DateTimeValue;

/**
 * @author Viktor Pracht
 */
public class DateTimeValueDefinition extends ValueDefinition {

	public static final ValueDefinition Default = new DateTimeValueDefinition();

	private static Pattern TZPattern = Pattern.compile("[-+]\\d{2}:?\\d{2}");

	@Override
	public Object createValue(final StringScanner s, final Property property)
			throws IOException {
		final DateTimeValue date = new DateTimeValue();
		parseDate(s, date);
		if (s.peek != 'T') {
			throw new VersitException(s, "Date and time expected");
		}
		s.read();
		parseTime(s, date);
		return date;
	}

	protected void parseDate(final StringScanner s, final DateTimeValue date)
			throws IOException {
		date.calendar.set(Calendar.YEAR, s.parseNumber(4));
		if (s.peek == '-') {
			s.read();
		}
		date.calendar.set(Calendar.MONTH, s.parseNumber(2) - 1);
		if (s.peek == '-') {
			s.read();
		}
		date.calendar.set(Calendar.DATE, s.parseNumber(2));
	}

	protected void parseTime(final StringScanner s, final DateTimeValue date)
			throws IOException {
		date.calendar.set(Calendar.HOUR_OF_DAY, s.parseNumber(2));
		skipColon(s);
		date.calendar.set(Calendar.MINUTE, s.parseNumber(2));
		skipColon(s);
		date.calendar.set(Calendar.SECOND, s.parseNumber(2));
		if (s.peek == '.') {
			s.read();
			int ms = 0;
			for (int scale = 100; s.peek >= '0' && s.peek <= '9' && scale >= 1; scale /= 10) {
				ms += scale * (s.read() - '0');
			}
			date.calendar.set(Calendar.MILLISECOND, ms);
			while (s.peek >= '0' && s.peek <= '9') {
				s.read();
			}
		}
		final StringBuilder tz = new StringBuilder("GMT");
		if (s.peek == 'Z') {
			s.read();
		} else {
			final String offs = s.regex(TZPattern);
			if (offs == null) {
				date.isFloating = true;
			} else {
				tz.append(offs);
				date.isUTC = false;
			}
		}
		date.calendar.setTimeZone(TimeZone.getTimeZone(tz.toString()));
	}

	private void skipColon(final StringScanner s) throws IOException {
		if (s.peek == '\\') {
			s.read();
		}
		if (s.peek == ':') {
			s.read();
		}
	}

	@Override
	public String writeValue(final Object value) {
		final DateTimeValue dtval = (DateTimeValue) value;
		final StringBuilder sb = new StringBuilder();
		if (dtval.hasDate) {
			sb.append(writeDate(dtval));
		}
		if (dtval.hasTime) {
			sb.append('T');
			sb.append(writeTime(dtval));
		}
		return sb.toString();
	}

	private static final DecimalFormat YearFormat = new DecimalFormat("0000");

	private static final DecimalFormat Format = new DecimalFormat("00");

	private static final DecimalFormat MSFormat = new DecimalFormat("000");

	protected String writeDate(final DateTimeValue dtval) {
		return YearFormat.format(dtval.calendar.get(Calendar.YEAR))
				+ Format.format(dtval.calendar.get(Calendar.MONTH) + 1)
				+ Format.format(dtval.calendar.get(Calendar.DATE));
	}

	protected String writeTime(final DateTimeValue dtval) {
		final StringBuilder sb = new StringBuilder();
		sb.append(Format.format(dtval.calendar.get(Calendar.HOUR_OF_DAY)));
		sb.append(Format.format(dtval.calendar.get(Calendar.MINUTE)));
		sb.append(Format.format(dtval.calendar.get(Calendar.SECOND)));
		final int ms = dtval.calendar.get(Calendar.MILLISECOND);
		if (ms != 0) {
			sb.append('.');
			sb.append(MSFormat.format(ms));
		}
		if (!dtval.isFloating) {
			final int offset = dtval.calendar.getTimeZone().getRawOffset();
			if (dtval.isUTC || offset == 0) {
				sb.append('Z');
			} else {
				sb.append(offset > 0 ? '+' : '-');
				sb.append(Format.format(offset / 3600000));
				sb.append(Format.format(offset / 60000 % 60));
			}
		}
		return sb.toString();
	}

}
