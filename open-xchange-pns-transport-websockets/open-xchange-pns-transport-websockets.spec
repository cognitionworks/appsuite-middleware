%define __jar_repack %{nil}

Name:          open-xchange-pns-transport-websockets
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-core
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:       @OXVERSION@
%define        ox_release 8
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Web Socket backed transport for Push Notification Service
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-pns-impl >= @OXVERSION@

%description
The Web Socket backed transport for Push Notification Service

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
. /opt/open-xchange/lib/oxfunctions.sh

# prevent bash from expanding, see bug 13316
GLOBIGNORE='*'

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Wed Dec 14 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-19 (3814)
* Tue Dec 13 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-14 (3806)
* Tue Dec 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-12 (3775)
* Fri Nov 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second release candidate for 7.8.3 release
* Thu Nov 24 2016 Thorben Betten <thorben.betten@open-xchange.com>
First release candidate for 7.8.3 release
* Tue Nov 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Third preview for 7.8.3 release
* Sat Oct 29 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second preview for 7.8.3 release
* Fri Oct 14 2016 Thorben Betten <thorben.betten@open-xchange.com>
First preview 7.8.3 release
* Tue Sep 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.3 release
* Mon Aug 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Initial release
