package com.redhat.ceylon.eclipse.code.imports;

import static com.redhat.ceylon.eclipse.code.editor.Navigation.gotoLocation;
import static com.redhat.ceylon.eclipse.util.EditorUtil.getDocument;
import static com.redhat.ceylon.eclipse.util.EditorUtil.performChange;
import static com.redhat.ceylon.eclipse.util.Indents.indents;
import static com.redhat.ceylon.eclipse.util.Nodes.getImportedName;
import static java.lang.Character.isWhitespace;
import static java.util.Collections.singletonMap;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ltk.core.refactoring.TextFileChange;
import org.eclipse.text.edits.DeleteEdit;
import org.eclipse.text.edits.InsertEdit;
import org.eclipse.text.edits.MultiTextEdit;

import com.redhat.ceylon.common.Backend;
import com.redhat.ceylon.common.Backends;
import com.redhat.ceylon.compiler.typechecker.tree.Tree;
import com.redhat.ceylon.eclipse.core.model.ProjectSourceFile;
import com.redhat.ceylon.eclipse.core.typechecker.ProjectPhasedUnit;
import com.redhat.ceylon.ide.common.modulesearch.ModuleNode;
import com.redhat.ceylon.ide.common.modulesearch.ModuleVersionNode;
import com.redhat.ceylon.model.typechecker.model.Module;
import com.redhat.ceylon.model.typechecker.model.Unit;

@Deprecated
public class ModuleImportUtil {

    public static void exportModuleImports(IProject project, 
            Module target, String moduleName) {
        ProjectPhasedUnit unit = 
                getDescriptorPhasedUnit(project, target);
        exportModuleImports(unit.getResourceFile(), 
                unit.getCompilationUnit(), 
                moduleName);
    }

    public static void removeModuleImports(IProject project, 
            Module target, List<String> moduleNames) {
        if (moduleNames.isEmpty()) return;
        ProjectPhasedUnit unit = 
                getDescriptorPhasedUnit(project, target);
        removeModuleImports(unit.getResourceFile(), 
                unit.getCompilationUnit(), 
                moduleNames);
    }

    public static void exportModuleImports(IFile file,
            Tree.CompilationUnit cu, String moduleName) {
        TextFileChange textFileChange = 
                new TextFileChange("Export Module Imports",
                        file);
        textFileChange.setEdit(new MultiTextEdit());
        InsertEdit edit = createExportEdit(cu, moduleName);
        if (edit!=null) {
            textFileChange.addEdit(edit);
        }
        performChange(textFileChange);
    }
    
    public static void removeModuleImports(IFile file,
            Tree.CompilationUnit cu, List<String> moduleNames) {
        TextFileChange textFileChange = 
                new TextFileChange("Remove Module Imports",
                        file);
        textFileChange.setEdit(new MultiTextEdit());
        for (String moduleName: moduleNames) {
            DeleteEdit edit =
                    createRemoveEdit(cu, moduleName);
            if (edit!=null) {
                textFileChange.addEdit(edit);
            }
        }
        performChange(textFileChange);
    }
    
    public static void addModuleImport(
            IProject project, Module target,
            String moduleName, String moduleVersion) {
        ModuleVersionNode versionNode =
                new ModuleVersionNode(
                        new ModuleNode(moduleName,
                                Collections.<ModuleVersionNode>emptyList()),
                            moduleVersion);
        int offset =
                addModuleImports(project, target,
                        singletonMap(moduleName,
                                versionNode));
        ProjectPhasedUnit unit =
                getDescriptorPhasedUnit(project, target);
        gotoLocation(unit.getUnit(),
                offset + moduleName.length() + 
                        indents().getDefaultIndent().length() + 10,
                moduleVersion.length());
    }
    
