
Name:          open-xchange-admin-user-copy
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: open-xchange-admin
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 2
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Extension to copy users into other contexts
Requires:      open-xchange-core >= @OXVERSION@
Requires:      open-xchange-admin >= @OXVERSION@
Provides:      open-xchange-admin-plugin-user-copy = %{version}
Obsoletes:     open-xchange-admin-plugin-user-copy <= %{version}
Provides:      open-xchange-admin-plugin-user-copy-client = %{version}
Obsoletes:     open-xchange-admin-plugin-user-copy-client <= %{version}
Provides:      open-xchange-user-copy = %{version}
Obsoletes:     open-xchange-user-copy <= %{version}

%description
This package installs administrative OSGi bundles that provide the extension to copy a user into another context. This is mainly used to
combine several users into the same context. To complete the move of a user, the user can be deleted in the source context after copying
it to the destination context.
This extension only copies all the private data of a user. All public information in a context does not belong to any user and therefore it is
not copied at all. The sharing information of the private data of a user needs to be removed.

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
%dir /opt/open-xchange/bundles
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/sbin
/opt/open-xchange/sbin/*
%dir /opt/open-xchange/lib
/opt/open-xchange/lib/*
%doc com.openexchange.admin.user.copy/ChangeLog

%changelog
* Thu Oct 11 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Release build for EDP drop #5
* Wed Oct 10 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Second release candidate for 6.22.0
* Tue Sep 04 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
prepare for next EDP drop
* Tue Aug 21 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
prepare for 6.22.0
* Tue Jul 03 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Release build for EDP drop #2
* Fri Jun 15 2012 Jan Bauerdick <jan.bauerdick@open-xchange.com>
Initial packaging
