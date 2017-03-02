%define __jar_repack %{nil}

Name:          open-xchange-mail-categories
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
BuildRequires: open-xchange-core >= @OXVERSION@
BuildRequires: open-xchange-mailfilter >= @OXVERSION@
Version:       @OXVERSION@
%define        ox_release 17
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Open Xchange Mail Categories Plugin
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-mailfilter >= @OXVERSION@

%description
This package offers the possibility to manage system and user categories for mails.

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
%dir /opt/open-xchange/etc/
%config(noreplace) /opt/open-xchange/etc/mail-categories.properties

%changelog
* Thu Mar 02 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-03-06 (3985)
* Fri Feb 24 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-02-24 (3994)
* Wed Feb 22 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-02-22 (3969)
* Tue Feb 14 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-02-20 (3952)
* Tue Jan 31 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-02-06 (3918)
* Thu Jan 26 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-01-26 (3925)
* Wed Jan 18 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-01-23 (3879)
* Wed Jan 04 2017 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2017-01-09 (3849)
* Tue Dec 20 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2016-12-23 (3857)
* Wed Dec 14 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2016-12-19 (3814)
* Tue Dec 13 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2016-12-14 (3806)
* Tue Dec 06 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Build for patch 2016-12-12 (3775)
* Fri Nov 25 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Second release candidate for 7.8.3 release
* Thu Nov 24 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
First release candidate for 7.8.3 release
* Tue Nov 15 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Third preview for 7.8.3 release
* Sat Oct 29 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Second preview for 7.8.3 release
* Fri Oct 14 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
First preview 7.8.3 release
* Tue Sep 06 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
prepare for 7.8.3 release
* Tue Jul 12 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Second candidate for 7.8.2 release
* Wed Jul 06 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
First candidate for 7.8.2 release
* Wed Jun 29 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Second candidate for 7.8.2 release
* Thu Jun 16 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
First candidate for 7.8.2 release
* Wed Apr 06 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
prepare for 7.8.2 release
* Tue Mar 15 2016 Kevin Ruthmann <kevin.ruthmann@open-xchange.com>
Initial release
