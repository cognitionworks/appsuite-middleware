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
 *    trademarks of the OX Software GmbH group of companies.
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
 *     Copyright (C) 2016-2020 OX Software GmbH
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

package com.openexchange.groupware.infostore.utils;

import java.util.Date;
import com.openexchange.groupware.infostore.DocumentMetadata;

public class GetSwitch implements MetadataSwitcher {

	private final DocumentMetadata metadata;

	public GetSwitch(final DocumentMetadata metadata){
		this.metadata = metadata;
	}

	@Override
	public Object meta() {
	    return metadata.getMeta();
	}

	@Override
    public Object lastModified() {
		return metadata.getLastModified();
	}

	@Override
    public Object creationDate() {
		return metadata.getCreationDate();
	}

	@Override
    public Object modifiedBy() {
		return Integer.valueOf(metadata.getModifiedBy());
	}

	@Override
    public Object folderId() {
		return Long.valueOf(metadata.getFolderId());
	}

	@Override
    public Object title() {
		return metadata.getTitle();
	}

	@Override
    public Object version() {
		return Integer.valueOf(metadata.getVersion());
	}

	@Override
    public Object content() {
		return metadata.getContent();
	}

	@Override
    public Object id() {
		return Integer.valueOf(metadata.getId());
	}

	@Override
    public Object fileSize() {
		return Long.valueOf(metadata.getFileSize());
	}

	@Override
    public Object description() {
		return metadata.getDescription();
	}

	@Override
    public Object url() {
		return metadata.getURL();
	}

	@Override
    public Object createdBy() {
		return Integer.valueOf(metadata.getCreatedBy());
	}

	@Override
    public Object fileName() {
		return metadata.getFileName();
	}

	@Override
    public Object fileMIMEType() {
		return metadata.getFileMIMEType();
	}

	@Override
    public Object sequenceNumber() {
		return Long.valueOf(metadata.getSequenceNumber());
	}

	@Override
    public Object categories() {
		return metadata.getCategories();
	}

	@Override
    public Object lockedUntil() {
		return metadata.getLockedUntil();
	}

	@Override
    public Object fileMD5Sum() {
		return metadata.getFileMD5Sum();
	}

	@Override
    public Object versionComment() {
		return metadata.getVersionComment();
	}

	@Override
    public Object currentVersion() {
		return Boolean.valueOf(metadata.isCurrentVersion());
	}

	@Override
    public Object colorLabel() {
		return Integer.valueOf(metadata.getColorLabel());
	}

	@Override
    public Object filestoreLocation() {
		return metadata.getFilestoreLocation();
	}

    @Override
    public Object lastModifiedUTC() {
        return metadata.getLastModified();
    }

    @Override
    public Object numberOfVersions() {
        return Integer.valueOf(metadata.getNumberOfVersions());
    }

    @Override
    public Object objectPermissions() {
        return metadata.getObjectPermissions();
    }

    @Override
    public Object shareable() {
        return Boolean.valueOf(metadata.isShareable());
    }

    @Override
    public Object origin() {
        return metadata.getOriginFolderPath();
    }

    @Override
    public Object captureDate() {
        return metadata.getCaptureDate();
    }

    @Override
    public Object geolocation() {
        return metadata.getGeoLocation();
    }

    @Override
    public Object width() {
        return metadata.getWidth();
    }

    @Override
    public Object height() {
        return metadata.getHeight();
    }

    @Override
    public Object cameraMake() {
        return metadata.getCameraMake();
    }

    @Override
    public Object cameraModel() {
        return metadata.getCameraModel();
    }

    @Override
    public Object cameraIsoSpeed() {
        return metadata.getCameraIsoSpeed();
    }

    @Override
    public Object cameraAperture() {
        return metadata.getCameraAperture();
    }

    @Override
    public Object cameraExposureTime() {
        return metadata.getCameraExposureTime();
    }

    @Override
    public Object cameraFocalLength() {
        return metadata.getCameraFocalLength();
    }

    @Override
    public Object mediaMeta() {
        return metadata.getMediaMeta();
    }

    @Override
    public Object mediaStatus() {
        return metadata.getMediaStatus();
    }

    @Override
    public Object mediaDate() {
        Date d = metadata.getCaptureDate();
        return null == d ? metadata.getLastModified() : d;
    }

}
