package com.redhat.ceylon.eclipse.code.correct;

import static com.redhat.ceylon.eclipse.util.EditorUtil.getDocument;
import static com.redhat.ceylon.eclipse.util.Indents.indents;

import java.util.Collection;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.model.typechecker.model.Constructor;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.FunctionOrValue;
import com.redhat.ceylon.model.typechecker.model.Type;
import com.redhat.ceylon.model.typechecker.model.TypeDeclaration;
import com.redhat.ceylon.model.typechecker.model.TypedDeclaration;

class AssignToFieldProposal {

    static void addAssignToFieldProposal(IFile file, 
                Tree.Statement statement, 
                Tree.Declaration declaration, 
                Collection<ICompletionProposal> proposals) {
        if (declaration instanceof Tree.TypedDeclaration && 
                statement instanceof Tree.Constructor) {
            Tree.Constructor constructor = 
                    (Tree.Constructor) statement;
            Tree.TypedDeclaration param = 
                    (Tree.TypedDeclaration) declaration;
            TypedDeclaration model = 
                    param.getDeclarationModel();
            String name = model.getName();
            Constructor cmodel = 
                    constructor.getConstructor();
            if (!model.getContainer().equals(cmodel)) {
                return;
            }
            TypeDeclaration clazz = 
                    cmodel.getExtendedType()
                        .getDeclaration();
            Declaration existing =
                    clazz.getMember(name, null, false);
            if (existing==null) {
                //ok, continue
            }
            else if (existing instanceof FunctionOrValue) {
                FunctionOrValue fov = 
                        (FunctionOrValue) existing;
                Type type = 
                        fov.getTypedReference()
                            .getFullType();
                Type paramType = 
                        model.getTypedReference()
                            .getFullType();
                if (type==null || paramType==null ||
                        !paramType.isSubtypeOf(type)) {
                    return;
                }
            }
            else {
                return;
            }
            
            TextFileChange change = 
                    new TextFileChange("Assign to Field", 
                            file);
            change.setEdit(new MultiTextEdit());
            IDocument document = getDocument(change);
            String indent = 
                    indents().getDefaultLineDelimiter(document) +
                    indents().getIndent(constructor, document);
            
            String desc;
            if (existing==null) {
                int start = declaration.getStartIndex();
                int end;
                Tree.SpecifierOrInitializerExpression sie;
                if (declaration 
                            instanceof Tree.AttributeDeclaration) {
                    Tree.AttributeDeclaration ad = 
                            (Tree.AttributeDeclaration) 
                                declaration;
                    sie = ad.getSpecifierOrInitializerExpression();
                }
                else if (declaration 
                            instanceof Tree.MethodDeclaration) {
                    Tree.MethodDeclaration ad = 
                            (Tree.MethodDeclaration) 
                                declaration;
                    sie = ad.getSpecifierExpression();
                }
                else {
                    sie = null;
                }
                end = sie==null ? 
                        declaration.getEndIndex() : 
                        sie.getStartIndex();
                String def;
                try {
                    def = document.get(start, end-start)
                            .trim();
                }
                catch (BadLocationException e) {
                    return;
                }
                def += ";" + indent;
                int loc = statement.getStartIndex();
                change.addEdit(new InsertEdit(loc, def));
                desc = 
                        "Assign parameter '" + name + 
                        "' to new field of '" + 
                        clazz.getName() + "'";
            }
            else {
                desc = "Assign parameter '" + name + 
                        "' to field '" + name + "' of '" + 
                        clazz.getName() + "'";
            }
            
            int offset = 
                    constructor.getBlock()
                        .getStartIndex() + 1;
            String text = 
                    indent +
                    indents().getDefaultIndent() +
                    "this." + name + 
                    " = " + name + ";";
            change.addEdit(new InsertEdit(offset, text));
            proposals.add(new CorrectionProposal(
                    desc, change, null));
        }
    }

}
