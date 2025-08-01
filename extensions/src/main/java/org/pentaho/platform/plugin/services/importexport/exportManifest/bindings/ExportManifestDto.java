/*! ******************************************************************************
 *
 * Pentaho
 *
 * Copyright (C) 2024 by Hitachi Vantara, LLC : http://www.pentaho.com
 *
 * Use of this software is governed by the Business Source License included
 * in the LICENSE.TXT file.
 *
 * Change Date: 2029-07-20
 ******************************************************************************/


//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, v2.2.4-2 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2013.07.25 at 11:25:28 AM EDT 
//

package org.pentaho.platform.plugin.services.importexport.exportManifest.bindings;

import org.pentaho.platform.plugin.services.importexport.ExportManifestUserSetting;
import org.pentaho.platform.plugin.services.importexport.RoleExport;
import org.pentaho.platform.plugin.services.importexport.UserExport;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlAttribute;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlType;
import java.util.ArrayList;
import java.util.List;

/**
 * <p/>
 * Java class for ExportManifestDto complex type.
 * <p/>
 * <p/>
 * The following schema fragment specifies the expected content contained within this class.
 * <p/>
 * <pre>
 * &lt;complexType name="ExportManifestDto">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="ExportManifestInformation">
 *           &lt;complexType>
 *             &lt;complexContent>
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *                 &lt;attribute name="exportDate" type="{http://www.w3.org/2001/XMLSchema}string" default="{date}" />
 *                 &lt;attribute name="exportBy" type="{http://www.w3.org/2001/XMLSchema}string" default="{user}" />
 *                 &lt;attribute name="rootFolder" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *                 &lt;attribute name="manifestVersion" type="{http://www.w3.org/2001/XMLSchema}string" default="{version}" />
 *               &lt;/restriction>
 *             &lt;/complexContent>
 *           &lt;/complexType>
 *         &lt;/element>
 *         &lt;element name="ExportManifestMondrian" type="{http://www.pentaho.com/schema/}ExportManifestMondrian"
 * maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ExportManifestMetadata" type="{http://www.pentaho.com/schema/}ExportManifestMetadata"
 * maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ExportManifestSchedule" type="{http://www.pentaho.com/schema/}jobScheduleRequest"
 * maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ExportManifestDatasource" type="{http://www.pentaho.com/schema/}databaseConnection"
 * maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="ExportManifestEntity" type="{http://www.pentaho.com/schema/}ExportManifestEntityDto"
 * maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 */
@XmlAccessorType ( XmlAccessType.FIELD )
@XmlType ( name = "ExportManifestDto", propOrder = { "exportManifestInformation", "exportManifestMondrian",
  "exportManifestMetadata", "exportManifestSchedule", "exportManifestDatasource", "exportManifestEntity",
  "exportManifestUser", "exportManifestRole", "exportManifestMetaStore", "globalUserSettings" } )
public class ExportManifestDto {

  @XmlElement ( name = "ExportManifestInformation", required = true )
  protected ExportManifestDto.ExportManifestInformation exportManifestInformation;
  @XmlElement ( name = "ExportManifestMondrian" )
  protected List<ExportManifestMondrian> exportManifestMondrian;
  @XmlElement ( name = "ExportManifestMetadata" )
  protected List<ExportManifestMetadata> exportManifestMetadata;
  @XmlElement ( name = "ExportManifestSchedule" )
  protected List<JobScheduleRequest> exportManifestSchedule;
  @XmlElement ( name = "ExportManifestDatasource" )
  protected List<DatabaseConnection> exportManifestDatasource;
  @XmlElement ( name = "ExportManifestEntity" )
  protected List<ExportManifestEntityDto> exportManifestEntity;
  @XmlElement ( name = "ExportManifestUser" )
  protected List<UserExport> exportManifestUser;
  @XmlElement ( name = "ExportManifestRole" )
  protected List<RoleExport> exportManifestRole;
  @XmlElement ( name = "ExportManifestMetaStore", required = false )
  protected ExportManifestMetaStore exportManifestMetaStore;
  @XmlElement ( name = "ExportManifestGlobalUserSetting" )
  protected List<ExportManifestUserSetting> globalUserSettings;

