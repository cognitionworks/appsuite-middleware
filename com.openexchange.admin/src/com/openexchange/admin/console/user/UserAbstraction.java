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
 *     Copyright (C) 2004-2010 Open-Xchange, Inc.
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

package com.openexchange.admin.console.user;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TimeZone;
import com.openexchange.admin.console.AdminParser;
import com.openexchange.admin.console.CLIOption;
import com.openexchange.admin.console.ObjectNamingAbstraction;
import com.openexchange.admin.console.AdminParser.NeededQuadState;
import com.openexchange.admin.rmi.OXUserInterface;
import com.openexchange.admin.rmi.dataobjects.Context;
import com.openexchange.admin.rmi.dataobjects.Credentials;
import com.openexchange.admin.rmi.dataobjects.User;
import com.openexchange.admin.rmi.dataobjects.UserModuleAccess;
import com.openexchange.admin.rmi.exceptions.InvalidDataException;

public abstract class UserAbstraction extends ObjectNamingAbstraction {
    
    protected class MethodAndNames {
        private Method method = null;
        
        private String name = null;
        
        private String returntype = null;
        
        /**
         * @param method
         * @param name
         */
        public MethodAndNames(final Method method, final String name, final String returntype) {
            super();
            this.method = method;
            this.name = name;
            this.returntype = returntype;
        }
        
        public Method getMethod() {
            return this.method;
        }
        
        public void setMethod(final Method method) {
            this.method = method;
        }
        
        public String getName() {
            return this.name;
        }
        
        public void setName(final String name) {
            this.name = name;
        }
        
        public final void setReturntype(final String returntype) {
            this.returntype = returntype;
        }
        
        public final String getReturntype() {
            return this.returntype;
        }
        
    }

    protected class OptionAndMethod {
        private Method method = null;
        
        private CLIOption option = null;
        
        private String returntype = null;
        
        public final Method getMethod() {
            return this.method;
        }
        
        public final void setMethod(final Method method) {
            this.method = method;
        }
        
        public final CLIOption getOption() {
            return this.option;
        }
        
        public final void setOption(final CLIOption option) {
            this.option = option;
        }
        
        /**
         * @param method
         * @param option
         */
        public OptionAndMethod(final Method method, final CLIOption option, final String returntype) {
            super();
            this.method = method;
            this.option = option;
            this.returntype = returntype;
        }
        
        public final String getReturntype() {
            return this.returntype;
        }
        
        public final void setReturntype(final String returntype) {
            this.returntype = returntype;
        }
        
    }

    public enum AccessCombinations {
        ACCESS_COMBI_NAME(0, OPT_ACCESSRIGHTS_COMBINATION_NAME, false),
        accessCalendar(1, OPT_ACCESS_CALENDAR, false),
        accessContacts(2, OPT_ACCESS_CONTACTS, false),
        accessDelegatetasks(3, OPT_ACCESS_DELEGATE_TASKS, false),
        accessEditPublicFolder(4, OPT_ACCESS_EDIT_PUBLIC_FOLDERS, false),
        accessForum(5, OPT_ACCESS_FORUM, false),
        accessIcal(6, OPT_ACCESS_ICAL, false),
        accessInfostore(7, OPT_ACCESS_INFOSTORE, false),
        accessPinboardWrite(8, OPT_ACCESS_PINBOARD_WRITE, false),
        accessProjects(9, OPT_ACCESS_PROJECTS, false),
        accessReadCreateSharedFolders(10, OPT_ACCESS_READCREATE_SHARED_FOLDERS, false),
        accessRssBookmarks(11, OPT_ACCESS_RSS_BOOKMARKS, false),
        accessRssPortal(12, OPT_ACCESS_RSS_PORTAL, false),
        accessSyncML(13, OPT_ACCESS_SYNCML, false),
        accessTasks(14, OPT_ACCESS_TASKS, false),
        accessVcard(15, OPT_ACCESS_VCARD, false),
        accessWebdav(16, OPT_ACCESS_WEBDAV, false),
        accessWebdavxml(17, OPT_ACCESS_WEBDAV_XML, false),
        accessWebmail(18, OPT_ACCESS_WEBMAIL, false),
        accessEditgroup(19, OPT_ACCESS_EDIT_GROUP, false),
        accessEditresource(20, OPT_ACCESS_EDIT_RESOURCE, false),
        accessEditpassword(21, OPT_ACCESS_EDIT_PASSWORD, false),
        accessCollectemailaddresses(22, OPT_ACCESS_COLLECT_EMAIL_ADDRESSES, false),
        accessMultiplemailaccounts(23, OPT_ACCESS_MULTIPLE_MAIL_ACCOUNTS, false),
        accessSubscription(24, OPT_ACCESS_SUBSCRIPTION, false),
        accessPublication(25, OPT_ACCESS_PUBLICATION, false),
        accessActiveSync(26, OPT_ACCESS_ACTIVE_SYNC, false),
        accessUsm(27, OPT_ACCESS_USM, false);
        
        private final static Map<String, AccessCombinations> CONSTANT_MAP = new HashMap<String, AccessCombinations>(28);
        
        static {
            for (final AccessCombinations value : AccessCombinations.values()) {
                CONSTANT_MAP.put(value.getString(), value);
            }
        }

        private final String string;
        
        private final int index;
        
        private final boolean required;
        
        private AccessCombinations(final int index, final String string, final boolean required) {
            this.index = index;
            this.string = string;
            this.required = required;
        }

        public String getString() {
            return string;
        }

        
        public int getIndex() {
            return index;
        }

        public boolean isRequired() {
            return required;
        }

        public static AccessCombinations getConstantFromString(final String string) {
            return CONSTANT_MAP.get(string);
        }


    }
    
    protected static int INITIAL_CONSTANTS_VALUE = AccessCombinations.values().length;
    
    public enum Constants {
        CONTEXTID(INITIAL_CONSTANTS_VALUE, OPT_NAME_CONTEXT_LONG, true),
        adminuser(INITIAL_CONSTANTS_VALUE + 1, OPT_NAME_ADMINUSER_LONG, false),
        adminpass(INITIAL_CONSTANTS_VALUE + 2, OPT_NAME_ADMINPASS_LONG, false),
        USERNAME(INITIAL_CONSTANTS_VALUE + 3, OPT_USERNAME_LONG, true),
        DISPLAYNAME(INITIAL_CONSTANTS_VALUE + 4, OPT_DISPLAYNAME_LONG, true),
        GIVENNAME(INITIAL_CONSTANTS_VALUE + 5,OPT_GIVENNAME_LONG, true),
        SURNAME(INITIAL_CONSTANTS_VALUE + 6,OPT_SURNAME_LONG, true),
        PASSWORD(INITIAL_CONSTANTS_VALUE + 7,OPT_PASSWORD_LONG, true),
        EMAIL(INITIAL_CONSTANTS_VALUE + 8,OPT_PRIMARY_EMAIL_LONG, true),
        LANGUAGE(INITIAL_CONSTANTS_VALUE + 9,OPT_LANGUAGE_LONG, false),
        timezone(INITIAL_CONSTANTS_VALUE + 10,OPT_TIMEZONE_LONG, false),
        THEME(INITIAL_CONSTANTS_VALUE + 11,OPT_ADD_GUI_SETTING_LONG, false),
        department(INITIAL_CONSTANTS_VALUE + 12,OPT_DEPARTMENT_LONG, false),
        company(INITIAL_CONSTANTS_VALUE + 13,OPT_COMPANY_LONG, false),
        MAILALIAS(INITIAL_CONSTANTS_VALUE + 14,OPT_ALIASES_LONG, false),
        EMAIL1(INITIAL_CONSTANTS_VALUE + 15,OPT_EMAIL1_LONG, false),
        mailenabled(INITIAL_CONSTANTS_VALUE + 16,OPT_MAILENABLED_LONG, false),
        birthday(INITIAL_CONSTANTS_VALUE + 17,OPT_BIRTHDAY_LONG, false),
        anniversary(INITIAL_CONSTANTS_VALUE + 18,OPT_ANNIVERSARY_LONG, false),
        branches(INITIAL_CONSTANTS_VALUE + 19,OPT_BRANCHES_LONG, false),
        business_category(INITIAL_CONSTANTS_VALUE + 20,OPT_BUSINESS_CATEGORY_LONG, false),
        postal_code_business(INITIAL_CONSTANTS_VALUE + 21,OPT_POSTAL_CODE_BUSINESS_LONG, false),
        state_business(INITIAL_CONSTANTS_VALUE + 22,OPT_STATE_BUSINESS_LONG, false),
        street_business(INITIAL_CONSTANTS_VALUE + 23,OPT_STREET_BUSINESS_LONG, false),
        telephone_callback(INITIAL_CONSTANTS_VALUE + 24,OPT_TELEPHONE_CALLBACK_LONG, false),
        city_home(INITIAL_CONSTANTS_VALUE + 25,OPT_CITY_HOME_LONG, false),
        commercial_register(INITIAL_CONSTANTS_VALUE + 26,OPT_COMMERCIAL_REGISTER_LONG, false),
        country_home(INITIAL_CONSTANTS_VALUE + 27,OPT_COUNTRY_HOME_LONG, false),
        email2(INITIAL_CONSTANTS_VALUE + 28,OPT_EMAIL2_LONG, false),
        email3(INITIAL_CONSTANTS_VALUE + 29,OPT_EMAIL3_LONG, false),
        employeetype(INITIAL_CONSTANTS_VALUE + 30,OPT_EMPLOYEETYPE_LONG, false),
        fax_business(INITIAL_CONSTANTS_VALUE + 31,OPT_FAX_BUSINESS_LONG, false),
        fax_home(INITIAL_CONSTANTS_VALUE + 32,OPT_FAX_HOME_LONG, false),
        fax_other(INITIAL_CONSTANTS_VALUE + 33,OPT_FAX_OTHER_LONG, false),
        imapserver(INITIAL_CONSTANTS_VALUE + 34,OPT_IMAPSERVER_LONG, false),
        imaplogin(INITIAL_CONSTANTS_VALUE + 35,OPT_IMAPLOGIN_LONG, false),
        smtpserver(INITIAL_CONSTANTS_VALUE + 36,OPT_SMTPSERVER_LONG, false),
        instant_messenger1(INITIAL_CONSTANTS_VALUE + 37,OPT_INSTANT_MESSENGER1_LONG, false),
        instant_messenger2(INITIAL_CONSTANTS_VALUE + 38,OPT_INSTANT_MESSENGER2_LONG, false),
        telephone_ip(INITIAL_CONSTANTS_VALUE + 39,OPT_TELEPHONE_IP_LONG, false),
        telephone_isdn(INITIAL_CONSTANTS_VALUE + 40,OPT_TELEPHONE_ISDN_LONG, false),
        mail_folder_drafts_name(INITIAL_CONSTANTS_VALUE + 41,OPT_MAIL_FOLDER_DRAFTS_NAME_LONG, false),
        mail_folder_sent_name(INITIAL_CONSTANTS_VALUE + 42,OPT_MAIL_FOLDER_SENT_NAME_LONG, false),
        mail_folder_spam_name(INITIAL_CONSTANTS_VALUE + 43,OPT_MAIL_FOLDER_SPAM_NAME_LONG, false),
        mail_folder_trash_name(INITIAL_CONSTANTS_VALUE + 44,OPT_MAIL_FOLDER_TRASH_NAME_LONG, false),
        manager_name(INITIAL_CONSTANTS_VALUE + 45,OPT_MANAGER_NAME_LONG, false),
        marital_status(INITIAL_CONSTANTS_VALUE + 46,OPT_MARITAL_STATUS_LONG, false),
        cellular_telephone1(INITIAL_CONSTANTS_VALUE + 47,OPT_CELLULAR_TELEPHONE1_LONG, false),
        cellular_telephone2(INITIAL_CONSTANTS_VALUE + 48,OPT_CELLULAR_TELEPHONE2_LONG, false),
        info(INITIAL_CONSTANTS_VALUE + 49,OPT_INFO_LONG, false),
        nickname(INITIAL_CONSTANTS_VALUE + 50,OPT_NICKNAME_LONG, false),
        number_of_children(INITIAL_CONSTANTS_VALUE + 51,OPT_NUMBER_OF_CHILDREN_LONG, false),
        note(INITIAL_CONSTANTS_VALUE + 52,OPT_NOTE_LONG, false),
        number_of_employee(INITIAL_CONSTANTS_VALUE + 53,OPT_NUMBER_OF_EMPLOYEE_LONG, false),
        telephone_pager(INITIAL_CONSTANTS_VALUE + 54,OPT_TELEPHONE_PAGER_LONG, false),
        password_expired(INITIAL_CONSTANTS_VALUE + 55,OPT_PASSWORD_EXPIRED_LONG, false),
        telephone_assistant(INITIAL_CONSTANTS_VALUE + 56,OPT_TELEPHONE_ASSISTANT_LONG, false),
        telephone_business1(INITIAL_CONSTANTS_VALUE + 57,OPT_TELEPHONE_BUSINESS1_LONG, false),
        telephone_business2(INITIAL_CONSTANTS_VALUE + 58,OPT_TELEPHONE_BUSINESS2_LONG, false),
        telephone_car(INITIAL_CONSTANTS_VALUE + 59,OPT_TELEPHONE_CAR_LONG, false),
        telephone_company(INITIAL_CONSTANTS_VALUE + 60,OPT_TELEPHONE_COMPANY_LONG, false),
        telephone_home1(INITIAL_CONSTANTS_VALUE + 61,OPT_TELEPHONE_HOME1_LONG, false),
        telephone_home2(INITIAL_CONSTANTS_VALUE + 62,OPT_TELEPHONE_HOME2_LONG, false),
        telephone_other(INITIAL_CONSTANTS_VALUE + 63,OPT_TELEPHONE_OTHER_LONG, false),
        position(INITIAL_CONSTANTS_VALUE + 64,OPT_POSITION_LONG, false),
        postal_code_home(INITIAL_CONSTANTS_VALUE + 65,OPT_POSTAL_CODE_HOME_LONG, false),
        profession(INITIAL_CONSTANTS_VALUE + 66,OPT_PROFESSION_LONG, false),
        telephone_radio(INITIAL_CONSTANTS_VALUE + 67,OPT_TELEPHONE_RADIO_LONG, false),
        room_number(INITIAL_CONSTANTS_VALUE + 68,OPT_ROOM_NUMBER_LONG, false),
        sales_volume(INITIAL_CONSTANTS_VALUE + 69,OPT_SALES_VOLUME_LONG, false),
        city_other(INITIAL_CONSTANTS_VALUE + 70,OPT_CITY_OTHER_LONG, false),
        country_other(INITIAL_CONSTANTS_VALUE + 71,OPT_COUNTRY_OTHER_LONG, false),
        middle_name(INITIAL_CONSTANTS_VALUE + 72,OPT_MIDDLE_NAME_LONG, false),
        postal_code_other(INITIAL_CONSTANTS_VALUE + 73,OPT_POSTAL_CODE_OTHER_LONG, false),
        state_other(INITIAL_CONSTANTS_VALUE + 74,OPT_STATE_OTHER_LONG, false),
        street_other(INITIAL_CONSTANTS_VALUE + 75,OPT_STREET_OTHER_LONG, false),
        spouse_name(INITIAL_CONSTANTS_VALUE + 76,OPT_SPOUSE_NAME_LONG, false),
        state_home(INITIAL_CONSTANTS_VALUE + 77,OPT_STATE_HOME_LONG, false),
        street_home(INITIAL_CONSTANTS_VALUE + 78,OPT_STREET_HOME_LONG, false),
        suffix(INITIAL_CONSTANTS_VALUE + 79,OPT_SUFFIX_LONG, false),
        tax_id(INITIAL_CONSTANTS_VALUE + 80,OPT_TAX_ID_LONG, false),
        telephone_telex(INITIAL_CONSTANTS_VALUE + 81,OPT_TELEPHONE_TELEX_LONG, false),
        title(INITIAL_CONSTANTS_VALUE + 82,OPT_TITLE_LONG, false),
        telephone_ttytdd(INITIAL_CONSTANTS_VALUE + 83,OPT_TELEPHONE_TTYTDD_LONG, false),
        UPLOADFILESIZELIMIT(INITIAL_CONSTANTS_VALUE + 84,OPT_UPLOADFILESIZELIMIT_LONG, false),
        uploadfilesizelimitperfile(INITIAL_CONSTANTS_VALUE + 85,OPT_UPLOADFILESIZELIMITPERFILE_LONG, false),
        url(INITIAL_CONSTANTS_VALUE + 86,OPT_URL_LONG, false),
        userfield01(INITIAL_CONSTANTS_VALUE + 87,OPT_USERFIELD01_LONG, false),
        userfield02(INITIAL_CONSTANTS_VALUE + 88,OPT_USERFIELD02_LONG, false),
        userfield03(INITIAL_CONSTANTS_VALUE + 89,OPT_USERFIELD03_LONG, false),
        userfield04(INITIAL_CONSTANTS_VALUE + 90,OPT_USERFIELD04_LONG, false),
        userfield05(INITIAL_CONSTANTS_VALUE + 91,OPT_USERFIELD05_LONG, false),
        userfield06(INITIAL_CONSTANTS_VALUE + 92,OPT_USERFIELD06_LONG, false),
        userfield07(INITIAL_CONSTANTS_VALUE + 93,OPT_USERFIELD07_LONG, false),
        userfield08(INITIAL_CONSTANTS_VALUE + 94,OPT_USERFIELD08_LONG, false),
        userfield09(INITIAL_CONSTANTS_VALUE + 95,OPT_USERFIELD09_LONG, false),
        userfield10(INITIAL_CONSTANTS_VALUE + 96,OPT_USERFIELD10_LONG, false),
        userfield11(INITIAL_CONSTANTS_VALUE + 97,OPT_USERFIELD11_LONG, false),
        userfield12(INITIAL_CONSTANTS_VALUE + 98,OPT_USERFIELD12_LONG, false),
        userfield13(INITIAL_CONSTANTS_VALUE + 99,OPT_USERFIELD13_LONG, false),
        userfield14(INITIAL_CONSTANTS_VALUE + 100,OPT_USERFIELD14_LONG, false),
        userfield15(INITIAL_CONSTANTS_VALUE + 101,OPT_USERFIELD15_LONG, false),
        userfield16(INITIAL_CONSTANTS_VALUE + 102,OPT_USERFIELD16_LONG, false),
        userfield17(INITIAL_CONSTANTS_VALUE + 103,OPT_USERFIELD17_LONG, false),
        userfield18(INITIAL_CONSTANTS_VALUE + 104,OPT_USERFIELD18_LONG, false),
        userfield19(INITIAL_CONSTANTS_VALUE + 105,OPT_USERFIELD19_LONG, false),
        userfield20(INITIAL_CONSTANTS_VALUE + 106,OPT_USERFIELD20_LONG, false),
        city_business(INITIAL_CONSTANTS_VALUE + 107,OPT_CITY_BUSINESS_LONG, false),
        country_business(INITIAL_CONSTANTS_VALUE + 108,OPT_COUNTRY_BUSINESS_LONG, false),
        assistant_name(INITIAL_CONSTANTS_VALUE + 109,OPT_ASSISTANT_NAME_LONG, false),
        telephone_primary(INITIAL_CONSTANTS_VALUE + 110,OPT_TELEPHONE_PRIMARY_LONG, false),
        categories(INITIAL_CONSTANTS_VALUE + 111,OPT_CATEGORIES_LONG, false),
        PASSWORDMECH(INITIAL_CONSTANTS_VALUE + 112,OPT_PASSWORDMECH_LONG, false),
        mail_folder_confirmed_ham_name(INITIAL_CONSTANTS_VALUE + 113,OPT_MAIL_FOLDER_CONFIRMED_HAM_NAME_LONG, false),
        mail_folder_confirmed_spam_name(INITIAL_CONSTANTS_VALUE + 114,OPT_MAIL_FOLDER_CONFIRMED_SPAM_NAME_LONG, false),
        DEFAULTSENDERADDRESS(INITIAL_CONSTANTS_VALUE + 115,OPT_DEFAULTSENDERADDRESS_LONG, false),
        gui_spam_filter_capabilities_enabled(INITIAL_CONSTANTS_VALUE + 116,OPT_GUI_LONG, false);

