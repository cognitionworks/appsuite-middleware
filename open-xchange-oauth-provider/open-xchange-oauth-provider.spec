
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
%define        ox_release 44
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
* Tue Sep 05 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-09-07 (4343)
* Thu Jan 26 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-01-26 (3926)
* Thu Jan 19 2017 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2017-01-23 (3876)
* Fri Nov 11 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-11-21 (3729)
* Thu Oct 13 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-10-24 (3628)
* Tue Sep 20 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-09-26 (3570)
* Fri Aug 19 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-29 (3520)
* Wed Aug 03 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-03 (3476)
* Mon Jul 25 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-08-01 (3465)
* Mon Jul 11 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-07-18 (3432)
* Thu Jun 30 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-07-04 (3399)
* Wed Jun 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-06-20 (3346)
* Fri May 27 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-06-06 (3316)
* Fri May 13 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-05-23 (3293)
* Tue May 03 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-05-09 (3271)
* Fri Apr 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-04-25 (3238)
* Tue Apr 05 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-04-11 (3214)
* Wed Mar 23 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-03-29 (3188)
* Mon Mar 07 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-03-14 (3148)
* Fri Feb 26 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-29 (3141)
* Mon Feb 22 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-29 (3121)
* Mon Feb 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-18 (3106)
* Wed Feb 10 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-08 (3073)
* Tue Jan 26 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-01-19 (3062)
* Mon Jan 25 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-01-25 (3031)
* Sat Jan 23 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Sat Jan 23 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Fri Jan 15 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-01-15 (3028)
* Wed Jan 13 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-01-13 (2982)
* Tue Jan 12 2016 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-12-23 (3011)
* Tue Dec 29 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2016-01-05 (2989)
* Tue Dec 22 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-12-23 (2971)
* Fri Dec 11 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-12-21 (2953)
* Tue Dec 08 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-12-07 (2918)
* Thu Nov 19 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-11-23 (2878)
* Thu Nov 05 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-11-09 (2840)
* Fri Oct 30 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-11-02 (2853)
* Mon Oct 19 2015 Steffen Templin <steffen.templin@open-xchange.com>
Build for patch 2015-10-26 (2812)
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
