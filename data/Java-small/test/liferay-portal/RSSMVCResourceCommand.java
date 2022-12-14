/**
 * Copyright (c) 2000-present Liferay, Inc. All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */

package com.liferay.social.activities.web.portlet.action;

import com.liferay.portal.kernel.dao.search.SearchContainer;
import com.liferay.portal.kernel.portlet.LiferayPortletResponse;
import com.liferay.portal.kernel.portlet.PortletResponseUtil;
import com.liferay.portal.kernel.portlet.bridges.mvc.MVCResourceCommand;
import com.liferay.portal.kernel.util.ContentTypes;
import com.liferay.portal.kernel.util.GetterUtil;
import com.liferay.portal.kernel.util.HtmlUtil;
import com.liferay.portal.kernel.util.ParamUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.kernel.util.WebKeys;
import com.liferay.portal.model.Group;
import com.liferay.portal.service.GroupLocalService;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.service.ServiceContextFactory;
import com.liferay.portal.theme.ThemeDisplay;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.social.model.SocialActivity;
import com.liferay.portlet.social.model.SocialActivityFeedEntry;
import com.liferay.portlet.social.service.SocialActivityInterpreterLocalService;
import com.liferay.portlet.social.service.SocialActivityLocalService;
import com.liferay.social.activities.web.constants.SocialActivitiesPortletKeys;
import com.liferay.util.RSSUtil;

import com.sun.syndication.feed.synd.SyndContent;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntry;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.feed.synd.SyndFeedImpl;
import com.sun.syndication.feed.synd.SyndLink;
import com.sun.syndication.feed.synd.SyndLinkImpl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.portlet.MimeResponse;
import javax.portlet.PortletException;
import javax.portlet.PortletPreferences;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;
import javax.portlet.ResourceURL;

import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

/**
 * @author Brian Wing Shun Chan
 * @author Vilmos Papp
 * @author Eduardo Garcia
 * @author Raymond Aug??
 */
@Component(
	immediate = true,
	property = {
		"javax.portlet.name=" + SocialActivitiesPortletKeys.SOCIAL_ACTIVITIES,
		"mvc.command.name=rss"
	},
	service = MVCResourceCommand.class
)
public class RSSMVCResourceCommand implements MVCResourceCommand {

	@Override
	public boolean serveResource(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws PortletException {

		if (!(resourceResponse instanceof MimeResponse)) {
			return false;
		}

		PortletPreferences portletPreferences =
			resourceRequest.getPreferences();

		boolean enableRss = GetterUtil.getBoolean(
			portletPreferences.getValue("enableRss", null), true);

		if (!PortalUtil.isRSSFeedsEnabled() || !enableRss) {
			try {
				PortalUtil.sendRSSFeedsDisabledError(
					resourceRequest, resourceResponse);
			}
			catch (Exception e) {
			}

			return false;
		}

		MimeResponse mimeResponse = (MimeResponse)resourceResponse;

		try {
			PortletResponseUtil.sendFile(
				resourceRequest, mimeResponse, null,
				getRSS(resourceRequest, resourceResponse),
				ContentTypes.TEXT_XML_UTF8);
		}
		catch (Exception e) {
			throw new PortletException(e);
		}

		return true;
	}

	protected String exportToRSS(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse,
			String title, String description, String format, double version,
			String displayStyle, List<SocialActivity> socialActivities,
			ServiceContext serviceContext)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		SyndFeed syndFeed = new SyndFeedImpl();

		syndFeed.setDescription(GetterUtil.getString(description, title));

		List<SyndEntry> syndEntries = new ArrayList<>();

		syndFeed.setEntries(syndEntries);

		for (SocialActivity socialActivity : socialActivities) {
			SocialActivityFeedEntry socialActivityFeedEntry =
				_socialActivityInterpreterLocalService.interpret(
					StringPool.BLANK, socialActivity, serviceContext);

			if (socialActivityFeedEntry == null) {
				continue;
			}

			SyndEntry syndEntry = new SyndEntryImpl();

			SyndContent syndContent = new SyndContentImpl();

			syndContent.setType(RSSUtil.ENTRY_TYPE_DEFAULT);

			String value = null;

			if (displayStyle.equals(RSSUtil.DISPLAY_STYLE_TITLE)) {
				value = StringPool.BLANK;
			}
			else {
				value = socialActivityFeedEntry.getBody();
			}

			syndContent.setValue(value);

			syndEntry.setDescription(syndContent);

			if (Validator.isNotNull(socialActivityFeedEntry.getLink())) {
				syndEntry.setLink(socialActivityFeedEntry.getLink());
			}

			syndEntry.setPublishedDate(
				new Date(socialActivity.getCreateDate()));
			syndEntry.setTitle(
				HtmlUtil.extractText(socialActivityFeedEntry.getTitle()));
			syndEntry.setUri(syndEntry.getLink());

			syndEntries.add(syndEntry);
		}

		syndFeed.setFeedType(RSSUtil.getFeedType(format, version));

		List<SyndLink> syndLinks = new ArrayList<>();

		syndFeed.setLinks(syndLinks);

		SyndLink selfSyndLink = new SyndLinkImpl();

		syndLinks.add(selfSyndLink);

		LiferayPortletResponse liferayPortletResponse =
			(LiferayPortletResponse)resourceResponse;

		ResourceURL rssURL = liferayPortletResponse.createResourceURL();

		rssURL.setParameter("feedTitle", title);
		rssURL.setResourceID("rss");

		selfSyndLink.setHref(rssURL.toString());

		selfSyndLink.setRel("self");

		SyndLink alternateSyndLink = new SyndLinkImpl();

		syndLinks.add(alternateSyndLink);

		alternateSyndLink.setHref(PortalUtil.getLayoutFullURL(themeDisplay));
		alternateSyndLink.setRel("alternate");

		syndFeed.setPublishedDate(new Date());
		syndFeed.setTitle(title);
		syndFeed.setUri(rssURL.toString());

		return RSSUtil.export(syndFeed);
	}

