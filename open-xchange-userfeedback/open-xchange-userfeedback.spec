%define __jar_repack %{nil}

Name:          open-xchange-userfeedback
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-core
BuildRequires: open-xchange-rest >= @OXVERSION@
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:       @OXVERSION@
%define        ox_release 14
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange user feedback service
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-rest >= @OXVERSION@

%description
This package provides user feedback bundles


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
%dir /opt/open-xchange/sbin/
/opt/open-xchange/sbin/*
%dir /opt/open-xchange/lib/
/opt/open-xchange/lib/com.openexchange.userfeedback.clt.jar

%changelog
* Tue Oct 10 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for Patch 2017-10-16 (4394)
* Wed Sep 27 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-10-02 (4377)
* Thu Sep 21 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-09-22 (4373)
* Tue Sep 12 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-09-18 (4354)
* Fri Sep 01 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-09-04 (4328)
* Mon Aug 14 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-08-21 (4318)
* Tue Aug 01 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-08-07 (4304)
* Mon Jul 17 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-07-24 (4285)
* Mon Jul 03 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-07-10 (4257)
* Wed Jun 21 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-06-26 (4233)
* Tue Jun 06 2017 Martin Schneider <martin.schneider@open-xchange.com>
Build for patch 2017-06-08 (4180)
* Fri May 19 2017 Martin Schneider <martin.schneider@open-xchange.com>
First candidate for 7.8.4 release
* Thu May 04 2017 Martin Schneider <martin.schneider@open-xchange.com>
Second preview of 7.8.4 release
* Mon Apr 03 2017 Martin Schneider <martin.schneider@open-xchange.com>
First preview of 7.8.4 release
* Mon Feb 08 2016 Martin Schneider <martin.schneider@open-xchange.com>
Initial release
