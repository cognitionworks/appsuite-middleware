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

package com.openexchange.groupware.contact;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DataTruncation;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.imageio.ImageIO;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.openexchange.api.OXConflictException;
import com.openexchange.api.OXObjectNotFoundException;
import com.openexchange.api2.OXConcurrentModificationException;
import com.openexchange.api2.OXException;
import com.openexchange.cache.FolderCacheManager;
import com.openexchange.event.EventClient;
import com.openexchange.event.InvalidStateException;
import com.openexchange.groupware.Component;
import com.openexchange.groupware.IDGenerator;
import com.openexchange.groupware.OXExceptionSource;
import com.openexchange.groupware.OXThrows;
import com.openexchange.groupware.OXThrowsMultiple;
import com.openexchange.groupware.Types;
import com.openexchange.groupware.UserConfiguration;
import com.openexchange.groupware.AbstractOXException.Category;
import com.openexchange.groupware.container.ContactObject;
import com.openexchange.groupware.container.DistributionListEntryObject;
import com.openexchange.groupware.container.FolderObject;
import com.openexchange.groupware.container.LinkEntryObject;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.delete.DeleteEvent;
import com.openexchange.groupware.delete.DeleteFailedException;
import com.openexchange.groupware.delete.DeleteListener;
import com.openexchange.groupware.ldap.LdapException;
import com.openexchange.server.DBPool;
import com.openexchange.server.DBPoolingException;
import com.openexchange.server.EffectivePermission;
import com.openexchange.server.OCLPermission;
import com.openexchange.sessiond.SessionObject;
import com.openexchange.tools.oxfolder.OXFolderAccess;
import com.openexchange.tools.sql.DBUtils;


/**
 Contacts
 @author <a href="mailto:ben.pahne@comfire.de">Benjamin Frederic Pahne</a>
 
 */

@OXExceptionSource(
		classId=Classes.COM_OPENEXCHANGE_GROUPWARE_CONTACTS_CONTACTS,
		component=Component.CONTACT
)

public class Contacts implements DeleteListener {
	
	private static final ContactExceptionFactory EXCEPTIONS = new ContactExceptionFactory(Contacts.class);
	private static final Log LOG = LogFactory.getLog(Contacts.class);
	
	public static mapper[] mapping;	
			
	
	@OXThrows(
			category=Category.USER_INPUT,
			desc="0",
			exceptionId=0,
			msg="The Application was unable to validate a given email address from this contact: %s"
	)
	public static void validateEmailAddress(final ContactObject co) throws OXException {

		String email = "";
		try {
			if (ContactConfig.getProperty("validate_contact_email") != null && ContactConfig.getProperty("validate_contact_email").equals("true")){
				if (co.containsEmail1() && co.getEmail1() != null){
					email = co.getEmail1();
					InternetAddress ia = new InternetAddress(email);
					ia.validate();
				}else if (co.containsEmail2() && co.getEmail2() != null){
					email = co.getEmail2();
					InternetAddress ia = new InternetAddress(email); 
					ia.validate();
				}else if (co.containsEmail3() && co.getEmail3() != null){
					email = co.getEmail3();
					InternetAddress ia = new InternetAddress(email);
					ia.validate();
				}
			}
		}catch (AddressException ae){
			LOG.error("Email Validation Failed", ae);
			throw EXCEPTIONS.create(0,ae,email);
		}
	}

