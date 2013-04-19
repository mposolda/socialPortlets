/*
 * JBoss, a division of Red Hat
 * Copyright 2013, Red Hat Middleware, LLC, and individual
 * contributors as indicated by the @authors tag. See the
 * copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */


package org.gatein.security.oauth.portlet.facebook;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletRequestDispatcher;
import javax.portlet.PortletSession;
import javax.portlet.PortletURL;
import javax.portlet.ProcessAction;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;

import com.restfb.DefaultFacebookClient;
import com.restfb.FacebookClient;
import com.restfb.Parameter;
import com.restfb.exception.FacebookOAuthException;
import com.restfb.types.FacebookType;
import org.exoplatform.container.ExoContainer;
import org.gatein.security.oauth.common.OAuthConstants;
import org.gatein.security.oauth.common.OAuthProviderType;
import org.gatein.security.oauth.facebook.FacebookAccessTokenContext;
import org.gatein.security.oauth.portlet.AbstractSocialPortlet;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class FacebookStatusUpdatePortlet extends AbstractSocialPortlet<FacebookAccessTokenContext> {

    public static final String ACTION_UPDATE_STATUS = "_updateStatus";
    public static final String ACTION_BACK = "_backToForm";

    private static final String ATTR_FB_ACCESS_TOKEN = "AttributeFacebookAccessToken";
    public static final String RENDER_PARAM_STATUS = "renderParamStatus";
    public static final String RENDER_PARAM_ERROR_MESSAGE = "renderParamErrorMessage";

    public enum Status {
        SUCCESS,
        NOT_SPECIFIED_MESSAGE_OR_LINK,
        FACEBOOK_ERROR_INSUFFICIENT_SCOPE,
        FACEBOOK_ERROR_OTHER
    }

    @Override
    protected void afterInit(ExoContainer container) {
    }

    @Override
    protected OAuthProviderType<FacebookAccessTokenContext> getOAuthProvider() {
        return getOauthProviderTypeRegistry().getOAuthProvider(OAuthConstants.OAUTH_PROVIDER_KEY_FACEBOOK);
    }


    @Override
    protected void handleRender(RenderRequest request, RenderResponse response, FacebookAccessTokenContext accessToken) throws IOException, PortletException {
        // Save FB AccessToken to session, so it could be used in actionUpdateStatus
        request.getPortletSession().setAttribute(ATTR_FB_ACCESS_TOKEN, accessToken);
        PortletRequestDispatcher prd = getPortletContext().getRequestDispatcher("/jsp/facebook/statusupdate.jsp");
        prd.include(request, response);
    }


    @ProcessAction(name = ACTION_UPDATE_STATUS)
    public void actionUpdateStatus(ActionRequest aReq, ActionResponse aResp) throws IOException {
        PortletSession session = aReq.getPortletSession();

        String message = getParameterAndSaveItToSession("message", aReq, session);
        String link = getParameterAndSaveItToSession("link", aReq, session);
        String picture = getParameterAndSaveItToSession("picture", aReq, session);
        String name = getParameterAndSaveItToSession("name", aReq, session);
        String caption = getParameterAndSaveItToSession("caption", aReq, session);
        String description = getParameterAndSaveItToSession("description", aReq, session);

        if (isEmpty(message) && isEmpty(link)) {
            aResp.setRenderParameter(RENDER_PARAM_STATUS, Status.NOT_SPECIFIED_MESSAGE_OR_LINK.name());
            return;
        }

        if (log.isTraceEnabled()) {
            StringBuilder builder = new StringBuilder("message=" + message)
                    .append(", link=" + link)
                    .append(", picture=" + picture)
                    .append(", name=" + name)
                    .append(", caption=" + caption)
                    .append(", description=" + description);
            log.trace(builder.toString());
        }

        // Obtain accessToken from portlet session
        FacebookAccessTokenContext accessTokenContext = (FacebookAccessTokenContext)aReq.getPortletSession().getAttribute(ATTR_FB_ACCESS_TOKEN);

        FacebookClient facebookClient = new DefaultFacebookClient(accessTokenContext.getAccessToken());
        List<Parameter> params = new ArrayList<Parameter>();
        appendParam(params, "message", message);
        appendParam(params, "link", link);
        appendParam(params, "picture", picture);
        appendParam(params, "name", name);
        appendParam(params, "caption", caption);
        appendParam(params, "description", description);

        try {
            FacebookType publishMessageResponse = facebookClient.publish("me/feed", FacebookType.class, params.toArray(new Parameter[] {}));
            if (publishMessageResponse.getId() != null) {
                log.debug("Message published successfully to Facebook profile of user " + aReq.getRemoteUser() + " with ID " + publishMessageResponse.getId());
                aResp.setRenderParameter(RENDER_PARAM_STATUS, Status.SUCCESS.name());
            }
        } catch (FacebookOAuthException foe) {
            String exMessage = foe.getErrorCode() + " - " + foe.getErrorType() + " - " + foe.getErrorMessage();
            log.warn(exMessage);
            if (foe.getErrorMessage().contains("The user hasn't authorized the application to perform this action")) {
                aResp.setRenderParameter(RENDER_PARAM_STATUS, Status.FACEBOOK_ERROR_INSUFFICIENT_SCOPE.name());
            } else {
                aResp.setRenderParameter(RENDER_PARAM_STATUS, Status.FACEBOOK_ERROR_OTHER.name());
                aResp.setRenderParameter(RENDER_PARAM_ERROR_MESSAGE, exMessage);
            }
        }
    }

    @ProcessAction(name = ACTION_BACK)
    public void actionBack(ActionRequest aReq, ActionResponse aResp) throws IOException {
        aResp.removePublicRenderParameter(RENDER_PARAM_STATUS);
        aResp.removePublicRenderParameter(RENDER_PARAM_ERROR_MESSAGE);
    }

    private boolean isEmpty(String message) {
        return message == null || message.length() == 0;
    }

    private void appendParam(List<Parameter> params, String paramName, String paramValue) {
        if (paramValue != null) {
            params.add(Parameter.with(paramName, paramValue));
        }
    }
}
