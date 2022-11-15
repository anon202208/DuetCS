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

package com.liferay.calendar.service.base;

import aQute.bnd.annotation.ProviderType;

import com.liferay.calendar.service.CalendarImporterLocalService;
import com.liferay.calendar.service.persistence.CalendarBookingFinder;
import com.liferay.calendar.service.persistence.CalendarBookingPersistence;
import com.liferay.calendar.service.persistence.CalendarFinder;
import com.liferay.calendar.service.persistence.CalendarNotificationTemplatePersistence;
import com.liferay.calendar.service.persistence.CalendarPersistence;
import com.liferay.calendar.service.persistence.CalendarResourceFinder;
import com.liferay.calendar.service.persistence.CalendarResourcePersistence;

import com.liferay.portal.kernel.bean.BeanReference;
import com.liferay.portal.kernel.dao.db.DB;
import com.liferay.portal.kernel.dao.db.DBManagerUtil;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdate;
import com.liferay.portal.kernel.dao.jdbc.SqlUpdateFactoryUtil;
import com.liferay.portal.kernel.exception.SystemException;
import com.liferay.portal.kernel.module.framework.service.IdentifiableOSGiService;
import com.liferay.portal.kernel.util.InfrastructureUtil;
import com.liferay.portal.service.BaseLocalServiceImpl;
import com.liferay.portal.service.persistence.ClassNamePersistence;
import com.liferay.portal.service.persistence.GroupPersistence;
import com.liferay.portal.service.persistence.ResourceActionPersistence;
import com.liferay.portal.service.persistence.ResourceBlockPersistence;
import com.liferay.portal.service.persistence.ResourcePermissionPersistence;
import com.liferay.portal.service.persistence.RolePersistence;
import com.liferay.portal.service.persistence.SubscriptionPersistence;
import com.liferay.portal.service.persistence.UserPersistence;
import com.liferay.portal.spring.extender.service.ServiceReference;
import com.liferay.portal.util.PortalUtil;

import com.liferay.portlet.asset.service.persistence.AssetCategoryPersistence;
import com.liferay.portlet.asset.service.persistence.AssetEntryPersistence;
import com.liferay.portlet.asset.service.persistence.AssetLinkPersistence;
import com.liferay.portlet.asset.service.persistence.AssetVocabularyPersistence;
import com.liferay.portlet.calendar.service.persistence.CalEventPersistence;
import com.liferay.portlet.messageboards.service.persistence.MBDiscussionPersistence;
import com.liferay.portlet.messageboards.service.persistence.MBMessagePersistence;
import com.liferay.portlet.messageboards.service.persistence.MBThreadPersistence;
import com.liferay.portlet.ratings.service.persistence.RatingsEntryPersistence;
import com.liferay.portlet.ratings.service.persistence.RatingsStatsPersistence;
import com.liferay.portlet.social.service.persistence.SocialActivityPersistence;

import javax.sql.DataSource;

/**
 * Provides the base implementation for the calendar importer local service.
 *
 * <p>
 * This implementation exists only as a container for the default service methods generated by ServiceBuilder. All custom service methods should be put in {@link com.liferay.calendar.service.impl.CalendarImporterLocalServiceImpl}.
 * </p>
 *
 * @author Eduardo Lundgren
 * @see com.liferay.calendar.service.impl.CalendarImporterLocalServiceImpl
 * @see com.liferay.calendar.service.CalendarImporterLocalServiceUtil
 * @generated
 */
