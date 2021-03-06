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
package org.talend.repository.generic.ui;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.talend.components.api.properties.ComponentProperties;
import org.talend.components.api.service.ComponentService;
import org.talend.core.model.metadata.builder.connection.Connection;
import org.talend.core.model.process.EComponentCategory;
import org.talend.core.model.process.EParameterFieldType;
import org.talend.core.model.process.Element;
import org.talend.core.model.process.IElementParameter;
import org.talend.core.model.process.INode;
import org.talend.core.model.properties.ConnectionItem;
import org.talend.core.model.properties.Property;
import org.talend.core.runtime.services.ComponentServiceWithValueEvaluator;
import org.talend.core.ui.check.Checker;
import org.talend.core.ui.check.IChecker;
import org.talend.daikon.properties.ValidationResult;
import org.talend.daikon.properties.ValidationResult.Result;
import org.talend.daikon.properties.presentation.Form;
import org.talend.designer.core.generic.constants.IContextEventProperties;
import org.talend.designer.core.generic.constants.IElementParameterEventProperties;
import org.talend.designer.core.generic.constants.IGenericConstants;
import org.talend.designer.core.generic.context.ComponentContextPropertyValueEvaluator;
import org.talend.designer.core.generic.model.GenericElementParameter;
import org.talend.designer.core.generic.utils.ComponentsUtils;
import org.talend.designer.core.model.FakeElement;
import org.talend.designer.core.model.components.EParameterName;
import org.talend.designer.core.model.components.ElementParameter;
import org.talend.designer.core.model.components.EmfComponent;
import org.talend.designer.core.ui.views.properties.MultipleThreadDynamicComposite;
import org.talend.metadata.managment.ui.wizard.context.MetadataContextPropertyValueEvaluator;
import org.talend.repository.generic.i18n.Messages;
import org.talend.repository.generic.internal.IGenericWizardInternalService;
import org.talend.repository.generic.internal.service.GenericWizardInternalService;
import org.talend.repository.generic.model.genericMetadata.GenericConnection;

/**
 * 
 * created by ycbai on 2015年9月24日 Detailled comment
 *
 */
public class DynamicComposite extends MultipleThreadDynamicComposite implements PropertyChangeListener {

    private Element element;

    private Form form;

    private IChecker checker;

    private ConnectionItem connectionItem;

    private IGenericWizardInternalService internalService;

    private boolean drivedByForm;

    public DynamicComposite(Composite parentComposite, int styles, EComponentCategory section, Element element,
            boolean isCompactView, Color backgroundColor, Form form) {
        this(parentComposite, styles, section, element, isCompactView, backgroundColor, form, false);
    }

    public DynamicComposite(Composite parentComposite, int styles, EComponentCategory section, Element element,
            boolean isCompactView, Color backgroundColor, Form form, boolean drivedByForm) {
        super(parentComposite, styles, section, element, isCompactView, backgroundColor);
        this.element = element;
        this.form = form;
        this.drivedByForm = drivedByForm;
        checker = new Checker();
        internalService = new GenericWizardInternalService();
        if (drivedByForm) {
            internalService.getComponentService().makeFormCancelable((ComponentProperties) form.getProperties(), form.getName());
        }
        resetParameters();
    }

    private void resetComponentProperties() {
        if (connectionItem != null) {
            Connection connection = connectionItem.getConnection();
            if (connection instanceof GenericConnection) {
                GenericConnection genericConnection = (GenericConnection) connection;
                genericConnection.setCompProperties(form.getProperties().toSerialized());
            }
        }
    }

