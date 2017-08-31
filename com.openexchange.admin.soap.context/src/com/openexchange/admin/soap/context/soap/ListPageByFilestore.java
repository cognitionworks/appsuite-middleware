
package com.openexchange.admin.soap.context.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.context.dataobjects.Credentials;
import com.openexchange.admin.soap.context.dataobjects.Filestore;


/**
 * <p>Java-Klasse f\u00fcr anonymous complex type.
 *
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType>
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="fs" type="{http://dataobjects.soap.admin.openexchange.com/xsd}Filestore" minOccurs="0"/>
 *         &lt;element name="offset" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="length" type="{http://www.w3.org/2001/XMLSchema}int" minOccurs="0"/>
 *         &lt;element name="auth" type="{http://dataobjects.rmi.admin.openexchange.com/xsd}Credentials" minOccurs="0"/>
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
    "fs",
    "offset",
    "length",
    "auth"
})
@XmlRootElement(name = "listPageByFilestore")
public class ListPageByFilestore {

    @XmlElement(nillable = true)
    protected Filestore fs;
    @XmlElement(name = "offset", nillable = true)
    protected int offset;
    @XmlElement(name = "length", nillable = true)
    protected int length;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Ruft den Wert der fs-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Filestore }
     *
     */
    public Filestore getFs() {
        return fs;
    }

    /**
     * Legt den Wert der fs-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Filestore }
     *
     */
    public void setFs(Filestore value) {
        this.fs = value;
    }

    public int getOffset() {
        return offset;
    }

    public void setOffset(int offset) {
        this.offset = offset;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    /**
     * Ruft den Wert der auth-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Credentials }
     *
     */
    public Credentials getAuth() {
        return auth;
    }

    /**
     * Legt den Wert der auth-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Credentials }
     *
     */
    public void setAuth(Credentials value) {
        this.auth = value;
    }

}
