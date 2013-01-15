
Name:          open-xchange-freebusy
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 5
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0 
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Free/Busy module
Requires:      open-xchange-core >= @OXVERSION@

%description
Free/Busy service providing user- and resource-availability information

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
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Tue Jan 15 2013 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2013-01-28
* Fri Dec 21 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for public patch 2012-12-21
* Tue Dec 18 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Third release candidate for 7.0.0
* Mon Dec 17 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second release candidate for 7.0.0
* Tue Dec 04 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First release candidate for 7.0.0
* Tue Dec 04 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
prepare for 7.0.0 release
* Tue Nov 13 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First release candidate for EDP drop #6
* Fri Oct 26 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
prepare for 6.22.1
* Thu Oct 18 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
prepare for 6.23.0
