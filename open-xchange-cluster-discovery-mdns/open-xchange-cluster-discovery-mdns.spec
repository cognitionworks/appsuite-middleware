
Name:          open-xchange-cluster-discovery-mdns
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 8
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Module for a cluster discovery service implementation based on Zeroconf
Requires:      open-xchange-core >= @OXVERSION@
Provides:      open-xchange-cluster-discovery
Conflicts:     open-xchange-cluster-discovery-static

%description
 This package installs the OSGi bundle implementing the OSGi ClusterDiscoveryService. 
 The implementation uses the Zerconf implementation provided by open-xchange-mdns to find all nodes within the cluster.
 This cluster discovery module is mutually exclusive with any other cluster discovery module. 
 Only one cluster discovery module can be installed on the backend.

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
* Fri Dec 28 2012 Steffen Templin <steffen.templin@open-xchange.com>
Build for public patch 2012-12-31
* Wed Dec 12 2012 Steffen Templin <steffen.templin@open-xchange.com>
Build for public patch 2012-12-04
* Mon Nov 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2012-11-28
* Wed Nov 14 2012 Steffen Templin <steffen.templin@open-xchange.com>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Steffen Templin <steffen.templin@open-xchange.com>
Fifth release candidate for 6.22.1
* Tue Nov 06 2012 Steffen Templin <steffen.templin@open-xchange.com>
Fourth release candidate for 6.22.1
* Fri Nov 02 2012 Steffen Templin <steffen.templin@open-xchange.com>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Steffen Templin <steffen.templin@open-xchange.com>
Second release candidate for 6.22.1
* Fri Oct 26 2012 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 6.22.1
* Wed Oct 24 2012 Steffen Templin <steffen.templin@open-xchange.com>
Initial release
