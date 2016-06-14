%define __jar_repack %{nil}

Name:          open-xchange-filestore-swift
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
%define        ox_release 12
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Filestore implementation storing files in a Swift storage
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
This package installs the OSGi bundles needed for storing files in a Swift storage.

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
ox_update_permissions /opt/open-xchange/etc/filestore-swift.properties root:open-xchange 640
if [ ${1:-0} -eq 2 ]; then
    # only when updating
    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'
    PFILE=/opt/open-xchange/etc/filestore-swift.properties

fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/filestore-swift.properties
%config(noreplace) /opt/open-xchange/etc/*

%changelog
* Tue Jun 14 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-06-20 (3347)
* Fri Jun 03 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-06-06 (3317)
* Fri May 20 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-05-23 (3294)
* Thu May 19 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-05-19 (3305)
* Fri May 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-05-09 (3272)
* Mon Apr 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-04-25 (3263)
* Fri Apr 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-04-25 (3239)
* Thu Apr 07 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-04-07 (3228)
* Wed Mar 30 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.1 release
* Fri Mar 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.1 release
* Tue Mar 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Fifth preview for 7.8.1 release
* Fri Mar 04 2016 Thorben Betten <thorben.betten@open-xchange.com>
Fourth preview for 7.8.1 release
* Mon Feb 22 2016 Thorben Betten <thorben.betten@open-xchange.com>
Initial release
