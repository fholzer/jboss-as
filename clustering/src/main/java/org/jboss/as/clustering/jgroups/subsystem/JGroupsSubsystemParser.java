/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2011, Red Hat, Inc., and individual contributors
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
package org.jboss.as.clustering.jgroups.subsystem;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;

import org.jboss.as.controller.descriptions.ModelDescriptionConstants;
import org.jboss.as.controller.operations.common.Util;
import org.jboss.as.controller.parsing.ParseUtils;
import org.jboss.as.controller.persistence.SubsystemMarshallingContext;
import org.jboss.dmr.ModelNode;
import org.jboss.dmr.Property;
import org.jboss.staxmapper.XMLElementReader;
import org.jboss.staxmapper.XMLElementWriter;
import org.jboss.staxmapper.XMLExtendedStreamReader;
import org.jboss.staxmapper.XMLExtendedStreamWriter;
import org.jgroups.stack.Protocol;

/**
 * Parser for the jgroups subsystem.
 * @author Paul Ferraro
 */
public class JGroupsSubsystemParser implements XMLElementReader<List<ModelNode>>, XMLElementWriter<SubsystemMarshallingContext> {

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementReader#readElement(org.jboss.staxmapper.XMLExtendedStreamReader, java.lang.Object)
     */
    @Override
    public void readElement(XMLExtendedStreamReader reader, List<ModelNode> operations) throws XMLStreamException {

        ModelNode address = new ModelNode();
        address.add(ModelDescriptionConstants.SUBSYSTEM, JGroupsExtension.SUBSYSTEM_NAME);
        address.protect();

        ModelNode subsystem = Util.getEmptyOperation(ModelDescriptionConstants.ADD, address);

        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case DEFAULT_STACK: {
                    subsystem.get(ModelKeys.DEFAULT_STACK).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        operations.add(subsystem);

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            switch (Namespace.forUri(reader.getNamespaceURI())) {
                case JGROUPS_1_0: {
                    Element element = Element.forName(reader.getLocalName());
                    switch (element) {
                        case STACK: {
                            operations.add(this.parseStack(reader, address));
                            break;
                        }
                        default: {
                            throw ParseUtils.unexpectedElement(reader);
                        }
                    }
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }

        }
    }

    private ModelNode parseStack(XMLExtendedStreamReader reader, ModelNode address) throws XMLStreamException {

        String name = null;
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            ParseUtils.requireNoNamespaceAttribute(reader, i);
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case NAME: {
                    name = value;
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (name == null) {
            throw ParseUtils.missingRequired(reader, EnumSet.of(Attribute.NAME));
        }

        final ModelNode stack = Util.getEmptyOperation(ModelDescriptionConstants.ADD, null);
        stack.get(ModelDescriptionConstants.OP_ADDR).set(address).add(ModelKeys.STACK, name);

        if (!reader.hasNext() || (reader.nextTag() == XMLStreamConstants.END_ELEMENT) || Element.forName(reader.getLocalName()) != Element.TRANSPORT) {
            throw ParseUtils.missingRequiredElement(reader, Collections.singleton(Element.TRANSPORT));
        }

        this.parseProtocol(reader, stack.get(ModelKeys.TRANSPORT));

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            Element element = Element.forName(reader.getLocalName());
            switch (element) {
                case PROTOCOL: {
                    this.parseProtocol(reader, stack.get(ModelKeys.PROTOCOL).add());
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedElement(reader);
                }
            }
        }

        return stack;
    }

    private void parseProtocol(XMLExtendedStreamReader reader, ModelNode protocol) throws XMLStreamException {
        for (int i = 0; i < reader.getAttributeCount(); i++) {
            String value = reader.getAttributeValue(i);
            Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
            switch (attribute) {
                case TYPE: {
                    try {
                        Class.forName("org.jgroups.protocols." + value).asSubclass(Protocol.class).newInstance();
                        protocol.get(ModelKeys.TYPE).set(value);
                    } catch (Exception e) {
                        throw ParseUtils.invalidAttributeValue(reader, i);
                    }
                    break;
                }
                case SOCKET_BINDING: {
                    protocol.get(ModelKeys.SOCKET_BINDING).set(value);
                    break;
                }
                case DIAGNOSTICS_SOCKET_BINDING: {
                    protocol.get(ModelKeys.DIAGNOSTICS_SOCKET_BINDING).set(value);
                    break;
                }
                case THREAD_POOL: {
                    protocol.get(ModelKeys.THREAD_POOL).set(value);
                    break;
                }
                case OOB_THREAD_POOL: {
                    protocol.get(ModelKeys.OOB_THREAD_POOL).set(value);
                    break;
                }
                case TIMER_THREAD_POOL: {
                    protocol.get(ModelKeys.TIMER_THREAD_POOL).set(value);
                    break;
                }
                case THREAD_FACTORY: {
                    protocol.get(ModelKeys.THREAD_FACTORY).set(value);
                    break;
                }
                default: {
                    throw ParseUtils.unexpectedAttribute(reader, i);
                }
            }
        }

        if (!protocol.hasDefined(ModelKeys.TYPE)) {
            throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.TYPE));
        }

