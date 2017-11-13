
package com.openexchange.oidc.spi;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.oauth2.sdk.AccessTokenResponse;
import com.nimbusds.oauth2.sdk.token.AccessToken;
import com.nimbusds.oauth2.sdk.token.AccessTokenType;
import com.nimbusds.oauth2.sdk.token.RefreshToken;
import com.nimbusds.oauth2.sdk.token.Tokens;
import com.openexchange.ajax.SessionUtility;
import com.openexchange.ajax.login.LoginConfiguration;
import com.openexchange.authentication.LoginExceptionCodes;
import com.openexchange.context.ContextService;
import com.openexchange.exception.OXException;
import com.openexchange.groupware.contexts.Context;
import com.openexchange.groupware.ldap.User;
import com.openexchange.login.internal.LoginPerformer;
import com.openexchange.oidc.OIDCBackendConfig;
import com.openexchange.oidc.OIDCExceptionCode;
import com.openexchange.oidc.osgi.Services;
import com.openexchange.oidc.tools.OIDCTools;
import com.openexchange.session.Session;
import com.openexchange.session.reservation.Reservation;
import com.openexchange.session.reservation.SessionReservationService;
import com.openexchange.tools.servlet.AjaxExceptionCodes;
import com.openexchange.user.UserService;

@RunWith(PowerMockRunner.class)
@PrepareForTest({AbstractOIDCBackend.class, OIDCTools.class, LoginPerformer.class, SessionUtility.class, LoginConfiguration.class, Services.class})
public class AbstractOIDCBackendTest {

    private static final int CONTEXT_ID = 1;
    private static final String ID_TOKEN = "IDToken";
    private static final String SESSION_TOKEN = "SessionToken";
    private static final String SCOPE_ONE = "scopeOne";
    private static final String SCOPE_TWO = "scopeTwo";

    @Mock
    private OIDCBackendConfig mockedBackendConfig;
    
    @Mock
    private Session mockedSession;
    
    @Mock
    private HttpServletRequest mockedRequest;
    
    @Mock
    private HttpServletResponse mockedResponse;
    
    @Mock
    private SessionReservationService mockedSessionReservationService;
    
    @Mock
    private Reservation mockedReservation;
    
    @Mock
    private ContextService mockedContextService;
    
    @Mock
    private Context mockedContext;
    
    @Mock
    private UserService mockedUserService;
    
    @Mock
    private User mockedUser;
    
