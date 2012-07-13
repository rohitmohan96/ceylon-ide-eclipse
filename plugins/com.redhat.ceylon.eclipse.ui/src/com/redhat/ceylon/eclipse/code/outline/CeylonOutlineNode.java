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

package com.redhat.ceylon.eclipse.code.outline;

import java.util.ArrayList;
import java.util.List;

import com.redhat.ceylon.compiler.typechecker.tree.Node;

public class CeylonOutlineNode {
	
    public static final int DEFAULT_CATEGORY = 0;
    public static final int ROOT_CATEGORY = -1;

    private List<CeylonOutlineNode> fChildren= new ArrayList<CeylonOutlineNode>();

    private CeylonOutlineNode fParent;

    private final Node fASTNode;

    private final int fCategory;

    public CeylonOutlineNode(Node astNode) {
        this(astNode, DEFAULT_CATEGORY);
    }

    public CeylonOutlineNode(Node astNode, int category) {
        fASTNode= astNode;
        fCategory= category;
    }

    public CeylonOutlineNode(Node astNode, CeylonOutlineNode parent) {
        this(astNode, parent, DEFAULT_CATEGORY);
    }

    public CeylonOutlineNode(Node astNode, CeylonOutlineNode parent, int category) {
        fASTNode= astNode;
        fParent= parent;
        fCategory= category;
    }

    public void addChild(CeylonOutlineNode child) {   
        fChildren.add(child);
    }

    public List<CeylonOutlineNode> getChildren() {
        return fChildren;
    }

    public CeylonOutlineNode getParent() {
        return fParent;
    }

    public Node getASTNode() {
        return fASTNode;
    }

    public int getCategory() {
        return fCategory;
    }

    public String toString() {
        StringBuilder sb= new StringBuilder();

        sb.append(fASTNode.toString());
        if (!fChildren.isEmpty()) {
            sb.append(" [");
            for(CeylonOutlineNode child: fChildren) {
                sb.append(child);
            }
            sb.append(" ]");
        }
        return sb.toString();
    }
}
