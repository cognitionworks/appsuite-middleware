
# norootforbuild

Name:           open-xchange-contacts-ldap
BuildArch:	    noarch
#!BuildIgnore:  post-build-checks
BuildRequires:  ant open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange-server >= @OXVERSION@
%if 0%{?suse_version} && 0%{?sles_version} < 11
%if %{?suse_version} <= 1010
# SLES10
BuildRequires:  java-1_5_0-ibm >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-devel >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-alsa >= 1.5.0_sr9
BuildRequires:  update-alternatives
%endif
%if %{?suse_version} >= 1100
BuildRequires:  java-sdk-openjdk
%endif
%if %{?suse_version} > 1010 && %{?suse_version} < 1100
BuildRequires:  java-sdk-1.5.0-sun
%endif
%endif
%if 0%{?sles_version} >= 11
# SLES11 or higher
BuildRequires:  java-1_6_0-ibm-devel
%endif

%if 0%{?rhel_version}
# libgcj seems to be installed whether we want or not and libgcj needs cairo
BuildRequires:  java-sdk-1.5.0-sun cairo
%endif
%if 0%{?fedora_version}
%if %{?fedora_version} > 8
BuildRequires:  java-1.6.0-openjdk-devel saxon
%endif
%if %{?fedora_version} <= 8
BuildRequires:  java-devel-icedtea saxon
%endif
%endif
Version:	@OXVERSION@
%define		ox_release 3
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        This bundle provides a global LDAP address book
Requires:       open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange >= @OXVERSION@
%description
This bundle provides a global LDAP address book

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install


%post

if [ ${1:-0} -eq 2 ]; then
   . /opt/open-xchange/etc/oxfunctions.sh

   # prevent bash from expanding, see bug 13316
   GLOBIGNORE='*'

   # SoftwareChange_Request-325
   # -----------------------------------------------------------------------
   for i in $(find /opt/open-xchange/etc/groupware/contacts-ldap/ -maxdepth 1 -name "mapping*.properties"); do
      oval=$(ox_read_property com.openexchange.contacts.ldap.mapping.ads.creationdate $i)
      if [ -z "$oval" ]; then
	  ox_set_property com.openexchange.contacts.ldap.mapping.ads.creationdate "whenCreated" $i
      fi
   done

   # SoftwareChange_Request-145
   # -----------------------------------------------------------------------
   for i in $(find /opt/open-xchange/etc/groupware/contacts-ldap/ -name "[0-9]*" -type d); do
      if [ -d $i ]; then
	  for prop in $(find $i -name "*.properties"); do
	      ctx=$(basename $i)
	      psname=$(basename $prop .properties)
	      ostr="com.openexchange.contacts.ldap.context${ctx}.${psname}.refreshinterval"
	      if ! grep $ostr $prop > /dev/null; then
		  echo -e "\n${ostr}=10000" >> $prop
	      fi
	  done
      fi
   done

   for i in $(find /opt/open-xchange/etc/groupware/contacts-ldap -name "*.example"); do
        ox_update_permissions "$i" root:open-xchange 640
   done
fi

%clean
%{__rm} -rf %{buildroot}



%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/groupware/osgi/bundle.d/
/opt/open-xchange/etc/groupware/osgi/bundle.d/*
/opt/open-xchange/bundles/com.openexchange.contacts.ldap.jar
%dir %attr(750,root,open-xchange) /opt/open-xchange/etc/groupware/contacts-ldap
%attr(640,root,open-xchange) /opt/open-xchange/etc/groupware/contacts-ldap/*.example
%attr(640,root,open-xchange) /opt/open-xchange/etc/groupware/contacts-ldap/*/*.example

%changelog
* Wed Jul 14 2010 - dennis.sieben@open-xchange.com
 - Bugfix #16049 - [L3] Distribution list not available after update
   - Added fallback to creation date if modified date is not available
   - Added missing mapping for creation date in ADS
   - Added two more properties: pooltimeout and derefAliases
* Tue Apr 20 2010 - dennis.sieben@open-xchange.com
 - Bugfix #15899 - Global addressbook doesn't return results via EAS once contacts-ldap is installed
   - Add new check for null values
* Wed Nov 25 2009 - choeger@open-xchange.com
 - Bugfix #14479 -  Contacts LDAP bundles does not start (SLES11, IBM JAVA)
* Tue Nov 10 2009 - dennis.sieben@open-xchange.com
 - Fix for UNAVAIL_EXTENSION error
 - Reduced fetching of attributes for the distributionlist members to needed ones only
* Mon Nov 02 2009 - dennis.sieben@open-xchange.com
 - Added ability to disable PagedResults (e.g. for Fedora DS) by setting pagesize to 0
* Mon Jul 27 2009 - marcus.klein@open-xchange.com
 - Bugfix #14213: Setting configuration file permissions to reduce readability to OX processes.
* Mon Jul 13 2009 - dennis.sieben@open-xchange.com
  - Bugfix #14151 Contacts-ldap currently concatenates multi-value attributes
    this must be changed
    - Removed concatenation - now taking the first value
* Fri Jul 10 2009 - dennis.sieben@open-xchange.com
  - Bugfix #14148 contact list is not sorted by name in contacts-ldap
    - Distributionlist now have a sur_name
* Thu Jul 09 2009 - dennis.sieben@open-xchange.com
  - Bugfix #14137 contacts-ldap must provide an option to deal with referrals
    - Added new property value to set referrals behaviour
  - Bugfix #14138 Fix for groups without members on ADS with contacts-ldap
    - Added catch to ignore this exceptions
* Mon Jun 22 2009 - dennis.sieben@open-xchange.com
  - Bugfix #13920 Unable to get public LDAP folders to Outlook
    - Now returning a SearchIterator in getDeletedContactsInFolder
* Thu Jun 18 2009 - dennis.sieben@open-xchange.com
  - Bugfix #13926 gal bundle: java.lang.Exception: The given value for authtype
    "%s" is not a possible one
    - Changed text to "The directory "%s" is not a context identifier."
* Tue Jun 16 2009 - dennis.sieben@open-xchange.com
  - Bugfix #13892 contacts-ldap bundle contains documentation as odt format at
   the sources
   - Removed documentation as it is now contained in the Installation and
     Administrator documentation
* Tue Jun 16 2009 - dennis.sieben@open-xchange.com
  - Bugfix #13909 NPE when contacts-ldap is enabled to access distribution
    lists (ADS)
    - Surrounded code segment with if
* Mon May 11 2009 - dennis.sieben@open-xchange.com
  - Implemented distributionlist
* Thu Apr 23 2009 - dennis.sieben@open-xchange.com
  - Bugfix #13539 Search field in global LDAP contact folder does not work
