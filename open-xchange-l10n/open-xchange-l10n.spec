
Name:          open-xchange-l10n
BuildArch:     noarch
#!BuildIgnore: post-build-checks
BuildRequires: ant
BuildRequires: ant-nodeps
BuildRequires: open-xchange-core
BuildRequires: java-devel >= 1.6.0
Version:       @OXVERSION@
%define        ox_release 6
Release:       %{ox_release}_<CI_CNT>.<B_CNT>
Group:         Applications/Productivity
License:       GPL-2.0 
BuildRoot:     %{_tmppath}/%{name}-%{version}-build
URL:           http://www.open-xchange.com/
Source:        %{name}_%{version}.orig.tar.bz2
Summary:       Package containing Open-Xchange backend localization
Requires:      open-xchange-core >= @OXVERSION@

%description
Package containing Open-Xchange backend localization

Authors:
--------
    Open-Xchange

%package de-de
Group:          Applications/Productivity
Summary:        Package containing Open-Xchange backend localization for de_DE

%description de-de
Package containing Open-Xchange backend localization for de_DE

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package fr-fr
Group:          Applications/Productivity
Summary:        Package containing Open-Xchange backend localization for fr_FR

%description fr-fr
Package containing Open-Xchange backend localization for fr_FR

Authors:
--------
    Open-Xchange

%package cs-cz
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for cs_CZ
Provides:       open-xchange-lang-cs-cz = %{version}
Obsoletes:      open-xchange-lang-cs-cz <= %{version}

%description cs-cz
Package containing Open-Xchange backend localization for cs_CZ

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package es-es
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for es_ES
Provides:       open-xchange-lang-es-es = %{version}
Obsoletes:      open-xchange-lang-es-es <= %{version}

%description es-es
Package containing Open-Xchange backend localization for es_ES

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package fr-ca
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for fr_CA
Provides:       open-xchange-lang-fr-ca = %{version}
Obsoletes:      open-xchange-lang-fr-ca <= %{version}

%description fr-ca
Package containing Open-Xchange backend localization for fr_CA

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package hu-hu
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for hu_HU
Provides:       open-xchange-lang-hu-hu = %{version}
Obsoletes:      open-xchange-lang-hu-hu <= %{version}

%description hu-hu
Package containing Open-Xchange backend localization for hu_HU

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package it-it
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for it_IT
Provides:       open-xchange-lang-it-it = %{version}
Obsoletes:      open-xchange-lang-it-it <= %{version}

%description it-it
Package containing Open-Xchange backend localization for it_IT

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package ja-jp
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for ja_JP
Provides:       open-xchange-lang-ja-jp = %{version}
Obsoletes:      open-xchange-lang-ja-jp <= %{version}

%description ja-jp
Package containing Open-Xchange backend localization for ja_JP

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package lv-lv
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for lv_LV
Provides:       open-xchange-lang-lv-lv = %{version}
Obsoletes:      open-xchange-lang-lv-lv <= %{version}

%description lv-lv
Package containing Open-Xchange backend localization for lv_LV

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package nl-nl
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for nl_NL
Provides:       open-xchange-lang-nl-nl = %{version}
Obsoletes:      open-xchange-lang-nl-nl <= %{version}

%description nl-nl
Package containing Open-Xchange backend localization for nl_NL

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package pl-pl
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for pl_PL
Provides:       open-xchange-lang-pl-pl = %{version}
Obsoletes:      open-xchange-lang-pl-pl <= %{version}

%description pl-pl
Package containing Open-Xchange backend localization for pl_PL

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package ro-ro
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for ro_RO
Provides:       open-xchange-lang-ro-ro = %{version}
Obsoletes:      open-xchange-lang-ro-ro <= %{version}

%description ro-ro
Package containing Open-Xchange backend localization for ro_RO

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package sk-sk
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for sk_SK
Provides:       open-xchange-lang-sk-sk = %{version}
Obsoletes:      open-xchange-lang-sk-sk <= %{version}

