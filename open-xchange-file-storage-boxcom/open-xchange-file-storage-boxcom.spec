%define __jar_repack %{nil}

Name:           open-xchange-file-storage-boxcom
BuildArch:      noarch
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires:  ant
%else
BuildRequires:  ant-nodeps
%endif
BuildRequires:  open-xchange-core
BuildRequires:  open-xchange-oauth
%if 0%{?suse_version}
BuildRequires: java-1_8_0-openjdk-devel
%else
BuildRequires: java-1.8.0-openjdk-devel
%endif
Version:        @OXVERSION@
%define         ox_release 17
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        The Open Xchange backend Box.com file storage extension
Autoreqprov:   no
Requires:       open-xchange-core >= @OXVERSION@
Requires:       open-xchange-oauth >= @OXVERSION@
Provides:       open-xchange-file-storage-boxcom = %{version}

%description
Adds Box.com file storage service to the backend installation.

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
* Tue Jul 23 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-07-29 (5340)
* Tue Jul 09 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-07-15 (5309)
* Mon Jun 24 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-07-01 (5290)
* Thu Jun 06 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-06-11 (5274)
* Mon May 13 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-05-14 (5247)
* Mon May 06 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-05-13 (5235)
* Wed Apr 24 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-04-29 (5211)
* Tue Mar 26 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-04-01 (5180)
* Tue Mar 12 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-03-11 (5149)
* Thu Feb 21 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-02-25 (5133)
* Thu Feb 07 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-02-11 (5108)
* Tue Jan 29 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-01-31 (5103)
* Mon Jan 21 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-01-28 (5076)
* Tue Jan 08 2019 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2019-01-14 (5023)
* Fri Nov 23 2018 Thorben Betten <thorben.betten@open-xchange.com>
RC 1 for 7.10.1 release
* Fri Nov 02 2018 Thorben Betten <thorben.betten@open-xchange.com>
Second preview for 7.10.1 release
* Thu Oct 11 2018 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.1 release
* Thu Sep 06 2018 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.1 release
* Fri Jun 29 2018 Thorben Betten <thorben.betten@open-xchange.com>
Fourth candidate for 7.10.0 release
* Wed Jun 27 2018 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.10.0 release
* Mon Jun 25 2018 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.10.0 release
* Mon Jun 11 2018 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.0 release
* Fri May 18 2018 Thorben Betten <thorben.betten@open-xchange.com>
Sixth preview of 7.10.0 release
* Thu Apr 19 2018 Thorben Betten <thorben.betten@open-xchange.com>
Fifth preview of 7.10.0 release
* Tue Apr 03 2018 Thorben Betten <thorben.betten@open-xchange.com>
Fourth preview of 7.10.0 release
* Tue Feb 20 2018 Thorben Betten <thorben.betten@open-xchange.com>
Third preview of 7.10.0 release
* Fri Feb 02 2018 Thorben Betten <thorben.betten@open-xchange.com>
Second preview for 7.10.0 release
* Fri Dec 01 2017 Thorben Betten <thorben.betten@open-xchange.com>
First preview for 7.10.0 release
* Thu Oct 12 2017 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.0 release
* Fri May 19 2017 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.4 release
* Thu May 04 2017 Thorben Betten <thorben.betten@open-xchange.com>
Second preview of 7.8.4 release
* Mon Apr 03 2017 Thorben Betten <thorben.betten@open-xchange.com>
First preview of 7.8.4 release
* Fri Nov 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second release candidate for 7.8.3 release
* Thu Nov 24 2016 Thorben Betten <thorben.betten@open-xchange.com>
First release candidate for 7.8.3 release
* Thu Nov 24 2016 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.4 release
* Tue Nov 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Third preview for 7.8.3 release
* Sat Oct 29 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second preview for 7.8.3 release
* Fri Oct 14 2016 Thorben Betten <thorben.betten@open-xchange.com>
First preview 7.8.3 release
* Tue Sep 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.3 release
* Tue Jul 12 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.2 release
* Wed Jul 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.2 release
* Wed Jun 29 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.2 release
* Thu Jun 16 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.2 release
* Wed Apr 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.2 release
* Wed Mar 30 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.1 release
* Fri Mar 25 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.1 release
* Tue Mar 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Fifth preview for 7.8.1 release
* Fri Mar 04 2016 Thorben Betten <thorben.betten@open-xchange.com>
Fourth preview for 7.8.1 release
* Sat Feb 20 2016 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.8.1 release
* Wed Feb 03 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.1 release
* Tue Jan 26 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.1 release
* Mon Oct 19 2015 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2015-10-26 (2812)
* Thu Oct 08 2015 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.1
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
