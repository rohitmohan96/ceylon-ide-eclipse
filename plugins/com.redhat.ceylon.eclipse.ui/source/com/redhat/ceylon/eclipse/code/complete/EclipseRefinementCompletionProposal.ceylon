import com.redhat.ceylon.eclipse.code.correct {
    EclipseDocumentChanges,
    eclipseImportProposals
}
import com.redhat.ceylon.eclipse.code.outline {
    CeylonLabelProvider
}
import com.redhat.ceylon.eclipse.code.parse {
    CeylonParseController
}
import com.redhat.ceylon.eclipse.ui {
    CeylonPlugin,
    CeylonResources
}
import com.redhat.ceylon.eclipse.util {
    Highlights
}
import com.redhat.ceylon.ide.common.completion {
    RefinementCompletionProposal,
    getProposedName,
    appendPositionalArgs
}
import com.redhat.ceylon.ide.common.correct {
    ImportProposals
}
import com.redhat.ceylon.model.typechecker.model {
    Declaration,
    Scope,
    Reference,
    Unit,
    ModelUtil,
    Functional
}

import java.lang {
    JCharacter=Character
}

import org.eclipse.core.resources {
    IProject,
    IFile
}
import org.eclipse.jface.text {
    IDocument,
    BadLocationException,
    IRegion,
    Region,
    ITextViewer,
    DocumentEvent
}
import org.eclipse.jface.text.contentassist {
    ICompletionProposal,
    IContextInformation
}
import org.eclipse.jface.text.link {
    LinkedModeModel
}
import org.eclipse.jface.viewers {
    StyledString
}
import org.eclipse.ltk.core.refactoring {
    TextChange
}
import org.eclipse.swt.graphics {
    Point,
    Image
}
import org.eclipse.text.edits {
    InsertEdit,
    TextEdit
}

class EclipseRefinementCompletionProposal(Integer _offset, String prefix, Reference pr, String desc, 
        String text, CeylonParseController cpc, Declaration declaration, Scope scope,
        Boolean fullType, Boolean explicitReturnType)
        extends RefinementCompletionProposal<CeylonParseController, IProject, ICompletionProposal, IFile,
                IDocument, InsertEdit, TextEdit, TextChange, Point, LinkedModeModel>
                (_offset, prefix, pr, desc, text, cpc, declaration, scope, fullType, explicitReturnType)
        satisfies EclipseDocumentChanges & EclipseCompletionProposal {

    shared actual variable String? currentPrefix = prefix;
    
    shared actual Image image => CeylonLabelProvider.getRefinementIcon(declaration);
    
    shared actual ImportProposals<IFile,ICompletionProposal,IDocument,InsertEdit,TextEdit,TextChange> importProposals
            => eclipseImportProposals;
    
    shared IRegion getCurrentSpecifierRegion(IDocument document, Integer offset) {
        Integer start = offset;
        variable Integer length = 0;
        variable Integer i = offset;
        while (i < document.length) {
            Character ch = document.getChar(i);
            if (JCharacter.isWhitespace(ch) || ch==';' || ch==',' || ch==')') {
                break;
            }
            
            length++;
            i++;
        }
        
        return Region(start, length);
    }
    
    class NestedCompletionProposal(Declaration dec, Integer offset, Unit unit) 
            satisfies IEclipseCompletionProposal2And6 {
                
        shared actual void apply(IDocument document) {
            try {
                value region = getCurrentSpecifierRegion(document, offset);
                document.replace(region.offset, region.length, getText(false));
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        
        shared actual Point? getSelection(IDocument document) => null;
        
        shared actual String? additionalProposalInfo => null;
        
        shared actual String displayString => getText(true);
        
        shared actual StyledString styledDisplayString {
            StyledString result = StyledString();
            Highlights.styleFragment(result, displayString, false, null, CeylonPlugin.completionFont);
            return result;
        }
        
        shared actual Image image => CeylonLabelProvider.getImageForDeclaration(dec);
        
        shared actual IContextInformation? contextInformation => null;
        
        String getText(Boolean description) {
            variable StringBuilder sb = StringBuilder();
            sb.append(getProposedName(null, dec, unit));
            if (is Functional dec) {
                appendPositionalArgs(dec, dec.reference, unit, sb, false, description, false);
            }
            
            return sb.string;
        }
        
        shared actual void apply(ITextViewer viewer, Character trigger, Integer stateMask, Integer offset) {
            apply(viewer.document);
        }
        
        shared actual void selected(ITextViewer viewer, Boolean smartToggle) {
        }
        
        shared actual void unselected(ITextViewer viewer) {
        }
        
        shared actual Boolean validate(IDocument document, Integer currentOffset, DocumentEvent? event) {
            if (!exists event) {
                return true;
            } else {
                try {
                    IRegion region = getCurrentSpecifierRegion(document, offset);
                    String content = document.get(region.offset, currentOffset - region.offset);
                    return isContentValid(content);
                } catch (BadLocationException e) {
                }
                
                return false;
            }
        }
        
        Boolean isContentValid(String content) {
            String filter = content.trimmed.lowercased;
            return ModelUtil.isNameMatching(content, dec) || getProposedName(null, dec, unit).lowercased.startsWith(filter);
        }
    }

    class NestedLiteralCompletionProposal(String text, Integer offset) 
            satisfies IEclipseCompletionProposal2And6 {
        
        shared actual Point? getSelection(IDocument document) => null;
        
        shared actual void apply(IDocument document) {
            try {
                IRegion region = getCurrentSpecifierRegion(document, offset);
                document.replace(region.offset, region.length, text);
            } catch (BadLocationException e) {
                e.printStackTrace();
            }
        }
        
        shared actual void apply(ITextViewer viewer, Character trigger, Integer stateMask, Integer offset) {
            apply(viewer.document);
        }
        
        shared actual String displayString => text;
        
        shared actual StyledString styledDisplayString {
            variable StyledString result = StyledString();
            Highlights.styleFragment(result, displayString, false, null, CeylonPlugin.completionFont);
            return result;
        }
        
        shared actual Image image {
            return CeylonLabelProvider.getDecoratedImage(CeylonResources.\iCEYLON_LITERAL, 0, false);
        }
        
        shared actual String? additionalProposalInfo => null;
        
        shared actual IContextInformation? contextInformation => null;
        
        shared actual void selected(ITextViewer viewer, Boolean smartToggle) {
        }
        
        shared actual void unselected(ITextViewer viewer) {
        }
        
        shared actual Boolean validate(IDocument document, Integer currentOffset, DocumentEvent? event) {
            if (!exists event) {
                return true;
            } else {
                try {
                    variable IRegion region = getCurrentSpecifierRegion(document, offset);
                    variable String content = document.get(region.offset, currentOffset - region.offset);
                    variable String filter = content.trimmed.lowercased;
                    if (text.lowercased.startsWith(filter)) {
                        return true;
                    }
                } catch (BadLocationException e) {
                }
                
                return false;
            }
        }
    }
    
    shared actual ICompletionProposal newNestedCompletionProposal(Declaration dec, Integer loc)
            => NestedCompletionProposal(dec, loc, cpc.lastCompilationUnit.unit);
    
    shared actual ICompletionProposal newNestedLiteralCompletionProposal(String val, Integer loc)
            => NestedLiteralCompletionProposal(val, loc);

    shared actual variable Boolean toggleOverwriteInternal = false;
    
    shared actual Boolean toggleOverwrite => toggleOverwriteInternal;
    
}