
# norootforbuild

Name:           open-xchange-admin-plugin-hosting
BuildArch:	noarch
BuildRequires:  ant open-xchange-admin
%if 0%{?suse_version}
%if %{?suse_version} <= 1010
# SLES10
BuildRequires:  java-1_5_0-ibm java-1_5_0-ibm-devel java-1_5_0-ibm-alsa update-alternatives
%endif
%if %{?suse_version} >= 1100
BuildRequires:  java-sdk-openjdk
%endif
%if %{?suse_version} > 1010 && %{?suse_version} < 1100
BuildRequires:  java-sdk-1.5.0-sun
%endif
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
Version:        6.5.0
Release:        4
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        Open Xchange Admin Hosting Plugin
Requires:       open-xchange-admin >= 6.6.0
Requires:       open-xchange-admin-client >= 6.6.0
Conflicts:	open-xchange-admin-plugin-context-light
#

%package -n     open-xchange-admin-plugin-hosting-doc
Group:          Applications/Productivity
Summary:        Documentation for the Open Xchange RMI client library.


%description -n open-xchange-admin-plugin-hosting-doc
Documentation for the Open Xchange RMI client library.

Authors:
--------
    Open-Xchange

%description
Open Xchange Admin Hosting Plugin

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
%define adminbundle	open_xchange_admin.jar
%define oxprefix	/opt/open-xchange

ant -Dadmin.classpath=%{oxprefix}/bundles/%{adminbundle} \
    -Ddestdir=%{buildroot} -Dprefix=%{oxprefix} doc install install-client
mv doc javadoc


%clean
%{__rm} -rf %{buildroot}