	@OXThrowsMultiple(
			category={ Category.USER_INPUT,
								Category.USER_INPUT},
			desc={"1", "2"},
			exceptionId={1,2 },
			msg={"Unable to scale the contact image. This is a not supported file type or it is too large! Your mime type is %1$s and your image size is %2$d KB. The max. allowed image size is %3$d KB.",		
					"This gif Image is to large. It can not be scaled and will not be accepted"}
	)
	public static byte[] scaleContactImage(final byte[] img, String mime) throws OXConflictException, OXException, IOException {
		
		/** TODO
		 * check acme giff scale

		int scaledWidth = 76;
		int scaledHeight = 76;
		int max_size = 33750;	
		*/
		final int scaledWidth = Integer.valueOf(ContactConfig.getProperty("scale_image_width")).intValue();
		final int scaledHeight = Integer.valueOf(ContactConfig.getProperty("scale_image_height")).intValue();
		final int max_size = Integer.valueOf(ContactConfig.getProperty("max_image_size")).intValue();		
		
		String fileType = "";
		String[] allowed_mime = ImageIO.getReaderFormatNames();
		/*
		for (int i=0;i<allowed_mime.length;i++){
			System.out.println(new StringBuiler("--> "+allowed_mime[i]+" vs "+mime));
		}
		*/
		boolean check = false;
	
		if ((mime.toLowerCase().indexOf("jpg") != -1) || (mime.toLowerCase().indexOf("jpeg") != -1)){
			mime = "image/jpg";
		}else if ((mime.toLowerCase().indexOf("bmp") != -1) || (mime.toLowerCase().indexOf("bmp") != -1)){
				mime = "image/bmp";
		} else if (mime.toLowerCase().indexOf("png") != -1){
			mime = "image/png";
		}
		for (int h = 0;h < allowed_mime.length;h++){
			if ( mime != null && mime.equals("image/"+allowed_mime[h])) {
				check = true;
				//fileType = mime.substring(6,mime.length());
			}
		}

		if (mime.toLowerCase().contains("gif")){
			check = true;
		}
		if (img.length > max_size){
			check = false;
		}
		if (!check){
			
			int ilkb = img.length / 1024;
			int mskb = max_size / 1024;
			
			throw EXCEPTIONS.createOXConflictException(1,mime,ilkb,mskb);
			//throw new OXException("This is a not supported file type for an Image or it is to large! MimeType ="+mime+" / Image Size = "+img.length+" / max. allowed Image size = "+max_size);
		}
		
		ByteArrayInputStream bais = new ByteArrayInputStream(img);
		BufferedImage bi = ImageIO.read(bais);

		int origHeigh = bi.getHeight();
		int origWidth = bi.getWidth();
		int origType = bi.getType();

		StringBuilder logi = new StringBuilder("OUR IMAGE -> mime="+mime+" / type="+origType+" / width="+origWidth+" / height="+origHeigh+" / byte[] size="+img.length);
		if (LOG.isDebugEnabled()) {
			LOG.debug(logi.toString());
		}
		/*
		if ((origHeigh > scaledHeight || origWidth > scaledWidth) && mime.toLowerCase().contains("gif")){
			throw EXCEPTIONS.createOXConflictException(2);
			//throw new OXException("This gif Image is to large. It can not be scaled and will not be accepted");
		}else if ((origHeigh > scaledHeight || origWidth > scaledWidth) && !mime.toLowerCase().contains("gif")){
			*/
		if (origHeigh > scaledHeight || origWidth > scaledWidth){
			float ratio = 0;
			int sWd = 0;
			int sHd = 0;

			if (origWidth == origHeigh){
				ratio = 1;

				if (scaledHeight < scaledWidth) {
					sWd = scaledHeight;
					sHd = scaledHeight;
				} else if (scaledHeight > scaledWidth) {
					sWd = scaledWidth;
					sHd = scaledWidth;
				} else if (scaledHeight == scaledWidth){
					sWd = scaledWidth;
					sHd = scaledWidth;
				}
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder("IMAGE SCALE Picture Heigh "+origHeigh+" Width "+origWidth+" -> Scale down to Heigh "+sHd+" Width "+sWd+" Ratio "+ratio).toString());
				}
				
			} else if (origWidth > origHeigh){
				float w1 = origWidth;
				float h1 = origHeigh;
				ratio = w1 / h1;
				
				float widthFloat = scaledWidth;
				float heighFloat = widthFloat / ratio;
				
				if (heighFloat > scaledHeight){
					heighFloat = scaledHeight;
					widthFloat =heighFloat * ratio;
				}
				sWd =  Math.round(widthFloat);
				sHd = Math.round(heighFloat);

				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder("IMAGE SCALE Picture Heigh "+origHeigh+" Width "+origWidth+" -> Scale down to Heigh "+sHd+" Width "+sWd+" Ratio "+ratio));
				}
				
			} else if (origWidth < origHeigh){
				float w1 = origWidth;
				float h1 = origHeigh;
				ratio = h1 / w1;
				
				float heighFloat = scaledHeight;
				float widthFloat = heighFloat / ratio;
				
				if (widthFloat > scaledWidth){
					widthFloat = scaledWidth;
					heighFloat = widthFloat * ratio;
				}
				
				sWd = Math.round(widthFloat);
				sHd = Math.round(heighFloat);
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder("IMAGE SCALE Picture Heigh "+origHeigh+" Width "+origWidth+" -> Scale down to Heigh "+sHd+" Width "+sWd+" Ratio "+ratio));
				}
			}
		
			BufferedImage scaledBufferedImage = new BufferedImage(sWd, sHd, origType);
			Graphics2D g2d = scaledBufferedImage.createGraphics();
			g2d.drawImage(bi, 0, 0, sWd, sHd, null);
			g2d.dispose();
			
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try{
			ImageIO.write(scaledBufferedImage,fileType,baos);
			} catch (Exception fallback){
				/**
				 * This is just a basic fallback i try when he is not able to scale the image 
				 * with the given mimetype. then we try the common jpg.
				 */
				LOG.debug("Unable to Scale the Image with default Parameters. Gonna try fallback");
			} finally {
				if (baos.toByteArray().length < 1){
					ImageIO.write(scaledBufferedImage,"JPG",baos);
				}
			}
			
			byte[] back =  baos.toByteArray();
			
			return back;
		} else {
			return img;
		}
	}
	
	@OXThrowsMultiple(
			category={	Category.PERMISSION,
									Category.PERMISSION,
									Category.PERMISSION,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.TRY_AGAIN,
									Category.TRY_AGAIN
								},
			desc={"3","4","5","6","7","8","9","51","53","58","62"},
			exceptionId={3,4,5,6,7,8,9,51,53,58,99},
			msg={	ContactException.NON_CONTACT_FOLDER_MSG, 
							ContactException.NO_PERMISSION_MSG, 
							ContactException.NO_PERMISSION_MSG, 
							"Unable to Insert Contacts! Context: %d", 
							"Got a -1 ID from IDGenerator", 
							"Unable to scale Image down.", 
							"Unable to insert Contact. Context: %d",
							ContactException.INIT_CONNECTION_FROM_DBPOOL,
							ContactException.INIT_CONNECTION_FROM_DBPOOL,
							"The image you tried to attach is not a valid picture. It may be broken or is not a valid file.",
							"Mandatory field last name is not set."
						}
	)
	public static void performContactStorageInsert(final ContactObject co, final int user,final int[] group, final SessionObject so) throws OXConflictException, OXException {
		
		StringBuilder insert_fields = new StringBuilder();
		StringBuilder insert_values = new StringBuilder();
		
		ContactSql cs = new ContactMySql(so);
		Connection writecon = null;
		Connection readcon = null;
		try{
			readcon = DBPool.pickup(so.getContext());
			validateEmailAddress(co);
			
			int fid = co.getParentFolderID();
			 
			FolderObject contactFolder;
			if (FolderCacheManager.isEnabled()){
				contactFolder = FolderCacheManager.getInstance().getFolderObject(fid, true, so.getContext(), readcon);
			}else{
				contactFolder = FolderObject.loadFolderObjectFromDB(fid, so.getContext(), readcon);
			}
			if (contactFolder.getModule() != FolderObject.CONTACT) {
				throw EXCEPTIONS.createOXConflictException(3,  fid, so.getContext().getContextId(),user);
				//throw new OXException("saveContactObject() called with a non-Contact-Folder! cid="+so.getContext().getContextId()+" fid="+fid+" uid="+user);
			}

			OXFolderAccess oxfs = new OXFolderAccess(readcon, so.getContext());
			final EffectivePermission oclPerm = oxfs.getFolderPermission(fid, user, so.getUserConfiguration());
			
				//OXFolderTools.getEffectiveFolderOCL(fid, user,group, so.getContext(),so.getUserConfiguration(), readcon);
			if (oclPerm.getFolderPermission() <= OCLPermission.NO_PERMISSIONS) {
				throw EXCEPTIONS.createOXPermissionException(4, fid, so.getContext().getContextId(), user);
				//throw new OXConflictException("NOT ALLOWED TO SAVE FOLDER OBJECTS CONTACT! cid="+so.getContext().getContextId()+" fid="+fid+" uid="+user);
			}
			if (!oclPerm.canCreateObjects()) {
				throw EXCEPTIONS.createOXPermissionException(5, fid, so.getContext().getContextId(), user);
				//throw new OXConflictException("NOT ALLOWED TO SAVE FOLDER OBJECTS! cid="+so.getContext().getContextId()+" fid="+fid+" uid="+user);
			}
			
			if ((contactFolder.getType() != FolderObject.PRIVATE) && co.getPrivateFlag()){
				co.setPrivateFlag(false);
			}
			
			if (!co.containsDisplayName() || co.getDisplayName() == null || co.getDisplayName().length() < 1){
				if (co.getSurName() != null && co.getSurName().length() > 0){
					if (co.getGivenName() != null && co.getGivenName().length() > 0){
						if (co.getMiddleName() != null && co.getMiddleName().length() > 0){
							co.setDisplayName(co.getSurName()+", "+co.getGivenName()+' '+co.getMiddleName());
						}else{
							co.setDisplayName(co.getSurName()+", "+co.getGivenName());
						}
					}else{
						co.setDisplayName(co.getSurName());
					}
				}else{
					if (co.getGivenName() != null && co.getGivenName().length() > 0){
						if (co.getMiddleName() != null && co.getMiddleName().length() > 0){
							co.setDisplayName(co.getGivenName()+' '+co.getMiddleName());
						}else{
							co.setDisplayName(co.getGivenName());
						}
					} else {
						co.setDisplayName("<unknown>");
					}
				}
			} else {
				if (co.getSurName() == null || co.getSurName().length() < 1){
					co.setSurName(co.getDisplayName());
				}
			}	
			if (!co.containsFileAs() || (co.getFileAs() != null && co.getFileAs().length() > 0)){
				co.setFileAs(co.getDisplayName());
			}
			
			if ((!co.containsSurName() || co.getSurName() == null || co.getSurName().length() < 1) && (!co.containsDisplayName() || co.getDisplayName() == null || co.getDisplayName().length() < 1)){
				throw EXCEPTIONS.createOXConflictException(62, so.getContext().getContextId());
			}
			
			co.removeContextID();
			co.removeLastModified();
			co.removeCreationDate();
			co.removeCreatedBy();
			co.removeModifiedBy();
			co.removeObjectID();

			
			StringBuilder sb = null;
			for (int i=0;i<650;i++){
				if (mapping[i] != null && mapping[i].containsElement(co) && i != ContactObject.DISTRIBUTIONLIST && i != ContactObject.LINKS && i != ContactObject.OBJECT_ID && i != ContactObject.IMAGE_LAST_MODIFIED && i != ContactObject.IMAGE1_CONTENT_TYPE){					
					sb = new StringBuilder(mapping[i].getDBFieldName()).append(',');
					insert_fields.append(sb);
					insert_values.append("?,");			
				}			
			}
		} catch (DBPoolingException d){
			throw EXCEPTIONS.create(51, d);
		} catch (OXConflictException oe){
			throw oe;
		} catch (OXException oe){
			throw oe;
			//throw EXCEPTIONS.create(6, oe, so.getContext().getContextId());
			//throw new OXException("ERROR: Unable to Insert Contacts! cid="+so.getContext().getContextId(),oe);
		} finally {
			try{
				DBPool.closeReaderSilent(so.getContext(), readcon);
			} catch (Exception ex) {
				LOG.error("Unable to close READ Connection");
			}
		}
		
		PreparedStatement ps = null;
		
		try {
			/*
			 * AutoCommit false for the IDGenerator! 
			 */
			writecon = DBPool.pickupWriteable(so.getContext());
			writecon.setAutoCommit(false);	
			
			int id = IDGenerator.getId(so.getContext(), Types.CONTACT, writecon);
			LOG.trace("Got ID from Generator -> "+id);
			if (id == -1){
				throw EXCEPTIONS.create(7);
				//throw new OXException("-1 ID from IDGenerator");
			}
			co.setObjectID(id);
			
			long lmd = System.currentTimeMillis();
			
			StringBuilder insert = cs.iFperformContactStorageInsert(insert_fields,insert_values,user,lmd,so.getContext().getContextId(),id);
			
			ps = writecon.prepareStatement(insert.toString());
			int counter = 1;
			for (int i=2;i<650;i++){
				if (mapping[i] != null && mapping[i].containsElement(co) && i != ContactObject.DISTRIBUTIONLIST && i != ContactObject.LINKS && i != ContactObject.OBJECT_ID && i != ContactObject.IMAGE_LAST_MODIFIED && i != ContactObject.IMAGE1_CONTENT_TYPE){
					mapping[i].fillPreparedStatement(ps, counter, co);	
					counter++;
				}
			}
			Date ddd = new Date(lmd);
			co.setLastModified(ddd);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("INFO: YOU WANT TO INSERT THIS: cid="+so.getContext().getContextId()+" oid="+co.getObjectID()+" -> "+ps.toString()));
			}
			
			ps.execute();	

			if(co.containsNumberOfDistributionLists() && co.getSizeOfDistributionListArray() > 0){
				writeDistributionListArrayInsert(co.getDistributionList(), co.getObjectID(),so.getContext().getContextId(),writecon);						
			}				
			if(co.containsNumberOfLinks() && co.getSizeOfLinks() > 0){
				writeContactLinkArrayInsert(co.getLinks(), co.getObjectID(), so.getContext().getContextId(),writecon);						
			}
			if(co.containsImage1()){
				if (ContactConfig.getProperty("scale_images").equals("true")){
					try{
						co.setImage1(scaleContactImage(co.getImage1(), co.getImageContentType()));
					} catch (OXConflictException ex){
						throw ex;
					} catch (OXException ex){
						throw ex;
					} catch (IOException ex){
						throw EXCEPTIONS.create(8, ex);
					} catch (Exception ex){
						throw EXCEPTIONS.create(58,ex);
					}
				}
				writeContactImage(co.getObjectID(), co.getImage1(), so.getContext().getContextId(), co.getImageContentType(), writecon);
			}
			writecon.commit();
		} catch (OXException ox){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Insert", see);
			}
			throw ox;
		} catch (DBPoolingException oe){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Insert", see);
			}
			throw EXCEPTIONS.create(53, oe);
		} catch (DataTruncation se){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Update", see);
			}
			throw Contacts.getTruncation(se);
		} catch (SQLException se) {
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Insert", see);
			}			throw EXCEPTIONS.create(9,se, so.getContext().getContextId());
		} finally {
			try{
				ps.close();
			}catch (SQLException sq){
				LOG.error("UNABLE TO CLOSE STATEMENT ",sq);
			}
			try{
				writecon.setAutoCommit(true);
			} catch (Exception ex) {
				LOG.error("Unable to set setAutoCommit = true");
			}
			try{
				if (writecon != null) {
					DBPool.closeWriterSilent(so.getContext(), writecon);
				}
			} catch (Exception ex) {
				LOG.error("Unable to set setAutoCommit = true");
			}
		}
	}

	@OXThrowsMultiple(
			category={	Category.PERMISSION, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.CODE_ERROR, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.PERMISSION, 
									Category.CODE_ERROR, 
									Category.CODE_ERROR, 
									Category.USER_INPUT,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.USER_INPUT,
									Category.TRY_AGAIN,
									Category.TRY_AGAIN
			},
			desc={"10","11","12","13","14","15","16","17","18","19","20","21","22","23", "24","55","56","59","63"},
			exceptionId={10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,55,56,59,63},
			msg={	ContactException.NON_CONTACT_FOLDER_MSG,
							ContactException.NO_PERMISSION_MSG, 
							ContactException.NO_PERMISSION_MSG, 
							ContactException.NON_CONTACT_FOLDER_MSG, 
							ContactException.NO_PERMISSION_MSG, 
							ContactException.NO_PERMISSION_MSG,
							"Unable to syncronice the old contact with the new changes: Context %1$d Object %2$d",
							ContactException.NO_PERMISSION_MSG,		
							"You are not allowed to mark this contact as private contact: Context %1$d Object %2$d",
							ContactException.OBJECT_HAS_CHANGED_MSG,
							"Unable to update Contact. Context %1$d Object %2$d",
							"An Error occurred: Object ID is -1",
							"No Changes found. No Update requiered. Context %1$d Object %2$d",
							"Unable to scale Image down.", 
							"Unable to update Contact. Context %1$d Object %2$d",
							ContactException.INIT_CONNECTION_FROM_DBPOOL,
							"One or more fields contain too much information. Field: %1$d Character Limit: %2$d Sent %3$d",
							"The image you tried to attach is not a valid picture. It may be broken or is not a valid file.",
							"Mandatory field last name is not set."
					}
	)
	public static void performContactStorageUpdate(ContactObject co, int fid, java.util.Date client_date, int user, int[] group, Context ctx, UserConfiguration uc) throws ContactException, OXConflictException, OXObjectNotFoundException, OXConcurrentModificationException, OXException {
		
		//TODO
		/*
			if ((!co.containsSurName() || co.getSurName() == null || co.getSurName().length() < 1) && (!co.containsDisplayName() || co.getDisplayName() == null || co.getDisplayName().length() < 1)){
				throw EXCEPTIONS.createOXConflictException(63,ctx.getContextId());
			}
		*/	
		validateEmailAddress(co);
		
		boolean can_edit_only_own = false;
		
		if(!co.containsParentFolderID() || co.getParentFolderID() == 0){
			co.setParentFolderID(fid);
		}
		
		ContactSql cs = new ContactMySql(ctx, user);
		
		ContactObject original = null;
		Connection writecon = null;
		Connection readcon = null;
		try{
			readcon = DBPool.pickup(ctx);
			
			/*
			 * Check Rights for Destination Folder
			 */
			FolderObject contactFolder;
			if (FolderCacheManager.isEnabled()){
				contactFolder = FolderCacheManager.getInstance().getFolderObject(co.getParentFolderID(), true, ctx, readcon);
			}else{
				contactFolder = FolderObject.loadFolderObjectFromDB(co.getParentFolderID(), ctx, readcon);
			}
			if (contactFolder.getModule() != FolderObject.CONTACT) {
				throw EXCEPTIONS.createOXConflictException(10, co.getParentFolderID(), ctx.getContextId(), user);
				//throw new OXConflictException("saveContactObject() called with a non-Contact-Folder! cid="+ctx.getContextId()+" fid="+co.getParentFolderID());
			}
			OXFolderAccess oxfs = new OXFolderAccess(readcon, ctx);
			EffectivePermission oclPerm = oxfs.getFolderPermission(fid, user, uc);

			if (oclPerm.getFolderPermission() <= OCLPermission.NO_PERMISSIONS) {
				throw EXCEPTIONS.createOXPermissionException(11, co.getParentFolderID(), ctx.getContextId(), user);
				//throw new OXConflictException("NOT ALLOWED TO MODIFIE CONTACT cid="+ctx.getContextId()+" fid="+co.getParentFolderID());
			}

			if (!oclPerm.canWriteAllObjects()) {
				if (oclPerm.canWriteOwnObjects()){
					can_edit_only_own = true;
				}else{
					throw EXCEPTIONS.createOXPermissionException(12, co.getParentFolderID(), ctx.getContextId(), user);
					//throw new OXConflictException("NOT ALLOWED TO MODIFIE CONTACT cid="+ctx.getContextId()+" fid="+co.getParentFolderID());	
				}
			}
			
			/*
			 *  ++++ MOVE ++++
			 *  
			 *  Check Rights if this is a Move
			 */
			if(co.getParentFolderID() != fid){
				if (!oclPerm.canCreateObjects()){
					throw EXCEPTIONS.createOXPermissionException(12, co.getParentFolderID(), ctx.getContextId(), user);
				}
				
				FolderObject source;
				if (FolderCacheManager.isEnabled()){
					source = FolderCacheManager.getInstance().getFolderObject(fid, true, ctx, readcon);
				}else{
					source = FolderObject.loadFolderObjectFromDB(fid, ctx, readcon);
				}
				if (source.getModule() != FolderObject.CONTACT) {
					throw EXCEPTIONS.createOXConflictException(13,fid, ctx.getContextId(), user);
					//throw new OXConflictException("saveContactObject() called with a non-Contact-Folder! cid="+ctx.getContextId()+" fid"+fid);
				}

				final EffectivePermission op = oxfs.getFolderPermission(fid, user, uc);
				
				if (op.getFolderPermission() <= OCLPermission.NO_PERMISSIONS) {
					throw EXCEPTIONS.createOXPermissionException(14, fid, ctx.getContextId(), user);
					//throw new OXConflictException("NOT ALLOWED TO MODIFIE CONTACT cid="+ctx.getContextId()+" fid"+fid);
				}
				if (!op.canWriteAllObjects()) {
					if (oclPerm.canWriteOwnObjects()){
						can_edit_only_own = true;
					}else{
						throw EXCEPTIONS.createOXPermissionException(15, fid, ctx.getContextId(), user);
						//throw new OXConflictException("NOT ALLOWED TO MODIFIE CONTACT cid="+ctx.getContextId()+" fid="+fid);
					}
				}
			}
			
			/*
			 *  ALL RIGHTS CHECK SO FAR
			 *  NOW LOAD THE OLD OBJECT AND CHECK FOR READ ONLY OWN 
			 * 
			 */
			
			try{
				original = getContactById(co.getObjectID(),user,group,ctx,uc,readcon);
			} catch (Exception e){
				throw EXCEPTIONS.createOXObjectNotFoundException(16,e,ctx.getContextId(), co.getObjectID());
				//throw new OXObjectNotFoundException("UNABLE TO LOAD CONTACT FOR UPDATE cid="+ctx.getContextId()+" oid"+co.getObjectID(), e);
			}
			if (can_edit_only_own && original.getCreatedBy() != user){
				throw EXCEPTIONS.createOXConflictException(17, fid, ctx.getContextId(), user);
				//throw new OXConflictException("NOT ALLOWED TO MODIFIE CONTACT cid="+ctx.getContextId()+" oid"+co.getObjectID());
			}
				
			if ((contactFolder.getType() != FolderObject.PRIVATE) && co.getPrivateFlag()){
				co.setPrivateFlag(false);
			} else if ((contactFolder.getType() == FolderObject.PRIVATE) && original.getPrivateFlag() && original.getCreatedBy() != user){
				throw EXCEPTIONS.createOXConflictException(18, ctx.getContextId(), co.getObjectID());
				//throw new OXConflictException("NOT ALLOWED TO SAVE FOLDER OBJECTS CONTACT AS PRIVATE cid="+ctx.getContextId()+" oid="+co.getObjectID());
			}
						
			java.util.Date server_date = original.getLastModified();
			
			try{
				if (LOG.isDebugEnabled()) {
					LOG.debug(new StringBuilder("Compare Dates for Contact Update\nClient-Date="+client_date.getTime()+"\nServer-Date="+server_date.getTime()));
				}
				
				if (client_date != null && client_date.getTime() > -1 && client_date.getTime() < server_date.getTime()) {
					throw EXCEPTIONS.createOXConcurrentModificationException(19);
					//throw new OXConcurrentModificationException("CONTACT HAS CHANGED ON SERVER SIDE SINCE THE LAST VISIT");
				}
			} catch (OXConcurrentModificationException xoxo){
				throw xoxo;
			}
			
			
			String firstname = null;
			if (co.containsGivenName() && co.getGivenName() != null){
				firstname = co.getGivenName();
			} else if (co.containsGivenName() && co.getGivenName() == null){
				firstname = null;
			} else if (original.containsGivenName() && original.getGivenName() != null){
				firstname = original.getGivenName();
			}
			String secondname = null;
			if (co.containsMiddleName() && co.getMiddleName() != null){
				secondname = co.getMiddleName();
			} else if (co.containsMiddleName() && co.getMiddleName() == null){
				secondname = null;
			} else if (original.containsMiddleName() && original.getMiddleName() != null){
				secondname = original.getMiddleName();
			}
			
			if (co.getDisplayName() == null || co.getDisplayName().length() < 1){
				if (co.getSurName() != null && co.getSurName().length() > 0){
					if (firstname != null){
						if (secondname != null){
							co.setDisplayName(co.getSurName()+", "+firstname+' '+secondname);
						}else{
							co.setDisplayName(co.getSurName()+", "+firstname);
						}
					}else if (secondname != null){
						co.setDisplayName(co.getSurName()+", "+secondname);
					}else{
						co.setDisplayName(co.getSurName());
					}
				}
			} else if (co.getDisplayName() != null || co.getDisplayName().length() > 0) {				
				if (co.getSurName() == null || co.getSurName().length() < 1){
					co.setSurName(co.getDisplayName());
				} else if (co.getSurName() != null && co.getSurName().length() > 0) {					
					if (firstname != null){
						if (secondname != null){
							co.setDisplayName(co.getSurName()+", "+firstname+' '+secondname);
						}else{
							co.setDisplayName(co.getSurName()+", "+firstname);
						}
					}else if (secondname != null){
						co.setDisplayName(co.getSurName()+", "+secondname);
					} else {
						co.setDisplayName(co.getSurName());
					}
				}
				}
			if (!co.containsFileAs() || (co.getFileAs() != null && co.getFileAs().length() > 0)){
				co.setFileAs(co.getDisplayName());
			}
		} catch (OXConcurrentModificationException cme){
			throw cme;
		} catch (OXObjectNotFoundException oe2){
			throw oe2;
		} catch (OXException oe3){
			throw oe3;
		} catch (DBPoolingException oe){
			throw EXCEPTIONS.create(20, oe);
			//throw new OXException("UNABLE TO UPDATE CONTACT OBJECT cid="+ctx.getContextId()+" oid="+co.getObjectID(), oe);
		} finally {
			try{
				DBPool.closeReaderSilent(ctx, readcon);
			} catch (Exception ex) { 
				LOG.error("Unable to close READ Connection", ex);
			}
		}
		
		PreparedStatement ps = null;
		StringBuilder update = new StringBuilder();
		
		try{
			int[] mod = new int[650];
			int cnt = 0;
			for (int i=0;i<650;i++){
				if (mapping[i] != null && !mapping[i].compare(co, original)){
					if (i == ContactObject.SUR_NAME && co.containsSurName() && (co.getSurName() == null || co.getSurName().length() < 1)){
						//System.out.println("---> "+co.getSurName());
						throw EXCEPTIONS.createOXConflictException(63,ctx.getContextId());
					}
					
					mod[cnt] = i;
					cnt ++;
				}
			}
			int[] modtrim = new int[cnt];
			System.arraycopy(mod, 0, modtrim, 0, cnt);  
			
			if (modtrim.length > 0){
				StringBuilder sb;
				for (int i=0;i<modtrim.length;i++){
					if (mapping[modtrim[i]] != null && mapping[modtrim[i]].containsElement(co) && modtrim[i] != ContactObject.DISTRIBUTIONLIST && modtrim[i] != ContactObject.LINKS && modtrim[i] != ContactObject.OBJECT_ID && i != ContactObject.IMAGE1_CONTENT_TYPE){					
						sb = new StringBuilder(mapping[modtrim[i]].getDBFieldName()).append(" = ?,");
						update.append(sb);				
					}	
				}
				int id = co.getObjectID();
				if (id == -1){
					throw EXCEPTIONS.createOXConflictException(21);
					//throw new OXConflictException("New ID is -1!");
				}
				long lmd = System.currentTimeMillis();
				
				StringBuilder updater  = cs.iFperformContactStorageUpdate(update, lmd,id,ctx.getContextId());
				
				writecon = DBPool.pickupWriteable(ctx);
				ps = writecon.prepareStatement(updater.toString());
				int counter = 1;
				for (int i=0;i<modtrim.length;i++){
					if (mapping[modtrim[i]] != null && mapping[modtrim[i]].containsElement(co) && modtrim[i] != ContactObject.DISTRIBUTIONLIST && modtrim[i] != ContactObject.LINKS && modtrim[i] != ContactObject.OBJECT_ID && i != ContactObject.IMAGE1_CONTENT_TYPE){
						mapping[modtrim[i]].fillPreparedStatement(ps, counter, co);	
						counter++;
					}
				}
				
				Date ddd = new Date(lmd);
				co.setLastModified(ddd);
			} else {
				throw EXCEPTIONS.create(22, ctx.getContextId(), co.getObjectID());
				//throw new OXException("NOTHING TO UPDATE - NOTHING CHANGED cid="+ctx.getContextId()+" oid"+co.getObjectID());
			}
			
			writecon.setAutoCommit(false);
			
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("INFO: YOU WANT TO UPDATE THIS: cid="+ctx.getContextId()+" oid="+co.getObjectID()+" -> "+ps.toString()));
			}
			//System.out.println(new StringBuilder("INFO: YOU WANT TO UPDATE THIS: cid="+ctx.getContextId()+" oid="+co.getObjectID()+" -> "+ps.toString()));
			ps.execute();	

			if(co.containsNumberOfDistributionLists()){
				if (co.getSizeOfDistributionListArray() > 0){
					writeDistributionListArrayUpdate(co.getDistributionList(),original.getDistributionList(), co.getObjectID(),ctx.getContextId(),writecon);							
				}
			}				
			if(co.containsNumberOfLinks()){
				if (co.getSizeOfLinks() > 0){
					writeContactLinkArrayUpdate(co.getLinks(), original.getLinks(),co.getObjectID(), ctx.getContextId(),writecon);						
				}
			}
			
			/*
			 * containsImage = true && image = stuff -> create image
			 * containsImage = true && image = null -> delete image
			 * containsImage = false -> nothing to do
			 * 
			 */
			
			if(co.containsImage1()){
				if (co.getImage1() != null){
					if (ContactConfig.getProperty("scale_images").equals("true")){
						try{
							co.setImage1(scaleContactImage(co.getImage1(), co.getImageContentType()));
						} catch (OXConflictException ex){
							throw ex;
						} catch (OXException ex){
							throw ex;
						} catch (IOException ex){
							throw EXCEPTIONS.create(23,ex);
							//throw new OXException("Unable to scale contact Image down.", ex);
						} catch (Exception ex){
							throw EXCEPTIONS.create(59,ex);
						}
					}
					
					if (original.containsImage1()){
						updateContactImage(co.getObjectID(), co.getImage1(), ctx.getContextId(), co.getImageContentType(), writecon);
					}else{
						writeContactImage(co.getObjectID(), co.getImage1(), ctx.getContextId(), co.getImageContentType(), writecon);
					}
					
				} else if (original.containsImage1()){
					try{
						deleteImage(co.getObjectID(),ctx.getContextId(), writecon);
					}catch (SQLException oxee){
						LOG.error("Unable to delete Contact Image", oxee);
					}
				}
			} 
			writecon.commit();
		} catch (OXException ox){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Insert", see);
			}
			throw ox;
		} catch (DBPoolingException oe){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Insert", see);
			}
			throw EXCEPTIONS.create(55, oe);
		} catch (DataTruncation se){
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Update", see);
			}
			throw Contacts.getTruncation(se);
			//throw EXCEPTIONS.create(56, se,se.getIndex(),se.getDataSize(), se.getTransferSize());
		} catch (SQLException se) {
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL Update", see);
			}
			throw EXCEPTIONS.create(24, ctx.getContextId(), co.getObjectID());
			//throw new OXException("ERROR: Unable to Update Contacts! cid="+ctx.getContextId()+" oid="+co.getObjectID(),se);
		} finally {
			try{
				ps.close();
			}catch (Exception sq){
				LOG.error("UNABLE TO CLOSE STATEMENR ",sq);
			}
			try{
				writecon.setAutoCommit(true);
			} catch (Exception ex) {
				LOG.error("Unable to set setAutoCommit = true");
			}
			try{
				if (writecon != null) {
					DBPool.closeWriterSilent(ctx, writecon);
				}
			} catch (Exception ex) {
				LOG.error("Unable to set return writeconnection");
			}
		}
	}
	
	public static ContactObject getContactById(int objectId, int userId, int[] memberInGroups, Context ctx, UserConfiguration uc, Connection readCon) throws OXException {
		
		ContactObject co = null;
		ContactSql contactSQL = new ContactMySql(ctx, userId);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0;i<650;i++){				
			if (mapping[i] != null){
				sb.append("co.");
				sb.append(mapping[i].getDBFieldName());
				sb.append(',');
			}
		}
		sb = contactSQL.iFgetContactById(sb);
		contactSQL.setSelect(sb.toString());
		contactSQL.setObjectID(objectId);	
		co = fillContactObject(contactSQL.getSqlCommand(), userId, memberInGroups, ctx, uc, readCon);
			    
		return co;
	}
	
	
	@OXThrowsMultiple(
			category={	Category.CODE_ERROR,
									Category.CODE_ERROR
								},
			desc={"25","26"},
			exceptionId={25,26},
			msg={	"Contact not found! Context %1$d",
							"Unable to load Contact: Context %1$d"
						}
	)
	public static ContactObject fillContactObject(String sql_string, int user, int[] group, Context ctx, UserConfiguration uc, Connection readCon) throws OXException {

		ContactObject co = new ContactObject();
		Statement stmt = null;
		ResultSet rs = null;
		
		try{
			stmt = readCon.createStatement();
			rs = stmt.executeQuery(sql_string);
			
			if (rs.next()) {
				int cnt = 1;
				for (int i=0;i<650;i++){				
					if (mapping[i] != null){
						mapping[i].addToContactObject(rs,cnt,co,readCon, user, group, ctx, uc);
						cnt++;
					}
				}
			} else {
				throw EXCEPTIONS.createOXObjectNotFoundException(25, ctx.getContextId());
				//throw new OXObjectNotFoundException("No Contact Found!");
			}		
		} catch (OXException ex) {
			throw ex;
		}catch (SQLException sq){
			throw EXCEPTIONS.create(26,sq, ctx.getContextId());
		} finally {
			try{ 
				if (rs != null){
					rs.close();
				}
				if (stmt != null){
					stmt.close();
				}
			}catch (SQLException sxe){
				LOG.error("Unable to close Statement or ResultSet", sxe);
			}
		}
		
		return co;
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="27",
			exceptionId=27,
			msg="Unable to delete Contact: Context %1$d Contact %2$d"
	)
	
	public static void deleteContact(int id, int cid, Connection writecon) throws OXException {
		Statement del = null;
		try{
			del = writecon.createStatement();
			trashDistributionList(id, cid, writecon, false);
			trashLinks(id, cid, writecon, false);
			trashImage(id, cid, writecon, false);
			
			ContactSql cs = new ContactMySql(null);
			cs.iFdeleteContact(id,cid,del);
			
		} catch (SQLException se) {
			throw EXCEPTIONS.create(27,se,cid,id);
			//throw new OXException("ERROR DURING CONTACT DELETE cid="+cid+" oid="+id, se);
		} finally {
			try{
				if (del != null){
					del.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Connection", see);
			}
		}
	}		
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="28",
			exceptionId=28,
			msg="Unable to load Dristributionlist : Context %1$d Contact %2$d"
	)
	public static DistributionListEntryObject[] fillDistributionListArray(int id, int user, int[] group, Context ctx, UserConfiguration uc, Connection readcon) throws OXException {		

		Statement smt = null;
		ResultSet rs = null;
		DistributionListEntryObject[] r = null;
		ContactSql cs = new ContactMySql(null);
		
		try{
			smt = readcon.createStatement();
			rs = smt.executeQuery(cs.iFfillDistributionListArray(id, ctx.getContextId()));

			rs.last();
		    int size = rs.getRow();
		    rs.beforeFirst();

		    DistributionListEntryObject[] dleos = new DistributionListEntryObject[size];		    
		    DistributionListEntryObject dleo = null;

		    String displayname = null;
		    String lastname = null;
		    String firstname = null;
		    int emailfield = 0;
		    int objectid = 0;
		    int folderid = 0;
		    int cnt = 0;

		    while (rs.next()){
		    	dleo = new DistributionListEntryObject();
		    			    	
		    	displayname = rs.getString(5);
		    	if (!rs.wasNull()){
		    		dleo.setDisplayname(displayname);
		    	}
		    	lastname = rs.getString(6);
		    	if (!rs.wasNull()){
		    		dleo.setLastname(lastname);	
		    	}
		    	firstname = rs.getString(7);
		    	if (!rs.wasNull()){
		    		dleo.setFirstname(firstname);
		    	}
		    	dleo.setEmailaddress(rs.getString(8));
		    	
		    	objectid = rs.getInt(2);
		    	if (!rs.wasNull() && objectid > 0){
		    		dleo.setEntryID(objectid);
		    		/*
					if (!performContactReadCheckByID(objectid, user,group,so)){
						continue;
					}
					*/
		    	}
		    		
		    	emailfield = rs.getInt(3);
		    	if (!rs.wasNull()){
		    		dleo.setEmailfield(emailfield);
		    	}
		    	folderid = rs.getInt(4);
		    	if (!rs.wasNull()){
		    		dleo.setFolderID(folderid);
		    	}
		    	dleos[cnt] = dleo;
		    	cnt++;
		    }
			r = new DistributionListEntryObject[cnt];
			System.arraycopy(dleos, 0, r, 0, cnt);   
		} catch (SQLException se) {
			throw EXCEPTIONS.create(28,se,ctx.getContextId(),id);
			//throw new OXException("ERROR DURING DISTRIBUTION LIST LOAD cid="+ctx.getContextId()+" oid="+id, se);
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Statement or ResultSet", see);
			}
		}
	    return r;
	}
	

	@OXThrowsMultiple(
			category={	Category.CODE_ERROR,
									Category.CODE_ERROR
								},
			desc={"29","60"},
			exceptionId={29,60},
			msg={	"Unable to save Dristributionlist : Context %1$d Contact %2$d",
							"This Contact has no FolderID: Entry %1$d Context %2$d"
						}
	)
	public static void writeDistributionListArrayInsert(DistributionListEntryObject[] dleos, int id, int cid, Connection writecon) throws OXException {


		DistributionListEntryObject dleo = null;
		ContactSql cs = new ContactMySql(null);
		
		try {	
			for (int i=0;i<dleos.length;i++){
				dleo = dleos[i];
				PreparedStatement ps = null;
				
				try{
					ps = writecon.prepareStatement(cs.iFwriteDistributionListArrayInsert());
					ps.setInt(1, id);

					if (dleo.containsEntryID() && dleo.getEntryID() > 0){
						ps.setInt(2, dleo.getEntryID());			
						ps.setInt(3, dleo.getEmailfield());
						/*
						if (dleo.getFolderID() == 0){
							throw EXCEPTIONS.createOXConflictException(60,dleo.getEntryID(),cid);
						}
						*/
						ps.setInt(9,dleo.getFolderID());
					}else{
						ps.setNull(2, java.sql.Types.INTEGER);
						ps.setNull(3, java.sql.Types.INTEGER);
						ps.setNull(9, java.sql.Types.INTEGER);					
					}
					if (dleo.containsDisplayname()){
						ps.setString(4, dleo.getDisplayname());
					}else if ((dleo.containsLastname() && dleo.getLastname() != null) && (dleo.containsFistname() && dleo.getFirstname() != null)){
						ps.setString(4, dleo.getLastname()+", "+dleo.getFirstname());
					}else if ((dleo.containsLastname() && dleo.getLastname() != null) && !dleo.containsFistname()){
						ps.setString(4, dleo.getLastname());
					}else{
						ps.setString(4, "unknown");
					}
					if (dleo.containsLastname() && dleo.getLastname() != null){
						ps.setString(5, dleo.getLastname());
					}else{
						ps.setNull(5, java.sql.Types.VARCHAR);
					}
					if (dleo.containsFistname() && dleo.getFirstname() != null){
						ps.setString(6, dleo.getFirstname());
					}else{
						ps.setNull(6, java.sql.Types.VARCHAR);
					}
					ps.setString(7, dleo.getEmailaddress());
					ps.setInt(8,cid);
					
					if (LOG.isDebugEnabled()) {
						LOG.debug(new StringBuilder("WRITE DLIST ").append(ps.toString()));
					}
					
					ps.execute();									
				} finally {
					try{
						if (ps != null){
							ps.close();
						}
					} catch (SQLException see){
						LOG.warn("Unable to close Connection", see);
					}
				}
			}
		} catch (SQLException se) {
			throw EXCEPTIONS.create(29,se,cid,id);
			//throw new OXException("ERROR DURING DISTRIBUTION LIST SAVE, DLIST NOT SAVED cid="+cid+" oid="+id, se);
		}
	}
	
	public static void writeDistributionListArrayUpdate(DistributionListEntryObject[] dleos, DistributionListEntryObject[] old_dleos, int id, int cid, Connection writecon) throws OXException {

		DistributionListEntryObject new_one = null;
		DistributionListEntryObject old_one = null;

		int sizey = 0;
		if (dleos != null && old_dleos != null){
			sizey = dleos.length + old_dleos.length;
		}else if (dleos != null && old_dleos == null){
			sizey = dleos.length;
		}else if (dleos == null && old_dleos != null){
			sizey = old_dleos.length;
		}else{
			sizey = 1;
		}
		DistributionListEntryObject[] inserts = new DistributionListEntryObject[sizey];
		DistributionListEntryObject[] updates = new DistributionListEntryObject[sizey];
		DistributionListEntryObject[] deletes = new DistributionListEntryObject[sizey];
		
		int insert_count = 0;
		int update_count = 0;		
		int delete_count = 0;
		
		for (int i=0;i<dleos.length;i++){ // this for;next goes to all new entries from the client
			new_one = dleos[i];
			
			if (new_one.containsEntryID() && new_one.getEntryID() > 0){ // this is a real contact entry in the distributionlist
				boolean actions = false;
				
				if (old_dleos != null){
					for (int u=0;u<old_dleos.length;u++){ // this for;next goes to all old entries from the server
						if (old_dleos[u] != null) { // maybe we have some empty entries here from previous checks
							old_one = old_dleos[u];
							
							if (new_one.searchDlistObject(old_one)){ // this will search the current entry in the old dlist
								if(!new_one.compareDlistObject(old_one)){ // is this true the dlistentrie has not changed, is it false the dlistentry missmatches the old one
									// ok the dlist has changed and needs to get updated
									updates[insert_count] = new_one;
									update_count++;
									actions = true;
								}else{
									actions = true;
									// ignore this entry cuz it has not changed
								}
								// ok we have found a entry in the old list and we have done something with him
								// no we must remove him from the old list cuz maybe he needs get deleted
								old_dleos[u] = null;							
								break; // when is the entry is found we can leave the old list for the nex new entry
							}else{
								// this old entry does not match the new one
								actions = false;
							}
						}
					}
				}
				// we checked the old list and nothing was found. this means we have to insert this entry cuz it is new
				if (!actions){
					inserts[insert_count] = new_one;
					insert_count++;	
				}				
			} else { // this is an independent entry in a distributionlist and they get a normal insert
				inserts[insert_count] = new_one;
				insert_count++;
			}
		}
		
		// the new list is fully checked, now we have to make sure that old entries get deleted
		if (old_dleos != null){
			for (int u=0;u<old_dleos.length;u++){ // this for;next goes to all old entries from the server
				old_one = old_dleos[u];
				if (old_one != null && old_one.containsEntryID() && old_one.getEntryID() > 0) { // maybe we have some empty entries here from previous checks
					//if (old_one.containsEntryID() && old_one.getEntryID() > 0){
						deletes[delete_count] = old_one;
						delete_count++;
					//}
				}
			}
		}
		// all is checked, we have 3 arrays now INSERT, UPDATE and DELETE. just make the stuff now
		
		DistributionListEntryObject[] insertcut = new DistributionListEntryObject[insert_count];
		System.arraycopy(inserts, 0, insertcut, 0, insert_count);
		
		DistributionListEntryObject[] updatecut = new DistributionListEntryObject[update_count];
		System.arraycopy(updates, 0, updatecut, 0, update_count);
		
		DistributionListEntryObject[] deletecut = new DistributionListEntryObject[delete_count];
		System.arraycopy(deletes, 0, deletecut, 0, delete_count);
		
		try{
			deleteDistributionListEntriesByIds(id,deletecut,cid,writecon);
			updateDistributionListEntriesByIds(id, updatecut ,cid,writecon);
			writeDistributionListArrayInsert(insertcut,id,cid,writecon);
		} catch (OXException x){
			throw x;
			//throw new OXException("UNABLE TO UPDATE DISTRIBUTION LIST cid="+cid+" oid="+id,x);
		}
	}


		@OXThrowsMultiple(
			category={	Category.CODE_ERROR,
									Category.CODE_ERROR
								},
			desc={"30","61"},
			exceptionId={30,61},
			msg={	"Unable to update Dristributionlist : Context %1$d Contact %2$d",
							"This Contact has no FolderID: Entry %1$d Context %2$d"
						}
	)
	public static void updateDistributionListEntriesByIds(int id, DistributionListEntryObject[] dleos, int cid, Connection writecon) throws OXException {
		if (dleos.length > 0) {

			DistributionListEntryObject dleo = null;
			ContactSql cs = new ContactMySql(null);
			try{
				for (int i=0;i<dleos.length;i++){
					 dleo = dleos[i];

					 PreparedStatement ps = null;
					 try{
						 
						 ps = writecon.prepareStatement(cs.iFupdateDistributionListEntriesByIds());
						 ps.setInt(1, id);
						 ps.setInt(9, id);
						
						if (dleo.containsEntryID() && dleo.getEntryID() > 0){
							ps.setInt(2, dleo.getEntryID());			
							ps.setInt(3, dleo.getEmailfield());
							ps.setInt(10, dleo.getEntryID());			
							ps.setInt(11, dleo.getEmailfield());
							/*
							if (dleo.getFolderID() == 0){
								throw EXCEPTIONS.createOXConflictException(61,dleo.getEntryID(),cid);
							}
							*/
							ps.setInt(4, dleo.getFolderID());
						}else{
							ps.setNull(2, java.sql.Types.INTEGER);
							ps.setNull(3, java.sql.Types.INTEGER);
							ps.setNull(10, java.sql.Types.INTEGER);
							ps.setNull(11, java.sql.Types.INTEGER);
							ps.setNull(4, java.sql.Types.INTEGER);	
						}
						if (dleo.containsDisplayname()){
							ps.setString(5, dleo.getDisplayname());
						}else if (dleo.containsLastname() && dleo.containsFistname()){
							ps.setString(5, dleo.getLastname()+", "+dleo.getFirstname());
						}else if (dleo.containsLastname() && !dleo.containsFistname()){
							ps.setString(5, dleo.getLastname());
						}else{
							ps.setString(5, "unknown");
						}
						if (dleo.containsLastname()){
							ps.setString(6, dleo.getLastname());
						}else{
							ps.setNull(6, java.sql.Types.VARCHAR);
						}
						if (dleo.containsFistname()){
							ps.setString(7, dleo.getFirstname());
						}else{
							ps.setNull(7, java.sql.Types.VARCHAR);
						}
						ps.setString(8, dleo.getEmailaddress());
						ps.setInt(12, cid);
						
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuilder("UPDATE DLIST ").append(ps.toString()));
						}
						
						ps.execute();
					
					 } finally {
							try{
								if (ps != null){
									ps.close();
								}
							} catch (SQLException see){
								LOG.warn("Unable to close Connection", see);
							}
					 }
				}				
			} catch (SQLException se) {
				throw EXCEPTIONS.create(30,se,cid,id);
				//throw new OXException("ERROR DURING DISTRIBUTION LIST UPDATE, DLIST NOT UPDATED cid="+cid+" oid="+id, se);
			}
		}
	}
	
	
	@OXThrowsMultiple(
			category={	Category.CODE_ERROR,
									Category.CODE_ERROR
								},
			desc={"48","31"},
			exceptionId={48,31},
			msg={	"Unable to delete Dristributionlist by Id : Context %1$d Contact %2$d",
							"Unable to delete Dristributionlist by Id : Context %1$d Contact %2$d"
						}
	)
	public static void deleteDistributionListEntriesByIds(int id, DistributionListEntryObject[] dleos, int cid, Connection writecon) throws OXException {

		PreparedStatement ps = null;
		DistributionListEntryObject dleo = null;
		ContactSql cs = new ContactMySql(null);
		try {
			ps = writecon.prepareStatement(cs.iFdeleteDistributionListEntriesByIds(cid));
			ps.setInt(1, id);
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("DELETE FROM DLIST ").append(ps.toString()));
			}
			ps.execute();
		} catch (SQLException se) {
			throw EXCEPTIONS.create(48,se,cid,id);
			//throw new OXException("ERROR DURING DISTRIBUTION LIST deleteDistributionListEntriesByIds, DLIST NOT UPDATED cid="+cid+" oid="+id, se);
		} finally {
			try{
				if (ps != null){
					ps.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Connection", see);
			}
		}
	
		if (dleos.length > 0) {
			try {				
				for (int i=0;i<dleos.length;i++){
					dleo = dleos[i];					 
					ps = writecon.prepareStatement(cs.iFdeleteDistributionListEntriesByIds2());
					
					try {
						ps.setInt(1, id);
						
						if (dleo.containsEntryID() && dleo.getEntryID() > 0){
							ps.setInt(2, dleo.getEntryID());			
							ps.setInt(3, dleo.getEmailfield());			
						}
						ps.setInt(4, cid);
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuilder("DELETE FROM DLIST ").append(ps.toString()));
						}
						ps.execute();
					} finally {
						try{
							if (ps != null){
								ps.close();
							}
						} catch (SQLException see){
							LOG.warn("Unable to close Connection", see);
						}
					}
				}
			} catch (SQLException se) {
				throw EXCEPTIONS.create(31,se,cid,id);
				//throw new OXException("ERROR DURING DISTRIBUTION LIST deleteDistributionListEntriesByIds, DLIST NOT UPDATED cid="+cid+" oid="+id, se);
			}
		}
	}

	@OXThrows(
			category=Category.CODE_ERROR,
			desc="32",
			exceptionId=32,
			msg="Unable to Load Linked Contacts : Context %1$d Contact %2$d"
	)
	public static LinkEntryObject[] fillLinkArray(ContactObject co, int user, int[] group, Context ctx, UserConfiguration uc, Connection readcon) throws OXException {		
		
		Statement smt = null;
		ResultSet rs = null;
		LinkEntryObject[] r = null;
		ContactSql cs = new ContactMySql(null);
		try{
			int id = co.getObjectID();
			
			smt = readcon.createStatement();		
			rs = smt.executeQuery(cs.iFgetFillLinkArrayString(id, ctx.getContextId()));

			rs.last();
			int size = rs.getRow();
			rs.beforeFirst();

			LinkEntryObject[] leos = new LinkEntryObject[size];
			LinkEntryObject leo = null;

			String contact_displayname = null;
			String link_displayname = null;
			int linkid = 0;

			int cnt = 0;

			while (rs.next()) {
				leo = new LinkEntryObject();

				leo.setContactID(id);

				contact_displayname = rs.getString(3);
				if (!rs.wasNull()){
					leo.setContactDisplayname(contact_displayname);
				}
				link_displayname = rs.getString(4);
				if (!rs.wasNull()){
					leo.setLinkDisplayname(link_displayname);
				}
				linkid = rs.getInt(2);
				if (!rs.wasNull()){
					leo.setLinkID(linkid);
					/*
					if (!performContactReadCheckByID(linkid, user,group,so)){
						continue;
					}
					*/
				}
							
				leos[cnt] = leo;
				cnt++;
			}
			
			r = new LinkEntryObject[cnt];
			System.arraycopy(leos, 0, r, 0, cnt);     
		} catch (SQLException se) {
			throw EXCEPTIONS.create(32,se,ctx.getContextId(),co.getObjectID());
			//throw new OXException("ERROR DURING fillLinkArray cid="+ctx.getContextId()+" oid="+co.getObjectID(), se);
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){	
				LOG.warn("Unable to close Connection", see);
			}
		}
		
		return r;
	}

	@OXThrows(
			category=Category.CODE_ERROR,
			desc="33",
			exceptionId=33,
			msg="Unable to save Linking between Contacts : Context %1$d Contact %2$d"
	)
	public static void writeContactLinkArrayInsert(LinkEntryObject[] leos, int id, int cid, Connection writecon) throws OXException {
		LinkEntryObject leo = null;
		ContactSql cs = new ContactMySql(null);
		try{
			for (int i=0;i<leos.length;i++){
				PreparedStatement ps = null;
				try {
					leo = leos[i];		
					ps = writecon.prepareStatement(cs.iFwriteContactLinkArrayInsert());
					ps.setInt(1, id);
					ps.setInt(2, leo.getLinkID());			
					ps.setString(3, leo.getContactDisplayname());
					ps.setString(4, leo.getLinkDisplayname());
					ps.setInt(5, cid);
					if (LOG.isDebugEnabled()) {
						LOG.debug(new StringBuilder("INSERT LINKAGE ").append(ps.toString()));
					}
					ps.execute();
				} finally {
					try{
						if (ps != null){
							ps.close();
						}
					} catch (SQLException see){
						LOG.warn("Unable to close Connection", see);
					}
				}
			}
		} catch (SQLException se) {
			throw EXCEPTIONS.create(33,se,cid,id);
			//throw new OXException("ERROR DURING DISTRIBUTION LIST UPDATE, DLIST NOT UPDATED cid="+cid+" oid="+id, se);
		}
	}
	
	public static void writeContactLinkArrayUpdate(LinkEntryObject[] leos, LinkEntryObject[] original, int id, int cid, Connection writecon) throws OXException {

		int sizey = 0;
		if (leos != null && original != null){
			sizey = leos.length + original.length;
		}else if (leos != null && original == null){
			sizey = leos.length;
		}else if (leos == null && original != null){
			sizey = original.length;
		}else{
			sizey = 1;
		}
		LinkEntryObject[] inserts = new LinkEntryObject[sizey];
		LinkEntryObject[] deletes = new LinkEntryObject[sizey];
		int delete_count = 0;
		int insert_count = 0;
		
		for (int i=0;i<leos.length;i++){
			LinkEntryObject new_leo = leos[i];
			boolean action = false;
			
			if (original != null){
				for(int u=0;u<original.length;u++){
					LinkEntryObject old_leo = original[u];
					
					if (new_leo.compare(old_leo)){
						// found this link in the old ones
						original[u] = null;
						action = true;
						break;
					}else{
						// this one don't equal
						action = false;
					}
				}
			}
			if (!action){
				// nothing found so it is a new one
				inserts[insert_count] = new_leo;
				insert_count++;
			}
		}
		if (original != null){
			for (int i=0;i<original.length;i++){
				LinkEntryObject del_leo = original[i];
				if (del_leo != null){
					deletes[delete_count] = del_leo;
					delete_count++;
				}
			}
		}
		LinkEntryObject[] deletecut = new LinkEntryObject[delete_count];
		System.arraycopy(deletes, 0, deletecut, 0, delete_count);
		
		LinkEntryObject[] insertcut = new LinkEntryObject[insert_count];
		System.arraycopy(inserts, 0, insertcut, 0, insert_count);

		
		try{
			deleteLinkEntriesByIds(id, deletecut ,cid, writecon);
			writeContactLinkArrayInsert(insertcut, id, cid, writecon);
		} catch (OXException x){
			throw x;
			//throw new OXException("UNABLE TO UPDATE CONTACT LIST cid="+cid+" oid="+id,x);
		}
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="34",
			exceptionId=34,
			msg="Unable to delete Linking between Contacts : Context %1$d Contact %2$d"
	)
	public static void deleteLinkEntriesByIds(int id, LinkEntryObject[] leos, int cid, Connection writecon) throws OXException {
		if (leos.length > 0) {
			LinkEntryObject leo = null;
			try{
				for (int i=0;i<leos.length;i++){
					leo = leos[i];
					PreparedStatement ps = null;
					try {
						ContactSql cs = new ContactMySql(null);
						ps = writecon.prepareStatement(cs.iFgetdeleteLinkEntriesByIdsString());
						ps.setInt(1, id);
						ps.setInt(2, leo.getLinkID());
						ps.setInt(3, cid);
						if (LOG.isDebugEnabled()) {
							LOG.debug(new StringBuilder("DELETE LINKAGE ENTRY").append(ps.toString()));
						}
						ps.execute();
					} finally {
						try{
							if (ps != null){
								ps.close();
							}
						} catch (SQLException see){		
							LOG.warn("Unable to close Connection", see);
						}
					}
				}
			} catch (SQLException se) {
				throw EXCEPTIONS.create(34,se,cid,id);
				//throw new OXException("ERROR DURING LINK LIST UPDATE, LINK NOT UPDATED cid="+cid+" oid="+id, se);
			}
		}
	}
	
	public static Date getContactImageLastModified(int id, int cid, Connection readcon) throws SQLException {
		Date last_mod = null;
		Statement smt = null;
		ResultSet rs = null;
		try{
			ContactSql cs = new ContactMySql(null);	
			smt = readcon.createStatement();
			rs = smt.executeQuery(cs.iFgetContactImageLastModified(id,cid));
		    if (rs.next()){
		    	last_mod = new Date(rs.getLong(1));
		    }
		} catch (SQLException sxe){
			throw sxe;
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement or ResultSet",see);
			}
		}
	    
		return last_mod;
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="35",
			exceptionId=35,
			msg="Unable to load Contact Image : Context %1$d Contact %2$d"
	)
	public static void getContactImage(int contact_id, ContactObject co, int cid, Connection readcon) throws OXException {
		Date last_mod = null;
		
		Statement smt = null;
		ResultSet rs = null;
		try {
			ContactSql cs = new ContactMySql(null);	
			smt = readcon.createStatement();
		    rs = smt.executeQuery(cs.iFgetContactImage(contact_id,cid));
		    if (rs.next()){
		    	byte[] bb = rs.getBytes(1);
		    	if (! rs.wasNull()){
			    	last_mod = new Date(rs.getLong(2));
				    co.setImageLastModified(last_mod);
				    co.setImage1(bb);
				    co.setImageContentType(rs.getString(3));
		    	}
		    }
		} catch (SQLException se) {
			throw EXCEPTIONS.create(35,se,cid,contact_id);
			//throw new OXException("ERROR DURING CONTACT IMAGE LOAD cid="+cid+" oid="+contact_id, se);
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Statement or ResultSet", see);
			}
		}
	}

	@OXThrowsMultiple(
			category={	Category.USER_INPUT,
									Category.CODE_ERROR
								},
			desc={"36","37"},
			exceptionId={36,37},
			msg={ "Unable to save contact image. The image appears to be broken.",
						"Unable to save Contact Image: Context %1$d Contact %2$d"
						}
	)
	public static void writeContactImage(int contact_id, byte[] img, int cid, String mime, Connection writecon) throws OXException {
		//System.out.println("contact_id -> "+contact_id+" img -> "+img+" cid -> "+cid+" mime -> "+mime+" img.length -> "+img.length+" mime.length -> "+mime.length());
		if (contact_id < 1 || img == null || img.length < 1 || cid < 1 || mime == null || mime.length() < 1){
			throw EXCEPTIONS.createOXConflictException(36);
			//throw new OXConflictException("Wrong Data in Image Save");
		}
		
		PreparedStatement ps = null;
		try {
			ContactSql cs = new ContactMySql(null);	
			ps = writecon.prepareStatement(cs.iFwriteContactImage());
			ps.setInt(1, contact_id);
			ps.setBytes(2, img);
			ps.setString(3, mime);
			ps.setInt(4, cid);
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("INSERT IMAGE ").append(ps.toString()));
			}
			ps.execute();
		} catch (SQLException se) {
			throw EXCEPTIONS.create(37,se,cid,contact_id);
			//throw new OXException("ERROR DURING CONTACT IMAGE SAVE cid="+cid+" oid="+contact_id, se);
		} finally {
			try{
				if (ps != null){
					ps.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement",see);
			}
		}
	}

	@OXThrowsMultiple(
			category={	Category.USER_INPUT,
									Category.CODE_ERROR
								},
			desc={"38","39"},
			exceptionId={38,39},
			msg={ "Unable to update contact image. The image appears to be broken."	,
						"Unable to update Contact Image: Context %1$d Contact %2$d"
						}
	)
	public static void updateContactImage(int contact_id, byte[] img, int cid, String mime, Connection writecon) throws OXException {
		if (contact_id < 1 || img == null || img.length < 1 || cid < 1 || mime == null || mime.length() < 1) {
			throw EXCEPTIONS.createOXConflictException(38);
			//throw new OXConflictException("Wrong Data in Image Save");
		}
		
		PreparedStatement ps = null;
		try {
			ContactSql cs = new ContactMySql(null);	
			ps = writecon.prepareStatement(cs.iFupdateContactImageString());
			ps.setInt(1, contact_id);
			ps.setBytes(2, img);
			ps.setString(3, mime);
			ps.setInt(4, cid);
			ps.setInt(5, contact_id);
			ps.setInt(6, cid);
			if (LOG.isDebugEnabled()) {
				LOG.debug(new StringBuilder("UPDATE IMAGE ").append(ps.toString()));
			}
			ps.execute();
		} catch (SQLException se) {
			throw EXCEPTIONS.create(39,se,cid,contact_id);
			//throw new OXException("ERROR DURING CONTACT IMAGE UPDATE cid="+cid+" oid="+contact_id, se);
		} finally {
			try{ 
				if (ps != null){
					ps.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement",see);
			}
		}
	}
	
	public static boolean performContactReadCheckByID(int objectId, int user, int[] group, Context ctx, UserConfiguration uc) throws DBPoolingException {
		
		Connection readCon = null;
		ResultSet rs = null;
		Statement st = null;
		try{
			
			ContactSql cs = new ContactMySql(ctx,user);		
			cs.setSelect(cs.iFgetRightsSelectString()); 	    		
			cs.setObjectID(objectId);	
			
			readCon = DBPool.pickup(ctx);
			st = readCon.createStatement(); 
			rs = st.executeQuery(cs.getSqlCommand());
			
			int fid = -1;
			int created_from = -1;
			boolean pflag = false;
			
			if (rs.next()) {
				fid = rs.getInt(5);
				created_from = rs.getInt(6);
				int xx = rs.getInt(7);
				if (!rs.wasNull() && xx == 1){
						pflag = true;
				}
			}else{
				return false;
			}
			if(pflag && (created_from != user)){
					return false;
			}
			if (fid != -1 && created_from != -1){
				return performContactReadCheck(fid,created_from,user,group,ctx,uc,readCon);
			}else{
				return false;
			}
		} catch (SQLException e){
			LOG.error("UNABLE TO performContactReadCheckByID cid="+ctx.getContextId()+" oid="+objectId, e);
			return false;
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (st != null){
					st.close();
				}
			} catch (SQLException see){
				LOG.error("Unablel to close Statement or ResultSet",see);
			}
			try{
				if (readCon != null){
					DBPool.closeReaderSilent(ctx, readCon);
				}
			} catch (Exception see){
				LOG.error("Unable to return Connection",see);
			}
		}
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="50",
			exceptionId=50,
			msg=ContactException.INIT_CONNECTION_FROM_DBPOOL
	)
	public static boolean performContactReadCheckByID(int folderId, int objectId, int user, int[] group, Context ctx, UserConfiguration uc) throws OXException {
				
		Connection readCon = null;
		ResultSet rs = null;
		Statement st = null;
		try{
			
			ContactSql cs = new ContactMySql(ctx,user);	
			cs.setSelect(cs.iFgetRightsSelectString());   		
			cs.setObjectID(objectId);	

			readCon = DBPool.pickup(ctx);
			st = readCon.createStatement(); 
			rs = st.executeQuery(cs.getSqlCommand());
			
			int fid = -1;
			int created_from = -1;
			boolean pflag = false;
			
			if (rs.next()) {
				fid = rs.getInt(5);
				created_from = rs.getInt(6);
				int xx = rs.getInt(7);
				if (!rs.wasNull() && xx == 1){
						pflag = true;
				}
			}else{
				return false;
			}
			if(pflag && (created_from != user)){
					return false;
			}
			if (fid != folderId){
				return false;
			}
			if (fid != -1 && created_from != -1){
				return performContactReadCheck(fid,created_from,user,group,ctx,uc,readCon);
			}else{
				return false;
			}
		} catch (DBPoolingException e){
			throw EXCEPTIONS.create(50,e);
		} catch (SQLException e){
			LOG.error("UNABLE TO performContactReadCheckByID cid="+ctx.getContextId()+" oid="+objectId, e);
			return false;
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (st != null){
					st.close();
				}
			} catch (SQLException see){
				LOG.error("Unablel to close Statement or ResultSet",see);
			}
			try{
				if (readCon != null){
					DBPool.closeReaderSilent(ctx, readCon);
				}
			} catch (Exception see){
				LOG.error("Unable to return Connection",see);
			}
		}
	}

	public static boolean performContactReadCheck(int folderId,int created_from, int user, int[] group, Context ctx, UserConfiguration uc, Connection readCon){
		try{
			FolderObject contactFolder;
			if (FolderCacheManager.isEnabled()){
				contactFolder = FolderCacheManager.getInstance().getFolderObject(folderId, true, ctx, readCon);
			}else{
				contactFolder = FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
			}
			if (contactFolder.getModule() != FolderObject.CONTACT) {
				return false;
			}
			OXFolderAccess oxfs = new OXFolderAccess(readCon, ctx);
			EffectivePermission oclPerm = oxfs.getFolderPermission(folderId, user, uc);

			if (oclPerm.getFolderPermission() <= OCLPermission.NO_PERMISSIONS) {
				return false;
			}	    		
			if (!oclPerm.canReadAllObjects()) {
				if (oclPerm.canReadOwnObjects() && created_from == user) {
					return true;
				} else {
					return false;
				}
			}else{
				return true;
			}
		} catch (OXException e){
			LOG.error("UNABLE TO PERFORM performContactReadCheck cid="+ctx.getContextId()+" fid="+folderId, e);
			return false;
		}
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="49",
			exceptionId=49,
			msg=ContactException.INIT_CONNECTION_FROM_DBPOOL
	)
	public static boolean performContactWriteCheckByID(int folderId, int objectId, int user, int[] group, Context ctx, UserConfiguration uc) throws OXException {
		
		Connection readCon = null;
		ResultSet rs = null;
		Statement st = null;
		try{
			
			ContactSql cs = new ContactMySql(ctx,user);		
			cs.setSelect(cs.iFgetRightsSelectString());	    		
			cs.setObjectID(objectId);	
			
			readCon = DBPool.pickup(ctx);
			st = readCon.createStatement(); 
			rs = st.executeQuery(cs.getSqlCommand());
			
			int fid = -1;
			int created_from = -1;
			boolean pflag = false;
			
			if (rs.next()) {
				fid = rs.getInt(5);
				created_from = rs.getInt(6);
				int xx = rs.getInt(7);
				if (!rs.wasNull() && xx == 1){
						pflag = true;
				}
			}else{
				return false;
			}
			if(pflag && (created_from != user)){
					return false;
			}
			if (fid != folderId){
				return false;
			}
			
			if (fid != -1 && created_from != -1){
				return performContactWriteCheck(fid,created_from,user,group,ctx,uc,readCon);
			}else{
				return false;
			}
		} catch (DBPoolingException e){
			throw EXCEPTIONS.create(49,e);
		} catch (SQLException e){
			LOG.error("UNABLE TO performContactWriteCheckByID cid="+ctx.getContextId()+" oid="+objectId, e);
			return false;
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (st != null){
					st.close();
				}
			} catch (SQLException see){
				LOG.error("Unablel to close Statement or ResultSet",see);
			}
			try{
				if (readCon != null){
					DBPool.closeReaderSilent(ctx, readCon);
				}
			} catch (Exception see){
				LOG.error("Unable to return Connection",see);
			}
		}
	}
	
	public static boolean performContactWriteCheck(int folderId,int created_from, int user, int[] group, Context ctx, UserConfiguration uc, Connection readCon){
		try{
			FolderObject contactFolder;
			if (FolderCacheManager.isEnabled()){
				contactFolder = FolderCacheManager.getInstance().getFolderObject(folderId, true, ctx, readCon);
			}else{
				contactFolder = FolderObject.loadFolderObjectFromDB(folderId, ctx, readCon);
			}
			if (contactFolder.getModule() != FolderObject.CONTACT) {
				return false;
			}
			OXFolderAccess oxfs = new OXFolderAccess(readCon, ctx);
			EffectivePermission oclPerm = oxfs.getFolderPermission(folderId, user, uc);
			if (oclPerm.getFolderPermission() <= OCLPermission.NO_PERMISSIONS) {
				return false;
			}	    		
			if (!oclPerm.canWriteAllObjects()) {
				if (oclPerm.canWriteOwnObjects() && created_from == user) {
					return true;
				} else {
					return false;
				}
			}else{
				return true;
			}
		} catch (OXException e){
			LOG.error("UNABLE TO PERFORM performContactWriteCheck cid="+ctx.getContextId()+" fid="+folderId, e);
			return false;
		}
	}
	
	@OXThrows(
			category=Category.CODE_ERROR,
			desc="40",
			exceptionId=40,
			msg="Unable to perform Contact Folder check for readable content: Context %1$d Folder %2$d"
	)
	public static boolean containsForeignObjectInFolder(int fid, int uid, SessionObject so) throws OXException, DBPoolingException {
		Connection readCon = null;
		ResultSet rs = null;
		Statement st = null;
		try{
			readCon = DBPool.pickup(so.getContext());
			st = readCon.createStatement();
			ContactSql cs = new ContactMySql(null);
			rs = st.executeQuery(cs.iFcontainsForeignObjectInFolder(fid,uid,so.getContext().getContextId()));
			if (rs.next()){
				return true;
			}else{
				return false;
			}
		} catch (SQLException se){
			throw EXCEPTIONS.create(40, se, so.getContext().getContextId(), fid);
			//throw new OXException("UNABLE TO PERFORM ForeignObjectCheck");
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (st != null){
					st.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close ResultSet or Statement",see);
			}
			try{
				if (readCon != null){
					DBPool.closeReaderSilent(so.getContext(), readCon);
				}
			} catch (Exception e){
				LOG.error("Unable to return Connection");
			}
		}
	}
	
	public static boolean containsAnyObjectInFolder(int fid, Context cx) throws OXException {
		Connection readCon = null;
		ResultSet rs = null;
		Statement st = null;
		try{
			readCon = DBPool.pickup(cx);
			st = readCon.createStatement(); 
			ContactSql cs = new ContactMySql(null);
			rs = st.executeQuery(cs.iFgetFolderSelectString(fid, cx.getContextId()));
			if (rs.next()){
				return true;
			}else{
				return false;
			}
		} catch (DBPoolingException se){
			LOG.error("Unable to perform containsAnyObjectInFolder check. Cid: "+cx.getContextId()+" Fid: "+fid+" Cause:"+se);
			return false;
		} catch (SQLException se){
			LOG.error("Unable to perform containsAnyObjectInFolder check. Cid: "+cx.getContextId()+" Fid: "+fid+" Cause:"+se);
			return false;
			//throw EXCEPTIONS.create(41, se, cx.getContextId(), fid);
			//throw new OXException("UNABLE TO PERFORM containsAnyObjectCheck");
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (st != null){
					st.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close ResultSet or Statement",see);
			}
			try{
				if (readCon != null){
					DBPool.closeReaderSilent(cx, readCon);
				}
			} catch (Exception e){
				LOG.error("Unable to return Connection");
			}
		}
	} 
	
	public static void deleteContactsFromFolder(int fid, int user, int[] group, SessionObject so, Connection readcon, Connection writecon) throws OXException {
		trashContactsFromFolder(fid, so, readcon, writecon, true);
	}	
	
	@OXThrowsMultiple(
			category={	Category.PERMISSION,
									Category.CODE_ERROR,
									Category.CODE_ERROR,
									Category.CODE_ERROR
								},
			desc={"42","44","45","46"},
			exceptionId={42,44,45,46},
			msg={	ContactException.NO_DELETE_PERMISSION_MSG,			
							"Critical Error occurred. This folder contains a contact with no id. Context %1$d Folder %2$d",						
							"Unable to delete Contacts from this folder. Context %1$d Folder %2$d",
							"Unable to trigger Object Events: Context %1$d Folder %2$d"
						}
	)
	public static void trashContactsFromFolder(int fid,  SessionObject so, Connection readcon, Connection writecon, boolean delit) throws OXException {

		Statement read = null; 
		Statement del = null;
		ResultSet rs = null;
		
		try {	
			read = readcon.createStatement();
			del = writecon.createStatement();
					
			try{
				FolderObject contactFolder;
				if (FolderCacheManager.isEnabled()){
					contactFolder = FolderCacheManager.getInstance().getFolderObject(fid, true, so.getContext(), readcon);
				}else{
					contactFolder = FolderObject.loadFolderObjectFromDB(fid, so.getContext(), readcon);
				}
				if (contactFolder.getModule() != FolderObject.CONTACT) {
					throw EXCEPTIONS.createOXConflictException(42, fid, so.getContext().getContextId(), so.getUserObject().getId());
					//throw new OXException("YOU TRY TO DELETE FROM A NON CONTACT FOLDER! cid="+so.getContext().getContextId()+" fid="+fid);
				}
				if (contactFolder.getType() == FolderObject.PRIVATE){
					delit = true;
				}
			} catch (OXException e){
				throw e;
				//throw EXCEPTIONS.create(43, fid, so.getContext().getContextId(), user);
				//throw new OXException("NO PERMISSIONS TO DELETE IN THIS FOLDER cid="+so.getContext().getContextId()+" fid="+fid,e);
			}			
			
			ContactSql cs = new ContactMySql(so);
			cs.setFolder(fid);
			cs.setSelect(cs.iFgetRightsSelectString());
			
			rs =read.executeQuery(cs.getSqlCommand());
			
			EventClient ec = new EventClient(so);
			
			int oid = 0;
			int dlist = 0;
			int link = 0;
			int image = 0;
			int created_from = 0;
			
			while (rs.next()){

				oid = rs.getInt(1);
				if (rs.wasNull()){
					throw EXCEPTIONS.create(44, so.getContext().getContextId(), fid);
					//throw new OXException("VERY BAD ERROR OCCURRED, OBJECT WITHOUT ID FOUND cid="+so.getContext().getContextId()+" fid="+fid);
				}
				dlist = rs.getInt(2);
				if (!rs.wasNull() && dlist > 0){
					trashDistributionList(oid, so.getContext().getContextId(), writecon, delit);
				}
				link = rs.getInt(3);
				if (!rs.wasNull() && link > 0){
					trashLinks(oid, so.getContext().getContextId(), writecon, delit);
				}
				image = rs.getInt(4);
				if (!rs.wasNull() && image > 0){
					trashImage(oid, so.getContext().getContextId(), writecon, delit);
				}
				created_from = rs.getInt(6);

				cs.iFtrashContactsFromFolder(delit,del,oid,so.getContext().getContextId());
				
				ContactObject co = new ContactObject();
				co.setCreatedBy(created_from);
				co.setParentFolderID(fid);
				co.setObjectID(oid);
				
				ec.delete(co);
			}
			
			if (LOG.isDebugEnabled()) {
				LOG.debug(cs.iFtrashContactsFromFolderUpdateString(fid,so.getContext().getContextId()));
			}
			del.execute(cs.iFtrashContactsFromFolderUpdateString(fid,so.getContext().getContextId()));

		} catch (InvalidStateException is){
			throw EXCEPTIONS.create(46,is, so.getContext().getContextId(), fid);
			//throw new OXException("UNABLE TO DELTE FOLDER OBJECTS cid="+so.getContext().getContextId()+" fid="+fid,se);
		} catch (SQLException se) {
			throw EXCEPTIONS.create(45,se, so.getContext().getContextId(), fid);
			//throw new OXException("UNABLE TO DELTE FOLDER OBJECTS cid="+so.getContext().getContextId()+" fid="+fid,se);
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (read != null){
					read.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Statement or ResultSet", see);
			}
			try{
				if (del != null){
					del.close();
				}
			} catch (SQLException see){
				LOG.warn("Unable to close Statement", see);
			}
		}
	}	
	
	public static void deleteDistributionList(int id, int cid, Connection writecon) throws SQLException {
		trashDistributionList(id, cid, writecon, true);
	}
	public static void trashDistributionList(int id, int cid, Connection writecon, boolean delete) throws SQLException {
		Statement smt = writecon.createStatement();
		try{
			ContactSql cs = new ContactMySql(null);
			cs.iFtrashDistributionList(delete,id,cid,smt);
		} catch (SQLException sxe){
			throw sxe;
		} finally {
			try{
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement");
			}
		}
	}
	
	public static void deleteLinks(int id, int cid, Connection writecon) throws SQLException {
		trashLinks(id, cid, writecon, true);
	}
	public static void trashLinks(int id, int cid, Connection writecon, boolean delete) throws SQLException {
		Statement smt = writecon.createStatement();
		try {
			ContactSql cs = new ContactMySql(null);
			cs.iFtrashLinks(delete,smt,id,cid);
		} catch (SQLException sxe){
			throw sxe;
		} finally {
			try{
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement");
			}
		}
	}
	
	public static void deleteImage(int id, int cid, Connection writecon) throws SQLException {
		trashImage(id,cid,writecon,true);
	}
	public static void trashImage(int id, int cid, Connection writecon, boolean delete) throws SQLException {
		Statement smt = writecon.createStatement();
		try{ 
			ContactSql cs = new ContactMySql(null);
			cs.iFtrashImage(delete,smt,id,cid);
		} catch (SQLException sxe){
			throw sxe;
		} finally {
			try{
				if (smt != null){
					smt.close();
				}
			} catch (SQLException see){
				LOG.error("Unable to close Statement");
			}
		}
	}
	
	public void deletePerformed(DeleteEvent sqlDelEvent, Connection readCon, Connection writeCon) throws DeleteFailedException,LdapException, SQLException, DBPoolingException {
	
		try{
			if (sqlDelEvent.getType() == DeleteEvent.TYPE_USER){
				trashAllUserContacts(sqlDelEvent.getId(),sqlDelEvent.getSession(),readCon,writeCon);
			}
		}catch (OXException ox){
			throw new DeleteFailedException(ox);
		} catch (LdapException e) {
			throw e;
		} catch (SQLException e) {
			throw e;
		}
	}
	
	@OXThrowsMultiple(
			category={	Category.PERMISSION,
									Category.PERMISSION,
									Category.PERMISSION
								},
			desc={"47","52","57"},
			exceptionId={47,52,57},
			msg={	"Unable to delete Contacts from User. Context %1$d User %2$d",
							"Unable to delete contacts from user because this is a non-contact folder. Context %1$d Folder %2$d User %3$d",
							"Unable to trigger Object Events: Context %1$d User %2$d"
						}
	)
	public static void trashAllUserContacts(int uid, SessionObject so, Connection readcon, Connection writecon) throws OXException {
		Statement read = null; 
		Statement del = null;
		ResultSet rs = null;
		ContactSql cs = new ContactMySql(null);
		
		try {	
			read = readcon.createStatement();
			del = writecon.createStatement();
			
			FolderObject contactFolder = null;

			rs = read.executeQuery(cs.iFgetRightsSelectString(uid,so.getContext().getContextId()));
			
			int fid = 0;
			int oid = 0;
			int created_from = 0;
			boolean delete =false;

			//writecon.setAutoCommit(false);	
			
			EventClient ec = new EventClient(so);
			
			while (rs.next()){
				delete = false;
				oid = rs.getInt(1);
				fid = rs.getInt(5);
				created_from = rs.getInt(6);
				
				if (FolderCacheManager.isEnabled()){
					contactFolder = FolderCacheManager.getInstance().getFolderObject(fid, true, so.getContext(), readcon);
				}else{
					contactFolder = FolderObject.loadFolderObjectFromDB(fid, so.getContext(), readcon);
				}
				if (contactFolder.getModule() != FolderObject.CONTACT) {
					throw EXCEPTIONS.create(52, so.getContext().getContextId(), fid, uid);
					//throw new OXException("YOU TRY TO DELETE FROM A NON CONTACT FOLDER! cid="+so.getContext().getContextId()+" uid="+uid);
				}
				if (contactFolder.getType() == FolderObject.PRIVATE){
					delete = true;
				}
				cs.iFtrashAllUserContacts(delete,del,so.getContext().getContextId(),oid,uid,rs,so);
				
				ContactObject co = new ContactObject();
				co.setCreatedBy(created_from);
				co.setParentFolderID(fid);
				co.setObjectID(oid);
				ec.delete(co);
				
			}
			cs.iFtrashAllUserContactsDeletedEntries(del,so.getContext().getContextId(),uid,so);
			//writecon.commit();
		} catch (InvalidStateException ox) {
			throw EXCEPTIONS.create(57, so.getContext().getContextId(),uid);
		} catch (OXException ox) {
			throw ox;
		} catch (SQLException se) {
			/*
			try {
				writecon.rollback();
			} catch (SQLException see){
				LOG.error("Uable to rollback SQL DELETE", see);
			}
			*/
			throw EXCEPTIONS.create(47,se, so.getContext().getContextId(), uid);
		} finally {
			try{
				if (rs != null){
					rs.close();
				}
				if (read != null){
					read.close();
				}
			} catch (SQLException sxe){
				LOG.error("Unable to close Statement or ResultSet",sxe);
			}
			try{
				if (del != null){
					del.close();
				}
			} catch (SQLException sxe){
				LOG.error("Unable to close Statement or ResultSet",sxe);
			}
			/*
			try{
				writecon.setAutoCommit(true);
			} catch (Exception see){
				LOG.error("Unable to return Connection", see);
			}
			*/
		}
	}
	
	@OXThrows(
			category=Category.TRUNCATED,
			desc="54",
			exceptionId=54,
			msg="One or more fields contain too much information. Fields: %1$s Character Limit: %2$d Sent %3$d"
	)
	
	
	   public static OXException getTruncation(final DataTruncation se) {
		   
	        final String[] fields = DBUtils.parseTruncatedFields(se);
	        final StringBuilder sFields = new StringBuilder();
	        
	        for (String field : fields) {
	            sFields.append(field);
	            sFields.append(", ");
	        }
	        sFields.setLength(sFields.length() - 1);
	        
	        
			final OXException oxx = EXCEPTIONS.create(54,se,sFields,se.getDataSize(), se.getTransferSize());
			

	        if (fields.length > 0){
		        for (String field : fields) {
		        	for (int i =0;i<650;i++){
		        		if (mapping[i] != null && mapping[i].getDBFieldName().equals(field)){
		        			oxx.addTruncatedId(i);
		        		}
		        	}
		        }
	        }
	        
	        return oxx;
	    }

	   
	/***************** MAPPER ******************/
	
	public static interface mapper {
		boolean containsElement(ContactObject co);
		void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException;
		String getDBFieldName();
		void fillPreparedStatement(PreparedStatement ps, int position, ContactObject co) throws SQLException;
		
		boolean compare(ContactObject co, ContactObject original);
		
		void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException;
		Object getData(ResultSet rs,int pos) throws SQLException;
		
		String getValueAsString(ContactObject co);
		String getReadableTitle();
	}
	
	static {
		mapping = new mapper[700];

		/**************** * field01 *  * *************/
		mapping[ContactObject.DISPLAY_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field01";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setDisplayName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsDisplayName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getDisplayName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getDisplayName();
				String y = original.getDisplayName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}				
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			
			public String getValueAsString(ContactObject co) {
				return co.getDisplayName();
			}
			public String getReadableTitle() {
				return "Display name";
			}
		};
		/**************** * field02 *  * *************/
		mapping[ContactObject.SUR_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field02";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setSurName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsSurName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getSurName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getSurName();
				String y = original.getSurName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}		
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getSurName();
			}
			public String getReadableTitle() {
				return "Sur name";
			}
		};	
		/**************** * field03 *  * *************/
		mapping[ContactObject.GIVEN_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field03";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setGivenName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsGivenName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getGivenName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getGivenName();
				String y = original.getGivenName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getGivenName();
			}
			public String getReadableTitle() {
				return "Given name";
			}
		};			
		/**************** * field04 *  * *************/
		mapping[ContactObject.MIDDLE_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field04";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setMiddleName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsMiddleName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getMiddleName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getMiddleName();
				String y = original.getMiddleName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				
				return co.getMiddleName();
			}
			public String getReadableTitle() {
				
				return "Middle name";
			}
		};		
		/**************** * field05 *  * *************/
		mapping[ContactObject.SUFFIX] = new mapper() {
			public String getDBFieldName() {
				return "field05";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setSuffix(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsSuffix();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getSuffix());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getSuffix();
				String y = original.getSuffix();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {	
				return co.getSuffix();
			}
			public String getReadableTitle() {
				return "Suffix";
			}
		};				
		/**************** * field06 *  * *************/
		mapping[ContactObject.TITLE] = new mapper() {
			
			public String getDBFieldName() {
				return "field06";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTitle(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTitle();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTitle());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTitle();
				String y = original.getTitle();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTitle();
			}
			public String getReadableTitle() {	
				return "Title";
			}
		};				
		/**************** * field07 *  * *************/
		mapping[ContactObject.STREET_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field07";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStreetHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStreetHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStreetHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStreetHome();
				String y = original.getStreetHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getStreetHome();
			}
			public String getReadableTitle() {
				return "Street home";
			}
		};				
		/**************** * field08 *  * *************/
		mapping[ContactObject.POSTAL_CODE_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field08";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setPostalCodeHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsPostalCodeHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getPostalCodeHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getPostalCodeHome();
				String y = original.getPostalCodeHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getPostalCodeHome();
			}
			public String getReadableTitle() {
				return "Postal code home";
			}
		};				
		/**************** * field09 *  * *************/
		mapping[ContactObject.CITY_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field09";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCityHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCityHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCityHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCityHome();
				String y = original.getCityHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}			
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCityHome();
			}
			public String getReadableTitle() {
					return "City home";
			}
		};			
		/**************** * field10 *  * *************/
		mapping[ContactObject.STATE_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field10";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStateHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStateHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStateHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStateHome();
				String y = original.getStateHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}			
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getStateHome();
			}
			public String getReadableTitle() {
				return "State home";
			}
		};				
		/**************** * field11 *  * *************/
		mapping[ContactObject.COUNTRY_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field11";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCountryHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCountryHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCountryHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCountryHome();
				String y = original.getCountryHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}			
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCountryHome();
			}
			public String getReadableTitle() {
				return "Country home";
			}
		};				

		/**************** * field12 *  * *************/
		mapping[ContactObject.MARITAL_STATUS] = new mapper() {
			public String getDBFieldName() {
				return "field12";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setMaritalStatus(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsMaritalStatus();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getMaritalStatus());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getMaritalStatus();
				String y = original.getMaritalStatus();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
					return co.getMaritalStatus();
			}
			public String getReadableTitle() {
				return "Martial status";
			}
		};				
		/**************** * field13 *  * *************/
		mapping[ContactObject.NUMBER_OF_CHILDREN] = new mapper() {
			public String getDBFieldName() {
				return "field13";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setNumberOfChildren(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNumberOfChildren();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getNumberOfChildren());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getNumberOfChildren();
				String y = original.getNumberOfChildren();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getNumberOfChildren();
			}
			public String getReadableTitle() {
				return "Number of children";
			}
		};				
		/**************** * field14 *  * *************/
		mapping[ContactObject.PROFESSION] = new mapper() {
			public String getDBFieldName() {
				return "field14";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setProfession(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsProfession();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getProfession());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getProfession();
				String y = original.getProfession();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getProfession();
			}
			public String getReadableTitle() {
				return "Profession";
			}
		};				
		/**************** * field15 *  * *************/
		mapping[ContactObject.NICKNAME] = new mapper() {
			public String getDBFieldName() {
				return "field15";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setNickname(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNickname();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getNickname());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getNickname();
				String y = original.getNickname();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getNickname();
			}
			public String getReadableTitle() {
				return "Nickname";
			}
		};				
		/**************** * field16 *  * *************/
		mapping[ContactObject.SPOUSE_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field16";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setSpouseName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsSpouseName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getSpouseName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getSpouseName();
				String y = original.getSpouseName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getSpouseName();
			}
			public String getReadableTitle() {
				return "Spouse name";
			}
		};				
		/**************** * field17 *  * *************/
		mapping[ContactObject.NOTE] = new mapper() {
			public String getDBFieldName() {
				return "field17";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setNote(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNote();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getNote());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getNote();
				String y = original.getNote();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getNote();
			}
			public String getReadableTitle() {
				return "Note";
			}
		};				
		/**************** * field18 *  * *************/
		mapping[ContactObject.COMPANY] = new mapper() {
			public String getDBFieldName() {
				return "field18";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCompany(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCompany();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCompany());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCompany();
				String y = original.getCompany();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCompany();
			}
			public String getReadableTitle() {
				return "Company";
			}
		};				
		/**************** * field19 *  * *************/
		mapping[ContactObject.DEPARTMENT] = new mapper() {
			public String getDBFieldName() {
				return "field19";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setDepartment(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsDepartment();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getDepartment());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getDepartment();
				String y = original.getDepartment();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getDepartment();
			}
			public String getReadableTitle() {
				return "Department";
			}
		};			
		/**************** * field20 *  * *************/
		mapping[ContactObject.POSITION] = new mapper() {
			public String getDBFieldName() {
				return "field20";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setPosition(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsPosition();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getPosition());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getPosition();
				String y = original.getPosition();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getPosition();
			}
			public String getReadableTitle() {
				return "Position";
			}
		};				
		/**************** * field21 *  * *************/
		mapping[ContactObject.EMPLOYEE_TYPE] = new mapper() {
			public String getDBFieldName() {
				return "field21";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setEmployeeType(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsEmployeeType();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getEmployeeType());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getEmployeeType();
				String y = original.getEmployeeType();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getEmployeeType();
			}
			public String getReadableTitle() {
				return "Employee type";
			}
		};				
		/**************** * field22 *  * *************/
		mapping[ContactObject.ROOM_NUMBER] = new mapper() {
			public String getDBFieldName() {
				return "field22";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setRoomNumber(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsRoomNumber();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getRoomNumber());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getRoomNumber();
				String y = original.getRoomNumber();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getRoomNumber();
			}
			public String getReadableTitle() {
				return "Room number";
			}
		};				
		/**************** * field23 *  * *************/
		mapping[ContactObject.STREET_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field23";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStreetBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStreetBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStreetBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStreetBusiness();
				String y = original.getStreetBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
					return co.getStreetBusiness();
			}
			public String getReadableTitle() {
				return "Street business";
			}
		};				
		/**************** * field24 *  * *************/
		mapping[ContactObject.POSTAL_CODE_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field24";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setPostalCodeBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsPostalCodeBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getPostalCodeBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getPostalCodeBusiness();
				String y = original.getPostalCodeBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getPostalCodeBusiness();
			}
			public String getReadableTitle() {
				return "Postal code business";
			}
		};				
		/**************** * field25 *  * *************/
		mapping[ContactObject.CITY_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field25";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCityBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCityBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCityBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCityBusiness();
				String y = original.getCityBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCityBusiness();
			}
			public String getReadableTitle() {
				return "City business";
			}
		};				
		/**************** * field26 *  * *************/
		mapping[ContactObject.STATE_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field26";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStateBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStateBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStateBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStateBusiness();
				String y = original.getStateBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getStateBusiness();
			}
			public String getReadableTitle() {
				return "State business";
			}
		};				
		/**************** * field27 *  * *************/
		mapping[ContactObject.COUNTRY_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field27";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCountryBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCountryBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCountryBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCountryBusiness();
				String y = original.getCountryBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
					return co.getCountryBusiness();
			}
			public String getReadableTitle() {
				return "Country business";
			}
		};				
		/**************** * field28 *  * *************/
		mapping[ContactObject.NUMBER_OF_EMPLOYEE] = new mapper() {
			public String getDBFieldName() {
				return "field28";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setNumberOfEmployee(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNumberOfEmployee();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getNumberOfEmployee());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getNumberOfEmployee();
				String y = original.getNumberOfEmployee();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getNumberOfEmployee();
			}
			public String getReadableTitle() {
				return "Number of employee";
			}
		};				
		/**************** * field29 *  * *************/
		mapping[ContactObject.SALES_VOLUME] = new mapper() {
			public String getDBFieldName() {
				return "field29";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setSalesVolume(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsSalesVolume();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getSalesVolume());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getSalesVolume();
				String y = original.getSalesVolume();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getSalesVolume();
			}
			public String getReadableTitle() {
				return "Sales volume";
			}
		};				
		/**************** * field30 *  * *************/
		mapping[ContactObject.TAX_ID] = new mapper() {
			public String getDBFieldName() {
				return "field30";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTaxID(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTaxID();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTaxID());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTaxID();
				String y = original.getTaxID();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public String getValueAsString(ContactObject co) {
				return co.getTaxID();
			}
			public String getReadableTitle() {
				return "Tax id";
			}
		};				
		/**************** * field31 *  * *************/
		mapping[ContactObject.COMMERCIAL_REGISTER] = new mapper() {
			public String getDBFieldName() {
				return "field31";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCommercialRegister(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCommercialRegister();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCommercialRegister());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCommercialRegister();
				String y = original.getCommercialRegister();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCommercialRegister();
			}
			public String getReadableTitle() {
				return "Commercial register";
			}
		};				
		/**************** * field32 *  * *************/
		mapping[ContactObject.BRANCHES] = new mapper() {
			public String getDBFieldName() {
				return "field32";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setBranches(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsBranches();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getBranches());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getBranches();
				String y = original.getBranches();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getBranches();
			}
			public String getReadableTitle() {
				return "Branches";
			}
		};				
		/**************** * field33 *  * *************/
		mapping[ContactObject.BUSINESS_CATEGORY] = new mapper() {
			public String getDBFieldName() {
				return "field33";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setBusinessCategory(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsBusinessCategory();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getBusinessCategory());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getBusinessCategory();
				String y = original.getBusinessCategory();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getBusinessCategory();
			}
			public String getReadableTitle() {
				return "Business category";
			}
		};				
		/**************** * field34 *  * *************/
		mapping[ContactObject.INFO] = new mapper() {
			public String getDBFieldName() {
				return "field34";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setInfo(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsInfo();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getInfo());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getInfo();
				String y = original.getInfo();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getInfo();
			}
			public String getReadableTitle() {
				return "Info";
			}
		};				
		/**************** * field35 *  * *************/
		mapping[ContactObject.MANAGER_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field35";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setManagerName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsManagerName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getManagerName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getManagerName();
				String y = original.getManagerName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getManagerName();
			}
			public String getReadableTitle() {
				return "Manager's name";
			}
		};				
		/**************** * field36 *  * *************/
		mapping[ContactObject.ASSISTANT_NAME] = new mapper() {
			public String getDBFieldName() {
				return "field36";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setAssistantName(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsAssistantName();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getAssistantName());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getAssistantName();
				String y = original.getAssistantName();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getAssistantName();
			}
			public String getReadableTitle() {
				return "Assistant's name";
			}
		};				
		/**************** * field37 *  * *************/
		mapping[ContactObject.STREET_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field37";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStreetOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStreetOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStreetOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStreetOther();
				String y = original.getStreetOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getStreetOther();
			}
			public String getReadableTitle() {
				return "Street other";
			}
		};				
		/**************** * field38 *  * *************/
		mapping[ContactObject.POSTAL_CODE_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field38";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setPostalCodeOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsPostalCodeOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getPostalCodeOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getPostalCodeOther();
				String y = original.getPostalCodeOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getPostalCodeOther();
			}
			public String getReadableTitle() {
				return "Postal code other";
			}
		};				
		/**************** * field39 *  * *************/
		mapping[ContactObject.CITY_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field39";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCityOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCityOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCityOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCityOther();
				String y = original.getCityOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCityOther();
			}
			public String getReadableTitle() {
				return "City other"; 
			}
		};				
		/**************** * field40 *  * *************/
		mapping[ContactObject.STATE_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field40";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setStateOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsStateOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getStateOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getStateOther();
				String y = original.getStateOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getStateOther();
			}
			public String getReadableTitle() {
				return "State other";
			}
		};				
		/**************** * field41 *  * *************/
		mapping[ContactObject.COUNTRY_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field41";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCountryOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCountryOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCountryOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCountryOther();
				String y = original.getCountryOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos)throws SQLException  {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCountryOther();
			}
			public String getReadableTitle() {
				return "Country other";
			}
		};
		/**************** * field42 *  * *************/
		mapping[ContactObject.TELEPHONE_ASSISTANT] = new mapper() {
			public String getDBFieldName() {
				return "field42";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneAssistant(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneAssistant();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneAssistant());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneAssistant();
				String y = original.getTelephoneAssistant();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneAssistant();
			}
			public String getReadableTitle() {
				return "Telephone assostant";
			}
		};				
		/**************** * field43 *  * *************/
		mapping[ContactObject.TELEPHONE_BUSINESS1] = new mapper() {
			public String getDBFieldName() {
				return "field43";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneBusiness1(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneBusiness1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneBusiness1());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneBusiness1();
				String y = original.getTelephoneBusiness1();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneBusiness1();
			}
			public String getReadableTitle() {
				return "Telephone business 1";
			}
		};				
		/**************** * field44 *  * *************/
		mapping[ContactObject.TELEPHONE_BUSINESS2] = new mapper() {
			public String getDBFieldName() {
				return "field44";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneBusiness2(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneBusiness2();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneBusiness2());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneBusiness2();
				String y = original.getTelephoneBusiness2();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneBusiness2();
			}
			public String getReadableTitle() {
				return "Telephone business 2";
			}
		};				
		/**************** * field45 *  * *************/
		mapping[ContactObject.FAX_BUSINESS] = new mapper() {
			public String getDBFieldName() {
				return "field45";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setFaxBusiness(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsFaxBusiness();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getFaxBusiness());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getFaxBusiness();
				String y = original.getFaxBusiness();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getFaxBusiness();
			}
			public String getReadableTitle() {
				return "FAX business";
			}
		};				
		/**************** * field46 *  * *************/
		mapping[ContactObject.TELEPHONE_CALLBACK] = new mapper() {
			public String getDBFieldName() {
				return "field46";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneCallback(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneCallback();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneCallback());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneCallback();
				String y = original.getTelephoneCallback();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneCallback();
			}
			public String getReadableTitle() {
				return "Telephone callback";
			}
		};				
		/**************** * field47 *  * *************/
		mapping[ContactObject.TELEPHONE_CAR] = new mapper() {
			public String getDBFieldName() {
				return "field47";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneCar(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneCar();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneCar());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneCar();
				String y = original.getTelephoneCar();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneCar();
			}
			public String getReadableTitle() {
				return "Telephone car";
			}
		};				
		/**************** * field48 *  * *************/
		mapping[ContactObject.TELEPHONE_COMPANY] = new mapper() {
			public String getDBFieldName() {
				return "field48";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneCompany(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneCompany();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneCompany());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneCompany();
				String y = original.getTelephoneCompany();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneCompany();
			}
			public String getReadableTitle() {
				return "Telephone company";
			}
		};				
		/**************** * field49 *  * *************/
		mapping[ContactObject.TELEPHONE_HOME1] = new mapper() {
			public String getDBFieldName() {
				return "field49";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneHome1(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneHome1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneHome1());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneHome1();
				String y = original.getTelephoneHome1();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneHome1();
			}
			public String getReadableTitle() {
				return "Telephone home 1";
			}
		};				
		/**************** * field50 *  * *************/
		mapping[ContactObject.TELEPHONE_HOME2] = new mapper() {
			public String getDBFieldName() {
				return "field50";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneHome2(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneHome2();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneHome2());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneHome2();
				String y = original.getTelephoneHome2();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneHome2();
			}
			public String getReadableTitle() {
				return "Telephone home 2";
			}
		};				
		/**************** * field51 *  * *************/
		mapping[ContactObject.FAX_HOME] = new mapper() {
			public String getDBFieldName() {
				return "field51";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setFaxHome(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsFaxHome();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getFaxHome());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getFaxHome();
				String y = original.getFaxHome();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getFaxHome();
			}
			public String getReadableTitle() {
				return "FAX home";
			}
		};				
		/**************** * field52 *  * *************/
		mapping[ContactObject.TELEPHONE_ISDN] = new mapper() {
			public String getDBFieldName() {
				return "field52";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneISDN(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneISDN();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneISDN());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneISDN();
				String y = original.getTelephoneISDN();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneISDN();
			}
			public String getReadableTitle() {
				return "Telephone ISDN";
			}
		};				
		/**************** * field53 *  * *************/
		mapping[ContactObject.CELLULAR_TELEPHONE1] = new mapper() {
			public String getDBFieldName() {
				return "field53";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCellularTelephone1(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCellularTelephone1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCellularTelephone1());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCellularTelephone1();
				String y = original.getCellularTelephone1();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCellularTelephone1();
			}
			public String getReadableTitle() {
				return "Cellular telephone 1";
			}
		};				
		/**************** * field54 *  * *************/
		mapping[ContactObject.CELLULAR_TELEPHONE2] = new mapper() {
			public String getDBFieldName() {
				return "field54";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCellularTelephone2(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCellularTelephone2();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCellularTelephone2());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCellularTelephone2();
				String y = original.getCellularTelephone2();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCellularTelephone2();
			}
			public String getReadableTitle() {
				return "Cellular telephone 2";
			}
		};				
		/**************** * field55 *  * *************/
		mapping[ContactObject.TELEPHONE_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field55";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneOther();
				String y = original.getTelephoneOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneOther();
			}
			public String getReadableTitle() {
				return "Telephone other";
			}
		};				
		/**************** * field56 *  * *************/
		mapping[ContactObject.FAX_OTHER] = new mapper() {
			public String getDBFieldName() {
				return "field56";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setFaxOther(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsFaxOther();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getFaxOther());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getFaxOther();
				String y = original.getFaxOther();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getFaxOther();
			}
			public String getReadableTitle() {
				return "FAX other";
			}
		};				
		/**************** * field57 *  * *************/
		mapping[ContactObject.TELEPHONE_PAGER] = new mapper() {
			public String getDBFieldName() {
				return "field57";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephonePager(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephonePager();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephonePager());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephonePager();
				String y = original.getTelephonePager();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephonePager();
			}
			public String getReadableTitle() {
				return "Telephone pager";
			}
		};				
		/**************** * field58 *  * *************/
		mapping[ContactObject.TELEPHONE_PRIMARY] = new mapper() {
			public String getDBFieldName() {
				return "field58";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephonePrimary(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephonePrimary();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephonePrimary());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephonePrimary();
				String y = original.getTelephonePrimary();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephonePrimary();
			}
			public String getReadableTitle() {
				return "Telephone primary";
			}
		};				
		/**************** * field59 *  * *************/
		mapping[ContactObject.TELEPHONE_RADIO] = new mapper() {
			public String getDBFieldName() {
				return "field59";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneRadio(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneRadio();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneRadio());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneRadio();
				String y = original.getTelephoneRadio();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneRadio();
			}
			public String getReadableTitle() {
				return "Telephone radio";
			}
		};				
		/**************** * field60 *  * *************/
		mapping[ContactObject.TELEPHONE_TELEX] = new mapper() {
			public String getDBFieldName() {
				return "field60";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneTelex(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneTelex();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneTelex());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneTelex();
				String y = original.getTelephoneTelex();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneTelex();
			}
			public String getReadableTitle() {
				return "Telephone telex";
			}
		};				
		/**************** * field61 *  * *************/
		mapping[ContactObject.TELEPHONE_TTYTDD] = new mapper() {
			public String getDBFieldName() {
				return "field61";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneTTYTTD(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneTTYTTD();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneTTYTTD());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneTTYTTD();
				String y = original.getTelephoneTTYTTD();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneTTYTTD();
			}
			public String getReadableTitle() {
				return "Telephone TTY/TDD";
			}
		};				
		/**************** * field62 *  * *************/
		mapping[ContactObject.INSTANT_MESSENGER1] = new mapper() {
			public String getDBFieldName() {
				return "field62";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setInstantMessenger1(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsInstantMessenger1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getInstantMessenger1());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getInstantMessenger1();
				String y = original.getInstantMessenger1();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getInstantMessenger1();
			}
			public String getReadableTitle() {
				return "Instantmessenger 1";
			}
		};				

		/**************** * field63 *  * *************/
		mapping[ContactObject.INSTANT_MESSENGER2] = new mapper() {
			public String getDBFieldName() {
				return "field63";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setInstantMessenger2(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsInstantMessenger2();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getInstantMessenger2());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getInstantMessenger2();
				String y = original.getInstantMessenger2();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getInstantMessenger2();
			}
			public String getReadableTitle() {
				return "Instantmessenger 2";
			}
		};				
	
		/**************** * field64 *  * *************/
		mapping[ContactObject.TELEPHONE_IP] = new mapper() {
			public String getDBFieldName() {
				return "field64";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setTelephoneIP(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsTelephoneIP();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getTelephoneIP());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getTelephoneIP();
				String y = original.getTelephoneIP();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getTelephoneIP();
			}
			public String getReadableTitle() {
				return "Telephone IP";
			}
		};			
		/**************** * field65 *  * *************/
		mapping[ContactObject.EMAIL1] = new mapper() {
			public String getDBFieldName() {
				return "field65";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setEmail1(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsEmail1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getEmail1());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getEmail1();
				String y = original.getEmail1();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getEmail1();
			}
			public String getReadableTitle() {
				return "Email 1";
			}
		};			
		/**************** * field66 *  * *************/
		mapping[ContactObject.EMAIL2] = new mapper() {
			public String getDBFieldName() {
				return "field66";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setEmail2(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsEmail2();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getEmail2());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getEmail2();
				String y = original.getEmail2();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getEmail2();
			}
			public String getReadableTitle() {
				return "Email 2";
			}
		};		
		/**************** * field67 *  * *************/
		mapping[ContactObject.EMAIL3] = new mapper() {
			public String getDBFieldName() {
				return "field67";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setEmail3(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsEmail3();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getEmail3());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getEmail3();
				String y = original.getEmail3();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getEmail3();
			}
			public String getReadableTitle() {
				return "Email 3";
			}
		};		
		/**************** * field68 *  * *************/
		mapping[ContactObject.URL] = new mapper() {
			public String getDBFieldName() {
				return "field68";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setURL(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsURL();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getURL());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getURL();
				String y = original.getURL();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getURL();
			}
			public String getReadableTitle() {
				return "URL";
			}
		};		
		/**************** * field69 *  * *************/
		mapping[ContactObject.CATEGORIES] = new mapper() {
			public String getDBFieldName() {
				return "field69";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setCategories(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCategories();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getCategories());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getCategories();
				String y = original.getCategories();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getCategories();
			}
			public String getReadableTitle() {
				return "Categories";
			}
		};
		/**************** * field70 *  * *************/
		mapping[ContactObject.USERFIELD01] = new mapper() {
			public String getDBFieldName() {
				return "field70";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField01(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField01();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField01());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField01();
				String y = original.getUserField01();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField01();
			}
			public String getReadableTitle() {
				return "Dynamic Field 1";
			}
		};
		/**************** * field71 *  * *************/
		mapping[ContactObject.USERFIELD02] = new mapper() {
			public String getDBFieldName() {
				return "field71";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField02(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField02();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField02());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField02();
				String y = original.getUserField02();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField02();
			}
			public String getReadableTitle() {
				return "Dynamic Field 2";
			}
		};		
		/**************** * field72 *  * *************/
		mapping[ContactObject.USERFIELD03] = new mapper() {
			public String getDBFieldName() {
				return "field72";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField03(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField03();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField03());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField03();
				String y = original.getUserField03();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField03();
			}
			public String getReadableTitle() {
				return "Dynamic Field 3";
			}
		};		
		/**************** * field73 *  * *************/
		mapping[ContactObject.USERFIELD04] = new mapper() {
			public String getDBFieldName() {
				return "field73";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField04(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField04();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField04());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField04();
				String y = original.getUserField04();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField04();
			}
			public String getReadableTitle() {
				return "Dynamic Field 4";
			}
		};		
		/**************** * field74 *  * *************/
		mapping[ContactObject.USERFIELD05] = new mapper() {
			public String getDBFieldName() {
				return "field74";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField05(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField05();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField05());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField05();
				String y = original.getUserField05();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField05();
			}
			public String getReadableTitle() {
				return "Dynamic Field 5";
			}
		};		
		/**************** * field75 *  * *************/
		mapping[ContactObject.USERFIELD06] = new mapper() {
			public String getDBFieldName() {
				return "field75";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField06(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField06();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField06());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField06();
				String y = original.getUserField06();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField06();
			}
			public String getReadableTitle() {
				return "Dynamic Field 6";
			}
		};		
		/**************** * field76 *  * *************/
		mapping[ContactObject.USERFIELD07] = new mapper() {
			public String getDBFieldName() {
				return "field76";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField07(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField07();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField07());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField07();
				String y = original.getUserField07();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField07();
			}
			public String getReadableTitle() {
				return "Dynamic Field 7";
			}
		};		
		/**************** * field77 *  * *************/
		mapping[ContactObject.USERFIELD08] = new mapper() {
			public String getDBFieldName() {
				return "field77";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField08(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField08();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField08());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField08();
				String y = original.getUserField08();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField08();
			}
			public String getReadableTitle() {
				return "Dynamic Field 8";
			}
		};		
		/**************** * field78 *  * *************/
		mapping[ContactObject.USERFIELD09] = new mapper() {
			public String getDBFieldName() {
				return "field78";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField09(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField09();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField09());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField09();
				String y = original.getUserField09();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField09();
			}
			public String getReadableTitle() {
				return "Dynamic Field 9";
			}
		};		
		/**************** * field79 *  * *************/
		mapping[ContactObject.USERFIELD10] = new mapper() {
			public String getDBFieldName() {
				return "field79";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField10(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField10();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField10());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField10();
				String y = original.getUserField10();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField10();
			}
			public String getReadableTitle() {
				return "Dynamic Field 10";
			}
		};
		/**************** * field80 *  * *************/
		mapping[ContactObject.USERFIELD11] = new mapper() {
			public String getDBFieldName() {
				return "field80";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField11(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField11();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField11());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField11();
				String y = original.getUserField11();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField11();
			}
			public String getReadableTitle() {
				return "Dynamic Field 11";
			}
		};			
		/**************** * field81 *  * *************/
		mapping[ContactObject.USERFIELD12] = new mapper() {
			public String getDBFieldName() {
				return "field81";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField12(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField12();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField12());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField12();
				String y = original.getUserField12();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField12();
			}
			public String getReadableTitle() {
				return "Dynamic Field 12";
			}
		};			
		/**************** * field82 *  * *************/
		mapping[ContactObject.USERFIELD13] = new mapper() {
			public String getDBFieldName() {
				return "field82";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField13(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField13();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField13());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField13();
				String y = original.getUserField13();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField13();
			}
			public String getReadableTitle() {
				return "Dynamic Field 13";
			}
		};			
		/**************** * field83 *  * *************/
		mapping[ContactObject.USERFIELD14] = new mapper() {
			public String getDBFieldName() {
				return "field83";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField14(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField14();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField14());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField14();
				String y = original.getUserField14();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField14();
			}
			public String getReadableTitle() {
				return "Dynamic Field 14";
			}
		};			
		/**************** * field84 *  * *************/
		mapping[ContactObject.USERFIELD15] = new mapper() {
			public String getDBFieldName() {
				return "field84";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField15(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField15();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField15());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField15();
				String y = original.getUserField15();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField15();
			}
			public String getReadableTitle() {
				return "Dynamic Field 15";
			}
		};			
		/**************** * field85 *  * *************/
		mapping[ContactObject.USERFIELD16] = new mapper() {
			public String getDBFieldName() {
				return "field85";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField16(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField16();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField16());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField16();
				String y = original.getUserField16();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField16();
			}
			public String getReadableTitle() {
				return "Dynamic Field 16";
			}
		};			
		/**************** * field86 *  * *************/
		mapping[ContactObject.USERFIELD17] = new mapper() {
			public String getDBFieldName() {
				return "field86";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField17(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField17();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField17());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField17();
				String y = original.getUserField17();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField17();
			}
			public String getReadableTitle() {
				return "Dynamic Field 17";
			}
		};			
		/**************** * field87 *  * *************/
		mapping[ContactObject.USERFIELD18] = new mapper() {
			public String getDBFieldName() {
				return "field87";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField18(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField18();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField18());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField18();
				String y = original.getUserField18();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField18();
			}
			public String getReadableTitle() {
				return "Dynamic Field 18";
			}
		};			
		/**************** * field88 *  * *************/
		mapping[ContactObject.USERFIELD19] = new mapper() {
			public String getDBFieldName() {
				return "field88";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField19(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField19();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField19());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField19();
				String y = original.getUserField19();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField19();
			}
			public String getReadableTitle() {
				return "Dynamic Field 19";
			}
		};			
		/**************** * field89 *  * *************/
		mapping[ContactObject.USERFIELD20] = new mapper() {
			public String getDBFieldName() {
				return "field89";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setUserField20(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsUserField20();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getUserField20());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getUserField20();
				String y = original.getUserField20();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return co.getUserField20();
			}
			public String getReadableTitle() {
				return "Dynamic Field 20";
			}
		};			
		/**************** * intfield01 *  * *************/
		mapping[ContactObject.OBJECT_ID] = new mapper() {
			public String getDBFieldName() {
				return "intfield01";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					co.setObjectID(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsObjectID();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getObjectID());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (original.getObjectID() == co.getObjectID());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getObjectID()+"";
			}
			public String getReadableTitle() {
				return "Object id";
			}
		};
		/**************** * intfield02 *  * *************/
		mapping[ContactObject.NUMBER_OF_DISTRIBUTIONLIST] = new mapper() {
			public String getDBFieldName() {
				return "intfield02";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull() && t > 0){
					co.setNumberOfDistributionLists(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNumberOfDistributionLists();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getNumberOfDistributionLists());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (original.getNumberOfDistributionLists() == co.getNumberOfDistributionLists());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getNumberOfDistributionLists()+"";
			}
			public String getReadableTitle() {
				return "Number of distributionlists";
			}
		};
		/**************** * intfield03 *  * *************/
		mapping[ContactObject.NUMBER_OF_LINKS] = new mapper() {
			public String getDBFieldName() {
				return "intfield03";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull() && t > 0){
					co.setNumberOfLinks(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNumberOfLinks();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getNumberOfLinks());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (original.getNumberOfLinks() == co.getNumberOfLinks());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getNumberOfLinks()+"";
			}
			public String getReadableTitle() {
				return "Number of links";
			}
		};
		/**************** * intfield02 Part 2 *  * *************/
		mapping[ContactObject.DISTRIBUTIONLIST] = new mapper() {
			public String getDBFieldName() {
				return "intfield02";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				try{
					int t = rs.getInt(pos);
					if (!rs.wasNull() && t > 0){
						co.setDistributionList(fillDistributionListArray(co.getObjectID(),user,group,ctx,uc,readcon));
					}
				}catch (Exception e){
					LOG.error("Unable to load Distributionlist",e);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsDistributionLists();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				// nix
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				// nix
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * intfield03 Part 2 *  * *************/
		mapping[ContactObject.LINKS] = new mapper() {
			public String getDBFieldName() {
				return "intfield03";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				try{
					int t = rs.getInt(pos);					
					if (!rs.wasNull() && t > 0){
						co.setLinks(fillLinkArray(co, user, group, ctx, uc, readcon));
					}
				}catch (Exception e){
					LOG.error("Unable to load Links",e);
				}
			}

			public boolean containsElement(ContactObject co) {
				return co.containsLinks();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				// nix
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				// nix
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * fid *  * *************/
		mapping[ContactObject.FOLDER_ID] = new mapper() {
			public String getDBFieldName() {
				return "fid";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					co.setParentFolderID(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsParentFolderID();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getParentFolderID());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getParentFolderID()+"";
			}
			public String getReadableTitle() {
				return "Folder id";
			}
		};
		/**************** * cid *  * *************/
		mapping[ContactObject.CONTEXTID] = new mapper() {
			public String getDBFieldName() {
				return "cid";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					co.setContextId(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsContextId();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getContextId());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (original.getContextId() == co.getContextId());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getContextId()+"";
			}
			public String getReadableTitle() {
				return "Context id";
			}
		};
		/**************** * pflag *  * *************/
		mapping[ContactObject.PRIVATE_FLAG] = new mapper() {
			public String getDBFieldName() {
				return "pflag";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					if (t == 1){
						co.setPrivateFlag(true);
					}else{
						co.setPrivateFlag(false);
					}
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsPrivateFlag();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.getPrivateFlag()){
					ps.setInt(pos, 1);
				}else{
					ps.setNull(pos,java.sql.Types.INTEGER);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (co.getPrivateFlag() == original.getPrivateFlag());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (ob == null){
					ps.setNull(position,java.sql.Types.INTEGER);
				} else {
					ps.setInt(position,Integer.parseInt((String)ob));						
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * created_from *  * *************/
		mapping[ContactObject.CREATED_BY] = new mapper() {
			public String getDBFieldName() {
				return "created_from";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					co.setCreatedBy(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCreatedBy();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getCreatedBy());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getCreatedBy()+"";
			}
			public String getReadableTitle() {
				return "Created by";
			}
		};
		/**************** * changed_from *  * *************/
		mapping[ContactObject.MODIFIED_BY] = new mapper() {
			public String getDBFieldName() {
				return "changed_from";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int t = rs.getInt(pos);
				if (!rs.wasNull()){
					co.setModifiedBy(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsModifiedBy();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setInt(pos, co.getModifiedBy());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (co.getModifiedBy() == original.getModifiedBy());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setInt(position,Integer.parseInt((String)ob));
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getModifiedBy()+"";
			}
			public String getReadableTitle() {
				return "Modified by";
			}
		};
		/**************** * creating_date *  * *************/
		mapping[ContactObject.CREATION_DATE] = new mapper() {
			public String getDBFieldName() {
				return "creating_date";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				long dong = rs.getLong(pos);				
				if (!rs.wasNull()){
					java.util.Date d = new java.util.Date(dong);
					co.setCreationDate(d);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsCreationDate();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				java.util.Date d = co.getCreationDate();
				ps.setLong(pos, d.getTime());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				java.util.Date d = (Date)ob;
				ps.setLong(position, d.getTime());
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				long x = rs.getLong(pos);
				Date d = null;
				if (!rs.wasNull()){
					d = new Date(x);
				}
				return d;
			}
			public String getValueAsString(ContactObject co) {
				return co.getCreationDate().toString();
			}
			public String getReadableTitle() {
				return "Creation date";
			}
		};
		/**************** * changing_date *  * *************/
		mapping[ContactObject.LAST_MODIFIED] = new mapper() {
			public String getDBFieldName() {
				return "changing_date";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				long dong = rs.getLong(pos);	
				if (!rs.wasNull()){
					java.util.Date d = new java.util.Date(dong);
					co.setLastModified(d);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsLastModified();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				java.util.Date d = co.getLastModified();
				ps.setLong(pos, d.getTime());
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				java.util.Date d = (Date)ob;
				ps.setLong(position, d.getTime());	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				long x = rs.getLong(pos);
				Date d  = null;
				if (!rs.wasNull()){
					d = new Date(x);
				}
				return d;
			}
			public String getValueAsString(ContactObject co) {
				return co.getLastModified().toString();
			}
			public String getReadableTitle() {
				return "Changing date";
			}
		};		
		/**************** * timestampfield01 *  * *************/
		mapping[ContactObject.BIRTHDAY] = new mapper() {
			public String getDBFieldName() {
				return "timestampfield01";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				Timestamp t = rs.getTimestamp(pos);
				if (!rs.wasNull()){
					co.setBirthday(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsBirthday();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.getBirthday() != null){
					ps.setTimestamp(pos, new java.sql.Timestamp(co.getBirthday().getTime()));
				}else{
					ps.setTimestamp(pos, null);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				java.util.Date x = co.getBirthday();
				java.util.Date y = original.getBirthday(); 
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.getTime() == y.getTime());
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				java.util.Date d = (Date)ob;
				if (d != null){
					ps.setTimestamp(position, new java.sql.Timestamp(d.getTime()));
				}else{
					ps.setTimestamp(position, null);
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				Timestamp x = rs.getTimestamp(pos);
				java.util.Date d = null;
				if (!rs.wasNull()){
					d = new java.util.Date(x.getTime());
				}
				return d;
			}
			public String getValueAsString(ContactObject co) {
				return co.getBirthday().toString();
			}
			public String getReadableTitle() {
				return "Birthday";
			}
		};		
		/**************** * timestampfield02 *  * *************/
		mapping[ContactObject.ANNIVERSARY] = new mapper() {
			public String getDBFieldName() {
				return "timestampfield02";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				Timestamp t = rs.getTimestamp(pos);
				if (!rs.wasNull()){
					co.setAnniversary(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsAnniversary();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.getAnniversary() != null){
					ps.setTimestamp(pos, new java.sql.Timestamp(co.getAnniversary().getTime()));
				}else{
					ps.setTimestamp(pos, null);	
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				java.util.Date x = co.getAnniversary();
				java.util.Date y = original.getAnniversary(); 
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.getTime() == y.getTime());
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				java.util.Date d = (Date)ob;
				if (d != null){
					ps.setTimestamp(position, new java.sql.Timestamp(d.getTime()));
				}else{
					ps.setTimestamp(position, null);
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				Timestamp x = rs.getTimestamp(pos);
				java.util.Date d = null;
				if (!rs.wasNull()){
					d = new java.util.Date(x.getTime());
				}
				return d;
			}
			public String getValueAsString(ContactObject co) {
				return co.getAnniversary().toString();
			}
			public String getReadableTitle() {
				return "Anniversay";
			}
		};			

		/**************** * image01 *  * *************/
		mapping[ContactObject.IMAGE1] = new mapper() {
			public String getDBFieldName() {
				return "intfield04";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				try{
					int t = rs.getInt(pos);
					if (!rs.wasNull() && t > 0){
						getContactImage(co.getObjectID(),co, ctx.getContextId(), readcon);
					}
				}catch (Exception e){ 
					LOG.error("Image not found", e);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsImage1();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsImage1()){
					ps.setInt(pos,1);
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){		
				
				if (co.getImage1() != null && original.getImage1() != null){
					String x = new String(co.getImage1());
					String y = new String(original.getImage1());
					
					return (x.equals(y));
				} else if ( (co.getImage1() == null && original.getImage1() != null) || (co.getImage1() != null && original.getImage1() == null) ){
					return false;
				} else {
					return true;
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0")){
					ps.setInt(position,0);
				}else{
					ps.setInt(position,1);
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};		
		/**************** * intfield04 *  * *************/
		mapping[ContactObject.IMAGE_LAST_MODIFIED] = new mapper() {
			public String getDBFieldName() {
				return "intfield04";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				try{
					int t = rs.getInt(pos);
					if (!rs.wasNull() && t > 0){
						Date dd = getContactImageLastModified(co.getObjectID(), ctx.getContextId(), readcon);
						if (dd != null){
							co.setImageLastModified(dd);
						}
					}
				}catch (Exception e){
					LOG.error("Unable to load Image",e);
				}	
			}
			public boolean containsElement(ContactObject co) {
				return co.containsImageLastModified();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsImage1()){
					ps.setInt(pos,1);
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){		
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0")){
					ps.setInt(position,0);
				}else{
					ps.setInt(position,1);
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * intfield04 *  * *************/
		mapping[ContactObject.IMAGE1_CONTENT_TYPE] = new mapper() {
			public String getDBFieldName() {
				return "intfield04";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				//
			}
			public boolean containsElement(ContactObject co) {
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				// false
			}
			public boolean compare(ContactObject co, ContactObject original){		
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				// nix	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * userid *  * *************/
		mapping[ContactObject.INTERNAL_USERID] = new mapper() {
			public String getDBFieldName() {
				return "userid";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int i = rs.getInt(pos);
				if (!rs.wasNull() && i > 0){
					co.setInternalUserId(i);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsInternalUserId();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsInternalUserId()){
					ps.setInt(pos,co.getInternalUserId());
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				return false;
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};	
		/**************** * intfield05 *  * *************/
		mapping[ContactObject.COLOR_LABEL] = new mapper() {
			public String getDBFieldName() {
				return "intfield05";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int i = rs.getInt(pos);
				if (!rs.wasNull() && i > 0){
					co.setLabel(i);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsLabel();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsLabel()){
					ps.setInt(pos,co.getLabel());
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (co.getLabel() == original.getLabel());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0") || ((String)ob).equals("null")) {
					ps.setInt(position, 0);
				}else{
					ps.setInt(position,Integer.parseInt((String)ob));
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};	
		/**************** * field90 *  * *************/
		mapping[ContactObject.FILE_AS] = new mapper() {
			public String getDBFieldName() {
				return "field90";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				String t = rs.getString(pos);
				if (!rs.wasNull()){
					co.setFileAs(t);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsFileAs();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				ps.setString(pos, co.getFileAs());
			}
			public boolean compare(ContactObject co, ContactObject original){
				String x = co.getFileAs();
				String y = original.getFileAs();
				
				if (x == null && y == null){
					return true;
				} else if ( (x == null && y != null) || (x != null && y == null)){
					return false;
				}else{
					return (x.equals(y));
				}	
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				ps.setString(position,(String)ob);	
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return rs.getString(pos);
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};	
		/**************** * intfield06 *  * *************/
		mapping[ContactObject.DEFAULT_ADDRESS] = new mapper() {
			public String getDBFieldName() {
				return "intfield06";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int i = rs.getInt(pos);
				if (!rs.wasNull() && i > 0){
					co.setDefaultAddress(i);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsDefaultAddress();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsDefaultAddress()){
					ps.setInt(pos,co.getDefaultAddress());
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (co.getDefaultAddress() == original.getDefaultAddress());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0")){
					ps.setInt(position,0);
				}else{
					ps.setInt(position, Integer.parseInt((String)ob));
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return co.getDefaultAddress()+"";
			}
			public String getReadableTitle() {
				return "Default address";
			}
		};	
		/**************** * intfield07 *  * *************/
		mapping[ContactObject.MARK_AS_DISTRIBUTIONLIST] = new mapper() {
			public String getDBFieldName() {
				return "intfield07";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int i = rs.getInt(pos);
				if (!rs.wasNull() && i > 0){
					co.markAsDistributionlist();
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsMarkAsDistributionlist();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsMarkAsDistributionlist()){
					if (co.getMarkAsDistribtuionlist()){
						ps.setInt(pos,1);
					}else{
						ps.setInt(pos,0);
					}
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				if (co.getMarkAsDistribtuionlist() == true && original.getMarkAsDistribtuionlist() == false){
					return false;
				}else if (co.getMarkAsDistribtuionlist() == false && original.getMarkAsDistribtuionlist() == true){
					return true;
				}else if (co.getMarkAsDistribtuionlist() == original.getMarkAsDistribtuionlist()){
					return true;
				} else {
					return false;
				}
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0")){
					ps.setInt(position,0);
				}else{
					ps.setInt(position,1);
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};
		/**************** * intfield08 *  * *************/
		mapping[ContactObject.NUMBER_OF_ATTACHMENTS] = new mapper() {
			public String getDBFieldName() {
				return "intfield08";
			}
			public void addToContactObject(ResultSet rs, int pos, ContactObject co, Connection readcon, int user, int[] group, Context ctx, UserConfiguration uc) throws SQLException {
				int i = rs.getInt(pos);
				if (!rs.wasNull() && i > 0){
					co.setNumberOfAttachments(i);
				}
			}
			public boolean containsElement(ContactObject co) {
				return co.containsNumberOfAttachments();
			}
			public void fillPreparedStatement(PreparedStatement ps, int pos, ContactObject co) throws SQLException{
				if (co.containsNumberOfAttachments()){
					ps.setInt(pos,co.getNumberOfAttachments());
				}else{
					ps.setInt(pos,0);
				}
			}
			public boolean compare(ContactObject co, ContactObject original){
				return (co.getNumberOfAttachments() == original.getNumberOfAttachments());
			}
			public void fillPreparedStatement(PreparedStatement ps, int position, Object ob) throws SQLException {
				if (((String)ob).equals("0")){
					ps.setInt(position,0);
				}else{
					ps.setInt(position,  Integer.parseInt((String)ob));
				}
			}
			public Object getData(ResultSet rs,int pos) throws SQLException {
				return String.valueOf(rs.getInt(pos));
			}
			public String getValueAsString(ContactObject co) {
				return null;
			}
			public String getReadableTitle() {
				return null;
			}
		};	
	}

}

