package com.redhat.ceylon.eclipse.code.preferences;

import static com.redhat.ceylon.eclipse.code.editor.Navigation.openInEditor;
import static org.eclipse.jface.layout.GridDataFactory.fillDefaults;
import static org.eclipse.jface.layout.GridDataFactory.swtDefaults;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.internal.ui.wizards.buildpaths.FolderSelectionDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;

import com.redhat.ceylon.eclipse.core.model.modelJ2C;
import com.redhat.ceylon.ide.common.model.CeylonProject;
import com.redhat.ceylon.ide.common.model.CeylonProjectConfig;

public class CeylonModuleResolutionBlock {

    private IProject project;

    private ceylon.language.Boolean flatClasspath;
    private ceylon.language.Boolean autoExportMavenDependencies;

    private Text overridesText;
    private Button overridesBrowseButton;
    private Button overridesCreateButton;
    private Button flatClasspathButton;
    private Button autoExportMavenDependenciesButton;
    

    public CeylonModuleResolutionBlock() {
    }
    
    public IProject getProject() {
        return project;
    }

    public String getOverrides() {
        String overrides = overridesText.getText();
        return overrides.isEmpty() ? null : overrides;
    }

    public Boolean getFlatClasspath() {
        return flatClasspath == null ? null : flatClasspath.booleanValue();
    }

    public Boolean getAutoExportMavenDependencies() {
        return autoExportMavenDependencies == null ? null : autoExportMavenDependencies.booleanValue();
    }

    public void performDefaults() {
        overridesText.setText("");

    }
    
    public void initState(IProject project, 
            boolean isCeylonNatureEnabled) {
        this.project = project;
        
        ceylon.language.String overrides = null;
        if (isCeylonNatureEnabled) {
            CeylonProject<IProject> ceylonProject = modelJ2C.ceylonModel().getProject(project);
            if (project != null) {
                
            }
            CeylonProjectConfig<IProject> config = 
                    ceylonProject.getConfiguration();
            overrides = config.getProjectOverrides();
            flatClasspath = config.getProjectFlatClasspath();
            autoExportMavenDependencies = 
                    config.getProjectAutoExportMavenDependencies();
        }
        
        boolean flat = 
                flatClasspath!=null && 
                flatClasspath.booleanValue();
        flatClasspathButton.setSelection(flat);
        flatClasspathButton.addListener(SWT.Selection, 
                new Listener() {
            public void handleEvent(Event e) {
                if (flatClasspath == null) {
                    flatClasspath = ceylon.language.Boolean.instance(true);
                } else {
                    flatClasspath = ceylon.language.Boolean.instance(!flatClasspath.booleanValue());
                }
            }
        });

        boolean autoExport = 
                autoExportMavenDependencies!=null &&
                autoExportMavenDependencies.booleanValue();
        autoExportMavenDependenciesButton.setSelection(autoExport);
        autoExportMavenDependenciesButton.addListener(SWT.Selection, 
                new Listener() {
            public void handleEvent(Event e) {
                if (autoExportMavenDependencies == null) {
                    autoExportMavenDependencies = ceylon.language.Boolean.instance(true);
                } else {
                    autoExportMavenDependencies = 
                            ceylon.language.Boolean.instance(!autoExportMavenDependencies.booleanValue());
                }
            }
        });

        if (overrides != null) {
            overridesText.setText(overrides.value.trim());
        } else {
            overridesText.setText("");
        }
    }

    private final String overridesTextWithoutLink = 
            "Module overrides file (customize module resolution)";
    private final String overridesTextWithLink = 
            "Module <a>overrides file</a> (customize module resolution)";
    
    public void initContents(Composite parent) {
//        final Group resolutionGroup = new Group(parent, SWT.NONE);
//        resolutionGroup.setText("Module Resolution");
//        resolutionGroup.setLayoutData(fillDefaults().grab(true, false).create());
//        resolutionGroup.setLayout(GridLayoutFactory.swtDefaults().create());
        
        final Link overridesLabel = 
                new Link(parent, SWT.LEFT | SWT.WRAP);
        overridesLabel.setText(overridesTextWithoutLink);
        overridesLabel.setToolTipText(
                "Choose the XML file used to specify module overrides");
        overridesLabel.setLayoutData(swtDefaults()
                .span(2, 1)
                .grab(true, false)
                .align(SWT.FILL, SWT.CENTER)
                .create());
        overridesLabel.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                final IPath overridesPath = 
                        new Path(overridesText.getText());
                if (overridesPath.isAbsolute()) {
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                openInEditor(overridesPath, true);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                } else {
                    final IFile overridesResource = 
                            project.getFile(overridesPath);
                    try {
                        overridesResource.refreshLocal(
                                IResource.DEPTH_ZERO, 
                                null);
                    } catch (CoreException e1) {
                        e1.printStackTrace();
                    }
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                openInEditor(overridesResource, true);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }
            }
        });

        overridesText = 
                new Text(parent, SWT.SINGLE | SWT.BORDER);
        overridesText.setLayoutData(swtDefaults()
                .align(SWT.FILL, SWT.CENTER)
                .hint(250,  SWT.DEFAULT)
                .grab(true, false)
                .create());
        overridesText.addModifyListener(new ModifyListener() {
            @Override
            public void modifyText(ModifyEvent e) {
                if (overridesText.getText().isEmpty()) {
                    overridesLabel.setText(overridesTextWithoutLink);
                } else {
                    overridesLabel.setText(overridesTextWithLink);
                }
            }
        });
                
        Composite overridesComposite = 
                new Composite(parent, SWT.NONE);
        overridesComposite.setLayout(new GridLayout(2, true));
        overridesComposite.setLayoutData(swtDefaults()
                .grab(true, true)
                .align(SWT.FILL, SWT.FILL)
                .create());

        initOverridesBrowseButton(overridesComposite);
        initOverridesCreateButton(overridesComposite);

