package com.redhat.ceylon.eclipse.code.search;

import static com.redhat.ceylon.eclipse.util.Highlights.ID_STYLER;
import static com.redhat.ceylon.eclipse.util.Highlights.MEMBER_STYLER;
import static com.redhat.ceylon.eclipse.util.Highlights.PACKAGE_STYLER;
import static com.redhat.ceylon.eclipse.util.Highlights.TYPE_ID_STYLER;
import static com.redhat.ceylon.eclipse.util.Highlights.TYPE_STYLER;
import static org.eclipse.jface.viewers.StyledString.COUNTER_STYLER;

import org.eclipse.core.resources.IFile;
import org.eclipse.jdt.core.Flags;
import org.eclipse.jdt.core.IField;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.Signature;
import org.eclipse.jface.viewers.StyledString;
import org.eclipse.swt.graphics.Image;

import com.redhat.ceylon.eclipse.code.outline.CeylonLabelProvider;
import com.redhat.ceylon.eclipse.util.Highlights;

public class SearchResultsLabelProvider extends CeylonLabelProvider {
    
    @Override
    public Image getImage(Object element) {
        if (element instanceof WithSourceFolder) {
            element = ((WithSourceFolder) element).element;
        }
        String key;
        int decorations;
        if (element instanceof ArchiveMatches) {
            key = RUNTIME_OBJ;
            decorations = 0;
        }
        else if (element instanceof CeylonElement) {
            key = ((CeylonElement) element).getImageKey(); 
            decorations = ((CeylonElement) element).getDecorations(); 
        }
        else if (element instanceof IType ||
                element instanceof IField ||
                element instanceof IMethod) {
            key = getImageKeyForDeclaration((IJavaElement) element); 
            decorations = 0; 
        }
        else {
            key = super.getImageKey(element);
            decorations = super.getDecorationAttributes(element);
        }
        return getDecoratedImage(key, decorations, false);
    }
    
    @Override
    public StyledString getStyledText(Object element) {
        if (element instanceof WithSourceFolder) {
            element = ((WithSourceFolder) element).element;
        }
        if (element instanceof ArchiveMatches) {
            return new StyledString("Source Archive Matches");
        }
        else if (element instanceof CeylonElement) {
            return getStyledLabelForSearchResult((CeylonElement) element);
        }
        else if (element instanceof IType ||
                element instanceof IField||
                element instanceof IMethod) {
            return getStyledLabelForSearchResult((IJavaElement) element);
        }
        else {
            return super.getStyledText(element);
        }
    }

    private StyledString getStyledLabelForSearchResult(CeylonElement ce) {
        StyledString styledString = new StyledString();
        IFile file = ce.getFile();
        String path = file==null ? 
                ce.getVirtualFile().getPath() : 
                    file.getFullPath().toString();
                styledString.append(ce.getLabel());
                //if (includePackage()) {
                styledString.append(" - " + ce.getPackageLabel(), PACKAGE_STYLER);
                //}
                styledString.append(" - " + path, COUNTER_STYLER)
                .append(":" + ce.getLocation(), COUNTER_STYLER);
                return styledString;
    }

    private StyledString getStyledLabelForSearchResult(IJavaElement je) {
        StyledString styledString = new StyledString();
        String name = je.getElementName();
        if (je instanceof IMethod) {
            try {
                String returnType = ((IMethod) je).getReturnType();
                if (returnType.equals("V")) {
                    styledString.append("void ", Highlights.KW_STYLER);
                }
                else {
                    styledString.append(Signature.toString(returnType), TYPE_STYLER)
                    .append(' ');
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            styledString.append(name, ID_STYLER);
            try {
                styledString.append('(');
                String[] parameterTypes = ((IMethod) je).getParameterTypes();
                String[] parameterNames = ((IMethod) je).getParameterNames();
                boolean first = true;
                for (int i=0; i<parameterTypes.length; i++) {
                    if (first) {
                        first = false;
                    }
                    else {
                        styledString.append(", ");
                    }
                    styledString.append(Signature.toString(parameterTypes[i]), TYPE_STYLER)
                                .append(' ').append(parameterNames[i], MEMBER_STYLER);
                }
                styledString.append(')');
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else if (je instanceof IField) {
            try {
                String type = Signature.toString(((IField) je).getTypeSignature());
                styledString.append(type, TYPE_STYLER);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
            styledString.append(name, ID_STYLER);
        }
        else {
            styledString.append(name, TYPE_ID_STYLER);
        }
        //if (includePackage()) {
        IJavaElement pkg = ((IJavaElement) je.getOpenable()).getParent();
        styledString.append(" - ", PACKAGE_STYLER)
                    .append(pkg.getElementName(), PACKAGE_STYLER);
        //}
        IFile file = (IFile) je.getResource();
        if (file!=null) {
            styledString.append(" - " + file.getFullPath().toString(), COUNTER_STYLER);
        }
        return styledString;
    }

    private static String getImageKeyForDeclaration(IJavaElement e) {
        if (e==null) return null;
        boolean shared = false;
        if (e instanceof IMember) {
            try {
                shared = Flags.isPublic(((IMember) e).getFlags());
            }
            catch (JavaModelException jme) {
                jme.printStackTrace();
            }
        }
        switch(e.getElementType()) {
        case IJavaElement.METHOD:
            if (shared) {
                return CEYLON_METHOD;
            }
            else {
                return CEYLON_LOCAL_METHOD;
            }
        case IJavaElement.FIELD:
            if (shared) {
                return CEYLON_ATTRIBUTE;
            }
            else {
                return CEYLON_LOCAL_ATTRIBUTE;
            }
        case IJavaElement.TYPE:
            if (shared) {
                return CEYLON_CLASS;
            }
            else {
                return CEYLON_LOCAL_CLASS;
            }
        default:
            return null;
        }
    }

}