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


package org.pentaho.platform.plugin.services.importexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang.StringUtils;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.glassfish.jersey.test.DeploymentContext;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.ServletDeploymentContext;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.pentaho.platform.api.engine.IAuthorizationAction;
import org.pentaho.platform.api.engine.IAuthorizationPolicy;
import org.pentaho.platform.api.engine.IPentahoDefinableObjectFactory;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.repository2.unified.IRepositoryContentConverterHandler;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.engine.services.solution.SolutionEngine;
import org.pentaho.platform.plugin.services.importer.NameBaseMimeResolver;
import org.pentaho.platform.plugin.services.messages.Messages;
import org.pentaho.platform.repository2.unified.fs.FileSystemBackedUnifiedRepository;
import org.pentaho.platform.security.policy.rolebased.actions.AdministerSecurityAction;
import org.pentaho.platform.web.http.filters.PentahoRequestContextFilter;
import org.pentaho.test.platform.engine.core.MicroPlatform;

import org.pentaho.test.platform.utils.TestResourceLocation;

/**
 * Class Description
 *
 * @author <a href="mailto:dkincade@pentaho.com">David M. Kincade</a>
 */
public class CommandLineProcessorIT extends JerseyTest {
  private static String VALID_URL_OPTION = "--url=http://localhost:8080/pentaho";
  private static String VALID_IMPORT_COMMAND_LINE = "--import --username=admin "
      + "--password=password --charset=UTF-8 --path=/public "
      + "--file-path=/home/dkincade/pentaho/platform/trunk/biserver-ee/pentaho-solutions " + VALID_URL_OPTION;

  private String tmpZipFileName;
  private static ResourceConfig config = new ResourceConfig().packages( "org.pentaho.platform.web.http.api.resources" );
  private static ServletDeploymentContext servletDeploymentContext = ServletDeploymentContext.forServlet( new ServletContainer( config ) )
     .addFilter( PentahoRequestContextFilter.class, "pentahoRequestContextFilter" ).contextPath( "api" )
     .build();
  private MicroPlatform mp = new MicroPlatform( TestResourceLocation.TEST_RESOURCES + "/solution" );

  public CommandLineProcessorIT() throws IOException {
    final TemporaryFolder tmpFolder = new TemporaryFolder();
    tmpFolder.create();
    tmpZipFileName = tmpFolder.getRoot().getAbsolutePath() + File.separator + "test.zip";

    NameBaseMimeResolver mimeResolver = mock( NameBaseMimeResolver.class );
    IRepositoryContentConverterHandler converterHandler = mock( IRepositoryContentConverterHandler.class );

    mp.setFullyQualifiedServerUrl( getBaseUri() + servletDeploymentContext.getContextPath() + "/" );
    mp.define( ISolutionEngine.class, SolutionEngine.class );
    mp.define( IUnifiedRepository.class, FileSystemBackedUnifiedRepository.class,
        IPentahoDefinableObjectFactory.Scope.GLOBAL );
    mp.define( IAuthorizationPolicy.class, TestAuthorizationPolicy.class );
    mp.define( IAuthorizationAction.class, AdministerSecurityAction.class );
    mp.define( DefaultExportHandler.class, DefaultExportHandler.class );
    mp.defineInstance( IRepositoryContentConverterHandler.class, converterHandler );
    mp.defineInstance( NameBaseMimeResolver.class, mimeResolver );

    FileSystemBackedUnifiedRepository repo =
        (FileSystemBackedUnifiedRepository) PentahoSystem.get( IUnifiedRepository.class );
    repo.setRootDir( new File( TestResourceLocation.TEST_RESOURCES + "/solution" ) );

    StandaloneSession session = new StandaloneSession();
    PentahoSessionHolder.setStrategyName( PentahoSessionHolder.MODE_GLOBAL );
    PentahoSessionHolder.setSession( session );
  }

