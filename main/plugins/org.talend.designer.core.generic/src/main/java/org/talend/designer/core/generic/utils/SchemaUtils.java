// ============================================================================
//
// Copyright (C) 2006-2016 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.generic.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.eclipse.emf.common.util.EList;
import org.talend.commons.runtime.model.components.IComponentConstants;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.core.model.metadata.IMetadataTable;
import org.talend.core.model.metadata.MetadataToolAvroHelper;
import org.talend.core.model.metadata.MetadataToolHelper;
import org.talend.core.model.metadata.builder.ConvertionHelper;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.metadata.builder.connection.ConnectionFactory;
import org.talend.core.model.metadata.builder.connection.MetadataColumn;
import org.talend.core.model.metadata.builder.connection.MetadataTable;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.repository.model.repositoryObject.MetadataTableRepositoryObject;
import org.talend.cwm.helper.PackageHelper;
import org.talend.daikon.properties.Properties.Deserialized;
import org.talend.repository.model.IProxyRepositoryFactory;
import orgomg.cwm.objectmodel.core.CoreFactory;
import orgomg.cwm.objectmodel.core.TaggedValue;

public class SchemaUtils {

    public static MetadataTable createSchema(String name, ComponentProperties properties, String schemaPropertyName) {
        if (name == null || properties == null || schemaPropertyName == null) {
            return null;
        }
        IProxyRepositoryFactory factory = ProxyRepositoryFactory.getInstance();
        MetadataTable metadataTable = ConnectionFactory.eINSTANCE.createMetadataTable();
        metadataTable.setId(factory.getNextId());
        metadataTable.setName(name);
        metadataTable.setLabel(name);
        metadataTable.setSourceName(name);
        Object schemaObj = ComponentsUtils.getGenericPropertyValue(properties, schemaPropertyName);
        if (schemaObj instanceof String) {
            Schema avroSchema = new Schema.Parser().parse((String) schemaObj);
            convertComponentSchemaIntoTalendSchema(avroSchema, metadataTable);
        }
        IMetadataTable iMetadataTable = MetadataToolHelper.convert(metadataTable);
        updateComponentSchema(properties, schemaPropertyName, iMetadataTable);
        metadataTable = ConvertionHelper.convert(iMetadataTable);
        properties.setValue(schemaPropertyName, convertTalendSchemaIntoComponentSchema(metadataTable));
        TaggedValue serializedPropsTV = CoreFactory.eINSTANCE.createTaggedValue();
        serializedPropsTV.setTag(IComponentConstants.COMPONENT_PROPERTIES_TAG);
        serializedPropsTV.setValue(properties.toSerialized());
        metadataTable.getTaggedValue().add(serializedPropsTV);
        TaggedValue schemaPropertyTV = CoreFactory.eINSTANCE.createTaggedValue();
        schemaPropertyTV.setTag(IComponentConstants.COMPONENT_SCHEMA_TAG);
        schemaPropertyTV.setValue(schemaPropertyName);
        metadataTable.getTaggedValue().add(schemaPropertyTV);
        return metadataTable;
    }

    private static void convertComponentSchemaIntoTalendSchema(Schema schema, MetadataTable metadataTable) {
        if (schema == null || metadataTable == null) {
            return;
        }
        for (Schema.Field field : schema.getFields()) {
            MetadataColumn metadataColumn = MetadataToolAvroHelper.convertFromAvro(field);
            metadataTable.getColumns().add(metadataColumn);
        }
    }

    public static Schema convertTalendSchemaIntoComponentSchema(MetadataTable metadataTable) {
        if (metadataTable == null) {
            return null;
        }
        return MetadataToolAvroHelper.convertToAvro(metadataTable);
    }

    /**
     * DOC ycbai Comment method "updateComponentSchema".
     * <p>
     * Recreate a component schema by <code>metadataTable<code> and save it back into the <code>metadataTable<code>.
     * 
     * @param metadataTable
     */
    public static void updateComponentSchema(MetadataTable metadataTable) {
        if (metadataTable == null) {
            return;
        }
        String componentPropertiesStr = null;
        String schemaPropertyName = null;
        TaggedValue componentPropertiesTaggedValue = null;
        EList<TaggedValue> taggedValues = metadataTable.getTaggedValue();
        for (TaggedValue taggedValue : taggedValues) {
            String tag = taggedValue.getTag();
            String tagValue = taggedValue.getValue();
            if (IComponentConstants.COMPONENT_PROPERTIES_TAG.equals(tag)) {
                componentPropertiesStr = tagValue;
                componentPropertiesTaggedValue = taggedValue;
            } else if (IComponentConstants.COMPONENT_SCHEMA_TAG.equals(tag)) {
                schemaPropertyName = tagValue;
            }
        }
        if (componentPropertiesStr != null && componentPropertiesTaggedValue != null && schemaPropertyName != null) {
            ComponentProperties componentProperties = ComponentsUtils
                    .getComponentPropertiesFromSerialized(componentPropertiesStr);
            componentProperties.setValue(schemaPropertyName, convertTalendSchemaIntoComponentSchema(metadataTable));
            componentPropertiesTaggedValue.setValue(componentProperties.toSerialized());
        }
    }

