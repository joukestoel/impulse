/*******************************************************************************
* Copyright (c) 2007 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package org.eclipse.imp.editor;

/*
 * Licensed Materials - Property of IBM,
 * (c) Copyright IBM Corp. 1998, 2004  All Rights Reserved
 */

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.imp.core.ErrorHandler;
import org.eclipse.imp.editor.internal.AnnotationCreator;
import org.eclipse.imp.editor.internal.CompletionProcessor;
import org.eclipse.imp.editor.internal.EditorErrorTickUpdater;
import org.eclipse.imp.editor.internal.FoldingController;
import org.eclipse.imp.editor.internal.FormattingController;
import org.eclipse.imp.editor.internal.HoverHelpController;
import org.eclipse.imp.editor.internal.OutlineController;
import org.eclipse.imp.editor.internal.PresentationController;
import org.eclipse.imp.editor.internal.ProblemMarkerManager;
import org.eclipse.imp.editor.internal.SourceHyperlinkController;
import org.eclipse.imp.editor.internal.ToggleBreakpointsAdapter;
import org.eclipse.imp.language.ILanguageService;
import org.eclipse.imp.language.Language;
import org.eclipse.imp.language.LanguageRegistry;
import org.eclipse.imp.language.ServiceFactory;
import org.eclipse.imp.parser.IMessageHandler;
import org.eclipse.imp.parser.IModelListener;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.preferences.PreferenceCache;
import org.eclipse.imp.preferences.PreferenceConstants;
import org.eclipse.imp.runtime.RuntimePlugin;
import org.eclipse.imp.services.IASTFindReplaceTarget;
import org.eclipse.imp.services.IAnnotationTypeInfo;
import org.eclipse.imp.services.IEditorService;
import org.eclipse.imp.services.ILanguageActionsContributor;
import org.eclipse.imp.services.ILanguageSyntaxProperties;
import org.eclipse.imp.services.IOccurrenceMarker;
import org.eclipse.imp.services.IRefactoringContributor;
import org.eclipse.imp.services.base.DefaultAnnotationHover;
import org.eclipse.imp.services.base.TreeModelBuilderBase;
import org.eclipse.imp.ui.DefaultPartListener;
import org.eclipse.imp.ui.textPresentation.HTMLTextPresenter;
import org.eclipse.jdt.ui.actions.IJavaEditorActionDefinitionIds;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.FontRegistry;
import org.eclipse.jface.text.AbstractInformationControlManager;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DefaultInformationControl;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IAutoEditStrategy;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IInformationControl;
import org.eclipse.jface.text.IInformationControlCreator;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextDoubleClickStrategy;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.ITextViewerExtension5;
import org.eclipse.jface.text.ITypedRegion;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.contentassist.ContentAssistant;
import org.eclipse.jface.text.contentassist.IContentAssistant;
import org.eclipse.jface.text.formatter.ContentFormatter;
import org.eclipse.jface.text.formatter.IContentFormatter;
import org.eclipse.jface.text.hyperlink.IHyperlinkDetector;
import org.eclipse.jface.text.hyperlink.IHyperlinkPresenter;
import org.eclipse.jface.text.information.IInformationPresenter;
import org.eclipse.jface.text.information.IInformationProvider;
import org.eclipse.jface.text.information.IInformationProviderExtension;
import org.eclipse.jface.text.information.InformationPresenter;
import org.eclipse.jface.text.presentation.IPresentationDamager;
import org.eclipse.jface.text.presentation.IPresentationReconciler;
import org.eclipse.jface.text.presentation.IPresentationRepairer;
import org.eclipse.jface.text.presentation.PresentationReconciler;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.DefaultCharacterPairMatcher;
import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.ICharacterPairMatcher;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.IVerticalRuler;
import org.eclipse.jface.text.source.projection.ProjectionAnnotationModel;
import org.eclipse.jface.text.source.projection.ProjectionSupport;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.SubActionBars;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.TextEditor;
import org.eclipse.ui.editors.text.TextSourceViewerConfiguration;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ContentAssistAction;
import org.eclipse.ui.texteditor.IEditorStatusLine;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.MarkerAnnotation;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;
import org.eclipse.ui.texteditor.TextOperationAction;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * An Eclipse editor, which is not enhanced using API; rather, we publish extension
 * points for outline, content assist, hover help, etc.
 * 
 * @author Chris Laffra
 * @author Robert M. Fuhrer
 */
public class UniversalEditor extends TextEditor implements IASTFindReplaceTarget {
    public static final String FORMAT_SOURCE_COMMAND= RuntimePlugin.IMP_RUNTIME + ".formatSource";

    public static final String TOGGLE_COMMENT_COMMAND= RuntimePlugin.IMP_RUNTIME + ".toggleComment";

    public static final String SHOW_OUTLINE_COMMAND= RuntimePlugin.IMP_RUNTIME + ".showOutlineCommand";

    /**
     * Action definition ID of the edit -> go to matching fence action
     * (value <code>"org.eclipse.imp.runtime.gotoMatchingFence"</code>).
     */
    public static final String GOTO_MATCHING_FENCE_COMMAND= RuntimePlugin.IMP_RUNTIME + ".gotoMatchingFence"; //$NON-NLS-1$

    public static final String MESSAGE_BUNDLE= "org.eclipse.imp.editor.messages";

    public static final String EDITOR_ID= RuntimePlugin.IMP_RUNTIME + ".impEditor";

    public static final String PARSE_ANNOTATION_TYPE= "org.eclipse.imp.editor.parseAnnotation";

    /** Preference key for matching brackets */
    protected final static String MATCHING_BRACKETS= PreferenceConstants.EDITOR_MATCHING_BRACKETS;

    /** Preference key for matching brackets color */
    protected final static String MATCHING_BRACKETS_COLOR= PreferenceConstants.EDITOR_MATCHING_BRACKETS_COLOR;

    public Language fLanguage;

    public ParserScheduler fParserScheduler;

    protected LanguageServiceManager fLanguageServiceManager;

    protected ServiceControllerManager fServiceControllerManager;

    private ProjectionAnnotationModel fAnnotationModel;

    private ProblemMarkerManager fProblemMarkerManager;

    private ICharacterPairMatcher fBracketMatcher;

    private SubActionBars fActionBars;

    private DefaultPartListener fRefreshContributions;

    private static final String BUNDLE_FOR_CONSTRUCTED_KEYS= MESSAGE_BUNDLE;//$NON-NLS-1$

    static ResourceBundle fgBundleForConstructedKeys= ResourceBundle.getBundle(BUNDLE_FOR_CONSTRUCTED_KEYS);

    public UniversalEditor() {
	if (PreferenceCache.emitMessages)
	    RuntimePlugin.getInstance().writeInfoMsg("Creating UniversalEditor instance");
	// SMS 4 Apr 2007
	// Do not set preference store with store obtained from plugin; one is
	// already defined for the parent text editor and populated with relevant
	// preferences
	//setPreferenceStore(RuntimePlugin.getInstance().getPreferenceStore());
	setSourceViewerConfiguration(new StructuredSourceViewerConfiguration());
	configureInsertMode(SMART_INSERT, true);
	setInsertMode(SMART_INSERT);
        fProblemMarkerManager= new ProblemMarkerManager();
    }

