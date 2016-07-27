---title: Properties Overview---# IntroductionWith release 7.8.3 Open-Xchange starts to use predefined values for properties stored in the code and to prevent adding the properties to files contained in /opt/open-xchange/etc and its folders below. The administrator is (of course) able to change the provided defaults by adding the property to the file of his choice (below /opt/open-xchange/etc). Because of this change this page describes (mainly) new properties that aren't visible  in a file but can be configured by the administrator.All properties will be described by using the layout of the table below:| Key 		  	| `com.openexchange.foo` 	| | :---        	|     :---      			|| Description 	| Defines the foo 			|| Default 		| true						|| Version 		| 7.8.3						|| Related 		| `com.openexchange.bar` 	|| File 			| 							|---| Key 		  	| `com.openexchange.bar` 	| | :---        	|     :---      			|| Description 	| Defines the bar 			|| Default 		| false						|| Version 		| 7.8.0						|| Related 		| `com.openexchange.foo` 	|| File 			| foobar.properties			|These information are contained within the columns have the following meaning:  * **Key**: The key of the property. This key has to be added to the file of the administrators choice to overwrite the default value.  * **Description**: A short description of the property.  * **Default**: The default value of the property (as defined within the code).  * **Version**: The first version the property is available with.  * **Related**: Contains information about other properties that are related to the currently described one.  * **File**: Describes the file where the property is defined. This column mainly exists for properties that have been available before 7.8.3 and are contained within a file.## New propertiesTo insert a new property you just have to create or update the corresponding yml file in /documentation/config folder.The yml file must have the following structure:array:  - data:      Key: c.o.some.property      Description: >        line1        line2        line3      Default: true      Version: 7.8.3      Related:       File:  - data:      Key: c.o.some.property2      Description: >        line1        line2      Default: true      Version: 7.8.0      Related: c.o.some.property      File: somefile.propertiesIf you would like to add a reference to another property use the following approach:  * tag the destination property key by using `<a name="com.openexchange.foo">com.openexchange.foo</a>`  * reference the tagged property by adding it to the 'related' column like `<a href="#com.openexchange.foo">com.openexchange.foo</a>`# Properties

## template 

| Key | <span style="font-weight:normal">c.o.some.property</span> |
|:----------------|:--------|
| __Description__ |         line1<br>        line2<br>        line3<br> |
| __Default__ | true  |
| __Version__ | 7.8.3  |

---
| Key | <span style="font-weight:normal">c.o.some.property2</span> |
|:----------------|:--------|
| __Description__ |         line1<br>        line2<br> |
| __Default__ | true  |
| __Version__ | 7.8.0  |
| __Related__ | c.o.some.property  |
| __File__ | somefile.properties  |

---
