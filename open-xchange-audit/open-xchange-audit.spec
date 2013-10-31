
Name:          open-xchange-audit
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 2
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Backend extension to track user actions
Requires:      open-xchange-core >= @OXVERSION@

%description
This package installs a bundle that allows tracking of user actions. The tracked information is written to a configurable log file.

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

    # SoftwareChange_Request-1640
    PFILE=/opt/open-xchange/etc/audit.properties
    if ! ox_exists_property com.openexchange.audit.logging.FileAccessLogging.enabled $PFILE; then
        ox_set_property com.openexchange.audit.logging.FileAccessLogging.enabled false $PFILE
    fi
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
/opt/open-xchange/etc/*


%changelog
* Wed Oct 30 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-28
* Thu Oct 24 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-30
* Wed Oct 23 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.4.1 release
* Tue Oct 22 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-23
* Mon Oct 21 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Thu Oct 17 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Tue Oct 15 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-11
* Mon Oct 14 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Mon Oct 14 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-15
* Thu Oct 10 2013 Marcus Klein <marcus.klein@open-xchange.com>
First sprint increment for 7.4.0 release
* Wed Oct 09 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-09
* Wed Oct 09 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-10-07
* Thu Sep 26 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-09-23
* Tue Sep 24 2013 Marcus Klein <marcus.klein@open-xchange.com>
Eleventh candidate for 7.4.0 release
* Fri Sep 20 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.4.1 release
* Fri Sep 20 2013 Marcus Klein <marcus.klein@open-xchange.com>
Tenth candidate for 7.4.0 release
* Thu Sep 12 2013 Marcus Klein <marcus.klein@open-xchange.com>
Ninth candidate for 7.4.0 release
* Wed Sep 11 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-09-12
* Thu Sep 05 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-09-05
* Mon Sep 02 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-09-26
* Mon Sep 02 2013 Marcus Klein <marcus.klein@open-xchange.com>
Eighth candidate for 7.4.0 release
* Fri Aug 30 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-08-30
* Tue Aug 27 2013 Marcus Klein <marcus.klein@open-xchange.com>
Seventh candidate for 7.4.0 release
* Fri Aug 23 2013 Marcus Klein <marcus.klein@open-xchange.com>
Sixth candidate for 7.4.0 release
* Thu Aug 22 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-08-22
* Tue Aug 20 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-08-19
* Mon Aug 19 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-08-21
* Mon Aug 19 2013 Marcus Klein <marcus.klein@open-xchange.com>
Fifth release candidate for 7.4.0
* Tue Aug 13 2013 Marcus Klein <marcus.klein@open-xchange.com>
Fourth release candidate for 7.4.0
* Tue Aug 06 2013 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 7.4.0
* Mon Aug 05 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-08-09
* Fri Aug 02 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 7.4.0
* Wed Jul 17 2013 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 7.4.0
* Tue Jul 16 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.4.0
* Fri Jul 12 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-07-18
* Fri Jul 12 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-07-18
* Thu Jul 11 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-07-10
* Wed Jul 03 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-06-27
* Mon Jul 01 2013 Marcus Klein <marcus.klein@open-xchange.com>
Third candidate for 7.2.2 release
* Fri Jun 28 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.2.2 release
* Wed Jun 26 2013 Marcus Klein <marcus.klein@open-xchange.com>
Release candidate for 7.2.2 release
* Fri Jun 21 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second feature freeze for 7.2.2 release
* Mon Jun 17 2013 Marcus Klein <marcus.klein@open-xchange.com>
Feature freeze for 7.2.2 release
* Mon Jun 10 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-06-11
* Fri Jun 07 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-06-20
* Mon Jun 03 2013 Marcus Klein <marcus.klein@open-xchange.com>
First sprint increment for 7.2.2 release
* Wed May 29 2013 Marcus Klein <marcus.klein@open-xchange.com>
First candidate for 7.2.2 release
* Mon May 27 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.2.2
* Thu May 23 2013 Marcus Klein <marcus.klein@open-xchange.com>
Third candidate for 7.2.1 release
* Wed May 15 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.2.1 release
* Mon Apr 22 2013 Marcus Klein <marcus.klein@open-xchange.com>
First candidate for 7.2.1 release
* Wed Apr 17 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.2.1
