import ceylon.interop.java {
    CeylonList
}

import com.redhat.ceylon.compiler.typechecker.context {
    PhasedUnit
}
import com.redhat.ceylon.eclipse.code.complete {
    EclipseCompletionManager
}
import com.redhat.ceylon.eclipse.code.editor {
    CeylonEditor
}
import com.redhat.ceylon.eclipse.core.builder {
    CeylonBuilder
}
import com.redhat.ceylon.eclipse.core.model {
    ModifiableSourceFile
}
import com.redhat.ceylon.eclipse.core.typechecker {
    ModifiablePhasedUnit
}
import com.redhat.ceylon.eclipse.util {
    eclipseIndents
}
import com.redhat.ceylon.ide.common.completion {
    IdeCompletionManager
}
import com.redhat.ceylon.ide.common.correct {
    AbstractQuickFix,
    ImportProposals
}
import com.redhat.ceylon.ide.common.util {
    Indents
}
import com.redhat.ceylon.model.typechecker.model {
    Unit
}

import org.eclipse.core.resources {
    IFile,
    IProject
}
import org.eclipse.jface.text {
    IDocument,
    Region
}
import org.eclipse.jface.text.contentassist {
    ICompletionProposal
}
import org.eclipse.ltk.core.refactoring {
    TextChange,
    TextFileChange,
    DocumentChange
}
import org.eclipse.text.edits {
    InsertEdit,
    TextEdit
}

interface EclipseAbstractQuickFix
        satisfies AbstractQuickFix<IFile,IDocument,InsertEdit,TextEdit,TextChange,Region,IProject,EclipseQuickFixData,ICompletionProposal> {
    
    shared actual IdeCompletionManager<out Anything,out Anything,out ICompletionProposal,IDocument> completionManager 
            => EclipseCompletionManager(CeylonEditor());
    
    shared actual Integer getTextEditOffset(TextEdit change) => change.offset;
    
    shared actual List<PhasedUnit> getUnits(IProject p) => CeylonList(CeylonBuilder.getUnits(p));
    
    shared actual ImportProposals<out Anything,out Anything,IDocument,InsertEdit,TextEdit,TextChange> importProposals
            => eclipseImportProposals;
    
    shared actual Indents<IDocument> indents => eclipseIndents;
    
    shared actual Region newRegion(Integer start, Integer length) => Region(start, length);
    
    shared actual TextChange newTextChange(String desc, PhasedUnit|IFile|IDocument u) {
        if (is IDocument u) {
            return DocumentChange(desc, u);
        } else if (is PhasedUnit u){
            assert(is ModifiablePhasedUnit u);
            return TextFileChange(desc, u.resourceFile);
        } else {
            return TextFileChange(desc, u);
        }
    }
    
    shared actual PhasedUnit? getPhasedUnit(Unit? u, EclipseQuickFixData data) {
        if (is ModifiableSourceFile u) {
            return u.phasedUnit;
        }
        return null;
    }
}
