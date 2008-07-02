package org.eclipse.imp.help;

import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.imp.editor.LanguageServiceManager;
import org.eclipse.imp.parser.IParseController;
import org.eclipse.imp.services.IHelpService;
import org.eclipse.jface.text.IRegion;

public class IMPHelpContextProvider implements IContextProvider {
    private final IHelpService fHelpContextSrvc;
    private final IParseController fParseController;
    private final String fContextID;
    private final Object[] fSelected;
    private final IRegion fSelection;

    public IMPHelpContextProvider(LanguageServiceManager srvcMgr, String helpContextID, Object[] selected) {
        fHelpContextSrvc= srvcMgr.getContextHelp();
        fParseController= srvcMgr.getParseController();
        fContextID= helpContextID;
        fSelected= selected;
        fSelection= null;
    }

    public IMPHelpContextProvider(LanguageServiceManager srvcMgr, String helpContextID, IRegion selection) {
        fHelpContextSrvc= srvcMgr.getContextHelp();
        fParseController= srvcMgr.getParseController();
        fContextID= helpContextID;
        fSelected= null;
        fSelection= selection;
    }

    public IContext getContext(Object target) {
        IContext context= HelpSystem.getContext(fContextID);

        if (fHelpContextSrvc != null) {
            if (fSelected != null && fSelected.length > 0) {
                String result= buildHelpString(fSelected, fHelpContextSrvc, fParseController);
                context= new IMPHelpContext(context, result);
            } else if (fSelection != null) {
                context= new IMPHelpContext(context, buildHelpString(fSelection, fHelpContextSrvc, fParseController));
            }
        }
        return context;
    }

    public static String buildHelpString(Object[] entities, IHelpService helpService, IParseController parseController) {
        StringBuilder sb= new StringBuilder();
        for(int i= 0; i < entities.length; i++) {
            sb.append(helpService.getHelp(entities[i], parseController));
        }
        String result= sb.toString();
        return result;
    }

    public static String buildHelpString(IRegion selection, IHelpService helpService, IParseController parseController) {
        return helpService.getHelp(selection, parseController);
    }

    public int getContextChangeMask() {
        return SELECTION;
    }

    public String getSearchExpression(Object target) {
        return null;
    }
}
