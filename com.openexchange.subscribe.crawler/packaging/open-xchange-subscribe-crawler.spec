
# norootforbuild

Name:           open-xchange-subscribe-crawler
BuildArch:	noarch
#!BuildIgnore: post-build-checks
BuildRequires:  ant open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange-subscribe >= @OXVERSION@ open-xchange-server >= @OXVERSION@ open-xchange-genconf >= @OXVERSION@ open-xchange-xml >= @OXVERSION@ open-xchange-threadpool >= @OXVERSION@
%if 0%{?suse_version} && 0%{?sles_version} < 11
%if %{?suse_version} <= 1010
# SLES10
BuildRequires:  open-xchange-xerces-ibm
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
BuildRequires:  open-xchange-xerces-sun
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
Version:	@OXVERSION@
%define		ox_release 10
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        Subscriptions for OXMF feeds
Requires:       open-xchange-common >= @OXVERSION@ open-xchange-global >= @OXVERSION@ open-xchange-subscribe >= @OXVERSION@ open-xchange >= @OXVERSION@ open-xchange-genconf >= @OXVERSION@ open-xchange-xml >= @OXVERSION@ open-xchange-threadpool >= @OXVERSION@
Conflicts:   open-xchange-subscribe-linkedin < @OXVERSION@
Conflicts:   open-xchange-subscribe-xing < @OXVERSION@
Obsoletes:   open-xchange-subscribe-linkedin
Obsoletes:   open-xchange-subscribe-xing
%if 0%{?sles_version} >= 10
Requires:   open-xchange-xerces-ibm
Conflicts:  open-xchange-xerces-sun
%else
Requires:   open-xchange-xerces-sun
Conflicts:  open-xchange-xerces-ibm
%endif
%if 0%{?centos_version}
BuildRequires:  java-1.6.0-openjdk-devel
%endif
#

%description
Subscribe Crawler feeds
  
Authors:
--------
    Open-Xchange

%prep
%setup -q

%build


%install
export NO_BRP_CHECK_BYTECODE_VERSION=true

ant -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install

%post

if [ ${1:-0} -eq 2 ]; then
   . /opt/open-xchange/etc/oxfunctions.sh

   # prevent bash from expanding, see bug 13316
   GLOBIGNORE='*'

   # SoftwareChange_Request-335
   # -----------------------------------------------------------------------
   pfile=/opt/open-xchange/etc/groupware/crawler.properties
   if ! ox_exists_property com.openexchange.subscribe.crawler.msn.de $pfile; then
      ox_set_property com.openexchange.subscribe.crawler.msn.de "true" $pfile
   fi

   ox_update_permissions "/opt/open-xchange/etc/groupware/crawlers" open-xchange:open-xchange 755
   find /opt/open-xchange/etc/groupware/crawlers -name "*.yml" -print0 | while read -d $'\0' i; do
            ox_update_permissions "$i" open-xchange:open-xchange 644
   done
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/etc/groupware/
%dir /opt/open-xchange/bundles/
%dir /opt/open-xchange/sbin/
%dir /opt/open-xchange/etc/*/osgi/bundle.d/
%dir %attr(-,open-xchange,open-xchange) /opt/open-xchange/etc/groupware/crawlers
%config(noreplace) /opt/open-xchange/etc/groupware/*.properties
%config(noreplace) %attr(-,open-xchange,open-xchange) /opt/open-xchange/etc/groupware/crawlers/*
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/*/osgi/bundle.d/*
/opt/open-xchange/sbin/*
