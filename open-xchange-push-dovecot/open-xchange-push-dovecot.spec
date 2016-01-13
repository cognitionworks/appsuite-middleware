
Name:          open-xchange-push-dovecot
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-core
BuildRequires: open-xchange-imap
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:       @OXVERSION@
%define        ox_release 15
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Open-Xchange Dovecot Push Bundle
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@, open-xchange-imap >= @OXVERSION@

%description
Open-Xchange Mail Push Bundle

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
%config(noreplace) /opt/open-xchange/etc/*
%dir /opt/open-xchange/etc/hazelcast/
%config(noreplace) /opt/open-xchange/etc/hazelcast/*

%changelog
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
* Wed Jul 01 2015 Thorben Betten <thorben.betten@open-xchange.com>
Initial release
