package com.redhat.ceylon.eclipse.code.outline;


import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_ERR;
import static com.redhat.ceylon.eclipse.ui.ICeylonResources.CEYLON_WARN;

import java.util.HashMap;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IDecoration;
import org.eclipse.jface.viewers.ILabelDecorator;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.LabelProviderChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DragSource;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.Widget;

import com.redhat.ceylon.eclipse.ui.CeylonPlugin;

/**
 * Decorates an image of a program element to indicate any related errors/warnings.
 * See org.eclipse.jdt.ui.ProblemsLabelDecorator for inspiration.
 * @author rfuhrer@watson.ibm.com
 */
public class CeylonLabelDecorator implements ILabelDecorator {
    /**
     * This is a special <code>LabelProviderChangedEvent</code> carrying additional 
     * information whether the event originates from a marker change.
     * <p>
     * <code>ProblemsLabelChangedEvent</code>s are only generated by <code>
     * ProblemsLabelDecorator</code>s.
     * </p>
     */
    public static class ProblemsLabelChangedEvent extends LabelProviderChangedEvent {
        private static final long serialVersionUID= 1L;

        private boolean fMarkerChange;

        /**
         * Note: This constructor is for internal use only. Clients should not call this constructor.
         * 
         * @param eventSource the base label provider
         * @param changedResource the changed resources
         * @param isMarkerChange <code>true<code> if the change is a marker change; otherwise
         *  <code>false</code> 
         */
        public ProblemsLabelChangedEvent(IBaseLabelProvider eventSource, IResource[] changedResource, boolean isMarkerChange) {
            super(eventSource, changedResource);
            fMarkerChange= isMarkerChange;
        }

        /**
         * Returns whether this event origins from marker changes. If <code>false</code> an annotation 
         * model change is the origin. In this case viewers not displaying working copies can ignore these 
         * events.
         * 
         * @return if this event origins from a marker change.
         */
        public boolean isMarkerChange() {
            return fMarkerChange;
        }
    }

    private static final int ERRORTICK_WARNING= 0;
    private static final int ERRORTICK_ERROR= 1;

    private ImageDescriptorRegistry fRegistry;
    private ImageDecoratorController fDecoratorController;

    //private IProblemChangedListener fProblemChangedListener;

    private ListenerList fListeners;

    public CeylonLabelDecorator() {
        fRegistry= null;
        //fProblemChangedListener= null;
        fDecoratorController= new ImageDecoratorController();
    }

    private ImageDescriptorRegistry getRegistry() {
        if (fRegistry==null) {
            fRegistry= new ImageDescriptorRegistry();
        }
        return fRegistry;
    }

    public String decorateText(String text, Object element) {
        return text;
    }

    public Image decorateImage(Image image, Object obj) {
        int adornmentFlags= computeAdornmentFlags(obj);
        if (adornmentFlags!=0) {
            ImageDescriptor baseImage= new ImageImageDescriptor(image);
            Rectangle bounds= image.getBounds();
            Point ptBounds= new Point(bounds.width, bounds.height);
            return getRegistry().get(fDecoratorController.getImageDescriptor(baseImage, obj, ptBounds));
        }
        return image;
    }

    protected int computeAdornmentFlags(Object obj) {
        return 1;
    }

    /*protected int computeAdornmentFlags(Object obj) {
        try {
            if (obj instanceof ISourceEntity) {
                ISourceEntity element= (ISourceEntity) obj;
                IResource res= element.getResource();
                int depth= IResource.DEPTH_INFINITE;
                if (element instanceof ICompilationUnit) {
                    depth= IResource.DEPTH_ONE;
                }
                return getErrorTicksFromMarkers(res, depth);
            } else if (obj instanceof IResource) {
                return getErrorTicksFromMarkers((IResource) obj, IResource.DEPTH_INFINITE);
            }
        }
        catch (CoreException e) {
            if (e.getStatus().getCode() == IResourceStatus.MARKER_NOT_FOUND) {
                return 0;
            }
            e.printStackTrace();
        }
        return 0;
    }*/

