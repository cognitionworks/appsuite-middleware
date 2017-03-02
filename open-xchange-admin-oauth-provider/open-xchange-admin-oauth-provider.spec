%define __jar_repack %{nil}

Name:          open-xchange-admin-oauth-provider
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-oauth-provider
BuildRequires: open-xchange-admin
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:       @OXVERSION@
%define        ox_release 17
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The OAuth provider management interfaces
Autoreqprov:   no
Requires:      open-xchange-oauth-provider >= @OXVERSION@
Requires:      open-xchange-admin >= @OXVERSION@

%description
This package adds the management interfaces for the OAuth 2.0 provider
feature.

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
%dir /opt/open-xchange/lib/
/opt/open-xchange/lib/*
%dir /opt/open-xchange/sbin/
/opt/open-xchange/sbin/*
%doc com.openexchange.oauth.provider.rmi/javadoc

%changelog
* Thu Mar 02 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-03-06 (3985)
* Fri Feb 24 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-02-24 (3994)
* Wed Feb 22 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-02-22 (3969)
* Tue Feb 14 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-02-20 (3952)
* Tue Jan 31 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-02-06 (3918)
* Thu Jan 26 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-01-26 (3925)
* Wed Jan 18 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-01-23 (3879)
* Wed Jan 04 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-01-09 (3849)
* Tue Dec 20 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-12-23 (3857)
* Wed Dec 14 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-12-19 (3814)
* Tue Dec 13 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-12-14 (3806)
* Tue Dec 06 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-12-12 (3775)
* Fri Nov 25 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second release candidate for 7.8.3 release
* Thu Nov 24 2016 Steffen Templin <steffen.templin@open-xchange.com>
First release candidate for 7.8.3 release
* Tue Nov 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Third preview for 7.8.3 release
* Sat Oct 29 2016 Steffen Templin <steffen.templin@open-xchange.com>
Second preview for 7.8.3 release
* Fri Oct 14 2016 Steffen Templin <steffen.templin@open-xchange.com>
First preview 7.8.3 release
* Tue Sep 06 2016 Steffen Templin <steffen.templin@open-xchange.com>
prepare for 7.8.3 release
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