%description sk-sk
Package containing Open-Xchange backend localization for sk_SK

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package zh-cn
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for zh_CN
Provides:       open-xchange-lang-zh-cn = %{version}
Obsoletes:      open-xchange-lang-zh-cn <= %{version}

%description zh-cn
Package containing Open-Xchange backend localization for zh_CN

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package zh-tw
Group:		Applications/Productivity
Summary:	Package containing Open-Xchange backend localization for zh_TW
Provides:       open-xchange-lang-zh-tw = %{version}
Obsoletes:      open-xchange-lang-zh-tw <= %{version}

%description zh-tw
Package containing Open-Xchange backend localization for zh_TW

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------
%package de-ch
Group:          Applications/Productivity
Summary:        Package containing Open-Xchange backend localization for de_CH

%description de-ch
Package containing Open-Xchange backend localization for de_CH

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------


%package ca-es
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for ca_ES
Provides:       open-xchange-lang-community-ca-es = %{version}
Obsoletes:      open-xchange-lang-community-ca-es <= %{version}

%description ca-es
Package containing Open-Xchange backend localization for ca_ES
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package da-dk
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for da_DK
Provides:       open-xchange-lang-community-da-dk = %{version}
Obsoletes:      open-xchange-lang-community-da-dk <= %{version}

%description da-dk
Package containing Open-Xchange backend localization for da_DK
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package el-gr
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for el_GR
Provides:       open-xchange-lang-community-el-gr = %{version}
Obsoletes:      open-xchange-lang-community-el-gr <= %{version}

%description el-gr
Package containing Open-Xchange backend localization for el_GR
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package eu-es
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for eu_ES
Provides:       open-xchange-lang-community-eu-es = %{version}
Obsoletes:      open-xchange-lang-community-eu-es <= %{version}

%description eu-es
Package containing Open-Xchange backend localization for eu_ES
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package gl-es
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for gl_ES
Provides:       open-xchange-lang-community-gl-es = %{version}
Obsoletes:      open-xchange-lang-community-gl-es <= %{version}

%description gl-es
Package containing Open-Xchange backend localization for gl_ES
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package he-he
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for he_HE
Provides:       open-xchange-lang-community-he-he = %{version}
Obsoletes:      open-xchange-lang-community-he-he <= %{version}

%description he-he
Package containing Open-Xchange backend localization for he_HE
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package hi-in
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for hi_IN
Provides:       open-xchange-lang-community-hi-in = %{version}
Obsoletes:      open-xchange-lang-community-hi-in <= %{version}

%description hi-in
Package containing Open-Xchange backend localization for hi_IN
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package ko-ko
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for ko_KO
Provides:       open-xchange-lang-community-ko-ko = %{version}
Obsoletes:      open-xchange-lang-community-ko-ko <= %{version}

%description ko-ko
Package containing Open-Xchange backend localization for ko_KO
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

#%package no-nb
#Group:      Applications/Productivity
#Summary:    Package containing Open-Xchange backend localization for no_NB
#Provides:       open-xchange-lang-community-no-nb = %{version}
#Obsoletes:      open-xchange-lang-community-no-nb <= %{version}
#
#%description no-nb
#Package containing Open-Xchange backend localization for no_NB
#This localization package are driven by the community.
#
#Authors:
#--------
#    Open-Xchange

#-------------------------------------------------------------------------------------

%package pt-br
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for pt_BR
Provides:       open-xchange-lang-community-pt-br = %{version}
Obsoletes:      open-xchange-lang-community-pt-br <= %{version}

%description pt-br
Package containing Open-Xchange backend localization for pt_BR
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package pt-pt
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for pt_PT
Provides:       open-xchange-lang-community-pt-pt = %{version}
Obsoletes:      open-xchange-lang-community-pt-pt <= %{version}

%description pt-pt
Package containing Open-Xchange backend localization for pt_PT
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package ru-ru
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for ru_RU
Provides:       open-xchange-lang-community-ru-ru = %{version}
Obsoletes:      open-xchange-lang-community-ru-ru <= %{version}