    // TODO The following code used to use the given ISourceReference to determine which markers
    // lay within the source range of the given entity. We don't yet have ISourceEntity's for
    // anything smaller than a compilation unit, so that functionality will have to wait for an
    // API enhancement.
    /*private int getErrorTicksFromMarkers(IResource res, int depth) //ISourceReference sourceElement 
    		throws CoreException {
        if (res == null || !res.isAccessible()) {
            return 0;
        }
        int info= 0;
        IMarker[] markers= res.findMarkers(IMarker.PROBLEM, true, depth);
        if (markers != null) {
            for(int i= 0; i < markers.length && (info != ERRORTICK_ERROR); i++) {
                IMarker curr= markers[i];
//              if (sourceElement == null || isMarkerInRange(curr, sourceElement)) {
                    int priority= curr.getAttribute(IMarker.SEVERITY, -1);
                    if (priority == IMarker.SEVERITY_WARNING) {
                        info= ERRORTICK_WARNING;
                    } else if (priority == IMarker.SEVERITY_ERROR) {
                        info= ERRORTICK_ERROR;
                    }
//              }
            }
        }
        return info;
    }

    private int getErrorTicksFromAnnotationModel(IAnnotationModel model)//ISourceReference sourceElement 
            throws CoreException {
        int info= 0;
        Iterator iter= model.getAnnotationIterator();
        while ((info != ERRORTICK_ERROR) && iter.hasNext()) {
            Annotation annot= (Annotation) iter.next();
            IMarker marker= isAnnotationInRange(model, annot);//sourceElement
            if (marker != null) {
                int priority= marker.getAttribute(IMarker.SEVERITY, -1);
                if (priority == IMarker.SEVERITY_WARNING) {
                    info= ERRORTICK_WARNING;
                } else if (priority == IMarker.SEVERITY_ERROR) {
                    info= ERRORTICK_ERROR;
                }
            }
        }
        return info;
    }

    private IMarker isAnnotationInRange(IAnnotationModel model, Annotation annot) //ISourceReference sourceElement
            throws CoreException {
        if (annot instanceof MarkerAnnotation) {
//          if (sourceElement == null || isInside(model.getPosition(annot), sourceElement)) {
                IMarker marker= ((MarkerAnnotation) annot).getMarker();
                if (marker.exists() && marker.isSubtypeOf(IMarker.PROBLEM)) {
                    return marker;
                }
//          }
        }
        return null;
    }*/

    public void dispose() {
        /*if (fProblemChangedListener != null) {
            // TODO RMF reenable
            //	    JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fProblemChangedListener);
            fProblemChangedListener= null;
        }*/
        if (fRegistry != null) {
            fRegistry.dispose();
        }
    }

    public boolean isLabelProperty(Object element, String property) {
        return true;
    }

    public void addListener(ILabelProviderListener listener) {
        if (fListeners == null) {
            fListeners= new ListenerList();
        }
        fListeners.add(listener);
        /*if (fProblemChangedListener == null) {
            fProblemChangedListener= new IProblemChangedListener() {
                public void problemsChanged(IResource[] changedResources, boolean isMarkerChange) {
                    fireProblemsChanged(changedResources, isMarkerChange);
                }
            };
            // TODO RMF reenable
            //	    JavaPlugin.getDefault().getProblemMarkerManager().addListener(fProblemChangedListener);
        }*/
    }

    public void removeListener(ILabelProviderListener listener) {
        if (fListeners != null) {
            fListeners.remove(listener);
            /*if (fListeners.isEmpty() && fProblemChangedListener != null) {
                // TODO RMF reenable
                //		JavaPlugin.getDefault().getProblemMarkerManager().removeListener(fProblemChangedListener);
                fProblemChangedListener= null;
            }*/
        }
    }

    /*private void fireProblemsChanged(IResource[] changedResources, boolean isMarkerChange) {
        if (fListeners != null && !fListeners.isEmpty()) {
            LabelProviderChangedEvent event= new ProblemsLabelChangedEvent(this, changedResources, isMarkerChange);
            Object[] listeners= fListeners.getListeners();
            for(int i= 0; i < listeners.length; i++) {
                ((ILabelProviderListener) listeners[i]).labelProviderChanged(event);
            }
        }
    }*/

    public void decorate(Object element, IDecoration decoration) {
        int adornmentFlags= computeAdornmentFlags(element);
        ImageRegistry registry = CeylonPlugin.getInstance().getImageRegistry();
        if (adornmentFlags == ERRORTICK_ERROR) {
            decoration.addOverlay(registry.getDescriptor(CEYLON_WARN));
        } 
        else if (adornmentFlags == ERRORTICK_WARNING) {
            decoration.addOverlay(registry.getDescriptor(CEYLON_ERR));
        }
    }
}

class ImageImageDescriptor extends ImageDescriptor {
    private Image fImage;

    public ImageImageDescriptor(Image image) {
        super();
        fImage= image;
    }

    public ImageData getImageData() {
        return fImage.getImageData();
    }

    public boolean equals(Object obj) {
        return (obj != null) && getClass().equals(obj.getClass()) && 
        		fImage.equals(((ImageImageDescriptor)obj).fImage);
    }

