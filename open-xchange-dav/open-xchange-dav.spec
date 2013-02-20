
Name:          open-xchange-dav
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
Summary:       The Open-Xchange CardDAV and CalDAV implementation
Requires:      open-xchange-core >= @OXVERSION@
Provides:      open-xchange-caldav = %{version}
Obsoletes:     open-xchange-caldav <= %{version}
Provides:      open-xchange-carddav = %{version}
Obsoletes:     open-xchange-carddav <= %{version}
Provides:      open-xchange-webdav-directory = %{version}
Obsoletes:     open-xchange-webdav-directory <= %{version}
Provides:      open-xchange-webdav-acl = %{version}
Obsoletes:     open-xchange-webdav-acl <= %{version}

%description
The Open-Xchange CardDAV and CalDAV implementation.

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
CONFFILES="caldav.properties contextSets/caldav.yml meta/caldav.yml carddav.properties contextSets/carddav.yml"
for FILE in ${CONFFILES}; do
    ox_move_config_file /opt/open-xchange/etc/groupware /opt/open-xchange/etc $FILE
done

# prevent bash from expanding, see bug 13316
GLOBIGNORE='*'

# SoftwareChange_Request-1028
pfile=/opt/open-xchange/etc/carddav.properties
if ! ox_exists_property com.openexchange.carddav.tree $pfile; then
    ox_set_property com.openexchange.carddav.tree "0" $pfile
fi
if ! ox_exists_property com.openexchange.carddav.exposedCollections $pfile; then
    ox_set_property com.openexchange.carddav.exposedCollections "0" $pfile
fi
# SoftwareChange_Request-1129
if ! ox_exists_property com.openexchange.carddav.reducedAggregatedCollection $pfile; then
    ox_set_property com.openexchange.carddav.reducedAggregatedCollection "false" $pfile
fi

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
%config(noreplace) /opt/open-xchange/etc/*.properties
%config(noreplace) /opt/open-xchange/etc/meta/*
%config(noreplace) /opt/open-xchange/etc/contextSets/*

%changelog
* Wed Feb 20 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-02-22
* Mon Jan 21 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-01-24
* Thu Jan 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for public patch 2013-01-15
* Wed Oct 10 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 6.22.0
* Tue Aug 21 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 6.22.0
* Tue Jul 03 2012 Steffen Templin <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Tue May 22 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #2
* Mon Apr 16 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #1
* Wed Apr 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #0
* Mon Jan 30 2012 Steffen Templin <steffen.templin@open-xchange.com>
Initial release
