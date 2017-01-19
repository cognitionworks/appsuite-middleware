%define __jar_repack %{nil}

Name:           open-xchange-file-storage-mail
BuildArch:      noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires:  ant
%else
BuildRequires:  ant-nodeps
%endif
BuildRequires:  open-xchange-core
BuildRequires:  open-xchange-imap
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:        @OXVERSION@
%define         ox_release 22
Release:        %{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:            http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        The Open Xchange backend Virtual Mail Attachment file storage extension
Autoreqprov:   no
Requires:       open-xchange-core >= @OXVERSION@
Requires:       open-xchange-imap >= @OXVERSION@

%description
Adds a file storage service for the Virtual Mail Attachments to the backend installation.

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
if [ ${1:-0} -eq 2 ]; then
    # only when updating
    . /opt/open-xchange/lib/oxfunctions.sh

    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'

    PFILE=/opt/open-xchange/etc/filestorage-maildrive.properties

    # SoftwareChange_Request-3470
    ox_add_property com.openexchange.file.storage.mail.maxAccessesPerUser 4 $PFILE
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/etc/
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/filestorage-maildrive.properties
%config(noreplace) /opt/open-xchange/etc/*

%changelog
* Thu Jan 19 2017 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2017-01-23 (3878)
* Fri Jan 06 2017 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2017-01-18 (3867)
* Wed Jan 04 2017 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2017-01-09 (3848)
* Wed Dec 14 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-19 (3813)
* Tue Dec 13 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-12 (3817)
* Mon Dec 05 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-12-05 (3763)
* Sat Nov 12 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-11-21 (3731)
* Tue Nov 08 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-11-07 (3678)
* Wed Oct 26 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-09-08 (3699)
* Mon Oct 17 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-10-24 (3630)
* Thu Oct 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-10-10 (3597)
* Mon Sep 19 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-09-26 (3572)
* Mon Sep 19 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-09-08 (3580)
* Mon Sep 05 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-09-12 (3547)
* Mon Aug 22 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-08-29 (3522)
* Mon Aug 15 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-08-26 (3512)
* Mon Aug 08 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-08-15 (3490)
* Fri Jul 22 2016 Thorben Betten <thorben.betten@open-xchange.com>
Build for patch 2016-08-01 (3467)
* Tue Jul 12 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.2 release
* Wed Jul 06 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.2 release
* Wed Jun 29 2016 Thorben Betten <thorben.betten@open-xchange.com>
Second candidate for 7.8.2 release
* Thu Jun 16 2016 Thorben Betten <thorben.betten@open-xchange.com>
First candidate for 7.8.2 release
* Fri Jun 03 2016 Thorben Betten <thorben.betten@open-xchange.com>
prepare for 7.8.2