        private final static Map<String, Constants> CONSTANT_MAP = new HashMap<String, Constants>(117);
        
        static {
            for (final Constants value : Constants.values()) {
                CONSTANT_MAP.put(value.getString(), value);
            }
        }
        
        private final String string;
        
        private final int index;
        
        private final boolean required;
        
        private Constants(final int index, final String string, final boolean required) {
            this.index = index;
            this.string = string;
            this.required = required;
        }

        public String getString() {
            return string;
        }

        
        public int getIndex() {
            return index;
        }
        
        public boolean isRequired() {
            return required;
        }

        public static Constants getConstantFromString(final String string) {
            return CONSTANT_MAP.get(string);
        }
    }

    final static protected UserModuleAccess NO_RIGHTS_ACCESS = new UserModuleAccess();

    protected static final String ACCESS_COMBINATION_NAME_AND_ACCESS_RIGHTS_DETECTED_ERROR = "You can\u00b4t specify access combination name AND single access attributes simultaneously!";
    
    protected static final char OPT_ID_SHORT = 'i';
    protected static final String OPT_ID_LONG = "userid";
    protected static final char OPT_USERNAME_SHORT = 'u';
    protected static final String OPT_USERNAME_LONG = "username";
    protected static final char OPT_DISPLAYNAME_SHORT = 'd';
    protected static final String OPT_DISPLAYNAME_LONG = "displayname";
    protected static final char OPT_PASSWORD_SHORT = 'p';
    protected static final String OPT_PASSWORD_LONG = "password";
    protected static final char OPT_GIVENNAME_SHORT = 'g';
    protected static final String OPT_GIVENNAME_LONG = "givenname";
    protected static final char OPT_SURNAME_SHORT = 's';
    protected static final String OPT_SURNAME_LONG = "surname";
    protected static final char OPT_LANGUAGE_SHORT = 'l';
    protected static final String OPT_LANGUAGE_LONG = "language";
    protected static final char OPT_TIMEZONE_SHORT = 't';
    protected static final String OPT_TIMEZONE_LONG = "timezone";
    protected static final char OPT_PRIMARY_EMAIL_SHORT = 'e';
    protected static final String OPT_PRIMARY_EMAIL_LONG = "email";
    protected static final char OPT_DEPARTMENT_SHORT = 'x';
    protected static final String OPT_DEPARTMENT_LONG = "department";
    protected static final char OPT_COMPANY_SHORT = 'z';
    protected static final String OPT_COMPANY_LONG = "company";
    protected static final char OPT_ALIASES_SHORT = 'a';
    protected static final String OPT_ALIASES_LONG = "aliases";
    
    protected static final String OPT_EXTENDED_LONG = "extendedoptions";
    
    protected static final String OPT_ACCESSRIGHTS_COMBINATION_NAME = "access-combination-name";
    
    protected static final String OPT_ACCESS_CALENDAR = "access-calendar";
    protected static final String OPT_ACCESS_CONTACTS = "access-contacts";
    protected static final String OPT_ACCESS_DELEGATE_TASKS = "access-delegate-tasks";
    protected static final String OPT_ACCESS_EDIT_PUBLIC_FOLDERS = "access-edit-public-folder";
    protected static final String OPT_ACCESS_FORUM = "access-forum";
    protected static final String OPT_ACCESS_ICAL = "access-ical";
    protected static final String OPT_ACCESS_INFOSTORE = "access-infostore";
    protected static final String OPT_ACCESS_PINBOARD_WRITE = "access-pinboard-write";
    protected static final String OPT_ACCESS_PROJECTS = "access-projects";
    protected static final String OPT_ACCESS_READCREATE_SHARED_FOLDERS = "access-read-create-shared-Folders";
    protected static final String OPT_ACCESS_RSS_BOOKMARKS = "access-rss-bookmarks";
    protected static final String OPT_ACCESS_RSS_PORTAL = "access-rss-portal";
    protected static final String OPT_ACCESS_SYNCML = "access-syncml";
    protected static final String OPT_ACCESS_TASKS = "access-tasks";
    protected static final String OPT_ACCESS_VCARD = "access-vcard";
    protected static final String OPT_ACCESS_WEBDAV = "access-webdav";
    protected static final String OPT_ACCESS_WEBDAV_XML = "access-webdav-xml";
    protected static final String OPT_ACCESS_WEBMAIL = "access-webmail";
    protected static final String OPT_ACCESS_EDIT_GROUP = "access-edit-group";
    protected static final String OPT_ACCESS_EDIT_RESOURCE = "access-edit-resource";
    protected static final String OPT_ACCESS_EDIT_PASSWORD = "access-edit-password";
    protected static final String OPT_ACCESS_COLLECT_EMAIL_ADDRESSES = "access-collect-email-addresses";
    protected static final String OPT_ACCESS_MULTIPLE_MAIL_ACCOUNTS = "access-multiple-mail-accounts";
    protected static final String OPT_ACCESS_SUBSCRIPTION = "access-subscription";
    protected static final String OPT_ACCESS_PUBLICATION = "access-publication";
    protected static final String OPT_ACCESS_ACTIVE_SYNC = "access-active-sync";
    protected static final String OPT_ACCESS_USM = "access-usm";
    protected static final String OPT_DISABLE_GAB = "access-global-address-book-disabled";
    protected static final String OPT_ACCESS_PUBLIC_FOLDER_EDITABLE = "access-public-folder-editable";
    protected static final String OPT_GUI_LONG = "gui_spam_filter_capabilities_enabled";
    protected static final String OPT_CSV_IMPORT = "csv-import";
    
    // extended options
    protected static final String OPT_EMAIL1_LONG = "email1";
    protected static final String OPT_MAILENABLED_LONG = "mailenabled";
    protected static final String OPT_BIRTHDAY_LONG = "birthday";
    protected static final String OPT_ANNIVERSARY_LONG = "anniversary";
    protected static final String OPT_BRANCHES_LONG = "branches";
    protected static final String OPT_BUSINESS_CATEGORY_LONG = "business_category";
    protected static final String OPT_POSTAL_CODE_BUSINESS_LONG = "postal_code_business";
    protected static final String OPT_STATE_BUSINESS_LONG = "state_business";
    protected static final String OPT_STREET_BUSINESS_LONG = "street_business";
    protected static final String OPT_TELEPHONE_CALLBACK_LONG = "telephone_callback";
    protected static final String OPT_CITY_HOME_LONG = "city_home";
    protected static final String OPT_COMMERCIAL_REGISTER_LONG = "commercial_register";
    protected static final String OPT_COUNTRY_HOME_LONG = "country_home";
    protected static final String OPT_EMAIL2_LONG = "email2";
    protected static final String OPT_EMAIL3_LONG = "email3";
    protected static final String OPT_EMPLOYEETYPE_LONG = "employeetype";
    protected static final String OPT_FAX_BUSINESS_LONG = "fax_business";
    protected static final String OPT_FAX_HOME_LONG = "fax_home";
    protected static final String OPT_FAX_OTHER_LONG = "fax_other";
    protected static final String OPT_IMAPSERVER_LONG = "imapserver";
    protected static final String OPT_IMAPLOGIN_LONG = "imaplogin";
    protected static final String OPT_SMTPSERVER_LONG = "smtpserver";
    protected static final String OPT_INSTANT_MESSENGER1_LONG = "instant_messenger1";
    protected static final String OPT_INSTANT_MESSENGER2_LONG = "instant_messenger2";
    protected static final String OPT_TELEPHONE_IP_LONG = "telephone_ip";
    protected static final String OPT_TELEPHONE_ISDN_LONG = "telephone_isdn";
    protected static final String OPT_MAIL_FOLDER_DRAFTS_NAME_LONG = "mail_folder_drafts_name";
    protected static final String OPT_MAIL_FOLDER_SENT_NAME_LONG = "mail_folder_sent_name";
    protected static final String OPT_MAIL_FOLDER_SPAM_NAME_LONG = "mail_folder_spam_name";
    protected static final String OPT_MAIL_FOLDER_TRASH_NAME_LONG = "mail_folder_trash_name";
    protected static final String OPT_MANAGER_NAME_LONG = "manager_name";
    protected static final String OPT_MARITAL_STATUS_LONG = "marital_status";
    protected static final String OPT_CELLULAR_TELEPHONE1_LONG = "cellular_telephone1";
    protected static final String OPT_CELLULAR_TELEPHONE2_LONG = "cellular_telephone2";
    protected static final String OPT_INFO_LONG = "info";
    protected static final String OPT_NICKNAME_LONG = "nickname";
    protected static final String OPT_NUMBER_OF_CHILDREN_LONG = "number_of_children";
    protected static final String OPT_NOTE_LONG = "note";
    protected static final String OPT_NUMBER_OF_EMPLOYEE_LONG = "number_of_employee";
    protected static final String OPT_TELEPHONE_PAGER_LONG = "telephone_pager";
    protected static final String OPT_PASSWORD_EXPIRED_LONG = "password_expired";
    protected static final String OPT_TELEPHONE_ASSISTANT_LONG = "telephone_assistant";
    protected static final String OPT_TELEPHONE_BUSINESS1_LONG = "telephone_business1";
    protected static final String OPT_TELEPHONE_BUSINESS2_LONG = "telephone_business2";
    protected static final String OPT_TELEPHONE_CAR_LONG = "telephone_car";
    protected static final String OPT_TELEPHONE_COMPANY_LONG = "telephone_company";
    protected static final String OPT_TELEPHONE_HOME1_LONG = "telephone_home1";
    protected static final String OPT_TELEPHONE_HOME2_LONG = "telephone_home2";
    protected static final String OPT_TELEPHONE_OTHER_LONG = "telephone_other";
    protected static final String OPT_POSTAL_CODE_HOME_LONG = "postal_code_home";
    protected static final String OPT_PROFESSION_LONG = "profession";
    protected static final String OPT_TELEPHONE_RADIO_LONG = "telephone_radio";
    protected static final String OPT_ROOM_NUMBER_LONG = "room_number";
    protected static final String OPT_SALES_VOLUME_LONG = "sales_volume";
    protected static final String OPT_CITY_OTHER_LONG = "city_other";
    protected static final String OPT_COUNTRY_OTHER_LONG = "country_other";
    protected static final String OPT_MIDDLE_NAME_LONG = "middle_name";
    protected static final String OPT_POSTAL_CODE_OTHER_LONG = "postal_code_other";
    protected static final String OPT_STATE_OTHER_LONG = "state_other";
    protected static final String OPT_STREET_OTHER_LONG = "street_other";
    protected static final String OPT_SPOUSE_NAME_LONG = "spouse_name";
    protected static final String OPT_STATE_HOME_LONG = "state_home";
    protected static final String OPT_STREET_HOME_LONG = "street_home";
    protected static final String OPT_SUFFIX_LONG = "suffix";
    protected static final String OPT_TAX_ID_LONG = "tax_id";
    protected static final String OPT_TELEPHONE_TELEX_LONG = "telephone_telex";
    protected static final String OPT_TELEPHONE_TTYTDD_LONG = "telephone_ttytdd";
    protected static final String OPT_UPLOADFILESIZELIMIT_LONG = "uploadfilesizelimit";
    protected static final String OPT_UPLOADFILESIZELIMITPERFILE_LONG = "uploadfilesizelimitperfile";
    protected static final String OPT_URL_LONG = "url";
    protected static final String OPT_USERFIELD01_LONG = "userfield01";
    protected static final String OPT_USERFIELD02_LONG = "userfield02";
    protected static final String OPT_USERFIELD03_LONG = "userfield03";
    protected static final String OPT_USERFIELD04_LONG = "userfield04";
    protected static final String OPT_USERFIELD05_LONG = "userfield05";
    protected static final String OPT_USERFIELD06_LONG = "userfield06";
    protected static final String OPT_USERFIELD07_LONG = "userfield07";
    protected static final String OPT_USERFIELD08_LONG = "userfield08";
    protected static final String OPT_USERFIELD09_LONG = "userfield09";
    protected static final String OPT_USERFIELD10_LONG = "userfield10";
    protected static final String OPT_USERFIELD11_LONG = "userfield11";
    protected static final String OPT_USERFIELD12_LONG = "userfield12";
    protected static final String OPT_USERFIELD13_LONG = "userfield13";
    protected static final String OPT_USERFIELD14_LONG = "userfield14";
    protected static final String OPT_USERFIELD15_LONG = "userfield15";
    protected static final String OPT_USERFIELD16_LONG = "userfield16";
    protected static final String OPT_USERFIELD17_LONG = "userfield17";
    protected static final String OPT_USERFIELD18_LONG = "userfield18";
    protected static final String OPT_USERFIELD19_LONG = "userfield19";
    protected static final String OPT_USERFIELD20_LONG = "userfield20";
    protected static final String OPT_CITY_BUSINESS_LONG = "city_business";
    protected static final String OPT_ASSISTANT_NAME_LONG = "assistant_name";
    protected static final String OPT_TELEPHONE_PRIMARY_LONG = "telephone_primary";
    protected static final String OPT_CATEGORIES_LONG = "categories";
    protected static final String OPT_PASSWORDMECH_LONG = "passwordmech";
    protected static final String OPT_MAIL_FOLDER_CONFIRMED_HAM_NAME_LONG = "mail_folder_confirmed_ham_name";
    protected static final String OPT_MAIL_FOLDER_CONFIRMED_SPAM_NAME_LONG = "mail_folder_confirmed_spam_name";
    protected static final String OPT_DEFAULTSENDERADDRESS_LONG = "defaultsenderaddress";
    protected static final String OPT_COUNTRY_BUSINESS_LONG = "country_business";
    protected static final String OPT_FOLDERTREE_LONG = "foldertree";
    protected static final String OPT_TITLE_LONG = "title";
    protected static final String OPT_POSITION_LONG = "position";

    
    
