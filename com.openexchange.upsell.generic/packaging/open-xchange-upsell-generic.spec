
# norootforbuild

Name:           open-xchange-upsell-generic
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-gui
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
Version:        @OXVERSION@
%define         ox_release 1
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The generic Open-Xchange upsell layer bundle
Requires:       open-xchange-gui
#

%description
The generic Open-Xchange upsell layer bundle

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

ant -Dlib.dir=/opt/open-xchange/lib -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/groupware/settings
%dir /var/www/ox6/plugins/com.openexchange.upsell.generic
%config(noreplace) /opt/open-xchange/etc/groupware/settings/upsell.properties
/var/www/ox6/plugins/com.openexchange.upsell.generic/*
%changelog
* Mon Oct 05 2009 - benjamin.otterbach@open-xchange.com
 - Enhancement: Changed default upsell text.
