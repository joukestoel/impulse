package org.eclipse.uide.parser;

public class AstLocator implements IASTNodeLocator {
    public Object findNode(Object ast, int offset) {
        Ast root= (Ast) ast;

        if (root.token != null) {
            if (offset >= root.token.getStartOffset() && offset <= root.token.getEndOffset())
                return ast;
            else
                return null;
        }
        if (root.children == null)
            return null;
        for(int i= 0; i < root.children.size(); i++) {
            Ast maybe= (Ast) findNode(root.children.get(i), offset);
            if (maybe != null)
                return maybe;
        }
        return null;
    }

    public Object findNode(Object ast, int startOffset, int endOffset) {
        throw new UnsupportedOperationException();
    }
}