    protected static final String JAVA_UTIL_TIME_ZONE = "java.util.TimeZone";
    protected static final String PASSWORDMECH_CLASS = "com.openexchange.admin.rmi.dataobjects.User$PASSWORDMECH";
    protected static final String JAVA_UTIL_HASH_SET = "java.util.HashSet";
    protected static final String JAVA_UTIL_MAP = "java.util.Map";
    protected static final String JAVA_UTIL_DATE = "java.util.Date";
    protected static final String JAVA_LANG_BOOLEAN = "java.lang.Boolean";
    protected static final String JAVA_LANG_INTEGER = "java.lang.Integer";
    protected static final String JAVA_UTIL_ARRAY_LIST = "java.util.ArrayList";
    protected static final String JAVA_UTIL_LOCALE = "java.util.Locale";
    protected static final String JAVA_LANG_LONG = "java.lang.Long";
    protected static final String JAVA_LANG_STRING = "java.lang.String";
    protected static final String SIMPLE_INT = "int";
    protected static final String OPT_IMAPONLY_LONG = "imaponly";
    protected static final String OPT_DBONLY_LONG = "dbonly";
    
    public static final ArrayList<OptionAndMethod> optionsandmethods = new ArrayList<OptionAndMethod>();

    static {
        NO_RIGHTS_ACCESS.disableAll();
    }

    protected CLIOption userNameOption = null;
    protected CLIOption displayNameOption = null;
    protected CLIOption givenNameOption = null;
    protected CLIOption surNameOption = null;
    protected CLIOption passwordOption = null;
    protected CLIOption primaryMailOption = null;
    protected CLIOption languageOption = null;
    protected CLIOption timezoneOption = null;
    protected CLIOption departmentOption = null;
    protected CLIOption companyOption = null;
    protected CLIOption aliasesOption = null;
    protected CLIOption idOption = null;
    protected CLIOption imapOnlyOption = null;
    protected CLIOption dbOnlyOption = null;
    protected CLIOption extendedOption = null;
    protected CLIOption imapQuotaOption = null;
    protected CLIOption inetMailAccessOption = null;
    protected CLIOption spamFilterOption = null;
    
    protected CLIOption accessRightsCombinationName = null;
    
    // access to modules
    protected CLIOption accessCalendarOption = null;
    protected CLIOption accessContactOption = null;
    protected CLIOption accessDelegateTasksOption = null;
    protected CLIOption accessEditPublicFolderOption = null;
    protected CLIOption accessForumOption = null;
    protected CLIOption accessIcalOption = null;
    protected CLIOption accessInfostoreOption = null;
    protected CLIOption accessPinboardWriteOption = null;
    protected CLIOption accessProjectsOption = null;
    protected CLIOption accessReadCreateSharedFolderOption = null;
    protected CLIOption accessRssBookmarkOption = null;
    protected CLIOption accessRssPortalOption = null;
    protected CLIOption accessSyncmlOption = null;
    protected CLIOption accessTasksOption = null;
    protected CLIOption accessVcardOption = null;
    protected CLIOption accessWebdavOption = null;
    protected CLIOption accessWebdavXmlOption = null;
    protected CLIOption accessWebmailOption = null;
    protected CLIOption accessEditGroupOption = null;
    protected CLIOption accessEditResourceOption = null;
    protected CLIOption accessEditPasswordOption = null;
    protected CLIOption accessCollectEmailAddresses = null;
    protected CLIOption accessMultipleMailAccounts = null;
    protected CLIOption accessPublication = null;
    protected CLIOption accessSubscription = null;
    protected CLIOption accessActiveSync = null;
    protected CLIOption accessUSM = null;
    protected CLIOption accessGAB = null;
    protected CLIOption accessPublicFolderEditable = null;
    
    
    // non-generic extended option
    protected CLIOption addGUISettingOption = null;
    protected CLIOption removeGUISettingOption = null;

    protected static final String OPT_ADD_GUI_SETTING_LONG = "addguipreferences";
    protected static final String OPT_REMOVE_GUI_SETTING_LONG = "removeguipreferences";
    
    // For right error output
    protected String username = null;
    protected Integer userid = null;
    private CLIOption email1Option;
    private CLIOption mailenabledOption;
    private CLIOption birthdayOption;
    private CLIOption anniversaryOption;
    private CLIOption branchesOption;
    private CLIOption business_categoryOption;
    private CLIOption postal_code_businessOption;
    private CLIOption state_businessOption;
    private CLIOption street_businessOption;
    private CLIOption telephone_callbackOption;
    private CLIOption city_homeOption;
    private CLIOption commercial_registerOption;
    private CLIOption country_homeOption;
    private CLIOption email2Option;
    private CLIOption email3Option;
    private CLIOption employeetypeOption;
    private CLIOption fax_businessOption;
    private CLIOption fax_homeOption;
    private CLIOption fax_otherOption;
    private CLIOption imapserverOption;
    private CLIOption imaploginOption;
    private CLIOption smtpserverOption;
    private CLIOption instant_messenger1Option;
    private CLIOption instant_messenger2Option;
    private CLIOption telephone_ipOption;
    private CLIOption telephone_isdnOption;
    private CLIOption mail_folder_drafts_nameOption;
    private CLIOption mail_folder_sent_nameOption;
    private CLIOption mail_folder_spam_nameOption;
    private CLIOption mail_folder_trash_nameOption;
    private CLIOption manager_nameOption;
    private CLIOption marital_statusOption;
    private CLIOption cellular_telephone1Option;
    private CLIOption cellular_telephone2Option;
    private CLIOption infoOption;
    private CLIOption nicknameOption;
    private CLIOption number_of_childrenOption;
    private CLIOption noteOption;
    private CLIOption number_of_employeeOption;
    private CLIOption telephone_pagerOption;
    private CLIOption password_expiredOption;
    private CLIOption telephone_assistantOption;
    private CLIOption telephone_business1Option;
    private CLIOption telephone_business2Option;
    private CLIOption telephone_carOption;
    private CLIOption telephone_companyOption;
    private CLIOption telephone_home1Option;
    private CLIOption telephone_home2Option;
    private CLIOption telephone_otherOption;
    private CLIOption postal_code_homeOption;
    private CLIOption professionOption;
    private CLIOption telephone_radioOption;
    private CLIOption room_numberOption;
    private CLIOption sales_volumeOption;
    private CLIOption city_otherOption;
    private CLIOption country_otherOption;
    private CLIOption middle_nameOption;
    private CLIOption postal_code_otherOption;
    private CLIOption state_otherOption;
    private CLIOption street_otherOption;
    private CLIOption spouse_nameOption;
    private CLIOption state_homeOption;
    private CLIOption street_homeOption;
    private CLIOption suffixOption;
    private CLIOption tax_idOption;
    private CLIOption telephone_telexOption;
    private CLIOption telephone_ttytddOption;
    private CLIOption uploadfilesizelimitOption;
    private CLIOption uploadfilesizelimitperfileOption;
    private CLIOption urlOption;
    private CLIOption userfield01Option;
    private CLIOption userfield02Option;
    private CLIOption userfield03Option;
    private CLIOption userfield06Option;
    private CLIOption userfield04Option;
    private CLIOption userfield05Option;
    private CLIOption userfield07Option;
    private CLIOption userfield08Option;
    private CLIOption userfield09Option;
    private CLIOption userfield10Option;
    private CLIOption userfield11Option;
    private CLIOption userfield12Option;
    private CLIOption userfield13Option;
    private CLIOption userfield14Option;
    private CLIOption userfield15Option;
    private CLIOption userfield16Option;
    private CLIOption userfield17Option;
    private CLIOption userfield18Option;
    private CLIOption userfield19Option;
    private CLIOption userfield20Option;
    private CLIOption city_businessOption;
    private CLIOption assistant_nameOption;
    private CLIOption telephone_primaryOption;
    private CLIOption categoriesOption;
    private CLIOption passwordmechOption;
    private CLIOption mail_folder_confirmed_ham_nameOption;
    private CLIOption mail_folder_confirmed_spam_nameOption;
    private CLIOption defaultsenderaddressOption;
    private CLIOption country_businessOption;
    private CLIOption foldertreeOption;
    private CLIOption titleOption;
    private CLIOption positionOption;
    
//    /**
//     * This field holds all the options which are displayed by default. So this options can be
//     * deducted from the other dynamically created options
//     */
//    public static final HashSet<String> standardoptions = new HashSet<String>(15);
//    
//    static {
//        // Here we define those getter which shouldn't be listed in the extendedoptions
//        standardoptions.add("id");
//        standardoptions.add("name");
//        standardoptions.add("display_name");
//        standardoptions.add(OPT_PASSWORD_LONG);
//        standardoptions.add("given_name");
//        standardoptions.add("sur_name");
//        standardoptions.add(OPT_LANGUAGE_LONG);
//        standardoptions.add("primaryemail");
//        standardoptions.add(OPT_DEPARTMENT_LONG);
//        standardoptions.add(OPT_COMPANY_LONG);
//        standardoptions.add(OPT_ALIASES_LONG);
//        standardoptions.add("gui_spam_filter_capabilities_enabled");
//        standardoptions.add("gui_spam_filter_enabled");
//
//    }

    protected static UserModuleAccess getUserModuleAccess(String[] nextLine, int[] idarray) {
        final UserModuleAccess moduleaccess = new UserModuleAccess();
        final int i = idarray[AccessCombinations.accessActiveSync.getIndex()];
        if (-1 != i) {
            if (nextLine[i].length() > 0) {
                moduleaccess.setActiveSync(stringToBool(nextLine[i]));
            }
        }
        final int j = idarray[AccessCombinations.accessCalendar.getIndex()];
        if (-1 != j) {
            if (nextLine[j].length() > 0) {
                moduleaccess.setCalendar(stringToBool(nextLine[j]));
            }
        }
        final int j2 = idarray[AccessCombinations.accessCollectemailaddresses.getIndex()];
        if (-1 != j2) {
            if (nextLine[j2].length() > 0) {
                moduleaccess.setCollectEmailAddresses(stringToBool(nextLine[j2]));
            }
        }
        final int k = idarray[AccessCombinations.accessContacts.getIndex()];
        if (-1 != k) {
            if (nextLine[k].length() > 0) {
                moduleaccess.setContacts(stringToBool(nextLine[k]));
            }
        }
        final int k2 = idarray[AccessCombinations.accessDelegatetasks.getIndex()];
        if (-1 != k2) {
            if (nextLine[k2].length() > 0) {
                moduleaccess.setDelegateTask(stringToBool(nextLine[k2]));
            }
        }
        final int l = idarray[AccessCombinations.accessEditgroup.getIndex()];
        if (-1 != l) {
            if (nextLine[l].length() > 0) {
                moduleaccess.setEditGroup(stringToBool(nextLine[l]));
            }
        }
        final int l2 = idarray[AccessCombinations.accessEditpassword.getIndex()];
        if (-1 != l2) {
            if (nextLine[l2].length() > 0) {
                moduleaccess.setEditPassword(stringToBool(nextLine[l2]));
            }
        }
        final int m = idarray[AccessCombinations.accessEditPublicFolder.getIndex()];
        if (-1 != m) {
            if (nextLine[m].length() > 0) {
                moduleaccess.setEditPublicFolders(stringToBool(nextLine[m]));
            }
        }
        final int m2 = idarray[AccessCombinations.accessEditresource.getIndex()];
        if (-1 != m2) {
            if (nextLine[m2].length() > 0) {
                moduleaccess.setEditResource(stringToBool(nextLine[m2]));
            }
        }
        final int n = idarray[AccessCombinations.accessForum.getIndex()];
        if (-1 != n) {
            if (nextLine[n].length() > 0) {
                moduleaccess.setForum(stringToBool(nextLine[n]));
            }
        }
        final int n2 = idarray[AccessCombinations.accessIcal.getIndex()];
        if (-1 != n2) {
            if (nextLine[n2].length() > 0) {
                moduleaccess.setIcal(stringToBool(nextLine[n2]));
            }
        }
        final int o = idarray[AccessCombinations.accessInfostore.getIndex()];
        if (-1 != o) {
            if (nextLine[o].length() > 0) {
                moduleaccess.setInfostore(stringToBool(nextLine[o]));
            }
        }
        final int o2 = idarray[AccessCombinations.accessMultiplemailaccounts.getIndex()];
        if (-1 != o2) {
            if (nextLine[o2].length() > 0) {
                moduleaccess.setMultipleMailAccounts(stringToBool(nextLine[o2]));
            }
        }
        final int p = idarray[AccessCombinations.accessPinboardWrite.getIndex()];
        if (-1 != p) {
            if (nextLine[p].length() > 0) {
                moduleaccess.setPinboardWrite(stringToBool(nextLine[p]));
            }
        }
        final int p2 = idarray[AccessCombinations.accessProjects.getIndex()];
        if (-1 != p2) {
            if (nextLine[p2].length() > 0) {
                moduleaccess.setProjects(stringToBool(nextLine[p2]));
            }
        }
        final int q = idarray[AccessCombinations.accessPublication.getIndex()];
        if (-1 != q) {
            if (nextLine[q].length() > 0) {
                moduleaccess.setPublication(stringToBool(nextLine[q]));
            }
        }
        final int q2 = idarray[AccessCombinations.accessReadCreateSharedFolders.getIndex()];
        if (-1 != q2) {
            if (nextLine[q2].length() > 0) {
                moduleaccess.setReadCreateSharedFolders(stringToBool(nextLine[q2]));
            }
        }
        final int r = idarray[AccessCombinations.accessRssBookmarks.getIndex()];
        if (-1 != r) {
            if (nextLine[r].length() > 0) {
                moduleaccess.setRssBookmarks(stringToBool(nextLine[r]));
            }
        }
        final int r2 = idarray[AccessCombinations.accessRssPortal.getIndex()];
        if (-1 != r2) {
            if (nextLine[r2].length() > 0) {
                moduleaccess.setRssPortal(stringToBool(nextLine[r2]));
            }
        }
        final int s = idarray[AccessCombinations.accessSubscription.getIndex()];
        if (-1 != s) {
            if (nextLine[s].length() > 0) {
                moduleaccess.setSubscription(stringToBool(nextLine[s]));
            }
        }
        final int s2 = idarray[AccessCombinations.accessSyncML.getIndex()];
        if (-1 != s2) {
            if (nextLine[s2].length() > 0) {
                moduleaccess.setSyncml(stringToBool(nextLine[s2]));
            }
        }
        final int t = idarray[AccessCombinations.accessTasks.getIndex()];
        if (-1 != t) {
            if (nextLine[t].length() > 0) {
                moduleaccess.setTasks(stringToBool(nextLine[t]));
            }
        }
        final int t2 = idarray[AccessCombinations.accessUsm.getIndex()];
        if (-1 != t2) {
            if (nextLine[t2].length() > 0) {
                moduleaccess.setUSM(stringToBool(nextLine[t2]));
            }
        }
        final int u = idarray[AccessCombinations.accessUsm.getIndex()];
        if (-1 != u) {
            if (nextLine[u].length() > 0) {
                moduleaccess.setUSM(stringToBool(nextLine[u]));
            }
        }
        final int u2 = idarray[AccessCombinations.accessVcard.getIndex()];
        if (-1 != u2) {
            if (nextLine[u2].length() > 0) {
                moduleaccess.setVcard(stringToBool(nextLine[u2]));
            }
        }
        final int v = idarray[AccessCombinations.accessWebdav.getIndex()];
        if (-1 != v) {
            if (nextLine[v].length() > 0) {
                moduleaccess.setWebdav(stringToBool(nextLine[v]));
            }
        }
        final int v2 = idarray[AccessCombinations.accessWebdavxml.getIndex()];
        if (-1 != v2) {
            if (nextLine[v2].length() > 0) {
                moduleaccess.setWebdavXml(stringToBool(nextLine[v2]));
            }
        }
        final int w = idarray[AccessCombinations.accessWebmail.getIndex()];
        if (-1 != w) {
            if (nextLine[w].length() > 0) {
                moduleaccess.setWebmail(stringToBool(nextLine[w]));
            }
        }
        return moduleaccess;
    }

    protected static boolean stringToBool(String string) {
        return "yes".equals(string) || "true".equals(string);
    }

    protected static Credentials getCreds(final String[] nextLine, final int[] idarray) {
        final Credentials credentials = new Credentials();
        final int i = idarray[Constants.adminuser.getIndex()];
        if (-1 != i) {
            if (nextLine[i].length() > 0) {
                credentials.setLogin(nextLine[i]);
            }
        }
        final int j = idarray[Constants.adminpass.getIndex()];
        if (-1 != j) {
            if (nextLine[j].length() > 0) {
                credentials.setPassword(nextLine[j]);
            }
        }
        return credentials;
    }

