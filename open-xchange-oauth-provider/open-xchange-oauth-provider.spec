%define __jar_repack %{nil}

Name:          open-xchange-oauth-provider
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
Summary:       The OAuth provider feature
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
OX App Suite is able to act as an OAuth 2.0 provider. Registered client
applications can access certain HTTP API calls in the name of users who
granted them access accordingly. This package adds the necessary core
functionality to serve those API calls.

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

if [ ${1:-0} -eq 2 ]; then
    # only when updating

    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'

    # SoftwareChange_Request-3098
    ox_add_property com.openexchange.oauth.provider.isAuthorizationServer true /opt/open-xchange/etc/oauth-provider.properties
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/templates/
/opt/open-xchange/templates/*
%config(noreplace) /opt/open-xchange/etc/hazelcast/authcode.properties
%config(noreplace) /opt/open-xchange/etc/oauth-provider.properties

%changelog
* Thu Oct 06 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-10-10 (3597)
* Mon Sep 19 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-09-26 (3572)
* Mon Sep 19 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-09-08 (3580)
* Mon Sep 05 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-09-12 (3547)
* Mon Aug 22 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-29 (3522)
* Mon Aug 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-26 (3512)
* Mon Aug 08 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-15 (3490)
* Fri Jul 22 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-01 (3467)
* Tue Jul 12 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.8.2 release
* Wed Jul 06 2016 Steffen Templin <steffen.templin@open-xchange.com>
First candidate for 7.8.2 release
* Wed Jun 29 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.8.2 release
* Thu Jun 16 2016 Steffen Templin <steffen.templin@open-xchange.com>
First candidate for 7.8.2 release
* Wed Apr 06 2016 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.8.2 release
* Wed Mar 30 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.8.1 release
* Fri Mar 25 2016 Steffen Templin <steffen.templin@open-xchange.com>
First candidate for 7.8.1 release
* Tue Mar 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Fifth preview for 7.8.1 release
* Fri Mar 04 2016 Steffen Templin <steffen.templin@open-xchange.com>
Fourth preview for 7.8.1 release
* Sat Feb 20 2016 Steffen Templin <steffen.templin@open-xchange.com>
Third candidate for 7.8.1 release
* Wed Feb 03 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.8.1 release
* Tue Jan 26 2016 Steffen Templin <steffen.templin@open-xchange.com>
First candidate for 7.8.1 release
* Mon Oct 19 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Thu Oct 08 2015 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.8.1
* Fri Oct 02 2015 Steffen Templin <steffen.templin@open-xchange.com>
Sixth candidate for 7.8.0 release
* Fri Sep 25 2015 Steffen Templin <steffen.templin@open-xchange.com>
Fith candidate for 7.8.0 release
* Fri Sep 18 2015 Steffen Templin <steffen.templin@open-xchange.com>
Fourth candidate for 7.8.0 release
* Mon Sep 07 2015 Steffen Templin <steffen.templin@open-xchange.com>
Third candidate for 7.8.0 release
* Fri Aug 21 2015 Steffen Templin <steffen.templin@open-xchange.com>
Second candidate for 7.8.0 release
* Wed Aug 05 2015 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.8.0
* Tue Apr 21 2015 Steffen Templin <steffen.templin@open-xchange.com>
Initial packaging
