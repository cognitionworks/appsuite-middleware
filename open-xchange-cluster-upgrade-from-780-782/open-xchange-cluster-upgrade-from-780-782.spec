%define __jar_repack %{nil}

Name:           open-xchange-cluster-upgrade-from-780-782
BuildArch:      noarch
BuildRequires: ant
BuildRequires:  open-xchange-core
BuildRequires: java-1.8.0-openjdk-devel
Version:        @OXVERSION@
%define         ox_release 5
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        Server module to invalidate cluster nodes running v7.8.0 to v7.8.2 of the Open-Xchange server (Hazelcast v3.5.5) during upgrade
Autoreqprov:    no
Requires:       open-xchange-core >= @OXVERSION@

%description
Server module to invalidate cluster nodes running v7.8.0 to v7.8.2 of the Open-Xchange server (Hazelcast v3.5.5) during upgrade

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
* Fri Feb 05 2021 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.10.5 release
* Mon Feb 01 2021 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.10.5 release
* Fri Jan 15 2021 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.5 release
* Thu Dec 17 2020 Thorben Betten <thorben.betten@open-xchange.com>
Second preview of 7.10.5 release
* Fri Nov 27 2020 Thorben Betten <thorben.betten@open-xchange.com>
First preview of 7.10.5 release
* Tue Oct 06 2020 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.5 release
* Wed Aug 05 2020 Thorben Betten <thorben.betten@open-xchange.com>
Fifth candidate for 7.10.4 release
* Tue Aug 04 2020 Thorben Betten <thorben.betten@open-xchange.com>
Fourth candidate for 7.10.4 release
* Tue Aug 04 2020 Thorben Betten <thorben.betten@open-xchange.com>
Third candidate for 7.10.4 release
* Fri Jul 31 2020 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.10.4 release
* Tue Jul 28 2020 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.4 release
* Tue Jun 30 2020 Thorben Betten <thorben.betten@open-xchange.com>
Second preview of 7.10.4 release
* Wed May 20 2020 Thorben Betten <thorben.betten@open-xchange.com>
First preview of 7.10.4 release
* Thu Jan 16 2020 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.4 release
* Thu Nov 28 2019 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.10.3 release
* Thu Nov 21 2019 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.3 release
* Thu Oct 17 2019 Thorben Betten <thorben.betten@open-xchange.com>
First preview for 7.10.3 release
* Mon Jun 17 2019 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.3 release
* Fri May 10 2019 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.10.2 release
* Fri May 10 2019 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.10.2 release
* Tue Apr 30 2019 Thorben Betten <thorben.betten@open-xchange.com>
Second preview for 7.10.2 release
* Thu Mar 28 2019 Thorben Betten <thorben.betten@open-xchange.com>
First preview for 7.10.2 release
* Thu Oct 18 2018 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.10.2 release
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
* Wed Aug 17 2016 Thorben Betten <thorben.betten@open-xchange.com>
Initial release