    protected static Context getContext(final String[] nextLine, final int[] idarray) {
        final Context context = new Context();
        final int i = idarray[Constants.CONTEXTID.getIndex()];
        if (-1 != i) {
            if (nextLine[i].length() > 0) {
                context.setId(Integer.parseInt(nextLine[i]));
            }
        }
        
        return context;
    }

    protected static User getUser(String[] nextLine, int[] idarray) throws InvalidDataException, ParseException {
        final User user = new User();
        final int i = idarray[Constants.USERNAME.getIndex()];
        if (-1 != i) {
            if (nextLine[i].length() > 0) {
                user.setName(nextLine[i]);
            }
        }
        final int j = idarray[Constants.PASSWORD.getIndex()];
        if (-1 != j) {
            if (nextLine[j].length() > 0) {
                user.setPassword(nextLine[j]);
            }
        }
        final int j2 = idarray[Constants.EMAIL.getIndex()];
        if (-1 != j2) {
            if (nextLine[j2].length() > 0) {
                user.setPrimaryEmail(nextLine[j2]);
                user.setEmail1(nextLine[j2]);
            }
        }
        final int k = idarray[Constants.DISPLAYNAME.getIndex()];
        if (-1 != k) {
            if (nextLine[k].length() > 0) {
                user.setDisplay_name(nextLine[k]);
            }
        }
        final int k2 = idarray[Constants.SURNAME.getIndex()];
        if (-1 != k2) {
            if (nextLine[k2].length() > 0) {
                user.setSur_name(nextLine[k2]);
            }
        }
        final int l = idarray[Constants.GIVENNAME.getIndex()];
        if (-1 != l) {
            if (nextLine[l].length() > 0) {
                user.setGiven_name(nextLine[l]);
            }
        }
        final int l2 = idarray[Constants.LANGUAGE.getIndex()];
        if (-1 != l2) {
            if (nextLine[l2].length() > 0) {
                user.setLanguage(nextLine[l2]);
            }
        }
        final int m = idarray[Constants.timezone.getIndex()];
        if (-1 != m) {
            if (nextLine[m].length() > 0) {
                user.setTimezone(nextLine[m]);
            }
        }
        final int m3 = idarray[Constants.THEME.getIndex()];
        if (-1 != m3) {
            if (nextLine[m3].length() > 0) {
                String addguival = nextLine[m3].trim();
                if( addguival.length() == 0 ) {
                    throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (empty value)");
                }
                if( ! addguival.contains("=") ) {
                    throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (not key = value)");
                }
                final int idx = addguival.indexOf("=");
                final String key = addguival.substring(0, idx).trim();
                final String val = addguival.substring(idx+1, addguival.length()).trim();
                if(key.length() == 0 || val.length() == 0) {
                    throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (key or val empty)");
                }
                user.addGuiPreferences(key, val);
            }
        }
        final int m4 = idarray[Constants.EMAIL1.getIndex()];
        if (-1 != m4) {
            if (nextLine[m4].length() > 0) {
                user.setEmail1(nextLine[m4]);
            }
        }
        final int m5 = idarray[Constants.mailenabled.getIndex()];
        if (-1 != m5) {
            if (nextLine[m5].length() > 0) {
                user.setMailenabled(stringToBool(nextLine[m5]));
            }
        }
        final int m6 = idarray[Constants.birthday.getIndex()];
        if (-1 != m6) {
            if (nextLine[m6].length() > 0) {
                final Date stringToDate = stringToDate(nextLine[m6]);
                if (null != stringToDate) {
                    user.setBirthday(stringToDate);
                }
            }
        }
        final int m7 = idarray[Constants.anniversary.getIndex()];
        if (-1 != m7) {
            if (nextLine[m7].length() > 0) {
                final Date stringToDate = stringToDate(nextLine[m7]);
                if (null != stringToDate) {
                    user.setAnniversary(stringToDate);
                }
            }
        }
        final int m8 = idarray[Constants.branches.getIndex()];
        if (-1 != m8) {
            if (nextLine[m8].length() > 0) {
                user.setBranches(nextLine[m8]);
            }
        }
        final int m9 = idarray[Constants.business_category.getIndex()];
        if (-1 != m9) {
            if (nextLine[m9].length() > 0) {
                user.setBusiness_category(nextLine[m9]);
            }
        }
        final int n = idarray[Constants.postal_code_business.getIndex()];
        if (-1 != n) {
            if (nextLine[n].length() > 0) {
                user.setPostal_code_business(nextLine[n]);
            }
        }
        final int n2 = idarray[Constants.state_business.getIndex()];
        if (-1 != n2) {
            if (nextLine[n2].length() > 0) {
                user.setState_business(nextLine[n2]);
            }
        }
        final int n3 = idarray[Constants.street_business.getIndex()];
        if (-1 != n3) {
            if (nextLine[n3].length() > 0) {
                user.setStreet_business(nextLine[n3]);
            }
        }
        final int n4 = idarray[Constants.telephone_callback.getIndex()];
        if (-1 != n4) {
            if (nextLine[n4].length() > 0) {
                user.setTelephone_callback(nextLine[n4]);
            }
        }
        final int n5 = idarray[Constants.city_home.getIndex()];
        if (-1 != n5) {
            if (nextLine[n5].length() > 0) {
                user.setCity_home(nextLine[n5]);
            }
        }
        final int n6 = idarray[Constants.commercial_register.getIndex()];
        if (-1 != n6) {
            if (nextLine[n6].length() > 0) {
                user.setCommercial_register(nextLine[n6]);
            }
        }
        final int n7 = idarray[Constants.country_home.getIndex()];
        if (-1 != n7) {
            if (nextLine[n7].length() > 0) {
                user.setCountry_home(nextLine[n7]);
            }
        }
        final int n8 = idarray[Constants.country_home.getIndex()];
        if (-1 != n8) {
            if (nextLine[n8].length() > 0) {
                user.setCountry_home(nextLine[n8]);
            }
        }
        final int n9 = idarray[Constants.email2.getIndex()];
        if (-1 != n9) {
            if (nextLine[n9].length() > 0) {
                user.setEmail2(nextLine[n9]);
            }
        }
        final int o = idarray[Constants.email3.getIndex()];
        if (-1 != o) {
            if (nextLine[o].length() > 0) {
                user.setEmail3(nextLine[o]);
            }
        }
        final int o2 = idarray[Constants.employeetype.getIndex()];
        if (-1 != o2) {
            if (nextLine[o2].length() > 0) {
                user.setEmployeeType(nextLine[o2]);
            }
        }
        final int o3 = idarray[Constants.fax_business.getIndex()];
        if (-1 != o3) {
            if (nextLine[o3].length() > 0) {
                user.setFax_business(nextLine[o3]);
            }
        }
        final int o4 = idarray[Constants.fax_home.getIndex()];
        if (-1 != o4) {
            if (nextLine[o4].length() > 0) {
                user.setFax_home(nextLine[o4]);
            }
        }
        final int o5 = idarray[Constants.fax_other.getIndex()];
        if (-1 != o5) {
            if (nextLine[o5].length() > 0) {
                user.setFax_other(nextLine[o5]);
            }
        }
        final int o6 = idarray[Constants.imapserver.getIndex()];
        if (-1 != o6) {
            if (nextLine[o6].length() > 0) {
                user.setImapServer(nextLine[o6]);
            }
        }
        final int o7 = idarray[Constants.imaplogin.getIndex()];
        if (-1 != o7) {
            if (nextLine[o7].length() > 0) {
                user.setImapLogin(nextLine[o7]);
            }
        }
        final int o8 = idarray[Constants.smtpserver.getIndex()];
        if (-1 != o8) {
            if (nextLine[o8].length() > 0) {
                user.setSmtpServer(nextLine[o8]);
            }
        }
        final int o9 = idarray[Constants.instant_messenger1.getIndex()];
        if (-1 != o9) {
            if (nextLine[o9].length() > 0) {
                user.setInstant_messenger1(nextLine[o9]);
            }
        }
        final int p = idarray[Constants.instant_messenger2.getIndex()];
        if (-1 != p) {
            if (nextLine[p].length() > 0) {
                user.setInstant_messenger2(nextLine[p]);
            }
        }
        final int p2 = idarray[Constants.telephone_ip.getIndex()];
        if (-1 != p2) {
            if (nextLine[p2].length() > 0) {
                user.setTelephone_ip(nextLine[p2]);
            }
        }
        final int p3 = idarray[Constants.telephone_isdn.getIndex()];
        if (-1 != p3) {
            if (nextLine[p3].length() > 0) {
                user.setTelephone_isdn(nextLine[p3]);
            }
        }
        final int p4 = idarray[Constants.mail_folder_drafts_name.getIndex()];
        if (-1 != p4) {
            if (nextLine[p4].length() > 0) {
                user.setMail_folder_drafts_name(nextLine[p4]);
            }
        }
        final int p5 = idarray[Constants.mail_folder_sent_name.getIndex()];
        if (-1 != p5) {
            if (nextLine[p5].length() > 0) {
                user.setMail_folder_sent_name(nextLine[p5]);
            }
        }
        final int p6 = idarray[Constants.mail_folder_spam_name.getIndex()];
        if (-1 != p6) {
            if (nextLine[p6].length() > 0) {
                user.setMail_folder_spam_name(nextLine[p6]);
            }
        }
        final int p7 = idarray[Constants.mail_folder_trash_name.getIndex()];
        if (-1 != p7) {
            if (nextLine[p7].length() > 0) {
                user.setMail_folder_trash_name(nextLine[p7]);
            }
        }
        final int p8 = idarray[Constants.manager_name.getIndex()];
        if (-1 != p8) {
            if (nextLine[p8].length() > 0) {
                user.setManager_name(nextLine[p8]);
            }
        }
        final int p9 = idarray[Constants.marital_status.getIndex()];
        if (-1 != p9) {
            if (nextLine[p9].length() > 0) {
                user.setMarital_status(nextLine[p9]);
            }
        }
        final int q = idarray[Constants.cellular_telephone1.getIndex()];
        if (-1 != q) {
            if (nextLine[q].length() > 0) {
                user.setCellular_telephone1(nextLine[q]);
            }
        }
        final int q2 = idarray[Constants.cellular_telephone2.getIndex()];
        if (-1 != q2) {
            if (nextLine[q2].length() > 0) {
                user.setCellular_telephone2(nextLine[q2]);
            }
        }
        final int q3 = idarray[Constants.info.getIndex()];
        if (-1 != q3) {
            if (nextLine[q3].length() > 0) {
                user.setInfo(nextLine[q3]);
            }
        }
        final int q4 = idarray[Constants.nickname.getIndex()];
        if (-1 != q4) {
            if (nextLine[q4].length() > 0) {
                user.setNickname(nextLine[q4]);
            }
        }
        final int q5 = idarray[Constants.number_of_children.getIndex()];
        if (-1 != q5) {
            if (nextLine[q5].length() > 0) {
                user.setNumber_of_children(nextLine[q5]);
            }
        }
        final int q6 = idarray[Constants.note.getIndex()];
        if (-1 != q6) {
            if (nextLine[q6].length() > 0) {
                user.setNote(nextLine[q6]);
            }
        }
        final int q7 = idarray[Constants.number_of_employee.getIndex()];
        if (-1 != q7) {
            if (nextLine[q7].length() > 0) {
                user.setNumber_of_employee(nextLine[q7]);
            }
        }
        final int q8 = idarray[Constants.telephone_pager.getIndex()];
        if (-1 != q8) {
            if (nextLine[q8].length() > 0) {
                user.setTelephone_pager(nextLine[q8]);
            }
        }
        final int q9 = idarray[Constants.password_expired.getIndex()];
        if (-1 != q9) {
            if (nextLine[q9].length() > 0) {
                user.setPassword_expired(stringToBool(nextLine[q9]));
            }
        }
        final int r = idarray[Constants.telephone_assistant.getIndex()];
        if (-1 != r) {
            if (nextLine[r].length() > 0) {
                user.setTelephone_assistant(nextLine[r]);
            }
        }
        final int r2 = idarray[Constants.telephone_business1.getIndex()];
        if (-1 != r2) {
            if (nextLine[r2].length() > 0) {
                user.setTelephone_business1(nextLine[r2]);
            }
        }
        final int r3 = idarray[Constants.telephone_business2.getIndex()];
        if (-1 != r3) {
            if (nextLine[r3].length() > 0) {
                user.setTelephone_business2(nextLine[r3]);
            }
        }
        final int r4 = idarray[Constants.telephone_car.getIndex()];
        if (-1 != r4) {
            if (nextLine[r4].length() > 0) {
                user.setTelephone_car(nextLine[r4]);
            }
        }
        final int r5 = idarray[Constants.telephone_company.getIndex()];
        if (-1 != r5) {
            if (nextLine[r5].length() > 0) {
                user.setTelephone_company(nextLine[r5]);
            }
        }
        final int r6 = idarray[Constants.telephone_home1.getIndex()];
        if (-1 != r6) {
            if (nextLine[r6].length() > 0) {
                user.setTelephone_home1(nextLine[r6]);
            }
        }
        final int r7 = idarray[Constants.telephone_home2.getIndex()];
        if (-1 != r7) {
            if (nextLine[r7].length() > 0) {
                user.setTelephone_home2(nextLine[r7]);
            }
        }
        final int r8 = idarray[Constants.telephone_other.getIndex()];
        if (-1 != r8) {
            if (nextLine[r8].length() > 0) {
                user.setTelephone_other(nextLine[r8]);
            }
        }
        final int r9 = idarray[Constants.position.getIndex()];
        if (-1 != r9) {
            if (nextLine[r9].length() > 0) {
                user.setPosition(nextLine[r9]);
            }
        }
        final int s = idarray[Constants.postal_code_home.getIndex()];
        if (-1 != s) {
            if (nextLine[s].length() > 0) {
                user.setPostal_code_home(nextLine[s]);
            }
        }
        final int s1 = idarray[Constants.profession.getIndex()];
        if (-1 != s1) {
            if (nextLine[s1].length() > 0) {
                user.setProfession(nextLine[s1]);
            }
        }
        final int s2 = idarray[Constants.telephone_radio.getIndex()];
        if (-1 != s2) {
            if (nextLine[s2].length() > 0) {
                user.setTelephone_radio(nextLine[s2]);
            }
        }
        final int s3 = idarray[Constants.room_number.getIndex()];
        if (-1 != s3) {
            if (nextLine[s3].length() > 0) {
                user.setRoom_number(nextLine[s3]);
            }
        }
        final int s4 = idarray[Constants.sales_volume.getIndex()];
        if (-1 != s4) {
            if (nextLine[s4].length() > 0) {
                user.setSales_volume(nextLine[s4]);
            }
        }
        final int s5 = idarray[Constants.city_other.getIndex()];
        if (-1 != s5) {
            if (nextLine[s5].length() > 0) {
                user.setCity_other(nextLine[s5]);
            }
        }
        final int s6 = idarray[Constants.country_other.getIndex()];
        if (-1 != s6) {
            if (nextLine[s6].length() > 0) {
                user.setCountry_other(nextLine[s6]);
            }
        }
        final int s7 = idarray[Constants.middle_name.getIndex()];
        if (-1 != s7) {
            if (nextLine[s7].length() > 0) {
                user.setMiddle_name(nextLine[s7]);
            }
        }
        final int s8 = idarray[Constants.postal_code_other.getIndex()];
        if (-1 != s8) {
            if (nextLine[s8].length() > 0) {
                user.setPostal_code_other(nextLine[s8]);
            }
        }
        final int s9 = idarray[Constants.state_other.getIndex()];
        if (-1 != s9) {
            if (nextLine[s9].length() > 0) {
                user.setState_other(nextLine[s9]);
            }
        }
        final int t = idarray[Constants.street_other.getIndex()];
        if (-1 != t) {
            if (nextLine[t].length() > 0) {
                user.setStreet_other(nextLine[t]);
            }
        }
        final int t2 = idarray[Constants.spouse_name.getIndex()];
        if (-1 != t2) {
            if (nextLine[t2].length() > 0) {
                user.setSpouse_name(nextLine[t2]);
            }
        }
        final int t3 = idarray[Constants.state_home.getIndex()];
        if (-1 != t3) {
            if (nextLine[t3].length() > 0) {
                user.setState_home(nextLine[t3]);
            }
        }
        final int t4 = idarray[Constants.street_home.getIndex()];
        if (-1 != t4) {
            if (nextLine[t4].length() > 0) {
                user.setStreet_home(nextLine[t4]);
            }
        }
        final int t5 = idarray[Constants.suffix.getIndex()];
        if (-1 != t5) {
            if (nextLine[t5].length() > 0) {
                user.setSuffix(nextLine[t5]);
            }
        }
        final int t6 = idarray[Constants.tax_id.getIndex()];
        if (-1 != t6) {
            if (nextLine[t6].length() > 0) {
                user.setTax_id(nextLine[t6]);
            }
        }
        final int t7 = idarray[Constants.telephone_telex.getIndex()];
        if (-1 != t7) {
            if (nextLine[t7].length() > 0) {
                user.setTelephone_telex(nextLine[t7]);
            }
        }
        final int t8 = idarray[Constants.title.getIndex()];
        if (-1 != t8) {
            if (nextLine[t8].length() > 0) {
                user.setTitle(nextLine[t8]);
            }
        }
        final int t9 = idarray[Constants.telephone_ttytdd.getIndex()];
        if (-1 != t9) {
            if (nextLine[t9].length() > 0) {
                user.setTelephone_ttytdd(nextLine[t9]);
            }
        }
        final int u = idarray[Constants.UPLOADFILESIZELIMIT.getIndex()];
        if (-1 != u) {
            if (nextLine[u].length() > 0) {
                user.setUploadFileSizeLimit(Integer.valueOf(nextLine[u]));
            }
        }
        final int u2 = idarray[Constants.uploadfilesizelimitperfile.getIndex()];
        if (-1 != u2) {
            if (nextLine[u2].length() > 0) {
                user.setUploadFileSizeLimitPerFile(Integer.valueOf(nextLine[u2]));
            }
        }
        final int u3 = idarray[Constants.url.getIndex()];
        if (-1 != u3) {
            if (nextLine[u3].length() > 0) {
                user.setUrl(nextLine[u3]);
            }
        }
        final int u5 = idarray[Constants.userfield01.getIndex()];
        if (-1 != u5) {
            if (nextLine[u5].length() > 0) {
                user.setUserfield01(nextLine[u5]);
            }
        }
        final int u6 = idarray[Constants.userfield02.getIndex()];
        if (-1 != u6) {
            if (nextLine[u6].length() > 0) {
                user.setUserfield02(nextLine[u6]);
            }
        }
        final int u7 = idarray[Constants.userfield03.getIndex()];
        if (-1 != u7) {
            if (nextLine[u7].length() > 0) {
                user.setUserfield03(nextLine[u7]);
            }
        }
        final int u8 = idarray[Constants.userfield04.getIndex()];
        if (-1 != u8) {
            if (nextLine[u8].length() > 0) {
                user.setUserfield04(nextLine[u8]);
            }
        }
        final int u9 = idarray[Constants.userfield05.getIndex()];
        if (-1 != u9) {
            if (nextLine[u9].length() > 0) {
                user.setUserfield05(nextLine[u9]);
            }
        }
        final int v = idarray[Constants.userfield06.getIndex()];
        if (-1 != v) {
            if (nextLine[v].length() > 0) {
                user.setUserfield06(nextLine[v]);
            }
        }
        final int v1 = idarray[Constants.userfield07.getIndex()];
        if (-1 != v1) {
            if (nextLine[v1].length() > 0) {
                user.setUserfield07(nextLine[v1]);
            }
        }
        final int v2 = idarray[Constants.userfield08.getIndex()];
        if (-1 != v2) {
            if (nextLine[v2].length() > 0) {
                user.setUserfield08(nextLine[v2]);
            }
        }
        final int v3 = idarray[Constants.userfield09.getIndex()];
        if (-1 != v3) {
            if (nextLine[v3].length() > 0) {
                user.setUserfield09(nextLine[v3]);
            }
        }
        final int v4 = idarray[Constants.userfield10.getIndex()];
        if (-1 != v4) {
            if (nextLine[v4].length() > 0) {
                user.setUserfield10(nextLine[v4]);
            }
        }
        final int v5 = idarray[Constants.userfield11.getIndex()];
        if (-1 != v5) {
            if (nextLine[v5].length() > 0) {
                user.setUserfield11(nextLine[v5]);
            }
        }
        final int v6 = idarray[Constants.userfield12.getIndex()];
        if (-1 != v6) {
            if (nextLine[v6].length() > 0) {
                user.setUserfield12(nextLine[v6]);
            }
        }
        final int v7 = idarray[Constants.userfield13.getIndex()];
        if (-1 != v7) {
            if (nextLine[v7].length() > 0) {
                user.setUserfield13(nextLine[v7]);
            }
        }
        final int v8 = idarray[Constants.userfield14.getIndex()];
        if (-1 != v8) {
            if (nextLine[v8].length() > 0) {
                user.setUserfield14(nextLine[v8]);
            }
        }
        final int v9 = idarray[Constants.userfield15.getIndex()];
        if (-1 != v9) {
            if (nextLine[v9].length() > 0) {
                user.setUserfield15(nextLine[v9]);
            }
        }
        final int w = idarray[Constants.userfield16.getIndex()];
        if (-1 != w) {
            if (nextLine[w].length() > 0) {
                user.setUserfield16(nextLine[w]);
            }
        }
        final int w1 = idarray[Constants.userfield17.getIndex()];
        if (-1 != w1) {
            if (nextLine[w1].length() > 0) {
                user.setUserfield17(nextLine[w1]);
            }
        }
        final int w2 = idarray[Constants.userfield18.getIndex()];
        if (-1 != w2) {
            if (nextLine[w2].length() > 0) {
                user.setUserfield18(nextLine[w2]);
            }
        }
        final int w3 = idarray[Constants.userfield19.getIndex()];
        if (-1 != w3) {
            if (nextLine[w3].length() > 0) {
                user.setUserfield19(nextLine[w3]);
            }
        }
        final int w4 = idarray[Constants.userfield20.getIndex()];
        if (-1 != w4) {
            if (nextLine[w4].length() > 0) {
                user.setUserfield20(nextLine[w4]);
            }
        }
        final int w5 = idarray[Constants.city_business.getIndex()];
        if (-1 != w5) {
            if (nextLine[w5].length() > 0) {
                user.setCity_business(nextLine[w5]);
            }
        }
        final int w6 = idarray[Constants.assistant_name.getIndex()];
        if (-1 != w6) {
            if (nextLine[w6].length() > 0) {
                user.setAssistant_name(nextLine[w6]);
            }
        }
        final int w7 = idarray[Constants.telephone_primary.getIndex()];
        if (-1 != w7) {
            if (nextLine[w7].length() > 0) {
                user.setTelephone_primary(nextLine[w7]);
            }
        }
        final int w8 = idarray[Constants.categories.getIndex()];
        if (-1 != w8) {
            if (nextLine[w8].length() > 0) {
                user.setCategories(nextLine[w8]);
            }
        }
        final int w9 = idarray[Constants.PASSWORDMECH.getIndex()];
        if (-1 != w9) {
            if (nextLine[w9].length() > 0) {
                user.setPasswordMech(nextLine[w9]);
            }
        }
        final int x = idarray[Constants.mail_folder_confirmed_ham_name.getIndex()];
        if (-1 != x) {
            if (nextLine[x].length() > 0) {
                user.setMail_folder_confirmed_ham_name(nextLine[x]);
            }
        }
        final int x1 = idarray[Constants.mail_folder_confirmed_spam_name.getIndex()];
        if (-1 != x1) {
            if (nextLine[x1].length() > 0) {
                user.setMail_folder_confirmed_spam_name(nextLine[x1]);
            }
        }
        final int x2 = idarray[Constants.DEFAULTSENDERADDRESS.getIndex()];
        if (-1 != x2) {
            if (nextLine[x2].length() > 0) {
                user.setDefaultSenderAddress(nextLine[x2]);
            }
        }
        final int x3 = idarray[Constants.gui_spam_filter_capabilities_enabled.getIndex()];
        if (-1 != x3) {
            if (nextLine[x3].length() > 0) {
                user.setGui_spam_filter_enabled(stringToBool(nextLine[x3]));
            }
        }
        final int m2 = idarray[Constants.MAILALIAS.getIndex()];
        if (-1 != m2) {
            if (nextLine[m2].length() > 0) {
                final String string = nextLine[m2];
                final String[] split = string.split(",");
                final HashSet<String> aliases = new HashSet<String>(Arrays.asList(split));
                final String primaryEmail = user.getPrimaryEmail();
                if (null != primaryEmail) {
                    aliases.add(primaryEmail);
                }
                final String email1 = user.getEmail1();
                if (null != email1) {
                    aliases.add(email1);
                }
                user.setAliases(aliases);
            }
        }
    
    
        return user;
    }

