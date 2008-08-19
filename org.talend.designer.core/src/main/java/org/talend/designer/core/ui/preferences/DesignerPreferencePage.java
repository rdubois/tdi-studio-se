// ============================================================================
//
// Copyright (C) 2006-2007 Talend Inc. - www.talend.com
//
// This source code is available under agreement available at
// %InstallDIR%\features\org.talend.rcp.branding.%PRODUCTNAME%\%PRODUCTNAME%license.txt
//
// You should have received a copy of the agreement
// along with this program; if not, write to Talend SA
// 9 rue Pages 92150 Suresnes, France
//
// ============================================================================
package org.talend.designer.core.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.DirectoryFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.RadioGroupFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.talend.core.model.components.ComponentUtilities;
import org.talend.core.prefs.ITalendCorePrefConstants;
import org.talend.designer.core.DesignerPlugin;
import org.talend.designer.core.i18n.Messages;

/**
 * This class represents a preference page that is contributed to the Preferences dialog. By subclassing
 * <samp>FieldEditorPreferencePage</samp>, we can use the field support built into JFace that allows us to create a page
 * that is small and knows how to save, restore and apply itself.
 * <p>
 * This page is used to modify preferences only. They are stored in the preference store that belongs to the main
 * plug-in class. That way, preferences can be accessed directly via the preference store.
 */