%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles
%dir /opt/open-xchange/etc/admindaemon/osgi/bundle.d
%dir /opt/open-xchange/sbin
%dir /opt/open-xchange/lib
%dir /opt/open-xchange/etc/admindaemon/plugin
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/admindaemon/osgi/bundle.d/*
/opt/open-xchange/sbin/*
/opt/open-xchange/lib/*
%config(noreplace) /opt/open-xchange/etc/admindaemon/plugin/*

%files -n open-xchange-admin-plugin-hosting-doc
%defattr(-,root,root)
%doc javadoc
%changelog
* Tue Jul 29 2008 - marcus.klein@open-xchange.com
 - Bugfix #11681: Removed a lot of .toString() in debug messages to prevent
   NullPointerExceptions.
* Mon Jul 28 2008 - holgi@open-xchange.com
  - Bugfix ID#11715 showruntimestats -d not usable for com.openexchange.caching
* Thu Jul 17 2008 - choeger@open-xchange.com
  - Bugfix ID#11572 CLT jmx tools do not work any more when jmx auth is enabled
* Mon Jul 07 2008 - choeger@open-xchange.com
  - Bugfix ID#11500 JMX error when starting admindaemon
    do not set contextclassloader
* Mon Jul 07 2008 - holger.achtziger@open-xchange.com
  - Bugfix ID#11575 OX installer fails if configjump.properties does not exist
* Thu Jul 03 2008 - manuel.kraft@open-xchange.com
  - Bugfix ID#11539 oxreport does not run any more: "java.lang.NoClassDefFoundError: com/openexchange/admin/console/ReportingTool"
* Wed Jul 02 2008 - manuel.kraft@open-xchange.com
  - Bugfix ID#11539 oxreport does not run any more: "java.lang.NoClassDefFoundError: com/openexchange/admin/console/ReportingTool"
* Fri Jun 27 2008 - holgi@open-xchange.com
  - Bugfix ID#11533 added separate cache ports for admin and groupware
* Tue Jun 24 2008 - manuel.kraft@open-xchange.com
  - Bugfix ID#11490 unable to create context using --access-combination-name on commandline
* Mon Jun 09 2008 - dennis.sieben@open-xchange.com
  - Bugfix ID#11358 [L3] Movecontextfilestore doesn't move filestore if context filestore isn't available
* Tue Apr 29 2008 - choeger@open-xchange.com
  - Bugfix ID#11194 getaccesscombinationnameforuser throws NoClassDefFoundError
* Wed Mar 05 2008 - choeger@open-xchange.com
  - Bugfix ID#10414 oxinstaller sets read db connection to the wrong server for master/slave setups
* Tue Feb 12 2008 - dennis.sieben@open-xchange.com
  - Bugfix ID#10894 AJP and general monitoring does not work anymore with showRuntimeStats
* Tue Dec 11 2007 - choeger@open-xchange.com
  - Bugfix ID#10223
    [L3] file plugin/hosting.properties gets overwritten on package update
* Mon Dec 10 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#10592 [HEAD] Filestorage leftovers for deleted contexts
  - Bugfix ID#10603 [HEAD] double push back on db connection causes warning log
* Fri Dec 07 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#10577 [HEAD ]admin does breake Database replication
* Mon Oct 29 2007 - choeger@open-xchange.com
  - Bugfix ID#9986 Admin should update schema automatically
* Mon Oct 29 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#9974 [HEAD] context deletion not to use any server api calls
* Thu Oct 25 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#9949 L3: Filestore directory layout not physically deleted when context
    is removed, only contained files
  - Bugfix ID#9948 No rollback when deleting a context
* Wed Sep 26 2007 - choeger@open-xchange.com
  - Bugfix ID#9614 initconfigdb "mysqladmin: connect to server at 'localhost'
  failed" when database not local
* Tue Sep 25 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#9569 showruntimestats shows "statistictools" as default usage
* Mon Sep 10 2007 - choeger@open-xchange.com
- Bugfix ID#8949 Unable to deinstall admin-plugin-hosting package when removing depending
  package (the fix from 2007-08-20 does not really work)
* Wed Sep 05 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#9254 showruntimestats gives NullPointerException
* Wed Aug 22 2007 - choeger@open-xchange.com
  - Bugfix ID#8989 generatempasswd no newline in output
  - Bugfix ID#8991 initconfigdb, return code is always 0
  - Bugfix ID#8853 'listcontexts' searchpattern only works for context id
  - Bugfix ID#9026 listcontextsbyfilestore does not print a error when the given fs does not exist
* Wed Aug 22 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#9023 createcontext name should be added to lmappings
* Tue Aug 21 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8543 rename of movedatabasecontext and movefilestorecontext
  - Bugfix ID#9004 unregisterserver operation by name missing
  - Bugfix ID#8993 clts should all be singular
  - Bugfix ID#9007 Context login mappings not validated
  - Bugfix ID#8994 oxinstaller --master-pass should not be needed if --disableauth is in use
* Mon Aug 20 2007 - choeger@open-xchange.com
  - Bugfix ID#8949 Unable to deinstall admin-plugin-hosting package when removing depending
  package
* Thu Aug 16 2007 - choeger@open-xchange.com
  - Bugfix ID#8915 Classpath problems with CLTs
* Thu Aug 16 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8917 CLT 'showruntimestats' does not give any output
* Tue Aug 14 2007 - choeger@open-xchange.com
  - Bugfix ID#8822 'generatepassword' creates curious output
* Thu Aug 09 2007 - choeger@open-xchange.com
  - Bugfix ID#8623 oxinstaller: switch needed to turn on/of context authentication
* Tue Aug 07 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8629 movefilestorecontext StringIndexOutOfBoundsException
  - Bugfix ID#8593 Operations by name not possible
* Mon Aug 06 2007 - choeger@open-xchange.com
  - Bugfix ID#8642 "listusers" command line tool output is limited to three digits
    as a side effect of now dynamically determining the widest row, this is also fixed
* Thu Aug 02 2007 - choeger@open-xchange.com
  - Bugfix ID#8651 no access must be the default when not specifying access on commandline
    using lowest set of access options as default (webmail)
* Tue Jul 31 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8597 [DEV] searchContextByFilestoreId,searchContextByFilestore and
    searchContextByDatabase must only return contexts bound to specific SERVER_NAME
* Mon Jul 30 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8575 CLT createcontext: It should be possible to create a mapping between contextID
  and context name during createcontext and not only on changecontext
* Mon Jul 30 2007 - choeger@open-xchange.com
  - Bugfix ID#8592 Misleading server response if "listuser" doesn't find any match
* Thu Jul 26 2007 - dennis.sieben@open-xchange.com
  - Bugfix ID#8553 CLT: After running CLT no reasonable message on console appear for user
* Wed Jul 25 2007 - choeger@open-xchange.com
  - Bugfix ID#8550 generatempasswd not developed to be used by humans
  made generatempasswd usable by humans... :-)
* Wed Jul 11 2007 - manuel.kraft@open-xchange.com
  -  Bugfix ID#8379 listcontext, lmapping not in csv output
* Mon Jul 09 2007 - manuel.kraft@open-xchange.com
  -  Bugfix ID#7302 changecontext is missing
* Fri Jun 29 2007 - dennis.sieben@open-xchange.com
  -  Bugfix ID#8171 need for a tool that does reset the jmx max values
* Thu Jun 21 2007 - manuel.kraft@open-xchange.com
  -  Bugfix ID#7675 LTs to manage users can not deal with modules
* Tue Jun 12 2007 - dennis.sieben@open-xchange.com
  -  Bugfix ID#7657 console clients check extension errors in the wrong place
* Tue May 29 2007 - choeger@open-xchange.com
  - Bugfix ID#7595 Groups member in several contexts are deleted on context delete