    private static Date stringToDate(String string) throws java.text.ParseException {
        final SimpleDateFormat sdf = new SimpleDateFormat(COMMANDLINE_DATEFORMAT);
        sdf.setTimeZone(TimeZone.getTimeZone(COMMANDLINE_TIMEZONE));
        final Date value = sdf.parse(string);
        if (null != value) {
            return value;
        } else {
            return null;
        }
    
    }

    protected final void setIdOption(final AdminParser admp){
        this.idOption =  setShortLongOpt(admp,OPT_ID_SHORT,OPT_ID_LONG,"Id of the user", true, NeededQuadState.eitheror);
    }
    
    protected final void setUsernameOption(final AdminParser admp, final NeededQuadState needed) {
        this.userNameOption = setShortLongOpt(admp,OPT_USERNAME_SHORT,OPT_USERNAME_LONG,"Username of the user", true, needed);
    }
    
    protected final void setDisplayNameOption(final AdminParser admp, final NeededQuadState needed) {
        this.displayNameOption = setShortLongOpt(admp,OPT_DISPLAYNAME_SHORT,OPT_DISPLAYNAME_LONG,"Display name of the user", true, needed); 
    }
    
    protected final void setPasswordOption(final AdminParser admp, final NeededQuadState needed) {
        this.passwordOption =  setShortLongOpt(admp,OPT_PASSWORD_SHORT,OPT_PASSWORD_LONG,"Password for the user", true, needed); 
    }
    
    protected final void setGivenNameOption(final AdminParser admp, final NeededQuadState needed) {
        this.givenNameOption =  setShortLongOpt(admp,OPT_GIVENNAME_SHORT,OPT_GIVENNAME_LONG,"Given name for the user", true, needed); 
    }
    
    protected final void setSurNameOption(final AdminParser admp, final NeededQuadState needed){
        this.surNameOption =  setShortLongOpt(admp,OPT_SURNAME_SHORT,OPT_SURNAME_LONG,"Sur name for the user", true, needed); 
    }
    
    protected final void setLanguageOption(final AdminParser admp){
        this.languageOption =  setShortLongOpt(admp,OPT_LANGUAGE_SHORT,OPT_LANGUAGE_LONG,"Language for the user (de_DE,en_US)", true, NeededQuadState.notneeded); 
    }
    
    protected final void setTimezoneOption(final AdminParser admp){
        this.timezoneOption =  setShortLongOpt(admp,OPT_TIMEZONE_SHORT,OPT_TIMEZONE_LONG,"Timezone of the user (Europe/Berlin)", true, NeededQuadState.notneeded); 
    }
    
    protected final void setPrimaryMailOption(final AdminParser admp, final NeededQuadState needed){
        this.primaryMailOption =  setShortLongOpt(admp,OPT_PRIMARY_EMAIL_SHORT,OPT_PRIMARY_EMAIL_LONG,"Primary mail address", true, needed); 
    }
    
    protected final void setDepartmentOption(final AdminParser admp){
        this.departmentOption = setShortLongOpt(admp,OPT_DEPARTMENT_SHORT,OPT_DEPARTMENT_LONG,"Department of the user", true, NeededQuadState.notneeded); 
    }
    
    protected final void setCompanyOption(final AdminParser admp){
        this.companyOption = setShortLongOpt(admp,OPT_COMPANY_SHORT,OPT_COMPANY_LONG,"Company of the user", true, NeededQuadState.notneeded); 
    }

    protected void setAddAccessRightCombinationNameOption(final AdminParser parser, final boolean required) {
        this.accessRightsCombinationName = setLongOpt(parser,OPT_ACCESSRIGHTS_COMBINATION_NAME,"Access combination name", true, false,false);
    }

    protected final void setAliasesOption(final AdminParser admp){
        this.aliasesOption = setShortLongOpt(admp,OPT_ALIASES_SHORT,OPT_ALIASES_LONG,"Email aliases of the user", true, NeededQuadState.notneeded); 
    }
    
    protected final void setImapOnlyOption(final AdminParser admp){
        this.imapOnlyOption =  setLongOpt(admp,OPT_IMAPONLY_LONG,"Do this operation only for the IMAP account of the user", false, false); 
    }
    
    protected final void setDBOnlyOption(final AdminParser admp){
        this.dbOnlyOption =  setLongOpt(admp,OPT_DBONLY_LONG,"Do this operation only in Database system (parameters which apply to extensions will be ignored)", false, false); 
    }
    
    protected final void setAddGuiSettingOption(final AdminParser admp){
        this.addGUISettingOption = setLongOpt(admp,OPT_ADD_GUI_SETTING_LONG,"Add a GUI setting (key=value)", true, false);
    }

    protected final void setRemoveGuiSettingOption(final AdminParser admp){
        this.removeGUISettingOption = setLongOpt(admp,OPT_REMOVE_GUI_SETTING_LONG,"Remove a GUI setting", true, false);
    }

    /**
     * @param theMethods
     * @param notallowedOrReplace Here we define the methods we don't want or want to replace. The name is the name of method without the prefix.
     * get or is. If the value of the map contains a string with length > 0, then this string will be used as columnname
     * @return
     */
    protected final ArrayList<MethodAndNames> getGetters(final Method[] theMethods, final Map<String, String> notallowedOrReplace) {
        // Define the returntypes we search for
        final HashSet<String> returntypes = new HashSet<String>();
        returntypes.add(JAVA_LANG_STRING);
        returntypes.add(JAVA_LANG_INTEGER);
        returntypes.add(JAVA_LANG_BOOLEAN);
        returntypes.add(JAVA_UTIL_DATE);
        returntypes.add(JAVA_UTIL_HASH_SET);
        returntypes.add(JAVA_UTIL_MAP);
        returntypes.add(JAVA_UTIL_TIME_ZONE);
        returntypes.add(JAVA_UTIL_LOCALE);
        returntypes.add(PASSWORDMECH_CLASS);
        returntypes.add(SIMPLE_INT);
        
        return getGetterGeneral(theMethods, notallowedOrReplace, returntypes);
    }

    private final ArrayList<MethodAndNames> getGetterGeneral(final Method[] theMethods, final Map<String, String> notallowedOrReplace, final HashSet<String> returntypes) {
        final ArrayList<MethodAndNames> retlist = new ArrayList<MethodAndNames>();
        // First we get all the getters of the user data class
        for (final Method method : theMethods) {
            // Getters shouldn't need parameters
            if(method.getParameterTypes().length > 0) {
                continue;
            }
            final String methodname = method.getName();
    
            if (methodname.startsWith("get")) {
                final String methodnamewithoutprefix = methodname.substring(3);
                if (!notallowedOrReplace.containsKey(methodnamewithoutprefix)) {
                    final String returntype = method.getReturnType().getName();
                    if (returntypes.contains(returntype)) {
                        retlist.add(new MethodAndNames(method, methodnamewithoutprefix, returntype));
                    }
                } else if (0 != notallowedOrReplace.get(methodnamewithoutprefix).length()) {
                    final String returntype = method.getReturnType().getName();
                    if (returntypes.contains(returntype)) {
                        retlist.add(new MethodAndNames(method, notallowedOrReplace.get(methodnamewithoutprefix), returntype));
                    }
                }
            } else if (methodname.startsWith("is")) {
                final String methodnamewithoutprefix = methodname.substring(2);
                if (!notallowedOrReplace.containsKey(methodnamewithoutprefix)) {
                    final String returntype = method.getReturnType().getName();
                    if (returntypes.contains(returntype)) {
                        retlist.add(new MethodAndNames(method, methodnamewithoutprefix, returntype));
                    }
                } else if (0 != notallowedOrReplace.get(methodnamewithoutprefix).length()) {
                    final String returntype = method.getReturnType().getName();
                    if (returntypes.contains(returntype)) {
                        retlist.add(new MethodAndNames(method, notallowedOrReplace.get(methodnamewithoutprefix), returntype));
                    }
                }
            }
        }
        return retlist;
    }
    
    public String parseAndSetAccessCombinationName(final AdminParser parser) {
        return (String) parser.getOptionValue(this.accessRightsCombinationName);
    }

    /**
     * Get the mandatory options from the command line and set's them in the user object
     * 
     * @param parser The parser object
     * @param usr User object which will be changed
     */
    protected final void parseAndSetMandatoryOptionsinUser(final AdminParser parser, final User usr) {
        parseAndSetUsername(parser, usr);
        parseAndSetMandatoryOptionsWithoutUsernameInUser(parser, usr);
    }

    protected void parseAndSetUsername(final AdminParser parser, final User usr) {
        this.username = (String) parser.getOptionValue(this.userNameOption);
        if (null != this.username) {
            usr.setName(this.username);
        }
    }

