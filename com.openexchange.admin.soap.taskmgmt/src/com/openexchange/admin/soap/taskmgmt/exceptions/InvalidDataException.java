
package com.openexchange.admin.soap.taskmgmt.exceptions;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;
import com.openexchange.admin.soap.taskmgmt.soap.Exception;


/**
 * <p>Java-Klasse f\u00fcr InvalidDataException complex type.
 *
 * <p>Das folgende Schemafragment gibt den erwarteten Content an, der in dieser Klasse enthalten ist.
 *
 * <pre>
 * &lt;complexType name="InvalidDataException">
 *   &lt;complexContent>
 *     &lt;extension base="{http://soap.admin.openexchange.com}Exception">
 *       &lt;sequence>
 *         &lt;element name="fieldnames" type="{http://www.w3.org/2001/XMLSchema}anyType" minOccurs="0"/>
 *         &lt;element name="objectname" type="{http://www.w3.org/2001/XMLSchema}string" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/extension>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 *
 *
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "InvalidDataException", propOrder = {
    "fieldnames",
    "objectname"
})
public class InvalidDataException
    extends Exception
{

    @XmlElement(nillable = true)
    protected Object fieldnames;
    @XmlElement(nillable = true)
    protected String objectname;

    /**
     * Ruft den Wert der fieldnames-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link Object }
     *
     */
    public Object getFieldnames() {
        return fieldnames;
    }

    /**
     * Legt den Wert der fieldnames-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link Object }
     *
     */
    public void setFieldnames(Object value) {
        this.fieldnames = value;
    }

    /**
     * Ruft den Wert der objectname-Eigenschaft ab.
     *
     * @return
     *     possible object is
     *     {@link String }
     *
     */
    public String getObjectname() {
        return objectname;
    }

    /**
     * Legt den Wert der objectname-Eigenschaft fest.
     *
     * @param value
     *     allowed object is
     *     {@link String }
     *
     */
    public void setObjectname(String value) {
        this.objectname = value;
    }

}
