
# norootforbuild

Name:           open-xchange-axis2
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-common >= @OXVERSION@ open-xchange-server >= @OXVERSION@ perl
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
BuildRequires:  java-sdk-sun cairo
%endif
%if 0%{?fedora_version}
%if %{?fedora_version} > 8
BuildRequires:  java-1.6.0-openjdk-devel saxon
%endif
%if %{?fedora_version} <= 8
BuildRequires:  java-devel-icedtea saxon
%endif
%endif
%if 0%{?centos_version}
BuildRequires:  java-1.6.0-openjdk-devel
%endif
Version:	@OXVERSION@
%define		ox_release 7
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The Open-Xchange Axis2 Bundle
Requires:       open-xchange-server >= @OXVERSION@
#

%description
The Open-Xchange Axis2 Bundle

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

ant -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install doc

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles
%dir /opt/open-xchange/etc/groupware/osgi/bundle.d
%dir /opt/open-xchange/etc/groupware
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/groupware/osgi/bundle.d/*
/opt/open-xchange/bundles/com.openexchange.axis2/lib
/opt/open-xchange/bundles/com.openexchange.axis2/modules
%dir /opt/open-xchange/bundles/com.openexchange.axis2/services
/opt/open-xchange/bundles/com.openexchange.axis2/META-INF
/opt/open-xchange/bundles/com.openexchange.axis2/com.openexchange.axis2.jar
%config(noreplace) /opt/open-xchange/etc/groupware/axis2.properties
%doc docs
%changelog
* Tue Sep 28 2010 - choeger@open-xchange.com
 - Bugfix #17048: Provisioning via SOAP broken
* Mon Jul 27 2009 - marcus.klein@open-xchange.com
 - Bugfix #14213: Setting configuration file permissions to reduce readability to OX processes.
* Thu Nov 13 2008 - dennis.sieben@open-xchange.com
 - Bugfix #12526: Replaced axis jar by a new version which fixes this issue