    public Object getAdapter(Class required) {
        if (IContentOutlinePage.class.equals(required)) {
            return fServiceControllerManager.getOutlineController();
	}
	if (IToggleBreakpointsTarget.class.equals(required)) {
		// SMS 14 MAR 2007  added "this" parameter
		// to make use of new constructor
	    return new ToggleBreakpointsAdapter(this);
	}
	if (IRegionSelectionService.class.equals(required)) {
	    return fRegionSelector;
	}
	return super.getAdapter(required);
    }

    protected void createActions() {
	super.createActions();

        final ResourceBundle bundle= ResourceBundle.getBundle(MESSAGE_BUNDLE);
	Action action= new ContentAssistAction(bundle, "ContentAssistProposal.", this);
	action.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
	setAction("ContentAssistProposal", action);
	markAsStateDependentAction("ContentAssistProposal", true);

        action= new TextOperationAction(bundle, "Format.", this, ISourceViewer.FORMAT); //$NON-NLS-1$
        action.setActionDefinitionId(FORMAT_SOURCE_COMMAND);
        setAction("Format", action); //$NON-NLS-1$
        markAsStateDependentAction("Format", true); //$NON-NLS-1$
        markAsSelectionDependentAction("Format", true); //$NON-NLS-1$
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.FORMAT_ACTION);

        action= new TextOperationAction(bundle, "ShowOutline.", this, StructuredSourceViewer.SHOW_OUTLINE); //$NON-NLS-1$
        action.setActionDefinitionId(SHOW_OUTLINE_COMMAND);
        setAction(SHOW_OUTLINE_COMMAND, action); //$NON-NLS-1$
//      PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.SHOW_OUTLINE_ACTION);

	action= new TextOperationAction(bundle, "ToggleComment.", this, StructuredSourceViewer.TOGGLE_COMMENT); //$NON-NLS-1$
	action.setActionDefinitionId(TOGGLE_COMMENT_COMMAND);
	setAction(TOGGLE_COMMENT_COMMAND, action); //$NON-NLS-1$
//	PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IJavaHelpContextIds.TOGGLE_COMMENT_ACTION);