    public int hashCode() {
        return fImage.hashCode();
    }
}

/**
 * A registry that maps <code>ImageDescriptors</code> to <code>Image</code>.
 */
class ImageDescriptorRegistry {
    private HashMap<ImageDescriptor,Image> fRegistry= new HashMap<ImageDescriptor,Image>(10);

    private Display fDisplay;

    /**
     * Creates a new image descriptor registry for the current or 
     * default display, respectively.
     */
    public ImageDescriptorRegistry() {
        this(SWTUtil.getStandardDisplay());
    }

    /**
     * Creates a new image descriptor registry for the given display. 
     * All images managed by this registry will be disposed when the 
     * display gets disposed.
     * 
     * @param display
     *            the display the images managed by this registry are allocated for
     */
    public ImageDescriptorRegistry(Display display) {
        fDisplay= display;
        Assert.isNotNull(fDisplay);
        hookDisplay();
    }

    /**
     * Returns the image associated with the given image descriptor.
     * 
     * @param descriptor
     *            the image descriptor for which the registry manages an image
     * @return the image associated with the image descriptor or <code>null</code> 
     * if the image descriptor can't create the requested image.
     */
    public Image get(ImageDescriptor descriptor) {
        if (descriptor == null)
            descriptor= ImageDescriptor.getMissingImageDescriptor();
        Image result= (Image) fRegistry.get(descriptor);
        if (result != null)
            return result;
        Assert.isTrue(fDisplay == SWTUtil.getStandardDisplay(), "Allocating image for wrong display."); //$NON-NLS-1$
        result= descriptor.createImage();
        if (result != null)
            fRegistry.put(descriptor, result);
        return result;
    }

    /**
     * Disposes all images managed by this registry.
     */
    public void dispose() {
        for(Image image: fRegistry.values()) {
            image.dispose();
        }
        fRegistry.clear();
    }

    private void hookDisplay() {
        fDisplay.disposeExec(new Runnable() {
            public void run() {
                dispose();
            }
        });
    }
}

class SWTUtil {
    /**
     * Returns the standard display to be used. The method first checks, if the thread calling this method has an associated display. If so, this display is
     * returned. Otherwise the method returns the default display.
     */
    public static Display getStandardDisplay() {
        Display display;
        display= Display.getCurrent();
        if (display == null)
            display= Display.getDefault();
        return display;
    }

    /**
     * Returns the shell for the given widget. If the widget doesn't represent a SWT object that manage a shell, <code>null</code> is returned.
     * 
     * @return the shell for the given widget
     */
    public static Shell getShell(Widget widget) {
        if (widget instanceof Control)
            return ((Control) widget).getShell();
        if (widget instanceof Caret)
            return ((Caret) widget).getParent().getShell();
        if (widget instanceof DragSource)
            return ((DragSource) widget).getControl().getShell();
        if (widget instanceof DropTarget)
            return ((DropTarget) widget).getControl().getShell();
        if (widget instanceof Menu)
            return ((Menu) widget).getParent().getShell();
        if (widget instanceof ScrollBar)
            return ((ScrollBar) widget).getParent().getShell();
        return null;
    }

    /**
     * Returns a width hint for a button control.
     */
    public static int getButtonWidthHint(Button button) {
        button.setFont(JFaceResources.getDialogFont());
        PixelConverter converter= new PixelConverter(button);
        int widthHint= converter.convertHorizontalDLUsToPixels(IDialogConstants.BUTTON_WIDTH);
        return Math.max(widthHint, button.computeSize(SWT.DEFAULT, SWT.DEFAULT, true).x);
    }

    /**
     * Sets width and height hint for the button control. <b>Note:</b> This is a NOP if the button's layout data is not an instance of <code>GridData</code>.
     * 
     * @param button
     *            the button for which to set the dimension hint
     */
    public static void setButtonDimensionHint(Button button) {
        Assert.isNotNull(button);
        Object gd= button.getLayoutData();
        if (gd instanceof GridData) {
            ((GridData) gd).widthHint= getButtonWidthHint(button);
            ((GridData) gd).horizontalAlignment= GridData.FILL;
        }
    }

    public static int getTableHeightHint(Table table, int rows) {
        if (table.getFont().equals(JFaceResources.getDefaultFont()))
            table.setFont(JFaceResources.getDialogFont());
        int result= table.getItemHeight() * rows + table.getHeaderHeight();
        if (table.getLinesVisible())
            result+= table.getGridLineWidth() * (rows - 1);
        return result;
    }
}