    public List<ElementParameter> resetParameters() {
        final List<ElementParameter> newParameters = new ArrayList<>();
        List<ElementParameter> currentParameters = (List<ElementParameter>) element.getElementParameters();
        List<ElementParameter> parameters = new ArrayList<>();
        ComponentService componentService = null;
        ComponentProperties componentProperties = null;
        if (element instanceof FakeElement) {
            Connection theConnection = null;
            if (connectionItem != null) {
                theConnection = connectionItem.getConnection();
            }
            componentService = new ComponentServiceWithValueEvaluator(internalService.getComponentService(),
                    new MetadataContextPropertyValueEvaluator(theConnection));
            parameters = ComponentsUtils.getParametersFromForm(element, form);
            parameters.add(getUpdateParameter());
            currentParameters.clear();
        } else {
            componentService = new ComponentServiceWithValueEvaluator(internalService.getComponentService(),
                    new ComponentContextPropertyValueEvaluator((INode) element));
            parameters = ComponentsUtils
                    .getParametersFromForm(element, section, ((INode) element).getComponentProperties(), form);
            componentProperties = ((INode) element).getComponentProperties();
        }
        for (ElementParameter parameter : parameters) {
            if (parameter instanceof GenericElementParameter) {
                GenericElementParameter genericElementParameter = (GenericElementParameter) parameter;
                genericElementParameter.setComponentService(componentService);
                genericElementParameter.setDrivedByForm(drivedByForm);
                genericElementParameter.callBeforePresent();
                genericElementParameter.addPropertyChangeListener(this);
                if (EParameterFieldType.SCHEMA_REFERENCE.equals(parameter.getFieldType())) {
                    if (parameter.getChildParameters().size() == 0) {
                        IElementParameter schemaParameter = element.getElementParameterFromField(
                                EParameterFieldType.SCHEMA_REFERENCE, section);
                        parameter.getChildParameters().putAll(schemaParameter.getChildParameters());
                    }
                }
                if (componentProperties != null && isRepository(element)) {
                    String repositoryValue = parameter.getRepositoryValue();
                    if (repositoryValue == null) {
                        if (parameter.getValue() != null) {
                            parameter.setRepositoryValue(parameter.getName());
                            repositoryValue = parameter.getRepositoryValue();
                        }
                    }
                    if (parameter.isShow(currentParameters) && (repositoryValue != null)
                            && (!parameter.getName().equals(EParameterName.PROPERTY_TYPE.getName()))
                            && parameter.getCategory() == section) {
                        org.talend.daikon.properties.Property property = componentProperties.getValuedProperty(parameter.getName());
                        if (property.getTaggedValue(IGenericConstants.REPOSITORY_VALUE) != null) {
                            parameter.setRepositoryValueUsed(true);
                            parameter.setReadOnly(true);
                        }
                    }
                }
            }
        }
        //
        boolean added = false;
        for (ElementParameter currentParameter : currentParameters) {
            if (EParameterName.UPDATE_COMPONENTS.getName().equals(currentParameter.getName())) {
                currentParameter.setValue(true);
            }
            if (currentParameter.isSerialized() && currentParameter.getCategory().equals(section)) {
                if (!added) {
                    newParameters.addAll(parameters);
                    added = true;
                }
                continue;
            }
            newParameters.add(currentParameter);
        }
        if (element instanceof FakeElement) {
            newParameters.addAll(parameters);
        }
        element.setElementParameters(newParameters);
        return newParameters;
    }

    private boolean isRepository(Element element) {
        IElementParameter property = element.getElementParameterFromField(EParameterFieldType.PROPERTY_TYPE, section);
        if (property != null) {
            Map<String, IElementParameter> childParameters = property.getChildParameters();
            if (childParameters != null) {
                IElementParameter elementParameter = childParameters.get(EParameterName.PROPERTY_TYPE.getName());
                return elementParameter != null && EmfComponent.REPOSITORY.equals(elementParameter.getValue());
            }
        }
        return false;
    }

    private void updateProperty(String newPropertyName) {
        if (connectionItem == null) {
            return;
        }
        Connection connection = connectionItem.getConnection();
        Property connectionProperty = connectionItem.getProperty();
        String propertyName = StringUtils.trimToNull(newPropertyName);
        connectionProperty.setDisplayName(propertyName);
        connectionProperty.setLabel(propertyName);
        connectionProperty.setModificationDate(new Date());
        connectionProperty.setLabel(propertyName);
        connection.setName(propertyName);
        connection.setLabel(propertyName);
    }

