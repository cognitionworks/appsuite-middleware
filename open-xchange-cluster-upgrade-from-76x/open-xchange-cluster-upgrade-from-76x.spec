%define __jar_repack %{nil}

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
%define         ox_release 17
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
* Mon Nov 28 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-12-05 (3763)
* Sat Nov 12 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-11-21 (3731)
* Tue Nov 08 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-11-07 (3678)
* Wed Oct 26 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-09-08 (3699)
* Mon Oct 17 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-10-24 (3630)
* Thu Oct 06 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-10-10 (3597)
* Mon Sep 19 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-09-26 (3572)
* Mon Sep 19 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-09-08 (3580)
* Mon Sep 05 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-09-12 (3547)
* Mon Aug 22 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-08-29 (3522)
* Mon Aug 15 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-08-26 (3512)
* Mon Aug 08 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-08-15 (3490)
* Fri Jul 22 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2016-08-01 (3467)
* Tue Jul 12 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second candidate for 7.8.2 release
* Wed Jul 06 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First candidate for 7.8.2 release
* Wed Jun 29 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second candidate for 7.8.2 release
* Thu Jun 16 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First candidate for 7.8.2 release
* Wed Apr 06 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
prepare for 7.8.2 release
* Wed Mar 30 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second candidate for 7.8.1 release
* Fri Mar 25 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First candidate for 7.8.1 release
* Tue Mar 15 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Fifth preview for 7.8.1 release
* Fri Mar 04 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Fourth preview for 7.8.1 release
* Sat Feb 20 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Third candidate for 7.8.1 release
* Wed Feb 03 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Second candidate for 7.8.1 release
* Tue Jan 26 2016 Tobias Friedrich <tobias.friedrich@open-xchange.com>
First candidate for 7.8.1 release
* Mon Oct 19 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Thu Oct 08 2015 Tobias Friedrich <tobias.friedrich@open-xchange.com>
prepare for 7.8.1
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
