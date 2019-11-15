---
title: Developer Guide
icon: fa-code
tags: SAML, Custom Dev, SSO
---

<!-- Change to 'fa-dev' icon once we are on FontAwesome 5.x -->

The core implementation is contained in its own bundle `com.openexchange.saml` within the backend repository and relies on the OpenSAML library (https://wiki.shibboleth.net/confluence/display/OpenSAML). This guide assumes that you are familiar with the terminology and technological aspects of SAML 2.0.


# The SAML backend

A SAML backend consists at least of an implementation of `com.openexchange.saml.spi.SAMLBackend`. An instance of this implementation must be registered as OSGi service under this interface. It is considered best practice to start with inheriting from `com.openexchange.saml.spi.AbstractSAMLBackend` instead of implementing the interface directly. This reduces the number of methods to implement while default implementations are used where it is possible. You probably need to override some of the other methods as well to customize their behavior. Especially the validation of SAML responses needs likely to be adjusted, as the default strategy is very strict and will fail if the IdP does not obey the specification in every point. Start with reading the JavaDoc of the mentioned classes and follow their references to get an overview of what you need to implement. Additionally there is an example implementation in the 
`examples/backend-samples` repository that targets WSO2 Identity Server as IdP. The according project is `com.openexchange.saml.wso2`, its packaging information is contained in 
`open-xchange-saml-wso2`.

A Linux package containing the `SAMLBackend` implementation bundle must provide the virtual package `open-xchange-saml-backend`.

SAML might replace the web login of OX App Suite but it cannot be used by non-web clients that make use of the HTTP API directly. Therefore it uses special login/logout calls instead of changing the behavior of the calls from the login module. While those calls are based on an installed authentication service (i.e. a package that provides `open-xchange-authentication`), SAML is not. Nevertheless an authentication service must always be provided. If you want SAML as your only login mechanisms, you can simply register an instance of 
`com.openexchange.saml.spi.DisabledAuthenticationService` as OSGi service and let your package provide `open-xchange-authentication`. You can also decide to implement an own authentication service that takes care of authentication for other clients. It is also possible to install one of the existing `open-xchange-authentication` providers to allow e.g. IMAP or LDAP authentication.
