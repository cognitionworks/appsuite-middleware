package com.openexchange.ajax;


import com.meterware.httpunit.GetMethodWebRequest;
import com.meterware.httpunit.PostMethodWebRequest;
import com.meterware.httpunit.PutMethodWebRequest;
import com.meterware.httpunit.WebConversation;
import com.meterware.httpunit.WebRequest;
import com.meterware.httpunit.WebResponse;
import com.openexchange.ajax.fields.DataFields;
import com.openexchange.ajax.parser.ContactParser;
import com.openexchange.ajax.container.Response;
import com.openexchange.ajax.fields.ContactFields;
import com.openexchange.ajax.fields.DistributionListFields;
import com.openexchange.ajax.parser.DataParser;
import com.openexchange.ajax.writer.ContactWriter;
import com.openexchange.groupware.container.AppointmentObject;
import com.openexchange.groupware.container.CommonObject;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.DataObject;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderChildObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.LinkEntryObject;
import com.openexchange.tools.URLParameter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.json.JSONArray;
import org.json.JSONObject;

public class ContactTest extends AbstractAJAXTest {
	
	public static final String CONTENT_TYPE = "text/plain";
	
	public static final String BASE64String = "ABCDEFGHIJK";
	
	protected final static int[] CONTACT_FIELDS = {
		DataObject.OBJECT_ID,
		DataObject.CREATED_BY,
		DataObject.CREATION_DATE,
		DataObject.LAST_MODIFIED,
		DataObject.MODIFIED_BY,
		FolderChildObject.FOLDER_ID,
		CommonObject.CATEGORIES,
		ContactObject.GIVEN_NAME,
		ContactObject.SUR_NAME,
		ContactObject.ANNIVERSARY,
		ContactObject.ASSISTANT_NAME,
		ContactObject.BIRTHDAY,
		ContactObject.BRANCHES,
		ContactObject.BUSINESS_CATEGORY,
		ContactObject.CELLULAR_TELEPHONE1,
		ContactObject.CELLULAR_TELEPHONE2,
		ContactObject.CITY_BUSINESS,
		ContactObject.CITY_HOME,
		ContactObject.CITY_OTHER,
		ContactObject.COLOR_LABEL,
		ContactObject.COMMERCIAL_REGISTER,
		ContactObject.COMPANY,
		ContactObject.COUNTRY_BUSINESS,
		ContactObject.COUNTRY_HOME,
		ContactObject.COUNTRY_OTHER,
		ContactObject.DEPARTMENT,
		ContactObject.DISPLAY_NAME,
		ContactObject.DISTRIBUTIONLIST,
		ContactObject.EMAIL1,
		ContactObject.EMAIL2,
		ContactObject.EMAIL3,
		ContactObject.EMPLOYEE_TYPE,
		ContactObject.FAX_BUSINESS,
		ContactObject.FAX_HOME,
		ContactObject.FAX_OTHER,
		ContactObject.INFO,
		ContactObject.INSTANT_MESSENGER1,
		ContactObject.INSTANT_MESSENGER2,
		ContactObject.IMAGE1,
		ContactObject.LINKS,
		ContactObject.MANAGER_NAME,
		ContactObject.MARITAL_STATUS,
		ContactObject.MIDDLE_NAME,
		ContactObject.NICKNAME,
		ContactObject.NOTE,
		ContactObject.NUMBER_OF_CHILDREN,
		ContactObject.NUMBER_OF_EMPLOYEE,
		ContactObject.POSITION,
		ContactObject.POSTAL_CODE_BUSINESS,
		ContactObject.POSTAL_CODE_HOME,
		ContactObject.POSTAL_CODE_OTHER,
		ContactObject.PRIVATE_FLAG,
		ContactObject.PROFESSION,
		ContactObject.ROOM_NUMBER,
		ContactObject.SALES_VOLUME,
		ContactObject.SPOUSE_NAME,
		ContactObject.STATE_BUSINESS,
		ContactObject.STATE_HOME,
		ContactObject.STATE_OTHER,
		ContactObject.STREET_BUSINESS,
		ContactObject.STREET_HOME,
		ContactObject.STREET_OTHER,
		ContactObject.SUFFIX,
		ContactObject.TAX_ID,
		ContactObject.TELEPHONE_ASSISTANT,
		ContactObject.TELEPHONE_BUSINESS1,
		ContactObject.TELEPHONE_BUSINESS2,
		ContactObject.TELEPHONE_CALLBACK,
		ContactObject.TELEPHONE_CAR,
		ContactObject.TELEPHONE_COMPANY,
		ContactObject.TELEPHONE_HOME1,
		ContactObject.TELEPHONE_HOME2,
		ContactObject.TELEPHONE_IP,
		ContactObject.TELEPHONE_ISDN,
		ContactObject.TELEPHONE_OTHER,
		ContactObject.TELEPHONE_PAGER,
		ContactObject.TELEPHONE_PRIMARY,
		ContactObject.TELEPHONE_RADIO,
		ContactObject.TELEPHONE_TELEX,
		ContactObject.TELEPHONE_TTYTDD,
		ContactObject.TITLE,
		ContactObject.URL,
		ContactObject.USERFIELD01,
		ContactObject.USERFIELD02,
		ContactObject.USERFIELD03,
		ContactObject.USERFIELD04,
		ContactObject.USERFIELD05,
		ContactObject.USERFIELD06,
		ContactObject.USERFIELD07,
		ContactObject.USERFIELD08,
		ContactObject.USERFIELD09,
		ContactObject.USERFIELD10,
		ContactObject.USERFIELD11,
		ContactObject.USERFIELD12,
		ContactObject.USERFIELD13,
		ContactObject.USERFIELD14,
		ContactObject.USERFIELD15,
		ContactObject.USERFIELD16,
		ContactObject.USERFIELD17,
		ContactObject.USERFIELD18,
		ContactObject.USERFIELD19,
		ContactObject.USERFIELD20
	};
	
	private static final String CONTACT_URL = "/ajax/contacts";
	
	private static int contactFolderId = -1;
	
	private long dateTime = 0;
	
	private static final Log LOG = LogFactory.getLog(ContactTest.class);
	
	protected void setUp() throws Exception {
		super.setUp();
		
		String values[] = LoginTest.getSessionIdWithUserId(getWebConversation(), getHostName(), getLogin(), getPassword());
		sessionId = values[0];
		userId = Integer.parseInt(values[1]);
		
		FolderObject contactFolder = FolderTest.getStandardContactFolder(getWebConversation(), getHostName(), getSessionId());
		contactFolderId = contactFolder.getObjectID();
		
		Calendar c = Calendar.getInstance();
		c.set(Calendar.HOUR_OF_DAY, 0);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		c.set(Calendar.MILLISECOND, 0);
		
		dateTime = c.getTimeInMillis();
	}
	