//        Composite checkBoxesComposite = new Composite(parent, SWT.NONE);
//        checkBoxesComposite.setLayout(GridLayoutFactory.fillDefaults().equalWidth(false).create());
//        checkBoxesComposite.setLayoutData(swtDefaults().grab(true, true).create());

        flatClasspathButton = new Button(parent, SWT.CHECK);
        flatClasspathButton.setText("Use a flat classpath");
        flatClasspathButton.setLayoutData(swtDefaults()
                .span(2, 1)
                .grab(true, false)
                .create());

        autoExportMavenDependenciesButton = 
                new Button(parent, SWT.CHECK);
        autoExportMavenDependenciesButton.setText(
                "Automatically export Maven dependencies");
        autoExportMavenDependenciesButton.setLayoutData(fillDefaults()
                .span(2, 1)
                .grab(true, false)
                .create());


        performDefaults();
    }


    private void initOverridesBrowseButton(final Composite composite) {
        overridesBrowseButton = 
                new Button(composite, SWT.PUSH);
        overridesBrowseButton.setText("Browse...");
        overridesBrowseButton.setLayoutData(swtDefaults()
                .grab(true, true)
                .align(SWT.FILL, SWT.CENTER)
                .create());
        overridesBrowseButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                 FileDialog fileDialog = 
                         new FileDialog(composite.getShell(), 
                                 SWT.SHEET);
                 fileDialog.setFilterExtensions(
                         new String[] {"*.xml", "*.*"});
                 String path = 
                         project.getLocation().toFile()
                             .getAbsolutePath();
                fileDialog.setFilterPath(path);
                 fileDialog.setFileName("overrides.xml");
                 String result = fileDialog.open();
                 if (result != null) {
                     IPath overridesPath = new Path(result);
                     IPath projectLocation = 
                             project.getLocation();
                     if (projectLocation.isPrefixOf(overridesPath)) {
                         result = overridesPath
                                 .removeFirstSegments(projectLocation.segmentCount())
                                 .toString();
                     }
                    overridesText.setText(result);
                }
            }
        });
    }

    private void initOverridesCreateButton(final Composite composite) {
        overridesCreateButton = 
                new Button(composite, SWT.PUSH);
        overridesCreateButton.setText("Create...");
        overridesCreateButton.setLayoutData(swtDefaults()
                .grab(true, true)
                .align(SWT.FILL, SWT.CENTER)
                .create());
        overridesCreateButton.addSelectionListener(new SelectionAdapter() {
            @Override
            public void widgetSelected(SelectionEvent e) {
                Shell shell = composite.getShell();

                FolderSelectionDialog dialog = 
                        new FolderSelectionDialog(shell, 
                                new WorkbenchLabelProvider(), 
                                new WorkbenchContentProvider());
                dialog.setTitle("Select Folder");
                dialog.setMessage("Select a folder to contain the module overrides file:");
                dialog.setAllowMultiple(false);
                dialog.addFilter(new ViewerFilter() {
                    @Override
                    public boolean select(Viewer viewer, 
                            Object parentElement, 
                            Object element) {
                        return (element instanceof IFolder ||
                                element instanceof IProject) &&
                                ((IContainer) element).getProject()
                                    .equals(project);
                    }
                });
                dialog.setInput(project.getParent());
                dialog.setInitialSelection(project);
                if (dialog.open()==Window.CANCEL) {
                    return;
                }
                IContainer folder = (IContainer) 
                        dialog.getFirstResult();
                try {
                    final IFile overridesResource = 
                            folder.getFile(Path.fromPortableString("overrides.xml"));
                    try {
                        overridesResource.refreshLocal(
                                IResource.DEPTH_ZERO, 
                                null);
                    } catch (CoreException e1) {
                        e1.printStackTrace();
                    }
                    if (overridesResource.exists()) {
                        MessageDialog.openError(shell, 
                                "Overrides File Creation", 
                                "An 'overrides.xml' file already exists in this folder.");
                        return;
                    }
                    try {
                        overridesResource.create(new ByteArrayInputStream(
                                ("<overrides xmlns=\"http://www.ceylon-lang.org/xsd/overrides\">\n" + 
                                "</overrides>").getBytes("ASCII")), true, null);
                    } catch (UnsupportedEncodingException e1) {
                        e1.printStackTrace();
                    }
                    Display.getDefault().asyncExec(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                openInEditor(overridesResource, true);
                            } catch (PartInitException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                    String path = 
                            overridesResource.getProjectRelativePath()
                                .toPortableString();
                    overridesText.setText(path);
                    MessageDialog.openInformation(shell, 
                            "Overrides File Creation", 
                            "The 'overrides.xml' file was created.");
                } catch (CoreException e1) {
                    e1.printStackTrace();
                }
            }
        });
    }

}
