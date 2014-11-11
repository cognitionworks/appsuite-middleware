
Name:          open-xchange-oauth
BuildArch:     noarch
#!BuildIgnore: post-build-checks
%if 0%{?rhel_version} && 0%{?rhel_version} >= 700
BuildRequires: ant
%else
BuildRequires: ant-nodeps
%endif
BuildRequires: open-xchange-core
%if 0%{?rhel_version} && 0%{?rhel_version} == 600
BuildRequires: java7-devel
%else
BuildRequires: java-devel >= 1.7.0
%endif
Version:       @OXVERSION@
%define        ox_release 0
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       The Open-Xchange OAuth implementation
Autoreqprov:   no
Requires:      open-xchange-core >= @OXVERSION@
Provides:      open-xchange-http-deferrer = %{version}
Obsoletes:     open-xchange-http-deferrer < %{version}
Provides:      open-xchange-oauth-facebook = %{version}
Obsoletes:     open-xchange-oauth-facebook < %{version}
Provides:      open-xchange-oauth-json = %{version}
Obsoletes:     open-xchange-oauth-json < %{version}
Provides:      open-xchange-oauth-linkedin = %{version}
Obsoletes:     open-xchange-oauth-linkedin < %{version}
Provides:      open-xchange-oauth-msn = %{version}
Obsoletes:     open-xchange-oauth-msn < %{version}
Provides:      open-xchange-oauth-twitter = %{version}
Obsoletes:     open-xchange-oauth-twitter < %{version}
Provides:      open-xchange-oauth-yahoo = %{version}
Obsoletes:     open-xchange-oauth-yahoo < %{version}

%description
The Open-Xchange OAuth implementation.

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
. /opt/open-xchange/lib/oxfunctions.sh
CONFFILES="deferrer.properties oauth.properties facebookoauth.properties linkedinoauth.properties twitteroauth.properties yahoooauth.properties"
for FILE in ${CONFFILES}; do
    ox_move_config_file /opt/open-xchange/etc/groupware /opt/open-xchange/etc $FILE
done

