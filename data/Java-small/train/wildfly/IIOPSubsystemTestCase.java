/*
* JBoss, Home of Professional Open Source.
* Copyright 2011, Red Hat Middleware LLC, and individual contributors
* as indicated by the @author tags. See the copyright.txt file in the
* distribution for a full listing of individual contributors.
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
package org.wildfly.iiop.openjdk;

import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.ADD;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.DESCRIBE;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.OP_ADDR;
import static org.jboss.as.controller.descriptions.ModelDescriptionConstants.SUBSYSTEM;

import java.io.IOException;
import java.util.List;

import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.PathAddress;
import org.jboss.as.controller.PathElement;
import org.jboss.as.subsystem.test.AbstractSubsystemBaseTest;
import org.jboss.as.subsystem.test.AdditionalInitialization;
import org.jboss.as.subsystem.test.ControllerInitializer;
import org.jboss.as.subsystem.test.KernelServices;
import org.jboss.dmr.ModelNode;
import org.junit.Assert;
import org.junit.Test;
import org.wildfly.iiop.openjdk.IIOPExtension;
import org.wildfly.iiop.openjdk.Namespace;

/**
 * <???>
 * IIOP subsystem tests.
 * </???>
 *
 * @author <a href="kabir.khan@jboss.com">Kabir Khan</a>
 * @author <a href="sguilhen@jboss.com">Stefan Guilhen</a>
 * @author <a href="mailto:tadamski@redhat.com">Tomasz Adamski</a>
 */
public class IIOPSubsystemTestCase extends AbstractSubsystemBaseTest {

    public IIOPSubsystemTestCase() {
        super(IIOPExtension.SUBSYSTEM_NAME, new IIOPExtension());
    }

    @Override
    protected String getSubsystemXml() throws IOException {
        return readResource("subsystem-1.0.xml");
    }

    @Override
    protected String getSubsystemXml(String configId) throws IOException {
        return readResource(configId);
    }

    @Test
    public void testExpressions() throws Exception {
        standardSubsystemTest("expressions-1.0.xml");
    }

    @Override
    protected String getSubsystemXsdPath() throws Exception {
        return "schema/jboss-as-iiop-openjdk_1_0.xsd";
    }

    @Override
    protected String[] getSubsystemTemplatePaths() throws IOException {
        return new String[] {
                "/subsystem-templates/iiop-openjdk.xml"
        };
    }

    @Test
    public void testParseEmptySubsystem() throws Exception {
        // parse the subsystem xml into operations.
        String subsystemXml =
                "<subsystem xmlns=\"" +Namespace.CURRENT.getUriString() + "\">" +
                "</subsystem>";
        List<ModelNode> operations = super.parse(subsystemXml);

        // check that we have the expected number of operations.
        Assert.assertEquals(1, operations.size());

        // check that each operation has the correct content.
        ModelNode addSubsystem = operations.get(0);
        Assert.assertEquals(ADD, addSubsystem.get(OP).asString());
        PathAddress addr = PathAddress.pathAddress(addSubsystem.get(OP_ADDR));
        Assert.assertEquals(1, addr.size());
        PathElement element = addr.getElement(0);
        Assert.assertEquals(SUBSYSTEM, element.getKey());
        Assert.assertEquals(IIOPExtension.SUBSYSTEM_NAME, element.getValue());
    }

    @Test
    public void testParseSubsystemWithBadChild() throws Exception {
        // try parsing a XML with an invalid element.
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "   <invalid/>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

        // now try parsing a valid element in an invalid position.
        subsystemXml =
                "<subsystem xmlns=\"urn:jboss:domain:iiop-openjdk:1.0\">" +
                "    <orb>" +
                "        <poa/>" +
                "    </orb>" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad child");
        } catch (XMLStreamException expected) {
        }

    }

    @Test
    public void testParseSubsystemWithBadAttribute() throws Exception {
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\" bad=\"very_bad\">" +
                "</subsystem>";
        try {
            super.parse(subsystemXml);
            Assert.fail("Should not have parsed bad attribute");
        } catch (XMLStreamException expected) {
        }
    }

    @Test
    public void testDescribeHandler() throws Exception {
        // parse the subsystem xml and install into the first controller.
        String subsystemXml =
                "<subsystem xmlns=\"" + Namespace.CURRENT.getUriString() + "\">" +
                "<orb socket-binding=\"iiop\" ssl-socket-binding=\"iiop-ssl\"/>"+
                "</subsystem>";

        AdditionalInitialization additionalInit = new AdditionalInitialization(){
            @Override
            protected void setupController(ControllerInitializer controllerInitializer) {
                controllerInitializer.addSocketBinding("iiop", 3528);
                controllerInitializer.addSocketBinding("iiop-ssl", 3529);
            }
        };

        KernelServices servicesA = createKernelServicesBuilder(additionalInit)
                .setSubsystemXml(subsystemXml)
                .build();
        // get the model and the describe operations from the first controller.
        ModelNode modelA = servicesA.readWholeModel();
        ModelNode describeOp = new ModelNode();
        describeOp.get(OP).set(DESCRIBE);
        describeOp.get(OP_ADDR).set(
                PathAddress.pathAddress(
                        PathElement.pathElement(SUBSYSTEM, IIOPExtension.SUBSYSTEM_NAME)).toModelNode());
        List<ModelNode> operations = checkResultAndGetContents(servicesA.executeOperation(describeOp)).asList();
        servicesA.shutdown();

        Assert.assertEquals(1, operations.size());

        // install the describe options from the first controller into a second controller.
        KernelServices servicesB = createKernelServicesBuilder(additionalInit).setBootOperations(operations).build();
        ModelNode modelB = servicesB.readWholeModel();
        servicesB.shutdown();

        // make sure the models from the two controllers are identical.
        super.compare(modelA, modelB);

    }

    @Test
    public void testSubsystemWithSecurityIdentity() throws Exception {
        super.standardSubsystemTest("subsystem-1.0-security-identity.xml");
    }

    @Test
    public void testSubsystemWithSecurityClient() throws Exception {
        super.standardSubsystemTest("subsystem-1.0-security-client.xml");
    }

}
