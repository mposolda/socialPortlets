<!--
JBoss, Home of Professional Open Source
Copyright 2012, Red Hat, Inc. and/or its affiliates, and individual
contributors by the @authors tag. See the copyright.txt in the
distribution for a full listing of individual contributors.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at
http://www.apache.org/licenses/LICENSE-2.0
Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->
<%@ taglib uri="http://java.sun.com/portlet_2_0" prefix="portlet" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ page import="java.util.Locale" %>
<%@ page import="java.util.ResourceBundle" %>
<%@ page import="org.gatein.security.oauth.common.OAuthConstants" %>
<%@ page import="org.gatein.security.oauth.portlet.facebook.FacebookFriendsPortlet" %>
<%@ page import="javax.portlet.ActionRequest" %>
<%@ page trimDirectiveWhitespaces="true" %>
<portlet:defineObjects/>

<div class="socialPortlet facebookFriendsPortlet">
    <h3 class="socialHeader facebookHeader">Friends (${fbMe.friendsNumber})</h3>

    <portlet:actionURL var="filterUrl">
        <portlet:param name="<%= ActionRequest.ACTION_NAME %>" value="<%= FacebookFriendsPortlet.ACTION_USER_FILTER%>" />
    </portlet:actionURL>

    <h4>Friend Filter (by name):</h4>
    <form action="${filterUrl}" class="socialForm horizontalForm" method="POST">
        <label><span>Filter:</span><input name="<%= FacebookFriendsPortlet.PARAM_USER_FILTER%>"/></label>
        <input type="submit" name="triggerFilter" value="Submit Filter"/>
        <input type="submit" name="cancelFilter" value="Cancel Filter"/><br>
    </form>

    <h4>Your Friends:</h4>
    <div class="socialFriends">
        <c:forEach var="fbFriend" items="${fbFriends}">
            <portlet:renderURL var="friendUrl">
                <portlet:param name="_personID" value="${fbFriend.id}"/>
            </portlet:renderURL>
            <a href="${friendUrl}" class="socialFriendItem">
                <img src="${fbFriend.imageUrl}" title="${fbFriend.name}"/>
            </a>
        </c:forEach>
    </div>
    <div class="socialFriendsPages">
        <c:set var="count" value="0" scope="page" />
        <c:forEach var="fbPaginatorUrl" items="${fbBean.friendPaginatorUrls}">
            <c:set var="count" value="${count + 1}" scope="page"/>
            <a href="${fbPaginatorUrl}"><c:out value="${count}" /></a>
        </c:forEach>
    </div>

    <c:if test="${!empty fbFriend}">
        <div class="socialFriendDetails">
            <h4>Friend Details for: ${fbFriend.name}</h4>
            <c:choose>
                <c:when test="${empty fbFriend.statuses}">
                    <portlet:actionURL var="scopeUrl">
                        <portlet:param name="javax.portlet.action" value="actionOAuthRedirect"/>
                        <portlet:param name="_oauthCustomScope"
                                       value="${fbFriend.id == fbMe.id ? 'user_status' : 'friends_status'}"/>
                    </portlet:actionURL>
                    <c:choose>
                        <c:when test="${fbFriend.scope}">
                            This user doesn't have any public messages<br/>
                        </c:when>
                        <c:otherwise>
                            <b>WARNING: </b>
                            You have insufficient privileges (Facebook scope) to show status on FB wall. Your access token needs to have scope:
                            <b>${fbFriend.id == fbMe.id ? 'user_status' : 'friends_status'}</b><br/>

                            Click <a style="color: blue;" href="${scopeUrl}">here</a> to fix it<br/><br/>
                        </c:otherwise>
                    </c:choose>
                </c:when>
                <c:otherwise>
                    <c:forEach var="fbStatus" items="${fbFriend.statuses}">
                        <div class='socialActivity'>
                            <div class="activityDetails">
                                <div class="activityHeader">${fbStatus.message}</div>
                                <div class="activityPopularity">
                                    <div class="activityLikes">+${fn:length(fbStatus.likes)}</div>
                                </div>
                            </div>

                            <div class="activityComments">
                                <c:forEach var="fbComment" items="${fbStatus.comments}">
                                    <div class="commentDetails">
                                        <div class="commentAuthor">${fbComment.from.name} (${fbComment.createdTime})</div>
                                        <c:if test="${fbComment.likeCount > 0}">
                                            <div class="commentLikes">+${fbComment.likeCount}</div>
                                        </c:if>
                                        <div class="commentContent">${fbComment.message}</div>
                                    </div>
                                </c:forEach>
                            </div>
                        </div>
                    </c:forEach>
                </c:otherwise>
            </c:choose>
        </div>
    </c:if>

</div>