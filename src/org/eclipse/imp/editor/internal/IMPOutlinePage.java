package org.eclipse.imp.editor.internal;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.imp.editor.ModelTreeNode;
import org.eclipse.imp.editor.OutlineContentProviderBase;
import org.eclipse.imp.editor.OutlineLabelProvider.IElementImageProvider;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.parser.ISourcePositionLocator;
import org.eclipse.imp.services.base.TreeModelBuilderBase;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

public class IMPOutlinePage extends ContentOutlinePage implements
        IModelListener {

    private final OutlineContentProviderBase fContentProvider;
    private final TreeModelBuilderBase fModelBuilder;
    private final ILabelProvider fLabelProvider;
    private final IElementImageProvider fImageProvider;
    private final IParseController fParseController;

    public IMPOutlinePage(IParseController parseController, // OutlineContentProviderBase contentProvider,
            TreeModelBuilderBase modelBuilder,
            ILabelProvider labelProvider, IElementImageProvider imageProvider) {
        fParseController= parseController;
//      fContentProvider= contentProvider;
        fModelBuilder= modelBuilder;
        fLabelProvider= labelProvider;
        fImageProvider= imageProvider;

        fContentProvider= new OutlineContentProviderBase(null) {
            private ModelTreeNode fOldTree= null;

            public Object[] getChildren(Object element) {
                ModelTreeNode node= (ModelTreeNode) element;
                return node.getChildren();
            }
            public Object getParent(Object element) {
                ModelTreeNode node= (ModelTreeNode) element;
                return node.getParent();
            }
            @Override
            public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
                super.inputChanged(viewer, oldInput, newInput);
                if (fOldTree != null) {
                    TreeDiffer treeDiffer= new TreeDiffer((TreeViewer) viewer, fLabelProvider);
                    treeDiffer.diff((ModelTreeNode) oldInput, (ModelTreeNode) newInput);
                }
                fOldTree= (ModelTreeNode) newInput;
            }
        };
    }

    public AnalysisRequired getAnalysisRequired() {
        return IModelListener.AnalysisRequired.SYNTACTIC_ANALYSIS;
    }

    public void update(final IParseController parseController,
            IProgressMonitor monitor) {
        if (getTreeViewer() != null) {
            getTreeViewer().getTree().getDisplay().asyncExec(new Runnable() {
                public void run() {
                    getTreeViewer().setInput(fModelBuilder.buildTree(fParseController.getCurrentAst()));
                }
            });
        }
    }

    @Override
    public void selectionChanged(SelectionChangedEvent event) {
        super.selectionChanged(event);
        ITreeSelection sel= (ITreeSelection) event.getSelection();

        if (sel.isEmpty())
            return;

        ModelTreeNode first= (ModelTreeNode) sel.getFirstElement();
        ISourcePositionLocator locator= fParseController.getNodeLocator();
        Object node= first.getASTNode();
        int startOffset= locator.getStartOffset(node);
        int endOffset= locator.getEndOffset(node);

        IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
        AbstractTextEditor textEditor= (AbstractTextEditor) activeEditor;

        textEditor.selectAndReveal(startOffset, endOffset - startOffset + 1);
    }

    public void createControl(Composite parent) {
        super.createControl(parent);
        TreeViewer viewer= getTreeViewer();
        viewer.setContentProvider(fContentProvider);
        viewer.setLabelProvider(fLabelProvider);
        viewer.addSelectionChangedListener(this);
        ModelTreeNode rootNode= fModelBuilder.buildTree(fParseController.getCurrentAst());
        viewer.setInput(rootNode);
     }
}