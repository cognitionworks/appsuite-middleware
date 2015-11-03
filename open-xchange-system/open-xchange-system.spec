
Name:          open-xchange-system
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 33
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       system integration specific infrastructure
Autoreqprov:   no
PreReq:        /usr/sbin/useradd

%description
system integration specific infrastructure

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
mkdir -p %{buildroot}/opt/open-xchange/lib

ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml clean build

%post

%clean
%{__rm} -rf %{buildroot}

%pre
/usr/sbin/groupadd -r open-xchange 2> /dev/null || :
/usr/sbin/useradd -r -g open-xchange -r -s /bin/false -c "open-xchange system user" -d /opt/open-xchange open-xchange 2> /dev/null || :

%files
%defattr(-,root,root)
/opt/open-xchange/lib/oxfunctions.sh

%changelog
* Mon Oct 26 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-10-30 (2825)
* Mon Oct 12 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-10-23 (2806)
* Thu Aug 06 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-08-17 (2666)
* Wed Jun 24 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-06-26 (2573)
* Wed Jun 10 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-06-08 (2539)
* Thu Apr 30 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-05-04 (2497)
* Tue Apr 28 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-05-04 (2505)
* Tue Apr 14 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-04-13 (2473)
* Thu Mar 26 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-03-30 (2459)
* Mon Mar 23 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-03-20
* Tue Mar 17 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-03-18
* Fri Mar 06 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-03-16
* Thu Feb 26 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-02-23
* Mon Feb 23 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-02-25
* Fri Feb 06 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-02-10
* Fri Feb 06 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-02-09
* Mon Jan 26 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-10-27
* Mon Jan 26 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-01-26
* Mon Jan 12 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-01-09
* Wed Jan 07 2015 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2015-01-12
* Mon Dec 08 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-12-15
* Tue Dec 02 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-12-03
* Tue Nov 25 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-12-01
* Mon Nov 24 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-12-01
* Tue Nov 18 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-11-20
* Mon Nov 10 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-11-17
* Mon Oct 27 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-10-30
* Fri Oct 17 2014 Carsten Hoeger <choeger@open-xchange.com>
Build for patch 2014-10-24
* Tue Oct 14 2014 Carsten Hoeger <choeger@open-xchange.com>
Fifth candidate for 7.6.1 release
* Fri Oct 10 2014 Carsten Hoeger <choeger@open-xchange.com>
Fourth candidate for 7.6.1 release
* Thu Oct 02 2014 Carsten Hoeger <choeger@open-xchange.com>
Third release candidate for 7.6.1
* Tue Sep 16 2014 Carsten Hoeger <choeger@open-xchange.com>
Second release candidate for 7.6.1
* Fri Sep 05 2014 Carsten Hoeger <choeger@open-xchange.com>
First release candidate for 7.6.1
* Fri Aug 29 2014 Carsten Hoeger <choeger@open-xchange.com>
Initial release