public class DesignerPreferencePage extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

    String oldLargeIconsSize;

    public DesignerPreferencePage() {
        super(GRID);
        setPreferenceStore(DesignerPlugin.getDefault().getPreferenceStore());
    }

    public void init(IWorkbench workbench) {
        oldLargeIconsSize = getPreferenceStore().getString(TalendDesignerPrefConstants.LARGE_ICONS_SIZE);
    }

    @Override
    protected void performApply() {
        super.performApply();

        checkPropertyChanged();
    }

    @Override
    public boolean performOk() {
        final boolean toReturn = super.performOk();
        checkPropertyChanged();
        return toReturn;
    }

    private void checkPropertyChanged() {
        if (!oldLargeIconsSize.equals(getPreferenceStore().getString(TalendDesignerPrefConstants.LARGE_ICONS_SIZE))) {
            ComponentUtilities.updatePalette();
        }
    }

    /**
     * Creates the field editors. Field editors are abstractions of the common GUI blocks needed to manipulate various
     * types of preferences. Each field editor knows how to save and restore itself.
     */
    @Override
    public void createFieldEditors() {
        StringFieldEditor labelField;
        StringFieldEditor hintField;
        StringFieldEditor connectionField;
        BooleanFieldEditor showHint;
        // BooleanFieldEditor displayComponent;
        BooleanFieldEditor useRepositoryName;
        BooleanFieldEditor deactiveRepositoryUpdate;

        labelField = new StringFieldEditor(TalendDesignerPrefConstants.DEFAULT_LABEL, Messages
                .getString("DesignerPreferencePage.component.defaultLabel"), //$NON-NLS-1$
                getFieldEditorParent());
        hintField = new StringFieldEditor(TalendDesignerPrefConstants.DEFAULT_HINT, Messages
                .getString("DesignerPreferencePage.component.defaultHint"), //$NON-NLS-1$
                getFieldEditorParent());
        connectionField = new StringFieldEditor(TalendDesignerPrefConstants.DEFAULT_CONNECTION_FORMAT,
                "Components default connection", getFieldEditorParent());
        showHint = new BooleanFieldEditor(TalendDesignerPrefConstants.DEFAULT_HINT_USED, Messages
                .getString("DesignerPreferencePage.hintShowed"), //$NON-NLS-1$
                getFieldEditorParent());
        // displayComponent = new BooleanFieldEditor(TalendDesignerPrefConstants.DEFAULT_DISPLAY, Messages
        // .getString("DesignerPreferencePage.display.hiddenComponents"), getFieldEditorParent()); //$NON-NLS-1$
        useRepositoryName = new BooleanFieldEditor(TalendDesignerPrefConstants.USE_REPOSITORY_NAME, Messages
                .getString("DesignerPreferencePage.display.useRepositoryName"), getFieldEditorParent()); //$NON-NLS-1$

        deactiveRepositoryUpdate = new BooleanFieldEditor(ITalendCorePrefConstants.DEACTIVE_REPOSITORY_UPDATE, Messages
                .getString("DesignerPreferencePage.display.deactiveRepositoryUpdate"), getFieldEditorParent()); //$NON-NLS-1$

        addField(labelField);
        addField(hintField);
        addField(connectionField);
        addField(showHint);
        // addField(displayComponent);
        addField(useRepositoryName);
        addField(deactiveRepositoryUpdate);

        addField(new BooleanFieldEditor(TalendDesignerPrefConstants.PROPERTY_CODE_CHECK, Messages
                .getString("DesignerPreferencePage.propertyCodeCheck"), getFieldEditorParent())); //$NON-NLS-1$

        BooleanFieldEditor antialiasing;
        antialiasing = new BooleanFieldEditor(TalendDesignerPrefConstants.EDITOR_ANTIALIASING,
                "Use antialiasing and interpolation in the Job Designer", getFieldEditorParent()); //$NON-NLS-1$
        addField(antialiasing);

        DirectoryFieldEditor compDefaultFileDir = new DirectoryFieldEditor(TalendDesignerPrefConstants.COMP_DEFAULT_FILE_DIR,
                Messages.getString("DesignerPreferencePage.defaultFilePathDirectory"), getFieldEditorParent()); //$NON-NLS-1$
        addField(compDefaultFileDir);

        RadioGroupFieldEditor largeIconsSizeField = new RadioGroupFieldEditor(TalendDesignerPrefConstants.LARGE_ICONS_SIZE,
                Messages.getString("DesignerPreferencePage.largeIconsSize"), 2, new String[][] { { "24 pixels", "" + 24 }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                        { "32 pixels", "" + 32 } }, getFieldEditorParent()); //$NON-NLS-1$ //$NON-NLS-2$
        addField(largeIconsSizeField);

        // disable this feature 1722 for the M1 release as it's not used yet in Perl or Java.

        // RadioGroupFieldEditor schemaOptions = new RadioGroupFieldEditor(
        // TalendDesignerPrefConstants.SCHEMA_OPTIONS,
        // Messages.getString("DesignerPreferencePage.schemaOptions"), 4, new String[][] { { "do nothing", "nothing" },
        // { "die", "die" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        // { "warn", "warn" }, { "enlarge or truncate row", "adapt" } }, getFieldEditorParent(), true); //$NON-NLS-1$
        // //$NON-NLS-2$
        // addField(schemaOptions);

        RadioGroupFieldEditor viewOptions = new RadioGroupFieldEditor(TalendDesignerPrefConstants.VIEW_OPTIONS, Messages
                .getString("DesignerPreferencePage.viewOptions"), 2, new String[][] { { "compact view", "default" }, //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                { "table view", "table view" } }, getFieldEditorParent(), true); //$NON-NLS-1$ //$NON-NLS-2$
        addField(viewOptions);

        BooleanFieldEditor viewSubjobs = new BooleanFieldEditor(TalendDesignerPrefConstants.DISPLAY_SUBJOBS,
                "Highlight subjobs in the job designs", getFieldEditorParent());
        addField(viewSubjobs);
        BooleanFieldEditor generateCodeWhenOpenJob = new BooleanFieldEditor(
                TalendDesignerPrefConstants.GENERATE_CODE_WHEN_OPEN_JOB, "Generate code when opening the job",
                getFieldEditorParent());
        addField(generateCodeWhenOpenJob);

        // When updating jobs or joblets, check only the last version, and checked by default
        BooleanFieldEditor checkOnlyLastVersion = new BooleanFieldEditor(TalendDesignerPrefConstants.CHECK_ONLY_LAST_VERSION,
                "Check only the last version when updating jobs or joblets", getFieldEditorParent());
        addField(checkOnlyLastVersion);

        BooleanFieldEditor propagateContextVariable = new BooleanFieldEditor(
                TalendDesignerPrefConstants.PROPAGATE_CONTEXT_VARIABLE,
                "Propagate add/delete variable changes in repository contexts", getFieldEditorParent());
        addField(propagateContextVariable);
    }
}
