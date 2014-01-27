
Name:           open-xchange-mobile-config
BuildArch: 	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant
BuildRequires:  ant-nodeps
BuildRequires:  java-devel >= 1.6.0
# TODO: version not hardcoded in spec file
Version:	@OXVERSION@
%define		ox_release 27
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        Creative Commons Attribution-Noncommercial-Share Alike 2.5 Generic
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        Config files for the Open-Xchange Mobile UI
Requires:       open-xchange-core >= @OXVERSION@

%description
 This package needs to be installed on the backend hosts of a cluster installation. It adds configuration files to the backend allowing the
 administrator to define some defaults for the mobile web app. Additionally it adds configuration paths on the backend for the Mobile Web Interface
 that allows to store end user preferences.

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml clean build

%post
if [ ${1:-0} -eq 2 ]; then
    # only when updating
    . /opt/open-xchange/lib/oxfunctions.sh

    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'

    # SoftwareChange_Request-1339
    pfile=/opt/open-xchange/etc/settings/oxmobile.properties
    if ! ox_exists_property mox/config/defaultContactFolder $pfile; then
        ox_set_property mox/config/defaultContactFolder "private" $pfile
    fi

    # SoftwareChange_Request-1294
    pfile=/opt/open-xchange/etc/settings/oxmobile.properties
    if ! ox_exists_property mox/defaultContactStoreFolder $pfile; then
        ox_set_property mox/defaultContactStoreFolder "-1" $pfile
    fi
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/settings
%dir /opt/open-xchange/etc/meta
%config(noreplace) /opt/open-xchange/etc/settings/*
%config(noreplace) /opt/open-xchange/etc/meta/*

%changelog
* Wed Jan 22 2014 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2014-01-22
* Thu Dec 19 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-12-23
* Fri Dec 06 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-11-29
* Mon Nov 11 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-11-12
* Thu Oct 24 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-10-30
* Thu Oct 17 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-10-21
* Tue Oct 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-10-11
* Mon Oct 14 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-10-21
* Mon Oct 14 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-10-15
* Thu Sep 26 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-09-23
* Wed Sep 11 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-09-12
* Thu Sep 05 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-09-05
* Mon Sep 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-09-26
* Fri Aug 30 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-30
* Thu Aug 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-22
* Thu Aug 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-22
* Tue Aug 20 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-19
* Mon Aug 19 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-21
* Mon Aug 05 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-08-09
* Mon Jul 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second build for patch  2013-07-18
* Mon Jul 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-07-18
* Fri Jul 12 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-07-18
* Thu Jul 11 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-07-10
* Mon Jul 01 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third candidate for 7.2.2 release
* Fri Jun 28 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second candidate for 7.2.2 release
* Wed Jun 26 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release candidate for 7.2.2 release
* Fri Jun 21 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second feature freeze for 7.2.2 release
* Mon Jun 17 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Feature freeze for 7.2.2 release
* Tue Jun 11 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-06-13
* Mon Jun 10 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-06-11
* Fri Jun 07 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-06-20
* Mon Jun 03 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First sprint increment for 7.2.2 release
* Wed May 29 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First candidate for 7.2.2 release
* Tue May 28 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second build for patch 2013-05-28
* Mon May 27 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.2.2
* Thu May 23 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third candidate for 7.2.1 release
* Wed May 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-22
* Wed May 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-22
* Wed May 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-22
* Wed May 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second candidate for 7.2.1 release
* Wed May 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-10
* Mon May 13 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-09
* Mon May 13 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-09
* Mon May 13 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-09
* Mon May 13 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-09
* Mon May 13 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-05-09
* Fri May 03 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-23
* Tue Apr 30 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-17
* Mon Apr 22 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First candidate for 7.2.1 release
* Mon Apr 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.2.1
* Fri Apr 12 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-12
* Wed Apr 10 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fourth candidate for 7.2.0 release
* Tue Apr 09 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third candidate for 7.2.0 release
* Tue Apr 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second candidate for 7.2.0 release
* Tue Apr 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-04
* Tue Apr 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-04
* Tue Apr 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-04
* Tue Apr 02 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-04-04
* Tue Mar 26 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 7.2.0
* Fri Mar 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.2.0
* Tue Mar 12 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Sixth release candidate for 6.22.2/7.0.2
* Mon Mar 11 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fifth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fourth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release candidate for 6.22.2/7.0.2
* Thu Mar 07 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release candidate for 6.22.2/7.0.2
* Mon Mar 04 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-03-07
* Mon Mar 04 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-03-08
* Fri Mar 01 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-03-07
* Wed Feb 27 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 6.22.2/7.0.2
* Tue Feb 26 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-02-22
* Mon Feb 25 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-02-22
* Tue Feb 19 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fourth release candidate for 7.0.1
* Tue Feb 19 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release candidate for 7.0.1
* Tue Feb 19 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.0.2 release
* Fri Feb 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-02-13
* Thu Feb 14 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release candidate for 7.0.1
* Fri Feb 01 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 7.0.1
* Tue Jan 29 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-01-28
* Mon Jan 21 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-01-24
* Tue Jan 15 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2013-01-23
* Thu Jan 10 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.0.1
* Thu Jan 03 2013 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for public patch 2013-01-15
* Fri Dec 28 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for public patch 2012-12-31
* Fri Dec 21 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for public patch 2012-12-21
* Tue Dec 18 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release candidate for 7.0.0
* Mon Dec 17 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release candidate for 7.0.0
* Wed Dec 12 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for public patch 2012-12-04
* Tue Dec 04 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 7.0.0
* Tue Dec 04 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 7.0.0 release
* Mon Nov 26 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Build for patch 2012-11-28
* Wed Nov 14 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fifth release candidate for 6.22.1
* Tue Nov 13 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for EDP drop #6
* Tue Nov 06 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fourth release candidate for 6.22.1
* Fri Nov 02 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 6.22.1
* Thu Oct 11 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build for EDP drop #5
* Wed Oct 10 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Second release candidate for 6.22.0
* Tue Sep 04 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 6.23.0
* Mon Sep 03 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for next EDP drop
* Tue Aug 21 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
First release candidate for 6.22.0
* Mon Aug 20 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
prepare for 6.22.0
* Tue Jul 03 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build for EDP drop #2
* Mon May 07 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Bugfixbuild
* Mon May 07 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Bugfixbuild for ox.io
* Tue Apr 03 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build for 1.1.0
* Tue Apr 03 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build for 1.1.0
* Tue Apr 03 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build for 1.1.0
* Thu Mar 29 2012 Marcus Klein <jenkins@jenkins.netline.de>
Next test build
* Thu Mar 29 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build
* Wed Mar 28 2012 Marcus Klein <jenkins@hudson-slave-1.netline.de>
Release build
* Thu Feb 16 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial 1.1 release.
