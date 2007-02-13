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

package com.openexchange.groupware.search;

import java.util.Date;

public class ContactSearchObject extends SearchObject {
	
	private String surname = null;
	
	private String displayName = null;
	
	private String givenname = null;
	
	private String company = null;
	
	private int[] dynamicSearchField = null;
	
	private String[] dynamicSearchFieldValue = null;
	
	private String[] privatePostalCodeRange = null;
	
	private String[] businessPostalCodeRange = null;
	
	private String[] otherPostalCodeRange = null;
	
	private Date[] birthdayRange = null;
	
	private Date[] anniversaryRange = null;
	
	private String[] numberOfEmployeesRange = null;
	
	private String[] salesVolumeRange = null;
	
	private Date[] creationDateRange = null;
	
	private Date[] lastModifiedRange = null;
	
	private String allFolderSQLINString = null;

	public ContactSearchObject() {
		super();
	}

	public Date[] getAnniversaryRange() {
		return anniversaryRange;
	}

	public void setAnniversaryRange(Date[] anniversaryRange) {
		this.anniversaryRange = anniversaryRange;
	}

	public Date[] getBirthdayRange() {
		return birthdayRange;
	}

	public void setBirthdayRange(Date[] birthdayRange) {
		this.birthdayRange = birthdayRange;
	}

	public String[] getBusinessPostalCodeRange() {
		return businessPostalCodeRange;
	}

	public void setBusinessPostalCodeRange(String[] businessPostalCodeRange) {
		this.businessPostalCodeRange = businessPostalCodeRange;
	}

	public String getCompany() {
		return company;
	}

	public void setCompany(String company) {
		this.company = company;
	}

	public Date[] getCreationDateRange() {
		return creationDateRange;
	}

	public void setCreationDateRange(Date[] creationDateRange) {
		this.creationDateRange = creationDateRange;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public int[] getDynamicSearchField() {
		return dynamicSearchField;
	}

	public void setDynamicSearchField(int[] dynamicSearchField) {
		this.dynamicSearchField = dynamicSearchField;
	}

	public String[] getDynamicSearchFieldValue() {
		return dynamicSearchFieldValue;
	}

	public void setDynamicSearchFieldValue(String[] dynamicSearchFieldValue) {
		this.dynamicSearchFieldValue = dynamicSearchFieldValue;
	}

	public String getGivenName() {
		return givenname;
	}
	
	public String getAllFolderSQLINString(){
		return allFolderSQLINString;
	}

	public void setAllFolderSQLINString(String allFolderSQLINString){
		this.allFolderSQLINString = allFolderSQLINString;
	}
	
	public void setGivenName(String forename) {
		this.givenname = forename;
	}

	public Date[] getLastModifiedRange() {
		return lastModifiedRange;
	}

	public void setLastModifiedRange(Date[] lastModifiedRange) {
		this.lastModifiedRange = lastModifiedRange;
	}

	public String[] getNumberOfEmployeesRange() {
		return numberOfEmployeesRange;
	}

	public void setNumberOfEmployeesRange(String[] numberOfEmployeesRange) {
		this.numberOfEmployeesRange = numberOfEmployeesRange;
	}

	public String[] getOtherPostalCodeRange() {
		return otherPostalCodeRange;
	}

	public void setOtherPostalCodeRange(String[] otherPostalCodeRange) {
		this.otherPostalCodeRange = otherPostalCodeRange;
	}

	public String[] getPrivatePostalCodeRange() {
		return privatePostalCodeRange;
	}

	public void setPrivatePostalCodeRange(String[] privatePostalCodeRange) {
		this.privatePostalCodeRange = privatePostalCodeRange;
	}

	public String[] getSalesVolumeRange() {
		return salesVolumeRange;
	}

	public void setSalesVolumeRange(String[] salesVolumeRange) {
		this.salesVolumeRange = salesVolumeRange;
	}

	public String getSurname() {
		return surname;
	}

	public void setSurname(String surname) {
		this.surname = surname;
	}
}
