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

package com.openexchange.admin.diff.file.domain;

import org.apache.commons.io.FilenameUtils;

/**
 * Domain object that reflects a file marked as configuration file
 *
 * @author <a href="mailto:martin.schneider@open-xchange.com">Martin Schneider</a>
 * @since 7.6.1
 */
public class ConfigurationFile {

    private String name;

    private String extension;

    private final String rootDirectory;

    private final String pathBelowRootDirectory;

    private final String content;

    private final boolean isOriginal;

    /**
     * Initializes a new {@link ConfigurationFile}.
     *
     * @param name - the name of the file (includes possible file extensions)
     * @param rootDirectory - root directory of the files
     * @param pathBelowRootDirectory - location of the file below the root directory
     * @param content - content of the file
     * @param isOriginal - marker if the file is an original configuration file (true) or from the installation (false)
     */
    public ConfigurationFile(final String name, final String rootDirectory, final String pathBelowRootDirectory, final String content, final boolean isOriginal) {
        this.name = name;
        this.extension = FilenameUtils.getExtension(name);
        this.rootDirectory = rootDirectory;
        this.pathBelowRootDirectory = pathBelowRootDirectory;
        this.content = content;
        this.isOriginal = isOriginal;
    }

    /**
     * Gets the name. This contains also the file name extension
     *
     * @return The name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the extension
     *
     * @return The extension
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Gets the content
     *
     * @return The content
     */
    public String getContent() {
        return content;
    }

    /**
     * Gets the isOriginal
     *
     * @return The isOriginal
     */
    public boolean isOriginal() {
        return isOriginal;
    }

    /**
     * Gets the rootDirectory
     *
     * @return The rootDirectory
     */
    protected String getRootDirectory() {
        return rootDirectory;
    }

    /**
     * Gets the pathBelowRootDirectory
     *
     * @return The pathBelowRootDirectory
     */
    public String getPathBelowRootDirectory() {
        return pathBelowRootDirectory;
    }

    /**
     * Returns the full file name (incl. extension) but no path
     *
     * @return String - full file name (incl. extension) but no path
     */
    public String getFileNameWithExtension() {
        if (this.extension.isEmpty()) {
            return this.getName() + "." + this.getExtension();
        }
        return this.getName();
    }

    /**
     * Returns the full file name (incl. extension) and the path the file is located in
     *
     * @return String - full file name (incl. extension) and the path the file is located in
     */
    public String getFullFilePathWithExtension() {
        return new StringBuilder().append(this.rootDirectory).append(this.pathBelowRootDirectory).append(this.name).toString().replaceAll("//", "/");
    }

    /**
     * Sets the name
     *
     * @param name The name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Sets the extension
     *
     * @param extension The extension to set
     */
    public void setExtension(String extension) {
        this.extension = extension;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return new StringBuilder().append(this.getFullFilePathWithExtension()).append("\n").toString();
    }
}