    public static void makeModuleImportShared(
            IProject project, Module target,
            String[] moduleNames) {
        ProjectPhasedUnit unit = 
                getDescriptorPhasedUnit(project, target);
        TextFileChange textFileChange = 
                new TextFileChange("Make Module Import Shared", 
                        unit.getResourceFile());
        textFileChange.setEdit(new MultiTextEdit());
        Tree.CompilationUnit compilationUnit =
                unit.getCompilationUnit();
        IDocument doc = getDocument(textFileChange);
        for (String moduleName: moduleNames) {
            Tree.ModuleDescriptor moduleDescriptor =
                    compilationUnit.getModuleDescriptors()
                        .get(0);
            List<Tree.ImportModule> importModules =
                    moduleDescriptor.getImportModuleList()
                        .getImportModules();
            for (Tree.ImportModule im: importModules) {
                String importedName = getImportedName(im);
                if (importedName!=null &&
                        importedName.equals(moduleName)) {
                    if (!removeSharedAnnotation(textFileChange,
                            doc, im.getAnnotationList())) {
                        textFileChange.addEdit(
                                new InsertEdit(im.getStartIndex(),
                                        "shared "));
                    }
                }
            }
        }
        performChange(textFileChange);
    }

    public static boolean removeSharedAnnotation(
            TextFileChange textFileChange,
            IDocument doc, Tree.AnnotationList al) {
        boolean result = false;
        for (Tree.Annotation a: al.getAnnotations()) {
            Tree.BaseMemberExpression bme =
                    (Tree.BaseMemberExpression)
                        a.getPrimary();
            if (bme.getDeclaration()
                    .getName().equals("shared")) {
                int stop = a.getEndIndex();
                int start = a.getStartIndex();
                try {
                    while (isWhitespace(doc.getChar(stop))) {
                        stop++;
                    }
                } catch (BadLocationException e) {
                    e.printStackTrace();
                }
                textFileChange.addEdit(
                        new DeleteEdit(start, stop-start));
                result = true;
            }
        }
        return result;
    }
    
    public static int addModuleImports(
            IProject project, Module target,
            Map<String,ModuleVersionNode> moduleNamesAndVersions) {
        if (moduleNamesAndVersions.isEmpty()) return 0;
        ProjectPhasedUnit unit = 
                getDescriptorPhasedUnit(project, target);
        return addModuleImports(unit.getResourceFile(),
                unit.getCompilationUnit(), project,
                moduleNamesAndVersions);
    }

    public static int addModuleImports(IFile file,
            Tree.CompilationUnit cu, IProject project,
            Map<String, ModuleVersionNode> moduleNamesAndVersions) {
        TextFileChange textFileChange = 
                new TextFileChange("Add Module Imports",
                        file);
        textFileChange.setEdit(new MultiTextEdit());
        for (Map.Entry<String, ModuleVersionNode> entry:
                moduleNamesAndVersions.entrySet()) {
            Backends nativeBackend;
            String name = entry.getKey();
            String version = entry.getValue().getVersion();
            /*if (CeylonBuilder.compileToJava(project) &&
                CeylonBuilder.compileToJs(project)) {*/
                nativeBackend =
                        entry.getValue().getNativeBackend();
            /*}
            else {
                nativeBackend = null;
            }*/
            Module module = cu.getUnit().getPackage().getModule();
            Backends moduleBackends = module.getNativeBackends();
            if (moduleBackends!=null &&
                    moduleBackends.equals(nativeBackend)) {
                nativeBackend = null;
            }
            InsertEdit edit =
                    createAddEdit(cu,
                            nativeBackend, name, version,
                            getDocument(textFileChange));
            if (edit!=null) {
                textFileChange.addEdit(edit);
            }
        }
        performChange(textFileChange);
        return textFileChange.getEdit().getOffset();
    }

    private static ProjectPhasedUnit getDescriptorPhasedUnit(
            IProject project, Module module) {
        Unit unit = module.getUnit();
        if (unit instanceof ProjectSourceFile) {
            ProjectSourceFile ceylonUnit =
                    (ProjectSourceFile) unit;
            return ceylonUnit.getPhasedUnit();
        }
        return null;
    }
    