  private static final String[] toStringArray( final String s ) {
    return StringUtils.split( s, ' ' );
  }

  private static final String[] toStringArray( final String s1, final String s2 ) {
    return StringUtils.split( s1 + " " + s2, ' ' );
  }

  @Test
  public void testInvalidCommandLineParameters() throws Exception {
    CommandLineProcessor.main( new String[] {} );
    assertEquals( ParseException.class, CommandLineProcessor.getException().getClass() );

    CommandLineProcessor.main( toStringArray( VALID_IMPORT_COMMAND_LINE, "--export" ) );
    assertEquals( ParseException.class, CommandLineProcessor.getException().getClass() );

    CommandLineProcessor.main( toStringArray( "--help" ) );
    assertNull( CommandLineProcessor.getException() );

    CommandLineProcessor.main( toStringArray( VALID_IMPORT_COMMAND_LINE.replace( VALID_URL_OPTION, "" ) ) );
    assertEquals( ParseException.class, CommandLineProcessor.getException().getClass() );
  }

/* Having these empty tests causes the micro platform to stand up for each... taking a second
  @Test
  public void testGetProperty() throws Exception {

  }

  @Test
  public void testGetOptionValue() throws Exception {

  }

  @Test
  public void testGetImportProcessor() throws Exception {

  }

  @Test
  public void testGetImportSource() throws Exception {

  }

  @Test
  public void testAddImportHandlers() throws Exception {

  }
*/
  @Test
  public void testExportFileParameter() throws Exception {
    final String baseOptions = "-e -a " + getBaseUrl() + " -u admin -p password -f \"/test\"";
    String fileOption;

    // correct file path
    fileOption = "-fp " + tmpZipFileName;
    CommandLineProcessor.main( toStringArray( baseOptions, fileOption ) );
    assertNull( CommandLineProcessor.getException() );

    // incorrect file path
    fileOption = "-fp test.zip";
    CommandLineProcessor.main( toStringArray( baseOptions, fileOption ) );
    assertEquals( ParseException.class, CommandLineProcessor.getException().getClass() );
  }

  @Test
  public void testExportPathParameter() throws Exception {
    final String baseOptions = "-e -a " + getBaseUrl() + " -u admin -p password -fp " + tmpZipFileName;
    String pathOption;

    //correct path
    pathOption = "-f \"/test\"";
    CommandLineProcessor.main( toStringArray( baseOptions, pathOption ) );
    assertNull( CommandLineProcessor.getException() );

    //path with trailing slash
    pathOption = "-f \"/test/\"";
    CommandLineProcessor.main( toStringArray( baseOptions, pathOption ) );
    assertNull( CommandLineProcessor.getException() );

    //path that doesn't exist
    pathOption = "-f \"/path_that_not_exists\"";
    CommandLineProcessor.main( toStringArray( baseOptions, pathOption ) );
    assertEquals( Messages.getInstance().getErrorString( "CommandLineProcessor.ERROR_0004_UNKNOWN_SOURCE",
        "/path_that_not_exists" ), CommandLineProcessor.getErrorMessage() );
  }

  @Test
  public void testExportAll() throws Exception {
    final String baseOptions = "-e -a " + getBaseUrl() + " -u admin -p password -fp " + tmpZipFileName + " -f \"/\"";

    CommandLineProcessor.main( toStringArray( baseOptions ) );
    assertNull( CommandLineProcessor.getException() );
  }


  @Override
  protected DeploymentContext configureDeployment() {
    return servletDeploymentContext;
  }

  @Override
  protected TestContainerFactory getTestContainerFactory() {
    return new GrizzlyWebTestContainerFactory();
  }

  private String getBaseUrl() {
    String baseUrl = getBaseUri().toString();
    if ( baseUrl.endsWith( "/" ) ) {
      baseUrl = baseUrl.substring( 0, baseUrl.length() - 1 );
    }
    return baseUrl;
  }
}
