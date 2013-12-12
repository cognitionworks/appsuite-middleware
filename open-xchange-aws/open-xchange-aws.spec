
Name:          open-xchange-aws
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
Summary:       The Open-Xchange Amazon S3 package
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
This package installs the OSGi bundles needed for Amazon S3 access.

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
ox_update_permissions /opt/open-xchange/etc/aws.properties root:open-xchange 640

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/awss3.properties
%config(noreplace) /opt/open-xchange/etc/*

%changelog
* Thu Oct 10 2013 Markus Wagner <markus.wagner@open-xchange.com>
First sprint increment for 7.4.0 release
* Mon Oct 07 2013 Markus Wagner <markus.wagner@open-xchange.com>
prepare for 7.4.1
* Tue Jul 16 2013 Jan Bauerdick <jan.bauerdick@open-xchange.com>
prepare for 7.4.0
* Mon Apr 15 2013 Jan Bauerdick <jan.bauerdick@open-xchange.com>
prepare for 7.4.0
* Tue Apr 02 2013 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Second candidate for 7.2.0 release
* Tue Mar 26 2013 Jan Bauerdick <jan.bauerdick@open-xchange.com>
First release candidate for 7.2.0
* Fri Mar 15 2013 Jan Bauerdick <jan.bauerdick@open-xchange.com>
prepare for 7.2.0
* Tue Sep 11 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Initial release
