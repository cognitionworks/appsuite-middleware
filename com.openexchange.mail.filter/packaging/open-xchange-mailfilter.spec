
# norootforbuild

Name:           open-xchange-mailfilter
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange-configread >= @OXVERSION@ open-xchange-server >= @OXVERSION@
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
%define		ox_release 2
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The Open-Xchange Server Mailfilter Bundle
Requires:       open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange-configread >= @OXVERSION@ open-xchange-server >= @OXVERSION@
#

%description
The Open-Xchange Server Mailfilter Bundle

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

   # SoftwareChange_Request-191
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/mailfilter.properties
   if ! ox_exists_property NON_RFC_COMPLIANT_TLS_REGEX $pfile; then
      ox_set_property NON_RFC_COMPLIANT_TLS_REGEX '^Cyrus.*v([0-1]\\.[0-9].*|2\\.[0-2].*|2\\.3\\.[0-9]|2\\.3\\.[0-9][^0-9].*)$' $pfile
   fi
   if ! ox_exists_property TLS $pfile; then
      ox_set_property TLS "true" $pfile
   fi

   # SoftwareChange_Request-142
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/mailfilter.properties
   if ! ox_exists_property SIEVE_AUTH_ENC $pfile; then
       ox_set_property SIEVE_AUTH_ENC "UTF-8" $pfile
   fi

fi

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles
%dir /opt/open-xchange/etc/groupware/osgi/bundle.d
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/groupware/osgi/bundle.d/*
%config(noreplace) /opt/open-xchange/etc/groupware/mailfilter.properties
%changelog
* Wed Dec 16 2009 - dennis.sieben@open-xchange.com
  - Added ability to disable TLS and define the regex for non-correct working
    TLS implementations in the config file
* Wed Dec 02 2009 - dennis.sieben@open-xchange.com
  - Bugfix #14655: [L3] Sieve capability wrong if TLS is used
    - Fixed regex once again to include all Cyrus versions including 2.3.9 to 
      the implementations which aren't working correct 
