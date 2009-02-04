
# norootforbuild

Name:           open-xchange-configjump-oxee
Provides:	open-xchange-configjump
BuildArch:	noarch
BuildRequires:  ant open-xchange-common open-xchange-global open-xchange-configread
%if 0%{?suse_version}
%if 0%{?suse_version} <= 1010
# SLES10
BuildRequires:  java-1_5_0-ibm >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-devel >= 1.5.0_sr9
BuildRequires:  java-1_5_0-ibm-alsa >= 1.5.0_sr9
BuildRequires:  update-alternatives
%else
BuildRequires:  java-sdk-1.5.0-sun
%endif
%endif
%if 0%{?rhel_version}
# libgcj seems to be installed whether we want or not and libgcj needs cairo
BuildRequires:  java-sdk-1.5.0-sun cairo
%endif
%if 0%{?fedora_version}
BuildRequires:  java-devel-icedtea
%endif
Version:        6.5.0
Release:        1
Group:          Applications/Productivity
License:        GNU General Public License (GPL)
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
#URL:            
Source:         %{name}_%{version}.orig.tar.gz
Summary:        The Open-Xchange ConfigJump for OX EE
Requires:       open-xchange-common open-xchange-global open-xchange-configread
#

%description
The Open-Xchange ConfigJump for OX EE

Authors:
--------
    Open Xchange

%prep
%setup -q

%build


%install

ant -Dlib.dir=/opt/open-xchange/lib -Ddestdir=%{buildroot} -Dprefix=/opt/open-xchange install

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
%dir /opt/open-xchange/etc/groupware/osgi/bundle.d/
%dir /opt/open-xchange/etc/groupware
/opt/open-xchange/bundles/*
/opt/open-xchange/etc/groupware/osgi/bundle.d/*
%config(noreplace) /opt/open-xchange/etc/groupware/*