%description ru-ru
Package containing Open-Xchange backend localization for ru_RU
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package sv-sv
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for sv_SV
Provides:       open-xchange-lang-community-sv-sv = %{version}
Obsoletes:      open-xchange-lang-community-sv-sv <= %{version}

%description sv-sv
Package containing Open-Xchange backend localization for sv_SV
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

%package tr-tr
Group:      Applications/Productivity
Summary:    Package containing Open-Xchange backend localization for tr_TR
Provides:       open-xchange-lang-community-tr-tr = %{version}
Obsoletes:      open-xchange-lang-community-tr-tr <= %{version}

%description tr-tr
Package containing Open-Xchange backend localization for tr_TR
This localization package are driven by the community.

Authors:
--------
    Open-Xchange

#-------------------------------------------------------------------------------------

#%package vi-vi
#Group:      Applications/Productivity
#Summary:    Package containing Open-Xchange backend localization for vi_VI
#Provides:       open-xchange-lang-community-vi-vi = %{version}
#Obsoletes:      open-xchange-lang-community-vi-vi <= %{version}
#
#%description vi-vi
#Package containing Open-Xchange backend localization for vi_VI
#This localization package are driven by the community.
#
#Authors:
#--------
#    Open-Xchange

#-------------------------------------------------------------------------------------

%prep

%setup -q

%build

%install
export NO_BRP_CHECK_BYTECODE_VERSION=true
for LANG in ca_ES cs_CZ da_DK de_DE el_GR es_ES eu_ES fr_CA fr_FR he_HE hu_HU it_IT ja_JP ko_KO lv_LV nl_NL pl_PL pt_BR pt_PT ro_RO sk_SK sv_SV tr_TR zh_CN zh_TW ru_RU de_CH; do \
    PACKAGE_EXTENSION=$(echo ${LANG} | tr '[:upper:]_' '[:lower:]-'); \
    ant -lib build/lib -Dbasedir=build -DdestDir=%{buildroot} -DpackageName=%{name} -Dlanguage=${LANG} -f build/build.xml clean build; \
done

%clean
%{__rm} -rf %{buildroot}

%files ca-es
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*ca_ES*

%files da-dk
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*da_DK*

%files de-de
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*de_DE*

%files el-gr
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*el_GR*

%files eu-es
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*eu_ES*

%files fr-fr
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*fr_FR*

%files cs-cz
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*cs_CZ*

%files es-es
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*es_ES*

%files fr-ca
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*fr_CA*

%files he-he
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*he_HE*

%files hu-hu
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*hu_HU*

%files it-it
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*it_IT*

%files ja-jp
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*ja_JP*

%files ko-ko
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*ko_KO*

%files lv-lv
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*lv_LV*

%files nl-nl
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*nl_NL*

%files pl-pl
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*pl_PL*

%files pt-br
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*pt_BR*

%files pt-pt
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*pt_PT*

%files ro-ro
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*ro_RO*

%files sk-sk
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*sk_SK*

%files sv-sv
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*sv_SV*

%files tr-tr
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*tr_TR*

%files zh-cn
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*zh_CN*

%files zh-tw
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*zh_TW*

%files ru-ru
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*ru_RU*

%files de-ch
%defattr(-,root,root)
%dir /opt/open-xchange/i18n/
/opt/open-xchange/i18n/*de_CH*

%changelog
* Wed Nov 14 2012 Marcus Klein <marcus.klein@open-xchange.com>
Sixth release candidate for 6.22.1
* Tue Nov 13 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fifth release candidate for 6.22.1
* Tue Nov 06 2012 Marcus Klein <marcus.klein@open-xchange.com>
Fourth release candidate for 6.22.1
* Fri Nov 02 2012 Marcus Klein <marcus.klein@open-xchange.com>
Third release candidate for 6.22.1
* Wed Oct 31 2012 Marcus Klein <marcus.klein@open-xchange.com>
Second release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
First release candidate for 6.22.1
* Fri Oct 26 2012 Marcus Klein <marcus.klein@open-xchange.com>
prepare for 6.22.1
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
* Wed Apr 25 2012 Marcus Klein <marcus.klein@open-xchange.com>
Initial release
