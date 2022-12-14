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

package com.liferay.journal.service.test;

import com.liferay.arquillian.extension.junit.bridge.junit.Arquillian;
import com.liferay.dynamic.data.mapping.model.DDMForm;
import com.liferay.dynamic.data.mapping.model.DDMFormField;
import com.liferay.dynamic.data.mapping.model.DDMStructure;
import com.liferay.dynamic.data.mapping.model.DDMTemplate;
import com.liferay.dynamic.data.mapping.model.LocalizedValue;
import com.liferay.dynamic.data.mapping.service.DDMTemplateLinkLocalServiceUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMFormTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMStructureTestUtil;
import com.liferay.dynamic.data.mapping.test.util.DDMTemplateTestUtil;
import com.liferay.journal.exception.DuplicateArticleIdException;
import com.liferay.journal.model.JournalArticle;
import com.liferay.journal.model.JournalFolderConstants;
import com.liferay.journal.service.JournalArticleLocalServiceUtil;
import com.liferay.journal.test.util.JournalTestUtil;
import com.liferay.portal.kernel.template.TemplateConstants;
import com.liferay.portal.kernel.test.rule.AggregateTestRule;
import com.liferay.portal.kernel.test.rule.DeleteAfterTestRun;
import com.liferay.portal.kernel.test.rule.Sync;
import com.liferay.portal.kernel.test.rule.SynchronousDestinationTestRule;
import com.liferay.portal.kernel.test.util.GroupTestUtil;
import com.liferay.portal.kernel.test.util.ServiceContextTestUtil;
import com.liferay.portal.kernel.util.LocaleUtil;
import com.liferay.portal.kernel.util.StringPool;
import com.liferay.portal.model.Group;
import com.liferay.portal.model.ResourceConstants;
import com.liferay.portal.model.ResourcePermission;
import com.liferay.portal.service.ClassNameLocalServiceUtil;
import com.liferay.portal.service.ResourcePermissionLocalServiceUtil;
import com.liferay.portal.service.ServiceContext;
import com.liferay.portal.test.rule.LiferayIntegrationTestRule;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portlet.asset.model.AssetEntry;
import com.liferay.portlet.asset.service.AssetEntryLocalServiceUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Michael C. Han
 */
@RunWith(Arquillian.class)
@Sync
public class JournalArticleLocalServiceTest {

	@ClassRule
	@Rule
	public static final AggregateTestRule aggregateTestRule =
		new AggregateTestRule(
			new LiferayIntegrationTestRule(),
			SynchronousDestinationTestRule.INSTANCE);

	@Before
	public void setUp() throws Exception {
		_group = GroupTestUtil.addGroup();
	}

