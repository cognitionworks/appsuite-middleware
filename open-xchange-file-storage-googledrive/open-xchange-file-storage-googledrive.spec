
Name:           open-xchange-file-storage-googledrive
BuildArch:      noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires:  ant
%else
BuildRequires:  ant-nodeps
%endif
BuildRequires:  open-xchange-core
BuildRequires:  open-xchange-oauth
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:        @OXVERSION@
%define         ox_release 31
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        The Open Xchange backend Google Drive file storage extension
Autoreqprov:   no
Requires:       open-xchange-core >= @OXVERSION@
Requires:       open-xchange-oauth >= @OXVERSION@
Provides:       open-xchange-file-storage-googledrive = %{version}

%description
Adds Google Drive file storage service to the backend installation.

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
* Fri May 13 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-05-23 (3293)
* Tue May 03 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-05-09 (3271)
* Fri Apr 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-04-25 (3238)
* Tue Apr 05 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-04-11 (3214)
* Wed Mar 23 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-03-29 (3188)
* Mon Mar 07 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-03-14 (3148)
* Fri Feb 26 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-29 (3141)
* Mon Feb 22 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-29 (3121)
* Mon Feb 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-18 (3106)
* Wed Feb 10 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-08 (3073)
* Tue Jan 26 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-01-19 (3062)
* Mon Jan 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-01-25 (3031)
* Sat Jan 23 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Sat Jan 23 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-02-05 (3058)
* Fri Jan 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-01-15 (3028)
* Wed Jan 13 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-01-13 (2982)
* Tue Jan 12 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-12-23 (3011)
* Tue Dec 29 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-01-05 (2989)
* Tue Dec 22 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-12-23 (2971)
* Fri Dec 11 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-12-21 (2953)
* Tue Dec 08 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-12-07 (2918)
* Thu Nov 19 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-11-23 (2878)
* Thu Nov 05 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-11-09 (2840)
* Fri Oct 30 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-11-02 (2853)
* Mon Oct 19 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Fri Oct 02 2015 Thorben Betten <thorben.betten@open-xchange.com>
Sixth candidate for 7.8.0 release
* Fri Sep 25 2015 Thorben Betten <thorben.betten@open-xchange.com>
Fith candidate for 7.8.0 release
* Fri Sep 18 2015 Thorben Betten <thorben.betten@open-xchange.com>
Fourth candidate for 7.8.0 release
* Mon Sep 07 2015 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.8.0 release
* Fri Aug 21 2015 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.0 release
* Wed Aug 05 2015 Thorben Betten <thorben.betten@open-xchange.com>
First release candidate for 7.8.0
* Fri Dec 12 2014 Thorben Betten <thorben.betten@open-xchange.com>
Fourth candidate for 7.6.2 release
* Mon Dec 08 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-12-15
* Fri Dec 05 2014 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.6.2 release
* Tue Dec 02 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-12-03
* Tue Nov 25 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-12-01
* Mon Nov 24 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-12-01
* Fri Nov 21 2014 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.6.2 release
* Tue Nov 18 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-11-20
* Mon Nov 10 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-11-17
* Wed Nov 05 2014 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.0 release
* Fri Oct 31 2014 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.6.2 release
* Mon Oct 27 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-10-30
* Fri Oct 17 2014 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2014-10-24
* Tue Oct 14 2014 Thorben Betten <thorben.betten@open-xchange.com>
Fifth candidate for 7.6.1 release
* Fri Oct 10 2014 Thorben Betten <thorben.betten@open-xchange.com>
Fourth candidate for 7.6.1 release
* Thu Oct 02 2014 Thorben Betten <thorben.betten@open-xchange.com>
Third release candidate for 7.6.1
* Wed Sep 17 2014 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.6.2 release
* Tue Sep 16 2014 Thorben Betten <thorben.betten@open-xchange.com>
Second release candidate for 7.6.1
* Fri Sep 05 2014 Thorben Betten <thorben.betten@open-xchange.com>
First release candidate for 7.6.1
* Thu Jun 26 2014 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.6.1
