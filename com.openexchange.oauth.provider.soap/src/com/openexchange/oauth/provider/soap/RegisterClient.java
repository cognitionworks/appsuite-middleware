
package com.openexchange.oauth.provider.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java-Klasse für anonymous complex type.
 * 
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 * 
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="args0" type="{http://soap.provider.oauth.openexchange.com}ClientData" minOccurs="0"/>
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
    "args0"
})
@XmlRootElement(name = "registerClient")
public class RegisterClient {

    @XmlElement(nillable = true)
    protected ClientData args0;

    /**
     * Ruft den Wert der args0-Eigenschaft ab.
     * 
     * @return
     *     possible object is
     *     {@link ClientData }
     *     
     */
    public ClientData getArgs0() {
        return args0;
    }

    /**
     * Legt den Wert der args0-Eigenschaft fest.
     * 
     * @param value
     *     allowed object is
     *     {@link ClientData }
     *     
     */
    public void setArgs0(ClientData value) {
        this.args0 = value;
    }

}