    public static void updateComponentSchema(ComponentProperties componentProperties, String schemaPropertyName,
            IMetadataTable metadataTable) {
        updateComponentSchema(componentProperties, schemaPropertyName, metadataTable, null);
    }

    public static void updateComponentSchema(ComponentProperties componentProperties, String schemaPropertyName,
            IMetadataTable metadataTable, IElementParameter param) {
        if (componentProperties == null || schemaPropertyName == null || metadataTable == null) {
            return;
        }
        Schema schema = convertTalendSchemaIntoComponentSchema(ConvertionHelper.convert(metadataTable));
        componentProperties.setValue(schemaPropertyName, schema);
        Map<String, String> additionalProperties = metadataTable.getAdditionalProperties();
        additionalProperties.put(IComponentConstants.COMPONENT_PROPERTIES_TAG, componentProperties.toSerialized());
        additionalProperties.put(IComponentConstants.COMPONENT_SCHEMA_TAG, schemaPropertyName);
        if (param != null) {
            param.setValue(schema.toString());
        }
    }

    public static ComponentProperties getCurrentComponentProperties(IMetadataTable table) {
        if (table != null) {
            String serializedProperties = null;
            if (table instanceof MetadataTableRepositoryObject) {
                MetadataTableRepositoryObject metaTableRepObj = (MetadataTableRepositoryObject) table;
                MetadataTable metadataTable = metaTableRepObj.getTable();
                if (metadataTable != null && metadataTable.getTaggedValue() != null) {
                    for (TaggedValue serializedProps : metadataTable.getTaggedValue()) {
                        if (IComponentConstants.COMPONENT_PROPERTIES_TAG.equals(serializedProps.getTag())) {
                            serializedProperties = serializedProps.getValue();
                            break;
                        }
                    }
                }
            } else if (table instanceof org.talend.core.model.metadata.MetadataTable) {
                org.talend.core.model.metadata.MetadataTable metaTable = (org.talend.core.model.metadata.MetadataTable) table;
                Map<String, String> additionalProperties = metaTable.getAdditionalProperties();
                serializedProperties = additionalProperties.get(IComponentConstants.COMPONENT_PROPERTIES_TAG);
            }
            if (serializedProperties != null) {
                Deserialized<ComponentProperties> fromSerializedProperties = ComponentProperties.fromSerialized(
                        serializedProperties, ComponentProperties.class);
                if (fromSerializedProperties != null) {
                    return fromSerializedProperties.properties;
                }
            }
        }
        return null;
    }

    public static List<MetadataTable> getMetadataTables(orgomg.cwm.objectmodel.core.Package parentPackage,
            Class containerTypeClass) {
        List<MetadataTable> metadataTables = new ArrayList<>();
        metadataTables.addAll(PackageHelper.getOwnedElements(parentPackage, MetadataTable.class));
        if (containerTypeClass != null) {
            List subContainers = PackageHelper.getOwnedElements(parentPackage, containerTypeClass);
            for (Object subContainer : subContainers) {
                if (subContainer instanceof orgomg.cwm.objectmodel.core.Package) {
                    orgomg.cwm.objectmodel.core.Package subContainerPackage = (orgomg.cwm.objectmodel.core.Package) subContainer;
                    metadataTables.addAll(getMetadataTables(subContainerPackage, containerTypeClass));
                }
            }
        }
        return metadataTables;
    }

    public static MetadataTable getMetadataTable(Connection connection, String tabLabel, Class containerTypeClass) {
        List<MetadataTable> tables = getMetadataTables(connection, containerTypeClass);
        for (MetadataTable table : tables) {
            if (tabLabel != null && tabLabel.equals(table.getLabel())) {
                return table;
            }
        }
        return null;
    }

}