if [ ${1:-0} -eq 2 ]; then
    # only when updating

    # prevent bash from expanding, see bug 13316
    GLOBIGNORE='*'

    PROTECT="facebookoauth.properties linkedinoauth.properties yahoooauth.properties xingoauth.properties settings/flickroauth.properties settings/tumblroauth.properties"
    for FILE in $PROTECT; do
        ox_update_permissions "/opt/open-xchange/etc/$FILE" root:open-xchange 640
    done

    # SoftwareChange_Request-1494
    pfile=/opt/open-xchange/etc/yahoooauth.properties
    if ! ox_exists_property com.openexchange.oauth.yahoo $pfile; then
       if grep -E '^com.openexchange.*REPLACE_THIS_WITH_VALUE_OBTAINED_FROM' $pfile > /dev/null; then
           ox_set_property com.openexchange.oauth.yahoo false $pfile
       else
           ox_set_property com.openexchange.oauth.yahoo true $pfile
       fi
    fi
    pfile=/opt/open-xchange/etc/linkedinoauth.properties
    if ! ox_exists_property com.openexchange.oauth.linkedin $pfile; then
       if grep -E '^com.openexchange.*REPLACE_THIS_WITH_THE_KEY_FROM' $pfile > /dev/null; then
           ox_set_property com.openexchange.oauth.linkedin false $pfile
       else
           ox_set_property com.openexchange.oauth.linkedin true $pfile
       fi
    fi
    pfile=/opt/open-xchange/etc/facebookoauth.properties
    if ! ox_exists_property com.openexchange.oauth.facebook $pfile; then
       if grep -E '^com.openexchange.*INSERT_YOUR_API_KEY_HERE' $pfile > /dev/null; then
           ox_set_property com.openexchange.oauth.facebook false $pfile
       else
           ox_set_property com.openexchange.oauth.facebook true $pfile
       fi
    fi

    # SoftwareChange_Request-2146
    PFILE=/opt/open-xchange/etc/xingoauth.properties
    ox_add_property com.openexchange.oauth.xing.consumerKey REPLACE_THIS_WITH_YOUR_XING_PRODUCTIVE_CONSUMER_KEY /opt/open-xchange/etc/xingoauth.properties
    ox_add_property com.openexchange.oauth.xing.consumerSecret REPLACE_THIS_WITH_YOUR_XING_PRODUCTIVE_CONSUMER_SECRET /opt/open-xchange/etc/xingoauth.properties
    VALUE=$(ox_read_property com.openexchange.oauth.xing $PFILE)
    if [ "$VALUE" = "false" ]; then
        ox_set_property com.openexchange.oauth.xing true $PFILE
    fi
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
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/boxcomoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/dropboxoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/facebookoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/googleoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/linkedinoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/msliveconnectoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/yahoooauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/xingoauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/settings/flickroauth.properties
%config(noreplace) %attr(640,root,open-xchange) /opt/open-xchange/etc/settings/tumblroauth.properties
%config(noreplace) /opt/open-xchange/etc/*
%dir /opt/open-xchange/etc/settings/
%config(noreplace) /opt/open-xchange/etc/settings/*

%changelog
* Wed Nov 05 2014 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.8.0 release
* Fri Oct 31 2014 Steffen Templin <marcus.klein@open-xchange.com>
First candidate for 7.6.2 release
* Tue Oct 28 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-11-03
* Mon Oct 27 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-30
* Fri Oct 24 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-11-04
* Fri Oct 24 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-11-03
* Fri Oct 24 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-22
* Fri Oct 17 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-24
* Tue Oct 14 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fifth candidate for 7.6.1 release
* Fri Oct 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-20
* Fri Oct 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fourth candidate for 7.6.1 release
* Fri Oct 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-20
* Thu Oct 09 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-13
* Tue Oct 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-09
* Tue Oct 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-09
* Tue Oct 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-10
* Thu Oct 02 2014 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.6.1
* Tue Sep 30 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-06
* Fri Sep 26 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-09-29
* Fri Sep 26 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-06
* Tue Sep 23 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-10-02
* Thu Sep 18 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-09-23
* Wed Sep 17 2014 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.6.2 release
* Tue Sep 16 2014 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.6.1
* Mon Sep 08 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-09-15
* Mon Sep 08 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-09-15
* Fri Sep 05 2014 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.6.1
* Thu Aug 21 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-25
* Wed Aug 20 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-25
* Mon Aug 18 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-25
* Wed Aug 13 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-15
* Tue Aug 05 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-06
* Mon Aug 04 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-11
* Mon Aug 04 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-08-11
* Mon Jul 28 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-30
* Mon Jul 21 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-28
* Tue Jul 15 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-21
* Mon Jul 14 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-24
* Thu Jul 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-15
* Mon Jul 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-14
* Mon Jul 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-07
* Tue Jul 01 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-07-07
* Thu Jun 26 2014 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.6.1
* Mon Jun 23 2014 Steffen Templin <marcus.klein@open-xchange.com>
Seventh candidate for 7.6.0 release
* Fri Jun 20 2014 Steffen Templin <marcus.klein@open-xchange.com>
Sixth release candidate for 7.6.0
* Wed Jun 18 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-06-30
* Fri Jun 13 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 7.6.0
* Fri Jun 13 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-06-23
* Thu Jun 05 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-06-16
* Fri May 30 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 7.6.0
* Thu May 22 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-05-26
* Fri May 16 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-05-26
* Fri May 16 2014 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.6.0
* Wed May 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-05-05
* Mon May 05 2014 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.6.0
* Fri Apr 25 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-04-29
* Tue Apr 15 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-04-22
* Fri Apr 11 2014 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.6.0
* Thu Apr 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-04-11
* Thu Apr 03 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-04-07
* Mon Mar 31 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-03-31
* Wed Mar 19 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-03-21
* Mon Mar 17 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-03-24
* Thu Mar 13 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-03-13
* Mon Mar 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-12
* Fri Mar 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-07
* Tue Mar 04 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-05
* Tue Feb 25 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-03-10
* Tue Feb 25 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-26
* Fri Feb 21 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-28
* Fri Feb 21 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-26
* Tue Feb 18 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-20
* Wed Feb 12 2014 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.6.0
* Fri Feb 07 2014 Steffen Templin <marcus.klein@open-xchange.com>
Sixth release candidate for 7.4.2
* Thu Feb 06 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 7.4.2
* Thu Feb 06 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-11
* Tue Feb 04 2014 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 7.4.2
* Fri Jan 31 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-03
* Thu Jan 30 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-02-03
* Wed Jan 29 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-30
* Tue Jan 28 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-31
* Tue Jan 28 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-30
* Tue Jan 28 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-30
* Mon Jan 27 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-30
* Fri Jan 24 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-17
* Thu Jan 23 2014 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.4.2
* Wed Jan 22 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-22
* Mon Jan 20 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-20
* Thu Jan 16 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-16
* Mon Jan 13 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-14
* Fri Jan 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.4.2
* Fri Jan 10 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-17
* Fri Jan 03 2014 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2014-01-06
* Mon Dec 23 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-09
* Mon Dec 23 2013 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.4.2
* Thu Dec 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-23
* Thu Dec 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-23
* Thu Dec 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-23
* Wed Dec 18 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.4.2
* Tue Dec 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-19
* Tue Dec 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-18
* Tue Dec 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-16
* Thu Dec 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-12
* Thu Dec 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-12
* Mon Dec 09 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-09
* Fri Dec 06 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-29
* Fri Dec 06 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-12-10
* Tue Dec 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-28
* Wed Nov 20 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fifth candidate for 7.4.1 release
* Tue Nov 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fourth candidate for 7.4.1 release
* Mon Nov 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-12
* Mon Nov 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-12
* Fri Nov 08 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-11
* Thu Nov 07 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-08
* Thu Nov 07 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third candidate for 7.4.1 release
* Tue Nov 05 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-11-12
* Wed Oct 30 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-28
* Thu Oct 24 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-30
* Thu Oct 24 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-30
* Wed Oct 23 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second candidate for 7.4.1 release
* Tue Oct 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-23
* Mon Oct 21 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Thu Oct 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Tue Oct 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-11
* Mon Oct 14 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-21
* Mon Oct 14 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-15
* Thu Oct 10 2013 Steffen Templin <marcus.klein@open-xchange.com>
First sprint increment for 7.4.0 release
* Wed Oct 09 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-09
* Wed Oct 09 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-10-07
* Thu Sep 26 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-23
* Tue Sep 24 2013 Steffen Templin <marcus.klein@open-xchange.com>
Eleventh candidate for 7.4.0 release
* Fri Sep 20 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.4.1 release
* Fri Sep 20 2013 Steffen Templin <marcus.klein@open-xchange.com>
Tenth candidate for 7.4.0 release
* Thu Sep 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Ninth candidate for 7.4.0 release
* Wed Sep 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-12
* Wed Sep 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-12
* Thu Sep 05 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-05
* Mon Sep 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-26
* Mon Sep 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Eighth candidate for 7.4.0 release
* Fri Aug 30 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-30
* Wed Aug 28 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-09-03
* Tue Aug 27 2013 Steffen Templin <marcus.klein@open-xchange.com>
Seventh candidate for 7.4.0 release
* Fri Aug 23 2013 Steffen Templin <marcus.klein@open-xchange.com>
Sixth candidate for 7.4.0 release
* Thu Aug 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-22
* Thu Aug 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-22
* Tue Aug 20 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-19
* Mon Aug 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-21
* Mon Aug 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 7.4.0
* Tue Aug 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 7.4.0
* Tue Aug 06 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.4.0
* Mon Aug 05 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-08-09
* Fri Aug 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.4.0
* Wed Jul 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.4.0
* Tue Jul 16 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.4.0
* Mon Jul 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second build for patch  2013-07-18
* Mon Jul 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-07-18
* Fri Jul 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-07-18
* Fri Jul 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-07-18
* Thu Jul 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-07-10
* Wed Jul 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-06-27
* Mon Jul 01 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third candidate for 7.2.2 release
* Fri Jun 28 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second candidate for 7.2.2 release
* Wed Jun 26 2013 Steffen Templin <marcus.klein@open-xchange.com>
Release candidate for 7.2.2 release
* Fri Jun 21 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second feature freeze for 7.2.2 release
* Mon Jun 17 2013 Steffen Templin <marcus.klein@open-xchange.com>
Feature freeze for 7.2.2 release
* Tue Jun 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-06-13
* Mon Jun 10 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-06-11
* Fri Jun 07 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-06-20
* Mon Jun 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
First sprint increment for 7.2.2 release
* Wed May 29 2013 Steffen Templin <marcus.klein@open-xchange.com>
First candidate for 7.2.2 release
* Tue May 28 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second build for patch 2013-05-28
* Mon May 27 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.2.2
* Thu May 23 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third candidate for 7.2.1 release
* Wed May 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-22
* Wed May 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-22
* Wed May 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-22
* Wed May 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second candidate for 7.2.1 release
* Wed May 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-10
* Mon May 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-09
* Mon May 13 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-05-09
* Fri May 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-23
* Tue Apr 30 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-17
* Mon Apr 22 2013 Steffen Templin <marcus.klein@open-xchange.com>
First candidate for 7.2.1 release
* Mon Apr 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.2.1
* Fri Apr 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-12
* Wed Apr 10 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fourth candidate for 7.2.0 release
* Tue Apr 09 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third candidate for 7.2.0 release
* Tue Apr 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second candidate for 7.2.0 release
* Tue Apr 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-04
* Tue Apr 02 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-04-04
* Tue Mar 26 2013 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.2.0
* Fri Mar 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.2.0
* Tue Mar 12 2013 Steffen Templin <marcus.klein@open-xchange.com>
Sixth release candidate for 6.22.2/7.0.2
* Mon Mar 11 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.2/7.0.2
* Fri Mar 08 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 6.22.2/7.0.2
* Thu Mar 07 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 6.22.2/7.0.2
* Mon Mar 04 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-07
* Mon Mar 04 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-08
* Fri Mar 01 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-03-07
* Wed Feb 27 2013 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 6.22.2/7.0.2
* Tue Feb 26 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-02-22
* Mon Feb 25 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-02-22
* Tue Feb 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 7.0.1
* Tue Feb 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.0.1
* Tue Feb 19 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.0.2 release
* Fri Feb 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-02-13
* Thu Feb 14 2013 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.0.1
* Fri Feb 01 2013 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.0.1
* Tue Jan 29 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-01-28
* Mon Jan 21 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-01-24
* Tue Jan 15 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2013-01-23
* Thu Jan 10 2013 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.0.1
* Thu Jan 03 2013 Steffen Templin <marcus.klein@open-xchange.com>
Build for public patch 2013-01-15
* Fri Dec 28 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for public patch 2012-12-31
* Fri Dec 21 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for public patch 2012-12-21
* Tue Dec 18 2012 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 7.0.0
* Mon Dec 17 2012 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 7.0.0
* Wed Dec 12 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for public patch 2012-12-04
* Tue Dec 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 7.0.0
* Tue Dec 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 7.0.0 release
* Mon Nov 26 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2012-11-28
* Wed Nov 14 2012 Steffen Templin <marcus.klein@open-xchange.com>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.1
* Tue Nov 13 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for EDP drop #6
* Tue Nov 06 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.1
* Mon Nov 05 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2012-10-31
* Fri Nov 02 2012 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 6.22.1
* Wed Oct 31 2012 Steffen Templin <marcus.klein@open-xchange.com>
Build for patch 2012-10-31
* Fri Oct 26 2012 Steffen Templin <marcus.klein@open-xchange.com>
Third release build for EDP drop #5
* Fri Oct 26 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 6.22.1
* Fri Oct 26 2012 Steffen Templin <marcus.klein@open-xchange.com>
Second release build for EDP drop #5
* Fri Oct 26 2012 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 6.22.1
* Thu Oct 11 2012 Steffen Templin <marcus.klein@open-xchange.com>
Release build for EDP drop #5
* Wed Oct 10 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.0
* Tue Oct 09 2012 Steffen Templin <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.0
* Fri Oct 05 2012 Steffen Templin <marcus.klein@open-xchange.com>
Third release candidate for 6.22.0
* Thu Oct 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Second release candidate for 6.22.0
* Tue Sep 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 6.23.0
* Mon Sep 03 2012 Steffen Templin <marcus.klein@open-xchange.com>
prepare for next EDP drop
* Tue Aug 21 2012 Steffen Templin <marcus.klein@open-xchange.com>
First release candidate for 6.22.0
* Mon Aug 20 2012 Steffen Templin <marcus.klein@open-xchange.com>
prepare for 6.22.0
* Tue Jul 03 2012 Steffen Templin <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Mon Jun 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Release build for EDP drop #2
* Tue May 22 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #2
* Mon Apr 16 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #1
* Wed Apr 04 2012 Steffen Templin <marcus.klein@open-xchange.com>
Internal release build for EDP drop #0
* Mon Jan 30 2012 Steffen Templin <steffen.templin@open-xchange.com>
Initial release
