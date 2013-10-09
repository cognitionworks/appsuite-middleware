
Name:          open-xchange-realtime-json
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-realtime-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 4
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Realtime channel implementation for frequent polling
Requires:      open-xchange-realtime-core >= @OXVERSION@
Requires:      open-xchange-grizzly >= @OXVERSION@
Obsoletes:     open-xchange-realtime-atmosphere

%description


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
* Wed Oct 09 2013 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2013-10-07
* Tue Sep 24 2013 Steffen Templin <steffen.templin@open-xchange.com>
Eleventh candidate for 7.4.0 release
* Fri Sep 20 2013 Steffen Templin <steffen.templin@open-xchange.com>
Tenth candidate for 7.4.0 release
* Thu Sep 12 2013 Steffen Templin <steffen.templin@open-xchange.com>
Ninth candidate for 7.4.0 release
* Tue Sep 03 2013 Steffen Templin <steffen.templin@open-xchange.com>
Initial build
