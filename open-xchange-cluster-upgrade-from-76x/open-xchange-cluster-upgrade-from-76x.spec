
Name:           open-xchange-cluster-upgrade-from-76x
BuildArch:      noarch
#!BuildIgnore:  post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires:  ant
%else
BuildRequires:  ant-nodeps
%endif
BuildRequires:  open-xchange-core
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires:  java7-devel
%else
BuildRequires:  java-devel >= 1.7.0
%endif
Version:        @OXVERSION@
%define         ox_release 9
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        Server module to invalidate cluster nodes running v7.6.x of the Open-Xchange server (Hazelcast v3.2.4) during upgrade
Autoreqprov:    no
Requires:       open-xchange-core >= @OXVERSION@

%description
Server module to invalidate cluster nodes running v7.6.x of the Open-Xchange server (Hazelcast v3.2.4) during upgrade

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

%post
. /opt/open-xchange/lib/oxfunctions.sh

# prevent bash from expanding, see bug 13316
GLOBIGNORE='*'

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Mon Nov 02 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2015-11-09 (2840)
* Fri Oct 30 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2015-11-02 (2853)
* Mon Oct 19 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Fri Oct 02 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Sixth candidate for 7.8.0 release
* Fri Sep 25 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Fith candidate for 7.8.0 release
* Fri Sep 18 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Fourth candidate for 7.8.0 release
* Mon Sep 07 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Third candidate for 7.8.0 release
* Fri Aug 21 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second candidate for 7.8.0 release
* Wed Aug 05 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First release candidate for 7.8.0
* Wed Apr 29 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Initial release