        while (reader.hasNext() && (reader.nextTag() != XMLStreamConstants.END_ELEMENT)) {
            if (Element.forName(reader.getLocalName()) != Element.PROPERTY) {
                throw ParseUtils.unexpectedElement(reader);
            }
            int attributes = reader.getAttributeCount();
            String property = null;
            for (int i = 0; i < attributes; i++) {
                String value = reader.getAttributeValue(i);
                Attribute attribute = Attribute.forName(reader.getAttributeLocalName(i));
                switch (attribute) {
                    case NAME: {
                        property = value;
                        break;
                    }
                    default: {
                        throw ParseUtils.unexpectedAttribute(reader, i);
                    }
                }
            }
            if (property == null) {
                throw ParseUtils.missingRequired(reader, Collections.singleton(Attribute.NAME));
            }
            String value = reader.getElementText();
            protocol.get(ModelKeys.PROPERTY).add(property, value);
        }
    }

    /**
     * {@inheritDoc}
     * @see org.jboss.staxmapper.XMLElementWriter#writeContent(org.jboss.staxmapper.XMLExtendedStreamWriter, java.lang.Object)
     */
    @Override
    public void writeContent(XMLExtendedStreamWriter writer, SubsystemMarshallingContext context) throws XMLStreamException {
        context.startSubsystemElement(Namespace.CURRENT.getUri(), false);
        ModelNode model = context.getModelNode();
        if (model.isDefined()) {
            if (model.hasDefined(ModelKeys.DEFAULT_STACK)) {
                writer.writeAttribute(Attribute.DEFAULT_STACK.getLocalName(), model.get(ModelKeys.DEFAULT_STACK).asString());
            }
            for (Property property: model.get(ModelKeys.STACK).asPropertyList()) {
                writer.writeStartElement(Element.STACK.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                ModelNode stack = property.getValue();
                this.writeProtocol(writer, stack.get(ModelKeys.TRANSPORT), Element.TRANSPORT);
                for (ModelNode protocol: stack.get(ModelKeys.PROTOCOL).asList()) {
                    this.writeProtocol(writer, protocol, Element.PROTOCOL);
                }
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }

    private void writeProtocol(XMLExtendedStreamWriter writer, ModelNode protocol, Element element) throws XMLStreamException {
        writer.writeStartElement(element.getLocalName());
        writer.writeAttribute(Attribute.TYPE.getLocalName(), protocol.get(ModelKeys.TYPE).asString());
        if (protocol.has(ModelKeys.SOCKET_BINDING)) {
            writer.writeAttribute(Attribute.SOCKET_BINDING.getLocalName(), protocol.get(ModelKeys.SOCKET_BINDING).asString());
        }
        if (protocol.has(ModelKeys.DIAGNOSTICS_SOCKET_BINDING)) {
            writer.writeAttribute(Attribute.DIAGNOSTICS_SOCKET_BINDING.getLocalName(), protocol.get(ModelKeys.DIAGNOSTICS_SOCKET_BINDING).asString());
        }
        if (protocol.has(ModelKeys.THREAD_POOL)) {
            writer.writeAttribute(Attribute.THREAD_POOL.getLocalName(), protocol.get(ModelKeys.THREAD_POOL).asString());
        }
        if (protocol.has(ModelKeys.OOB_THREAD_POOL)) {
            writer.writeAttribute(Attribute.OOB_THREAD_POOL.getLocalName(), protocol.get(ModelKeys.OOB_THREAD_POOL).asString());
        }
        if (protocol.has(ModelKeys.TIMER_THREAD_POOL)) {
            writer.writeAttribute(Attribute.TIMER_THREAD_POOL.getLocalName(), protocol.get(ModelKeys.TIMER_THREAD_POOL).asString());
        }
        if (protocol.has(ModelKeys.THREAD_FACTORY)) {
            writer.writeAttribute(Attribute.THREAD_FACTORY.getLocalName(), protocol.get(ModelKeys.THREAD_FACTORY).asString());
        }
        if (protocol.has(ModelKeys.PROPERTY)) {
            for (Property property: protocol.get(ModelKeys.PROPERTY).asPropertyList()) {
                writer.writeStartElement(Element.PROPERTY.getLocalName());
                writer.writeAttribute(Attribute.NAME.getLocalName(), property.getName());
                writer.writeCharacters(property.getValue().asString());
                writer.writeEndElement();
            }
        }
        writer.writeEndElement();
    }
}
