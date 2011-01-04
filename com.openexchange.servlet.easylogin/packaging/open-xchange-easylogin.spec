
# norootforbuild

Name:           open-xchange-easylogin
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-common open-xchange-global open-xchange-server
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
%define		ox_release 0
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The Open-Xchange Easylogin Servlet
Requires:       open-xchange-common open-xchange-global open-xchange-server
#

%description
The Open-Xchange Easylogin Servlet

Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

ant -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install

%clean
%{__rm} -rf %{buildroot}

%post

if [ ${1:-0} -eq 2 ]; then
   # only when updating
   . /opt/open-xchange/etc/oxfunctions.sh

   # prevent bash from expanding, see bug 13316
   GLOBIGNORE='*'

   # SoftwareChange_Request-548
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/easylogin.properties
   if ox_exists_property com.openexchange.easylogin.OX_PATH_RELATIVE $pfile; then
      ox_remove_property com.openexchange.easylogin.OX_PATH_RELATIVE $pfile
   fi

   # SoftwareChange_Request-409
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/easylogin.properties
   if ! ox_exists_property com.openexchange.easylogin.autologinPara $pfile; then
       ox_set_property com.openexchange.easylogin.autologinPara "autologin" $pfile
   fi
   if ! ox_exists_property com.openexchange.easylogin.autologin.default $pfile; then
       ox_set_property com.openexchange.easylogin.autologin.default false $pfile
   fi
   if ! ox_exists_property com.openexchange.easylogin.defaultClient $pfile; then
       ox_set_property com.openexchange.easylogin.defaultClient "com.openexchange.ox.gui.dhtml" $pfile
   fi

   # SoftwareChange_Request-189
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/easylogin.properties
   if ! ox_exists_property com.openexchange.easylogin.allowInsecureTransmission $pfile; then
       ox_set_property com.openexchange.easylogin.allowInsecureTransmission false $pfile
   fi

fi

%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/groupware
%dir /opt/open-xchange/bundles
%dir /opt/open-xchange/etc/groupware/osgi/bundle.d
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/groupware/osgi/bundle.d/*
/opt/open-xchange/etc/groupware/*.properties

%changelog
* Mon Dec 14 2009 - choeger@open-xchange.com
  - added more logging
  - disable caching in proxies
  - do not allow insecure transmissions (must use https now)
* Mon Oct 12 2009 - manuel.kraft@open-xchange.com
  - Bugfix #14657 for checking if referrer already contains "?"
* Fri Jul 10 2009 - dennis.sieben@open-xchange.com
  - Bugfix #14116 Easylogin throws exception on server shutdown
    - Added null check in shutdown code
