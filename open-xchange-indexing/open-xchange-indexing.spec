
Name:          open-xchange-indexing
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: open-xchange-admin
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 17
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0 
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange backend indexing extension
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-admin >= @OXVERSION@

%description
This package contains the extensions for the backend installations implementing the indexing feature.

Authors:
--------
    Open-Xchange

%prep

%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml build

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/etc/
%config(noreplace) /opt/open-xchange/etc/solr.properties
%config(noreplace) /opt/open-xchange/etc/indexing-service.properties
%config(noreplace) /opt/open-xchange/etc/smal.properties
%dir /opt/open-xchange/lib/
/opt/open-xchange/lib/*
%dir /opt/open-xchange/sbin/
/opt/open-xchange/sbin/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/solr/
/opt/open-xchange/solr/*

%changelog
* Tue Jan 28 2014 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2014-01-30
* Mon Jan 20 2014 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2014-01-20
* Thu Jan 16 2014 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2014-01-16
* Mon Jan 13 2014 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2014-01-14
* Fri Jan 03 2014 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2014-01-06
* Mon Dec 23 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-09
* Thu Dec 19 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-23
* Tue Dec 17 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-18
* Thu Dec 12 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-12
* Mon Dec 09 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-09
* Fri Dec 06 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-12-10
* Tue Dec 03 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-11-28
* Wed Nov 20 2013 Steffen Templin <steffen.templin@open-xchange.com>
Fifth candidate for 7.4.1 release
* Tue Nov 19 2013 Steffen Templin <steffen.templin@open-xchange.com>
Fourth candidate for 7.4.1 release
* Thu Nov 07 2013 Steffen Templin <steffen.templin@open-xchange.com>
Third candidate for 7.4.1 release
* Wed Oct 23 2013 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.4.1 release
* Thu Oct 10 2013 Steffen Templin <steffen.templin@open-xchange.com>
First sprint increment for 7.4.0 release
* Fri Sep 20 2013 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.4.1 release
* Thu Sep 12 2013 Steffen Templin <steffen.templin@open-xchange.com>
Ninth candidate for 7.4.0 release
* Mon Sep 02 2013 Steffen Templin <steffen.templin@open-xchange.com>
Eighth candidate for 7.4.0 release
* Tue Aug 27 2013 Steffen Templin <steffen.templin@open-xchange.com>
Seventh candidate for 7.4.0 release
* Fri Aug 23 2013 Steffen Templin <steffen.templin@open-xchange.com>
Sixth candidate for 7.4.0 release
* Mon Aug 19 2013 Steffen Templin <steffen.templin@open-xchange.com>
Fifth release candidate for 7.4.0
* Tue Aug 13 2013 Steffen Templin <steffen.templin@open-xchange.com>
Fourth release candidate for 7.4.0
* Tue Aug 06 2013 Steffen Templin <steffen.templin@open-xchange.com>
Third release candidate for 7.4.0
* Fri Aug 02 2013 Steffen Templin <steffen.templin@open-xchange.com>
Second release candidate for 7.4.0
* Wed Jul 17 2013 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.4.0
* Tue Jul 16 2013 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.4.0
* Fri Jun 21 2013 Steffen Templin <steffen.templin@open-xchange.com>
Second feature freeze for 7.2.2 release
* Mon Jun 17 2013 Steffen Templin <steffen.templin@open-xchange.com>
Feature freeze for 7.2.2 release
* Mon Apr 15 2013 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.4.0
* Tue Apr 02 2013 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.2.0 release
* Tue Mar 26 2013 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.2.0
* Fri Mar 15 2013 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.2.0
* Thu Feb 14 2013 Steffen Templin <steffen.templin@open-xchange.com>
Second release candidate for 7.0.1
* Fri Feb 01 2013 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.0.1
* Thu Jan 10 2013 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.0.1
* Tue Dec 04 2012 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.0.0
* Tue Dec 04 2012 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.0.0 release
* Tue Nov 13 2012 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for EDP drop #6
* Fri Oct 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
Third release build for EDP drop #5
* Fri Oct 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
Second release build for EDP drop #5
* Fri Oct 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 6.22.1
* Fri Oct 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 6.22.1
* Thu Oct 11 2012 Steffen Templin <steffen.templin@open-xchange.com>
Release build for EDP drop #5
* Tue Sep 04 2012 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Steffen Templin <steffen.templin@open-xchange.com>
prepare for next EDP drop
* Tue Jul 03 2012 Steffen Templin <steffen.templin@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Steffen Templin <steffen.templin@open-xchange.com>
Release build for EDP drop #2
* Fri May 11 2012 Steffen Templin <steffen.templin@open-xchange.com>
Build for Rev. 4
* Tue May 08 2012 Steffen Templin <steffen.templin@open-xchange.com>
Build for Rev. 3
* Mon May 07 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #2
* Tue Feb 28 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