	@Test(expected = DuplicateArticleIdException.class)
	public void testDuplicatedArticleId() throws Exception {
		JournalArticle article = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			article.getArticleId(), false);
	}

	@Test
	public void testDuplicatedAutoGeneratedArticleId() throws Exception {
		JournalArticle article = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
			article.getArticleId(), true);
	}

	@Test
	public void testGetNoAssetArticles() throws Exception {
		JournalArticle article = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		AssetEntry assetEntry = AssetEntryLocalServiceUtil.fetchEntry(
			JournalArticle.class.getName(), article.getResourcePrimKey());

		Assert.assertNotNull(assetEntry);

		AssetEntryLocalServiceUtil.deleteAssetEntry(assetEntry);

		List<JournalArticle> articles =
			JournalArticleLocalServiceUtil.getNoAssetArticles();

		for (JournalArticle curArticle : articles) {
			assetEntry = AssetEntryLocalServiceUtil.fetchEntry(
				JournalArticle.class.getName(),
				curArticle.getResourcePrimKey());

			Assert.assertNull(assetEntry);

			DDMTemplateLinkLocalServiceUtil.deleteTemplateLink(
				PortalUtil.getClassNameId(JournalArticle.class),
				curArticle.getPrimaryKey());

			JournalArticleLocalServiceUtil.deleteJournalArticle(
				curArticle.getPrimaryKey());
		}
	}

	@Test
	public void testGetNoPermissionArticles() throws Exception {
		JournalArticle article = JournalTestUtil.addArticle(
			_group.getGroupId(),
			JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID);

		List<ResourcePermission> resourcePermissions =
			ResourcePermissionLocalServiceUtil.getResourcePermissions(
				article.getCompanyId(), JournalArticle.class.getName(),
				ResourceConstants.SCOPE_INDIVIDUAL,
				String.valueOf(article.getResourcePrimKey()));

		for (ResourcePermission resourcePermission : resourcePermissions) {
			ResourcePermissionLocalServiceUtil.deleteResourcePermission(
				resourcePermission.getResourcePermissionId());
		}

		List<JournalArticle> articles =
			JournalArticleLocalServiceUtil.getNoPermissionArticles();

		Assert.assertEquals(1, articles.size());
		Assert.assertEquals(article, articles.get(0));
	}

	@Test
	public void testUpdateDDMStructurePredefinedValues() throws Exception {
		Set<Locale> availableLocales = DDMFormTestUtil.createAvailableLocales(
			LocaleUtil.BRAZIL, LocaleUtil.FRENCH, LocaleUtil.ITALY,
			LocaleUtil.US);

		DDMForm ddmForm = DDMFormTestUtil.createDDMForm(
			availableLocales, LocaleUtil.US);

		DDMFormField ddmFormField =
			DDMFormTestUtil.createLocalizableTextDDMFormField("name");

		LocalizedValue label = new LocalizedValue(LocaleUtil.US);

		label.addString(LocaleUtil.BRAZIL, "r??tulo");
		label.addString(LocaleUtil.FRENCH, "??tiquette");
		label.addString(LocaleUtil.ITALY, "etichetta");
		label.addString(LocaleUtil.US, "label");

		ddmFormField.setLabel(label);

		ddmForm.addDDMFormField(ddmFormField);

		DDMStructure ddmStructure = DDMStructureTestUtil.addStructure(
			_group.getGroupId(), JournalArticle.class.getName(), ddmForm);

		DDMTemplate ddmTemplate = DDMTemplateTestUtil.addTemplate(
			_group.getGroupId(), ddmStructure.getStructureId(),
			PortalUtil.getClassNameId(JournalArticle.class),
			TemplateConstants.LANG_TYPE_VM,
			JournalTestUtil.getSampleTemplateXSL(), LocaleUtil.US);

		Map<Locale, String> values = new HashMap<>();

		values.put(LocaleUtil.BRAZIL, "Valor Predefinido");
		values.put(LocaleUtil.FRENCH, "Valeur Pr??d??finie");
		values.put(LocaleUtil.ITALY, "Valore Predefinito");
		values.put(LocaleUtil.US, "Predefined Value");

		String content = DDMStructureTestUtil.getSampleStructuredContent(
			values, LocaleUtil.US.toString());

		ServiceContext serviceContext =
			ServiceContextTestUtil.getServiceContext(_group.getGroupId());

		Map<Locale, String> titleMap = new HashMap<>();

		titleMap.put(LocaleUtil.US, "Test Article");

		JournalArticle journalArticle =
			JournalArticleLocalServiceUtil.addArticle(
				serviceContext.getUserId(), serviceContext.getScopeGroupId(),
				JournalFolderConstants.DEFAULT_PARENT_FOLDER_ID,
				ClassNameLocalServiceUtil.getClassNameId(DDMStructure.class),
				ddmStructure.getStructureId(), StringPool.BLANK, true, 0,
				titleMap, null, content, ddmStructure.getStructureKey(),
				ddmTemplate.getTemplateKey(), null, 1, 1, 1965, 0, 0, 0, 0, 0,
				0, 0, true, 0, 0, 0, 0, 0, true, true, false, null, null, null,
				null, serviceContext);

		DDMStructure actualDDMStrucure = journalArticle.getDDMStructure();

		Assert.assertEquals(
			actualDDMStrucure.getStructureId(), ddmStructure.getStructureId());

		DDMFormField actualDDMFormField = actualDDMStrucure.getDDMFormField(
			"name");

		Assert.assertNotNull(actualDDMFormField);

		LocalizedValue actualDDMFormFieldPredefinedValue =
			actualDDMFormField.getPredefinedValue();

		Assert.assertEquals(
			"Valor Predefinido",
			actualDDMFormFieldPredefinedValue.getString(LocaleUtil.BRAZIL));
		Assert.assertEquals(
			"Valeur Pr??d??finie",
			actualDDMFormFieldPredefinedValue.getString(LocaleUtil.FRENCH));
		Assert.assertEquals(
			"Valore Predefinito",
			actualDDMFormFieldPredefinedValue.getString(LocaleUtil.ITALY));
		Assert.assertEquals(
			"Predefined Value",
			actualDDMFormFieldPredefinedValue.getString(LocaleUtil.US));
	}

	@DeleteAfterTestRun
	private Group _group;

}