@ProviderType
public abstract class CalendarImporterLocalServiceBaseImpl
	extends BaseLocalServiceImpl implements CalendarImporterLocalService,
		IdentifiableOSGiService {
	/*
	 * NOTE FOR DEVELOPERS:
	 *
	 * Never modify or reference this class directly. Always use {@link com.liferay.calendar.service.CalendarImporterLocalServiceUtil} to access the calendar importer local service.
	 */

	/**
	 * Returns the calendar local service.
	 *
	 * @return the calendar local service
	 */
	public com.liferay.calendar.service.CalendarLocalService getCalendarLocalService() {
		return calendarLocalService;
	}

	/**
	 * Sets the calendar local service.
	 *
	 * @param calendarLocalService the calendar local service
	 */
	public void setCalendarLocalService(
		com.liferay.calendar.service.CalendarLocalService calendarLocalService) {
		this.calendarLocalService = calendarLocalService;
	}

	/**
	 * Returns the calendar persistence.
	 *
	 * @return the calendar persistence
	 */
	public CalendarPersistence getCalendarPersistence() {
		return calendarPersistence;
	}

	/**
	 * Sets the calendar persistence.
	 *
	 * @param calendarPersistence the calendar persistence
	 */
	public void setCalendarPersistence(CalendarPersistence calendarPersistence) {
		this.calendarPersistence = calendarPersistence;
	}

	/**
	 * Returns the calendar finder.
	 *
	 * @return the calendar finder
	 */
	public CalendarFinder getCalendarFinder() {
		return calendarFinder;
	}

	/**
	 * Sets the calendar finder.
	 *
	 * @param calendarFinder the calendar finder
	 */
	public void setCalendarFinder(CalendarFinder calendarFinder) {
		this.calendarFinder = calendarFinder;
	}

	/**
	 * Returns the calendar booking local service.
	 *
	 * @return the calendar booking local service
	 */
	public com.liferay.calendar.service.CalendarBookingLocalService getCalendarBookingLocalService() {
		return calendarBookingLocalService;
	}

	/**
	 * Sets the calendar booking local service.
	 *
	 * @param calendarBookingLocalService the calendar booking local service
	 */
	public void setCalendarBookingLocalService(
		com.liferay.calendar.service.CalendarBookingLocalService calendarBookingLocalService) {
		this.calendarBookingLocalService = calendarBookingLocalService;
	}

	/**
	 * Returns the calendar booking persistence.
	 *
	 * @return the calendar booking persistence
	 */
	public CalendarBookingPersistence getCalendarBookingPersistence() {
		return calendarBookingPersistence;
	}

	/**
	 * Sets the calendar booking persistence.
	 *
	 * @param calendarBookingPersistence the calendar booking persistence
	 */
	public void setCalendarBookingPersistence(
		CalendarBookingPersistence calendarBookingPersistence) {
		this.calendarBookingPersistence = calendarBookingPersistence;
	}

	/**
	 * Returns the calendar booking finder.
	 *
	 * @return the calendar booking finder
	 */
	public CalendarBookingFinder getCalendarBookingFinder() {
		return calendarBookingFinder;
	}

	/**
	 * Sets the calendar booking finder.
	 *
	 * @param calendarBookingFinder the calendar booking finder
	 */
	public void setCalendarBookingFinder(
		CalendarBookingFinder calendarBookingFinder) {
		this.calendarBookingFinder = calendarBookingFinder;
	}

	/**
	 * Returns the calendar importer local service.
	 *
	 * @return the calendar importer local service
	 */
	public CalendarImporterLocalService getCalendarImporterLocalService() {
		return calendarImporterLocalService;
	}

	/**
	 * Sets the calendar importer local service.
	 *
	 * @param calendarImporterLocalService the calendar importer local service
	 */
	public void setCalendarImporterLocalService(
		CalendarImporterLocalService calendarImporterLocalService) {
		this.calendarImporterLocalService = calendarImporterLocalService;
	}

	/**
	 * Returns the calendar notification template local service.
	 *
	 * @return the calendar notification template local service
	 */
	public com.liferay.calendar.service.CalendarNotificationTemplateLocalService getCalendarNotificationTemplateLocalService() {
		return calendarNotificationTemplateLocalService;
	}

	/**
	 * Sets the calendar notification template local service.
	 *
	 * @param calendarNotificationTemplateLocalService the calendar notification template local service
	 */
	public void setCalendarNotificationTemplateLocalService(
		com.liferay.calendar.service.CalendarNotificationTemplateLocalService calendarNotificationTemplateLocalService) {
		this.calendarNotificationTemplateLocalService = calendarNotificationTemplateLocalService;
	}

	/**
	 * Returns the calendar notification template persistence.
	 *
	 * @return the calendar notification template persistence
	 */
	public CalendarNotificationTemplatePersistence getCalendarNotificationTemplatePersistence() {
		return calendarNotificationTemplatePersistence;
	}

	/**
	 * Sets the calendar notification template persistence.
	 *
	 * @param calendarNotificationTemplatePersistence the calendar notification template persistence
	 */
	public void setCalendarNotificationTemplatePersistence(
		CalendarNotificationTemplatePersistence calendarNotificationTemplatePersistence) {
		this.calendarNotificationTemplatePersistence = calendarNotificationTemplatePersistence;
	}

	/**
	 * Returns the calendar resource local service.
	 *
	 * @return the calendar resource local service
	 */
	public com.liferay.calendar.service.CalendarResourceLocalService getCalendarResourceLocalService() {
		return calendarResourceLocalService;
	}

	/**
	 * Sets the calendar resource local service.
	 *
	 * @param calendarResourceLocalService the calendar resource local service
	 */
	public void setCalendarResourceLocalService(
		com.liferay.calendar.service.CalendarResourceLocalService calendarResourceLocalService) {
		this.calendarResourceLocalService = calendarResourceLocalService;
	}

	/**
	 * Returns the calendar resource persistence.
	 *
	 * @return the calendar resource persistence
	 */
	public CalendarResourcePersistence getCalendarResourcePersistence() {
		return calendarResourcePersistence;
	}

	/**
	 * Sets the calendar resource persistence.
	 *
	 * @param calendarResourcePersistence the calendar resource persistence
	 */
	public void setCalendarResourcePersistence(
		CalendarResourcePersistence calendarResourcePersistence) {
		this.calendarResourcePersistence = calendarResourcePersistence;
	}

	/**
	 * Returns the calendar resource finder.
	 *
	 * @return the calendar resource finder
	 */
	public CalendarResourceFinder getCalendarResourceFinder() {
		return calendarResourceFinder;
	}

	/**
	 * Sets the calendar resource finder.
	 *
	 * @param calendarResourceFinder the calendar resource finder
	 */
	public void setCalendarResourceFinder(
		CalendarResourceFinder calendarResourceFinder) {
		this.calendarResourceFinder = calendarResourceFinder;
	}

	/**
	 * Returns the counter local service.
	 *
	 * @return the counter local service
	 */
	public com.liferay.counter.service.CounterLocalService getCounterLocalService() {
		return counterLocalService;
	}

	/**
	 * Sets the counter local service.
	 *
	 * @param counterLocalService the counter local service
	 */
	public void setCounterLocalService(
		com.liferay.counter.service.CounterLocalService counterLocalService) {
		this.counterLocalService = counterLocalService;
	}

	/**
	 * Returns the class name local service.
	 *
	 * @return the class name local service
	 */
	public com.liferay.portal.service.ClassNameLocalService getClassNameLocalService() {
		return classNameLocalService;
	}

	/**
	 * Sets the class name local service.
	 *
	 * @param classNameLocalService the class name local service
	 */
	public void setClassNameLocalService(
		com.liferay.portal.service.ClassNameLocalService classNameLocalService) {
		this.classNameLocalService = classNameLocalService;
	}

	/**
	 * Returns the class name persistence.
	 *
	 * @return the class name persistence
	 */
	public ClassNamePersistence getClassNamePersistence() {
		return classNamePersistence;
	}

	/**
	 * Sets the class name persistence.
	 *
	 * @param classNamePersistence the class name persistence
	 */
	public void setClassNamePersistence(
		ClassNamePersistence classNamePersistence) {
		this.classNamePersistence = classNamePersistence;
	}

	/**
	 * Returns the group local service.
	 *
	 * @return the group local service
	 */
	public com.liferay.portal.service.GroupLocalService getGroupLocalService() {
		return groupLocalService;
	}

	/**
	 * Sets the group local service.
	 *
	 * @param groupLocalService the group local service
	 */
	public void setGroupLocalService(
		com.liferay.portal.service.GroupLocalService groupLocalService) {
		this.groupLocalService = groupLocalService;
	}

	/**
	 * Returns the group persistence.
	 *
	 * @return the group persistence
	 */
	public GroupPersistence getGroupPersistence() {
		return groupPersistence;
	}

	/**
	 * Sets the group persistence.
	 *
	 * @param groupPersistence the group persistence
	 */
	public void setGroupPersistence(GroupPersistence groupPersistence) {
		this.groupPersistence = groupPersistence;
	}

	/**
	 * Returns the resource local service.
	 *
	 * @return the resource local service
	 */
	public com.liferay.portal.service.ResourceLocalService getResourceLocalService() {
		return resourceLocalService;
	}

	/**
	 * Sets the resource local service.
	 *
	 * @param resourceLocalService the resource local service
	 */
	public void setResourceLocalService(
		com.liferay.portal.service.ResourceLocalService resourceLocalService) {
		this.resourceLocalService = resourceLocalService;
	}

	/**
	 * Returns the resource action local service.
	 *
	 * @return the resource action local service
	 */
	public com.liferay.portal.service.ResourceActionLocalService getResourceActionLocalService() {
		return resourceActionLocalService;
	}

	/**
	 * Sets the resource action local service.
	 *
	 * @param resourceActionLocalService the resource action local service
	 */
	public void setResourceActionLocalService(
		com.liferay.portal.service.ResourceActionLocalService resourceActionLocalService) {
		this.resourceActionLocalService = resourceActionLocalService;
	}

	/**
	 * Returns the resource action persistence.
	 *
	 * @return the resource action persistence
	 */
	public ResourceActionPersistence getResourceActionPersistence() {
		return resourceActionPersistence;
	}

	/**
	 * Sets the resource action persistence.
	 *
	 * @param resourceActionPersistence the resource action persistence
	 */
	public void setResourceActionPersistence(
		ResourceActionPersistence resourceActionPersistence) {
		this.resourceActionPersistence = resourceActionPersistence;
	}

	/**
	 * Returns the resource block local service.
	 *
	 * @return the resource block local service
	 */
	public com.liferay.portal.service.ResourceBlockLocalService getResourceBlockLocalService() {
		return resourceBlockLocalService;
	}

	/**
	 * Sets the resource block local service.
	 *
	 * @param resourceBlockLocalService the resource block local service
	 */
	public void setResourceBlockLocalService(
		com.liferay.portal.service.ResourceBlockLocalService resourceBlockLocalService) {
		this.resourceBlockLocalService = resourceBlockLocalService;
	}

	/**
	 * Returns the resource block persistence.
	 *
	 * @return the resource block persistence
	 */
	public ResourceBlockPersistence getResourceBlockPersistence() {
		return resourceBlockPersistence;
	}

	/**
	 * Sets the resource block persistence.
	 *
	 * @param resourceBlockPersistence the resource block persistence
	 */
	public void setResourceBlockPersistence(
		ResourceBlockPersistence resourceBlockPersistence) {
		this.resourceBlockPersistence = resourceBlockPersistence;
	}

	/**
	 * Returns the resource permission local service.
	 *
	 * @return the resource permission local service
	 */
	public com.liferay.portal.service.ResourcePermissionLocalService getResourcePermissionLocalService() {
		return resourcePermissionLocalService;
	}

	/**
	 * Sets the resource permission local service.
	 *
	 * @param resourcePermissionLocalService the resource permission local service
	 */
	public void setResourcePermissionLocalService(
		com.liferay.portal.service.ResourcePermissionLocalService resourcePermissionLocalService) {
		this.resourcePermissionLocalService = resourcePermissionLocalService;
	}

	/**
	 * Returns the resource permission persistence.
	 *
	 * @return the resource permission persistence
	 */
	public ResourcePermissionPersistence getResourcePermissionPersistence() {
		return resourcePermissionPersistence;
	}

	/**
	 * Sets the resource permission persistence.
	 *
	 * @param resourcePermissionPersistence the resource permission persistence
	 */
	public void setResourcePermissionPersistence(
		ResourcePermissionPersistence resourcePermissionPersistence) {
		this.resourcePermissionPersistence = resourcePermissionPersistence;
	}

	/**
	 * Returns the role local service.
	 *
	 * @return the role local service
	 */
	public com.liferay.portal.service.RoleLocalService getRoleLocalService() {
		return roleLocalService;
	}

	/**
	 * Sets the role local service.
	 *
	 * @param roleLocalService the role local service
	 */
	public void setRoleLocalService(
		com.liferay.portal.service.RoleLocalService roleLocalService) {
		this.roleLocalService = roleLocalService;
	}

	/**
	 * Returns the role persistence.
	 *
	 * @return the role persistence
	 */
	public RolePersistence getRolePersistence() {
		return rolePersistence;
	}

	/**
	 * Sets the role persistence.
	 *
	 * @param rolePersistence the role persistence
	 */
	public void setRolePersistence(RolePersistence rolePersistence) {
		this.rolePersistence = rolePersistence;
	}

	/**
	 * Returns the subscription local service.
	 *
	 * @return the subscription local service
	 */
	public com.liferay.portal.service.SubscriptionLocalService getSubscriptionLocalService() {
		return subscriptionLocalService;
	}

	/**
	 * Sets the subscription local service.
	 *
	 * @param subscriptionLocalService the subscription local service
	 */
	public void setSubscriptionLocalService(
		com.liferay.portal.service.SubscriptionLocalService subscriptionLocalService) {
		this.subscriptionLocalService = subscriptionLocalService;
	}

	/**
	 * Returns the subscription persistence.
	 *
	 * @return the subscription persistence
	 */
	public SubscriptionPersistence getSubscriptionPersistence() {
		return subscriptionPersistence;
	}

	/**
	 * Sets the subscription persistence.
	 *
	 * @param subscriptionPersistence the subscription persistence
	 */
	public void setSubscriptionPersistence(
		SubscriptionPersistence subscriptionPersistence) {
		this.subscriptionPersistence = subscriptionPersistence;
	}

	/**
	 * Returns the user local service.
	 *
	 * @return the user local service
	 */
	public com.liferay.portal.service.UserLocalService getUserLocalService() {
		return userLocalService;
	}

	/**
	 * Sets the user local service.
	 *
	 * @param userLocalService the user local service
	 */
	public void setUserLocalService(
		com.liferay.portal.service.UserLocalService userLocalService) {
		this.userLocalService = userLocalService;
	}

	/**
	 * Returns the user persistence.
	 *
	 * @return the user persistence
	 */
	public UserPersistence getUserPersistence() {
		return userPersistence;
	}

	/**
	 * Sets the user persistence.
	 *
	 * @param userPersistence the user persistence
	 */
	public void setUserPersistence(UserPersistence userPersistence) {
		this.userPersistence = userPersistence;
	}

	/**
	 * Returns the asset category local service.
	 *
	 * @return the asset category local service
	 */
	public com.liferay.portlet.asset.service.AssetCategoryLocalService getAssetCategoryLocalService() {
		return assetCategoryLocalService;
	}

	/**
	 * Sets the asset category local service.
	 *
	 * @param assetCategoryLocalService the asset category local service
	 */
	public void setAssetCategoryLocalService(
		com.liferay.portlet.asset.service.AssetCategoryLocalService assetCategoryLocalService) {
		this.assetCategoryLocalService = assetCategoryLocalService;
	}

	/**
	 * Returns the asset category persistence.
	 *
	 * @return the asset category persistence
	 */
	public AssetCategoryPersistence getAssetCategoryPersistence() {
		return assetCategoryPersistence;
	}

	/**
	 * Sets the asset category persistence.
	 *
	 * @param assetCategoryPersistence the asset category persistence
	 */
	public void setAssetCategoryPersistence(
		AssetCategoryPersistence assetCategoryPersistence) {
		this.assetCategoryPersistence = assetCategoryPersistence;
	}

	/**
	 * Returns the asset entry local service.
	 *
	 * @return the asset entry local service
	 */
	public com.liferay.portlet.asset.service.AssetEntryLocalService getAssetEntryLocalService() {
		return assetEntryLocalService;
	}

	/**
	 * Sets the asset entry local service.
	 *
	 * @param assetEntryLocalService the asset entry local service
	 */
	public void setAssetEntryLocalService(
		com.liferay.portlet.asset.service.AssetEntryLocalService assetEntryLocalService) {
		this.assetEntryLocalService = assetEntryLocalService;
	}

	/**
	 * Returns the asset entry persistence.
	 *
	 * @return the asset entry persistence
	 */
	public AssetEntryPersistence getAssetEntryPersistence() {
		return assetEntryPersistence;
	}

	/**
	 * Sets the asset entry persistence.
	 *
	 * @param assetEntryPersistence the asset entry persistence
	 */
	public void setAssetEntryPersistence(
		AssetEntryPersistence assetEntryPersistence) {
		this.assetEntryPersistence = assetEntryPersistence;
	}

	/**
	 * Returns the asset link local service.
	 *
	 * @return the asset link local service
	 */
	public com.liferay.portlet.asset.service.AssetLinkLocalService getAssetLinkLocalService() {
		return assetLinkLocalService;
	}

	/**
	 * Sets the asset link local service.
	 *
	 * @param assetLinkLocalService the asset link local service
	 */
	public void setAssetLinkLocalService(
		com.liferay.portlet.asset.service.AssetLinkLocalService assetLinkLocalService) {
		this.assetLinkLocalService = assetLinkLocalService;
	}

	/**
	 * Returns the asset link persistence.
	 *
	 * @return the asset link persistence
	 */
	public AssetLinkPersistence getAssetLinkPersistence() {
		return assetLinkPersistence;
	}

	/**
	 * Sets the asset link persistence.
	 *
	 * @param assetLinkPersistence the asset link persistence
	 */
	public void setAssetLinkPersistence(
		AssetLinkPersistence assetLinkPersistence) {
		this.assetLinkPersistence = assetLinkPersistence;
	}

	/**
	 * Returns the asset vocabulary local service.
	 *
	 * @return the asset vocabulary local service
	 */
	public com.liferay.portlet.asset.service.AssetVocabularyLocalService getAssetVocabularyLocalService() {
		return assetVocabularyLocalService;
	}

	/**
	 * Sets the asset vocabulary local service.
	 *
	 * @param assetVocabularyLocalService the asset vocabulary local service
	 */
	public void setAssetVocabularyLocalService(
		com.liferay.portlet.asset.service.AssetVocabularyLocalService assetVocabularyLocalService) {
		this.assetVocabularyLocalService = assetVocabularyLocalService;
	}

	/**
	 * Returns the asset vocabulary persistence.
	 *
	 * @return the asset vocabulary persistence
	 */
	public AssetVocabularyPersistence getAssetVocabularyPersistence() {
		return assetVocabularyPersistence;
	}

	/**
	 * Sets the asset vocabulary persistence.
	 *
	 * @param assetVocabularyPersistence the asset vocabulary persistence
	 */
	public void setAssetVocabularyPersistence(
		AssetVocabularyPersistence assetVocabularyPersistence) {
		this.assetVocabularyPersistence = assetVocabularyPersistence;
	}

	/**
	 * Returns the cal event local service.
	 *
	 * @return the cal event local service
	 */
	public com.liferay.portlet.calendar.service.CalEventLocalService getCalEventLocalService() {
		return calEventLocalService;
	}

	/**
	 * Sets the cal event local service.
	 *
	 * @param calEventLocalService the cal event local service
	 */
	public void setCalEventLocalService(
		com.liferay.portlet.calendar.service.CalEventLocalService calEventLocalService) {
		this.calEventLocalService = calEventLocalService;
	}

	/**
	 * Returns the cal event persistence.
	 *
	 * @return the cal event persistence
	 */
	public CalEventPersistence getCalEventPersistence() {
		return calEventPersistence;
	}

	/**
	 * Sets the cal event persistence.
	 *
	 * @param calEventPersistence the cal event persistence
	 */
	public void setCalEventPersistence(CalEventPersistence calEventPersistence) {
		this.calEventPersistence = calEventPersistence;
	}

	/**
	 * Returns the message boards discussion local service.
	 *
	 * @return the message boards discussion local service
	 */
	public com.liferay.portlet.messageboards.service.MBDiscussionLocalService getMBDiscussionLocalService() {
		return mbDiscussionLocalService;
	}

	/**
	 * Sets the message boards discussion local service.
	 *
	 * @param mbDiscussionLocalService the message boards discussion local service
	 */
	public void setMBDiscussionLocalService(
		com.liferay.portlet.messageboards.service.MBDiscussionLocalService mbDiscussionLocalService) {
		this.mbDiscussionLocalService = mbDiscussionLocalService;
	}

	/**
	 * Returns the message boards discussion persistence.
	 *
	 * @return the message boards discussion persistence
	 */
	public MBDiscussionPersistence getMBDiscussionPersistence() {
		return mbDiscussionPersistence;
	}

	/**
	 * Sets the message boards discussion persistence.
	 *
	 * @param mbDiscussionPersistence the message boards discussion persistence
	 */
	public void setMBDiscussionPersistence(
		MBDiscussionPersistence mbDiscussionPersistence) {
		this.mbDiscussionPersistence = mbDiscussionPersistence;
	}

	/**
	 * Returns the message-boards message local service.
	 *
	 * @return the message-boards message local service
	 */
	public com.liferay.portlet.messageboards.service.MBMessageLocalService getMBMessageLocalService() {
		return mbMessageLocalService;
	}

	/**
	 * Sets the message-boards message local service.
	 *
	 * @param mbMessageLocalService the message-boards message local service
	 */
	public void setMBMessageLocalService(
		com.liferay.portlet.messageboards.service.MBMessageLocalService mbMessageLocalService) {
		this.mbMessageLocalService = mbMessageLocalService;
	}

	/**
	 * Returns the message-boards message persistence.
	 *
	 * @return the message-boards message persistence
	 */
	public MBMessagePersistence getMBMessagePersistence() {
		return mbMessagePersistence;
	}

	/**
	 * Sets the message-boards message persistence.
	 *
	 * @param mbMessagePersistence the message-boards message persistence
	 */
	public void setMBMessagePersistence(
		MBMessagePersistence mbMessagePersistence) {
		this.mbMessagePersistence = mbMessagePersistence;
	}

	/**
	 * Returns the message boards thread local service.
	 *
	 * @return the message boards thread local service
	 */
	public com.liferay.portlet.messageboards.service.MBThreadLocalService getMBThreadLocalService() {
		return mbThreadLocalService;
	}

	/**
	 * Sets the message boards thread local service.
	 *
	 * @param mbThreadLocalService the message boards thread local service
	 */
	public void setMBThreadLocalService(
		com.liferay.portlet.messageboards.service.MBThreadLocalService mbThreadLocalService) {
		this.mbThreadLocalService = mbThreadLocalService;
	}

	/**
	 * Returns the message boards thread persistence.
	 *
	 * @return the message boards thread persistence
	 */
	public MBThreadPersistence getMBThreadPersistence() {
		return mbThreadPersistence;
	}

	/**
	 * Sets the message boards thread persistence.
	 *
	 * @param mbThreadPersistence the message boards thread persistence
	 */
	public void setMBThreadPersistence(MBThreadPersistence mbThreadPersistence) {
		this.mbThreadPersistence = mbThreadPersistence;
	}

	/**
	 * Returns the ratings entry local service.
	 *
	 * @return the ratings entry local service
	 */
	public com.liferay.portlet.ratings.service.RatingsEntryLocalService getRatingsEntryLocalService() {
		return ratingsEntryLocalService;
	}

	/**
	 * Sets the ratings entry local service.
	 *
	 * @param ratingsEntryLocalService the ratings entry local service
	 */
	public void setRatingsEntryLocalService(
		com.liferay.portlet.ratings.service.RatingsEntryLocalService ratingsEntryLocalService) {
		this.ratingsEntryLocalService = ratingsEntryLocalService;
	}

	/**
	 * Returns the ratings entry persistence.
	 *
	 * @return the ratings entry persistence
	 */
	public RatingsEntryPersistence getRatingsEntryPersistence() {
		return ratingsEntryPersistence;
	}

	/**
	 * Sets the ratings entry persistence.
	 *
	 * @param ratingsEntryPersistence the ratings entry persistence
	 */
	public void setRatingsEntryPersistence(
		RatingsEntryPersistence ratingsEntryPersistence) {
		this.ratingsEntryPersistence = ratingsEntryPersistence;
	}

	/**
	 * Returns the ratings stats local service.
	 *
	 * @return the ratings stats local service
	 */
	public com.liferay.portlet.ratings.service.RatingsStatsLocalService getRatingsStatsLocalService() {
		return ratingsStatsLocalService;
	}

	/**
	 * Sets the ratings stats local service.
	 *
	 * @param ratingsStatsLocalService the ratings stats local service
	 */
	public void setRatingsStatsLocalService(
		com.liferay.portlet.ratings.service.RatingsStatsLocalService ratingsStatsLocalService) {
		this.ratingsStatsLocalService = ratingsStatsLocalService;
	}

	/**
	 * Returns the ratings stats persistence.
	 *
	 * @return the ratings stats persistence
	 */
	public RatingsStatsPersistence getRatingsStatsPersistence() {
		return ratingsStatsPersistence;
	}

	/**
	 * Sets the ratings stats persistence.
	 *
	 * @param ratingsStatsPersistence the ratings stats persistence
	 */
	public void setRatingsStatsPersistence(
		RatingsStatsPersistence ratingsStatsPersistence) {
		this.ratingsStatsPersistence = ratingsStatsPersistence;
	}

	/**
	 * Returns the social activity local service.
	 *
	 * @return the social activity local service
	 */
	public com.liferay.portlet.social.service.SocialActivityLocalService getSocialActivityLocalService() {
		return socialActivityLocalService;
	}

	/**
	 * Sets the social activity local service.
	 *
	 * @param socialActivityLocalService the social activity local service
	 */
	public void setSocialActivityLocalService(
		com.liferay.portlet.social.service.SocialActivityLocalService socialActivityLocalService) {
		this.socialActivityLocalService = socialActivityLocalService;
	}

	/**
	 * Returns the social activity persistence.
	 *
	 * @return the social activity persistence
	 */
	public SocialActivityPersistence getSocialActivityPersistence() {
		return socialActivityPersistence;
	}

	/**
	 * Sets the social activity persistence.
	 *
	 * @param socialActivityPersistence the social activity persistence
	 */
	public void setSocialActivityPersistence(
		SocialActivityPersistence socialActivityPersistence) {
		this.socialActivityPersistence = socialActivityPersistence;
	}

	public void afterPropertiesSet() {
	}

	public void destroy() {
	}

	/**
	 * Returns the OSGi service identifier.
	 *
	 * @return the OSGi service identifier
	 */
	@Override
	public String getOSGiServiceIdentifier() {
		return CalendarImporterLocalService.class.getName();
	}

	/**
	 * Performs a SQL query.
	 *
	 * @param sql the sql query
	 */
	protected void runSQL(String sql) {
		try {
			DataSource dataSource = InfrastructureUtil.getDataSource();

			DB db = DBManagerUtil.getDB();

			sql = db.buildSQL(sql);
			sql = PortalUtil.transformSQL(sql);

			SqlUpdate sqlUpdate = SqlUpdateFactoryUtil.getSqlUpdate(dataSource,
					sql, new int[0]);

			sqlUpdate.update();
		}
		catch (Exception e) {
			throw new SystemException(e);
		}
	}

	@BeanReference(type = com.liferay.calendar.service.CalendarLocalService.class)
	protected com.liferay.calendar.service.CalendarLocalService calendarLocalService;
	@BeanReference(type = CalendarPersistence.class)
	protected CalendarPersistence calendarPersistence;
	@BeanReference(type = CalendarFinder.class)
	protected CalendarFinder calendarFinder;
	@BeanReference(type = com.liferay.calendar.service.CalendarBookingLocalService.class)
	protected com.liferay.calendar.service.CalendarBookingLocalService calendarBookingLocalService;
	@BeanReference(type = CalendarBookingPersistence.class)
	protected CalendarBookingPersistence calendarBookingPersistence;
	@BeanReference(type = CalendarBookingFinder.class)
	protected CalendarBookingFinder calendarBookingFinder;
	@BeanReference(type = com.liferay.calendar.service.CalendarImporterLocalService.class)
	protected CalendarImporterLocalService calendarImporterLocalService;
	@BeanReference(type = com.liferay.calendar.service.CalendarNotificationTemplateLocalService.class)
	protected com.liferay.calendar.service.CalendarNotificationTemplateLocalService calendarNotificationTemplateLocalService;
	@BeanReference(type = CalendarNotificationTemplatePersistence.class)
	protected CalendarNotificationTemplatePersistence calendarNotificationTemplatePersistence;
	@BeanReference(type = com.liferay.calendar.service.CalendarResourceLocalService.class)
	protected com.liferay.calendar.service.CalendarResourceLocalService calendarResourceLocalService;
	@BeanReference(type = CalendarResourcePersistence.class)
	protected CalendarResourcePersistence calendarResourcePersistence;
	@BeanReference(type = CalendarResourceFinder.class)
	protected CalendarResourceFinder calendarResourceFinder;
	@ServiceReference(type = com.liferay.counter.service.CounterLocalService.class)
	protected com.liferay.counter.service.CounterLocalService counterLocalService;
	@ServiceReference(type = com.liferay.portal.service.ClassNameLocalService.class)
	protected com.liferay.portal.service.ClassNameLocalService classNameLocalService;
	@ServiceReference(type = ClassNamePersistence.class)
	protected ClassNamePersistence classNamePersistence;
	@ServiceReference(type = com.liferay.portal.service.GroupLocalService.class)
	protected com.liferay.portal.service.GroupLocalService groupLocalService;
	@ServiceReference(type = GroupPersistence.class)
	protected GroupPersistence groupPersistence;
	@ServiceReference(type = com.liferay.portal.service.ResourceLocalService.class)
	protected com.liferay.portal.service.ResourceLocalService resourceLocalService;
	@ServiceReference(type = com.liferay.portal.service.ResourceActionLocalService.class)
	protected com.liferay.portal.service.ResourceActionLocalService resourceActionLocalService;
	@ServiceReference(type = ResourceActionPersistence.class)
	protected ResourceActionPersistence resourceActionPersistence;
	@ServiceReference(type = com.liferay.portal.service.ResourceBlockLocalService.class)
	protected com.liferay.portal.service.ResourceBlockLocalService resourceBlockLocalService;
	@ServiceReference(type = ResourceBlockPersistence.class)
	protected ResourceBlockPersistence resourceBlockPersistence;
	@ServiceReference(type = com.liferay.portal.service.ResourcePermissionLocalService.class)
	protected com.liferay.portal.service.ResourcePermissionLocalService resourcePermissionLocalService;
	@ServiceReference(type = ResourcePermissionPersistence.class)
	protected ResourcePermissionPersistence resourcePermissionPersistence;
	@ServiceReference(type = com.liferay.portal.service.RoleLocalService.class)
	protected com.liferay.portal.service.RoleLocalService roleLocalService;
	@ServiceReference(type = RolePersistence.class)
	protected RolePersistence rolePersistence;
	@ServiceReference(type = com.liferay.portal.service.SubscriptionLocalService.class)
	protected com.liferay.portal.service.SubscriptionLocalService subscriptionLocalService;
	@ServiceReference(type = SubscriptionPersistence.class)
	protected SubscriptionPersistence subscriptionPersistence;
	@ServiceReference(type = com.liferay.portal.service.UserLocalService.class)
	protected com.liferay.portal.service.UserLocalService userLocalService;
	@ServiceReference(type = UserPersistence.class)
	protected UserPersistence userPersistence;
	@ServiceReference(type = com.liferay.portlet.asset.service.AssetCategoryLocalService.class)
	protected com.liferay.portlet.asset.service.AssetCategoryLocalService assetCategoryLocalService;
	@ServiceReference(type = AssetCategoryPersistence.class)
	protected AssetCategoryPersistence assetCategoryPersistence;
	@ServiceReference(type = com.liferay.portlet.asset.service.AssetEntryLocalService.class)
	protected com.liferay.portlet.asset.service.AssetEntryLocalService assetEntryLocalService;
	@ServiceReference(type = AssetEntryPersistence.class)
	protected AssetEntryPersistence assetEntryPersistence;
	@ServiceReference(type = com.liferay.portlet.asset.service.AssetLinkLocalService.class)
	protected com.liferay.portlet.asset.service.AssetLinkLocalService assetLinkLocalService;
	@ServiceReference(type = AssetLinkPersistence.class)
	protected AssetLinkPersistence assetLinkPersistence;
	@ServiceReference(type = com.liferay.portlet.asset.service.AssetVocabularyLocalService.class)
	protected com.liferay.portlet.asset.service.AssetVocabularyLocalService assetVocabularyLocalService;
	@ServiceReference(type = AssetVocabularyPersistence.class)
	protected AssetVocabularyPersistence assetVocabularyPersistence;
	@ServiceReference(type = com.liferay.portlet.calendar.service.CalEventLocalService.class)
	protected com.liferay.portlet.calendar.service.CalEventLocalService calEventLocalService;
	@ServiceReference(type = CalEventPersistence.class)
	protected CalEventPersistence calEventPersistence;
	@ServiceReference(type = com.liferay.portlet.messageboards.service.MBDiscussionLocalService.class)
	protected com.liferay.portlet.messageboards.service.MBDiscussionLocalService mbDiscussionLocalService;
	@ServiceReference(type = MBDiscussionPersistence.class)
	protected MBDiscussionPersistence mbDiscussionPersistence;
	@ServiceReference(type = com.liferay.portlet.messageboards.service.MBMessageLocalService.class)
	protected com.liferay.portlet.messageboards.service.MBMessageLocalService mbMessageLocalService;
	@ServiceReference(type = MBMessagePersistence.class)
	protected MBMessagePersistence mbMessagePersistence;
	@ServiceReference(type = com.liferay.portlet.messageboards.service.MBThreadLocalService.class)
	protected com.liferay.portlet.messageboards.service.MBThreadLocalService mbThreadLocalService;
	@ServiceReference(type = MBThreadPersistence.class)
	protected MBThreadPersistence mbThreadPersistence;
	@ServiceReference(type = com.liferay.portlet.ratings.service.RatingsEntryLocalService.class)
	protected com.liferay.portlet.ratings.service.RatingsEntryLocalService ratingsEntryLocalService;
	@ServiceReference(type = RatingsEntryPersistence.class)
	protected RatingsEntryPersistence ratingsEntryPersistence;
	@ServiceReference(type = com.liferay.portlet.ratings.service.RatingsStatsLocalService.class)
	protected com.liferay.portlet.ratings.service.RatingsStatsLocalService ratingsStatsLocalService;
	@ServiceReference(type = RatingsStatsPersistence.class)
	protected RatingsStatsPersistence ratingsStatsPersistence;
	@ServiceReference(type = com.liferay.portlet.social.service.SocialActivityLocalService.class)
	protected com.liferay.portlet.social.service.SocialActivityLocalService socialActivityLocalService;
	@ServiceReference(type = SocialActivityPersistence.class)
	protected SocialActivityPersistence socialActivityPersistence;
}