    private OIDCBackend testBackend;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Services.class);
        PowerMockito.stub(PowerMockito.method(AbstractOIDCBackend.class, "getBackendConfig")).toReturn(mockedBackendConfig);
        this.testBackend = new OIDCCoreBackend() {

            @Override
            public OIDCBackendConfig getBackendConfig() {
                return mockedBackendConfig;
            }

        };
    }

    @Test
    public void getJWSAlgorithm_StandardTest() throws OXException {
        Mockito.when(mockedBackendConfig.getJWSAlgortihm()).thenReturn(null);
        
        JWSAlgorithm jwsAlgorithm = this.testBackend.getJWSAlgorithm();
        assertTrue("Failed to use standard algorith", jwsAlgorithm.toString().equals(JWSAlgorithm.RS256.toString()));
    }

    @Test
    public void getJWSAlgorithm_FromConfigTest() throws OXException {
        Mockito.when(mockedBackendConfig.getJWSAlgortihm()).thenReturn(JWSAlgorithm.ES256.toString());
        JWSAlgorithm jwsAlgorithm = this.testBackend.getJWSAlgorithm();
        assertTrue("Failed to use standard algorith", jwsAlgorithm.toString().equals(JWSAlgorithm.ES256.toString()));

    }

    @Test
    public void getJWSAlgorithm_FailTest() {
        Mockito.when(mockedBackendConfig.getJWSAlgortihm()).thenReturn("12345");
        try {
            this.testBackend.getJWSAlgorithm();
        } catch (OXException e) {
            assertTrue("Wrong JWSAlgorithm was accepted", OIDCExceptionCode.UNABLE_TO_PARSE_JWS_ALGORITHM == e.getExceptionCode());
        }
    }

    @Test
    public void getScope_SingleTest() {
        Mockito.when(mockedBackendConfig.getScope()).thenReturn(SCOPE_ONE);
        List<String> scopeList = this.testBackend.getScope().toStringList();
        assertTrue("Not the number of scopes that were expected, should be one", scopeList.size() == 1);
        assertTrue("Scope is not what expected", scopeList.get(0).equals(SCOPE_ONE));
    }

    @Test
    public void getScope_MultipleTest() {
        Mockito.when(mockedBackendConfig.getScope()).thenReturn(SCOPE_ONE + ";" + SCOPE_TWO);
        List<String> scopeList = this.testBackend.getScope().toStringList();
        assertTrue("Not the number of scopes that were expected, should be two", scopeList.size() == 2);
        assertTrue("Scope is not what expected", scopeList.get(0).equals(SCOPE_ONE));
        assertTrue("Scope is not what expected", scopeList.get(1).equals(SCOPE_TWO));
    }

    @Test
    public void updateOauthTokens_TokenGatheringFailTest() throws Exception {
        
        // Spy on the AbstractOIDCBackend to mock private method
        AbstractOIDCBackend abstractBackend = PowerMockito.spy(new OIDCCoreBackend() {

            @Override
            public OIDCBackendConfig getBackendConfig() {
                return mockedBackendConfig;
            }

        });
        
        // Mock the token response to give null tokens back
        AccessTokenResponse tokenResponse = new AccessTokenResponse(new Tokens(new AccessToken(AccessTokenType.BEARER) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toAuthorizationHeader() {
                return null;
            }
            
        }, new RefreshToken())) {
            @Override
            public Tokens getTokens() {
                return null;
            }
        };
        
        
        PowerMockito.doReturn(tokenResponse).when(abstractBackend, "loadAccessToken", this.mockedSession);
        
        try {
            boolean updateOauthTokens = abstractBackend.updateOauthTokens(this.mockedSession);
            assertFalse("False expected, got true", updateOauthTokens);
        } catch (OXException e) {
            e.printStackTrace();
            fail("No errors should occur");
        }
    }

    @Test
    public void updateOauthTokens_PassTest() throws Exception {
        // Spy on the AbstractOIDCBackend to mock private method
        AbstractOIDCBackend abstractBackend = PowerMockito.spy(new OIDCCoreBackend() {

            @Override
            public OIDCBackendConfig getBackendConfig() {
                return mockedBackendConfig;
            }

        });
        
        // Mock the token response to give null tokens back
        AccessTokenResponse tokenResponse = new AccessTokenResponse(new Tokens(new AccessToken(AccessTokenType.BEARER) {
            private static final long serialVersionUID = 1L;

            @Override
            public String toAuthorizationHeader() {
                return null;
            }
            
        }, new RefreshToken()));
        
        PowerMockito.doReturn(tokenResponse).when(abstractBackend, "loadAccessToken", this.mockedSession);
        PowerMockito.doNothing().when(abstractBackend, "updateSession", Matchers.any(Session.class), Matchers.any(Map.class));
        try {
            boolean updateOauthTokens = abstractBackend.updateOauthTokens(this.mockedSession);
            assertTrue("True expected, got false", updateOauthTokens);
        } catch (OXException e) {
            e.printStackTrace();
            fail("No errors should occur");
        }
    }

    @Test
    public void isTokenExpired_TrueTest() {
        Mockito.when(mockedBackendConfig.getOauthRefreshTime()).thenReturn(10000);
        Mockito.when(mockedSession.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE)).thenReturn(String.valueOf(new Date().getTime()));
        try {
            boolean result = this.testBackend.isTokenExpired(mockedSession);
            assertTrue("True expected, got false", result);
        } catch (OXException e) {
            e.printStackTrace();
            fail("No error should happen");
        }
    }

    @Test
    public void isTokenExpired_FailTest() {
        Mockito.when(mockedBackendConfig.getOauthRefreshTime()).thenReturn(0);
        Mockito.when(mockedSession.getParameter(Session.PARAM_OAUTH_ACCESS_TOKEN_EXPIRY_DATE)).thenReturn(String.valueOf(new Date().getTime()+10000));
        try {
            boolean result = this.testBackend.isTokenExpired(mockedSession);
            assertFalse("False expected, got true", result);
        } catch (OXException e) {
            e.printStackTrace();
            fail("No error should happen");
        }
    }

    @Test
    public void logoutCurrentUser_NoOIDCCookieDeletionTest() throws Exception {
        PowerMockito.mockStatic(OIDCTools.class);
        PowerMockito.mockStatic(LoginPerformer.class);
        PowerMockito.mockStatic(SessionUtility.class);
        PowerMockito.doNothing().when(OIDCTools.class, "validateSession", Matchers.any(Session.class), Matchers.any(HttpServletRequest.class));
        LoginPerformer loginPerformer = Mockito.mock(LoginPerformer.class);
        PowerMockito.doReturn(loginPerformer).when(LoginPerformer.class, "getInstance");
        Mockito.when(loginPerformer.doLogout(Matchers.anyString())).thenReturn(mockedSession);
        PowerMockito.doNothing().when(SessionUtility.class, "removeOXCookies", Matchers.any(Session.class), Matchers.any(HttpServletRequest.class), Matchers.any(HttpServletResponse.class));
        PowerMockito.doNothing().when(SessionUtility.class, "removeJSESSIONID", Matchers.any(HttpServletRequest.class), Matchers.any(HttpServletResponse.class));
        Mockito.when(mockedBackendConfig.isAutologinEnabled()).thenReturn(false);
        
        PowerMockito.verifyStatic(Mockito.times(0));
        SessionUtility.removeCookie(Matchers.any(javax.servlet.http.Cookie.class), Matchers.anyString(), Matchers.anyString(), Matchers.any(HttpServletResponse.class));
        
        HttpServletRequest mockedRequest = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse mockedResponse = Mockito.mock(HttpServletResponse.class);
        this.testBackend.logoutCurrentUser(mockedSession, mockedRequest, mockedResponse);
        
    }
    
    @Test
    public void performLogin_NoSessionTokenFailTest() {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(null);
            try {
                testBackend.performLogin(mockedRequest, mockedResponse, true);
            } catch (@SuppressWarnings("unused") IOException | JSONException e) {
                fail("Wrong exception thrown.");
                return;
            } catch (OXException e) {
                assertTrue("Wrong error message thrown", e.getExceptionCode() == AjaxExceptionCodes.BAD_REQUEST);
                return;
            } 
        fail("An error was expected but not thrown.");
    }
    
    @Test
    public void performLogin_NoReservationFailTest() throws Exception {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(SESSION_TOKEN);
        PowerMockito.doReturn(mockedSessionReservationService).when(Services.class, "getService", SessionReservationService.class);
        Mockito.when(mockedSessionReservationService.removeReservation(SESSION_TOKEN)).thenReturn(null);
        try {
            testBackend.performLogin(mockedRequest, mockedResponse, true);
        } catch (@SuppressWarnings("unused") IOException | JSONException e) {
            fail("Wrong exception thrown.");
            return;
        } catch (OXException e) {
            assertTrue("Wrong error message thrown", e.getExceptionCode() == LoginExceptionCodes.INVALID_CREDENTIALS);
            return;
        } 
        fail("An error was expected but not thrown.");
    }
    
    @Test
    public void performLogin_NoIDTokenFailTest() throws Exception {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(SESSION_TOKEN);
        PowerMockito.doReturn(mockedSessionReservationService).when(Services.class, "getService", SessionReservationService.class);
        Mockito.when(mockedSessionReservationService.removeReservation(SESSION_TOKEN)).thenReturn(mockedReservation);
        Mockito.when(mockedReservation.getState()).thenReturn(new HashMap<String,String>());
        try {
            testBackend.performLogin(mockedRequest, mockedResponse, true);
        } catch (@SuppressWarnings("unused") IOException | JSONException e) {
            fail("Wrong exception thrown.");
            return;
        } catch (OXException e) {
            assertTrue("Wrong error message thrown", e.getExceptionCode() == AjaxExceptionCodes.BAD_REQUEST);
            return;
        } 
        fail("An error was expected but not thrown.");
    }
    
    @Test
    public void performLogin_NoContextServiceFailTest() throws Exception {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(SESSION_TOKEN);
        PowerMockito.doReturn(mockedSessionReservationService).when(Services.class, "getService", SessionReservationService.class);
        Mockito.when(mockedSessionReservationService.removeReservation(SESSION_TOKEN)).thenReturn(mockedReservation);
        HashMap<String, String> idTokenMap = new HashMap<String,String>();
        idTokenMap.put(OIDCTools.IDTOKEN, ID_TOKEN);
        Mockito.when(mockedReservation.getState()).thenReturn(idTokenMap);
        PowerMockito.doReturn(mockedContextService).when(Services.class, "getService", ContextService.class);
        Mockito.when(mockedReservation.getContextId()).thenReturn(CONTEXT_ID);
        Mockito.when(mockedContextService.getContext(CONTEXT_ID)).thenReturn(mockedContext);
        Mockito.when(mockedContext.isEnabled()).thenReturn(false);
        
        try {
            testBackend.performLogin(mockedRequest, mockedResponse, true);
        } catch (@SuppressWarnings("unused") IOException | JSONException e) {
            fail("Wrong exception thrown.");
            return;
        } catch (OXException e) {
            assertTrue("Wrong error message thrown", e.getExceptionCode() == LoginExceptionCodes.INVALID_CREDENTIALS);
            return;
        } 
        fail("An error was expected but not thrown.");
    }
    
    @Test
    public void performLogin_NoUserServiceFailTest() throws Exception {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(SESSION_TOKEN);
        PowerMockito.doReturn(mockedSessionReservationService).when(Services.class, "getService", SessionReservationService.class);
        Mockito.when(mockedSessionReservationService.removeReservation(SESSION_TOKEN)).thenReturn(mockedReservation);
        HashMap<String, String> idTokenMap = new HashMap<String,String>();
        idTokenMap.put(OIDCTools.IDTOKEN, ID_TOKEN);
        Mockito.when(mockedReservation.getState()).thenReturn(idTokenMap);
        PowerMockito.doReturn(mockedContextService).when(Services.class, "getService", ContextService.class);
        Mockito.when(mockedReservation.getContextId()).thenReturn(CONTEXT_ID);
        Mockito.when(mockedContextService.getContext(CONTEXT_ID)).thenReturn(mockedContext);
        Mockito.when(mockedContext.isEnabled()).thenReturn(false);
        PowerMockito.doReturn(mockedContextService).when(Services.class, "getService", UserService.class);
        Mockito.when(mockedReservation.getUserId()).thenReturn(2);
        Mockito.when(mockedUserService.getUser(2, mockedContext)).thenReturn(mockedUser);
        Mockito.when(mockedUser.isMailEnabled()).thenReturn(false);
        
        try {
            testBackend.performLogin(mockedRequest, mockedResponse, true);
        } catch (@SuppressWarnings("unused") IOException | JSONException e) {
            fail("Wrong exception thrown.");
            return;
        } catch (OXException e) {
            assertTrue("Wrong error message thrown", e.getExceptionCode() == LoginExceptionCodes.INVALID_CREDENTIALS);
            return;
        } 
        fail("An error was expected but not thrown.");
    }
    
    /*
     * Shorter form for Autologin is AL, Only autologin from here on
     */
    
    private void setUpForLogin() throws Exception {
        Mockito.when(mockedRequest.getParameter(OIDCTools.SESSION_TOKEN)).thenReturn(SESSION_TOKEN);
        PowerMockito.doReturn(mockedSessionReservationService).when(Services.class, "getService", SessionReservationService.class);
        Mockito.when(mockedSessionReservationService.removeReservation(SESSION_TOKEN)).thenReturn(mockedReservation);
        HashMap<String, String> idTokenMap = new HashMap<String,String>();
        idTokenMap.put(OIDCTools.IDTOKEN, ID_TOKEN);
        Mockito.when(mockedReservation.getState()).thenReturn(idTokenMap);
        PowerMockito.doReturn(mockedContextService).when(Services.class, "getService", ContextService.class);
        Mockito.when(mockedReservation.getContextId()).thenReturn(CONTEXT_ID);
        Mockito.when(mockedContextService.getContext(CONTEXT_ID)).thenReturn(mockedContext);
        Mockito.when(mockedContext.isEnabled()).thenReturn(false);
        PowerMockito.doReturn(mockedContextService).when(Services.class, "getService", UserService.class);
        Mockito.when(mockedReservation.getUserId()).thenReturn(2);
        Mockito.when(mockedUserService.getUser(2, mockedContext)).thenReturn(mockedUser);
        Mockito.when(mockedUser.isMailEnabled()).thenReturn(true);
    }
    
    @Test
    public void performLogin_CookieALNoSSOCookieLoginTest() throws Exception {
        setUpForLogin();
        Mockito.when(mockedBackendConfig.isAutologinEnabled()).thenReturn(true);
        Mockito.when(mockedBackendConfig.autologinCookieMode()).thenReturn(OIDCBackendConfig.AutologinMode.SSO_REDIRECT.getValue());
        try {
            testBackend.performLogin(mockedRequest, mockedResponse, true);
        } catch (@SuppressWarnings("unused") IOException | JSONException e) {
            fail("Wrong exception thrown.");
            return;
        } catch (OXException e) {
            assertTrue("Wrong error message thrown", e.getExceptionCode() == LoginExceptionCodes.INVALID_CREDENTIALS);
            return;
        } 
        fail("An error was expected but not thrown.");
    }
    
    @Test
    public void performLogin_CookieALNoSessionInCookieFailTest() {
        
    }
    
    @Test
    public void performLogin_CookieALUpdateSessionSessionStorageTest() {
        
    }
    
    @Test
    public void performLogin_CookieALUpdateSessionSessionDServTest() {
        
    }
    
    @Test
    public void performLogin_CookieALSessionAsJSONTest() {
        
    }
    
    @Test
    public void performLogin_CookieALGetRedirectFailTest() {
        
    }
    
    @Test
    public void performLogin_CookieALGetRedirectTest() {
        
    }
    
    @Test
    public void performLogin_LoginAddALCookieToResponseTest() {
        
    }
    
    @Test
    public void performLogin_LoginNoALTest() {
        
    }
    
}
