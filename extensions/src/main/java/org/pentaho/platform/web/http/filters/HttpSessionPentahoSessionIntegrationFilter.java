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


package org.pentaho.platform.web.http.filters;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.api.util.ITempFileDeleter;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.engine.core.system.StandaloneSession;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.pentaho.platform.web.http.session.PentahoHttpSession;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.security.authentication.AuthenticationProvider;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.SessionCookieConfig;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServletResponseWrapper;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

/**
 * Populates the {@link PentahoSessionHolder} with information obtained from the <code>HttpSession</code>.
 *
 * <p>
 * Originally this functionality existed in PentahoHttpRequestListener but has been moved here. Javadoc for that class:
 * </p>
 *
 * <blockquote> In a J2EE environment, sets the Hitachi Vantara session statically per request so the session can be retrieved
 * by other consumers within the same request without having it passed to them explicitly. -- aphillips </blockquote>
 *
 * <p>
 * There are two reasons that this is a {@link Filter} and not a {@code ServletRequestListener}:
 * </p>
 * <ul>
 * <li>Filters are compatible with Servlet 2.3 web applications.</li>
 * <li>Filters can be ordered.</li>
 * </ul>
 *
 * <p>
 * This implementation is based on {@code org.springframework.security.context.HttpSessionContextIntegrationFilter}.
 * </p>
 *
 * <p>
 * The <code>HttpSession</code> will be queried to retrieve the <code>IPentahoSession</code> that should be stored
 * against the <code>PentahoSessionHolder</code> for the duration of the web request. At the end of the web request,
 * any updates made to the <code>PentahoSessionHolder</code> will be persisted back to the <code>HttpSession</code> by
 * this filter.
 * </p>
 * <p/>
 * No <code>HttpSession</code> will be created by this filter if one does not already exist. If at the end of the web
 * request the <code>HttpSession</code> does not exist, a <code>HttpSession</code> will <b>only</b> be created if the
 * current Hitachi Vantara session in <code>PentahoSessionHolder</code> is not null. This avoids needless
 * <code>HttpSession</code> creation, but automates the storage of changes made to the <code>PentahoSessionHolder</code>
 * . There is one exception to this rule, that is if the {@link #forceEagerSessionCreation} property is
 * <code>true</code>, in which case sessions will always be created irrespective of normal session-minimization logic
 * (the default is <code>false</code>, as this is resource intensive and not recommended).
 * </p>
 * <p/>
 * This filter will only execute once per request, to resolve servlet container (specifically Weblogic)
 * incompatibilities.
 * </p>
 * <p/>
 * If for whatever reason no <code>HttpSession</code> should <b>ever</b> be created (eg this filter is only being used
 * with Basic authentication or similar clients that will never present the same <code>jsessionid</code> etc), the
 * {@link #setAllowSessionCreation(boolean)} should be set to <code>false</code>. Only do this if you really need to
 * conserve server memory and ensure all classes using the <code>PentahoSessionHolder</code> are designed to have no
 * persistence of the Hitachi Vantara session between web requests. Please note that if {@link #forceEagerSessionCreation} is
 * <code>true</code>, the <code>allowSessionCreation</code> must also be <code>true</code> (setting it to
 * <code>false</code> will cause a startup time error).
 * </p>
 * <p/>
 * This filter MUST be executed BEFORE any code that expects the <code>PentahoSessionHolder</code> to contain a valid
 * <code>IPentahoSession</code> by the time they execute.
 * </p>
 */
public class HttpSessionPentahoSessionIntegrationFilter implements Filter, InitializingBean {
  // ~ Static fields/initializers =====================================================================================

  private static final Log logger = LogFactory.getLog( HttpSessionPentahoSessionIntegrationFilter.class );

  static final String FILTER_APPLIED = "__pentaho_session_integration_filter_applied"; //$NON-NLS-1$

  // ~ Instance fields ================================================================================================

