package org.gatein.security.oauth.portlet;

import java.io.IOException;

import javax.inject.Inject;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.GenericPortlet;
import javax.portlet.PortletRequest;
import javax.portlet.PortletSession;
import javax.portlet.ProcessAction;
import org.gatein.api.oauth.AccessToken;
import org.gatein.api.oauth.OAuthProvider;

/**
 * Base social portlet class, which handles basic function (Handle action for redirection to OAuth login etc)
 *
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public abstract class AbstractSocialPortlet extends GenericPortlet {

    public static final String ACTION_OAUTH_REDIRECT = "actionOAuthRedirect";
    public static final String PARAM_CUSTOM_SCOPE = "_oauthCustomScope";

    @Inject
    private RequestContext requestContext;

    @ProcessAction(name = ACTION_OAUTH_REDIRECT)
    public void actionRedirectToOAuthFlow(ActionRequest aReq, ActionResponse aResp) throws IOException {
        String customScope = aReq.getParameter(PARAM_CUSTOM_SCOPE);

        getOAuthProvider().startOAuthWorkflow(customScope);
    }

    // Needs to be implemented by Subclass. Subclass should return value of key (For example: "FACEBOOK" or "GOOGLE")
    protected abstract String getOAuthProviderKey();


    // Intended to be used by subclasses
    protected OAuthProvider getOAuthProvider() {
        return requestContext.getOAuthProvider(getOAuthProviderKey());
    }

    // Intended to be used by subclasses
    protected AccessToken getAccessToken() {
        return requestContext.getAccessToken(getOAuthProviderKey());
    }

    // Helper method. Intended to be used by subclasses
    protected String getParameterAndSaveItToSession(String paramName, PortletRequest req, PortletSession session) {
        String paramValue = req.getParameter(paramName);
        if (paramValue != null) {
            session.setAttribute(paramName, paramValue);
        } else {
            paramValue = (String)session.getAttribute(paramName);
        }

        return paramValue;
    }
}
