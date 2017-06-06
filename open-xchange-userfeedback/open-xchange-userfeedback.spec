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
%define        ox_release 4
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
