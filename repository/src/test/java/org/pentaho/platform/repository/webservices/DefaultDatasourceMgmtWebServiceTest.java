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


package org.pentaho.platform.repository.webservices;

import junit.framework.TestCase;
import org.junit.Test;
import org.mockito.Mockito;
import org.pentaho.database.model.DatabaseAccessType;
import org.pentaho.database.model.DatabaseConnection;
import org.pentaho.database.model.IDatabaseType;
import org.pentaho.database.service.DatabaseDialectService;
import org.pentaho.di.core.KettleClientEnvironment;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFileAcl;
import org.pentaho.platform.repository.JcrBackedDatasourceMgmtService;
import org.pentaho.test.platform.repository2.unified.MockUnifiedRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.ArrayList;
import java.util.List;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.pentaho.platform.api.repository2.unified.RepositoryFilePermission.READ;
import static org.pentaho.platform.api.repository2.unified.RepositoryFilePermission.WRITE;

public class DefaultDatasourceMgmtWebServiceTest extends TestCase {

  private static final String EXP_DBMETA_NAME = "haha";

  private static final String EXP_DBMETA_HOSTNAME = "acme";

  private static final String EXP_DBMETA_PORT = "10521";

  private static final String EXP_UPDATED_DBMETA_NAME = "hahaUpdated";

  private static final String EXP_UPDATED_DBMETA_HOSTNAME = "acmeUpdated";

  private static final String EXP_UPDATED_DBMETA_PORT = "10522";

  public static final String EXP_LOGIN = "admin";

  private static final String FOLDER_PDI = "pdi";

  private static final String FOLDER_DATABASES = "databases"; //$NON-NLS-1$
  private JcrBackedDatasourceMgmtService datasourceMgmtService;
  private IUnifiedRepository repository;
  private IDatasourceMgmtWebService datasourceMgmtWebService;
  private DatabaseConnectionAdapter dbConnectionAdapter;

  public void setUp() throws Exception {
    repository =
      new MockUnifiedRepository( new MockUnifiedRepository.SpringSecurityCurrentUserProvider() );
    SecurityContextHolder.getContext()
      .setAuthentication(
        new UsernamePasswordAuthenticationToken( MockUnifiedRepository.root().getName(), null,
          new ArrayList<GrantedAuthority>() ) );
    repository.createFolder( repository.getFile( "/etc" ).getId(), new RepositoryFile.Builder( FOLDER_PDI ).folder(
      true ).build(), new RepositoryFileAcl.Builder( MockUnifiedRepository.root() ).ace(
      MockUnifiedRepository.everyone(), READ, WRITE ).build(), null );
    repository.createFolder( repository.getFile( "/etc/pdi" ).getId(), new RepositoryFile.Builder( FOLDER_DATABASES )
      .folder( true ).build(), null );
    SecurityContextHolder.getContext().setAuthentication(
      new UsernamePasswordAuthenticationToken( EXP_LOGIN, null, new ArrayList<GrantedAuthority>() ) );

    KettleClientEnvironment.init();

  }