	public void testNew() throws Exception {
		ContactObject contactObj = createContactObject("testNew");
		int objectId = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testNewWithDistributionList() throws Exception {
		ContactObject contactEntry = createContactObject("internal contact");
		contactEntry.setEmail1("internalcontact@x.de");
		int contactId = insertContact(getWebConversation(), contactEntry, PROTOCOL + getHostName(), getSessionId());
		contactEntry.setObjectID(contactId);
		
		int objectId = createContactWithDistributionList("testNewWithDistributionList", contactEntry);
	}
	
	public void testNewWithLinks() throws Exception {
		ContactObject link1 = createContactObject("link1");
		ContactObject link2 = createContactObject("link2");
		int linkId1 = insertContact(getWebConversation(), link1, PROTOCOL + getHostName(), getSessionId());
		link1.setObjectID(linkId1);
		int linkId2 = insertContact(getWebConversation(), link2, PROTOCOL + getHostName(), getSessionId());
		link2.setObjectID(linkId2);
		
		int objectId = createContactWithLinks("testNewWithLinks", link1, link2);
	}
	
	public void testUpdate() throws Exception {
		ContactObject contactObj = createContactObject("testUpdate");
		int objectId = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		contactObj.setObjectID(objectId);
		
		contactObj.setTelephoneBusiness1("+49009988776655");
		contactObj.setStateBusiness(null);
		contactObj.removeParentFolderID();
		
		updateContact(getWebConversation(), contactObj, objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testUpdateWithDistributionList() throws Exception {
		ContactObject contactEntry = createContactObject("internal contact");
		contactEntry.setEmail1("internalcontact@x.de");
		int contactId = insertContact(getWebConversation(), contactEntry, PROTOCOL + getHostName(), getSessionId());
		contactEntry.setObjectID(contactId);
		
		int objectId = createContactWithDistributionList("testUpdateWithDistributionList", contactEntry);
		
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName("testUpdateWithDistributionList");
		contactObj.setParentFolderID(contactFolderId);
		contactObj.setObjectID(objectId);
		
		DistributionListEntryObject[] entry = new DistributionListEntryObject[2];
		entry[0] = new DistributionListEntryObject("displayname a", "a@a.de", DistributionListEntryObject.INDEPENDENT);
		entry[1] = new DistributionListEntryObject("displayname b", "b@b.de", DistributionListEntryObject.INDEPENDENT);
		
		contactObj.setDistributionList(entry);
		
		contactObj.removeParentFolderID();
		
		updateContact(getWebConversation(), contactObj, objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testUpdateWithLinks() throws Exception {
		ContactObject link1 = createContactObject("link1");
		ContactObject link2 = createContactObject("link2");
		int linkId1 = insertContact(getWebConversation(), link1, PROTOCOL + getHostName(), getSessionId());
		link1.setObjectID(linkId1);
		int linkId2 = insertContact(getWebConversation(), link2, PROTOCOL + getHostName(), getSessionId());
		link2.setObjectID(linkId2);
		
		int objectId = createContactWithLinks("testUpdateWithLinks", link1, link2);
		
		ContactObject link3 = createContactObject("link3");
		int linkId3 = insertContact(getWebConversation(), link3, PROTOCOL + getHostName(), getSessionId());
		
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName("testUpdateWithLinks");
		contactObj.setParentFolderID(contactFolderId);
		contactObj.setObjectID(objectId);
		
		LinkEntryObject[] links = new LinkEntryObject[1];
		links[0] = new LinkEntryObject();
		links[0].setLinkID(linkId3);
		links[0].setLinkDisplayname(link3.getDisplayName());
		
		contactObj.setLinks(links);
		
		contactObj.removeParentFolderID();
		
		updateContact(getWebConversation(), contactObj, objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testAll() throws Exception {
		final int cols[] = new int[]{ AppointmentObject.OBJECT_ID };
		
		ContactObject[] contactArray = listContact(getWebConversation(), contactFolderId, cols, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testList() throws Exception {
		ContactObject contactObj = createContactObject("testList");
		int id1 = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		int id2 = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		int id3 = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		final int[][] objectIdAndFolderId = { { id1, contactFolderId }, { id2, contactFolderId }, { id3, contactFolderId } };
		
		final int cols[] = new int[]{ ContactObject.OBJECT_ID, ContactObject.SUR_NAME, ContactObject.DISPLAY_NAME } ;
		
		ContactObject[] contactArray = listContact(getWebConversation(), objectIdAndFolderId, cols, PROTOCOL + getHostName(), getSessionId());
		
		assertEquals("check response array", 3, contactArray.length);
	}
	
	public void testDelete() throws Exception {
		ContactObject contactObj = createContactObject("testDelete");
		int id1 = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		int id2 = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		final int[][] objectIdAndFolderId = { { id1, contactFolderId }, { id2, contactFolderId }, { 1, contactFolderId } };
		
		deleteContact(getWebConversation(), objectIdAndFolderId, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testGet() throws Exception {
		ContactObject contactObj = createContactObject("testGet");
		int objectId = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		loadContact(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
	}
	
	public void testGetWithAllFields() throws Exception {
		ContactObject contactObject = createCompleteContactObject();
		
		int objectId = insertContact(getWebConversation(), contactObject, PROTOCOL + getHostName(), getSessionId());
		
		ContactObject loadContact = loadContact(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		contactObject.setObjectID(objectId);
		compareObject(contactObject, loadContact);
	}
	
	public void testGetWithAllFieldsOnUpdate() throws Exception {
		ContactObject contactObject = new ContactObject();
		contactObject.setSurName("testGetWithAllFieldsOnUpdate");
		contactObject.setParentFolderID(contactFolderId);
		
		int objectId = insertContact(getWebConversation(), contactObject, PROTOCOL + getHostName(), getSessionId());
		
		contactObject = createCompleteContactObject();
		
		updateContact(getWebConversation(), contactObject, objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		ContactObject loadContact = loadContact(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		contactObject.setObjectID(objectId);
		compareObject(contactObject, loadContact);
	}

	
	public void testListWithAllFields() throws Exception {
		ContactObject contactObject = createCompleteContactObject();
		
		int objectId = insertContact(getWebConversation(), contactObject, PROTOCOL + getHostName(), getSessionId());
		
		final int[][] objectIdAndFolderId = { { objectId, contactFolderId } };
		
		ContactObject[] contactArray = listContact(getWebConversation(), objectIdAndFolderId, CONTACT_FIELDS, PROTOCOL + getHostName(), getSessionId());
		
		assertEquals("check response array", 1, contactArray.length);
		
		ContactObject loadContact = contactArray[0];
		
		contactObject.setObjectID(objectId);
		compareObject(contactObject, loadContact);
	}
	
	public void testContactWithImage() throws Exception {
		ContactObject contactObj = createContactObject("testContactWithImage");
		contactObj.setImage1(BASE64String.getBytes());
		int objectId = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		byte[] b = loadImage(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		assertEqualsAndNotNull("image", contactObj.getImage1(), b);
	}
	
	public void testUpdateContactWithImage() throws Exception {
		ContactObject contactObj = createContactObject("testUpdateContactWithImage");
		int objectId = insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
		
		contactObj.setImage1(BASE64String.getBytes());
		contactObj.removeParentFolderID();
		updateContact(getWebConversation(), contactObj, objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		byte[] b = loadImage(getWebConversation(), objectId, contactFolderId, PROTOCOL + getHostName(), getSessionId());
		
		assertEqualsAndNotNull("image", contactObj.getImage1(), b);
	}

	
	public void testSearchLoginUser() throws Exception {
		ContactObject[] contactArray = searchContact(getWebConversation(), getLogin(), FolderObject.SYSTEM_LDAP_FOLDER_ID, new int[] { ContactObject.INTERNAL_USERID }, PROTOCOL + getHostName(), getSessionId());
		assertTrue("contact array size is 0", contactArray.length > 0);
		assertEquals("user id is not equals", userId, contactArray[0].getInternalUserId());
	}
	
	protected int createContactWithDistributionList(String title, ContactObject contactEntry) throws Exception {
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName(title);
		contactObj.setParentFolderID(contactFolderId);
		
		DistributionListEntryObject[] entry = new DistributionListEntryObject[3];
		entry[0] = new DistributionListEntryObject("displayname a", "a@a.de", DistributionListEntryObject.INDEPENDENT);
		entry[1] = new DistributionListEntryObject("displayname b", "b@b.de", DistributionListEntryObject.INDEPENDENT);
		entry[2] = new DistributionListEntryObject(contactEntry.getDisplayName(), contactEntry.getEmail1(), DistributionListEntryObject.EMAILFIELD1);
		entry[2].setEntryID(contactEntry.getObjectID());
		
		contactObj.setDistributionList(entry);
		return insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
	}
	
	protected int createContactWithLinks(String title, ContactObject link1, ContactObject link2) throws Exception {
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName(title);
		contactObj.setParentFolderID(contactFolderId);
		
		LinkEntryObject[] links = new LinkEntryObject[2];
		links[0] = new LinkEntryObject();
		links[0].setLinkID(link1.getObjectID());
		links[0].setLinkDisplayname(link1.getDisplayName());
		links[1] = new LinkEntryObject();
		links[1].setLinkID(link2.getObjectID());
		links[1].setLinkDisplayname(link2.getDisplayName());
		
		contactObj.setLinks(links);
		
		return insertContact(getWebConversation(), contactObj, PROTOCOL + getHostName(), getSessionId());
	}
	
	private void compareObject(ContactObject contactObj1, ContactObject contactObj2) throws Exception {
		assertEquals("id is not equals", contactObj1.getObjectID(), contactObj2.getObjectID());
		assertEquals("folder id is not equals", contactObj1.getParentFolderID(), contactObj2.getParentFolderID());
		assertEquals("private flag is not equals", contactObj1.getPrivateFlag(), contactObj2.getPrivateFlag());
		assertEqualsAndNotNull("categories is not equals", contactObj1.getCategories(), contactObj2.getCategories());
		assertEqualsAndNotNull("given name is not equals", contactObj1.getGivenName(), contactObj2.getGivenName());
		assertEqualsAndNotNull("surname is not equals", contactObj1.getSurName(), contactObj2.getSurName());
		assertEqualsAndNotNull("anniversary is not equals", contactObj1.getAnniversary(), contactObj2.getAnniversary());
		assertEqualsAndNotNull("assistant name is not equals", contactObj1.getAssistantName(), contactObj2.getAssistantName());
		assertEqualsAndNotNull("birthday is not equals", contactObj1.getBirthday(), contactObj2.getBirthday());
		assertEqualsAndNotNull("branches is not equals", contactObj1.getBranches(), contactObj2.getBranches());
		assertEqualsAndNotNull("business categorie is not equals", contactObj1.getBusinessCategory(), contactObj2.getBusinessCategory());
		assertEqualsAndNotNull("cellular telephone1 is not equals", contactObj1.getCellularTelephone1(), contactObj2.getCellularTelephone1());
		assertEqualsAndNotNull("cellular telephone2 is not equals", contactObj1.getCellularTelephone2(), contactObj2.getCellularTelephone2());
		assertEqualsAndNotNull("city business is not equals", contactObj1.getCityBusiness(), contactObj2.getCityBusiness());
		assertEqualsAndNotNull("city home is not equals", contactObj1.getCityHome(), contactObj2.getCityHome());
		assertEqualsAndNotNull("city other is not equals", contactObj1.getCityOther(), contactObj2.getCityOther());
		assertEqualsAndNotNull("commercial register is not equals", contactObj1.getCommercialRegister(), contactObj2.getCommercialRegister());
		assertEqualsAndNotNull("company is not equals", contactObj1.getCompany(), contactObj2.getCompany());
		assertEqualsAndNotNull("country business is not equals", contactObj1.getCountryBusiness(), contactObj2.getCountryBusiness());
		assertEqualsAndNotNull("country home is not equals", contactObj1.getCountryHome(), contactObj2.getCountryHome());
		assertEqualsAndNotNull("country other is not equals", contactObj1.getCountryOther(), contactObj2.getCountryOther());
		assertEqualsAndNotNull("department is not equals", contactObj1.getDepartment(), contactObj2.getDepartment());
		assertEqualsAndNotNull("display name is not equals", contactObj1.getDisplayName(), contactObj2.getDisplayName());
		assertEqualsAndNotNull("email1 is not equals", contactObj1.getEmail1(), contactObj2.getEmail1());
		assertEqualsAndNotNull("email2 is not equals", contactObj1.getEmail2(), contactObj2.getEmail2());
		assertEqualsAndNotNull("email3 is not equals", contactObj1.getEmail3(), contactObj2.getEmail3());
		assertEqualsAndNotNull("employee type is not equals", contactObj1.getEmployeeType(), contactObj2.getEmployeeType());
		assertEqualsAndNotNull("fax business is not equals", contactObj1.getFaxBusiness(), contactObj2.getFaxBusiness());
		assertEqualsAndNotNull("fax home is not equals", contactObj1.getFaxHome(), contactObj2.getFaxHome());
		assertEqualsAndNotNull("fax other is not equals", contactObj1.getFaxOther(), contactObj2.getFaxOther());
		assertEqualsAndNotNull("image1 is not equals", contactObj1.getImage1(), contactObj2.getImage1());
		assertEqualsAndNotNull("info is not equals", contactObj1.getInfo(), contactObj2.getInfo());
		assertEqualsAndNotNull("instant messenger1 is not equals", contactObj1.getInstantMessenger1(), contactObj2.getInstantMessenger1());
		assertEqualsAndNotNull("instant messenger2 is not equals", contactObj1.getInstantMessenger2(), contactObj2.getInstantMessenger2());
		assertEqualsAndNotNull("instant messenger2 is not equals", contactObj1.getInstantMessenger2(), contactObj2.getInstantMessenger2());
		assertEqualsAndNotNull("marital status is not equals", contactObj1.getMaritalStatus(), contactObj2.getMaritalStatus());
		assertEqualsAndNotNull("middle name is not equals", contactObj1.getMiddleName(), contactObj2.getMiddleName());
		assertEqualsAndNotNull("nickname is not equals", contactObj1.getNickname(), contactObj2.getNickname());
		assertEqualsAndNotNull("note is not equals", contactObj1.getNote(), contactObj2.getNote());
		assertEqualsAndNotNull("number of children is not equals", contactObj1.getNumberOfChildren(), contactObj2.getNumberOfChildren());
		assertEqualsAndNotNull("number of employee is not equals", contactObj1.getNumberOfEmployee(), contactObj2.getNumberOfEmployee());
		assertEqualsAndNotNull("position is not equals", contactObj1.getPosition(), contactObj2.getPosition());
		assertEqualsAndNotNull("postal code business is not equals", contactObj1.getPostalCodeBusiness(), contactObj2.getPostalCodeBusiness());
		assertEqualsAndNotNull("postal code home is not equals", contactObj1.getPostalCodeHome(), contactObj2.getPostalCodeHome());
		assertEqualsAndNotNull("postal code other is not equals", contactObj1.getPostalCodeOther(), contactObj2.getPostalCodeOther());
		assertEqualsAndNotNull("profession is not equals", contactObj1.getProfession(), contactObj2.getProfession());
		assertEqualsAndNotNull("room number is not equals", contactObj1.getRoomNumber(), contactObj2.getRoomNumber());
		assertEqualsAndNotNull("sales volume is not equals", contactObj1.getSalesVolume(), contactObj2.getSalesVolume());
		assertEqualsAndNotNull("spouse name is not equals", contactObj1.getSpouseName(), contactObj2.getSpouseName());
		assertEqualsAndNotNull("state business is not equals", contactObj1.getStreetBusiness(), contactObj2.getStateBusiness());
		assertEqualsAndNotNull("state home is not equals", contactObj1.getStateHome(), contactObj2.getStateHome());
		assertEqualsAndNotNull("state other is not equals", contactObj1.getStateOther(), contactObj2.getStateOther());
		assertEqualsAndNotNull("street business is not equals", contactObj1.getStreetBusiness(), contactObj2.getStreetBusiness());
		assertEqualsAndNotNull("street home is not equals", contactObj1.getStreetHome(), contactObj2.getStreetHome());
		assertEqualsAndNotNull("street other is not equals", contactObj1.getStreetOther(), contactObj2.getStreetOther());
		assertEqualsAndNotNull("suffix is not equals", contactObj1.getSuffix(), contactObj2.getSuffix());
		assertEqualsAndNotNull("tax id is not equals", contactObj1.getTaxID(), contactObj2.getTaxID());
		assertEqualsAndNotNull("telephone assistant is not equals", contactObj1.getTelephoneAssistant(), contactObj2.getTelephoneAssistant());
		assertEqualsAndNotNull("telephone business1 is not equals", contactObj1.getTelephoneBusiness1(), contactObj2.getTelephoneBusiness1());
		assertEqualsAndNotNull("telephone business2 is not equals", contactObj1.getTelephoneBusiness2(), contactObj2.getTelephoneBusiness2());
		assertEqualsAndNotNull("telephone callback is not equals", contactObj1.getTelephoneCallback(), contactObj2.getTelephoneCallback());
		assertEqualsAndNotNull("telephone car is not equals", contactObj1.getTelephoneCar(), contactObj2.getTelephoneCar());
		assertEqualsAndNotNull("telehpone company is not equals", contactObj1.getTelephoneCompany(), contactObj2.getTelephoneCompany());
		assertEqualsAndNotNull("telephone home1 is not equals", contactObj1.getTelephoneHome1(), contactObj2.getTelephoneHome1());
		assertEqualsAndNotNull("telephone home2 is not equals", contactObj1.getTelephoneHome2(), contactObj2.getTelephoneHome2());
		assertEqualsAndNotNull("telehpone ip is not equals", contactObj1.getTelephoneIP(), contactObj2.getTelephoneIP());
		assertEqualsAndNotNull("telehpone isdn is not equals", contactObj1.getTelephoneISDN(), contactObj2.getTelephoneISDN());
		assertEqualsAndNotNull("telephone other is not equals", contactObj1.getTelephoneOther(), contactObj2.getTelephoneOther());
		assertEqualsAndNotNull("telephone pager is not equals", contactObj1.getTelephonePager(), contactObj2.getTelephonePager());
		assertEqualsAndNotNull("telephone primary is not equals", contactObj1.getTelephonePrimary(), contactObj2.getTelephonePrimary());
		assertEqualsAndNotNull("telephone radio is not equals", contactObj1.getTelephoneRadio(), contactObj2.getTelephoneRadio());
		assertEqualsAndNotNull("telephone telex is not equals", contactObj1.getTelephoneTelex(), contactObj2.getTelephoneTelex());
		assertEqualsAndNotNull("telephone ttytdd is not equals", contactObj1.getTelephoneTTYTTD(), contactObj2.getTelephoneTTYTTD());
		assertEqualsAndNotNull("title is not equals", contactObj1.getTitle(), contactObj2.getTitle());
		assertEqualsAndNotNull("url is not equals", contactObj1.getURL(), contactObj2.getURL());
		assertEqualsAndNotNull("userfield01 is not equals", contactObj1.getUserField01(), contactObj2.getUserField01());
		assertEqualsAndNotNull("userfield02 is not equals", contactObj1.getUserField02(), contactObj2.getUserField02());
		assertEqualsAndNotNull("userfield03 is not equals", contactObj1.getUserField03(), contactObj2.getUserField03());
		assertEqualsAndNotNull("userfield04 is not equals", contactObj1.getUserField04(), contactObj2.getUserField04());
		assertEqualsAndNotNull("userfield05 is not equals", contactObj1.getUserField05(), contactObj2.getUserField05());
		assertEqualsAndNotNull("userfield06 is not equals", contactObj1.getUserField06(), contactObj2.getUserField06());
		assertEqualsAndNotNull("userfield07 is not equals", contactObj1.getUserField07(), contactObj2.getUserField07());
		assertEqualsAndNotNull("userfield08 is not equals", contactObj1.getUserField08(), contactObj2.getUserField08());
		assertEqualsAndNotNull("userfield09 is not equals", contactObj1.getUserField09(), contactObj2.getUserField09());
		assertEqualsAndNotNull("userfield10 is not equals", contactObj1.getUserField10(), contactObj2.getUserField10());
		assertEqualsAndNotNull("userfield11 is not equals", contactObj1.getUserField11(), contactObj2.getUserField11());
		assertEqualsAndNotNull("userfield12 is not equals", contactObj1.getUserField12(), contactObj2.getUserField12());
		assertEqualsAndNotNull("userfield13 is not equals", contactObj1.getUserField13(), contactObj2.getUserField13());
		assertEqualsAndNotNull("userfield14 is not equals", contactObj1.getUserField14(), contactObj2.getUserField14());
		assertEqualsAndNotNull("userfield15 is not equals", contactObj1.getUserField15(), contactObj2.getUserField15());
		assertEqualsAndNotNull("userfield16 is not equals", contactObj1.getUserField16(), contactObj2.getUserField16());
		assertEqualsAndNotNull("userfield17 is not equals", contactObj1.getUserField17(), contactObj2.getUserField17());
		assertEqualsAndNotNull("userfield18 is not equals", contactObj1.getUserField18(), contactObj2.getUserField18());
		assertEqualsAndNotNull("userfield19 is not equals", contactObj1.getUserField19(), contactObj2.getUserField19());
		assertEqualsAndNotNull("userfield20 is not equals", contactObj1.getUserField20(), contactObj2.getUserField20());
		
		assertEqualsAndNotNull("links are not equals", links2String(contactObj1.getLinks()), links2String(contactObj2.getLinks()));
		assertEqualsAndNotNull("distribution list is not equals", distributionlist2String(contactObj1.getDistributionList()), distributionlist2String(contactObj2.getDistributionList()));
	}
	
	private ContactObject createContactObject(String displayname) {
		ContactObject contactObj = new ContactObject();
		contactObj.setSurName("Meier");
		contactObj.setGivenName("Herbert");
		contactObj.setDisplayName(displayname);
		contactObj.setStreetBusiness("Franz-Meier Weg 17");
		contactObj.setCityBusiness("Test Stadt");
		contactObj.setStateBusiness("NRW");
		contactObj.setCountryBusiness("Deutschland");
		contactObj.setTelephoneBusiness1("+49112233445566");
		contactObj.setCompany("Internal Test AG");
		contactObj.setEmail1("hebert.meier@open-xchange.com");
		contactObj.setParentFolderID(contactFolderId);
		
		return contactObj;
	}
	
	private ContactObject createCompleteContactObject() throws Exception {
		ContactObject contactObj = new ContactObject();
		contactObj.setPrivateFlag(true);
		contactObj.setCategories("categories");
		contactObj.setGivenName("given name");
		contactObj.setSurName("surname");
		contactObj.setAnniversary(new Date(dateTime));
		contactObj.setAssistantName("assistant name");
		contactObj.setBirthday(new Date(dateTime));
		contactObj.setBranches("branches");
		contactObj.setBusinessCategory("business categorie");
		contactObj.setCellularTelephone1("cellular telephone1");
		contactObj.setCellularTelephone2("cellular telephone2");
		contactObj.setCityBusiness("city business");
		contactObj.setCityHome("city home");
		contactObj.setCityOther("city other");
		contactObj.setCommercialRegister("commercial register");
		contactObj.setCompany("company");
		contactObj.setCountryBusiness("country business");
		contactObj.setCountryHome("country home");
		contactObj.setCountryOther("country other");
		contactObj.setDepartment("department");
		contactObj.setDisplayName("display name");
		contactObj.setEmail1("email1@test.de");
		contactObj.setEmail2("email2@test.de");
		contactObj.setEmail3("email3@test.de");
		contactObj.setEmployeeType("employee type");
		contactObj.setFaxBusiness("fax business");
		contactObj.setFaxHome("fax home");
		contactObj.setFaxOther("fax other");
		contactObj.setInfo("info");
		contactObj.setInstantMessenger1("instant messenger1");
		contactObj.setInstantMessenger2("instant messenger2");
		contactObj.setImage1(BASE64String.getBytes());
		contactObj.setManagerName("manager name");
		contactObj.setMaritalStatus("marital status");
		contactObj.setMiddleName("middle name");
		contactObj.setNickname("nickname");
		contactObj.setNote("note");
		contactObj.setNumberOfChildren("number of children");
		contactObj.setNumberOfEmployee("number of employee");
		contactObj.setPosition("position");
		contactObj.setPostalCodeBusiness("postal code business");
		contactObj.setPostalCodeHome("postal code home");
		contactObj.setPostalCodeOther("postal code other");
		contactObj.setProfession("profession");
		contactObj.setRoomNumber("room number");
		contactObj.setSalesVolume("sales volume");
		contactObj.setSpouseName("spouse name");
		contactObj.setStateBusiness("state business");
		contactObj.setStateHome("state home");
		contactObj.setStateOther("state other");
		contactObj.setStreetBusiness("street business");
		contactObj.setStreetHome("street home");
		contactObj.setStreetOther("street other");
		contactObj.setSuffix("suffix");
		contactObj.setTaxID("tax id");
		contactObj.setTelephoneAssistant("telephone assistant");
		contactObj.setTelephoneBusiness1("telephone business1");
		contactObj.setTelephoneBusiness2("telephone business2");
		contactObj.setTelephoneCallback("telephone callback");
		contactObj.setTelephoneCar("telephone car");
		contactObj.setTelephoneCompany("telehpone company");
		contactObj.setTelephoneHome1("telephone home1");
		contactObj.setTelephoneHome2("telephone home2");
		contactObj.setTelephoneIP("telehpone ip");
		contactObj.setTelephoneISDN("telehpone isdn");
		contactObj.setTelephoneOther("telephone other");
		contactObj.setTelephonePager("telephone pager");
		contactObj.setTelephonePrimary("telephone primary");
		contactObj.setTelephoneRadio("telephone radio");
		contactObj.setTelephoneTelex("telephone telex");
		contactObj.setTelephoneTTYTTD("telephone ttytdd");
		contactObj.setTitle("title");
		contactObj.setURL("url");
		contactObj.setUserField01("userfield01");
		contactObj.setUserField02("userfield02");
		contactObj.setUserField03("userfield03");
		contactObj.setUserField04("userfield04");
		contactObj.setUserField05("userfield05");
		contactObj.setUserField06("userfield06");
		contactObj.setUserField07("userfield07");
		contactObj.setUserField08("userfield08");
		contactObj.setUserField09("userfield09");
		contactObj.setUserField10("userfield10");
		contactObj.setUserField11("userfield11");
		contactObj.setUserField12("userfield12");
		contactObj.setUserField13("userfield13");
		contactObj.setUserField14("userfield14");
		contactObj.setUserField15("userfield15");
		contactObj.setUserField16("userfield16");
		contactObj.setUserField17("userfield17");
		contactObj.setUserField18("userfield18");
		contactObj.setUserField19("userfield19");
		contactObj.setUserField20("userfield20");
		
		contactObj.setParentFolderID(contactFolderId);
		
		ContactObject link1 = createContactObject("link1");
		ContactObject link2 = createContactObject("link2");
		int linkId1 = insertContact(getWebConversation(), link1, PROTOCOL + getHostName(), getSessionId());
		link1.setObjectID(linkId1);
		int linkId2 = insertContact(getWebConversation(), link2, PROTOCOL + getHostName(), getSessionId());
		link2.setObjectID(linkId2);
		
		LinkEntryObject[] links = new LinkEntryObject[2];
		links[0] = new LinkEntryObject();
		links[0].setLinkID(link1.getObjectID());
		links[0].setLinkDisplayname(link1.getDisplayName());
		links[1] = new LinkEntryObject();
		links[1].setLinkID(link2.getObjectID());
		links[1].setLinkDisplayname(link2.getDisplayName());
		
		contactObj.setLinks(links);
		
		DistributionListEntryObject[] entry = new DistributionListEntryObject[2];
		entry[0] = new DistributionListEntryObject("displayname a", "a@a.de", DistributionListEntryObject.INDEPENDENT);
		entry[1] = new DistributionListEntryObject(link1.getDisplayName(), link1.getEmail1(), DistributionListEntryObject.EMAILFIELD1);
		
		contactObj.setDistributionList(entry);
		
		return contactObj;
	}
	
	public static int insertContact(WebConversation webCon, ContactObject contactObj, String host, String session) throws Exception {
		int objectId = 0;
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		
		ContactWriter contactWriter = new ContactWriter(pw);
		contactWriter.writeContact(contactObj);
		
		pw.flush();
		
		byte b[] = baos.toByteArray();
		
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_NEW);
		
		WebRequest req = null;
		WebResponse resp = null;
		
		JSONObject jResponse = null;
		
		if (contactObj.containsImage1()) {
			PostMethodWebRequest postReq = new PostMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
			postReq.setMimeEncoded(true);
			
			postReq.setParameter("json", new String(baos.toByteArray()));
			
			File f = File.createTempFile("open-xchange_image", ".jpg");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(contactObj.getImage1());
			fos.flush();
			fos.close();
			
			postReq.selectFile("file", f, CONTENT_TYPE);
			
			req = postReq;
			resp = webCon.getResource(req);	
			jResponse = extractFromCallback(resp.getText());
		} else {
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

			req = new PutMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters(), bais, "text/javascript");
			resp = webCon.getResponse(req);
			
			jResponse = new JSONObject(resp.getText());
		}
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(jResponse.toString());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		JSONObject data = (JSONObject)response.getData();
		if (data.has(DataFields.ID)) {
			objectId = data.getInt(DataFields.ID);
		}
		
		return objectId;
	}
	
	public static void updateContact(WebConversation webCon, ContactObject contactObj, int objectId, int inFolder, String host, String session) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PrintWriter pw = new PrintWriter(baos);
		
		ContactWriter contactWriter = new ContactWriter(pw);
		contactWriter.writeContact(contactObj);
		
		pw.flush();
		
		byte b[] = baos.toByteArray();
		
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATE);
		parameter.setParameter(DataFields.ID, String.valueOf(objectId));
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, String.valueOf(inFolder));
		parameter.setParameter(AJAXServlet.PARAMETER_TIMESTAMP, new Date());
		
		WebRequest req = null;
		WebResponse resp = null;
		
		JSONObject jResponse = null;
		
		if (contactObj.containsImage1()) {
			PostMethodWebRequest postReq = new PostMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
			postReq.setMimeEncoded(true);
			
			postReq.setParameter("json", new String(baos.toByteArray()));
			
			File f = File.createTempFile("open-xchange_image", ".jpg");
			FileOutputStream fos = new FileOutputStream(f);
			fos.write(contactObj.getImage1());
			fos.flush();
			fos.close();
			
			postReq.selectFile("file", f, CONTENT_TYPE);
			
			req = postReq;
			resp = webCon.getResource(req);	
			jResponse = extractFromCallback(resp.getText());
		} else {
			ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());

			req = new PutMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters(), bais, "text/javascript");
			resp = webCon.getResponse(req);
			
			jResponse = new JSONObject(resp.getText());
		}
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(jResponse.toString());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
	}
	
	public static int[] deleteContact(WebConversation webCon, int[][] objectIdAndFolderId, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_DELETE);
		parameter.setParameter(AJAXServlet.PARAMETER_TIMESTAMP, new Date());
		
		JSONArray jsonArray = new JSONArray();
		
		for (int a = 0; a < objectIdAndFolderId.length; a++) {
			int[] i = objectIdAndFolderId[a];
			JSONObject jObj = new JSONObject();
			jObj.put(DataFields.ID, i[0]);
			jObj.put(AJAXServlet.PARAMETER_INFOLDER, i[1]);
			jsonArray.put(jObj);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(jsonArray.toString().getBytes());
		WebRequest req = new PutMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters(), bais, "text/javascript");
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		JSONArray data = (JSONArray)response.getData();
		int i[] = new int[data.length()];
		for (int a = 0; a < i.length; a++) {
			i[a] = data.getInt(a);
		}
		
		return i;
	}
	
	public static ContactObject[] listContact(WebConversation webCon, int inFolder, int[] cols, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_ALL);
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		parameter.setParameter(AJAXServlet.PARAMETER_COLUMNS, URLParameter.colsArray2String(cols));
		
		System.out.println(host + CONTACT_URL + parameter.getURLParameters());
		
		WebRequest req = new GetMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		assertNotNull("timestamp", response.getTimestamp());
		
		assertEquals(200, resp.getResponseCode());
		
		return jsonArray2AppointmentArray((JSONArray)response.getData(), cols);
	}
	
	public static ContactObject[] searchContact(WebConversation webCon, String searchpattern, int inFolder, int[] cols, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_SEARCH);
		parameter.setParameter(AJAXServlet.PARAMETER_COLUMNS, URLParameter.colsArray2String(cols));
		
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("pattern", searchpattern);
		jsonObj.put(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		
		WebRequest req = new PutMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters(), new ByteArrayInputStream(jsonObj.toString().getBytes()), "text/javascript");
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		assertNotNull("timestamp", response.getTimestamp());
		
		assertEquals(200, resp.getResponseCode());
		
		return jsonArray2AppointmentArray((JSONArray)response.getData(), cols);
	}
	
	public static ContactObject[] listContact(WebConversation webCon, int[][] objectIdAndFolderId, int[] cols, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_LIST);
		parameter.setParameter(AJAXServlet.PARAMETER_COLUMNS, URLParameter.colsArray2String( cols ));
		
		JSONArray jsonArray = new JSONArray();
		
		for (int a = 0; a < objectIdAndFolderId.length; a++) {
			int i[] = objectIdAndFolderId[a];
			JSONObject jsonObj = new JSONObject();
			jsonObj.put(DataFields.ID, i[0]);
			jsonObj.put(AJAXServlet.PARAMETER_INFOLDER, i[1]);
			jsonArray.put(jsonObj);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(jsonArray.toString().getBytes());
		WebRequest req = new PutMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters(), bais, "text/javascript");
		WebResponse resp = webCon.getResponse(req);
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		assertNotNull("timestamp", response.getTimestamp());
		
		assertEquals(200, resp.getResponseCode());
		
		return jsonArray2AppointmentArray((JSONArray)response.getData(), cols);
	}
	
	public static ContactObject loadContact(WebConversation webCon, int objectId, int inFolder, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_GET);
		parameter.setParameter(DataFields.ID, objectId);
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		
		WebRequest req = new GetMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		ContactObject contactObj = new ContactObject();
		
		ContactParser contactParser = new ContactParser(null);
		contactParser.parse(contactObj, (JSONObject)response.getData());
		
		return contactObj;
	}
	
	public static byte[] loadImage(WebConversation webCon, int objectId, int inFolder, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_IMAGE);
		parameter.setParameter(DataFields.ID, objectId);
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		
		WebRequest req = new GetMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		InputStream is = resp.getInputStream();
		assertNotNull("response InputStream is null", is);
		
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] b = new byte[1024];
		int i = 0;
		while ((i = is.read(b)) != -1) {
			baos.write(b, 0, i);
		}

		return baos.toByteArray();
	}

	
	public static ContactObject[] listModifiedAppointment(WebConversation webCon, int inFolder, Date modified, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATES);
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		parameter.setParameter(AJAXServlet.PARAMETER_IGNORE, "deleted");
		parameter.setParameter(AJAXServlet.PARAMETER_TIMESTAMP_SINCE, modified);
		parameter.setParameter(AJAXServlet.PARAMETER_COLUMNS, URLParameter.colsArray2String(new int[]{ AppointmentObject.OBJECT_ID }));
		
		WebRequest req = new GetMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		assertNotNull("timestamp", response.getTimestamp());
		
		assertEquals(200, resp.getResponseCode());
		
		return jsonArray2AppointmentArray((JSONArray)response.getData());
	}
	
	public static ContactObject[] listDeleteAppointment(WebConversation webCon, int inFolder, Date modified, String host, String session) throws Exception {
		final URLParameter parameter = new URLParameter();
		parameter.setParameter(AJAXServlet.PARAMETER_SESSION, session);
		parameter.setParameter(AJAXServlet.PARAMETER_ACTION, AJAXServlet.ACTION_UPDATES);
		parameter.setParameter(AJAXServlet.PARAMETER_INFOLDER, inFolder);
		parameter.setParameter(AJAXServlet.PARAMETER_IGNORE, "updated");
		parameter.setParameter(AJAXServlet.PARAMETER_TIMESTAMP_SINCE, modified);
		parameter.setParameter(AJAXServlet.PARAMETER_COLUMNS, URLParameter.colsArray2String(new int[]{ AppointmentObject.OBJECT_ID }));
		
		WebRequest req = new GetMethodWebRequest(host + CONTACT_URL + parameter.getURLParameters());
		WebResponse resp = webCon.getResponse(req);
		
		assertEquals(200, resp.getResponseCode());
		
		final Response response = Response.parse(resp.getText());
		
		if (response.hasError()) {
			fail("json error: " + response.getErrorMessage());
		}
		
		assertNotNull("timestamp", response.getTimestamp());
		
		assertEquals(200, resp.getResponseCode());
		
		return jsonArray2AppointmentArray((JSONArray)response.getData());
	}
	
	private static ContactObject[] jsonArray2AppointmentArray(JSONArray jsonArray) throws Exception {
		ContactObject[] contactArray = new ContactObject[jsonArray.length()];
		
		ContactParser contactParser = new ContactParser();
		
		for (int a = 0; a < contactArray.length; a++) {
			contactArray[a] = new ContactObject();
			JSONObject jObj = jsonArray.getJSONObject(a);
			
			contactParser.parse(contactArray[a], jObj);
		}
		
		return contactArray;
	}
	
	private static ContactObject[] jsonArray2AppointmentArray(JSONArray jsonArray, int[] cols) throws Exception {
		ContactObject[] contactArray = new ContactObject[jsonArray.length()];
		
		for (int a = 0; a < contactArray.length; a++) {
			contactArray[a] = new ContactObject();
			parseCols(cols, jsonArray.getJSONArray(a), contactArray[a]);
		}
		
		return contactArray;
	}
	
	private static void parseCols(int[] cols, JSONArray jsonArray, ContactObject contactObj) throws Exception {
		assertEquals("compare array size with cols size", cols.length, jsonArray.length());
		
		for (int a = 0; a < cols.length; a++) {
			parse(a, cols[a], jsonArray, contactObj);
		}
	}
	
	private static void parse(int pos, int field, JSONArray jsonArray, ContactObject contactObj) throws Exception {
		switch (field) {
			case ContactObject.OBJECT_ID:
				contactObj.setObjectID(jsonArray.getInt(pos));
				break;
			case ContactObject.CREATED_BY:
				contactObj.setCreatedBy(jsonArray.getInt(pos));
				break;
			case ContactObject.CREATION_DATE:
				contactObj.setCreationDate(new Date(jsonArray.getLong(pos)));
				break;
			case ContactObject.MODIFIED_BY:
				contactObj.setModifiedBy(jsonArray.getInt(pos));
				break;
			case ContactObject.LAST_MODIFIED:
				contactObj.setLastModified(new Date(jsonArray.getLong(pos)));
				break;
			case ContactObject.FOLDER_ID:
				contactObj.setParentFolderID(jsonArray.getInt(pos));
				break;
			case ContactObject.PRIVATE_FLAG:
				contactObj.setPrivateFlag(jsonArray.getBoolean(pos));
				break;
			case ContactObject.SUR_NAME:
				contactObj.setSurName(jsonArray.getString(pos));
				break;
			case ContactObject.GIVEN_NAME:
				contactObj.setGivenName(jsonArray.getString(pos));
				break;
			case ContactObject.ANNIVERSARY:
				contactObj.setAnniversary(new Date(jsonArray.getLong(pos)));
				break;
			case ContactObject.ASSISTANT_NAME:
				contactObj.setAssistantName(jsonArray.getString(pos));
				break;
			case ContactObject.BIRTHDAY:
				contactObj.setBirthday(new Date(jsonArray.getLong(pos)));
				break;
			case ContactObject.BRANCHES:
				contactObj.setBranches(jsonArray.getString(pos));
				break;
			case ContactObject.BUSINESS_CATEGORY:
				contactObj.setBusinessCategory(jsonArray.getString(pos));
				break;
			case ContactObject.CATEGORIES:
				contactObj.setCategories(jsonArray.getString(pos));
				break;
			case ContactObject.CELLULAR_TELEPHONE1:
				contactObj.setCellularTelephone1(jsonArray.getString(pos));
				break;
			case ContactObject.CELLULAR_TELEPHONE2:
				contactObj.setCellularTelephone2(jsonArray.getString(pos));
				break;
			case ContactObject.CITY_HOME:
				contactObj.setCityHome(jsonArray.getString(pos));
				break;
			case ContactObject.CITY_BUSINESS:
				contactObj.setCityBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.CITY_OTHER:
				contactObj.setCityOther(jsonArray.getString(pos));
				break;
			case ContactObject.COMMERCIAL_REGISTER:
				contactObj.setCommercialRegister(jsonArray.getString(pos));
				break;
			case ContactObject.COMPANY:
				contactObj.setCompany(jsonArray.getString(pos));
				break;
			case ContactObject.COUNTRY_HOME:
				contactObj.setCountryHome(jsonArray.getString(pos));
				break;
			case ContactObject.COUNTRY_BUSINESS:
				contactObj.setCountryBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.COUNTRY_OTHER:
				contactObj.setCountryOther(jsonArray.getString(pos));
				break;
			case ContactObject.DEPARTMENT:
				contactObj.setDepartment(jsonArray.getString(pos));
				break;
			case ContactObject.DISPLAY_NAME:
				contactObj.setDisplayName(jsonArray.getString(pos));
				break;
			case ContactObject.EMAIL1:
				contactObj.setEmail1(jsonArray.getString(pos));
				break;
			case ContactObject.EMAIL2:
				contactObj.setEmail2(jsonArray.getString(pos));
				break;
			case ContactObject.EMAIL3:
				contactObj.setEmail3(jsonArray.getString(pos));
				break;
			case ContactObject.EMPLOYEE_TYPE:
				contactObj.setEmployeeType(jsonArray.getString(pos));
				break;
			case ContactObject.FAX_BUSINESS:
				contactObj.setFaxBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.FAX_HOME:
				contactObj.setFaxHome(jsonArray.getString(pos));
				break;
			case ContactObject.FAX_OTHER:
				contactObj.setFaxOther(jsonArray.getString(pos));
				break;
			case ContactObject.IMAGE1:
				contactObj.setImage1(jsonArray.getString(pos).getBytes());
				break;
			case ContactObject.INFO:
				contactObj.setInfo(jsonArray.getString(pos));
				break;
			case ContactObject.INSTANT_MESSENGER1:
				contactObj.setInstantMessenger1(jsonArray.getString(pos));
				break;
			case ContactObject.INSTANT_MESSENGER2:
				contactObj.setInstantMessenger2(jsonArray.getString(pos));
				break;
			case ContactObject.INTERNAL_USERID:
				contactObj.setInternalUserId(jsonArray.getInt(pos));
				break;
			case ContactObject.COLOR_LABEL:
				contactObj.setLabel(jsonArray.getInt(pos));
				break;
			case ContactObject.MANAGER_NAME:
				contactObj.setManagerName(jsonArray.getString(pos));
				break;
			case ContactObject.MARITAL_STATUS:
				contactObj.setMaritalStatus(jsonArray.getString(pos));
				break;
			case ContactObject.MIDDLE_NAME:
				contactObj.setMiddleName(jsonArray.getString(pos));
				break;
			case ContactObject.NICKNAME:
				contactObj.setNickname(jsonArray.getString(pos));
				break;
			case ContactObject.NOTE:
				contactObj.setNote(jsonArray.getString(pos));
				break;
			case ContactObject.NUMBER_OF_CHILDREN:
				contactObj.setNumberOfChildren(jsonArray.getString(pos));
				break;
			case ContactObject.NUMBER_OF_EMPLOYEE:
				contactObj.setNumberOfEmployee(jsonArray.getString(pos));
				break;
			case ContactObject.POSITION:
				contactObj.setPosition(jsonArray.getString(pos));
				break;
			case ContactObject.POSTAL_CODE_HOME:
				contactObj.setPostalCodeHome(jsonArray.getString(pos));
				break;
			case ContactObject.POSTAL_CODE_BUSINESS:
				contactObj.setPostalCodeBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.POSTAL_CODE_OTHER:
				contactObj.setPostalCodeOther(jsonArray.getString(pos));
				break;
			case ContactObject.PROFESSION:
				contactObj.setProfession(jsonArray.getString(pos));
				break;
			case ContactObject.ROOM_NUMBER:
				contactObj.setRoomNumber(jsonArray.getString(pos));
				break;
			case ContactObject.SALES_VOLUME:
				contactObj.setSalesVolume(jsonArray.getString(pos));
				break;
			case ContactObject.SPOUSE_NAME:
				contactObj.setSpouseName(jsonArray.getString(pos));
				break;
			case ContactObject.STATE_HOME:
				contactObj.setStateHome(jsonArray.getString(pos));
				break;
			case ContactObject.STATE_BUSINESS:
				contactObj.setStateBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.STATE_OTHER:
				contactObj.setStateOther(jsonArray.getString(pos));
				break;
			case ContactObject.STREET_HOME:
				contactObj.setStreetHome(jsonArray.getString(pos));
				break;
			case ContactObject.STREET_BUSINESS:
				contactObj.setStreetBusiness(jsonArray.getString(pos));
				break;
			case ContactObject.STREET_OTHER:
				contactObj.setStreetOther(jsonArray.getString(pos));
				break;
			case ContactObject.SUFFIX:
				contactObj.setSuffix(jsonArray.getString(pos));
				break;
			case ContactObject.TAX_ID:
				contactObj.setTaxID(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_ASSISTANT:
				contactObj.setTelephoneAssistant(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_BUSINESS1:
				contactObj.setTelephoneBusiness1(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_BUSINESS2:
				contactObj.setTelephoneBusiness2(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_CALLBACK:
				contactObj.setTelephoneCallback(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_CAR:
				contactObj.setTelephoneCar(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_COMPANY:
				contactObj.setTelephoneCompany(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_HOME1:
				contactObj.setTelephoneHome1(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_HOME2:
				contactObj.setTelephoneHome2(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_IP:
				contactObj.setTelephoneIP(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_ISDN:
				contactObj.setTelephoneISDN(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_OTHER:
				contactObj.setTelephoneOther(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_PAGER:
				contactObj.setTelephonePager(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_PRIMARY:
				contactObj.setTelephonePrimary(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_RADIO:
				contactObj.setTelephoneRadio(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_TELEX:
				contactObj.setTelephoneTelex(jsonArray.getString(pos));
				break;
			case ContactObject.TELEPHONE_TTYTDD:
				contactObj.setTelephoneTTYTTD(jsonArray.getString(pos));
				break;
			case ContactObject.TITLE:
				contactObj.setTitle(jsonArray.getString(pos));
				break;
			case ContactObject.URL:
				contactObj.setURL(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD01:
				contactObj.setUserField01(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD02:
				contactObj.setUserField02(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD03:
				contactObj.setUserField03(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD04:
				contactObj.setUserField04(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD05:
				contactObj.setUserField05(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD06:
				contactObj.setUserField06(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD07:
				contactObj.setUserField07(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD08:
				contactObj.setUserField08(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD09:
				contactObj.setUserField09(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD10:
				contactObj.setUserField10(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD11:
				contactObj.setUserField11(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD12:
				contactObj.setUserField12(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD13:
				contactObj.setUserField13(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD14:
				contactObj.setUserField14(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD15:
				contactObj.setUserField15(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD16:
				contactObj.setUserField16(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD17:
				contactObj.setUserField17(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD18:
				contactObj.setUserField18(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD19:
				contactObj.setUserField19(jsonArray.getString(pos));
				break;
			case ContactObject.USERFIELD20:
				contactObj.setUserField20(jsonArray.getString(pos));
				break;
			case ContactObject.LINKS:
				contactObj.setLinks(parseLinks(contactObj, jsonArray.getJSONArray(pos)));
				break;
			case ContactObject.DISTRIBUTIONLIST:
				contactObj.setDistributionList(parseDistributionList(contactObj, jsonArray.getJSONArray(pos)));
				break;
			default:
				throw new Exception("missing field in mapping: " + field);
				
		}
	}
	
	private static LinkEntryObject[] parseLinks(ContactObject contactObj, JSONArray jsonArray) throws Exception {
		LinkEntryObject[] links = new LinkEntryObject[jsonArray.length()];
		for (int a = 0; a < links.length; a++) {
			links[a] = new LinkEntryObject();
			JSONObject entry = jsonArray.getJSONObject(a);
			if (entry.has(ContactFields.ID)) {
				links[a].setLinkID(DataParser.parseInt(entry, ContactFields.ID));
			}
			
			links[a].setLinkDisplayname(DataParser.parseString(entry, DistributionListFields.DISPLAY_NAME));
		}
		
		return links;
	}
	
	private static DistributionListEntryObject[] parseDistributionList(ContactObject contactObj, JSONArray jsonArray) throws Exception {
		DistributionListEntryObject[] distributionlist = new DistributionListEntryObject[jsonArray.length()];
		for (int a = 0; a < jsonArray.length(); a++) {
			JSONObject entry = jsonArray.getJSONObject(a);
			distributionlist[a] = new DistributionListEntryObject();
			if (entry.has(DistributionListFields.ID)) {
				distributionlist[a].setEntryID(DataParser.parseInt(entry, DistributionListFields.ID));
			}
			
			if (entry.has(DistributionListFields.FIRST_NAME)) {
				distributionlist[a].setFirstname(DataParser.parseString(entry, DistributionListFields.FIRST_NAME));
			}
			
			if (entry.has(DistributionListFields.LAST_NAME)) {
				distributionlist[a].setLastname(DataParser.parseString(entry, DistributionListFields.LAST_NAME));
			}
			
			distributionlist[a].setDisplayname(DataParser.parseString(entry, DistributionListFields.DISPLAY_NAME));
			distributionlist[a].setEmailaddress(DataParser.parseString(entry, DistributionListFields.MAIL));
			distributionlist[a].setEmailfield(DataParser.parseInt(entry, DistributionListFields.MAIL_FIELD));
		}
		
		return distributionlist;
	}
	
	private HashSet links2String(LinkEntryObject[] linkEntryObject) throws Exception {
		if (linkEntryObject == null) {
			return null;
		}
		
		HashSet hs = new HashSet();
		
		for (int a = 0; a < linkEntryObject.length; a++) {
			hs.add(link2String(linkEntryObject[a]));
		}
		
		return hs;
	}
	
	private String link2String(LinkEntryObject linkEntryObject) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("ID" + linkEntryObject.getLinkID());
		sb.append("DISPLAYNAME" + linkEntryObject.getLinkDisplayname());
		
		return sb.toString();
	}
	
	private HashSet distributionlist2String(DistributionListEntryObject[] distributionListEntry) throws Exception {
		if (distributionListEntry == null) {
			return null;
		}
		
		HashSet hs = new HashSet();
		
		for (int a = 0; a < distributionListEntry.length; a++) {
			hs.add(entry2String(distributionListEntry[a]));
		}
		
		return hs;
	}
	
	private String entry2String(DistributionListEntryObject entry) throws Exception {
		StringBuffer sb = new StringBuffer();
		sb.append("ID" + entry.getEntryID());
		sb.append("D" + entry.getDisplayname());
		sb.append("F" + entry.getEmailfield());
		sb.append("E" + entry.getEmailaddress());
		
		return sb.toString();
	}
}

