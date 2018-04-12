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

package com.openexchange.documentation.tools;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;
import com.openxchange.documentation.tools.internal.ConfigDocu;
import com.openxchange.documentation.tools.internal.CurrentConfig;
import com.openxchange.documentation.tools.internal.CurrentConfig.PropertyWithLocation;
import com.openxchange.documentation.tools.internal.Property;

/**
 * {@link ShowConfig} is a CLT which provides access to the config documentation.
 *
 * @author <a href="mailto:kevin.ruthmann@open-xchange.com">Kevin Ruthmann</a>
 * @since v7.10.0
 */
@SuppressWarnings("static-access")
public class ShowConfig {

    private static final Options options = new Options();
    private static final String DEFAULT_FOLDER = "/opt/open-xchange/documentation/etc";
    private static final String CURRENT_CONFIG_FOLDER = "/opt/open-xchange/etc";

    private static final String SEARCH_OPTION = "s";
    private static final String TAG_OPTION = "t";
    private static final String KEY_OPTION = "k";
    private static final String HELP_OPTION = "h";
    private static final String ANSI_OPTION = "n";
    private static final String ONLY_KEY_OPTION = "o";
    private static final String PRINT_TAG_OPTION = "p";
    private static final String INCLUDE_CONFIGURED_VALUE_OPTION = "i";
    private static final String DIFFERENCES_ONLY_OPTION = "d";

    private static final Comparator<String> IGNORE_CASE_COMP = new Comparator<String>() {
        @Override
        public int compare(String s1, String s2) {
            return s1.toLowerCase().compareTo(s2.toLowerCase());
        }
    };

    static {
        options.addOption(OptionBuilder.withLongOpt("help").hasArg(false).withDescription("Prints this usage.").isRequired(false).create(HELP_OPTION));
        options.addOption(OptionBuilder.withLongOpt("tag").hasArgs(1).withDescription("If set only properties with the given tag are returned.").isRequired(false).create(TAG_OPTION));
        options.addOption(OptionBuilder.withLongOpt("search").hasArgs(1).withDescription("If set only properties which match the given search term are returned.").isRequired(false).create(SEARCH_OPTION));
        options.addOption(OptionBuilder.withLongOpt("key").hasArgs(1).withDescription("If set only properties with a key which contains the given term are returned.").isRequired(false).create(KEY_OPTION));
        options.addOption(OptionBuilder.withLongOpt("only-key").hasArg(false).withDescription("Prints only the key for each property.").isRequired(false).create(ONLY_KEY_OPTION));
        options.addOption(OptionBuilder.withLongOpt("print-tags").hasArg(false).withDescription("Prints a list of available tags.").isRequired(false).create(PRINT_TAG_OPTION));
        options.addOption(OptionBuilder.withLongOpt("no-color").hasArg(false).withDescription("Removes ansi color formatting.").isRequired(false).create(ANSI_OPTION));
        options.addOption(OptionBuilder.withLongOpt("include_configured").hasArg(false).withDescription("Also prints the current configured value if present.").isRequired(false).create(INCLUDE_CONFIGURED_VALUE_OPTION));
        options.addOption(OptionBuilder.withLongOpt("only_differences").hasArg(false).withDescription("Only prints properties which are configured and have a different value than the default.").isRequired(false).create(DIFFERENCES_ONLY_OPTION));
    }

