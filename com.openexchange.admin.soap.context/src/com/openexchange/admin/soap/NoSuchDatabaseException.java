
package com.openexchange.admin.soap;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElementRef;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse f�r anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="NoSuchDatabaseException" type="{http://exceptions.rmi.admin.openexchange.com/xsd}NoSuchDatabaseException" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
    "noSuchDatabaseException"
})
@XmlRootElement(name = "NoSuchDatabaseException")
public class NoSuchDatabaseException {

    @XmlElementRef(name = "NoSuchDatabaseException", namespace = "http://soap.admin.openexchange.com", type = JAXBElement.class)
    protected JAXBElement<com.openexchange.admin.rmi.exceptions.xsd.NoSuchDatabaseException> noSuchDatabaseException;

    /**
     * Ruft den Wert der noSuchDatabaseException-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link JAXBElement }{@code <}{@link com.openexchange.admin.rmi.exceptions.xsd.NoSuchDatabaseException }{@code >}
     *     
     */
    public JAXBElement<com.openexchange.admin.rmi.exceptions.xsd.NoSuchDatabaseException> getNoSuchDatabaseException() {
        return noSuchDatabaseException;
    }

    /**
     * Legt den Wert der noSuchDatabaseException-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link JAXBElement }{@code <}{@link com.openexchange.admin.rmi.exceptions.xsd.NoSuchDatabaseException }{@code >}
     *     
     */
    public void setNoSuchDatabaseException(JAXBElement<com.openexchange.admin.rmi.exceptions.xsd.NoSuchDatabaseException> value) {
        this.noSuchDatabaseException = value;
    }

}
