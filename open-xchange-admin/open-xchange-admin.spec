
Name:          open-xchange-admin
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 2
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange backend administration extension
Requires:      open-xchange-core >= @OXVERSION@
Provides:      open-xchange-admin-plugin-hosting = %{version}
Obsoletes:     open-xchange-admin-plugin-hosting <= %{version}
Provides:      open-xchange-admin-lib = %{version}
Obsoletes:     open-xchange-admin-lib <= %{version}
Provides:      open-xchange-admin-plugin-hosting-client = %{version}
Obsoletes:     open-xchange-admin-plugin-hosting-client <= %{version}
Provides:      open-xchange-admin-plugin-hosting-doc = %{version}
Obsoletes:     open-xchange-admin-plugin-hosting-doc <= %{version}
Provides:      open-xchange-admin-client = %{version}
Obsoletes:     open-xchange-admin-client <= %{version}
Provides:      open-xchange-admin-plugin-hosting-lib = %{version}
Obsoletes:     open-xchange-admin-plugin-hosting-lib <= %{version}
Provides:      open-xchange-admin-doc = %{version}
Obsoletes:     open-xchange-admin-doc <= %{version}
%if 0%{?suse_version}
Requires:      mysql-client >= 5.0.0
%endif
%if 0%{?fedora_version} || 0%{?rhel_version}
Requires:      mysql >= 5.0.0
%endif

%description
This package installs the OSGi bundles to the backend that provide the RMI interface to administer the installation. This package contains
the RMI interfaces for the overall administrative tasks like registering, changing and deleting servers, databases and filestores. It also
contains the interfaces for creating, changing and deleting contexts, users, groups and resources.

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

    ##
    ## start update from < 6.21
    ##
    CONFFILES="AdminDaemon.properties Group.properties ModuleAccessDefinitions.properties RMI.properties Resource.properties Sql.properties mpasswd plugin/hosting.properties"
    for FILE in ${CONFFILES}; do
	ox_move_config_file /opt/open-xchange/etc/admindaemon /opt/open-xchange/etc $FILE
    done
    ox_move_config_file /opt/open-xchange/etc/admindaemon /opt/open-xchange/etc User.properties AdminUser.properties

    ofile=/opt/open-xchange/etc/admindaemon/ox-admin-scriptconf.sh
    pfile=/opt/open-xchange/etc/ox-scriptconf.sh
    if [ -e $ofile ]; then
        oval=$(ox_read_property JAVA_OXCMD_OPTS $ofile)
        if [ -n "$oval" ]; then
           ox_set_property JAVA_OXCMD_OPTS "$oval" $pfile
        else
           ox_set_property JAVA_OXCMD_OPTS "-Djava.net.preferIPv4Stack=true" $pfile
        fi
        rm -f $ofile
    fi

    # SoftwareChange_Request-1118
    # -----------------------------------------------------------------------
    pfile=/opt/open-xchange/etc/AdminDaemon.properties
    ox_remove_property TOOL_STORAGE $pfile

    ofile=/opt/open-xchange/etc/AdminDaemon.properties
    pfile=/opt/open-xchange/etc/rmi.properties
    if ox_exists_property BIND_ADDRESS $ofile; then
	oval=$(ox_read_property BIND_ADDRESS $ofile)
	if [ -n "$oval" ]; then
	   ox_set_property com.openexchange.rmi.host $oval $pfile
	fi
	ox_remove_property BIND_ADDRESS $ofile
    fi

    ofile=/opt/open-xchange/etc/RMI.properties
    if [ -e $ofile ]; then
	oval=$(ox_read_property RMI_PORT $ofile)
	if [ -n "$oval" ]; then
	   ox_set_property com.openexchange.rmi.port $oval $pfile
	fi
	rm -f $ofile
    fi

    # SoftwareChange_Request-1091
    # -----------------------------------------------------------------------
    pfile=/opt/open-xchange/etc/AdminUser.properties
    ox_remove_property CREATE_HOMEDIRECTORY $pfile
    ox_remove_property HOME_DIR_ROOT $pfile
    pfile=/opt/open-xchange/etc/AdminDaemon.properties
    ox_remove_property USER_PROP $pfile
    ox_remove_property GROUP_PROP $pfile
    ox_remove_property RESOURCE_PROP $pfile
    ox_remove_property RMI_PROP $pfile
    ox_remove_property SQL_PROP $pfile
    ox_remove_property MASTER_AUTH_FILE $pfile
    ox_remove_property ACCESS_COMBINATIONS_FILE $pfile

    # SoftwareChange_Request-1100
    # -----------------------------------------------------------------------
    pfile=/opt/open-xchange/etc/AdminDaemon.properties
    ox_remove_property SERVER_NAME $pfile
    ##
    ## end update from < 6.21
    ##

    ox_update_permissions "/opt/open-xchange/etc/mpasswd" root:open-xchange 640
fi

%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/etc/
/opt/open-xchange/etc/mysql
%dir /opt/open-xchange/etc/plugin
%config(noreplace) /opt/open-xchange/etc/plugin/*
%config(noreplace) /opt/open-xchange/etc/*.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/mpasswd
%dir /opt/open-xchange/lib/
/opt/open-xchange/lib/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir /opt/open-xchange/sbin/
/opt/open-xchange/sbin/*
%doc com.openexchange.admin.rmi/javadoc
%doc com.openexchange.admin/ChangeLog

%changelog
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
* Thu Oct 11 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #5
* Wed Oct 10 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 6.22.0
* Tue Sep 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for next EDP drop
* Tue Aug 21 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.0
* Tue Jul 03 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Tue May 22 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #2
* Mon Apr 16 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #1
* Wed Apr 04 2012 Marcus Klein <marcus.klein@open-xchange.com>
Internal release build for EDP drop #0
* Thu Mar 01 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
