
Name:           open-xchange-parallels
BuildArch:      noarch
#!BuildIgnore:  post-build-checks
BuildRequires:  ant
BuildRequires:  ant-nodeps
BuildRequires:  open-xchange-admin
BuildRequires:  java-devel >= 1.6.0
Version:	@OXVERSION@
%define		ox_release 4
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        Extensions for integration with Parallels
Requires:       open-xchange-admin-soap >= @OXVERSION@
Requires:       open-xchange-spamhandler
Provides:       open-xchange-authentication
Provides:       open-xchange-custom-parallels = %{version}
Obsoletes:      open-xchange-custom-parallels <= %{version}
Conflicts:      open-xchange-authentication-database open-xchange-authentication-ldap open-xchange-authentication-imap open-xchange-authentication-kerberos

%description
This package contains the authentication bundle and a bundle for branding. The
spam handler is installed with a separate package.

Authors:
--------
    Open-Xchange

%prep

%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -f build/build.xml clean build

%post
. /opt/open-xchange/lib/oxfunctions.sh

ox_move_config_file /opt/open-xchange/etc/groupware /opt/open-xchange/etc parallels.properties
ox_move_config_file /opt/open-xchange/etc/groupware /opt/open-xchange/etc settings/parallels_gui.properties settings/parallels-ui.properties

# SoftwareChange_Request-1207
pfile=/opt/open-xchange/etc/parallels.properties
if ox_exists_property com.openexchange.custom.parallels.branding.fallbackurl $pfile; then
    oval=$(ox_read_property com.openexchange.custom.parallels.branding.fallbackurl $pfile)
    ox_remove_property com.openexchange.custom.parallels.branding.fallbackurl $pfile
    ox_set_property com.openexchange.custom.parallels.branding.fallbackhost "$oval" $pfile
fi

# SoftwareChange_Request-1209
pfile=/opt/open-xchange/etc/settings/parallels-ui.properties
if ! ox_exists_property ui/parallels/use_parallels_antispam_features $pfile; then
    ox_set_property ui/parallels/use_parallels_antispam_features true $pfile
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
%config(noreplace) /opt/open-xchange/etc/*.properties
%dir /opt/open-xchange/etc/settings/
%config(noreplace) /opt/open-xchange/etc/settings/parallels-ui.properties
%doc com.openexchange.parallels/ChangeLog

%changelog
* Tue Jan 29 2013 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2013-01-28
* Fri Dec 28 2012 Marcus Klein <marcus.klein@open-xchange.com>
Build for public patch 2012-12-31
* Fri Dec 21 2012 Marcus Klein <marcus.klein@open-xchange.com>
Build for public patch 2012-12-21
* Tue Dec 18 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 7.0.0
* Mon Dec 17 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 7.0.0
* Wed Dec 12 2012 Marcus Klein <marcus.klein@open-xchange.com>
Build for public patch 2012-12-04
* Tue Dec 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 7.0.0
* Tue Dec 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 7.0.0 release
* Mon Nov 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Build for patch 2012-11-28
* Wed Nov 14 2012 Marcus Klein <marcus.klein@open-xchange.com>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.1
* Tue Nov 13 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for EDP drop #6
* Tue Nov 06 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.1
* Fri Nov 02 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release build for EDP drop #5
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.1
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.1
* Thu Oct 11 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #5
* Tue Sep 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for next EDP drop
* Tue Aug 21 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.0
* Wed Jul 11 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
