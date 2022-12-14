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

package com.liferay.portal.servlet;

import static org.mockito.Mockito.verify;

import com.liferay.portal.kernel.util.Validator;
import com.liferay.portal.model.Portlet;
import com.liferay.portal.model.PortletApp;
import com.liferay.portal.service.PortletLocalService;
import com.liferay.portal.service.PortletLocalServiceUtil;
import com.liferay.portal.tools.ToolDependencies;
import com.liferay.portal.util.Portal;
import com.liferay.portal.util.PortalImpl;
import com.liferay.portal.util.PortalUtil;
import com.liferay.portal.util.PortletKeys;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletConfig;
import org.springframework.mock.web.MockServletContext;

/**
 * @author Carlos Sierra Andr??s
 * @author Raymond Aug??
 */
@PrepareForTest({PortletLocalServiceUtil.class})
@RunWith(PowerMockRunner.class)
public class ComboServletTest extends PowerMockito {

	@BeforeClass
	public static void setUpClass() throws Exception {
		ToolDependencies.wireCaches();

		_portal = PortalUtil.getPortal();

		_portalUtil.setPortal(new PortalImpl());
	}

	@AfterClass
	public static void tearDownClass() {
		_portalUtil.setPortal(_portal);
	}

	@Before
	public void setUp() throws ServletException {
		MockitoAnnotations.initMocks(this);

		when(
			_portletLocalService.getPortletById(
				Matchers.anyString()
			)
		).thenAnswer(
			new Answer<Portlet>() {

				@Override
				public Portlet answer(InvocationOnMock invocation)
					throws Throwable {

					Object[] args = invocation.getArguments();

					if (Validator.equals(_TEST_PORTLET_ID, args[0])) {
						return _testPortlet;
					}
					else if (Validator.equals(PortletKeys.PORTAL, args[0])) {
						return _portalPortlet;
					}

					return _portletUndeployed;
				}

			}
		);

		mockStatic(PortletLocalServiceUtil.class, new CallsRealMethods());

		stub(
			method(PortletLocalServiceUtil.class, "getService")
		).toReturn(
			_portletLocalService
		);

		setUpComboServlet();

		setUpPortalServletContext();

		setUpPortalPortlet();

		setUpPluginServletContext();

		setUpTestPortlet();

		when(
			_portletUndeployed.isUndeployedPortlet()
		).thenReturn(
			true
		);

		_mockHttpServletRequest = new MockHttpServletRequest();

		_mockHttpServletRequest.setLocalAddr("localhost");
		_mockHttpServletRequest.setLocalPort(8080);
		_mockHttpServletRequest.setScheme("http");

		_mockHttpServletResponse = new MockHttpServletResponse();
	}

	@Test
	public void testGetResourceRequestDispatcherWithNonexistingPortletId()
		throws Exception {

		RequestDispatcher requestDispatcher =
			_comboServlet.getResourceRequestDispatcher(
				_mockHttpServletRequest, _mockHttpServletResponse,
				"2345678:/js/javascript.js");

		Assert.assertNull(requestDispatcher);
	}

	@Test
	public void testGetResourceRequestDispatcherWithoutPortletId()
		throws Exception {

		String path = "/js/javascript.js";

		_comboServlet.getResourceRequestDispatcher(
			_mockHttpServletRequest, _mockHttpServletResponse,
			"/js/javascript.js");

		verify(_portalServletContext);

		_portalServletContext.getRequestDispatcher(path);
	}

	@Test
	public void testGetResourceWithPortletId() throws Exception {
		_comboServlet.getResourceRequestDispatcher(
			_mockHttpServletRequest, _mockHttpServletResponse,
			_TEST_PORTLET_ID + ":/js/javascript.js");

		verify(_pluginServletContext);

		_pluginServletContext.getRequestDispatcher("/js/javascript.js");
	}

	protected ServletConfig getServletConfig() {
		ServletConfig servletConfig = new MockServletConfig(
			_portalServletContext);

		return servletConfig;
	}

	protected void setUpComboServlet() throws ServletException {
		_comboServlet = new ComboServlet();

		ServletConfig servletConfig = getServletConfig();

		_comboServlet.init(servletConfig);
	}

	protected void setUpPluginServletContext() {
		_pluginServletContext = spy(new MockServletContext());
	}

	protected void setUpPortalPortlet() {
		when(
			_portalPortletApp.getServletContext()
		).thenReturn(
			_portalServletContext
		);

		when(
			_portalPortlet.getPortletApp()
		).thenReturn(
			_portalPortletApp
		);

		when(
			_portalPortlet.getRootPortletId()
		).thenReturn(
			PortletKeys.PORTAL
		);
	}

	protected void setUpPortalServletContext() {
		_portalServletContext = spy(new MockServletContext());

		_portalServletContext.setContextPath("portal");
	}

	protected void setUpTestPortlet() {
		when(
			_testPortletApp.getServletContext()
		).thenReturn(
			_pluginServletContext
		);

		when(
			_testPortlet.getPortletApp()
		).thenReturn(
			_testPortletApp
		);

		when(
			_testPortlet.getRootPortletId()
		).thenReturn(
			_TEST_PORTLET_ID
		);
	}

	private static final String _TEST_PORTLET_ID = "TEST_PORTLET_ID";

	private static Portal _portal;
	private static final PortalUtil _portalUtil = new PortalUtil();

	private ComboServlet _comboServlet;
	private MockHttpServletRequest _mockHttpServletRequest;
	private MockHttpServletResponse _mockHttpServletResponse;
	private MockServletContext _pluginServletContext;

	@Mock
	private Portlet _portalPortlet;

	@Mock
	private PortletApp _portalPortletApp;

	private MockServletContext _portalServletContext;

	@Mock
	private PortletLocalService _portletLocalService;

	@Mock
	private Portlet _portletUndeployed;

	@Mock
	private Portlet _testPortlet;

	@Mock
	private PortletApp _testPortletApp;

}