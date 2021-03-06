package com.openexchange.admin.soap.reseller.resource.reseller.soap;

import java.net.URL;
import javax.xml.namespace.QName;
import javax.xml.ws.WebEndpoint;
import javax.xml.ws.WebServiceClient;
import javax.xml.ws.WebServiceFeature;
import javax.xml.ws.Service;

/**
 * This class was generated by Apache CXF 2.6.0
 * 2012-06-06T11:10:25.017+02:00
 * Generated source version: 2.6.0
 *
 */
@WebServiceClient(name = "OXResellerResourceService",

                  targetNamespace = "http://soap.reseller.admin.openexchange.com")
public class OXResellerResourceService extends Service {

    public final static URL WSDL_LOCATION;

    public final static QName SERVICE = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceService");
    public final static QName OXResellerResourceServiceHttpSoap12Endpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpSoap12Endpoint");
    public final static QName OXResellerResourceServiceHttpEndpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpEndpoint");
    public final static QName OXResellerResourceServiceHttpSoap11Endpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpSoap11Endpoint");
    public final static QName OXResellerResourceServiceHttpsEndpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpsEndpoint");
    public final static QName OXResellerResourceServiceHttpsSoap11Endpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpsSoap11Endpoint");
    public final static QName OXResellerResourceServiceHttpsSoap12Endpoint = new QName("http://soap.reseller.admin.openexchange.com", "OXResellerResourceServiceHttpsSoap12Endpoint");
    static {
        WSDL_LOCATION = null;
    }

    public OXResellerResourceService(URL wsdlLocation) {
        super(wsdlLocation, SERVICE);
    }

    public OXResellerResourceService(URL wsdlLocation, QName serviceName) {
        super(wsdlLocation, serviceName);
    }

    public OXResellerResourceService() {
        super(WSDL_LOCATION, SERVICE);
    }


    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpSoap12Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpSoap12Endpoint() {
        return super.getPort(OXResellerResourceServiceHttpSoap12Endpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpSoap12Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpSoap12Endpoint, OXResellerResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpEndpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpEndpoint() {
        return super.getPort(OXResellerResourceServiceHttpEndpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpEndpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpEndpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpEndpoint, OXResellerResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpSoap11Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpSoap11Endpoint() {
        return super.getPort(OXResellerResourceServiceHttpSoap11Endpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpSoap11Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpSoap11Endpoint, OXResellerResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsEndpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsEndpoint() {
        return super.getPort(OXResellerResourceServiceHttpsEndpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsEndpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsEndpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpsEndpoint, OXResellerResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsSoap11Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsSoap11Endpoint() {
        return super.getPort(OXResellerResourceServiceHttpsSoap11Endpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsSoap11Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsSoap11Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpsSoap11Endpoint, OXResellerResourceServicePortType.class, features);
    }
    /**
     *
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsSoap12Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsSoap12Endpoint() {
        return super.getPort(OXResellerResourceServiceHttpsSoap12Endpoint, OXResellerResourceServicePortType.class);
    }

    /**
     *
     * @param features
     *     A list of {@link javax.xml.ws.WebServiceFeature} to configure on the proxy.  Supported features not in the <code>features</code> parameter will have their default values.
     * @return
     *     returns OXResellerResourceServicePortType
     */
    @WebEndpoint(name = "OXResellerResourceServiceHttpsSoap12Endpoint")
    public OXResellerResourceServicePortType getOXResellerResourceServiceHttpsSoap12Endpoint(WebServiceFeature... features) {
        return super.getPort(OXResellerResourceServiceHttpsSoap12Endpoint, OXResellerResourceServicePortType.class, features);
    }

}