  /**
   * Indicates if this filter can create a <code>HttpSession</code> if needed (sessions are always created sparingly,
   * but setting this value to <code>false</code> will prohibit sessions from ever being created). Defaults to
   * <code>true</code>. Do not set to <code>false</code> if you are have set {@link #forceEagerSessionCreation} to
   * <code>true</code>, as the properties would be in conflict.
   */
  private boolean allowSessionCreation = true;

  /**
   * Indicates if this filter is required to create a <code>HttpSession</code> for every request before proceeding
   * through the filter chain, even if the <code>HttpSession</code> would not ordinarily have been created. By default
   * this is <code>false</code>, which is entirely appropriate for most circumstances as you do not want a
   * <code>HttpSession</code> created unless the filter actually needs one. It is envisaged the main situation in which
   * this property would be set to <code>true</code> is if using other filters that depend on a <code>HttpSession</code>
   * already existing, such as those which need to obtain a session ID. This is only required in specialised cases, so
   * leave it set to <code>false</code> unless you have an actual requirement and are conscious of the session creation
   * overhead.
   */
  private boolean forceEagerSessionCreation = false;

  /**
   * Lazily initialized since PentahoSystem isn't available when this class is constructed.
   */
  private static String anonymousUser;

  /**
   * If true (the default), call {@link IPentahoSession#setAuthenticated(String)} on new {@code IPentahoSession}s where
   * argument is value from {@code /pentaho-system/anonymous-authentication/anonymous-user} from {@code pentaho.xml}.
   * Otherwise, {@link IPentahoSession#setAuthenticated(String)} is not called. This is necessary for code that calls
   * {@link IPentahoSession#isAuthenticated()} in anonymous-only or mixed (i.e. anonymous and non-anonymous)
   * environments. Even if not in anonymous or mixed environment, this can be true--access must still be given to
   * anonymous users for URLs and ACLs--hence the default value of true.
   */
  protected boolean callSetAuthenticatedForAnonymousUsers = true;

  private boolean ssoEnabled = false;

  // ~ Methods ========================================================================================================

  /**
   * Does nothing. We use IoC container lifecycle services instead.
   *
   * @param filterConfig
   *          ignored
   * @throws ServletException
   *           ignored
   */
  @Override
  public void init( FilterConfig filterConfig ) throws ServletException {
    // Does nothing. We use IoC container lifecycle services instead.
  }

  /**
   * Does nothing. We use IoC container lifecycle services instead.
   */
  @Override
  public void destroy() {
    // Does nothing. We use IoC container lifecycle services instead.
  }

  public void afterPropertiesSet() throws Exception {
    if ( forceEagerSessionCreation && !allowSessionCreation ) {
      throw new IllegalArgumentException(
        "If using forceEagerSessionCreation, you must set allowSessionCreation to also be true" );
    }
  }

  protected IPentahoSession generatePentahoSession( final HttpServletRequest httpRequest ) {
    IPentahoSession pentahoSession;

    HttpSession httpSession = httpRequest.getSession( false );
    if ( httpSession != null ) {
      pentahoSession = new PentahoHttpSession( null, httpSession, httpRequest.getLocale(), null );
    } else {
      pentahoSession = new NoDestroyStandaloneSession( null );
    }

    if ( callSetAuthenticatedForAnonymousUsers ) {
      pentahoSession.setAuthenticated( getAnonymousUser() );
    }

    ITempFileDeleter deleter = PentahoSystem.get( ITempFileDeleter.class, pentahoSession );
    if ( deleter != null ) {
      pentahoSession.setAttribute( ITempFileDeleter.DELETER_SESSION_VARIABLE, deleter );
    }

    return pentahoSession;
  }

  /**
   * Copied from {@code PentahoHttpSessionHelper.getPentahoSession(HttpServletRequest)}. Not sure what locale code was
   * doing there in the first place. TODO mlowery move this somewhere else
   */
  protected void localeLeftovers( final HttpServletRequest httpRequest ) {

    // Sync the thread static LocaleHelper's locale override with that stored in the session, if any.
    LocaleHelper.setThreadLocaleOverride( readLocaleOverrideFromHttpSession( httpRequest ) );

    // Even if there is no session, or if it has no locale override,
    // set the thread's fallback locale to that of the HTTP request.
    LocaleHelper.setThreadLocaleBase( httpRequest.getLocale() );
  }