    protected final void parseAndSetMandatoryOptionsWithoutUsernameInUser(final AdminParser parser, final User usr) {
        String optionValue2 = (String) parser.getOptionValue(this.displayNameOption);
        if (null != optionValue2) {
            if ("".equals(optionValue2)) { optionValue2 = null; }
            usr.setDisplay_name(optionValue2);
        }        
        
        String optionValue3 = (String) parser.getOptionValue(this.givenNameOption);
        if (null != optionValue3) {
            if ("".equals(optionValue3)) { optionValue3 = null; }
            usr.setGiven_name(optionValue3);
        }
        
        String optionValue4 = (String) parser.getOptionValue(this.surNameOption);
        if (null != optionValue4) {
            if ("".equals(optionValue4)) { optionValue4 = null; }
            usr.setSur_name(optionValue4);
        }        

        String optionValue5 = null;
        if( NEW_USER_PASSWORD != null ) {
            optionValue5 = NEW_USER_PASSWORD;
        } else {
            optionValue5 = (String) parser.getOptionValue(this.passwordOption);
        }
        if (null != optionValue5) {
            usr.setPassword(optionValue5);
        }   

        final String optionValue6 = (String) parser.getOptionValue(this.primaryMailOption);
        if (null != optionValue6) {
            usr.setPrimaryEmail(optionValue6);
            usr.setEmail1(optionValue6);
        }        
    }
    
    /**
     * Apply module access rights given from command line to the given module access object. 
     * 
     * @param parser The parser object
     * @param usr User object which will be changed
     */
    protected final void setModuleAccessOptionsinUserCreate(final AdminParser parser, final UserModuleAccess access) {
        access.setCalendar(accessOption2BooleanCreate(parser,this.accessCalendarOption));
        access.setContacts(accessOption2BooleanCreate(parser,this.accessContactOption));
        access.setDelegateTask(accessOption2BooleanCreate(parser,this.accessDelegateTasksOption));
        access.setEditPublicFolders(accessOption2BooleanCreate(parser,this.accessEditPublicFolderOption));
        access.setForum(accessOption2BooleanCreate(parser,this.accessForumOption));
        access.setIcal(accessOption2BooleanCreate(parser,this.accessIcalOption));
        access.setInfostore(accessOption2BooleanCreate(parser,this.accessInfostoreOption));
        access.setPinboardWrite(accessOption2BooleanCreate(parser,this.accessPinboardWriteOption));
        access.setProjects(accessOption2BooleanCreate(parser,this.accessProjectsOption));
        access.setReadCreateSharedFolders(accessOption2BooleanCreate(parser,this.accessReadCreateSharedFolderOption));
        access.setRssBookmarks(accessOption2BooleanCreate(parser,this.accessRssBookmarkOption));
        access.setRssPortal(accessOption2BooleanCreate(parser,this.accessRssPortalOption));
        access.setSyncml(accessOption2BooleanCreate(parser,this.accessSyncmlOption));
        access.setTasks(accessOption2BooleanCreate(parser,this.accessTasksOption));
        access.setVcard(accessOption2BooleanCreate(parser,this.accessVcardOption));
        access.setWebdav(accessOption2BooleanCreate(parser,this.accessWebdavOption));
        access.setWebdavXml(accessOption2BooleanCreate(parser,this.accessWebdavXmlOption));
        access.setWebmail(accessOption2BooleanCreate(parser,this.accessWebmailOption));
        access.setEditGroup(accessOption2BooleanCreate(parser,this.accessEditGroupOption));
        access.setEditResource(accessOption2BooleanCreate(parser,this.accessEditResourceOption));
        access.setEditPassword(accessOption2BooleanCreate(parser,this.accessEditPasswordOption));
        access.setCollectEmailAddresses(accessOption2BooleanCreate(parser,this.accessCollectEmailAddresses));
        access.setMultipleMailAccounts(accessOption2BooleanCreate(parser,this.accessMultipleMailAccounts));
        access.setSubscription(accessOption2BooleanCreate(parser,this.accessSubscription));
        access.setPublication(accessOption2BooleanCreate(parser,this.accessPublication));
        access.setActiveSync(accessOption2BooleanCreate(parser,this.accessActiveSync));
        access.setUSM(accessOption2BooleanCreate(parser, this.accessUSM));
        access.setGlobalAddressBookDisabled(accessOption2BooleanCreate(parser, this.accessGAB));
        access.setPublicFolderEditable(accessOption2BooleanCreate(parser, this.accessPublicFolderEditable));
    }
    
    protected final boolean accessOption2BooleanCreate(final AdminParser parser,final CLIOption accessOption){
        // option was set, check what text was sent
        final String optionValue = (String) parser.getOptionValue(accessOption);
        if (optionValue == null) {
            // option was not set in create. we return true, because default is
            // on
            return true;
        }
        if (optionValue.trim().length() > 0 && optionValue.trim().equalsIgnoreCase("on")) {
            return true;
        }
        return false;
    }
    
