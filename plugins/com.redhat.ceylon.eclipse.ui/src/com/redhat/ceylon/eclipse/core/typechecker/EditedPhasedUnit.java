package com.redhat.ceylon.eclipse.core.typechecker;

import java.lang.ref.WeakReference;
import java.util.List;

import org.antlr.runtime.CommonToken;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;

import com.redhat.ceylon.compiler.typechecker.TypeChecker;
import com.redhat.ceylon.compiler.typechecker.analyzer.ModuleSourceMapper;
import com.redhat.ceylon.compiler.typechecker.context.PhasedUnit;
import com.redhat.ceylon.compiler.typechecker.context.TypecheckerUnit;
import com.redhat.ceylon.compiler.typechecker.io.VirtualFile;
import com.redhat.ceylon.compiler.typechecker.tree.Tree.CompilationUnit;
import com.redhat.ceylon.eclipse.core.model.EditedSourceFile;
import com.redhat.ceylon.model.typechecker.model.Declaration;
import com.redhat.ceylon.model.typechecker.model.Package;
import com.redhat.ceylon.model.typechecker.util.ModuleManager;

public class EditedPhasedUnit extends ModifiablePhasedUnit {
    WeakReference<ProjectPhasedUnit> savedPhasedUnitRef;
    
    public EditedPhasedUnit(VirtualFile unitFile, VirtualFile srcDir,
            CompilationUnit cu, Package p, ModuleManager moduleManager,
            ModuleSourceMapper moduleSourceMapper,
            TypeChecker typeChecker, List<CommonToken> tokenStream, ProjectPhasedUnit savedPhasedUnit) {
        super(unitFile, srcDir, cu, p, moduleManager, moduleSourceMapper, typeChecker, tokenStream);
        savedPhasedUnitRef = new WeakReference<ProjectPhasedUnit>(savedPhasedUnit);
        if (savedPhasedUnit!=null) {
            savedPhasedUnit.addWorkingCopy(this);
        }
    }
    
    public EditedPhasedUnit(PhasedUnit other) {
        super(other);
    }

    @Override
    public TypecheckerUnit newUnit() {
        return new EditedSourceFile(this);
    }
    
    @Override
    public EditedSourceFile getUnit() {
        return (EditedSourceFile) super.getUnit();
    }

    public ProjectPhasedUnit getOriginalPhasedUnit() {
        return savedPhasedUnitRef.get();
    }
    
    @Override
    public IFile getFileResource() {
        return getOriginalPhasedUnit() == null ? null
                : getOriginalPhasedUnit().getFileResource();
    }
    

    @Override
    public IFolder getRootFolderResource() {
        return getOriginalPhasedUnit() == null ? null
                : getOriginalPhasedUnit().getRootFolderResource();
    }
    

    @Override
    public IProject getProjectResource() {
        return getOriginalPhasedUnit() == null ? null
                : getOriginalPhasedUnit().getProjectResource();
    }
    
    @Override
    public boolean isAllowedToChangeModel(Declaration declaration) {
        return !IdePhasedUnit.isCentralModelDeclaration(declaration);
    }
}
