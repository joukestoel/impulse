package io.usethesource.impulse.editor;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.texteditor.ITextEditor;

import io.usethesource.impulse.language.ServiceFactory;
import io.usethesource.impulse.parser.IParseController;
import io.usethesource.impulse.parser.ISourcePositionLocator;
import io.usethesource.impulse.services.INavigationTargetFinder;

public class SelectEnclosingAction extends Action {
    private UniversalEditor fEditor;
    private INavigationTargetFinder fNavTargetFinder;

    public SelectEnclosingAction() {
        this(null);
    }

    public SelectEnclosingAction(UniversalEditor editor) {
        super("Select Enclosing");
        setActionDefinitionId(IEditorActionDefinitionIds.SELECT_ENCLOSING);
        setEditor(editor);
    }

    public void setEditor(ITextEditor editor) {
        fNavTargetFinder= null;
        if (editor instanceof UniversalEditor) {
            fEditor= (UniversalEditor) editor;
            if (fEditor.getLanguage() != null) {
                fNavTargetFinder= ServiceFactory.getInstance().getNavigationTargetFinder(fEditor.getLanguage());
            }
        } else {
            fEditor= null;
        }
        setEnabled(fNavTargetFinder != null);
    }

    @Override
    public void run() {
        IRegion selection= fEditor.getSelectedRegion();
        IParseController pc= fEditor.getParseController();
        ISourcePositionLocator locator= pc.getSourcePositionLocator();
        Object curNode= locator.findNode(pc.getCurrentAst(), selection.getOffset(), selection.getOffset() + selection.getLength() - 1);
        if (curNode == null || selection.getOffset() == 0) {
            curNode= pc.getCurrentAst();
        }
        Object enclosing= fNavTargetFinder.getEnclosingConstruct(curNode, pc.getCurrentAst());
    
        if (enclosing != null) {
            int enclOffset= locator.getStartOffset(enclosing);
            int enclEnd= locator.getEndOffset(enclosing);

            fEditor.selectAndReveal(enclOffset, enclEnd - enclOffset + 1);
        }
    }
}
