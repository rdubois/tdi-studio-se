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
package org.talend.repository.generic.model.migration;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Properties;

import org.talend.components.api.properties.ComponentProperties;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.migration.AbstractItemMigrationTask;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Item;
import org.talend.core.model.properties.PropertiesFactory;
import org.talend.core.model.properties.Property;
import org.talend.core.model.repository.ERepositoryObjectType;
import org.talend.core.repository.model.ProxyRepositoryFactory;
import org.talend.core.utils.ReflectionUtils;
import org.talend.daikon.NamedThing;
import org.talend.repository.generic.model.genericMetadata.GenericConnection;
import org.talend.repository.generic.model.genericMetadata.GenericConnectionItem;
import org.talend.repository.generic.model.genericMetadata.GenericMetadataFactory;

/**
 * created by hcyi on Apr 11, 2016 Detailled comment
 *
 */
public abstract class NewGenericWizardMigrationTask extends AbstractItemMigrationTask {

    @Override
    public List<ERepositoryObjectType> getTypes() {
        List<ERepositoryObjectType> toReturn = new ArrayList<>();
        return toReturn;
    }

    @Override
    public ExecutionResult execute(Item item) {
        return ExecutionResult.NOTHING_TO_DO;
    }

    protected Properties getPropertiesFromFile() {
        // with default implementation
        return new Properties();
    }

    protected boolean updateComponentProperties(Connection oldConnection, ComponentProperties componentProperties,
            Properties props) {
        boolean modified = false;
        if (props != null) {
            for (Object element : props.keySet()) {
                boolean changed = false;
                String propsKey = (String) element;
                String propsValue = props.getProperty(propsKey);
                if (propsValue != null && !"".equals(propsValue.trim())) { //$NON-NLS-1$
                    try {
                        String methodName = propsValue.substring(0, 1).toUpperCase() + propsValue.substring(1);
                        Object value = ReflectionUtils.invokeMethod(oldConnection, "get" + methodName, new Object[0]); //$NON-NLS-1$
                        if (value != null) {
                            NamedThing namedThing = componentProperties.getProperty(propsKey);
                            if (namedThing != null && namedThing instanceof org.talend.daikon.properties.Property) {
                                org.talend.daikon.properties.Property property = (org.talend.daikon.properties.Property) namedThing;
                                if (org.talend.daikon.properties.Property.Type.ENUM.equals(property.getType())) {
                                    Object obj = props.get(propsKey + "." + value);//$NON-NLS-1$
                                    if (obj != null) {
                                        componentProperties.setValue(propsKey, obj);
                                        changed = true;
                                    }
                                }
                            }
                            if (!changed) {
                                componentProperties.setValue(propsKey, value);
                                modified = true;
                            }
                        }
                    } catch (Exception e) {
                        // Do nothing
                    }
                }
            }
        }
        return modified;
    }

    protected boolean updateMetadataTable(Connection oldConnection, Connection genericConnection,
            ComponentProperties componentProperties) {
        return false;
    }

    protected GenericConnectionItem initGenericConnectionItem(ConnectionItem oldConnectionItem) {
        if (oldConnectionItem == null) {
            return null;
        }
        GenericConnectionItem genericConnectionItem = GenericMetadataFactory.eINSTANCE.createGenericConnectionItem();
        genericConnectionItem.setTypeName("");////$NON-NLS-1$
        genericConnectionItem.setState(oldConnectionItem.getState());
        genericConnectionItem.setFileExtension(oldConnectionItem.getFileExtension());
        genericConnectionItem.setNeedVersion(oldConnectionItem.isNeedVersion());
        return genericConnectionItem;
    }

    protected GenericConnection initGenericConnection(Connection oldConnection) {
        if (oldConnection == null) {
            return null;
        }
        GenericConnection genericConnection = GenericMetadataFactory.eINSTANCE.createGenericConnection();
        genericConnection.setName(oldConnection.getName());
        genericConnection.setLabel(oldConnection.getLabel() != null ? oldConnection.getLabel() : oldConnection.getName());
        genericConnection.setContextId(oldConnection.getContextId());
        genericConnection.setContextMode(oldConnection.isContextMode());
        return genericConnection;
    }

    protected Property initProperty(ConnectionItem oldConnectionItem, ConnectionItem newConnectionItem) {
        if (oldConnectionItem == null || newConnectionItem == null) {
            return null;
        }
        Property oldProperty = oldConnectionItem.getProperty();
        Property newProperty = PropertiesFactory.eINSTANCE.createProperty();
        newProperty.setId(ProxyRepositoryFactory.getInstance().getNextId());
        newProperty.setLabel(oldProperty.getLabel());
        newProperty.setDisplayName(oldProperty.getDisplayName());
        newProperty.setDescription(oldProperty.getDescription());
        newProperty.setAuthor(oldProperty.getAuthor());
        newProperty.setCreationDate(oldProperty.getCreationDate());
        newProperty.setMaxInformationLevel(oldProperty.getMaxInformationLevel());
        newProperty.setModificationDate(oldProperty.getModificationDate());
        newProperty.setOldStatusCode(oldProperty.getOldStatusCode());
        newProperty.setPurpose(oldProperty.getPurpose());
        newProperty.setStatusCode(oldProperty.getStatusCode());
        newProperty.setVersion(oldProperty.getVersion());
        newProperty.setItem(newConnectionItem);
        newConnectionItem.setProperty(newProperty);
        return newProperty;
    }

    @Override
    public Date getOrder() {
        GregorianCalendar gc = new GregorianCalendar(2016, 4, 11, 12, 0, 0);
        return gc.getTime();
    }
}