  private void localeReset() {
    LocaleHelper.setThreadLocaleOverride( null );
    LocaleHelper.setThreadLocaleBase( null );
  }

  public void doFilter( ServletRequest request, ServletResponse response, FilterChain chain ) throws IOException,
    ServletException {

    // Do we really need the checks on the types in practice ?
    if ( !( request instanceof HttpServletRequest ) ) {
      throw new ServletException( "Can only process HttpServletRequest" );
    }

    if ( !( response instanceof HttpServletResponse ) ) {
      throw new ServletException( "Can only process HttpServletResponse" );
    }

    HttpServletRequest httpRequest = (HttpServletRequest) request;
    HttpServletResponse httpResponse = (HttpServletResponse) response;

    if ( httpRequest.getAttribute( FILTER_APPLIED ) != null ) {
      // ensure that filter is only applied once per request
      chain.doFilter( httpRequest, httpResponse );
      return;
    }

    HttpSession httpSession = safeGetSession( httpRequest, forceEagerSessionCreation );
    boolean httpSessionExistedAtStartOfRequest = httpSession != null;
    IPentahoSession pentahoSessionBeforeChainExecution = readPentahoSessionFromHttpSession( httpSession );

    if ( httpSessionExistedAtStartOfRequest ) {
      setSessionExpirationCookies( httpSession, pentahoSessionBeforeChainExecution, httpResponse );
    }

    // Make the HttpSession null, as we don't want to keep a reference to it lying
    // around in case chain.doFilter() invalidates it.
    httpSession = null;

    localeLeftovers( httpRequest );

    if ( pentahoSessionBeforeChainExecution == null ) {
      pentahoSessionBeforeChainExecution = generatePentahoSession( httpRequest );

      if ( logger.isDebugEnabled() ) {
        logger.debug( "Found no IPentahoSession in HTTP session; created new IPentahoSession" );
      }
    } else {
      if ( logger.isDebugEnabled() ) {
        logger.debug( "Obtained a valid IPentahoSession from HTTP session to "
          + "associate with PentahoSessionHolder: '" + pentahoSessionBeforeChainExecution + "'" );
      }
    }

    httpRequest.setAttribute( FILTER_APPLIED, Boolean.TRUE );

    // Create a wrapper that will eagerly update the session with the Hitachi Vantara session
    // if anything in the chain does a sendError() or sendRedirect().

    OnRedirectUpdateSessionResponseWrapper responseWrapper =
      new OnRedirectUpdateSessionResponseWrapper( httpResponse, httpRequest, httpSessionExistedAtStartOfRequest );

    // Proceed with chain

    try {
      // This is the only place in this class where PentahoSessionHolder.setSession() is called
      PentahoSessionHolder.setSession( pentahoSessionBeforeChainExecution );

      chain.doFilter( httpRequest, responseWrapper );
    } finally {
      // This is the only place in this class where PentahoSessionHolder.getSession() is called
      IPentahoSession pentahoSessionAfterChainExecution = PentahoSessionHolder.getSession();

      // Crucial removal of PentahoSessionHolder contents - do this before anything else.
      PentahoSessionHolder.removeSession();

      httpRequest.removeAttribute( FILTER_APPLIED );

      // storePentahoSessionInHttpSession() might already have been called by the response wrapper
      // if something in the chain called sendError() or sendRedirect(). This ensures we only call it
      // once per request.
      if ( !responseWrapper.isSessionUpdateDone() ) {
        storePentahoSessionInHttpSession( pentahoSessionAfterChainExecution, httpRequest,
          httpSessionExistedAtStartOfRequest );
      }

      localeReset();

      if ( logger.isDebugEnabled() ) {
        logger.debug( "PentahoSessionHolder now cleared, as request processing completed" );
      }
    }
  }

