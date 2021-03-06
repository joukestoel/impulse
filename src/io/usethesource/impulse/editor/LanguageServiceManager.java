/*******************************************************************************
* Copyright (c) 2008 IBM Corporation.
* All rights reserved. This program and the accompanying materials
* are made available under the terms of the Eclipse Public License v1.0
* which accompanies this distribution, and is available at
* http://www.eclipse.org/legal/epl-v10.html
*
* Contributors:
*    Robert Fuhrer (rfuhrer@watson.ibm.com) - initial API and implementation

*******************************************************************************/

package io.usethesource.impulse.editor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.text.source.IAnnotationHover;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IPartListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;

import io.usethesource.impulse.core.ErrorHandler;
import io.usethesource.impulse.editor.OutlineLabelProvider.IElementImageProvider;
import io.usethesource.impulse.language.Language;
import io.usethesource.impulse.language.ServiceFactory;
import io.usethesource.impulse.parser.IModelListener;
import io.usethesource.impulse.parser.IParseController;
import io.usethesource.impulse.preferences.PreferenceCache;
import io.usethesource.impulse.runtime.RuntimePlugin;
import io.usethesource.impulse.services.IAutoEditStrategy;
import io.usethesource.impulse.services.IContentProposer;
import io.usethesource.impulse.services.IDocumentationProvider;
import io.usethesource.impulse.services.IEditorInputResolver;
import io.usethesource.impulse.services.IEntityNameLocator;
import io.usethesource.impulse.services.IFoldingUpdater;
import io.usethesource.impulse.services.IHelpService;
import io.usethesource.impulse.services.IHoverHelper;
import io.usethesource.impulse.services.ILanguageActionsContributor;
import io.usethesource.impulse.services.IOccurrenceMarker;
import io.usethesource.impulse.services.IQuickFixAssistant;
import io.usethesource.impulse.services.IRefactoringContributor;
import io.usethesource.impulse.services.IReferenceResolver;
import io.usethesource.impulse.services.ISourceFormatter;
import io.usethesource.impulse.services.ISourceHyperlinkDetector;
import io.usethesource.impulse.services.IToggleBreakpointsHandler;
import io.usethesource.impulse.services.ITokenColorer;
import io.usethesource.impulse.services.base.TreeModelBuilderBase;

public class LanguageServiceManager {
    private Language fLanguage;

    private ServiceFactory fServiceFactory = ServiceFactory.getInstance();

    private Set<ILanguageActionsContributor> fActionContributors;

    private IAnnotationHover fAnnotationHover;

    private Set<IAutoEditStrategy> fAutoEditStrategies;

    private IContentProposer fContentProposer;

    private IHelpService fContextHelper;

    private IDocumentationProvider fDocProvider;

    private Set<IModelListener> fEditorServices;

    private IEntityNameLocator fEntityNameLocator;

    private IFoldingUpdater fFoldingUpdater;

    private ISourceFormatter fFormattingStrategy;

    private IHoverHelper fHoverHelper;

    private ISourceHyperlinkDetector fHyperLinkDetector;

    private IElementImageProvider fImageProvider;

    private ILabelProvider fLabelProvider;

    private TreeModelBuilderBase fModelBuilder;

    private IOccurrenceMarker fOccurrenceMarker;

    private IParseController fParseController;

    private Set<IRefactoringContributor> fRefactoringContributors;

    private IReferenceResolver fResolver;

    private IToggleBreakpointsHandler fToggleBreakpointsHandler;

    private ITokenColorer fTokenColorer;

    private IEditorInputResolver fEditorInputResolver;

    private IQuickFixAssistant fQuickFixAssistant;

    public LanguageServiceManager(Language lang) {
        fLanguage= lang;
    }

    public void initialize(IEditorPart part) {
    	saveMyServiceManager(part, this);
        if (PreferenceCache.emitMessages)
            RuntimePlugin.getInstance().writeInfoMsg("Instantiating language service extensions for " + fLanguage.getName());

        fActionContributors= fServiceFactory.getLanguageActionsContributors(fLanguage);
        fAnnotationHover= fServiceFactory.getAnnotationHover(fLanguage);
        fAutoEditStrategies= fServiceFactory.getAutoEditStrategies(fLanguage);
        fContentProposer= fServiceFactory.getContentProposer(fLanguage);
        fContextHelper= fServiceFactory.getContextHelper(fLanguage);
        fDocProvider= fServiceFactory.getDocumentationProvider(fLanguage);
        fEditorServices= fServiceFactory.getEditorServices(fLanguage);
        fEntityNameLocator= fServiceFactory.getEntityNameLocator(fLanguage);
        fFoldingUpdater= fServiceFactory.getFoldingUpdater(fLanguage);
        fFormattingStrategy= fServiceFactory.getSourceFormatter(fLanguage);
        fHyperLinkDetector= fServiceFactory.getSourceHyperlinkDetector(fLanguage);
        fImageProvider= fServiceFactory.getElementImageProvider(fLanguage);
        fLabelProvider= fServiceFactory.getLabelProvider(fLanguage);
        fModelBuilder= fServiceFactory.getTreeModelBuilder(fLanguage);
        fOccurrenceMarker = fServiceFactory.getOccurrenceMarker(fLanguage);
        fParseController= fServiceFactory.getParseController(fLanguage);
        fRefactoringContributors= fServiceFactory.getRefactoringContributors(fLanguage);
        fResolver= fServiceFactory.getReferenceResolver(fLanguage);
        fToggleBreakpointsHandler= fServiceFactory.getToggleBreakpointsHandler(fLanguage);
        fTokenColorer= fServiceFactory.getTokenColorer(fLanguage);
        fEditorInputResolver = fServiceFactory.getEditorInputResolver(fLanguage);
        fQuickFixAssistant= fServiceFactory.getQuickFixAssistant(fLanguage);

        if (fHyperLinkDetector == null)
            fHyperLinkDetector= new HyperlinkDetector(fLanguage);

        if (fParseController == null) {
            ErrorHandler.reportError("Unable to instantiate parser for language '" + fLanguage.getName()
                    + "'; parser-related services will be disabled.", true, true);
        }
    }

