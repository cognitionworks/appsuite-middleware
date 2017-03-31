%define __jar_repack %{nil}

Name:          open-xchange-rest
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
%define        ox_release 34
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Micro services REST API
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@

%description
This package adds the bundles to the backend that provide the internal REST
API for the micro services architecture. Currently it contains the REST API
HTTP interface and some core services. These are services for accessing the
configuration, the database and for mapping email addresses to specific users
of certain contexts.

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

%changelog
* Fri Mar 31 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-04-03 (4048)
* Fri Feb 24 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-02-24 (3992)
* Wed Feb 08 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-02-20 (3950)
* Mon Jan 30 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-02-02 (3929)
* Thu Jan 26 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-01-26 (3923)
* Thu Jan 19 2017 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2017-01-23 (3877)
* Tue Dec 13 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-12-16 (3824)
* Wed Nov 23 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-11-28 (3758)
* Fri Nov 11 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-11-21 (3730)
* Fri Oct 28 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-11-07 (3677)
* Mon Oct 17 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-10-24 (3629)
* Fri Sep 30 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-10-10 (3596)
* Tue Sep 20 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-09-26 (3571)
* Mon Sep 05 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-09-12 (3546)
* Fri Aug 19 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-08-29 (3521)
* Mon Aug 08 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-08-15 (3489)
* Thu Jul 28 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-08-01 (3466)
* Thu Jul 14 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-07-18 (3433)
* Thu Jun 30 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-07-04 (3400)
* Wed Jun 15 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-06-20 (3347)
* Fri Jun 03 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-06-06 (3317)
* Fri May 20 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-05-23 (3294)
* Thu May 19 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-05-19 (3305)
* Fri May 06 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-05-09 (3272)
* Mon Apr 25 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-04-25 (3263)
* Fri Apr 15 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-04-25 (3239)
* Thu Apr 07 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-04-07 (3228)
* Wed Mar 30 2016 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.8.1 release
* Fri Mar 25 2016 Marcus Klein <marcus.klein@open-xchange.com>
First candidate for 7.8.1 release
* Wed Mar 23 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-03-29 (3188)
* Tue Mar 15 2016 Marcus Klein <marcus.klein@open-xchange.com>
Fifth preview for 7.8.1 release
* Mon Mar 07 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-03-14 (3148)
* Fri Mar 04 2016 Marcus Klein <marcus.klein@open-xchange.com>
Fourth preview for 7.8.1 release
* Fri Feb 26 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-29 (3141)
* Mon Feb 22 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-29 (3121)
* Sat Feb 20 2016 Marcus Klein <marcus.klein@open-xchange.com>
Third candidate for 7.8.1 release
* Mon Feb 15 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-18 (3106)
* Wed Feb 10 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-08 (3073)
* Wed Feb 03 2016 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.8.1 release
* Tue Jan 26 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-01-19 (3062)
* Tue Jan 26 2016 Marcus Klein <marcus.klein@open-xchange.com>
First candidate for 7.8.1 release
* Mon Jan 25 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-01-25 (3031)
* Sat Jan 23 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Sat Jan 23 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Fri Jan 15 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-01-15 (3028)
* Wed Jan 13 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-01-13 (2982)
* Tue Jan 12 2016 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-12-23 (3011)
* Tue Dec 29 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2016-01-05 (2989)
* Tue Dec 22 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-12-23 (2971)
* Fri Dec 11 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-12-21 (2953)
* Tue Dec 08 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-12-07 (2918)
* Thu Nov 19 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-11-23 (2878)
* Thu Nov 05 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-11-09 (2840)
* Fri Oct 30 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-11-02 (2853)
* Tue Oct 20 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-10-26 (2813)
* Mon Oct 19 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-10-30 (2818)
* Mon Oct 19 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Mon Oct 12 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-10-23 (2806)
* Thu Oct 08 2015 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.8.1
* Fri Oct 02 2015 Marcus Klein <marcus.klein@open-xchange.com>
Sixth candidate for 7.8.0 release
* Wed Sep 30 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-10-12 (2784)
* Fri Sep 25 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-09-28 (2767)
* Fri Sep 25 2015 Marcus Klein <marcus.klein@open-xchange.com>
Fith candidate for 7.8.0 release
* Fri Sep 18 2015 Marcus Klein <marcus.klein@open-xchange.com>
Fourth candidate for 7.8.0 release
* Tue Sep 08 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-09-14 (2732)
* Mon Sep 07 2015 Marcus Klein <marcus.klein@open-xchange.com>
Third candidate for 7.8.0 release
* Wed Sep 02 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-09-01 (2726)
* Mon Aug 24 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-08-24 (2674)
* Fri Aug 21 2015 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.8.0 release
* Mon Aug 17 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-08-12 (2671)
* Thu Aug 06 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-08-17 (2666)
* Wed Aug 05 2015 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 7.8.0
* Tue Aug 04 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-08-10 (2655)
* Mon Aug 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-08-03 (2650)
* Thu Jul 23 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-07-27 (2626)
* Wed Jul 15 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-07-20 (2614)
* Fri Jul 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-07-10
* Fri Jul 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-07-02 (2611)
* Fri Jul 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-29 (2578)
* Fri Jul 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-29 (2542)
* Wed Jun 24 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-29 (2569)
* Wed Jun 24 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-26 (2573)
* Wed Jun 10 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-08 (2539)
* Wed Jun 10 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-06-08 (2540)
* Mon May 18 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-26 (2521)
* Fri May 15 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-15 (2529)
* Fri May 08 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-12 (2478)
* Thu Apr 30 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-04 (2496)
* Thu Apr 30 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-04 (2497)
* Tue Apr 28 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-05-04 (2505)
* Fri Apr 24 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-09-09 (2495)
* Tue Apr 14 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-04-13 (2473)
* Wed Apr 08 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-04-13 (2474)
* Tue Apr 07 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-04-09 (2486)
* Thu Mar 26 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-03-30 (2459)
* Mon Mar 23 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-03-20
* Tue Mar 17 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-03-18
* Fri Mar 13 2015 Marcus Klein <marcus.klein@open-xchange.com>
Twelfth candidate for 7.6.2 release
* Fri Mar 06 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-03-16
* Fri Mar 06 2015 Marcus Klein <marcus.klein@open-xchange.com>
Eleventh candidate for 7.6.2 release
* Wed Mar 04 2015 Marcus Klein <marcus.klein@open-xchange.com>
Tenth candidate for 7.6.2 release
* Tue Mar 03 2015 Marcus Klein <marcus.klein@open-xchange.com>
Nineth candidate for 7.6.2 release
* Thu Feb 26 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-02-23
* Tue Feb 24 2015 Marcus Klein <marcus.klein@open-xchange.com>
Eighth candidate for 7.6.2 release
* Mon Feb 23 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-02-25
* Thu Feb 12 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-02-23
* Wed Feb 11 2015 Marcus Klein <marcus.klein@open-xchange.com>
Seventh candidate for 7.6.2 release
* Fri Feb 06 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-02-10
* Fri Feb 06 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-02-09
* Fri Jan 30 2015 Marcus Klein <marcus.klein@open-xchange.com>
Sixth candidate for 7.6.2 release
* Wed Jan 28 2015 Marcus Klein <marcus.klein@open-xchange.com>
Fifth candidate for 7.6.2 release
* Mon Jan 26 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-27
* Mon Jan 26 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-01-26
* Wed Jan 21 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-01-29
* Mon Jan 12 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-01-09
* Wed Jan 07 2015 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-01-12
* Tue Dec 30 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2015-01-12
* Tue Dec 16 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-10
* Fri Dec 12 2014 Marcus Klein <marcus.klein@open-xchange.com>
Fourth candidate for 7.6.2 release
* Mon Dec 08 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-15
* Mon Dec 08 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-10
* Fri Dec 05 2014 Marcus Klein <marcus.klein@open-xchange.com>
Third candidate for 7.6.2 release
* Thu Dec 04 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-09
* Tue Dec 02 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-03
* Tue Nov 25 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-01
* Mon Nov 24 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-01
* Mon Nov 24 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-12-01
* Fri Nov 21 2014 Marcus Klein <marcus.klein@open-xchange.com>
Second candidate for 7.6.2 release
* Wed Nov 19 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-21
* Tue Nov 18 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-20
* Mon Nov 10 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-17
* Mon Nov 10 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-17
* Wed Nov 05 2014 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.8.0 release
* Tue Nov 04 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-10
* Fri Oct 31 2014 Marcus Klein <marcus.klein@open-xchange.com>
First candidate for 7.6.2 release
* Tue Oct 28 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-03
* Mon Oct 27 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-30
* Fri Oct 24 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-11-04
* Fri Oct 24 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-22
* Fri Oct 17 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-24
* Tue Oct 14 2014 Marcus Klein <marcus.klein@open-xchange.com>
Fifth candidate for 7.6.1 release
* Fri Oct 10 2014 Marcus Klein <marcus.klein@open-xchange.com>
Fourth candidate for 7.6.1 release
* Fri Oct 10 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-20
* Thu Oct 09 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-13
* Tue Oct 07 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-09
* Tue Oct 07 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-10
* Thu Oct 02 2014 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 7.6.1
* Tue Sep 30 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-06
* Fri Sep 26 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-09-29
* Tue Sep 23 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-10-02
* Thu Sep 18 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-09-23
* Wed Sep 17 2014 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.6.2 release
* Tue Sep 16 2014 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 7.6.1
* Mon Sep 08 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-09-15
* Fri Sep 05 2014 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 7.6.1
* Thu Aug 21 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-08-25
* Mon Aug 18 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-08-25
* Tue Aug 05 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-08-06
* Mon Aug 04 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-08-11
* Mon Jul 28 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-07-30
* Tue Jul 15 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-07-21
* Mon Jul 14 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-07-24
* Thu Jul 10 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-07-15
* Tue Jul 01 2014 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2014-07-07
* Thu Jun 26 2014 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.6.1
* Mon Jun 23 2014 Marcus Klein <marcus.klein@open-xchange.com>
Seventh candidate for 7.6.0 release
* Fri Jun 20 2014 Marcus Klein <marcus.klein@open-xchange.com>
Sixth release candidate for 7.6.0
* Fri Jun 13 2014 Marcus Klein <marcus.klein@open-xchange.com>
Fifth release candidate for 7.6.0
* Fri May 30 2014 Marcus Klein <marcus.klein@open-xchange.com>
Fourth release candidate for 7.6.0
* Fri May 16 2014 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 7.6.0
* Mon May 05 2014 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 7.6.0
* Fri May 02 2014 Marcus Klein <marcus.klein@open-xchange.com>
initial packaging for internal REST services API