  /**
   * Gets the value of the exportManifestInformation property.
   *
   * @return possible object is {@link ExportManifestDto.ExportManifestInformation }
   */
  public ExportManifestDto.ExportManifestInformation getExportManifestInformation() {
    return exportManifestInformation;
  }

  /**
   * Sets the value of the exportManifestInformation property.
   *
   * @param value allowed object is {@link ExportManifestDto.ExportManifestInformation }
   */
  public void setExportManifestInformation( ExportManifestDto.ExportManifestInformation value ) {
    this.exportManifestInformation = value;
  }

  /**
   * Gets the value of the exportManifestMondrian property.
   * <p/>
   * <p/>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
   * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
   * the exportManifestMondrian property.
   * <p/>
   * <p/>
   * For example, to add a new item, do as follows:
   * <p/>
   * <pre>
   * getExportManifestMondrian().add( newItem );
   * </pre>
   * <p/>
   * <p/>
   * <p/>
   * Objects of the following type(s) are allowed in the list {@link ExportManifestMondrian }
   */
  public List<ExportManifestMondrian> getExportManifestMondrian() {
    if ( exportManifestMondrian == null ) {
      exportManifestMondrian = new ArrayList<>();
    }
    return this.exportManifestMondrian;
  }

  public List<UserExport> getExportManifestUser() {
    if ( exportManifestUser == null ) {
      exportManifestUser = new ArrayList<>();
    }
    return this.exportManifestUser;
  }

  public List<RoleExport> getExportManifestRole() {
    if ( exportManifestRole == null ) {
      exportManifestRole = new ArrayList<>();
    }
    return this.exportManifestRole;
  }

  /**
   * Gets the value of the exportManifestMetadata property.
   * <p/>
   * <p/>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
   * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
   * the exportManifestMetadata property.
   * <p/>
   * <p/>
   * For example, to add a new item, do as follows:
   * <p/>
   * <pre>
   * getExportManifestMetadata().add( newItem );
   * </pre>
   * <p/>
   * <p/>
   * <p/>
   * Objects of the following type(s) are allowed in the list {@link ExportManifestMetadata }
   */
  public List<ExportManifestMetadata> getExportManifestMetadata() {
    if ( exportManifestMetadata == null ) {
      exportManifestMetadata = new ArrayList<>();
    }
    return this.exportManifestMetadata;
  }

  /**
   * Gets the value of the exportManifestSchedule property.
   * <p/>
   * <p/>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
   * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
   * the exportManifestSchedule property.
   * <p/>
   * <p/>
   * For example, to add a new item, do as follows:
   * <p/>
   * <pre>
   * getExportManifestSchedule().add( newItem );
   * </pre>
   * <p/>
   * <p/>
   * <p/>
   * Objects of the following type(s) are allowed in the list {@link JobScheduleRequest }
   */
  public List<JobScheduleRequest> getExportManifestSchedule() {
    if ( exportManifestSchedule == null ) {
      exportManifestSchedule = new ArrayList<>();
    }
    return this.exportManifestSchedule;
  }

  /**
   * Gets the value of the exportManifestDatasource property.
   * <p/>
   * <p/>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
   * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
   * the exportManifestDatasource property.
   * <p/>
   * <p/>
   * For example, to add a new item, do as follows:
   * <p/>
   * <pre>
   * getExportManifestDatasource().add( newItem );
   * </pre>
   * <p/>
   * <p/>
   * <p/>
   * Objects of the following type(s) are allowed in the list {@link DatabaseConnection }
   */
  public List<DatabaseConnection> getExportManifestDatasource() {
    if ( exportManifestDatasource == null ) {
      exportManifestDatasource = new ArrayList<>();
    }
    return this.exportManifestDatasource;
  }