    private static InsertEdit createAddEdit(
            Tree.CompilationUnit unit, Backends backend,
            String moduleName, String moduleVersion,
            IDocument doc) {
        Tree.ImportModuleList iml = getImportList(unit);
        if (iml==null) return null;
        int offset;
        if (iml.getImportModules().isEmpty()) {
            offset = iml.getStartIndex() + 1;
        }
        else {
            offset = iml.getImportModules()
                    .get(iml.getImportModules().size() - 1)
                    .getEndIndex();
        }
        String newline = indents().getDefaultLineDelimiter(doc);
        StringBuilder importModule = new StringBuilder();
        appendImportStatement(importModule, false, backend,
                moduleName, moduleVersion, newline);
        if (iml.getEndToken().getLine()==iml.getToken().getLine()) {
            importModule.append(newline);
        }
        return new InsertEdit(offset,
                importModule.toString());
    }

    public static void appendImportStatement(
            StringBuilder importModule,
            boolean shared, Backends backend,
            String moduleName, String moduleVersion,
            String newline) {
        importModule.append(newline)
            .append(indents().getDefaultIndent());
        if (shared) {
            importModule.append("shared ");
        }
        if (backend!=null) {
            appendNative(importModule, backend);
            importModule.append(' ');
        }
        importModule.append("import ");
        if (!moduleName.matches("^[a-z_]\\w*(\\.[a-z_]\\w*)*$")) {
            importModule.append('"')
                .append(moduleName)
                .append('"');
        }
        else {
            importModule.append(moduleName);
        }
        importModule.append(" \"")
            .append(moduleVersion)
            .append("\";");
    }

    public static void appendNative(
    		StringBuilder builder, Backends backends) {
        builder.append("native(");
        appendNativeBackends(builder, backends);
        builder.append(")");
    }

	public static void appendNativeBackends(
			StringBuilder builder, Backends backends) {
		boolean first = true;
        for (Backend backend: backends) {
            if (first) {
                first = false;
            }
            else {
                builder.append(", ");
            }
            builder.append('"')
                   .append(backend.nativeAnnotation)
                   .append('"');
        }
	}

    private static DeleteEdit createRemoveEdit(
            Tree.CompilationUnit unit, String moduleName) {
        Tree.ImportModuleList iml = getImportList(unit);
        if (iml==null) return null;
        Tree.ImportModule prev = null;
        for (Tree.ImportModule im: iml.getImportModules()) {
            String ip = getImportedName(im);
            if (ip!=null && ip.equals(moduleName)) {
                int startOffset = im.getStartIndex();
                int length = im.getDistance();
                //TODO: handle whitespace for first import in list
                if (prev!=null) {
                    int endOffset = prev.getEndIndex();
                    length += startOffset-endOffset;
                    startOffset = endOffset;
                }
                return new DeleteEdit(startOffset, length);
            }
            prev = im;
        }
        return null;
    }

    private static InsertEdit createExportEdit(
            Tree.CompilationUnit unit, String moduleName) {
        Tree.ImportModuleList iml = getImportList(unit);
        if (iml==null) return null;
        for (Tree.ImportModule im: iml.getImportModules()) {
            String ip = getImportedName(im);
            if (ip!=null && ip.equals(moduleName)) {
                int startOffset = im.getStartIndex();
                return new InsertEdit(startOffset, "shared ");
            }
        }
        return null;
    }

    private static Tree.ImportModuleList getImportList(
            Tree.CompilationUnit unit) {
        List<Tree.ModuleDescriptor> moduleDescriptors =
                unit.getModuleDescriptors();
        if (!moduleDescriptors.isEmpty()) {
            Tree.ModuleDescriptor moduleDescriptor =
                    moduleDescriptors.get(0);
            return moduleDescriptor.getImportModuleList();
        }
        else {
            return null;
        }
    }

}
