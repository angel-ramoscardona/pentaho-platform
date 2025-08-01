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


package org.pentaho.platform.web.http.api.resources;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.pentaho.actionsequence.dom.IActionDefinition;
import org.pentaho.platform.api.engine.IActionParameter;
import org.pentaho.platform.api.engine.IActionSequence;
import org.pentaho.platform.api.engine.IMimeTypeListener;
import org.pentaho.platform.api.engine.IOutputHandler;
import org.pentaho.platform.api.engine.IParameterProvider;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.engine.IRuntimeContext;
import org.pentaho.platform.api.engine.ISolutionEngine;
import org.pentaho.platform.api.engine.ISystemSettings;
import org.pentaho.platform.api.engine.ObjectFactoryException;
import org.pentaho.platform.api.repository.IContentItem;
import org.pentaho.platform.api.repository2.unified.IUnifiedRepository;
import org.pentaho.platform.api.repository2.unified.RepositoryFile;
import org.pentaho.platform.api.repository2.unified.RepositoryFilePermission;
import org.pentaho.platform.engine.core.output.FileContentItem;
import org.pentaho.platform.engine.core.output.SimpleOutputHandler;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.services.ActionSequenceJCRHelper;
import org.pentaho.platform.engine.services.SoapHelper;
import org.pentaho.platform.repository2.unified.fileio.RepositoryFileContentItem;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.util.web.SimpleUrlFactory;
import org.pentaho.platform.web.http.HttpOutputHandler;
import org.pentaho.platform.web.http.MessageFormatUtils;
import org.pentaho.platform.web.http.messages.Messages;
import org.pentaho.platform.web.http.request.HttpRequestParameterProvider;
import org.pentaho.platform.web.http.session.HttpSessionParameterProvider;
import org.pentaho.platform.web.servlet.HttpMimeTypeListener;
import org.pentaho.reporting.libraries.base.util.StringUtils;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class XactionUtil {
  private static final Log logger = LogFactory.getLog( XactionUtil.class );

  @SuppressWarnings ( "rawtypes" )
  public static void createOutputFileName( RepositoryFile file, IOutputHandler outputHandler ) {
    IPentahoSession userSession = PentahoSessionHolder.getSession();
    ActionSequenceJCRHelper actionHelper = new ActionSequenceJCRHelper( userSession );
    IActionSequence actionSequence =
        actionHelper.getActionSequence( file.getPath(), PentahoSystem.loggingLevel, RepositoryFilePermission.READ );

    String fileName = "content"; //$NON-NLS-1$
    if ( actionSequence != null ) {
      String title = actionSequence.getTitle();
      if ( ( title != null ) && ( title.length() > 0 ) ) {
        fileName = title;
      } else {
        String sequenceName = actionSequence.getSequenceName();

        if ( ( sequenceName != null ) && ( sequenceName.length() > 0 ) ) {
          fileName = sequenceName;
        } else {
          List actionDefinitionsList = actionSequence.getActionDefinitionsAndSequences();
          int i = 0;
          boolean done = false;

          while ( ( actionDefinitionsList.size() > i ) && ( !done ) ) {
            IActionDefinition actionDefinition = (IActionDefinition) actionDefinitionsList.get( i );
            String componentName = actionDefinition.getComponentName();
            if ( ( componentName != null ) && ( componentName.length() > 0 ) ) {
              fileName = componentName;
              done = true;
            } else {
              ++i;
            }
          }
        }
      }
    }
    IMimeTypeListener mimeTypeListener = outputHandler.getMimeTypeListener();
    if ( mimeTypeListener != null ) {
      mimeTypeListener.setName( fileName );
    }
  }

  public static OutputStream getOutputStream( HttpServletResponse response, boolean doMessages )
    throws ServletException, IOException {
    OutputStream outputStream = null;
    if ( doMessages ) {
      outputStream = new ByteArrayOutputStream();
    } else {
      outputStream = response.getOutputStream();
    }

    return outputStream;
  }

  public static void setupOutputHandler( HttpOutputHandler outputHandler, IParameterProvider requestParameters ) {
    int outputPreference = IOutputHandler.OUTPUT_TYPE_DEFAULT;
    outputHandler.setOutputPreference( outputPreference );
  }

  public static HttpOutputHandler createOutputHandler( HttpServletResponse response, OutputStream outputStream ) {
    return new HttpOutputHandler( response, outputStream, true );
  }

  public static String postExecute( IRuntimeContext runtime, boolean debugMessages, boolean doWrapper,
                                    IOutputHandler outputHandler, Map<String, IParameterProvider> parameterProviders,
                                    HttpServletRequest request,
                                    HttpServletResponse response, List<?> messages, boolean deleteGeneratedFiles ) throws Exception {
    StringBuffer buffer = new StringBuffer();

    boolean hasResponse = outputHandler.isResponseExpected();
    IContentItem responseContentItem = outputHandler.getOutputContentItem( IOutputHandler.RESPONSE, IOutputHandler.CONTENT, null, null );

    boolean success = ( runtime != null && runtime.getStatus() == IRuntimeContext.RUNTIME_STATUS_SUCCESS );
    boolean printSuccess = ( runtime != null ) && success && ( !hasResponse || debugMessages );
    boolean printError = ( runtime != null ) && !success && !response.isCommitted();

    if ( printSuccess || printError ) {
      final String htmlMimeType = "text/html"; //$NON-NLS-1$
      responseContentItem.setMimeType( htmlMimeType );
      response.setContentType( htmlMimeType );

      if ( printSuccess ) {
        MessageFormatUtils.formatSuccessMessage( htmlMimeType, runtime, buffer, debugMessages, doWrapper );
      } else {
        response.resetBuffer();
        MessageFormatUtils.formatFailureMessage( htmlMimeType, runtime, buffer, messages );
      }
    }
    // clear files which was generated during action execution 
    // http://jira.pentaho.com/browse/BISERVER-12639
    IUnifiedRepository unifiedRepository = PentahoSystem.get( IUnifiedRepository.class, null );
    if ( unifiedRepository != null ) {
      for ( IContentItem contentItem : runtime.getOutputContentItems() ) {
        if ( contentItem != null ) {
          try {
            contentItem.closeOutputStream();
            if ( deleteGeneratedFiles ) {
              deleteContentItem( contentItem, unifiedRepository );
            }
          } catch ( Exception e ) {
            logger.warn( Messages.getInstance().getString( "XactionUtil.CANNOT_REMOVE_OUTPUT_FILE", contentItem.getPath() ), e );
          }
        }
      }
    }
    return buffer.toString();
  }

  static void deleteContentItem( IContentItem contentItem, IUnifiedRepository unifiedRepository ) {
    if ( contentItem instanceof RepositoryFileContentItem ) {
      String path = contentItem.getPath();
      RepositoryFile repositoryFile = unifiedRepository.getFile( path );
      //repositoryFile can be null if we have not access or file does not exist
      if ( repositoryFile != null ) {
        unifiedRepository.deleteFile( repositoryFile.getId(), true, null );
      }
    } else if ( contentItem instanceof FileContentItem ) {
      // Files in the file system must not be deleted here
      String path = ( (FileContentItem) contentItem ).getFile().getName();
      logger.warn( Messages.getInstance().getString( "XactionUtil.SKIP_REMOVING_OUTPUT_FILE", path ) );
    }

  }

  @SuppressWarnings ( { "unchecked", "rawtypes" } )
  public static String executeHtml( RepositoryFile file, HttpServletRequest httpServletRequest,
                                    HttpServletResponse httpServletResponse, IPentahoSession userSession,
                                    IMimeTypeListener mimeTypeListener )
    throws Exception {
    IParameterProvider requestParams = new HttpRequestParameterProvider( httpServletRequest );
    IRuntimeContext runtime = null;
    try {

      HttpSessionParameterProvider sessionParameters = new HttpSessionParameterProvider( userSession );
      HttpRequestParameterProvider requestParameters = new HttpRequestParameterProvider( httpServletRequest );

      boolean doMessages = "true".equalsIgnoreCase( requestParams.getStringParameter( "debug", "false" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      boolean doWrapper = "true".equalsIgnoreCase( requestParams.getStringParameter( "wrapper", "true" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      IOutputHandler outputHandler = createOutputHandler( httpServletResponse, getOutputStream( httpServletResponse, doMessages ) );

      // configure output handler, this is necessary so that the right content
      // disposition is set on the response header
      if ( mimeTypeListener == null ) {
        mimeTypeListener = new HttpMimeTypeListener( httpServletRequest, httpServletResponse, null );
      }
      outputHandler.setMimeTypeListener( mimeTypeListener );
      outputHandler.setSession( userSession );

      Map parameterProviders = new HashMap();
      parameterProviders.put( "request", requestParameters ); //$NON-NLS-1$
      parameterProviders.put( "session", sessionParameters ); //$NON-NLS-1$
      createOutputFileName( file, outputHandler );
      int outputPreference = IOutputHandler.OUTPUT_TYPE_DEFAULT;
      outputHandler.setOutputPreference( outputPreference );
      boolean forcePrompt = "true".equalsIgnoreCase( requestParams.getStringParameter( "prompt", "false" ) ); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

      List messages = new ArrayList();
      runtime =
        executeInternal( file, requestParams, httpServletRequest, outputHandler, parameterProviders, userSession,
          forcePrompt, messages );
      String str = postExecute( runtime, doMessages, doWrapper, outputHandler, parameterProviders, httpServletRequest,
          httpServletResponse, messages, true );
      return str;
    } catch ( Exception e ) {
      logger.error( Messages.getInstance().getString( "XactionUtil.ERROR_EXECUTING_ACTION_SEQUENCE", file.getName() ), e ); //$NON-NLS-1$
      throw e;
    } finally {
      if ( runtime != null ) {
        runtime.dispose();
      }

    }
  }

  /**
   * This method executes an xaction with forcePrompt=true and outputPreference=PARAMETERS, allowing for the xaction to
   * render the secure filter appropriately when being executed in the background or while being scheduled.
   *
   * @param file                the location of the xaction
   * @param httpServletRequest  the request object
   * @param httpServletResponse the response object
   * @param userSession         the user session
   * @return potential response message
   * @throws Exception
   */
  @SuppressWarnings ( { "unchecked", "rawtypes" } )
  public static String executeScheduleUi( RepositoryFile file, HttpServletRequest httpServletRequest,
                                          HttpServletResponse httpServletResponse, IPentahoSession userSession,
                                          IMimeTypeListener mimeTypeListener )
    throws Exception {
    IParameterProvider requestParams = new HttpRequestParameterProvider( httpServletRequest );
    IRuntimeContext runtime = null;
    try {
      HttpSessionParameterProvider sessionParameters = new HttpSessionParameterProvider( userSession );
      HttpRequestParameterProvider requestParameters = new HttpRequestParameterProvider( httpServletRequest );

      boolean doMessages = "true".equalsIgnoreCase( requestParams.getStringParameter( "debug", "false" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      boolean doWrapper = "true".equalsIgnoreCase( requestParams.getStringParameter( "wrapper", "true" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$

      IOutputHandler outputHandler = createOutputHandler( httpServletResponse, getOutputStream( httpServletResponse, doMessages ) );
      if ( mimeTypeListener == null ) {
        mimeTypeListener = new HttpMimeTypeListener( httpServletRequest, httpServletResponse, null );
      }
      outputHandler.setMimeTypeListener( mimeTypeListener );
      outputHandler.setSession( userSession );

      Map parameterProviders = new HashMap();
      parameterProviders.put( "request", requestParameters ); //$NON-NLS-1$
      parameterProviders.put( "session", sessionParameters ); //$NON-NLS-1$
      createOutputFileName( file, outputHandler );
      int outputPreference = IOutputHandler.OUTPUT_TYPE_PARAMETERS;
      outputHandler.setOutputPreference( outputPreference );
      List messages = new ArrayList();

      // forcePrompt=true when displaying the scheduling UI
      runtime =
        executeInternal( file, requestParams, httpServletRequest, outputHandler, parameterProviders, userSession,
          true, messages );
      String str = postExecute( runtime, doMessages, doWrapper, outputHandler, parameterProviders, httpServletRequest,
          httpServletResponse, messages, false );
      return str;
    } catch ( Exception e ) {
      logger.error( Messages.getInstance().getString( "XactionUtil.ERROR_EXECUTING_ACTION_SEQUENCE", file.getName() ), e ); //$NON-NLS-1$
      throw e;
    } finally {
      if ( runtime != null ) {
        runtime.dispose();
      }
    }
  }

  @SuppressWarnings ( "rawtypes" )
  protected static IRuntimeContext executeInternal( RepositoryFile file, IParameterProvider requestParams,
                                                    HttpServletRequest httpServletRequest, IOutputHandler outputHandler,
                                                    Map<String, IParameterProvider> parameterProviders,
                                                    IPentahoSession userSession, boolean forcePrompt,
                                                    List messages ) throws Exception {
    String processId = XactionUtil.class.getName();
    String instanceId = httpServletRequest.getParameter( "instance-id" ); //$NON-NLS-1$
    SimpleUrlFactory urlFactory = new SimpleUrlFactory( "" ); //$NON-NLS-1$
    ISolutionEngine solutionEngine = PentahoSystem.get( ISolutionEngine.class, userSession );
    ISystemSettings systemSettings = PentahoSystem.getSystemSettings();

    if ( solutionEngine == null ) {
      throw new ObjectFactoryException( "No Solution Engine" );
    }

    boolean instanceEnds = "true".equalsIgnoreCase( requestParams.getStringParameter( "instanceends", "true" ) ); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    String parameterXsl = systemSettings.getSystemSetting( "default-parameter-xsl", "DefaultParameterForm.xsl" ); //$NON-NLS-1$ //$NON-NLS-2$

    solutionEngine.setLoggingLevel( 2 );
    solutionEngine.init( userSession );
    solutionEngine.setForcePrompt( forcePrompt );
    if ( parameterXsl != null ) {
      solutionEngine.setParameterXsl( parameterXsl );
    }
    return solutionEngine.execute( file.getPath(), processId, false, instanceEnds, instanceId, false,
      parameterProviders, outputHandler, null, urlFactory, messages );
  }

  @SuppressWarnings ( { "unchecked", "rawtypes" } )
  public static String executeXml( RepositoryFile file, HttpServletRequest httpServletRequest,
                                   HttpServletResponse httpServletResponse, IPentahoSession userSession )
    throws Exception {
    try {
      HttpSessionParameterProvider sessionParameters = new HttpSessionParameterProvider( userSession );
      HttpRequestParameterProvider requestParameters = new HttpRequestParameterProvider( httpServletRequest );
      Map parameterProviders = new HashMap();
      parameterProviders.put( "request", requestParameters ); //$NON-NLS-1$
      parameterProviders.put( "session", sessionParameters ); //$NON-NLS-1$
      List messages = new ArrayList();
      IParameterProvider requestParams = new HttpRequestParameterProvider( httpServletRequest );
      httpServletResponse.setContentType( "text/xml" ); //$NON-NLS-1$
      httpServletResponse.setCharacterEncoding( LocaleHelper.getSystemEncoding() );
      boolean forcePrompt = "true".equalsIgnoreCase( requestParams.getStringParameter( "prompt", "false" ) ); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

      OutputStream contentStream = new ByteArrayOutputStream();
      SimpleOutputHandler outputHandler = new SimpleOutputHandler( contentStream, false );
      IRuntimeContext runtime = null;
      try {
        runtime =
          executeInternal( file, requestParams, httpServletRequest, outputHandler, parameterProviders, userSession,
            forcePrompt, messages );
        Document responseDoc = SoapHelper.createSoapResponseDocument( runtime, outputHandler, contentStream, messages );
        OutputFormat format = OutputFormat.createCompactFormat();
        format.setSuppressDeclaration( true );
        format.setEncoding( "utf-8" ); //$NON-NLS-1$
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        XMLWriter writer = new XMLWriter( outputStream, format );
        writer.write( responseDoc );
        writer.flush();
        return outputStream.toString( "utf-8" ); //$NON-NLS-1$
      } finally {
        if ( runtime != null ) {
          runtime.dispose();
        }
      }
    } catch ( Exception e ) {
      logger.warn( Messages.getInstance().getString( "XactionUtil.XML_OUTPUT_NOT_SUPPORTED" ) ); //$NON-NLS-1$
      throw e;
    }
  }

  public static String execute( String returnContentType, RepositoryFile file, HttpServletRequest httpServletRequest,
                                HttpServletResponse httpServletResponse, IPentahoSession userSession,
                                IMimeTypeListener mimeTypeListener )
    throws Exception {
    if ( ( returnContentType != null ) && ( returnContentType.equals( MediaType.APPLICATION_XML ) ) ) {
      return executeXml( file, httpServletRequest, httpServletResponse, userSession );
    }
    return executeHtml( file, httpServletRequest, httpServletResponse, userSession, mimeTypeListener );
  }

  @SuppressWarnings ( { "unchecked", "rawtypes" } )
  public static String doParameter( final RepositoryFile file, IParameterProvider parameterProvider,
                                    final IPentahoSession userSession ) throws IOException {
    ActionSequenceJCRHelper helper = new ActionSequenceJCRHelper();
    final IActionSequence actionSequence = helper.getActionSequence( file.getPath(),
        PentahoSystem.loggingLevel, RepositoryFilePermission.READ );
    final Document document = DocumentHelper.createDocument();
    try {
      final Element parametersElement = document.addElement( "parameters" );

      // noinspection unchecked
      final Map<String, IActionParameter> params =
          actionSequence.getInputDefinitionsForParameterProvider( IParameterProvider.SCOPE_REQUEST );
      for ( final Map.Entry<String, IActionParameter> entry : params.entrySet() ) {
        final String paramName = entry.getKey();
        final IActionParameter paramDef = entry.getValue();
        final String value = paramDef.getStringValue();
        final Class type;
        // yes, the actual type-code uses equals-ignore-case and thus allows the user
        // to specify type information in a random case. sTrInG is equal to STRING is equal to the value
        // defined as constant (string)
        if ( IActionParameter.TYPE_LIST.equalsIgnoreCase( paramDef.getType() ) ) {
          type = String[].class;
        } else {
          type = String.class;
        }
        final String label = paramDef.getSelectionDisplayName();

        final String[] values;
        if ( StringUtils.isEmpty( value ) ) {
          values = new String[ 0 ];
        } else {
          values = new String[] { value };
        }

        createParameterElement( parametersElement, paramName, type, label, "user", "parameters", values );
      }

      createParameterElement( parametersElement, "path", String.class, null, "system", "system", new String[] { file.getPath() } );
      createParameterElement( parametersElement, "prompt", String.class, null, "system", "system", new String[] { "yes", "no" } );
      createParameterElement( parametersElement, "instance-id", String.class, null, "system", "system",
          new String[] { parameterProvider.getStringParameter( "instance-id", null ) } );
      // no close, as far as I know tomcat does not like it that much ..
      OutputFormat format = OutputFormat.createCompactFormat();
      format.setSuppressDeclaration( true );
      format.setEncoding( "utf-8" ); //$NON-NLS-1$
      ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
      XMLWriter writer = new XMLWriter( outputStream, format );
      writer.write( document );
      writer.flush();
      return outputStream.toString( "utf-8" );
    } catch ( Exception e ) {
      logger.warn( Messages.getInstance().getString( "HttpWebService.ERROR_0003_UNEXPECTED" ), e );
      return null;
    }
  }

  @SuppressWarnings ( "rawtypes" )
  private static Element createParameterElement( final Element parametersElement, final String paramName,
                                                 final Class type, final String label, final String role,
                                                 final String group, final String[] values ) {
    final Element parameterElement = parametersElement.addElement( "parameter" );
    parameterElement.addAttribute( "name", paramName );
    parameterElement.addAttribute( "type", type.getName() );

    if ( StringUtils.isEmpty( label ) == false ) {
      final Element labelAttr = parameterElement.addElement( "attribute" );
      labelAttr.addAttribute( "namespace", "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core" );
      labelAttr.addAttribute( "name", "label" );
      labelAttr.addAttribute( "value", label );
    }

    final Element roleAttr = parameterElement.addElement( "attribute" );
    roleAttr.addAttribute( "namespace", "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core" );
    roleAttr.addAttribute( "name", "role" );
    roleAttr.addAttribute( "value", role );

    final Element paramGroupAttr = parameterElement.addElement( "attribute" );
    paramGroupAttr.addAttribute( "namespace", "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core" );
    paramGroupAttr.addAttribute( "name", "parameter-group" );
    paramGroupAttr.addAttribute( "value", group );

    final Element paramGroupLabelAttr = parameterElement.addElement( "attribute" );
    paramGroupLabelAttr.addAttribute( "namespace", "http://reporting.pentaho.org/namespaces/engine/parameter-attributes/core" );
    paramGroupLabelAttr.addAttribute( "name", "parameter-group-label" );
    paramGroupLabelAttr.addAttribute( "value", lookupParameterGroupLabel( group ) );

    if ( values.length > 0 ) {
      final Element valuesElement = parameterElement.addElement( "values" );
      for ( final String value : values ) {
        final Element valueAttr = valuesElement.addElement( "value" );
        valueAttr.addAttribute( "type", String.class.getName() );
        valueAttr.addAttribute( "value", value );
        valueAttr.addAttribute( "selected", String.valueOf( values.length == 1 ) );
      }
    }
    return parameterElement;
  }

  private static String lookupParameterGroupLabel( final String group ) {
    if ( "system".equals( group ) ) {
      return Messages.getInstance().getString( "HttpWebService.PARAMETER_GROUP_SYSTEM" );
    }
    return Messages.getInstance().getString( "HttpWebService.PARAMETER_GROUP_USER" );
  }
}