    public void dispose() {
        fActionContributors= null;
        fAnnotationHover= null;
        fAutoEditStrategies= null;
        fContentProposer= null;
        fContextHelper= null;
        fDocProvider= null;
        fEditorServices= null;
        fEntityNameLocator= null;
        fFoldingUpdater= null;
        fFormattingStrategy= null;
        fHyperLinkDetector= null;
        fImageProvider= null;
        fLabelProvider= null;
        fModelBuilder= null;
        fOccurrenceMarker = null;
        fParseController= null;
        fRefactoringContributors= null;
        fResolver= null;
        fToggleBreakpointsHandler= null;
        fTokenColorer= null;
    }

    public Language getLanguage() {
        return fLanguage;
    }
    public Set<ILanguageActionsContributor> getActionContributors() {
        return fActionContributors;
    }

    public IAnnotationHover getAnnotationHover() {
        return fAnnotationHover;
    }

    public Set<IAutoEditStrategy> getAutoEditStrategies() {
        return fAutoEditStrategies;
    }

    public IContentProposer getContentProposer() {
        return fContentProposer;
    }

    public IHelpService getContextHelp() {
        return fContextHelper;
    }

    public IDocumentationProvider getDocProvider() {
        return fDocProvider;
    }

    public Set<IModelListener> getEditorServices() {
        return fEditorServices;
    }

    public IFoldingUpdater getFoldingUpdater() {
        return fFoldingUpdater;
    }

    public ISourceFormatter getFormattingStrategy() {
        return fFormattingStrategy;
    }

    public IHoverHelper getHoverHelper() {
        return fHoverHelper;
    }

    public ISourceHyperlinkDetector getHyperLinkDetector() {
        return fHyperLinkDetector;
    }

    public IElementImageProvider getImageProvider() {
        return fImageProvider;
    }

    public ILabelProvider getLabelProvider() {
        return fLabelProvider;
    }

    public TreeModelBuilderBase getModelBuilder() {
        return fModelBuilder;
    }

    public IOccurrenceMarker getOccurrenceMarker() {
        return fOccurrenceMarker;
    }

    public IParseController getParseController() {
        return fParseController;
    }

    public Set<IRefactoringContributor> getRefactoringContributors() {
        return fRefactoringContributors;
    }

    public IReferenceResolver getResolver() {
        return fResolver;
    }

    public IToggleBreakpointsHandler getToggleBreakpointsHandler() {
        return fToggleBreakpointsHandler;
    }

    public ITokenColorer getTokenColorer() {
        return fTokenColorer;
    }

    public IEntityNameLocator getEntityNameLocator() {
        return fEntityNameLocator;
    }
    
    public IEditorInputResolver getEditorInputResolver() {
        return fEditorInputResolver;
    }

    public IQuickFixAssistant getQuickFixAssistant() {
        return fQuickFixAssistant;
    }

    private static HashMap<IEditorPart, LanguageServiceManager> editorServiceMap = new HashMap<IEditorPart, LanguageServiceManager>();

    static {
        // Clean up editor references in the editorServiceMap when that editor gets closed.
        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().addPartListener(new IPartListener() {
            public void partOpened(IWorkbenchPart part) { }
            public void partDeactivated(IWorkbenchPart part) { }
            public void partBroughtToTop(IWorkbenchPart part) { }
            public void partActivated(IWorkbenchPart part) { }
            
            public void partClosed(IWorkbenchPart part) {
                if (part instanceof UniversalEditor) {
                    editorServiceMap.remove(part);
                }
            }
        });
    }

    public static void saveMyServiceManager(IEditorPart part, LanguageServiceManager manager) {
    	clearDeadEntries();
    	editorServiceMap.put(part, manager);
    }

    public static LanguageServiceManager getMyServiceManager(IEditorPart part) {
    	clearDeadEntries();
    	return editorServiceMap.get(part);
    }

    private static void clearDeadEntries() {
        List<IEditorPart> deadEditors = new ArrayList<IEditorPart>();
        for (IEditorPart edPart: editorServiceMap.keySet()) {
            IEditorSite edSite = edPart.getEditorSite();
            IWorkbenchPart wbPart = edSite.getPart();
            if (wbPart == null) {
                deadEditors.add(edPart);
            } else {
            }
        }
        for (IEditorPart edPart: deadEditors) {
            editorServiceMap.remove(edPart);
        }
    }
}
