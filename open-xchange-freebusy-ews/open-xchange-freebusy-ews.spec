
Name:          open-xchange-freebusy-ews
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: open-xchange-freebusy
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 2
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0 
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Microsoft Exchange / Open-Xchange Free/Busy Interoperability
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-freebusy >= @OXVERSION@

%description
Free/Busy data provider and -publisher accessing an Microsoft Exchange server using EWS.

Authors:
--------
    Open-Xchange

%prep

%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml build

%post
. /opt/open-xchange/lib/oxfunctions.sh
PROTECT="freebusy_provider_ews.properties freebusy_publisher_ews.properties"
for FILE in $PROTECT
do
    ox_update_permissions /opt/open-xchange/etc/$FILE root:open-xchange 640
done

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/etc/
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/freebusy_provider_ews.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/freebusy_publisher_ews.properties
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Tue Nov 13 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First release candidate for EDP drop #6
* Thu Oct 18 2012 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Initial release