  @Test
  public void testEverything() throws Exception {
    datasourceMgmtService = Mockito.spy( new JcrBackedDatasourceMgmtService( repository, new DatabaseDialectService() ) );
    when( datasourceMgmtService.hasDataAccessPermission() ).thenReturn( true );
    datasourceMgmtWebService = new DefaultDatasourceMgmtWebService( datasourceMgmtService );
    dbConnectionAdapter = new DatabaseConnectionAdapter();
    DatabaseConnection databaseConnection = createDatabaseConnection( "testDatabase" );
    String id = datasourceMgmtWebService.createDatasource( dbConnectionAdapter.marshal( databaseConnection ) );
    assertNotNull( id );
    DatabaseConnectionDto databaseConnectionDto = datasourceMgmtWebService.getDatasourceByName( "testDatabase" );
    assertNotNull( databaseConnectionDto );
    assertEquals( EXP_DBMETA_HOSTNAME, databaseConnectionDto.getHostname() );
    DatabaseConnectionDto databaseConnectionDto1 = datasourceMgmtWebService.getDatasourceById( id );
    assertNotNull( databaseConnectionDto1 );
    assertEquals( EXP_DBMETA_HOSTNAME, databaseConnectionDto1.getHostname() );
    DatabaseConnection databaseConnection1 = createDatabaseConnection( "testDatabase1" );
    String id1 = datasourceMgmtWebService.createDatasource( dbConnectionAdapter.marshal( databaseConnection1 ) );
    assertNotNull( id1 );
    DatabaseConnectionDto databaseConnectionDto2 = datasourceMgmtWebService.getDatasourceByName( "testDatabase1" );
    assertNotNull( databaseConnectionDto2 );
    assertEquals( EXP_DBMETA_HOSTNAME, databaseConnectionDto2.getHostname() );
    DatabaseConnectionDto databaseConnectionDto3 = datasourceMgmtWebService.getDatasourceById( id1 );
    assertNotNull( databaseConnectionDto3 );
    assertEquals( EXP_DBMETA_HOSTNAME, databaseConnectionDto3.getHostname() );
    List<DatabaseConnectionDto> databaseConnectionDtos = datasourceMgmtWebService.getDatasources();
    assertEquals( 2, databaseConnectionDtos.size() );
    List<String> ids = datasourceMgmtWebService.getDatasourceIds();
    assertEquals( 2, ids.size() );
    databaseConnection = dbConnectionAdapter.unmarshal( databaseConnectionDto );
    updateDatabaseConnection( databaseConnection );
    String id2 =
      datasourceMgmtWebService.updateDatasourceByName( "testDatabase", dbConnectionAdapter
        .marshal( databaseConnection ) );
    assertNotNull( id2 );
    DatabaseConnectionDto updatedDatabaseConnectionDto = datasourceMgmtWebService.getDatasourceByName( "testDatabase" );
    assertNotNull( updatedDatabaseConnectionDto );
    assertEquals( EXP_UPDATED_DBMETA_HOSTNAME, updatedDatabaseConnectionDto.getHostname() );
    databaseConnection1 = dbConnectionAdapter.unmarshal( databaseConnectionDto2 );
    updateDatabaseConnection( databaseConnection1 );
    String id3 =
      datasourceMgmtWebService.updateDatasourceById( id1, dbConnectionAdapter.marshal( databaseConnection1 ) );
    assertNotNull( id3 );
    DatabaseConnectionDto updatedDatabaseConnectionDto1 = datasourceMgmtWebService.getDatasourceById( id3 );
    assertNotNull( updatedDatabaseConnectionDto1 );
    assertEquals( EXP_UPDATED_DBMETA_HOSTNAME, updatedDatabaseConnectionDto1.getHostname() );
    datasourceMgmtWebService.deleteDatasourceByName( "testDatabase" );
    DatabaseConnectionDto deletedDatabaseConnectionDto = datasourceMgmtWebService.getDatasourceByName( "testDatabase" );
    assertNull( deletedDatabaseConnectionDto );
    datasourceMgmtWebService.deleteDatasourceById( id3 );
    DatabaseConnectionDto deletedDatabaseConnectionDto2 = datasourceMgmtWebService.getDatasourceById( id3 );
    assertNull( deletedDatabaseConnectionDto2 );
    ids = datasourceMgmtWebService.getDatasourceIds();
    assertEquals( 0, ids.size() );
  }

  private DatabaseConnection createDatabaseConnection( final String dbName ) throws Exception {
    DatabaseConnection dbConnection = new DatabaseConnection();
    dbConnection.setName( dbName );
    dbConnection.setHostname( EXP_DBMETA_HOSTNAME );
    dbConnection.setDatabaseType( mockDatabaseType( "Hypersonic" ) );
    dbConnection.setAccessType( DatabaseAccessType.NATIVE );
    dbConnection.setDatabasePort( EXP_DBMETA_PORT );
    return dbConnection;
  }

  private void updateDatabaseConnection( DatabaseConnection dbConnection ) throws Exception {
    dbConnection.setHostname( EXP_UPDATED_DBMETA_HOSTNAME );
    dbConnection.setDatabaseType( mockDatabaseType( "Generic database" ) );
    dbConnection.setAccessType( DatabaseAccessType.JNDI );
    dbConnection.setDatabasePort( EXP_UPDATED_DBMETA_PORT );
  }

  private IDatabaseType mockDatabaseType( final String shortName ) {
    IDatabaseType dbType = mock( IDatabaseType.class );
    doReturn( shortName ).when( dbType ).getShortName();
    return dbType;
  }
}