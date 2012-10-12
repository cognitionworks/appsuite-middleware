
Name:          open-xchange-indexing
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 1
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0 
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange backend indexing extension
Requires:      open-xchange-core >= @OXVERSION@

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
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/solr/
/opt/open-xchange/solr/*

%changelog
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
