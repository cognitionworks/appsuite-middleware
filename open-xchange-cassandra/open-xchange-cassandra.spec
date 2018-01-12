%define __jar_repack %{nil}

Name:          open-xchange-cassandra
BuildArch:     noarch
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-core
%if 0%{?suse_version}
BuildRequires: java-1_8_0-openjdk-devel
%else
BuildRequires: java-1.8.0-openjdk-devel
%endif
Version:       @OXVERSION@
%define        ox_release 0
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange Cassandra Service
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
This package provides connectivity to a Cassandra cluster via a Cassandra Service


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
* Thu Oct 12 2017 Ioannis Chouklis <ioannis.chouklis@open-xchange.com>
prepare for 7.10.0 release
* Fri May 19 2017 Ioannis Chouklis <ioannis.chouklis@open-xchange.com>
First candidate for 7.8.4 release
* Thu May 04 2017 Ioannis Chouklis <ioannis.chouklis@open-xchange.com>
Second preview of 7.8.4 release
* Mon Apr 03 2017 Ioannis Chouklis <ioannis.chouklis@open-xchange.com>
First preview of 7.8.4 release
* Tue Dec 13 2016 Ioannis Chouklis <ioannis.chouklis@open-xchange.com>
Initial release