    public static void main(String[] args) {

        CommandLineParser parser = new PosixParser();
        try {
            CommandLine parse = parser.parse(options, args);
            if(parse.hasOption(HELP_OPTION)) {
                printUsage(1);
            }

            File yamlFolder = new File(DEFAULT_FOLDER);
            if (!yamlFolder.exists() || !yamlFolder.isDirectory()) {
                System.out.println("Unable to find the config documentation. The folder containing the docu doesn't exists ('"+DEFAULT_FOLDER+"').");
                System.exit(2);
            }

            try {
                ConfigDocu configDocu = new ConfigDocu(yamlFolder);

                if(parse.hasOption(PRINT_TAG_OPTION)) {
                    printTags(configDocu.getTags());
                    System.exit(0);
                }

                boolean onlyKey = parse.hasOption(ONLY_KEY_OPTION);

                List<Property> props = null;
                if(parse.hasOption(TAG_OPTION)) {
                    props = configDocu.getProperties(parse.getOptionValue(TAG_OPTION));
                } else {
                    props = configDocu.getProperties();
                }

                if(parse.hasOption(KEY_OPTION)) {
                    String term = parse.getOptionValue(KEY_OPTION);
                    props = props.stream()
                        .filter(prop -> prop.getKey().contains(term))
                        .collect(Collectors.toList());
                }
                if(parse.hasOption(SEARCH_OPTION)) {
                    String term = parse.getOptionValue(SEARCH_OPTION);
                    props = props.stream()
                        .filter(prop -> prop.contains(term))
                        .collect(Collectors.toList());
                }

                boolean useAnsi = true;
                if(parse.hasOption(ANSI_OPTION)) {
                    useAnsi = Boolean.valueOf(parse.getOptionValue(ANSI_OPTION));
                }

                CurrentConfig currentConfig = null;
                boolean diffOnly = parse.hasOption(DIFFERENCES_ONLY_OPTION);
                if (parse.hasOption(INCLUDE_CONFIGURED_VALUE_OPTION) || diffOnly) {
                    try {
                        currentConfig = new CurrentConfig(CURRENT_CONFIG_FOLDER);
                    } catch (IOException e) {
                        System.err.println("Unable to read current configuration.");
                        if (diffOnly) {
                            handleError(e);
                        }
                    }
                }

                printProperties(props, currentConfig, useAnsi, onlyKey, diffOnly);
            } catch (FileNotFoundException e) {
                handleError(e);
            }
        } catch (ParseException e1) {
            handleError(e1);
        }
    }

    /**
     * Prints the given tags as a list
     *
     * @param tags The tags
     */
    private static void printTags(Set<String> tags) {
        List<String> sortedTags = new ArrayList<>(tags);
        Collections.sort(sortedTags, IGNORE_CASE_COMP);
        System.out.println("Available tags:\n");
        for(String tag : sortedTags) {
            System.out.println(tag);
        }

    }

    private static void printProperties(List<Property> props, CurrentConfig config, boolean useAnsi, boolean onlyKey, boolean onlyDiff) {
        if (useAnsi) {
            System.out.println(ANSI_BOLD + "Properties:" + ANSI_RESET);
        } else {
            System.out.println("Properties:");
        }
        for(Property prop: props) {
            if (config != null) {
                List<PropertyWithLocation> list = config.getValue(prop.getKey());
                PropertyWithLocation matchedProp = findMatch(prop.getFile(), list);
                if (!onlyDiff || (matchedProp != null && matchedProp.getValue().equals(prop.getDefaultValue()))) {
                    String value = null;
                    if (matchedProp != null) {
                        value = matchedProp.getValue();
                    }
                    printProperty(prop, value, useAnsi, onlyKey);
                    if (!onlyKey) {
                        printWarningForDuplicateEntries(props, prop, list, useAnsi);
                        System.out.println("\n" + format("-------------------------------------------------", ANSI_RED, useAnsi) + "\n");
                    }
                }
            } else {
                printProperty(prop, null, useAnsi, onlyKey);
                if (!onlyKey) {
                    System.out.println("\n" + format("-------------------------------------------------", ANSI_RED, useAnsi) + "\n");
                }
            }
        }
    }

    /**
     * Prints a warning message for duplicate configured properties.
     *
     * @param props All properties
     * @param prop The current property
     * @param list A list of possible duplicates
     * @param useAnsi Whether to use ansi coloring or not
     */
    private static void printWarningForDuplicateEntries(List<Property> props, Property prop, List<PropertyWithLocation> list, boolean useAnsi) {
        if (list != null && list.size() > 1) {
            for (Property tmpProp : props) {
                if (!prop.equals(tmpProp) && tmpProp.getKey().equals(prop.getKey())) {
                    Iterator<PropertyWithLocation> iter = list.iterator();
                    while (iter.hasNext()) {
                        PropertyWithLocation next = iter.next();
                        if (tmpProp.getFile().equalsIgnoreCase(next.getLocation().getName())) {
                            // belongs to a different property with the same name
                            iter.remove();
                            break;
                        }
                    }
                }
            }
            if (list.size() > 1) {
                printWarning(prop, list, useAnsi);
            }
        }
    }

