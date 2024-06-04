package com.asanasoft.gsml.example;


// other imports omitted...

import com.asanasoft.gsml.client.TokenManager;
import com.asanasoft.gsml.client.events.listener.EventListener;
import com.asanasoft.gsml.exception.TokenManagerException;
import com.asanasoft.gsml.client.events.Event;
import com.asanasoft.gsml.client.events.Register;
import com.asanasoft.gsml.client.events.Unregister;

public class IntegrationExample implements EventListener {
    private Map<String, HttpSession> sessions = null;
    private CommandProcessor commands = null;

    public IntegrationExample() {
        sessions = new HashMap();
    }

    public void acceptRequest(HttpServletRequest request) {
        String bearerToken;
        TokenManager tokenManager;
        HttpSession session;

        /**
         * We don't have Design by Contract, but we are going to assume that
         * there's a bearer token in the request header. In Eiffel, the
         * precondition is only processed during development and turned off
         * in production, if all checks pass in development. Since contracts are
         * ONLY between software components, once clients of this method abide
         * by the precondition, there is no need to test the precondition on
         * every invocation.
         */
        bearerToken = request.getHeader("Bearer");
        session     = request.getSession(false);

        if (session != null) {
            tokenManager = (TokenManager)session.getAttribute("tokenManager");
        }
        else {
            tokenManager = new TokenManager();
        }

        try {
            tokenManager.setAccessToken(bearerToken);
        } catch (TokenManagerException e) {
            tokenManager.invalidate();
        }

        if (tokenManager.isValid()) {
            if (session == null) session = request.getSession(true);

            session.setAttribute("tokenManager", tokenManager);

            //no need to set a flag for a valid session. The fact that we have a session implies validity.

            command.execute(request);
        }
        else {
            //possibly redirect to an error page or a login page
        }
    }

    /**
     * This method is called by GSML when TokenManager changes
     * state, or experiences an error.
     */
    @Override
    public void eventTriggered(Event event) {
        switch (event.getType()) {
            case REGISTER: {
                Register registerEvent = (Register)event;
                registerEvent.getTokenManager().getContextValue("some context value that I want...");
                //do something "register-y
                break;
            }
            case UNREGISTER: {
                Unregister unregisterEvent = (Unregister)event;
                unregisterEvent.getTokenManager().getContextValue("some context value that I want...");
                //do something "unregister-y
                break;
            }
            case REVOKE: {
                logger.debug("***** A token has been revoked: " + event.getPayload().get("revokedTokenId"));
                HttpSession session = (HttpSession)event.get("session");
                session.invalidate();
                break;
            }
            case INVALID: {
                logger.debug("***** An invalid token has been received!!");
                break;
            }
            case REFRESH: {
                /**
                 * In this scenario, the application will take care of session inactivity and invalidate the session
                 * and token if the session "timesout"...
                 */
                boolean invalidated = false;
                HttpSession session = (HttpSession)event.get("session");
                logger.debug("Got session from REFRESH..." + session.getId());

                if (session != null) {
                    /**
                     * SESSION_HERE is an attribute assigned/created by
                     * the application to determine if a user is
                     * interacting with it at the moment. GSML
                     * (with the JEE option) will extend the session's
                     * maxInactivityTimeout to match the expiration of
                     * the Refresh Token. Because of this, the application
                     * must manage the inactivity timeout manually
                     * (that is, if inactivity is important to the application)
                     */
                    if (session.getAttribute("SESSION_HERE") != null) {
                        long now = (new Date()).getTime();
                        if ((session.getLastAccessedTime() + INACTIVITY_TIMEOUT) < now) {
                            TokenManager tokenManager = (TokenManager)session.getAttribute("tokenManager");
                            session.invalidate(); //this will invalidate the TokenManager as well
                            logger.debug("Session timed out based on inactivity...");
                            invalidated = true;
                        }
                    }
                }

                if (!invalidated) logger.debug("***** A new Carrier Token has been requested by the TokenManager...");
                break;
            }
            case ERROR: {
                logger.debug("***** An error occurred: " + event.getMessage());
                break;
            }
            case NO_TOKEN: {
                logger.debug("***** No token found: " + event.getMessage());
                break;
            }
            default: noop();
        }
    }
    // rest of the implementation ommitted...
}