        action= new GotoMatchingFenceAction(this);
        action.setActionDefinitionId(GOTO_MATCHING_FENCE_COMMAND);
        setAction(GOTO_MATCHING_FENCE_COMMAND, action);
    }

    protected void editorContextMenuAboutToShow(IMenuManager menu) {
        super.editorContextMenuAboutToShow(menu);

        contributeRefactoringActions(menu);
        contributeLanguageActions(menu);
    }

	private void contributeRefactoringActions(IMenuManager menu) {
		Set<IRefactoringContributor> contributors = fLanguageServiceManager.getRefactoringContributors();

		if (contributors != null && !contributors.isEmpty()) {
			List<IAction> editorActions = new ArrayList<IAction>();

			for (Iterator<IRefactoringContributor> iter = contributors
					.iterator(); iter.hasNext();) {
				IRefactoringContributor con = iter.next();

				try {
					IAction[] conActions = con
							.getEditorRefactoringActions(this);

					for (int i = 0; i < conActions.length; i++)
						editorActions.add(conActions[i]);
				} catch (Exception e) {
					RuntimePlugin.getInstance().logException(
							"Unable to create refactoring actions for contributor "
									+ con, e);
				}
			}
			Separator refGroup = new Separator("group.refactor");
			IMenuManager refMenu = new MenuManager("Refac&tor");

			menu.add(refGroup);
			menu.appendToGroup("group.refactor", refMenu);

			for (Iterator<IAction> actionIter = editorActions.iterator(); actionIter
					.hasNext();) {
				refMenu.add(actionIter.next());
			}
		}
	}

	private void contributeLanguageActions(IMenuManager menu) {
		Set<ILanguageActionsContributor> actionContributors= fLanguageServiceManager.getActionContributors();
		
		if (!actionContributors.isEmpty()) {
			menu.add(new Separator());
		}
		
        for(ILanguageActionsContributor con : actionContributors) {
		  try {
			con.contributeToEditorMenu(this, menu);
		  } catch(Exception e) {
		    RuntimePlugin.getInstance().logException("Unable to create editor actions for contributor " + con, e);
		  }
	    }
	}

    /* (non-Javadoc)
     * @see org.eclipse.ui.texteditor.AbstractDecoratedTextEditor#isOverviewRulerVisible()
     */
    protected boolean isOverviewRulerVisible() {
        return true;
    }

    /**
     * Sets the given message as error message to this editor's status line.
     *
     * @param msg message to be set
     */
    protected void setStatusLineErrorMessage(String msg) {
	IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
	if (statusLine != null)
	    statusLine.setMessage(true, msg, null);
    }

    /**
     * Sets the given message as message to this editor's status line.
     *
     * @param msg message to be set
     * @since 3.0
     */
    protected void setStatusLineMessage(String msg) {
	IEditorStatusLine statusLine= (IEditorStatusLine) getAdapter(IEditorStatusLine.class);
	if (statusLine != null)
	    statusLine.setMessage(false, msg, null);
    }

    public ProblemMarkerManager getProblemMarkerManager() {
        return fProblemMarkerManager;
    }

    public void updatedTitleImage(Image image) {
        setTitleImage(image);
    }

    
    // SMS 24 Jan 2007:  Restoring gotoAnnotation (which had been briefly
    // commented out) because it is called by the recently added class
    // GotoAnnotationAction.  Also made return void (since nothing is
    // returned and no return seems expected).
    // This change may be specific to Eclipse 3.1 and the method may
    // be removed again in versions of the Editor intended for Eclipse 3.2.
    // 
    // SMS 28 Jun 2007:  Yes, indeed, the void return type doesn't work
    // in Eclipse 3.2.  Converted return type back to Annotation and adapted
    // procedure to return an annotation in any case.
    
    /**
     * Jumps to the next enabled annotation according to the given direction.
     * An annotation type is enabled if it is configured to be in the
     * Next/Previous tool bar drop down menu and if it is checked.
     *
     * @param forward <code>true</code> if search direction is forward, <code>false</code> if backward
     */
    public Annotation /*void*/ gotoAnnotation(boolean forward) {
	ITextSelection selection= (ITextSelection) getSelectionProvider().getSelection();
	Position position= new Position(0, 0);

	// SMS 28 Jun 2007:  declared here for something to return from both
	// branches
	 Annotation annotation = null;
	
	if (false /* delayed - see bug 18316 */) {
	    annotation = getNextAnnotation(selection.getOffset(), selection.getLength(), forward, position);
	    selectAndReveal(position.getOffset(), position.getLength());
	} else /* no delay - see bug 18316 */{
	    /*Annotation*/ annotation= getNextAnnotation(selection.getOffset(), selection.getLength(), forward, position);

	    setStatusLineErrorMessage(null);
	    setStatusLineMessage(null);
	    if (annotation != null) {
		updateAnnotationViews(annotation);
		selectAndReveal(position.getOffset(), position.getLength());
		setStatusLineMessage(annotation.getText());
	    }
	}
	return annotation;
    }
    
    
    /**
     * Returns the annotation closest to the given range respecting the given
     * direction. If an annotation is found, the annotations current position
     * is copied into the provided annotation position.
     *
     * @param offset the region offset	
     * @param length the region length
     * @param forward <code>true</code> for forwards, <code>false</code> for backward
     * @param annotationPosition the position of the found annotation
     * @return the found annotation
     */
    private Annotation getNextAnnotation(final int offset, final int length, boolean forward, Position annotationPosition) {
	Annotation nextAnnotation= null;
	Position nextAnnotationPosition= null;
	Annotation containingAnnotation= null;
	Position containingAnnotationPosition= null;
	boolean currentAnnotation= false;

	IDocument document= getDocumentProvider().getDocument(getEditorInput());
	int endOfDocument= document.getLength();
	int distance= Integer.MAX_VALUE;

	IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());

	for(Iterator e= model.getAnnotationIterator(); e.hasNext(); ) {
	    Annotation a= (Annotation) e.next();
	    //	    if ((a instanceof IJavaAnnotation) && ((IJavaAnnotation) a).hasOverlay() || !isNavigationTarget(a))
	    //		continue;
	    // TODO RMF 4/19/2006 - Need more accurate logic here for filtering annotations
	    if (!(a instanceof MarkerAnnotation) && !a.getType().equals(PARSE_ANNOTATION_TYPE))
		continue;

	    Position p= model.getPosition(a);
	    if (p == null)
		continue;

	    if (forward && p.offset == offset || !forward && p.offset + p.getLength() == offset + length) {// || p.includes(offset)) {
		if (containingAnnotation == null
			|| (forward && p.length >= containingAnnotationPosition.length || !forward
				&& p.length >= containingAnnotationPosition.length)) {
		    containingAnnotation= a;
		    containingAnnotationPosition= p;
		    currentAnnotation= p.length == length;
		}
	    } else {
		int currentDistance= 0;

		if (forward) {
		    currentDistance= p.getOffset() - offset;
		    if (currentDistance < 0)
			currentDistance= endOfDocument + currentDistance;

		    if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
			distance= currentDistance;
			nextAnnotation= a;
			nextAnnotationPosition= p;
		    }
		} else {
		    currentDistance= offset + length - (p.getOffset() + p.length);
		    if (currentDistance < 0)
			currentDistance= endOfDocument + currentDistance;

		    if (currentDistance < distance || currentDistance == distance && p.length < nextAnnotationPosition.length) {
			distance= currentDistance;
			nextAnnotation= a;
			nextAnnotationPosition= p;
		    }
		}
	    }
	}
	if (containingAnnotationPosition != null && (!currentAnnotation || nextAnnotation == null)) {
	    annotationPosition.setOffset(containingAnnotationPosition.getOffset());
	    annotationPosition.setLength(containingAnnotationPosition.getLength());
	    return containingAnnotation;
	}
	if (nextAnnotationPosition != null) {
	    annotationPosition.setOffset(nextAnnotationPosition.getOffset());
	    annotationPosition.setLength(nextAnnotationPosition.getLength());
	}

	return nextAnnotation;
    }

    /**
     * Updates the annotation views that show the given annotation.
     *
     * @param annotation the annotation
     */
    private void updateAnnotationViews(Annotation annotation) {
	IMarker marker= null;
	if (annotation instanceof MarkerAnnotation)
	    marker= ((MarkerAnnotation) annotation).getMarker();
	else
	//        if (annotation instanceof IJavaAnnotation) {
	//	    Iterator e= ((IJavaAnnotation) annotation).getOverlaidIterator();
	//	    if (e != null) {
	//		while (e.hasNext()) {
	//		    Object o= e.next();
	//		    if (o instanceof MarkerAnnotation) {
	//			marker= ((MarkerAnnotation) o).getMarker();
	//			break;
	//		    }
	//		}
	//	    }
	//	}

	if (marker != null /*&& !marker.equals(fLastMarkerTarget)*/) {
	    try {
		boolean isProblem= marker.isSubtypeOf(IMarker.PROBLEM);
		IWorkbenchPage page= getSite().getPage();
		IViewPart view= page.findView(isProblem ? IPageLayout.ID_PROBLEM_VIEW : IPageLayout.ID_TASK_LIST); //$NON-NLS-1$  //$NON-NLS-2$
		if (view != null) {
		    Method method= view.getClass().getMethod(
			    "setSelection", new Class[] { IStructuredSelection.class, boolean.class }); //$NON-NLS-1$
		    method.invoke(view, new Object[] { new StructuredSelection(marker), Boolean.TRUE });
		}
	    } catch (CoreException x) {
	    } catch (NoSuchMethodException x) {
	    } catch (IllegalAccessException x) {
	    } catch (InvocationTargetException x) {
	    }
	    // ignore exceptions, don't update any of the lists, just set status line
	}
    }

    public void createPartControl(Composite parent) {
	fLanguage= LanguageRegistry.findLanguage(getEditorInput(), getDocumentProvider());

	// Create language service extensions now, since some services could
	// get accessed via super.createPartControl() (in particular, while
	// setting up the ISourceViewer).
	if (fLanguage != null) {
	    fLanguageServiceManager= new LanguageServiceManager(fLanguage);
	    fLanguageServiceManager.initialize();
            fServiceControllerManager= new ServiceControllerManager(this, fLanguageServiceManager);
            fServiceControllerManager.initialize();
	}

        super.createPartControl(parent);

	if (fLanguageServiceManager.getParseController() != null) {
	    fServiceControllerManager.setSourceViewer(getSourceViewer());
	    instantiateServiceControllers();
	}

        // SMS 4 Apr 2007:  Call no longer needed because preferences for the
        // overview ruler are now obtained from appropriate preference store directly
        //setupOverviewRulerAnnotations();

        // SMS 4 Apr 2007:  Also should not need this, since we're not using
        // the plugin's store (for this purpose)
        //AbstractDecoratedTextEditorPreferenceConstants.initializeDefaultValues(RuntimePlugin.getInstance().getPreferenceStore());

        setTitleImageFromLanguageIcon();

        IPreferenceStore prefStore= setSourceFontFromPreference();

        // N.B.: The editor's preference store is not the same as the IMP plugin's
        // preference store, which gets manipulated by the preference dialog fields.
        // So don't bother listening to what this.getPreferenceStore() returns.
//      getPreferenceStore().addPropertyChangeListener(fPrefStoreListener);
        prefStore.addPropertyChangeListener(fPrefStoreListener);

        initializeEditorContributors();
    }

    private IPreferenceStore setSourceFontFromPreference() {
        IPreferenceStore prefStore= RuntimePlugin.getInstance().getPreferenceStore();

        String fontName= prefStore.getString(PreferenceConstants.P_SOURCE_FONT);
        FontRegistry fontRegistry= RuntimePlugin.getInstance().getFontRegistry();

        if (!fontRegistry.hasValueFor(fontName)) {
            fontRegistry.put(fontName, PreferenceConverter.readFontData(fontName));
        }
        Font sourceFont= fontRegistry.get(fontName);

        if (sourceFont != null) {
            getSourceViewer().getTextWidget().setFont(sourceFont);
        }
        return prefStore;
    }

    private void instantiateServiceControllers() {
        try {
            StructuredSourceViewer sourceViewer= (StructuredSourceViewer) getSourceViewer();

            if (PreferenceCache.emitMessages) {
                RuntimePlugin.getInstance().writeInfoMsg(
                        "Creating language service controllers for " + fLanguage.getName());
            }

            fProblemMarkerManager.addListener(new EditorErrorTickUpdater(this));

            fParserScheduler= new ParserScheduler(fLanguageServiceManager.getParseController(), this, getDocumentProvider(),
                    fAnnotationCreator);

            // N.B. To first order, the source viewer can be created before instantiating
            // any service controllers. The following three services: text hover, hyperlink
            // detector, and content formatting, are the only exceptions. Most other controllers
            // *must* be instantiated after the source viewer has been created, since they
            // actually need to maintain a reference to it.

            // The source viewer configuration has already been asked for its ITextHover,
            // but before we actually instantiated the relevant controller class. So update
            // the source viewer, now that we actually have the hover provider.
            sourceViewer.setTextHover(fServiceControllerManager.getHoverHelpController(), IDocument.DEFAULT_CONTENT_TYPE);

            // The source viewer configuration has already been asked for its IContentFormatter,
            // but before we actually instantiated the relevant controller class. So update the
            // source viewer, now that we actually have the IContentFormatter.
            ContentFormatter formatter= new ContentFormatter();

            formatter.setFormattingStrategy(fServiceControllerManager.getFormattingController(), IDocument.DEFAULT_CONTENT_TYPE);
            sourceViewer.setFormatter(formatter);

            fServiceControllerManager.getPresentationController().damage(new Region(0, sourceViewer.getDocument().getLength()));
            // SMS 29 May 2007 (to give viewer access to single-line comment prefix)
            sourceViewer.setParseController(fLanguageServiceManager.getParseController());

            if (fLanguageServiceManager.getFoldingUpdater() != null) {
                ProjectionViewer projViewer= (ProjectionViewer) sourceViewer;
                ProjectionSupport projectionSupport= new ProjectionSupport(projViewer, getAnnotationAccess(),
                        getSharedColors());

                projectionSupport.install();
                projViewer.doOperation(ProjectionViewer.TOGGLE);
                fAnnotationModel= projViewer.getProjectionAnnotationModel();
                fParserScheduler.addModelListener(new FoldingController(fAnnotationModel, fLanguageServiceManager.getFoldingUpdater()));
            }

            fParserScheduler.addModelListener(new AnnotationCreatorListener());
            fServiceControllerManager.setupModelListeners(fParserScheduler);

            // TODO RMF 8/6/2007 - Disable "Mark Occurrences" if no occurrence marker exists for this language
            // The following doesn't work b/c getAction() doesn't find the Mark Occurrences action (why?)
            // if (this.fOccurrenceMarker == null)
            // getAction("org.eclipse.imp.runtime.actions.markOccurrencesAction").setEnabled(false);


            installExternalEditorServices();
            fParserScheduler.run(new NullProgressMonitor());
        } catch (Exception e) {
            ErrorHandler.reportError("Error while creating service controllers", e);
        }
    }

    private void setTitleImageFromLanguageIcon() {
        // Only set the editor's title bar icon if the language has a label provider
        if (fLanguageServiceManager.getLabelProvider() != null) {
            IEditorInput editorInput= getEditorInput();
            IFile file= EditorInputUtils.getFile(editorInput);

            setTitleImage(fLanguageServiceManager.getLabelProvider().getImage(file));
        }
    }

    private void installExternalEditorServices() {
        Set<IModelListener> editorServices= fLanguageServiceManager.getEditorServices();

        for(ILanguageService editorService : editorServices) {
            if (editorService instanceof IEditorService)
                ((IEditorService) editorService).setEditor(this);
            fParserScheduler.addModelListener((IModelListener) editorService);
        }
    }
    
    /**
	 * Adds elements to toolbars, menubars and statusbars
	 * 
	 */
	private void initializeEditorContributors() {
		if (fLanguage != null) {
			addEditorActions();
			registerEditorContributionsActivator();
		}
	}

	/**
	 * Makes sure that menu items and status bar items dissappear as the editor
	 * is out of focus, and reappear when it gets focus again. This does
	 * not work for toolbar items for unknown reasons, they stay visible.
	 *
	 */
	private void registerEditorContributionsActivator() {
		fRefreshContributions = new DefaultPartListener() {
			private UniversalEditor editor = UniversalEditor.this;

			public void partActivated(IWorkbenchPart part) {
				if (part == editor) {
					editor.fActionBars.activate();
					editor.fActionBars.updateActionBars();
				}
			}

			public void partDeactivated(IWorkbenchPart part) {
				if (part == editor) {
					editor.fActionBars.deactivate();
					editor.fActionBars.updateActionBars();
				}
			}
		};
		getSite().getPage().addPartListener(fRefreshContributions);
	}
	
	private void unregisterEditorContributionsActivator() {
	  getSite().getPage().removePartListener(fRefreshContributions);
	  fRefreshContributions = null;
	}

	/**
	 * Uses the LanguageActionsContributor extension point to add
	 * elements to (sub) action bars. 
	 *
	 */
	private void addEditorActions() {
		final IActionBars allActionBars = getEditorSite().getActionBars();
		if (fActionBars == null) {
			Set<ILanguageActionsContributor> contributors = ServiceFactory
					.getInstance().getLanguageActionsContributors(fLanguage);

			fActionBars = new SubActionBars(allActionBars);

			IStatusLineManager status = fActionBars.getStatusLineManager();
			IToolBarManager toolbar = fActionBars.getToolBarManager();
			IMenuManager menu = fActionBars.getMenuManager();

			for (ILanguageActionsContributor c : contributors) {
				c.contributeToStatusLine(this, status);
				c.contributeToToolBar(this, toolbar);
				c.contributeToMenuBar(this, menu);
			}

			fActionBars.updateActionBars();
			allActionBars.updateActionBars();
		}
		allActionBars.updateActionBars();
	}
	
    public void dispose() {
	// Remove the pref store listener *before* calling super; super nulls out the pref store.
        getPreferenceStore().removePropertyChangeListener(fPrefStoreListener);
        unregisterEditorContributionsActivator();
        
        if (fActionBars != null) {
          fActionBars.dispose();
          fActionBars = null;
        }
        
        super.dispose();
    }

    /**
     * Override creation of the normal source viewer with one that supports source folding.
     */
    protected ISourceViewer createSourceViewer(Composite parent, IVerticalRuler ruler, int styles) {
//	if (fFoldingUpdater == null)
//	    return super.createSourceViewer(parent, ruler, styles);

	fAnnotationAccess= createAnnotationAccess();
	fOverviewRuler= createOverviewRuler(getSharedColors());

	ISourceViewer viewer= new StructuredSourceViewer(parent, ruler, getOverviewRuler(), isOverviewRulerVisible(), styles);
	// ensure decoration support has been created and configured.
	getSourceViewerDecorationSupport(viewer);
	
	return viewer;
    }

    protected void configureSourceViewerDecorationSupport(SourceViewerDecorationSupport support) {
        ILanguageSyntaxProperties syntaxProps= fLanguageServiceManager.getParseController().getSyntaxProperties();
        getPreferenceStore().setValue(MATCHING_BRACKETS, true);
        if (syntaxProps != null) {
//          fBracketMatcher.setSourceVersion(getPreferenceStore().getString(JavaCore.COMPILER_SOURCE));
            String[][] fences= syntaxProps.getFences();
            if (fences != null) {
                StringBuilder sb= new StringBuilder();
                for(int i= 0; i < fences.length; i++) {
                    sb.append(fences[i][0]);
                    sb.append(fences[i][1]);
                }
                fBracketMatcher= new DefaultCharacterPairMatcher(sb.toString().toCharArray());
                support.setCharacterPairMatcher(fBracketMatcher);
                support.setMatchingCharacterPainterPreferenceKeys(MATCHING_BRACKETS, MATCHING_BRACKETS_COLOR);
            }
        }
        super.configureSourceViewerDecorationSupport(support);
    }

    /**
     * Jumps to the matching bracket.
     */
    public void gotoMatchingFence() {
        ISourceViewer sourceViewer= getSourceViewer();
        IDocument document= sourceViewer.getDocument();
        if (document == null)
            return;

        IRegion selection= getSignedSelection(sourceViewer);
        int selectionLength= Math.abs(selection.getLength());

        if (selectionLength > 1) {
            setStatusLineErrorMessage("Invalid selection");
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }

        // #26314
        int sourceCaretOffset= selection.getOffset() + selection.getLength();
        if (isSurroundedByBrackets(document, sourceCaretOffset))
            sourceCaretOffset -= selection.getLength();

        IRegion region= fBracketMatcher.match(document, sourceCaretOffset);
        if (region == null) {
            setStatusLineErrorMessage("No matching fence!");
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }

        int offset= region.getOffset();
        int length= region.getLength();

        if (length < 1)
            return;

        int anchor= fBracketMatcher.getAnchor();
        // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
        int targetOffset= (ICharacterPairMatcher.RIGHT == anchor) ? offset + 1: offset + length;

        boolean visible= false;
        if (sourceViewer instanceof ITextViewerExtension5) {
            ITextViewerExtension5 extension= (ITextViewerExtension5) sourceViewer;
            visible= (extension.modelOffset2WidgetOffset(targetOffset) > -1);
        } else {
            IRegion visibleRegion= sourceViewer.getVisibleRegion();
            // http://dev.eclipse.org/bugs/show_bug.cgi?id=34195
            visible= (targetOffset >= visibleRegion.getOffset() && targetOffset <= visibleRegion.getOffset() + visibleRegion.getLength());
        }

        if (!visible) {
            setStatusLineErrorMessage("Matching fence is outside the currently selected element.");
            sourceViewer.getTextWidget().getDisplay().beep();
            return;
        }

        if (selection.getLength() < 0)
            targetOffset -= selection.getLength();

        sourceViewer.setSelectedRange(targetOffset, selection.getLength());
        sourceViewer.revealRange(targetOffset, selection.getLength());
    }

    private boolean isBracket(char character) {
        ILanguageSyntaxProperties syntaxProps= fLanguageServiceManager.getParseController().getSyntaxProperties();
        String[][] fences= syntaxProps.getFences();

        for(int i= 0; i != fences.length; ++i) {
            if (fences[i][0].indexOf(character) >= 0)
                return true;
            if (fences[i][1].indexOf(character) >= 0)
                return true;
        }
        return false;
    }

    private boolean isSurroundedByBrackets(IDocument document, int offset) {
        if (offset == 0 || offset == document.getLength())
            return false;

        try {
            return isBracket(document.getChar(offset - 1)) &&
                   isBracket(document.getChar(offset));
        } catch (BadLocationException e) {
                return false;
        }
    }

    /**
     * Returns the signed current selection.
     * The length will be negative if the resulting selection
     * is right-to-left (RtoL).
     * <p>
     * The selection offset is model based.
     * </p>
     *
     * @param sourceViewer the source viewer
     * @return a region denoting the current signed selection, for a resulting RtoL selections length is < 0
     */
    protected IRegion getSignedSelection(ISourceViewer sourceViewer) {
            StyledText text= sourceViewer.getTextWidget();
            Point selection= text.getSelectionRange();

            if (text.getCaretOffset() == selection.x) {
                    selection.x= selection.x + selection.y;
                    selection.y= -selection.y;
            }

            selection.x= widgetOffset2ModelOffset(sourceViewer, selection.x);

            return new Region(selection.x, selection.y);
    }

    public final String PARSE_ANNOTATION = "Parse_Annotation";

    private Map<IMarker, Annotation> markerParseAnnotations = new HashMap<IMarker, Annotation>();
    private Map<IMarker, MarkerAnnotation> markerMarkerAnnotations = new HashMap<IMarker, MarkerAnnotation>();

    /**
     * Refresh the marker annotations on the input document by removing any
     * that do not map to current parse annotations.  Do this for problem
     * markers, specifically; ignore other types of markers.
     * 
     * SMS 25 Apr 2007
     */
    public void refreshMarkerAnnotations(String problemMarkerType)
    {
    	// Get current marker annotations
		IAnnotationModel model = getDocumentProvider().getAnnotationModel(getEditorInput());
		Iterator annIter = model.getAnnotationIterator();
		List<MarkerAnnotation> markerAnnotations = new ArrayList<MarkerAnnotation>();
		while (annIter.hasNext()) {
			Object ann = annIter.next();
			if (ann instanceof MarkerAnnotation) {
				markerAnnotations.add((MarkerAnnotation) ann);
			} 
		}

		// For the current marker annotations, if any lacks a corresponding
		// parse annotation, delete the marker annotation from the document's
		// annotation model (but leave the marker on the underlying resource,
		// which presumably hasn't been changed, despite changes to the document)
		for (int i = 0; i < markerAnnotations.size(); i++) {
			IMarker marker = markerAnnotations.get(i).getMarker();
			try {
				String markerType = marker.getType();
				if (!markerType.endsWith(problemMarkerType))
					continue;
			} catch (CoreException e) {
				// If we get a core exception here, probably something is wrong with the
				// marker, and we probably don't want to keep any annotation that may be
				// associated with it (I don't think)
				model.removeAnnotation(markerAnnotations.get(i));
				continue;
			}
			if (markerParseAnnotations.get(marker) != null) {
				continue;
			} else {
				model.removeAnnotation(markerAnnotations.get(i));
			}	
		}
    }
    
    
    /**
     * This is a type of listener whose purpose is to monitor changes to a document
     * annotation model and to maintain at a mapping from markers on the underlying
     * resource to parse annotations on the document.
     * 
     * The association of markers to annotations is determined by a subroutine that
     * may be more or less sophisticated in how it identifies associations.  The
     * accuracy of the map depends on the implementation of this routine.  (The
     * current implementation of the method simply compares text ranges of annotations
     * and markers.)
     * 
     * The motivating purpose of the mapping is to enable the identification of marker
     * annotations that are (or are not) associated with a current parse annotation.
     * Then, for instance, marker annotations that are not associated with current parse 
     * annotations might be removed from the document.
     * 
     * No assumptions are made here about the type (or types) of marker annotation of
     * interest; all types of marker annotation are considered.
     * 
     * SMS 25 Apr 2007
     */
    protected class InputAnnotationModelListener implements IAnnotationModelListener
    {
    	public void modelChanged(IAnnotationModel model)
    	{
    		List<Annotation> currentParseAnnotations = new ArrayList<Annotation>();
    		List<IMarker> currentMarkers = new ArrayList<IMarker>();
    		markerParseAnnotations = new HashMap<IMarker,Annotation>();
    		markerMarkerAnnotations = new HashMap<IMarker,MarkerAnnotation>();
    		
    		// Collect the current set of markers and parse annotations;
    		// also maintain a map of markers to marker annotations (as	
    		// there doesn't seem to be a way to get from a marker to the
    		// annotations that may represent it)
    		Iterator annotations = model.getAnnotationIterator();
    		while (annotations.hasNext()) {
    			Object ann = annotations.next();
    			if (ann instanceof MarkerAnnotation) {
    				IMarker marker = ((MarkerAnnotation)ann).getMarker();
    				currentMarkers.add(marker);
    				markerMarkerAnnotations.put(marker, (MarkerAnnotation) ann);
    			} else if (ann instanceof Annotation) {
    				Annotation annotation = (Annotation) ann;
    				if (annotation.getType().equals(PARSE_ANNOTATION_TYPE)) {
    					currentParseAnnotations.add(annotation);
    				}
    			}
    		}

    		// Create a mapping between current markers and parse annotations
    		for (int i = 0; i < currentMarkers.size(); i++) {
    			IMarker marker = currentMarkers.get(i);
				Annotation annotation = findParseAnnotationForMarker(model, marker, currentParseAnnotations);
				if (annotation != null) {
					markerParseAnnotations.put(marker, annotation);
				}
    		}
    	}
    	
    	
    	public Annotation findParseAnnotationForMarker(IAnnotationModel model, IMarker marker, List parseAnnotations) {
    		Integer markerStartAttr = null;
    		Integer markerEndAttr = null;
			try {
				// SMS 22 May 2007:  With markers created through the editor the CHAR_START
				// and CHAR_END attributes are null, giving rise to NPEs here.  Not sure
				// why this happens, but it seems to help down the line to trap the NPE.
				markerStartAttr = ((Integer) marker.getAttribute(IMarker.CHAR_START));
				markerEndAttr = ((Integer) marker.getAttribute(IMarker.CHAR_END));
				if (markerStartAttr == null || markerEndAttr == null) {
					return null;
				}
			} catch (CoreException e) {
				System.err.println("UniversalEditor.findParseAnnotationForMarker:  CoreException geting marker start and end attributes");
				return null;
			} catch (NullPointerException e) {
				System.err.println("UniversalEditor.findParseAnnotationForMarker:  NullPointerException geting marker start and end attributes");
				return null;
			}
			
   			int markerStart = markerStartAttr.intValue();
			int markerEnd = markerEndAttr.intValue();
			int markerLength = markerEnd - markerStart;

			for (int j = 0; j < parseAnnotations.size(); j++) {
				Annotation parseAnnotation = (Annotation) parseAnnotations.get(j);
				Position pos = model.getPosition(parseAnnotation);
				if (pos == null)
					// And this would be why?
					continue;
				int annotationStart = pos.offset;
				int annotationLength = pos.length;
				//System.out.println("\tfindPareseAnnotationForMarker:  Checking annotation offset and length = " + annotationStart + ", " + annotationLength);
				
				if (markerStart == annotationStart && markerLength == annotationLength) {
					//System.out.println("\tfindPareseAnnotationForMarker:  Returning annotation at offset = " + markerStart);
					return parseAnnotation;
				} else {
  					//System.out.println("\tfindPareseAnnotationForMarker:  Not returning annotation at offset = " + markerStart);
				}
			}
			
			//System.out.println("  findPareseAnnotationForMarker:  No corresponding annotation found; returning null");
			return null;
    	}   	
    }

    protected void doSetInput(IEditorInput input) throws CoreException {
    	// SMS 22 May 2007:  added try/catch around doSetInput(..)
    	try {
    		super.doSetInput(input);
    	} catch (NullPointerException e) {
    		return;
    	}
    	setInsertMode(SMART_INSERT);
	
    	// SMS 25 Apr 2007
    	// Added for maintenance of associations between marker annotations
    	// and parse annotations	
    	IAnnotationModel annotationModel = getDocumentProvider().getAnnotationModel(input);
    	// RMF 6 Jun 2007 - Not sure why annotationModel is null for files outside the
    	// workspace, but they are, so make sure we don't cause an NPE here.
    	if (annotationModel != null)
    	    annotationModel.addAnnotationModelListener(new InputAnnotationModelListener());
    }

    /**
     * Add a Model listener to this editor. Anytime the underlying AST is recomputed, the listener is notified.
     * 
     * @param listener the listener to notify of Model changes
     */
    public void addModelListener(IModelListener listener) {
	fParserScheduler.addModelListener(listener);
    }

    class StructuredSourceViewerConfiguration extends TextSourceViewerConfiguration {
	public int getTabWidth(ISourceViewer sourceViewer) {
            return PreferenceCache.tabWidth;
//	    return fPreferenceStore.getInt(PreferenceConstants.P_TAB_WIDTH);
	}

	public IPresentationReconciler getPresentationReconciler(ISourceViewer sourceViewer) {
	    // BUG Perhaps we shouldn't use a PresentationReconciler; its JavaDoc says it runs in the UI thread!
	    PresentationReconciler reconciler= new PresentationReconciler();
	    reconciler.setRepairer(new PresentationRepairer(), IDocument.DEFAULT_CONTENT_TYPE);
            reconciler.setDamager(new IPresentationDamager() {

                public IRegion getDamageRegion(ITypedRegion partition, DocumentEvent event, boolean documentPartitioningChanged) {
                    return partition;
                }

                public void setDocument(IDocument document) {
                    // TODO Auto-generated method stub
                    
                }
                
            }, IDocument.DEFAULT_CONTENT_TYPE);
	    return reconciler;
	}

	public IContentAssistant getContentAssistant(ISourceViewer sourceViewer) {
	    ContentAssistant ca= new ContentAssistant();
	    ca.setContentAssistProcessor(fServiceControllerManager.getCompletionProcessor(), IDocument.DEFAULT_CONTENT_TYPE);
	    ca.setInformationControlCreator(getInformationControlCreator(sourceViewer));
	    return ca;
	}

	public IAnnotationHover getAnnotationHover(ISourceViewer sourceViewer) {
	    IAnnotationHover hover= fLanguageServiceManager.getAnnotationHover();
	    if (hover == null)
		hover= new DefaultAnnotationHover();
	    return hover;
	}

	public IAutoEditStrategy[] getAutoEditStrategies(ISourceViewer sourceViewer, String contentType) {
	    IAutoEditStrategy autoEdit= fLanguageServiceManager.getAutoEditStrategy();
	    if (autoEdit == null)
		autoEdit= super.getAutoEditStrategies(sourceViewer, contentType)[0];

            // TODO Permit multiple auto-edit strategies
	    return new IAutoEditStrategy[] { autoEdit };
	}

	public IContentFormatter getContentFormatter(ISourceViewer sourceViewer) {
	    // Disable the content formatter if no language-specific implementation exists.
	    // N.B.: This will probably always be null, since this method gets called before 
	    // the formatting controller has been instantiated (which happens in
	    // instantiateServiceControllers()).
	    if (fServiceControllerManager.getFormattingController() == null)
		return null;

	    // For now, assumes only one content type (i.e. one kind of partition)
	    ContentFormatter formatter= new ContentFormatter();

	    formatter.setFormattingStrategy(fServiceControllerManager.getFormattingController(), IDocument.DEFAULT_CONTENT_TYPE);
	    return formatter;
	}

	public String[] getDefaultPrefixes(ISourceViewer sourceViewer, String contentType) {
	    return super.getDefaultPrefixes(sourceViewer, contentType);
	}

	public ITextDoubleClickStrategy getDoubleClickStrategy(ISourceViewer sourceViewer, String contentType) {
	    return super.getDoubleClickStrategy(sourceViewer, contentType);
	}

	public IHyperlinkDetector[] getHyperlinkDetectors(ISourceViewer sourceViewer) {
	    if (fServiceControllerManager.getHyperLinkController() != null)
		return new IHyperlinkDetector[] { fServiceControllerManager.getHyperLinkController() };
	    return super.getHyperlinkDetectors(sourceViewer);
	}

	public IHyperlinkPresenter getHyperlinkPresenter(ISourceViewer sourceViewer) {
	    return super.getHyperlinkPresenter(sourceViewer);
	}

	public String[] getIndentPrefixes(ISourceViewer sourceViewer, String contentType) {
	    return super.getIndentPrefixes(sourceViewer, contentType);
	}

	public IInformationControlCreator getInformationControlCreator(ISourceViewer sourceViewer) {
	    return new IInformationControlCreator() {
		public IInformationControl createInformationControl(Shell parent) {
		    int shellStyle= SWT.RESIZE | SWT.TOOL;
		    int style= SWT.NONE; // SWT.V_SCROLL | SWT.H_SCROLL;

		    // return new OutlineInformationControl(parent, shellStyle, style, new HTMLTextPresenter(false));
		    return new DefaultInformationControl(parent, style, new HTMLTextPresenter(false), "Press 'F2' for focus");
		}
	    };
	}

	private InformationPresenter fInfoPresenter;

	public IInformationPresenter getInformationPresenter(ISourceViewer sourceViewer) {
	    if (fInfoPresenter == null) {
		fInfoPresenter= new InformationPresenter(getInformationControlCreator(sourceViewer));
		fInfoPresenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
		fInfoPresenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
		IInformationProvider provider= new IInformationProvider() { // this should be language-specific
		    public IRegion getSubject(ITextViewer textViewer, int offset) {
			return new Region(offset, 10);
		    }
		    public String getInformation(ITextViewer textViewer, IRegion subject) {
			return "Hi Mom!";
		    }
		};
		fInfoPresenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
		fInfoPresenter.setSizeConstraints(60, 10, true, false);
		fInfoPresenter.setRestoreInformationControlBounds(getSettings("outline_presenter_bounds"), true, true); //$NON-NLS-1$
	    }
	    return fInfoPresenter;
	}

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType) {
	    return fServiceControllerManager.getHoverHelpController();
	}

	public ITextHover getTextHover(ISourceViewer sourceViewer, String contentType, int stateMask) {
	    return super.getTextHover(sourceViewer, contentType, stateMask);
	}

	public IUndoManager getUndoManager(ISourceViewer sourceViewer) {
	    return super.getUndoManager(sourceViewer);
	}

	public IAnnotationHover getOverviewRulerAnnotationHover(ISourceViewer sourceViewer) {
	    return super.getOverviewRulerAnnotationHover(sourceViewer);
	}

	private class LangInformationProvider implements IInformationProvider, IInformationProviderExtension {
	    private TreeModelBuilderBase fBuilder;
	    public IRegion getSubject(ITextViewer textViewer, int offset) {
		return new Region(offset, 10);
	    }
	    public String getInformation(ITextViewer textViewer, IRegion subject) {
		return "never called?!?"; // shouldn't be called, given IInformationProviderExtension???
	    }
	    public Object getInformation2(ITextViewer textViewer, IRegion subject) {
                if (fBuilder == null) {
                    fBuilder= fLanguageServiceManager.getModelBuilder();
                }
		return fBuilder.buildTree(fLanguageServiceManager.getParseController().getCurrentAst());
	    }
	}

	private IInformationProvider fSourceElementProvider= new LangInformationProvider();

	public IInformationPresenter getOutlinePresenter(ISourceViewer sourceViewer) {
	    TreeModelBuilderBase modelBuilder = fLanguageServiceManager.getModelBuilder();
	    if (modelBuilder == null) {
		  return null;
	    }

	    InformationPresenter presenter;

	    presenter= new InformationPresenter(getOutlinePresenterControlCreator(sourceViewer, IJavaEditorActionDefinitionIds.SHOW_OUTLINE));
	    presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
	    presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);

	    IInformationProvider provider= fSourceElementProvider;

	    presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
	    // TODO Should associate all other partition types with this provider, too
	    presenter.setSizeConstraints(50, 20, true, false);
	    presenter.setRestoreInformationControlBounds(getSettings("outline_presenter_bounds"), true, true); //$NON-NLS-1$
	    return presenter;
	}

	/**
	 * Returns the outline presenter control creator. The creator is a factory creating outline
	 * presenter controls for the given source viewer. This implementation always returns a creator
	 * for <code>OutlineInformationControl</code> instances.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param commandId the ID of the command that opens this control
	 * @return an information control creator
	 */
	private IInformationControlCreator getOutlinePresenterControlCreator(ISourceViewer sourceViewer, final String commandId) {
	    return new IInformationControlCreator() {
		public IInformationControl createInformationControl(Shell parent) {
		    int shellStyle= SWT.RESIZE;
		    int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;

		    return new OutlineInformationControl(parent, shellStyle, treeStyle, commandId, UniversalEditor.this.fLanguage);
		}
	    };
	}

	/**
	 * Returns the hierarchy presenter which will determine and shown type hierarchy
	 * information requested for the current cursor position.
	 *
	 * @param sourceViewer the source viewer to be configured by this configuration
	 * @param doCodeResolve a boolean which specifies whether code resolve should be used to compute the program element
	 * @return an information presenter
	 */
	public IInformationPresenter getHierarchyPresenter(ISourceViewer sourceViewer, boolean doCodeResolve) {
	    InformationPresenter presenter= new InformationPresenter(getHierarchyPresenterControlCreator(sourceViewer));
	    presenter.setDocumentPartitioning(getConfiguredDocumentPartitioning(sourceViewer));
	    presenter.setAnchor(AbstractInformationControlManager.ANCHOR_GLOBAL);
	    IInformationProvider provider= null; // TODO RMF new HierarchyInformationProvider(this);
	    presenter.setInformationProvider(provider, IDocument.DEFAULT_CONTENT_TYPE);
//	    presenter.setInformationProvider(provider, IJavaPartitions.JAVA_DOC);
//	    presenter.setInformationProvider(provider, IJavaPartitions.JAVA_MULTI_LINE_COMMENT);
//	    presenter.setInformationProvider(provider, IJavaPartitions.JAVA_SINGLE_LINE_COMMENT);
//	    presenter.setInformationProvider(provider, IJavaPartitions.JAVA_STRING);
//	    presenter.setInformationProvider(provider, IJavaPartitions.JAVA_CHARACTER);
	    presenter.setSizeConstraints(50, 20, true, false);
	    presenter.setRestoreInformationControlBounds(getSettings("hierarchy_presenter_bounds"), true, true); //$NON-NLS-1$
	    return presenter;
	}

	private IInformationControlCreator getHierarchyPresenterControlCreator(ISourceViewer sourceViewer) {
	    return new IInformationControlCreator() {
		public IInformationControl createInformationControl(Shell parent) {
		    int shellStyle= SWT.RESIZE;
		    int treeStyle= SWT.V_SCROLL | SWT.H_SCROLL;

		    return new DefaultInformationControl(parent); // HierarchyInformationControl(parent, shellStyle, treeStyle);
		}
	    };
	}

	/**
	 * Returns the settings for the given section.
	 *
	 * @param sectionName the section name
	 * @return the settings
	 * @since 3.0
	 */
	private IDialogSettings getSettings(String sectionName) {
	    IDialogSettings settings= RuntimePlugin.getInstance().getDialogSettings().getSection(sectionName);
	    if (settings == null)
		settings= RuntimePlugin.getInstance().getDialogSettings().addNewSection(sectionName);
	    return settings;
	}
    }

    class PresentationRepairer implements IPresentationRepairer {
	IDocument fDocument;

	public void createPresentation(TextPresentation presentation, ITypedRegion damage) {
	    // BUG Should we really just ignore the presentation passed in???
	    // JavaDoc says we're responsible for "merging" our changes in...
	    try {
		if (fServiceControllerManager.getPresentationController() != null && fLanguageServiceManager.getParseController() != null) {
		    fServiceControllerManager.getPresentationController().damage(damage);
		    fParserScheduler.cancel();
		    fParserScheduler.schedule();
		}
	    } catch (Exception e) {
		ErrorHandler.reportError("Could not repair damage ", e);
	    }
	}

	public void setDocument(IDocument document) {
	    fDocument= document;
	}
    }

    private IMessageHandler fAnnotationCreator= new AnnotationCreator(this, PARSE_ANNOTATION_TYPE);

    private final IPropertyChangeListener fPrefStoreListener= new IPropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent event) {
            if (event.getProperty().equals(PreferenceConstants.P_SOURCE_FONT)) {
                FontRegistry fontReg= RuntimePlugin.getInstance().getFontRegistry();

                fontReg.put(PreferenceConstants.P_SOURCE_FONT, (FontData[]) event.getNewValue());

                Font sourceFont= fontReg.get(PreferenceConstants.P_SOURCE_FONT);

                if (getSourceViewer() != null)
        	    getSourceViewer().getTextWidget().setFont(sourceFont);
            } else if (event.getProperty().equals(PreferenceConstants.P_TAB_WIDTH)) {
                int newTab= (event.getNewValue() instanceof String) ? Integer.parseInt((String) event.getNewValue()) : ((Integer) event.getNewValue()).intValue();
        	PreferenceCache.tabWidth= newTab;
        	if (getSourceViewer() != null)
        	    getSourceViewer().getTextWidget().setTabs(PreferenceCache.tabWidth);
            } else if (event.getProperty().equals(PreferenceConstants.P_EMIT_MESSAGES)) {
        	PreferenceCache.emitMessages= ((Boolean) event.getNewValue()).booleanValue();
            } else if (event.getProperty().equals(PreferenceConstants.P_DUMP_TOKENS)) {
        	PreferenceCache.dumpTokens= ((Boolean) event.getNewValue()).booleanValue();
            }
        }
    };

    private final IRegionSelectionService fRegionSelector= new IRegionSelectionService() {
        public void selectAndReveal(int startOffset, int length) {
            IEditorPart activeEditor= PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
            AbstractTextEditor textEditor= (AbstractTextEditor) activeEditor;

            textEditor.selectAndReveal(startOffset, length);
        }
    };

    private class AnnotationCreatorListener implements IModelListener {
        public AnalysisRequired getAnalysisRequired() {
            return AnalysisRequired.NONE; // Even if it doesn't scan, it's ok - this posts the error annotations!
        }
        public void update(IParseController parseController, IProgressMonitor monitor) {
            // SMS 25 Apr 2007
            // Since parsing has finished, check whether the marker annotations
            // are up-to-date with the most recent parse annotations.
            // Assuming that's often enough--i.e., don't refresh the marker
            // annotations after every update to the document annotation model
            // since there will be many of these, including possibly many that
            // don't relate to problem markers.
            final IAnnotationTypeInfo annotationTypeInfo= fLanguageServiceManager.getParseController().getAnnotationTypeInfo();
            if (annotationTypeInfo != null) {
                List problemMarkerTypes = annotationTypeInfo.getProblemMarkerTypes();
                for (int i = 0; i < problemMarkerTypes.size(); i++) {
                    refreshMarkerAnnotations((String)problemMarkerTypes.get(i));
                }
            }
        }
    }

    public String getSelectionText() {
	Point sel= getSelection();
        IFileEditorInput fileEditorInput= (IFileEditorInput) getEditorInput();
        IDocument document= getDocumentProvider().getDocument(fileEditorInput);

        try {
	    return document.get(sel.x, sel.y);
	} catch (BadLocationException e) {
	    e.printStackTrace();
	    return "";
	}
    }

    public Point getSelection() {
	ISelection sel= this.getSelectionProvider().getSelection();
	ITextSelection textSel= (ITextSelection) sel;

	return new Point(textSel.getOffset(), textSel.getLength());
    }

    public boolean canPerformFind() {
	return true;
    }

    public IParseController getParseController() {
	return fLanguageServiceManager.getParseController();
    }
    
    public IOccurrenceMarker getOccurrenceMarker() {
        return fLanguageServiceManager.getOccurrenceMarker();
    }

    // SMS 4 May 2006:
    // Added this as the only way I could think of (so far) to
    // remove parser annotations that I expect to be duplicated
    // if a save triggers a build that leads to the creation
    // of markers and another set of annotations.
	public void doSave(IProgressMonitor progressMonitor) {
		// SMS 25 Apr 2007:  Removing parser annotations here
		// may not hurt but also doesn't seem to be necessary
		//removeParserAnnotations();
		super.doSave(progressMonitor);
	}

    public void removeParserAnnotations() {
    	IAnnotationModel model= getDocumentProvider().getAnnotationModel(getEditorInput());

    	for(Iterator i= model.getAnnotationIterator(); i.hasNext(); ) {
    	    Annotation a= (Annotation) i.next();

    	    if (a.getType().equals(PARSE_ANNOTATION_TYPE))
    	    	model.removeAnnotation(a);
    	}
    }

    public String toString() {
        String langName= (fLanguage != null ? " for " + fLanguage.getName() : "");
        String inputDesc= (fParserScheduler != null && fLanguageServiceManager.getParseController() != null && fLanguageServiceManager.getParseController().getPath() != null) ? " on source " + fLanguageServiceManager.getParseController().getPath().toPortableString() : "";
        return "Universal Editor" + langName +  " on " + inputDesc + getEditorInput();
    }
}