    private ElementParameter getUpdateParameter() {
        ElementParameter param = new ElementParameter(element);
        param.setName(EParameterName.UPDATE_COMPONENTS.getName());
        param.setValue(true);
        param.setDisplayName(EParameterName.UPDATE_COMPONENTS.getDisplayName());
        param.setFieldType(EParameterFieldType.CHECK);
        param.setCategory(EComponentCategory.TECHNICAL);
        param.setNumRow(1000);
        param.setReadOnly(true);
        param.setRequired(false);
        param.setShow(false);
        return param;
    }

    private void updateValidationStatus(ValidationResult validationResult) {
        String validationMessage = validationResult.getMessage();
        Result validationStatus = validationResult.getStatus();
        switch (validationStatus) {
        case WARNING:
            checker.updateStatus(IStatus.WARNING, null);
            MessageDialog.openWarning(getShell(), Messages.getString("DynamicComposite.connectionTest.title"), validationMessage); //$NON-NLS-1$
            break;
        case ERROR:
            checker.updateStatus(IStatus.ERROR, null);
            MessageDialog.openError(getShell(), Messages.getString("DynamicComposite.connectionTest.title"), validationMessage); //$NON-NLS-1$
            break;
        default:
            checker.updateStatus(IStatus.OK, null);
            MessageDialog.openInformation(getShell(), Messages.getString("DynamicComposite.connectionTest.title"), //$NON-NLS-1$
                    Messages.getString("DynamicComposite.connectionTest.msg.success")); //$NON-NLS-1$
            break;
        }
    }

    @Override
    public void propertyChange(PropertyChangeEvent event) {
        String propertyName = event.getPropertyName();
        if (IElementParameterEventProperties.EVENT_PROPERTY_VALUE_CHANGED.equals(propertyName)) {
            reset(true);
        } else if (IElementParameterEventProperties.EVENT_PROPERTY_NAME_CHANGED.equals(propertyName)) {
            String newPropertyName = String.valueOf(event.getNewValue());
            updateProperty(newPropertyName);
        } else if (IElementParameterEventProperties.EVENT_VALIDATE_RESULT_UPDATE.equals(propertyName)) {
            Object newValue = event.getNewValue();
            if (newValue instanceof ValidationResult) {
                updateValidationStatus((ValidationResult) newValue);
            }
        } else if (IElementParameterEventProperties.EVENT_SHOW_DIALOG.equals(propertyName)) {
            Object newValue = event.getNewValue();
            if (newValue instanceof Form) {
                GenericDialog genericDialog = new GenericDialog(getShell(), (Form) newValue);
                genericDialog.setConnectionItem(connectionItem);
                genericDialog.open();
            }
        } else if (IContextEventProperties.EVENT_PROPERTY_EXPORT_CONTEXT.equals(propertyName)) {
            resetComponentProperties();
        } else if (IContextEventProperties.EVENT_PROPERTY_REFRESH_UI.equals(propertyName)) {
            Object newValue = event.getNewValue();
            if (newValue instanceof ComponentProperties) {
                ComponentProperties newComponentProperties = (ComponentProperties) newValue;
                form.getProperties().copyValuesFrom(newComponentProperties);
                reset(true);
            }
        }
    }

    public IChecker getChecker() {
        return this.checker;
    }

    private void reset(boolean refresh) {
        resetParameters();
        if (refresh) {
            Display.getDefault().asyncExec(new Runnable() {

                @Override
                public void run() {
                    refresh();
                }
            });
        }
    }

    @Override
    public synchronized void dispose() {
        List<? extends IElementParameter> elementParameters = element.getElementParameters();
        for (IElementParameter elementParameter : elementParameters) {
            if (elementParameter instanceof GenericElementParameter) {
                ((GenericElementParameter) elementParameter).removePropertyChangeListener(this);
            }
        }
        super.dispose();
    }

    public ConnectionItem getConnectionItem() {
        return this.connectionItem;
    }

    public void setConnectionItem(ConnectionItem connectionItem) {
        this.connectionItem = connectionItem;
    }
}
