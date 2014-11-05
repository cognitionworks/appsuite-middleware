
Name:          open-xchange-chat
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 0
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Backend chat extension
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
This package contains the backend components for the chat feature. This includes
database storage, chat logic and HTTP/JSON chat interface for clients.

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml clean build

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Wed Nov 05 2014 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.8.0 release
* Thu Oct 30 2014 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.6.2 release
* Thu Jun 26 2014 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.6.1
* Wed Feb 12 2014 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.6.0
* Wed Dec 18 2013 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.4.2
* Thu Oct 10 2013 Markus Wagner <markus.wagner@open-xchange.com>
First sprint increment for 7.4.0 release
* Mon Oct 07 2013 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.4.1
* Tue Jul 16 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.4.0
* Mon Apr 15 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.4.0
* Tue Apr 02 2013 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.2.0 release
* Tue Mar 26 2013 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 7.2.0
* Fri Mar 15 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.2.0
* Thu Jan 10 2013 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.0.1
* Tue Dec 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.0.0 release
* Tue Nov 13 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for EDP drop #6
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.1
* Thu Oct 11 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #5
* Tue Sep 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for next EDP drop
* Tue Jul 03 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Tue May 22 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #2
* Mon Apr 16 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #1
* Wed Apr 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #0
* Fri Feb 10 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
