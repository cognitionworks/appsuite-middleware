
Name:           open-xchange-blackwhitelist
BuildArch:      noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant
BuildRequires:  ant-nodeps
BuildRequires:  open-xchange-core >= @OXVERSION@
BuildRequires:  java-devel >= 1.6.0
Version:        @OXVERSION@
%define         ox_release 28
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        JSON interface for some black/white list implementation
Requires:       open-xchange-core >= @OXVERSION@

%description
This package installs an OSGi bundle that adds the JSON interface for some black/white list implementation. The concrete implementation
needs to be installed additionally and is not required by this package.

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
%if 0%{?rhel_version} || 0%{?fedora_version}
%define docroot /var/www/html
%else
%define docroot /srv/www/htdocs
%endif

ant -lib build/lib -Dbasedir=build -Dhtdoc=%{docroot} -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml build

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/settings
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
/opt/open-xchange/bundles/*
%config(noreplace) /opt/open-xchange/etc/settings/*
%doc com.openexchange.blackwhitelist/ChangeLog

%changelog
* Fri Jan 31 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-02-03
* Tue Jan 28 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-01-30
* Fri Jan 24 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-17
* Fri Jan 10 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-17
* Thu Dec 19 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-23
* Tue Dec 17 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-19
* Tue Dec 17 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-16
* Thu Dec 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-12-12
* Mon Nov 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-11-12
* Fri Nov 08 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-11-11
* Thu Nov 07 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-11-08
* Tue Nov 05 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-11-12
* Wed Oct 30 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-28
* Thu Oct 24 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-30
* Tue Oct 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-23
* Mon Oct 21 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-21
* Wed Oct 09 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-09
* Wed Oct 09 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-10-07
* Tue Sep 24 2013 Carsten Hoeger <choeger@open-xchange.com>
Eleventh candidate for 7.4.0 release
* Fri Sep 20 2013 Carsten Hoeger <choeger@open-xchange.com>
Tenth candidate for 7.4.0 release
* Tue Sep 17 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-26
* Thu Sep 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Ninth candidate for 7.4.0 release
* Wed Sep 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-12
* Wed Sep 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-12
* Thu Sep 05 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-05
* Mon Sep 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-26
* Mon Sep 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Eighth candidate for 7.4.0 release
* Fri Aug 30 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-30
* Wed Aug 28 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-09-03
* Tue Aug 27 2013 Carsten Hoeger <choeger@open-xchange.com>
Seventh candidate for 7.4.0 release
* Fri Aug 23 2013 Carsten Hoeger <choeger@open-xchange.com>
Sixth candidate for 7.4.0 release
* Thu Aug 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-22
* Thu Aug 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-22
* Tue Aug 20 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-19
* Mon Aug 19 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-21
* Mon Aug 19 2013 Carsten Hoeger <choeger@open-xchange.com>
Fifth release candidate for 7.4.0
* Tue Aug 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Fourth release candidate for 7.4.0
* Tue Aug 06 2013 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 7.4.0
* Mon Aug 05 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-08-09
* Fri Aug 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 7.4.0
* Wed Jul 17 2013 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 7.4.0
* Tue Jul 16 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.4.0
* Mon Jul 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Second build for patch  2013-07-18
* Mon Jul 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-07-18
* Fri Jul 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-07-18
* Fri Jul 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-07-18
* Thu Jul 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-07-10
* Wed Jul 03 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-06-27
* Mon Jul 01 2013 Carsten Hoeger <choeger@open-xchange.com>
Third candidate for 7.2.2 release
* Fri Jun 28 2013 Carsten Hoeger <choeger@open-xchange.com>
Second candidate for 7.2.2 release
* Wed Jun 26 2013 Carsten Hoeger <choeger@open-xchange.com>
Release candidate for 7.2.2 release
* Fri Jun 21 2013 Carsten Hoeger <choeger@open-xchange.com>
Second feature freeze for 7.2.2 release
* Mon Jun 17 2013 Carsten Hoeger <choeger@open-xchange.com>
Feature freeze for 7.2.2 release
* Tue Jun 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-06-13
* Mon Jun 10 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-06-11
* Fri Jun 07 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-06-20
* Mon Jun 03 2013 Carsten Hoeger <choeger@open-xchange.com>
First sprint increment for 7.2.2 release
* Wed May 29 2013 Carsten Hoeger <choeger@open-xchange.com>
First candidate for 7.2.2 release
* Tue May 28 2013 Carsten Hoeger <choeger@open-xchange.com>
Second build for patch 2013-05-28
* Mon May 27 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.2.2
* Thu May 23 2013 Carsten Hoeger <choeger@open-xchange.com>
Third candidate for 7.2.1 release
* Wed May 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-22
* Wed May 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-22
* Wed May 22 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-22
* Wed May 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Second candidate for 7.2.1 release
* Wed May 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-10
* Mon May 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-05-09
* Fri May 03 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-23
* Tue Apr 30 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-17
* Mon Apr 22 2013 Carsten Hoeger <choeger@open-xchange.com>
First candidate for 7.2.1 release
* Mon Apr 15 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.4.0
* Mon Apr 15 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.2.1
* Fri Apr 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-12
* Wed Apr 10 2013 Carsten Hoeger <choeger@open-xchange.com>
Fourth candidate for 7.2.0 release
* Tue Apr 09 2013 Carsten Hoeger <choeger@open-xchange.com>
Third candidate for 7.2.0 release
* Tue Apr 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Second candidate for 7.2.0 release
* Tue Apr 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-04-04
* Tue Mar 26 2013 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 7.2.0
* Fri Mar 15 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.2.0
* Tue Mar 12 2013 Carsten Hoeger <choeger@open-xchange.com>
Sixth release candidate for 6.22.2/7.0.2
* Mon Mar 11 2013 Carsten Hoeger <choeger@open-xchange.com>
Fifth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Carsten Hoeger <choeger@open-xchange.com>
Fourth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 6.22.2/7.0.2
* Thu Mar 07 2013 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 6.22.2/7.0.2
* Mon Mar 04 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-03-07
* Mon Mar 04 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-03-08
* Fri Mar 01 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-03-07
* Wed Feb 27 2013 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 6.22.2/7.0.2
* Tue Feb 26 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-02-22
* Mon Feb 25 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-02-22
* Tue Feb 19 2013 Carsten Hoeger <choeger@open-xchange.com>
Fourth release candidate for 7.0.1
* Tue Feb 19 2013 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 7.0.1
* Tue Feb 19 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.0.2 release
* Fri Feb 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-02-13
* Thu Feb 14 2013 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 7.0.1
* Fri Feb 01 2013 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 7.0.1
* Tue Jan 29 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-01-28
* Mon Jan 21 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-01-24
* Tue Jan 15 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2013-01-23
* Thu Jan 10 2013 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.0.1
* Thu Jan 03 2013 Carsten Hoeger <choeger@open-xchange.com>
Build for public patch 2013-01-15
* Fri Dec 28 2012 Carsten Hoeger <choeger@open-xchange.com>
Build for public patch 2012-12-31
* Fri Dec 21 2012 Carsten Hoeger <choeger@open-xchange.com>
Build for public patch 2012-12-21
* Tue Dec 18 2012 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 7.0.0
* Mon Dec 17 2012 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 7.0.0
* Wed Dec 12 2012 Carsten Hoeger <choeger@open-xchange.com>
Build for public patch 2012-12-04
* Tue Dec 04 2012 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 7.0.0
* Tue Dec 04 2012 Carsten Hoeger <choeger@open-xchange.com>
prepare for 7.0.0 release
* Mon Nov 26 2012 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2012-11-28
* Wed Nov 14 2012 Carsten Hoeger <choeger@open-xchange.com>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Carsten Hoeger <choeger@open-xchange.com>
Fifth release candidate for 6.22.1
* Tue Nov 13 2012 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for EDP drop #6
* Tue Nov 06 2012 Carsten Hoeger <choeger@open-xchange.com>
Fourth release candidate for 6.22.1
* Fri Nov 02 2012 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 6.22.1
* Fri Oct 26 2012 Carsten Hoeger <choeger@open-xchange.com>
Third release build for EDP drop #5
* Fri Oct 26 2012 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 6.22.1
* Fri Oct 26 2012 Carsten Hoeger <choeger@open-xchange.com>
Second release build for EDP drop #5
* Fri Oct 26 2012 Carsten Hoeger <choeger@open-xchange.com>
prepare for 6.22.1
* Thu Oct 11 2012 Carsten Hoeger <choeger@open-xchange.com>
Release build for EDP drop #5
* Wed Oct 10 2012 Carsten Hoeger <choeger@open-xchange.com>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Carsten Hoeger <choeger@open-xchange.com>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 6.22.0
* Tue Sep 04 2012 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Carsten Hoeger <choeger@open-xchange.com>
prepare for next EDP drop
* Tue Aug 21 2012 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Carsten Hoeger <choeger@open-xchange.com>
prepare for 6.22.0
* Tue Jul 03 2012 Carsten Hoeger <choeger@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Carsten Hoeger <choeger@open-xchange.com>
Release build for EDP drop #2
* Tue May 22 2012 Carsten Hoeger <choeger@open-xchange.com>
Internal release build for EDP drop #2
* Mon May 14 2012 Carsten Hoeger <choeger@open-xchange.com>
prepare for 6.21.0
