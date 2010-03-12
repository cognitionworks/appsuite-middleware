
# norootforbuild

Name:           open-xchange-admin-plugin-autocontextid
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-admin-plugin-hosting-lib >= @OXVERSION@ open-xchange-admin >= @OXVERSION@
%if 0%{?suse_version} && 0%{?sles_version} < 11
%if %{?suse_version} <= 1010
# SLES10
BuildRequires:  java-1_5_0-ibm >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-devel >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-alsa >= 1.5.0_sr9
BuildRequires:  update-alternatives
%endif
%if %{?suse_version} >= 1100
BuildRequires:  java-sdk-openjdk
%endif
%if %{?suse_version} > 1010 && %{?suse_version} < 1100
BuildRequires:  java-sdk-1.5.0-sun
%endif
%endif
%if 0%{?sles_version} >= 11
# SLES11 or higher
BuildRequires:  java-1_6_0-ibm-devel
%endif

%if 0%{?rhel_version}
# libgcj seems to be installed whether we want or not and libgcj needs cairo
BuildRequires:  java-sdk-1.5.0-sun cairo
%endif
%if 0%{?fedora_version}
%if %{?fedora_version} > 8
BuildRequires:  java-1.6.0-openjdk-devel saxon
%endif
%if %{?fedora_version} <= 8
BuildRequires:  java-devel-icedtea saxon
%endif
%endif
Version:	@OXVERSION@
%define		ox_release 4
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The Open Xchange Admin Auto Context ID plugin
Requires:       open-xchange-admin-client >= @OXVERSION@
Requires:       open-xchange-admin-plugin-hosting >= @OXVERSION@
#

%package -n     open-xchange-admin-plugin-autocontextid-client
Group:          Applications/Productivity
Summary:        The Open Xchange Admin Auto Context ID plugin client library
Requires:       open-xchange-admin-client >= @OXVERSION@ open-xchange-admin-plugin-hosting-client >= @OXVERSION@

%description -n open-xchange-admin-plugin-autocontextid-client
The Open Xchange Admin Auto Context ID plugin client library

Authors:
--------
    Open-Xchange

%description
Open Xchange Admin Auto Context ID Plugin

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

%define adminbundle	com.openexchange.admin.jar
%define oxprefix	/opt/open-xchange
%define adminhostingbundle open_xchange_admin_plugin_hosting.jar

ant -Dadmin.classpath=%{oxprefix}/bundles/%{adminbundle} \
    -Ddestdir=%{buildroot} -Dprefix=%{oxprefix} \
    -Dadminhosting.classpath=%{oxprefix}/bundles/%{adminhostingbundle} \
    install install-client

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles
%dir /opt/open-xchange/etc/admindaemon/osgi/bundle.d
%dir /opt/open-xchange/etc/admindaemon/mysql
%dir /opt/open-xchange/etc/admindaemon/plugin
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/admindaemon/osgi/bundle.d/*
/opt/open-xchange/etc/admindaemon/mysql/*
%config(noreplace) /opt/open-xchange/etc/admindaemon/plugin/*

%files -n open-xchange-admin-plugin-autocontextid-client
%defattr(-,root,root)
%dir /opt/open-xchange/lib
/opt/open-xchange/lib/*

%changelog