	protected byte[] getRSS(
			ResourceRequest resourceRequest, ResourceResponse resourceResponse)
		throws Exception {

		String feedTitle = ParamUtil.getString(resourceRequest, "feedTitle");
		String format = ParamUtil.getString(
			resourceRequest, "type", RSSUtil.FORMAT_DEFAULT);
		double version = ParamUtil.getDouble(
			resourceRequest, "version", RSSUtil.VERSION_DEFAULT);
		String displayStyle = ParamUtil.getString(
			resourceRequest, "displayStyle", RSSUtil.DISPLAY_STYLE_DEFAULT);
		int max = ParamUtil.getInteger(
			resourceRequest, "max", SearchContainer.DEFAULT_DELTA);

		List<SocialActivity> socialActivities = getSocialActivities(
			resourceRequest, max);

		ServiceContext serviceContext = ServiceContextFactory.getInstance(
			resourceRequest);

		String rss = exportToRSS(
			resourceRequest, resourceResponse, feedTitle, null, format, version,
			displayStyle, socialActivities, serviceContext);

		return rss.getBytes(StringPool.UTF8);
	}

	protected List<SocialActivity> getSocialActivities(
			ResourceRequest resourceRequest, int max)
		throws Exception {

		ThemeDisplay themeDisplay = (ThemeDisplay)resourceRequest.getAttribute(
			WebKeys.THEME_DISPLAY);

		Group group = _groupLocalService.getGroup(
			themeDisplay.getScopeGroupId());

		int start = 0;

		if (group.isOrganization()) {
			return _socialActivityLocalService.getOrganizationActivities(
				group.getOrganizationId(), start, max);
		}
		else if (group.isRegularSite()) {
			return _socialActivityLocalService.getGroupActivities(
				group.getGroupId(), start, max);
		}
		else if (group.isUser()) {
			return _socialActivityLocalService.getUserActivities(
				group.getClassPK(), start, max);
		}

		return Collections.emptyList();
	}

	@Reference(unbind = "-")
	protected void setGroupLocalService(GroupLocalService groupLocalService) {
		_groupLocalService = groupLocalService;
	}

	@Reference(unbind = "-")
	protected void setSocialActivityInterpreterLocalService(
		SocialActivityInterpreterLocalService
			socialActivityInterpreterLocalService) {

		_socialActivityInterpreterLocalService =
			socialActivityInterpreterLocalService;
	}

	@Reference(unbind = "-")
	protected void setSocialActivityLocalService(
		SocialActivityLocalService socialActivityLocalService) {

		_socialActivityLocalService = socialActivityLocalService;
	}

	private volatile GroupLocalService _groupLocalService;
	private volatile SocialActivityInterpreterLocalService
		_socialActivityInterpreterLocalService;
	private volatile SocialActivityLocalService _socialActivityLocalService;

}