  /**
   * Gets the Hitachi Vantara session from the HTTP session (if available) and returns it.
   * <p/>
   * If the HTTP session is null or the Hitachi Vantara session is null it will return null.
   * <p/>
   *
   * @param httpSession
   *          the session obtained from the request.
   */
  private IPentahoSession readPentahoSessionFromHttpSession( HttpSession httpSession ) {
    if ( httpSession == null ) {
      if ( logger.isDebugEnabled() ) {
        logger.debug( "No HttpSession currently exists" );
      }

      return null;
    }

    // HTTP session exists, so try to obtain a Hitachi Vantara session from it.

    IPentahoSession pentahoSessionFromHttpSession =
      (IPentahoSession) httpSession.getAttribute( PentahoSystem.PENTAHO_SESSION_KEY );

    if ( pentahoSessionFromHttpSession == null ) {
      if ( logger.isDebugEnabled() ) {
        logger.debug( "HttpSession returned null object for " + PentahoSystem.PENTAHO_SESSION_KEY );
      }

      return null;
    }

    return pentahoSessionFromHttpSession;
  }

  /**
   * Stores the supplied Hitachi Vantara session in the HTTP session (if available).
   *
   * @param pentahoSession
   *          the Hitachi Vantara session obtained from the PentahoSessionHolder after the request has been processed by the
   *          filter chain. PentahoSessionHolder.getSession() cannot be used to obtain the Pentaho session as it has
   *          already been cleared by the time this method is called.
   * @param request
   *          the request object (used to obtain the session, if one exists).
   * @param httpSessionExistedAtStartOfRequest
   *          indicates whether there was a session in place before the filter chain executed. If this is true, and the
   *          session is found to be null, this indicates that it was invalidated during the request and a new session
   *                                           will now be created.
   * 
   */
  private void storePentahoSessionInHttpSession( IPentahoSession pentahoSession, HttpServletRequest request,
                                                 boolean httpSessionExistedAtStartOfRequest ) {
    HttpSession httpSession = safeGetSession( request, false );

    if ( httpSession == null ) {
      if ( httpSessionExistedAtStartOfRequest ) {
        if ( logger.isDebugEnabled() ) {
          logger.debug( "HttpSession is now null, but was not null at start of request; "
            + "session was invalidated, so do not create a new session" );
        }
      } else {
        // Generate a HttpSession only if we need to

        if ( !allowSessionCreation ) {
          if ( logger.isDebugEnabled() ) {
            logger.debug( "The HttpSession is currently null, and the " + this.getClass().getSimpleName()
              + " is prohibited from creating an HttpSession "
              + "(because the allowSessionCreation property is false) - Pentaho session thus not "
              + "stored for next request" );
          }
        } else if ( pentahoSession != null ) {
          if ( logger.isDebugEnabled() ) {
            logger.debug( "HttpSession being created as Pentaho session is non-null" );
          }

          httpSession = safeGetSession( request, true );

        } else {
          if ( logger.isDebugEnabled() ) {
            logger.debug( "HttpSession is null, and Pentaho session is null; "
              + "not creating HttpSession or storing SecurityContextHolder contents" );
          }
        }
      }
    }

    // If HttpSession exists, store current PentahoSessionHolder contents
    if ( httpSession != null ) {
      // See SEC-766

      httpSession.setAttribute( PentahoSystem.PENTAHO_SESSION_KEY, pentahoSession );

      if ( logger.isDebugEnabled() ) {
        logger.debug( "Pentaho session stored to HttpSession: '" + pentahoSession + "'" );
      }

    }
  }

