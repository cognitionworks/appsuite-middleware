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
 *     Copyright (C) 2004-2012 Open-Xchange, Inc.
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

package com.openexchange.tools.images;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;


/**
 * {@link ImageTransformations}
 * 
 * Allows chaining of multiple transformations to an image.
 * 
 * @author <a href="mailto:tobias.friedrich@open-xchange.com">Tobias Friedrich</a>
 */
public interface ImageTransformations {

    /**
     * Adds a 'rotate' transformation, leading to the image being rotated according to the contained EXIF information.
     * 
     * @return A self reference
     */
    ImageTransformations rotate();
    
    /**
     * Adds a 'scale' transformation, leading to the image being scaled according to the supplied parameters.
     * 
     * @param maxWidth The maximum width of the target image
     * @param maxHeight The maximum height of the target image
     * @param scaleType The scale type to use
     * @return A self reference
     */
    ImageTransformations scale(int maxWidth, int maxHeight, ScaleType scaleType);
    
    /**
     * Adds a 'crop' transformation, leading to the image being cropped according to the supplied parameters.
     * 
     * @param x The X coordinate of the upper-left corner of the specified rectangular region
     * @param y The Y coordinate of the upper-left corner of the specified rectangular region
     * @param width The width of the specified rectangular region
     * @param height The height of the specified rectangular region 
     * @return A self reference
     */
    ImageTransformations crop(int x, int y, int width, int height);
    
    /**
     * Applies all transformations and returns the result as image.
     * 
     * @return The resulting image
     */
    BufferedImage getImage() throws IOException;
    
    /**
     * Applies all transformations and writes the result as raw image data in the given format.
     * 
     * @return The resulting image data
     */
    byte[] getBytes(String formatName) throws IOException;
    
    /**
     * Applies all transformations and provides an input stream on the resulting raw image data in the given format.
     * 
     * @param formatName The image format to use, e.g. <code>jpeg</code> or <code>png</code>
     * @return A new input stream carrying the resulting image data
     */
    InputStream getInputStream(String formatName) throws IOException;
    
}
