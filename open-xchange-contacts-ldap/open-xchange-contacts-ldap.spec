
Name:           open-xchange-contacts-ldap
BuildArch:	    noarch
#!BuildIgnore:  post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:	@OXVERSION@
%define		ox_release 3
Release:	%{ox_release}_<CI_CNT>.<B_CNT>
Group:          Applications/Productivity
License:        GPL-2.0
BuildRoot:      %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:         %{name}_%{version}.orig.tar.bz2
Summary:        This bundle provides a global LDAP address book
Requires:      open-xchange-core >= @OXVERSION@

%description
This bundle provides a global LDAP address book

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
    . /opt/open-xchange/lib/oxfunctions.sh

    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'

    ##
    ## start update from < 6.21
    ##
    odir=/opt/open-xchange/etc/groupware/contacts-ldap
    ndir=/opt/open-xchange/etc/contacts-ldap
    for i in $(find $odir -maxdepth 1 -name "mapping*.properties"); do
	cp -a $i $ndir/$(basename $i) && rm $i || true
    done
    for i in $(find $odir -name "[0-9]*" -type d); do
	npropdir=$ndir/$(basename $i)
	if [ ! -d $npropdir ]; then
	    mkdir $npropdir
	    ox_update_permissions "$npropdir" root:open-xchange 750
	fi
	for prop in $(find $i -name "*.properties"); do
	    cp -a $prop $npropdir && rm $prop || true
	done
    done

    # SoftwareChange_Request-1080
    # -----------------------------------------------------------------------
    for i in $(find /opt/open-xchange/etc/contacts-ldap/ -name "[0-9]*" -type d); do
        if [ -d $i ]; then
            for prop in $(find $i -name "*.properties"); do
                ctx=$(basename $i)
                psname=$(basename $prop .properties)
                ostr="com.openexchange.contacts.ldap.context${ctx}.${psname}.storagePriority"
                if ! grep $ostr $prop > /dev/null; then
                    echo -e "\n${ostr}=17" >> $prop
                fi
            done
        fi
    done    
    ##
    ## end update from < 6.21
    ##

    for i in $(find /opt/open-xchange/etc/contacts-ldap -name "*.example"); do
         ox_update_permissions "$i" root:open-xchange 640
    done
fi


%clean
%{__rm} -rf %{buildroot}

%files
%defattr(-,root,root)
%dir /opt/open-xchange/bundles/
/opt/open-xchange/bundles/*
%dir /opt/open-xchange/osgi/bundle.d/
/opt/open-xchange/osgi/bundle.d/*
%dir %attr(750,root,open-xchange) /opt/open-xchange/etc/contacts-ldap
%attr(640,root,open-xchange) /opt/open-xchange/etc/contacts-ldap/*.example
%attr(640,root,open-xchange) /opt/open-xchange/etc/contacts-ldap/*/*.example

%changelog
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release build for EDP drop #5
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
* Mon Jul 16 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