  /**
   * Sets cookies needed to implement a session expiration dialog.
   * Enabled by default, could be disabled by a session-expired-dialog=false in a pentaho.xml.
   * Doesn't set the cookie in case SSO is used, for session expired dialog not to be displayed.
   *
   * The 'session-expiry' is needed to check if the session has expired.
   * The 'server-time' is needed to calculate offset between server and client time.
   *
   * @param httpSession         http session
   * @param pentahoSession      pentaho session
   * @param httpServletResponse http response
   */
  @VisibleForTesting
  void setSessionExpirationCookies( final HttpSession httpSession,
                                    final IPentahoSession pentahoSession,
                                    final HttpServletResponse httpServletResponse ) {

    if ( null == httpSession || null == pentahoSession ) {
      return;
    }

    //Show by default
    final String showDialog = PentahoSystem.getSystemSetting( "session-expired-dialog", "true" );

    if ( "true".equals( showDialog ) ) {

      final List<AuthenticationProvider> authenticationProviders =
        PentahoSystem.getAll( AuthenticationProvider.class, pentahoSession );

      //No authentication - no session expiration
      if ( null == authenticationProviders || authenticationProviders.isEmpty() ) {
        return;
      }

      //No session expired dialog when SSO is used
      if ( isSsoEnabled() ) {
        return;
      }

      SessionCookieConfig sessionCookieConfig = httpSession.getServletContext().getSessionCookieConfig();
      final long serverTime = System.currentTimeMillis();
      final long expiryTime = serverTime + httpSession.getMaxInactiveInterval() * 1000L;
      final String contextPath =
        httpSession.getServletContext().getContextPath() != null ? httpSession.getServletContext().getContextPath() :
          "/";

      final Cookie sessionExpirationCookie = new Cookie( "session-expiry", String.valueOf( expiryTime ) );
      sessionExpirationCookie.setPath( contextPath );
      // "httpOnly" attribute must be 'false' as there is Javascript (client side) that interacts with the cookie.
      // Check org.pentaho.mantle.client.commands.SessionExpiredCommand and issues PPP-4635 and BISERVER-14869
      sessionExpirationCookie.setHttpOnly( false );
      sessionExpirationCookie.setSecure( sessionCookieConfig.isSecure() );

      final Cookie serverTimeCookie = new Cookie( "server-time", String.valueOf( serverTime ) );
      serverTimeCookie.setPath( contextPath );
      // "httpOnly" attribute must be 'false' as there is Javascript (client side) that interacts with the cookie.
      // Check org.pentaho.mantle.client.commands.SessionExpiredCommand and issues PPP-4635 and BISERVER-14869
      serverTimeCookie.setHttpOnly( false );
      serverTimeCookie.setSecure( sessionCookieConfig.isSecure() );

      httpServletResponse.addCookie( sessionExpirationCookie );
      httpServletResponse.addCookie( serverTimeCookie );

    }
  }

  private Locale readLocaleOverrideFromHttpSession( HttpServletRequest httpRequest ) {
    HttpSession httpSession = httpRequest.getSession( false );
    return httpSession != null
      ? LocaleHelper.parseLocale( (String) httpSession.getAttribute( IPentahoSession.ATTRIBUTE_LOCALE_OVERRIDE ) )
      : null;
  }

  private HttpSession safeGetSession( HttpServletRequest request, boolean allowCreate ) {
    try {
      return request.getSession( allowCreate );
    } catch ( IllegalStateException ignored ) {
      return null;
    }
  }

  public boolean isAllowSessionCreation() {
    return allowSessionCreation;
  }

  public void setAllowSessionCreation( boolean allowSessionCreation ) {
    this.allowSessionCreation = allowSessionCreation;
  }

  public boolean isForceEagerSessionCreation() {
    return forceEagerSessionCreation;
  }

  public void setForceEagerSessionCreation( boolean forceEagerSessionCreation ) {
    this.forceEagerSessionCreation = forceEagerSessionCreation;
  }

  public void setCallSetAuthenticatedForAnonymousUsers( final boolean callSetAuthenticatedForAnonymousUsers ) {
    this.callSetAuthenticatedForAnonymousUsers = callSetAuthenticatedForAnonymousUsers;
  }

  protected String getAnonymousUser() {
    if ( anonymousUser == null ) {
      anonymousUser = PentahoSystem.getSystemSetting( "anonymous-authentication/anonymous-user", "anonymousUser" ); //$NON-NLS-1$//$NON-NLS-2$
    }
    return anonymousUser;
  }