  /**
   * Gets the value of the exportManifestEntity property.
   * <p/>
   * <p/>
   * This accessor method returns a reference to the live list, not a snapshot. Therefore any modification you make to
   * the returned list will be present inside the JAXB object. This is why there is not a <CODE>set</CODE> method for
   * the exportManifestEntity property.
   * <p/>
   * <p/>
   * For example, to add a new item, do as follows:
   * <p/>
   * <pre>
   * getExportManifestEntity().add( newItem );
   * </pre>
   * <p/>
   * <p/>
   * <p/>
   * Objects of the following type(s) are allowed in the list {@link ExportManifestEntityDto }
   */
  public List<ExportManifestEntityDto> getExportManifestEntity() {
    if ( exportManifestEntity == null ) {
      exportManifestEntity = new ArrayList<>();
    }
    return this.exportManifestEntity;
  }

  /**
   * <p/>
   * Java class for anonymous complex type.
   * <p/>
   * <p/>
   * The following schema fragment specifies the expected content contained within this class.
   * <p/>
   * <pre>
   * &lt;complexType>
   *   &lt;complexContent>
   *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
   *       &lt;attribute name="exportDate" type="{http://www.w3.org/2001/XMLSchema}string" default="{date}" />
   *       &lt;attribute name="exportBy" type="{http://www.w3.org/2001/XMLSchema}string" default="{user}" />
   *       &lt;attribute name="rootFolder" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
   *       &lt;attribute name="manifestVersion" type="{http://www.w3.org/2001/XMLSchema}string" default="{version}" />
   *     &lt;/restriction>
   *   &lt;/complexContent>
   * &lt;/complexType>
   * </pre>
   */
  @XmlAccessorType ( XmlAccessType.FIELD )
  @XmlType ( name = "" )
  public static class ExportManifestInformation {

    @XmlAttribute ( name = "exportDate" )
    protected String exportDate;
    @XmlAttribute ( name = "exportBy" )
    protected String exportBy;
    @XmlAttribute ( name = "rootFolder", required = true )
    protected String rootFolder;
    @XmlAttribute ( name = "manifestVersion" )
    protected String manifestVersion;

    /**
     * Gets the value of the exportDate property.
     *
     * @return possible object is {@link String }
     */
    public String getExportDate() {
      if ( exportDate == null ) {
        return "{date}";
      } else {
        return exportDate;
      }
    }

    /**
     * Sets the value of the exportDate property.
     *
     * @param value allowed object is {@link String }
     */
    public void setExportDate( String value ) {
      this.exportDate = value;
    }

    /**
     * Gets the value of the exportBy property.
     *
     * @return possible object is {@link String }
     */
    public String getExportBy() {
      if ( exportBy == null ) {
        return "{user}";
      } else {
        return exportBy;
      }
    }

    /**
     * Sets the value of the exportBy property.
     *
     * @param value allowed object is {@link String }
     */
    public void setExportBy( String value ) {
      this.exportBy = value;
    }

    /**
     * Gets the value of the rootFolder property.
     *
     * @return possible object is {@link String }
     */
    public String getRootFolder() {
      return rootFolder;
    }

    /**
     * Sets the value of the rootFolder property.
     *
     * @param value allowed object is {@link String }
     */
    public void setRootFolder( String value ) {
      this.rootFolder = value;
    }

    /**
     * Gets the value of the manifest version, if present.
     *
     * @return possible object is {@link String }
     */
    public String getManifestVersion() {
      return manifestVersion;
    }

    /**
     * Sets the value of the manifestVersion property.
     *
     * @param manifestVersion allowed object is {@link String }
     */
    public void setManifestVersion( String manifestVersion ) {
      this.manifestVersion = manifestVersion;
    }

  }

  /**
   * Gets the metastore, if present
   * @return possible object is {@link ExportManifestMetaStore}
   */
  public ExportManifestMetaStore getExportManifestMetaStore() {
    return exportManifestMetaStore;
  }

  /**
   * sets the metastore
   * @param exportManifestMetaStore allowed object is {@link ExportManifestMetaStore}
   */
  public void setExportManifestMetaStore(
    ExportManifestMetaStore exportManifestMetaStore ) {
    this.exportManifestMetaStore = exportManifestMetaStore;
  }

  public List<ExportManifestUserSetting> getGlobalUserSettings() {
    if ( globalUserSettings == null ) {
      globalUserSettings = new ArrayList<>();
    }
    return globalUserSettings;
  }
}
