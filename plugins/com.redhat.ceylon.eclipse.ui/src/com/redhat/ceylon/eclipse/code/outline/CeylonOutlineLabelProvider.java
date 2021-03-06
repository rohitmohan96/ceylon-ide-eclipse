package com.redhat.ceylon.eclipse.code.outline;

import static com.redhat.ceylon.eclipse.code.complete.CodeCompletions.getQualifiedDescriptionFor;
import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider.getDecoratedImage;
import static com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider.getImageForDeclaration;
import static com.redhat.ceylon.eclipse.code.preferences.CeylonPreferenceInitializer.PARAMS_IN_OUTLINES;
import static com.redhat.ceylon.eclipse.code.preferences.CeylonPreferenceInitializer.PARAM_TYPES_IN_OUTLINES;
import static com.redhat.ceylon.eclipse.code.preferences.CeylonPreferenceInitializer.RETURN_TYPES_IN_OUTLINES;
import static com.redhat.ceylon.eclipse.code.preferences.CeylonPreferenceInitializer.TYPE_PARAMS_IN_OUTLINES;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.DelegatingStyledCellLabelProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.StyledCellLabelProvider;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;

import com.redhat.ceylon.eclipse.ui.CeylonPlugin;
import com.redhat.ceylon.model.typechecker.model.Declaration;

class CeylonOutlineLabelProvider 
        extends StyledCellLabelProvider 
        implements DelegatingStyledCellLabelProvider.IStyledLabelProvider,
                   ILabelProvider {
    
    @Override
    public void removeListener(ILabelProviderListener listener) {}

    @Override
    public boolean isLabelProperty(Object element, String property) {
        return false;
    }

    @Override
    public void dispose() {}

    @Override
    public void addListener(ILabelProviderListener listener) {}

    Font getFont() {
        return null;
    }

    String getPrefix() {
        return null;
    }
    
    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof CeylonOutlineNode) {
            CeylonOutlineNode node = 
                    (CeylonOutlineNode) element;
            return node.getLabel(getPrefix(), getFont());
        }
        else if (element instanceof Declaration) {
            IPreferenceStore prefs = CeylonPlugin.getPreferences();
            return getQualifiedDescriptionFor((Declaration) element,
                    prefs.getBoolean(TYPE_PARAMS_IN_OUTLINES),
                    prefs.getBoolean(PARAMS_IN_OUTLINES),
                    prefs.getBoolean(PARAM_TYPES_IN_OUTLINES),
                    prefs.getBoolean(RETURN_TYPES_IN_OUTLINES),
                    getPrefix(), getFont());
        }
        else {
            return new StyledString();
        }
    }
    
    @Override
    public Image getImage(Object element) {
        if (element instanceof CeylonOutlineNode) {
            CeylonOutlineNode node = 
                    (CeylonOutlineNode) element;
            return getDecoratedImage(
                    node.getImageKey(),
                    node.getDecorations(),
                    false);
        }
        else if (element instanceof Declaration) {
            return getImageForDeclaration((Declaration) element);
        }
        else {
            return null;
        }
    }
    
    @Override
    public void update(ViewerCell cell) {
        Object element = cell.getElement();
        StyledString styledText = getStyledText(element);
        cell.setText(styledText.toString());
        cell.setStyleRanges(styledText.getStyleRanges());
        cell.setImage(getImage(element));
        super.update(cell);
    }

    @Override
    public String getText(Object element) {
        return getStyledText(element).toString();
    }
    
}