    /**
     * Find the property which match the preferred location or the last one if none matches.
     *
     * @param file The preferred filename
     * @param list The list of properties
     * @return The {@link PropertyWithLocation} or null in case list is null
     */
    private static PropertyWithLocation findMatch(String file, List<PropertyWithLocation> list) {
        if (list == null) {
            return null;
        }
        for (PropertyWithLocation prop : list) {
            if (prop.getLocation().getName().equals(file)) {
                return prop;
            }
        }
        return list.get(list.size() - 1);
    }

    private static final String WARNING = "There are multiple values defined for the same property:\n";
    private static final String ADVICE = "  This could indicate a misconfiguration.\n";

    /**
     * @param prop
     * @param list
     * @param useAnsi
     */
    private static void printWarning(Property prop, List<PropertyWithLocation> list, boolean useAnsi) {
        StringBuilder builder = new StringBuilder();
        builder.append(WARNING);
        for (PropertyWithLocation propLoc : list) {
            builder.append("  - '").append(propLoc.getValue()).append("' in file '").append(propLoc.getLocation()).append("'\n");
        }
        builder.append("\n").append(ADVICE);
        printKeyValue("\nWarning", builder.toString(), useAnsi);
    }

    public static final String ANSI_RESET = "\u001B[0m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BOLD = "\u001B[1m";
    public static final String ANSI_FRAME = "\u001B[51m";

    private static void printProperty(Property prop, String currentValue, boolean useAnsi, boolean onlyKey) {
        printKeyValue("Key", prop.getKey(), useAnsi);
        if(onlyKey) {
            return;
        }
        System.out.println(format("Description", ANSI_RED, useAnsi)+":");
        System.out.println(formatDescription(prop.getDescription(), useAnsi));
        printKeyValue("Default", prop.getDefaultValue(), true, useAnsi);
        if (currentValue != null) {
            printKeyValue("Configured value", currentValue, true, useAnsi);
        }
        printKeyValue("File", prop.getFile(), useAnsi);
        printKeyValue("Version", prop.getVersion(), useAnsi);
        printKeyValue("Package", prop.getPackageName(), useAnsi);
        System.out.print(format("Tags", ANSI_RED, useAnsi)+": ");
        boolean first = true;
        for(String tag: prop.getTags()) {
            if(first) {
                System.out.print(tag);
                first = false;
            } else {
                System.out.print(format(" | ", ANSI_RED, useAnsi)+tag);
            }
        }
    }

    private static String formatDescription(String value, boolean useAnsi) {
        if(useAnsi) {
            value = value.replaceAll("<code>|\\[\\[", ANSI_FRAME+" ");
            value = value.replaceAll("</code>|\\]\\]", " "+ANSI_RESET);
            value = value.replaceAll("<b>", ANSI_BOLD);
            value = value.replaceAll("</b>", ANSI_RESET);
        } else {
            value = value.replaceAll("\\[\\[|\\]\\]", "");
        }
        value = value.replaceAll("<li>", "-");
        value = value.replaceAll("\\<[^>]*>", "");
        return "  " + value.replaceAll("\n", "\n  ");
    }

    private static String format(String value, String color, boolean useAnsi) {
        if(useAnsi) {
            return color+value+ANSI_RESET;
        }
        return value;
    }

    private static void printKeyValue(String key, String value, boolean useAnsi) {
        printKeyValue(key, value, false, useAnsi);
    }

    private static void printKeyValue(String key, String value, boolean printEmptyValue, boolean useAnsi) {
        if(value!=null) {
            System.out.println(format(key, ANSI_RED, useAnsi)+": "+value);
        } else if(printEmptyValue) {
            System.out.println(format(key, ANSI_RED, useAnsi)+": ");
        }
    }

    /**
     * Prints an error message with exit code 3
     *
     * @param e The error
     */
    private static final void handleError(Exception e){
        System.err.println("An error occured: " + e.getMessage());
        System.exit(3);
    }

    /**
     * Print usage
     *
     * @param exitCode
     */
    private static final void printUsage(int exitCode) {
        HelpFormatter hf = new HelpFormatter();
        hf.setWidth(120);
        hf.printHelp("showconfig", options);
        System.exit(exitCode);
    }
}
