
Name:          open-xchange-emig-mock
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-emig
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 3
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange EMiG Mock Bundle
Autoreqprov:   no
Requires:      open-xchange-emig >= @OXVERSION@

%description
The Open-Xchange EMiG Mock Bundle

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
%dir /opt/open-xchange/etc/
%config(noreplace) /opt/open-xchange/etc/emig-mock.properties
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*

%changelog
* Thu Jan 23 2014 Thorben Betten <thorben.betten@open-xchange.com>
Third release candidate for 7.4.2
* Fri Jan 10 2014 Thorben Betten <thorben.betten@open-xchange.com>
Second release candidate for 7.4.2
* Mon Dec 23 2013 Thorben Betten <thorben.betten@open-xchange.com>
First release candidate for 7.4.2
* Sun Dec 22 2013 Thorben Betten <thorben.betten@open-xchange.com>
Initial release
