
package com.openexchange.admin.soap.context.soap;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.context.dataobjects.Credentials;
import com.openexchange.admin.soap.context.dataobjects.Database;


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
 *         &lt;element name="db" type="{http://dataobjects.soap.admin.openexchange.com/xsd}Database" minOccurs="0"/>
 *         &lt;element name="offset" type="{http://dataobjects.soap.admin.openexchange.com/xsd}integer" minOccurs="0"/>
 *         &lt;element name="length" type="{http://dataobjects.soap.admin.openexchange.com/xsd}integer" minOccurs="0"/>
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
    "db",
    "offset",
    "length",
    "auth"
})
@XmlRootElement(name = "listPageByDatabase")
public class ListPageByDatabase {

    @XmlElement(nillable = true)
    protected Database db;
    @XmlElement(nillable = true)
    protected String offset;
    @XmlElement(nillable = true)
    protected String length;
    @XmlElement(nillable = true)
    protected Credentials auth;

    /**
     * Ruft den Wert der db-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Database }
     *
     */
    public Database getDb() {
        return db;
    }

    /**
     * Legt den Wert der db-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Database }
     *
     */
    public void setDb(Database value) {
        this.db = value;
    }

    public String getOffset() {
        return offset;
    }

    public void setOffset(String offset) {
        this.offset = offset;
    }

    public String getLength() {
        return length;
    }

    public void setLength(String length) {
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