  /**
   * Serves to identify if the server is using SSO for authentication. If <code>true</code>, it disables the session
   * expire dialog in PUC. The default value is <code>false</code>.
   */
  public boolean isSsoEnabled() {
    return ssoEnabled;
  }

  public void setSsoEnabled( boolean ssoEnabled ) {
    this.ssoEnabled = ssoEnabled;
  }

  // ~ Inner Classes ==================================================================================================

  /**
   * Wrapper that is applied to every request to update the <code>HttpSession<code> with
   * the <code>IPentahoSession</code> when a <code>sendError()</code> or <code>sendRedirect</code> happens. The class
   * contains the fields needed to call <code>storePentahoSessionInHttpSession()</code>
   */
  private class OnRedirectUpdateSessionResponseWrapper extends HttpServletResponseWrapper {

    HttpServletRequest request;

    boolean httpSessionExistedAtStartOfRequest;

    // Used to ensure storePentahoSessionInHttpSession() is only
    // called once.
    boolean sessionUpdateDone = false;

    /**
     * Takes the parameters required to call <code>storePentahoSessionInHttpSession()</code> in addition to the response
     * object we are wrapping.
     *
     * @see #storePentahoSessionInHttpSession(IPentahoSession, HttpServletRequest, boolean)
     */
    public OnRedirectUpdateSessionResponseWrapper( HttpServletResponse response, HttpServletRequest request,
                                                   boolean httpSessionExistedAtStartOfRequest ) {
      super( response );
      this.request = request;
      this.httpSessionExistedAtStartOfRequest = httpSessionExistedAtStartOfRequest;
    }

    /**
     * Makes sure the session is updated before calling the superclass <code>sendError()</code>
     */
    @Override
    public void sendError( int sc ) throws IOException {
      doSessionUpdate();
      super.sendError( sc );
    }

    /**
     * Makes sure the session is updated before calling the superclass <code>sendError()</code>
     */
    @Override
    public void sendError( int sc, String msg ) throws IOException {
      doSessionUpdate();
      super.sendError( sc, msg );
    }

    /**
     * Makes sure the session is updated before calling the superclass <code>sendRedirect()</code>
     */
    @Override
    public void sendRedirect( String location ) throws IOException {
      doSessionUpdate();
      super.sendRedirect( location );
    }

    /**
     * Calls <code>storePentahoSessionInHttpSession()</code>
     */
    private void doSessionUpdate() {
      if ( sessionUpdateDone ) {
        return;
      }
      IPentahoSession pentahoSession = PentahoSessionHolder.getSession();
      storePentahoSessionInHttpSession( pentahoSession, request, httpSessionExistedAtStartOfRequest );
      sessionUpdateDone = true;
    }

    /**
     * Tells if the response wrapper has called <code>storePentahoSessionInHttpSession()</code>.
     */
    public boolean isSessionUpdateDone() {
      return sessionUpdateDone;
    }

  }

  /**
   * An {@code StandaloneSession} that does nothing in its destroy implementation.
   * {@code InheritableThreadLocalPentahoSessionHolderStrategy} has the following code in {@code removeSession}:
   *
   * <pre>
   * {@code
   * if (sess instanceof StandaloneSession)
   *   ((StandaloneSession) sess).destroy();
   * }
   * </pre>
   * 
   * While this code cleans up broken code, it assumes that sessions should be destroyed. Merely removing the session
   * from the holder should not destroy the session. We can safely avoid not doing a destroy because this is new code
   * and does not store the session.
   * 
   * Until now, Standalone sessions were only used for scheduled jobs. This is a new use for StandaloneSessions.
   *
   * @author mlowery
   */
  private static class NoDestroyStandaloneSession extends StandaloneSession {

    private static final long serialVersionUID = -2402127216157794843L;

    public NoDestroyStandaloneSession( String name ) {
      super( name );
    }

    @Override
    public void destroy() {
      // nothing to do
    }

  }
}
