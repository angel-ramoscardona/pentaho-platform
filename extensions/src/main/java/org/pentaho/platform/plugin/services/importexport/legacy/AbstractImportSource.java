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


package org.pentaho.platform.plugin.services.importexport.legacy;

import java.util.HashMap;
import java.util.Map;

/**
 * @author wseyler
 */
public abstract class AbstractImportSource implements org.pentaho.platform.plugin.services.importexport.ImportSource {
  private static final Map<String, String> mimeTypes = new HashMap<String, String>();

  static {
    // Keys are extensions and values are MIME types.
    mimeTypes.put( "prpt", "application/zip" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "prpti", "application/zip" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "mondrian.xml", "application/vnd.pentaho.mondrian+xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "gif", "image/gif" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "css", "text/css" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "html", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "htm", "text/html" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "jpg", "image/jpeg" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "jpeg", "image/jpeg" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "js", "text/javascript" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "cfg.xml", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "jrxml", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "kjb", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "ktr", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "png", "image/png" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "properties", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "report", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "rptdesign", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "svg", "image/svg+xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "url", "application/internet-shortcut" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "sql", "text/plain" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xaction", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xanalyzer", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xcdf", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xdash", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xmi", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xml", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xreportspec", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "cda", "text/xml" ); //$NON-NLS-1$ //$NON-NLS-2$
    mimeTypes.put( "xls", "application/vnd.ms-excel" ); //$NON-NLS-1$ //$NON-NLS-2$   
    mimeTypes.put( null, null );
  }

  /**
   * Default constructor (does nothing)
   */
  public AbstractImportSource() {
  }

  /**
   * Returns the mime-type for the given file extension
   * 
   * @return the mime-type for the given file extension, or {@code null} if none is defined
   */
  protected String getMimeType( final String extension ) {
    return mimeTypes.get( extension );
  }
}