    /**
     * @param parser
     * @param access
     * @return true if options have been specified, false if not
     */
    protected final boolean setModuleAccessOptions(final AdminParser parser, final UserModuleAccess access) {
        boolean changed = false;
        if ((String) parser.getOptionValue(this.accessCalendarOption) != null) {
            access.setCalendar(accessOption2BooleanCreate(parser, this.accessCalendarOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessContactOption) != null) {
            access.setContacts(accessOption2BooleanCreate(parser, this.accessContactOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessDelegateTasksOption) != null) {
            access.setDelegateTask(accessOption2BooleanCreate(parser, this.accessDelegateTasksOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessEditPublicFolderOption) != null) {
            access.setEditPublicFolders(accessOption2BooleanCreate(parser, this.accessEditPublicFolderOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessForumOption) != null) {
            access.setForum(accessOption2BooleanCreate(parser, this.accessForumOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessIcalOption) != null) {
            access.setIcal(accessOption2BooleanCreate(parser, this.accessIcalOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessInfostoreOption) != null) {
            access.setInfostore(accessOption2BooleanCreate(parser, this.accessInfostoreOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessPinboardWriteOption) != null) {
            access.setPinboardWrite(accessOption2BooleanCreate(parser, this.accessPinboardWriteOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessProjectsOption) != null) {
            access.setProjects(accessOption2BooleanCreate(parser, this.accessProjectsOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessReadCreateSharedFolderOption) != null) {
            access.setReadCreateSharedFolders(accessOption2BooleanCreate(parser, this.accessReadCreateSharedFolderOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessRssBookmarkOption) != null) {
            access.setRssBookmarks(accessOption2BooleanCreate(parser, this.accessRssBookmarkOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessRssPortalOption) != null) {
            access.setRssPortal(accessOption2BooleanCreate(parser, this.accessRssPortalOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessSyncmlOption) != null) {
            access.setSyncml(accessOption2BooleanCreate(parser, this.accessSyncmlOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessTasksOption) != null) {
            access.setTasks(accessOption2BooleanCreate(parser, this.accessTasksOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessVcardOption) != null) {
            access.setVcard(accessOption2BooleanCreate(parser, this.accessVcardOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessWebdavOption) != null) {
            access.setWebdav(accessOption2BooleanCreate(parser, this.accessWebdavOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessWebdavXmlOption) != null) {
            access.setWebdavXml(accessOption2BooleanCreate(parser, this.accessWebdavXmlOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessWebmailOption) != null) {
            access.setWebmail(accessOption2BooleanCreate(parser, this.accessWebmailOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessEditGroupOption) != null) {
            access.setEditGroup(accessOption2BooleanCreate(parser, this.accessEditGroupOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessEditResourceOption) != null) {
            access.setEditResource(accessOption2BooleanCreate(parser, this.accessEditResourceOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessEditPasswordOption) != null) {
            access.setEditPassword(accessOption2BooleanCreate(parser, this.accessEditPasswordOption));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessCollectEmailAddresses) != null) {
            access.setCollectEmailAddresses(accessOption2BooleanCreate(parser, this.accessCollectEmailAddresses));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessMultipleMailAccounts) != null) {
            access.setMultipleMailAccounts(accessOption2BooleanCreate(parser, this.accessMultipleMailAccounts));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessSubscription) != null) {
            access.setSubscription(accessOption2BooleanCreate(parser, this.accessSubscription));
            changed = true;
        }
        if ((String) parser.getOptionValue(this.accessPublication) != null) {
            access.setPublication(accessOption2BooleanCreate(parser, this.accessPublication));
            changed = true;
        }
        if((String) parser.getOptionValue(this.accessActiveSync) != null) {
        	access.setActiveSync(accessOption2BooleanCreate(parser, this.accessActiveSync));
        	changed = true;
        }
        if((String) parser.getOptionValue(this.accessUSM) != null) {
        	access.setUSM(accessOption2BooleanCreate(parser, this.accessUSM));
        	changed = true;
        }
        if((String) parser.getOptionValue(this.accessGAB) != null) {
            access.setGlobalAddressBookDisabled(accessOption2BooleanCreate(parser, this.accessGAB));
            changed = true;
        }
        if((String) parser.getOptionValue(this.accessPublicFolderEditable) != null) {
            access.setPublicFolderEditable(accessOption2BooleanCreate(parser, this.accessPublicFolderEditable));
            changed = true;
        }
        return changed;
    }
    
    protected final boolean accessOption2BooleanChange(final AdminParser parser, final CLIOption accessOption) {
        // option was set, check what text was sent
        final String optionValue = (String) parser.getOptionValue(accessOption);
        if (optionValue.trim().length() > 0 && optionValue.trim().equalsIgnoreCase("on")) {
            return true;
        }
        return false;
    }

    /**
     * Get the optional options from the command line and set's them in the user object
     * 
     * @param parser The parser object
     * @param usr User object which will be changed
     * @throws InvalidDataException 
     */
    protected final void parseAndSetOptionalOptionsinUser(final AdminParser parser, final User usr) throws InvalidDataException {
        final String optionValue = (String) parser.getOptionValue(this.companyOption);
        if (null != optionValue) {
            usr.setCompany(optionValue);
        }
    
        final String optionValue2 = (String) parser.getOptionValue(this.departmentOption);
        if (null != optionValue2) {
            usr.setDepartment(optionValue2);
        }
    
        final String optionValue3 = (String) parser.getOptionValue(this.languageOption);
        if (null != optionValue3) {
            usr.setLanguage(optionValue3);
        }
    
        final String optionValue4 = (String) parser.getOptionValue(this.timezoneOption);
        if (null != optionValue4) {
            if (!Arrays.asList(TimeZone.getAvailableIDs()).contains(optionValue4)) {
                throw new InvalidDataException("The given timezone is invalid");
            }
            usr.setTimezone(optionValue4);
        }
    
        final String aliasOpt = (String) parser.getOptionValue(this.aliasesOption);
        if (null != aliasOpt) {
            final HashSet<String> aliases = new HashSet<String>();
            for (final String alias : aliasOpt.split(",")) {
                aliases.add(alias.trim());
            }
            usr.setAliases(aliases);
        }
    }
    
    
    protected final void setModuleAccessOptions(final AdminParser admp) {
        // TODO: The default values should be dynamically generates from the setting in the core
        this.accessCalendarOption = setLongOpt(admp, OPT_ACCESS_CALENDAR,"on/off","Calendar module (Default is off)", true, false,true);
        this.accessContactOption = setLongOpt(admp, OPT_ACCESS_CONTACTS,"on/off","Contact module access (Default is on)", true, false,true);
        this.accessDelegateTasksOption = setLongOpt(admp, OPT_ACCESS_DELEGATE_TASKS,"on/off","Delegate tasks access (Default is off)", true, false,true);
        this.accessEditPublicFolderOption = setLongOpt(admp, OPT_ACCESS_EDIT_PUBLIC_FOLDERS,"on/off","Edit public folder access (Default is off)", true, false,true);
        this.accessForumOption = setLongOpt(admp, OPT_ACCESS_FORUM,"on/off","Forum module access (Default is off)", true, false,true);
        this.accessIcalOption = setLongOpt(admp, OPT_ACCESS_ICAL,"on/off","Ical module access (Default is off)", true, false,true);
        this.accessInfostoreOption = setLongOpt(admp, OPT_ACCESS_INFOSTORE,"on/off","Infostore module access (Default is off)", true, false,true);
        this.accessPinboardWriteOption = setLongOpt(admp, OPT_ACCESS_PINBOARD_WRITE,"on/off","Pinboard write access (Default is off)", true, false,true);
        this.accessProjectsOption = setLongOpt(admp, OPT_ACCESS_PROJECTS,"on/off","Project module access (Default is off)", true, false,true);
        this.accessReadCreateSharedFolderOption = setLongOpt(admp, OPT_ACCESS_READCREATE_SHARED_FOLDERS,"on/off","Read create shared folder access (Default is off)", true, false,true);
        this.accessRssBookmarkOption= setLongOpt(admp, OPT_ACCESS_RSS_BOOKMARKS,"on/off","RSS bookmarks access (Default is off)", true, false,true);
        this.accessRssPortalOption = setLongOpt(admp, OPT_ACCESS_RSS_PORTAL,"on/off","RSS portal access (Default is off)", true, false,true);
        this.accessSyncmlOption = setLongOpt(admp, OPT_ACCESS_SYNCML,"on/off","Syncml access (Default is off)", true, false,true);
        this.accessTasksOption = setLongOpt(admp, OPT_ACCESS_TASKS,"on/off","Tasks access (Default is off)", true, false,true);
        this.accessVcardOption = setLongOpt(admp, OPT_ACCESS_VCARD,"on/off","Vcard access (Default is off)", true, false,true);
        this.accessWebdavOption = setLongOpt(admp, OPT_ACCESS_WEBDAV,"on/off","Webdav access (Default is off)", true, false,true);
        this.accessWebdavXmlOption = setLongOpt(admp, OPT_ACCESS_WEBDAV_XML,"on/off","Webdav-Xml access (Default is off)", true, false,true);
        this.accessWebmailOption = setLongOpt(admp, OPT_ACCESS_WEBMAIL,"on/off","Webmail access (Default is on)", true, false,true);
        this.accessEditGroupOption = setLongOpt(admp, OPT_ACCESS_EDIT_GROUP,"on/off","Edit Group access (Default is off)", true, false,true);
        this.accessEditResourceOption = setLongOpt(admp, OPT_ACCESS_EDIT_RESOURCE,"on/off","Edit Resource access (Default is off)", true, false,true);
        this.accessEditPasswordOption = setLongOpt(admp, OPT_ACCESS_EDIT_PASSWORD,"on/off","Edit Password access (Default is off)", true, false,true);
        this.accessCollectEmailAddresses = setLongOpt(admp, OPT_ACCESS_COLLECT_EMAIL_ADDRESSES,"on/off","Collect Email Addresses access (Default is off)", true, false,true);
        this.accessMultipleMailAccounts = setLongOpt(admp, OPT_ACCESS_MULTIPLE_MAIL_ACCOUNTS,"on/off","Multiple Mail Accounts access (Default is off)", true, false,true);
        this.accessSubscription = setLongOpt(admp, OPT_ACCESS_SUBSCRIPTION,"on/off","Subscription access (Default is off)", true, false,true);
        this.accessPublication = setLongOpt(admp, OPT_ACCESS_PUBLICATION,"on/off","Publication access (Default is off)", true, false,true);
        this.accessActiveSync = setLongOpt(admp, OPT_ACCESS_ACTIVE_SYNC, "on/off", "Exchange Active Sync access (Default is off)", true, false, true);
        this.accessUSM = setLongOpt(admp, OPT_ACCESS_USM, "on/off", "Universal Sync access (Default is off)", true, false, true);
        this.accessGAB = setLongOpt(admp, OPT_DISABLE_GAB, "on/off", "Disable Global Address Book access (Default is off)", true, false, true);
        this.accessPublicFolderEditable = setLongOpt(admp, OPT_ACCESS_PUBLIC_FOLDER_EDITABLE, "on/off", "Whether public folder(s) is/are editable (Default is off). Applies only to context admin user.", true, false, true);
    }

    protected final void setMandatoryOptions(final AdminParser parser) {
        setUsernameOption(parser, NeededQuadState.needed);
        setMandatoryOptionsWithoutUsername(parser, NeededQuadState.needed);
    }

    protected void setMandatoryOptionsWithoutUsername(final AdminParser parser, final NeededQuadState needed) {
        setDisplayNameOption(parser, needed);
        setGivenNameOption(parser, needed);
        setSurNameOption(parser, needed);
        // if password of new user is supplied in environment, do not insist on password option
        if( NEW_USER_PASSWORD != null ) {
            setPasswordOption(parser, NeededQuadState.notneeded);
        } else {
            setPasswordOption(parser, needed);
        }
        setPrimaryMailOption(parser, needed);
    }

    protected final void setOptionalOptions(final AdminParser parser) {
        setLanguageOption(parser);
        setTimezoneOption(parser);
        setDepartmentOption(parser);
        setCompanyOption(parser);
        setAliasesOption(parser);
    }

    protected void setExtendedOptions(final AdminParser parser) {
        setAddGuiSettingOption(parser);
        if( this.getClass().getName().endsWith("Change") ) {
            setRemoveGuiSettingOption(parser);
        }
        
        this.email1Option = setLongOpt(parser, OPT_EMAIL1_LONG, "stringvalue", "Email1", true, false, true);
        this.mailenabledOption = setSettableBooleanLongOpt(parser, OPT_MAILENABLED_LONG, "true / false", "Mailenabled", true, false, true);
        this.birthdayOption = setLongOpt(parser, OPT_BIRTHDAY_LONG, "datevalue", "Birthday", true, false, true);
        this.anniversaryOption = setLongOpt(parser, OPT_ANNIVERSARY_LONG, "datevalue", "Anniversary", true, false, true);
        this.branchesOption = setLongOpt(parser, OPT_BRANCHES_LONG, "stringvalue", "Branches", true, false, true);
        this.business_categoryOption = setLongOpt(parser, OPT_BUSINESS_CATEGORY_LONG, "stringvalue", "Business_category", true, false, true);
        this.postal_code_businessOption = setLongOpt(parser, OPT_POSTAL_CODE_BUSINESS_LONG, "stringvalue", "Postal_code_business", true, false, true);
        this.state_businessOption = setLongOpt(parser, OPT_STATE_BUSINESS_LONG, "stringvalue", "State_business", true, false, true);
        this.street_businessOption = setLongOpt(parser, OPT_STREET_BUSINESS_LONG, "stringvalue", "Street_business", true, false, true);
        this.telephone_callbackOption = setLongOpt(parser, OPT_TELEPHONE_CALLBACK_LONG, "stringvalue", "Telephone_callback", true, false, true);
        this.city_homeOption = setLongOpt(parser, OPT_CITY_HOME_LONG, "stringvalue", "City_home", true, false, true);
        this.commercial_registerOption = setLongOpt(parser, OPT_COMMERCIAL_REGISTER_LONG, "stringvalue", "Commercial_register", true, false, true);
        this.country_homeOption = setLongOpt(parser, OPT_COUNTRY_HOME_LONG, "stringvalue", "Country_home", true, false, true);
        this.email2Option = setLongOpt(parser, OPT_EMAIL2_LONG, "stringvalue", "Email2", true, false, true);
        this.email3Option = setLongOpt(parser, OPT_EMAIL3_LONG, "stringvalue", "Email3", true, false, true);
        this.employeetypeOption = setLongOpt(parser, OPT_EMPLOYEETYPE_LONG, "stringvalue", "EmployeeType", true, false, true);
        this.fax_businessOption = setLongOpt(parser, OPT_FAX_BUSINESS_LONG, "stringvalue", "Fax_business", true, false, true);
        this.fax_homeOption = setLongOpt(parser, OPT_FAX_HOME_LONG, "stringvalue", "Fax_home", true, false, true);
        this.fax_otherOption = setLongOpt(parser, OPT_FAX_OTHER_LONG, "stringvalue", "Fax_other", true, false, true);
        this.imapserverOption = setLongOpt(parser, OPT_IMAPSERVER_LONG, "stringvalue", "ImapServer", true, false, true);
        this.imaploginOption = setLongOpt(parser, OPT_IMAPLOGIN_LONG, "stringvalue", "ImapLogin", true, false, true);
        this.smtpserverOption = setLongOpt(parser, OPT_SMTPSERVER_LONG, "stringvalue", "SmtpServer", true, false, true);
        this.instant_messenger1Option = setLongOpt(parser, OPT_INSTANT_MESSENGER1_LONG, "stringvalue", "Instant_messenger1", true, false, true);
        this.instant_messenger2Option = setLongOpt(parser, OPT_INSTANT_MESSENGER2_LONG, "stringvalue", "Instant_messenger2", true, false, true);
        this.telephone_ipOption = setLongOpt(parser, OPT_TELEPHONE_IP_LONG, "stringvalue", "Telephone_ip", true, false, true);
        this.telephone_isdnOption = setLongOpt(parser, OPT_TELEPHONE_ISDN_LONG, "stringvalue", "Telephone_isdn", true, false, true);
        this.mail_folder_drafts_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_DRAFTS_NAME_LONG, "stringvalue", "Mail_folder_drafts_name", true, false, true);
        this.mail_folder_sent_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_SENT_NAME_LONG, "stringvalue", "Mail_folder_sent_name", true, false, true);
        this.mail_folder_spam_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_SPAM_NAME_LONG, "stringvalue", "Mail_folder_spam_name", true, false, true);
        this.mail_folder_trash_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_TRASH_NAME_LONG, "stringvalue", "Mail_folder_trash_name", true, false, true);
        this.manager_nameOption = setLongOpt(parser, OPT_MANAGER_NAME_LONG, "stringvalue", "Manager_name", true, false, true);
        this.marital_statusOption = setLongOpt(parser, OPT_MARITAL_STATUS_LONG, "stringvalue", "Marital_status", true, false, true);
        this.cellular_telephone1Option = setLongOpt(parser, OPT_CELLULAR_TELEPHONE1_LONG, "stringvalue", "Cellular_telephone1", true, false, true);
        this.cellular_telephone2Option = setLongOpt(parser, OPT_CELLULAR_TELEPHONE2_LONG, "stringvalue", "Cellular_telephone2", true, false, true);
        this.infoOption = setLongOpt(parser, OPT_INFO_LONG, "stringvalue", "Info", true, false, true);
        this.nicknameOption = setLongOpt(parser, OPT_NICKNAME_LONG, "stringvalue", "Nickname", true, false, true);
        this.number_of_childrenOption = setLongOpt(parser, OPT_NUMBER_OF_CHILDREN_LONG, "stringvalue", "Number_of_children", true, false, true);
        this.noteOption = setLongOpt(parser, OPT_NOTE_LONG, "stringvalue", "Note", true, false, true);
        this.number_of_employeeOption = setLongOpt(parser, OPT_NUMBER_OF_EMPLOYEE_LONG, "stringvalue", "Number_of_employee", true, false, true);
        this.telephone_pagerOption = setLongOpt(parser, OPT_TELEPHONE_PAGER_LONG, "stringvalue", "Telephone_pager", true, false, true);
        this.password_expiredOption = setSettableBooleanLongOpt(parser, OPT_PASSWORD_EXPIRED_LONG, "true / false", "Password_expired", true, false, true);
        this.telephone_assistantOption = setLongOpt(parser, OPT_TELEPHONE_ASSISTANT_LONG, "stringvalue", "Telephone_assistant", true, false, true);
        this.telephone_business1Option = setLongOpt(parser, OPT_TELEPHONE_BUSINESS1_LONG, "stringvalue", "Telephone_business1", true, false, true);
        this.telephone_business2Option = setLongOpt(parser, OPT_TELEPHONE_BUSINESS2_LONG, "stringvalue", "Telephone_business2", true, false, true);
        this.telephone_carOption = setLongOpt(parser, OPT_TELEPHONE_CAR_LONG, "stringvalue", "Telephone_car", true, false, true);
        this.telephone_companyOption = setLongOpt(parser, OPT_TELEPHONE_COMPANY_LONG, "stringvalue", "Telephone_company", true, false, true);
        this.telephone_home1Option = setLongOpt(parser, OPT_TELEPHONE_HOME1_LONG, "stringvalue", "Telephone_home1", true, false, true);
        this.telephone_home2Option = setLongOpt(parser, OPT_TELEPHONE_HOME2_LONG, "stringvalue", "Telephone_home2", true, false, true);
        this.telephone_otherOption = setLongOpt(parser, OPT_TELEPHONE_OTHER_LONG, "stringvalue", "Telephone_other", true, false, true);
        this.positionOption = setLongOpt(parser, OPT_POSITION_LONG, "stringvalue", "Position", true, false, true);
        this.postal_code_homeOption = setLongOpt(parser, OPT_POSTAL_CODE_HOME_LONG, "stringvalue", "Postal_code_home", true, false, true);
        this.professionOption = setLongOpt(parser, OPT_PROFESSION_LONG, "stringvalue", "Profession", true, false, true);
        this.telephone_radioOption = setLongOpt(parser, OPT_TELEPHONE_RADIO_LONG, "stringvalue", "Telephone_radio", true, false, true);
        this.room_numberOption = setLongOpt(parser, OPT_ROOM_NUMBER_LONG, "stringvalue", "Room_number", true, false, true);
        this.sales_volumeOption = setLongOpt(parser, OPT_SALES_VOLUME_LONG, "stringvalue", "Sales_volume", true, false, true);
        this.city_otherOption = setLongOpt(parser, OPT_CITY_OTHER_LONG, "stringvalue", "City_other", true, false, true);
        this.country_otherOption = setLongOpt(parser, OPT_COUNTRY_OTHER_LONG, "stringvalue", "Country_other", true, false, true);
        this.middle_nameOption = setLongOpt(parser, OPT_MIDDLE_NAME_LONG, "stringvalue", "Middle_name", true, false, true);
        this.postal_code_otherOption = setLongOpt(parser, OPT_POSTAL_CODE_OTHER_LONG, "stringvalue", "Postal_code_other", true, false, true);
        this.state_otherOption = setLongOpt(parser, OPT_STATE_OTHER_LONG, "stringvalue", "State_other", true, false, true);
        this.street_otherOption = setLongOpt(parser, OPT_STREET_OTHER_LONG, "stringvalue", "Street_other", true, false, true);
        this.spouse_nameOption = setLongOpt(parser, OPT_SPOUSE_NAME_LONG, "stringvalue", "Spouse_name", true, false, true);
        this.state_homeOption = setLongOpt(parser, OPT_STATE_HOME_LONG, "stringvalue", "State_home", true, false, true);
        this.street_homeOption = setLongOpt(parser, OPT_STREET_HOME_LONG, "stringvalue", "Street_home", true, false, true);
        this.suffixOption = setLongOpt(parser, OPT_SUFFIX_LONG, "stringvalue", "Suffix", true, false, true);
        this.tax_idOption = setLongOpt(parser, OPT_TAX_ID_LONG, "stringvalue", "Tax_id", true, false, true);
        this.telephone_telexOption = setLongOpt(parser, OPT_TELEPHONE_TELEX_LONG, "stringvalue", "Telephone_telex", true, false, true);
        this.titleOption = setLongOpt(parser, OPT_TITLE_LONG, "stringvalue", "Title", true, false, true);
        this.telephone_ttytddOption = setLongOpt(parser, OPT_TELEPHONE_TTYTDD_LONG, "stringvalue", "Telephone_ttytdd", true, false, true);
        this.uploadfilesizelimitOption = setIntegerLongOpt(parser, OPT_UPLOADFILESIZELIMIT_LONG, "intvalue", "UploadFileSizeLimit", true, false, true);
        this.uploadfilesizelimitperfileOption = setIntegerLongOpt(parser, OPT_UPLOADFILESIZELIMITPERFILE_LONG, "intvalue", "UploadFileSizeLimitPerFile", true, false, true);
        this.urlOption = setLongOpt(parser, OPT_URL_LONG, "stringvalue", "Url", true, false, true);
        this.userfield01Option = setLongOpt(parser, OPT_USERFIELD01_LONG, "stringvalue", "Userfield01", true, false, true);
        this.userfield02Option = setLongOpt(parser, OPT_USERFIELD02_LONG, "stringvalue", "Userfield02", true, false, true);
        this.userfield03Option = setLongOpt(parser, OPT_USERFIELD03_LONG, "stringvalue", "Userfield03", true, false, true);
        this.userfield04Option = setLongOpt(parser, OPT_USERFIELD04_LONG, "stringvalue", "Userfield04", true, false, true);
        this.userfield05Option = setLongOpt(parser, OPT_USERFIELD05_LONG, "stringvalue", "Userfield05", true, false, true);
        this.userfield06Option = setLongOpt(parser, OPT_USERFIELD06_LONG, "stringvalue", "Userfield06", true, false, true);
        this.userfield07Option = setLongOpt(parser, OPT_USERFIELD07_LONG, "stringvalue", "Userfield07", true, false, true);
        this.userfield08Option = setLongOpt(parser, OPT_USERFIELD08_LONG, "stringvalue", "Userfield08", true, false, true);
        this.userfield09Option = setLongOpt(parser, OPT_USERFIELD09_LONG, "stringvalue", "Userfield09", true, false, true);
        this.userfield10Option = setLongOpt(parser, OPT_USERFIELD10_LONG, "stringvalue", "Userfield10", true, false, true);
        this.userfield11Option = setLongOpt(parser, OPT_USERFIELD11_LONG, "stringvalue", "Userfield11", true, false, true);
        this.userfield12Option = setLongOpt(parser, OPT_USERFIELD12_LONG, "stringvalue", "Userfield12", true, false, true);
        this.userfield13Option = setLongOpt(parser, OPT_USERFIELD13_LONG, "stringvalue", "Userfield13", true, false, true);
        this.userfield14Option = setLongOpt(parser, OPT_USERFIELD14_LONG, "stringvalue", "Userfield14", true, false, true);
        this.userfield15Option = setLongOpt(parser, OPT_USERFIELD15_LONG, "stringvalue", "Userfield15", true, false, true);
        this.userfield16Option = setLongOpt(parser, OPT_USERFIELD16_LONG, "stringvalue", "Userfield16", true, false, true);
        this.userfield17Option = setLongOpt(parser, OPT_USERFIELD17_LONG, "stringvalue", "Userfield17", true, false, true);
        this.userfield18Option = setLongOpt(parser, OPT_USERFIELD18_LONG, "stringvalue", "Userfield18", true, false, true);
        this.userfield19Option = setLongOpt(parser, OPT_USERFIELD19_LONG, "stringvalue", "Userfield19", true, false, true);
        this.userfield20Option = setLongOpt(parser, OPT_USERFIELD20_LONG, "stringvalue", "Userfield20", true, false, true);
        this.city_businessOption = setLongOpt(parser, OPT_CITY_BUSINESS_LONG, "stringvalue", "City_business", true, false, true);
        this.country_businessOption = setLongOpt(parser, OPT_COUNTRY_BUSINESS_LONG, "stringvalue", "Country_business", true, false, true);
        this.assistant_nameOption = setLongOpt(parser, OPT_ASSISTANT_NAME_LONG, "stringvalue", "Assistant_name", true, false, true);
        this.telephone_primaryOption = setLongOpt(parser, OPT_TELEPHONE_PRIMARY_LONG, "stringvalue", "Telephone_primary", true, false, true);
        this.categoriesOption = setLongOpt(parser, OPT_CATEGORIES_LONG, "stringvalue", "Categories", true, false, true);
        this.passwordmechOption = setLongOpt(parser, OPT_PASSWORDMECH_LONG, "stringvalue", "PasswordMech", true, false, true);
        this.mail_folder_confirmed_ham_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_CONFIRMED_HAM_NAME_LONG, "stringvalue", "Mail_folder_confirmed_ham_name", true, false, true);
        this.mail_folder_confirmed_spam_nameOption = setLongOpt(parser, OPT_MAIL_FOLDER_CONFIRMED_SPAM_NAME_LONG, "stringvalue", "Mail_folder_confirmed_spam_name", true, false, true);
        this.defaultsenderaddressOption = setLongOpt(parser, OPT_DEFAULTSENDERADDRESS_LONG, "stringvalue", "DefaultSenderAddress", true, false, true);
        this.foldertreeOption = setIntegerLongOpt(parser, OPT_FOLDERTREE_LONG, "intvalue", "FolderTree", true, false, true);
        
//        final Method[] methods = User.class.getMethods();
//        final ArrayList<MethodAndNames> methArrayList = getSetters(methods);
//    
//        for (final MethodAndNames methodandnames : methArrayList) {
//            if (!standardoptions.contains(methodandnames.getName().toLowerCase())) {
//                if (methodandnames.getReturntype().equals(JAVA_LANG_STRING)) {
//                    optionsandmethods.add(new OptionAndMethod(methodandnames.getMethod(), setLongOpt(parser, methodandnames.getName().toLowerCase(), "stringvalue", methodandnames.getName(), true, false, true), methodandnames.getReturntype()));
//                    System.err.println("this." + methodandnames.getName().toLowerCase() + "Option = setLongOpt(parser, OPT_" + methodandnames.getName().toUpperCase() + "_LONG, \"stringvalue\", \"" + methodandnames.getName() + "\", true, false, true);");
////                    System.err.println("protected static final String OPT_" + methodandnames.getName().toUpperCase() + "_LONG = \"" + methodandnames.getName().toLowerCase() + "\";");
//
////                    System.err.println("                {");
////                    System.err.println("                String value = (String)parser.getOptionValue(" + methodandnames.getName().toLowerCase() + "Option);");
////                    System.err.println("                if (null != value) {");
////                    System.err.println("                    // On the command line an empty string can be used to clear that specific attribute.");
////                    System.err.println("                    if (\"\".equals(value)) { value = null; }");
////                    System.err.println("                    usr." + methodandnames.getMethod().getName() + "(value);");
////                    System.err.println("                }");
////                    System.err.println("                }");
//                } else if (methodandnames.getReturntype().equals(JAVA_LANG_INTEGER)) {
//                    optionsandmethods.add(new OptionAndMethod(methodandnames.getMethod(), setIntegerLongOpt(parser, methodandnames.getName().toLowerCase(), "intvalue", methodandnames.getName(), true, false, true), methodandnames.getReturntype()));
//                    System.err.println("this." + methodandnames.getName().toLowerCase() + "Option = setIntegerLongOpt(parser, OPT_" + methodandnames.getName().toUpperCase() + "_LONG, \"intvalue\", \"" + methodandnames.getName() + "\", true, false, true);");
////                    System.err.println("protected static final String OPT_" + methodandnames.getName().toUpperCase() + "_LONG = \"" + methodandnames.getName().toLowerCase() + "\";");
//
////                    System.err.println("                {");
////                    System.err.println("                final Integer value = (Integer)parser.getOptionValue(" + methodandnames.getName().toLowerCase() + "Option);");
////                    System.err.println("                if (null != value) {");
////                    System.err.println("                    usr." + methodandnames.getMethod().getName() + "(value);");
////                    System.err.println("                }");
////                    System.err.println("                }");
//                } else if (methodandnames.getReturntype().equals(JAVA_LANG_BOOLEAN)) {
//                    optionsandmethods.add(new OptionAndMethod(methodandnames.getMethod(), setSettableBooleanLongOpt(parser, methodandnames.getName().toLowerCase(), "true / false", methodandnames.getName(), true, false, true), methodandnames.getReturntype()));
//                    System.err.println("this." + methodandnames.getName().toLowerCase() + "Option = setSettableBooleanLongOpt(parser, OPT_" + methodandnames.getName().toUpperCase() + "_LONG, \"true / false\", \"" + methodandnames.getName() + "\", true, false, true)");
////                    System.err.println("protected static final String OPT_" + methodandnames.getName().toUpperCase() + "_LONG = \"" + methodandnames.getName().toLowerCase() + "\";");
//
////                    System.err.println("                {");
////                    System.err.println("                final Boolean value = (Boolean)parser.getOptionValue(" + methodandnames.getName().toLowerCase() + "Option());");
////                    System.err.println("                if (null != value) {");
////                    System.err.println("                    usr." + methodandnames.getMethod().getName() + "(value);");
////                    System.err.println("                }");
////                    System.err.println("                }");
//                } else if (methodandnames.getReturntype().equals(JAVA_UTIL_DATE)) {
//                    optionsandmethods.add(new OptionAndMethod(methodandnames.getMethod(), setLongOpt(parser, methodandnames.getName().toLowerCase(), "datevalue", methodandnames.getName(), true, false, true), methodandnames.getReturntype()));
//                    System.err.println("this." + methodandnames.getName().toLowerCase() + "Option = setLongOpt(parser, OPT_" + methodandnames.getName().toUpperCase() + "_LONG, \"datevalue\", \"" + methodandnames.getName() + "\", true, false, true)");
////                    System.err.println("protected static final String OPT_" + methodandnames.getName().toUpperCase() + "_LONG = \"" + methodandnames.getName().toLowerCase() + "\";");
//
////                    System.err.println("                {");
////                    System.err.println("                final SimpleDateFormat sdf = new SimpleDateFormat(COMMANDLINE_DATEFORMAT);");
////                    System.err.println("                sdf.setTimeZone(TimeZone.getTimeZone(COMMANDLINE_TIMEZONE));");
////                    System.err.println("                try {");
////                    System.err.println("                    final String date = (String)parser.getOptionValue(" + methodandnames.getName().toLowerCase() + "Option);");
////                    System.err.println("                    if( date != null ) {");
////                    System.err.println("                        final Date value = sdf.parse(date);");
////                    System.err.println("                        if (null != value) {");
////                    System.err.println("                            usr." + methodandnames.getMethod().getName() + "(value);");
////                    System.err.println("                        }");
////                    System.err.println("                    }");
////                    System.err.println("                } catch (final ParseException e) {");
////                    System.err.println("                    throw new InvalidDataException(\"Wrong dateformat, use \\\"\" + sdf.toPattern() + \"\\\"\");");
////                    System.err.println("                }");
////                    System.err.println("                }");
////                } else if (methodandnames.getReturntype().equals(PASSWORDMECH_CLASS)) {
////                    optionsandmethods.add(new OptionAndMethod(methodandnames.getMethod(), setLongOpt(parser, methodandnames.getName().toLowerCase(), "CRYPT/SHA", methodandnames.getName(), true, false, true), methodandnames.getReturntype()));
////                    //System.err.println("this." + methodandnames.getName().toLowerCase() + "Option = setLongOpt(parser, \"" + methodandnames.getName().toLowerCase() + "\", \"CRYPT/SHA\", \"" + methodandnames.getName() + "\", true, false, true)");
////                    System.err.println("                    final HashSet<?> value = (HashSet<?>)parser.getOptionValue(optionAndMethod.getOption());");
////                    System.err.println("                    if (null != value) {");
////                    System.err.println("                        optionAndMethod.getMethod().invoke(usr, value);");
////                    System.err.println("                    }");
//                }
//            }
//        }
        setGui_Spam_option(parser);
        setModuleAccessOptions(parser);
    }

    protected final void setGui_Spam_option(final AdminParser admp){
        this.spamFilterOption =  setSettableBooleanLongOpt(admp, OPT_GUI_LONG, "true / false", "GUI_Spam_filter_capabilities_enabled", true, false, true); 
    }
    
    protected final void setCsvImport(final AdminParser admp) {
        admp.setCsvImportOption(setLongOpt(admp, OPT_CSV_IMPORT, "CSV file","Full path to CSV file with user data to import. This option makes \r\n" +
            "                                                   mandatory command line options obsolete, except credential options (if\r\n" + 
            "                                                   needed). But they have to be set in the CSV file.", true, false, false));
    }

    /**
     * This method goes through the dynamically created options, and sets the corresponding values
     * in the user object.
     * 
     * Attention the user object given as parameter is changed
     * 
     * @param parser
     * @param usr
     * @throws InvocationTargetException 
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @throws InvalidDataException 
     */
    protected final void applyExtendedOptionsToUser(final AdminParser parser, final User usr) throws IllegalArgumentException, IllegalAccessException, InvocationTargetException, InvalidDataException {
        
        String addguival    = (String)parser.getOptionValue(this.addGUISettingOption);
        if( addguival != null ) {
            addguival = addguival.trim();
            if( addguival.length() == 0 ) {
                throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (empty value)");
            }
            if( ! addguival.contains("=") ) {
                throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (not key = value)");
            }
            final int idx = addguival.indexOf("=");
            final String key = addguival.substring(0, idx).trim();
            final String val = addguival.substring(idx+1, addguival.length()).trim();
            if(key.length() == 0 || val.length() == 0) {
                throw new InvalidDataException("Argument for " + OPT_ADD_GUI_SETTING_LONG + "is wrong (key or val empty)");
            }
            usr.addGuiPreferences(key, val);
        }
        if( this.getClass().getName().endsWith("Change") ) {
            String removeguival = (String)parser.getOptionValue(this.removeGUISettingOption);
            if( removeguival != null ) {
                removeguival = removeguival.trim();
                if( removeguival.length() == 0 ) {
                    throw new InvalidDataException("Argument for " + OPT_REMOVE_GUI_SETTING_LONG + "is wrong (empty value)");
                }
                usr.removeGuiPreferences(removeguival);
            }
        }
        final Boolean spamfilter = (Boolean)parser.getOptionValue(this.spamFilterOption);
        if (null != spamfilter) {
            usr.setGui_spam_filter_enabled(spamfilter);
        }

        
        {
            String value = (String)parser.getOptionValue(email1Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setEmail1(value);
            }
        }
        {
            final Boolean value = (Boolean)parser.getOptionValue(mailenabledOption);
            if (null != value) {
                usr.setMailenabled(value);
            }
        }
        {
            final SimpleDateFormat sdf = new SimpleDateFormat(COMMANDLINE_DATEFORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone(COMMANDLINE_TIMEZONE));
            try {
                final String date = (String)parser.getOptionValue(birthdayOption);
                if( date != null ) {
                    final Date value = sdf.parse(date);
                    if (null != value) {
                        usr.setBirthday(value);
                    }
                }
            } catch (final ParseException e) {
                throw new InvalidDataException("Wrong dateformat, use \"" + sdf.toPattern() + "\"");
            }
        }
        {
            final SimpleDateFormat sdf = new SimpleDateFormat(COMMANDLINE_DATEFORMAT);
            sdf.setTimeZone(TimeZone.getTimeZone(COMMANDLINE_TIMEZONE));
            try {
                final String date = (String)parser.getOptionValue(anniversaryOption);
                if( date != null ) {
                    final Date value = sdf.parse(date);
                    if (null != value) {
                        usr.setAnniversary(value);
                    }
                }
            } catch (final ParseException e) {
                throw new InvalidDataException("Wrong dateformat, use \"" + sdf.toPattern() + "\"");
            }
        }
        {
            String value = (String)parser.getOptionValue(branchesOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setBranches(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(business_categoryOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setBusiness_category(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(postal_code_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setPostal_code_business(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(state_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setState_business(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(street_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setStreet_business(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_callbackOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_callback(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(city_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCity_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(commercial_registerOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCommercial_register(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(country_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCountry_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(email2Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setEmail2(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(email3Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setEmail3(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(employeetypeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setEmployeeType(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(fax_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setFax_business(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(fax_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setFax_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(fax_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setFax_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(imapserverOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setImapServer(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(imaploginOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setImapLogin(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(smtpserverOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setSmtpServer(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(instant_messenger1Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setInstant_messenger1(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(instant_messenger2Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setInstant_messenger2(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_ipOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_ip(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_isdnOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_isdn(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_drafts_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_drafts_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_sent_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_sent_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_spam_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_spam_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_trash_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_trash_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(manager_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setManager_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(marital_statusOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMarital_status(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(cellular_telephone1Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCellular_telephone1(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(cellular_telephone2Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCellular_telephone2(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(infoOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setInfo(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(nicknameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setNickname(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(number_of_childrenOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setNumber_of_children(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(noteOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setNote(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(number_of_employeeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setNumber_of_employee(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_pagerOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_pager(value);
            }
        }
        {
            final Boolean value = (Boolean)parser.getOptionValue(password_expiredOption);
            if (null != value) {
                usr.setPassword_expired(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_assistantOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_assistant(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_business1Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_business1(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_business2Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_business2(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_carOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_car(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_companyOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_company(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_home1Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_home1(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_home2Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_home2(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(postal_code_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setPostal_code_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(professionOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setProfession(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_radioOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_radio(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(room_numberOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setRoom_number(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(sales_volumeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setSales_volume(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(city_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCity_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(country_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCountry_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(middle_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMiddle_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(postal_code_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setPostal_code_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(state_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setState_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(street_otherOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setStreet_other(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(spouse_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setSpouse_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(state_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setState_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(street_homeOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setStreet_home(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(suffixOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setSuffix(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(tax_idOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTax_id(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_telexOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_telex(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_ttytddOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_ttytdd(value);
            }
        }
        {
            final Integer value = (Integer)parser.getOptionValue(uploadfilesizelimitOption);
            if (null != value) {
                usr.setUploadFileSizeLimit(value);
            }
        }
        {
            final Integer value = (Integer)parser.getOptionValue(uploadfilesizelimitperfileOption);
            if (null != value) {
                usr.setUploadFileSizeLimitPerFile(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(urlOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUrl(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield01Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield01(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield02Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield02(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield03Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield03(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield04Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield04(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield05Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield05(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield06Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield06(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield07Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield07(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield08Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield08(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield09Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield09(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield10Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield10(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield11Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield11(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield12Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield12(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield13Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield13(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield14Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield14(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield15Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield15(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield16Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield16(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield17Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield17(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield18Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield18(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield19Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield19(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(userfield20Option);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setUserfield20(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(city_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCity_business(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(assistant_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setAssistant_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(telephone_primaryOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTelephone_primary(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(categoriesOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCategories(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(passwordmechOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setPasswordMech(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_confirmed_ham_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_confirmed_ham_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(mail_folder_confirmed_spam_nameOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setMail_folder_confirmed_spam_name(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(defaultsenderaddressOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setDefaultSenderAddress(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(country_businessOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setCountry_business(value);
            }
        }
        {
            final Integer value = (Integer)parser.getOptionValue(foldertreeOption);
            if (null != value) {
                usr.setFolderTree(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(titleOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setTitle(value);
            }
        }
        {
            String value = (String)parser.getOptionValue(positionOption);
            if (null != value) {
                // On the command line an empty string can be used to clear that specific attribute.
                if ("".equals(value)) { value = null; }
                usr.setPosition(value);
            }
        }
        
//        for (final OptionAndMethod optionAndMethod : optionsandmethods) {
//            if (optionAndMethod.getReturntype().equals(JAVA_LANG_STRING)) {
//                String value = (String)parser.getOptionValue(optionAndMethod.getOption());
//                if (null != value) {
//                    // On the command line an empty string can be used to clear that specific attribute.
//                    if ("".equals(value)) { value = null; }
//                    optionAndMethod.getMethod().invoke(usr, value);
//                }
//            } else if (optionAndMethod.getReturntype().equals(JAVA_LANG_INTEGER)) {
//                final Integer value = (Integer)parser.getOptionValue(optionAndMethod.getOption());
//                if (null != value) {
//                    optionAndMethod.getMethod().invoke(usr, value);
//                }
//            } else if (optionAndMethod.getReturntype().equals(JAVA_LANG_BOOLEAN)) {
//                final Boolean value = (Boolean)parser.getOptionValue(optionAndMethod.getOption());
//                if (null != value) {
//                    optionAndMethod.getMethod().invoke(usr, value);
//                }
//            } else if (optionAndMethod.getReturntype().equals(JAVA_UTIL_DATE)) {
//                final SimpleDateFormat sdf = new SimpleDateFormat(COMMANDLINE_DATEFORMAT);
//                sdf.setTimeZone(TimeZone.getTimeZone(COMMANDLINE_TIMEZONE));
//                try {
//                    final String date = (String)parser.getOptionValue(optionAndMethod.getOption());
//                    if( date != null ) {
//                        final Date value = sdf.parse(date);
//                        if (null != value) {
//                            optionAndMethod.getMethod().invoke(usr, value);
//                        }
//                    }
//                } catch (final ParseException e) {
//                    throw new InvalidDataException("Wrong dateformat, use \"" + sdf.toPattern() + "\"");
//                }
//            } else if (optionAndMethod.getReturntype().equals(JAVA_UTIL_HASH_SET)) {
//                final HashSet<?> value = (HashSet<?>)parser.getOptionValue(optionAndMethod.getOption());
//                if (null != value) {
//                    optionAndMethod.getMethod().invoke(usr, value);
//                }
//            }
//        }
    }
    
    protected void applyDynamicOptionsToUser(AdminParser parser, User usr) {
        Map<String, Map<String, String>> dynamicArguments = parser.getDynamicArguments();
        for(Map.Entry<String, Map<String, String>> namespaced : dynamicArguments.entrySet()) {
            String namespace = namespaced.getKey();
            for(Map.Entry<String, String> pair : namespaced.getValue().entrySet()) {
                String name = pair.getKey();
                String value = pair.getValue();
                
                usr.setUserAttribute(namespace, name, value);
            }
        }
    }


    protected final OXUserInterface getUserInterface() throws NotBoundException, MalformedURLException, RemoteException {
        return (OXUserInterface) Naming.lookup(RMI_HOSTNAME + OXUserInterface.RMI_NAME);
    }
    
    @Override
    protected String getObjectName() {
        return "user";
    }

    protected void parseAndSetUserId(final AdminParser parser, final User usr) {
        final String optionValue = (String) parser.getOptionValue(this.idOption);
        if (null != optionValue) {
            userid = Integer.valueOf(optionValue);
            usr.setId(userid);
        }
    }

    /**
     * Checks if required columns are set
     * 
     * @param idarray
     * @throws InvalidDataException 
     */
    protected static void checkUserRequired(int[] idarray) throws InvalidDataException {
        for (final Constants value : Constants.values()) {
            if (value.isRequired()) {
                if (-1 == idarray[value.getIndex()]) {
                    throw new InvalidDataException("The required column \"" + value.getString() + "\" is missing");
                }
            }
        }
        for (final AccessCombinations value : AccessCombinations.values()) {
            if (value.isRequired()) {
                if (-1 == idarray[value.getIndex()]) {
                    throw new InvalidDataException("The required column \"" + value.getString() + "\" is missing");
                }
            }
        }
    }
}


