/*
 * @(#)TreeModel.java	1.27 10/03/23
 *
 * Copyright (c) 2006, Oracle and/or its affiliates. All rights reserved.
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package org.jaudiotagger.utils.tree;



/**
 * The model used by <code>JTree</code>.
 * <p>
 * <code>JTree</code> and its related classes make extensive use of
 * <code>TreePath</code>s for indentifying nodes in the <code>TreeModel</code>.
 * If a <code>TreeModel</code> returns the same object, as compared by
 * <code>equals</code>, at two different indices under the same parent
 * than the resulting <code>TreePath</code> objects will be considered equal
 * as well. Some implementations may assume that if two
 * <code>TreePath</code>s are equal, they identify the same node. If this
 * condition is not met, painting problems and other oddities may result.
 * In other words, if <code>getChild</code> for a given parent returns
 * the same Object (as determined by <code>equals</code>) problems may
 * result, and it is recommended you avoid doing this.
 * <p>
 * Similarly <code>JTree</code> and its related classes place
 * <code>TreePath</code>s in <code>Map</code>s.  As such if
 * a node is requested twice, the return values must be equal
 * (using the <code>equals</code> method) and have the same
 * <code>hashCode</code>.
 * <p>
 * For further information on tree models,
 * including an example of a custom implementation,
 * see <a
 href="http://java.sun.com/docs/books/tutorial/uiswing/components/tree.html">How to Use Trees</a>
 * in <em>The Java Tutorial.</em>
 * 
 * @see TreePath
 * 
 * @version 1.27 03/23/10
 * @author Rob Davis
 * @author Ray Ryan
 */
public interface TreeModel
{

    /**
     * Returns the root of the tree.  Returns <code>null</code>
     * only if the tree has no nodes.
     *
     * @return  the root of the tree
     */
    Object getRoot();


    /**
     * Returns the child of <code>parent</code> at index <code>index</code>
     * in the parent's
     * child array.  <code>parent</code> must be a node previously obtained
     * from this data source. This should not return <code>null</code>
     * if <code>index</code>
     * is a valid index for <code>parent</code> (that is <code>index >= 0 &&
     * index < getChildCount(parent</code>)).
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the child of <code>parent</code> at index <code>index</code>
     */
    Object getChild(Object parent, int index);


    /**
     * Returns the number of children of <code>parent</code>.
     * Returns 0 if the node
     * is a leaf or if it has no children.  <code>parent</code> must be a node
     * previously obtained from this data source.
     *
     * @param   parent  a node in the tree, obtained from this data source
     * @return  the number of children of the node <code>parent</code>
     */
    int getChildCount(Object parent);


    /**
     * Returns <code>true</code> if <code>node</code> is a leaf.
     * It is possible for this method to return <code>false</code>
     * even if <code>node</code> has no children.
     * A directory in a filesystem, for example,
     * may contain no files; the node representing
     * the directory is not a leaf, but it also has no children.
     *
     * @param   node  a node in the tree, obtained from this data source
     * @return  true if <code>node</code> is a leaf
     */
    boolean isLeaf(Object node);

    /**
      * Messaged when the user has altered the value for the item identified
      * by <code>path</code> to <code>newValue</code>. 
      * If <code>newValue</code> signifies a truly new value
      * the model should post a <code>treeNodesChanged</code> event.
      *
      * @param path path to the node that the user has altered
      * @param newValue the new value from the TreeCellEditor
      */
    void valueForPathChanged(TreePath path, Object newValue);

    /**
     * Returns the index of child in parent.  If either <code>parent</code>
     * or <code>child</code> is <code>null</code>, returns -1.
     * If either <code>parent</code> or <code>child</code> don't
     * belong to this tree model, returns -1.
     *
     * @param parent a node in the tree, obtained from this data source
     * @param child the node we are interested in
     * @return the index of the child in the parent, or -1 if either
     *    <code>child</code> or <code>parent</code> are <code>null</code>
     *    or don't belong to this tree model
     */
    int getIndexOfChild(Object parent, Object child);

//
//  Change Events
//

    /**
     * Adds a listener for the <code>TreeModelEvent</code>
     * posted after the tree changes.
     *
     * @param   l       the listener to add
     * @see     #removeTreeModelListener
     */
    void addTreeModelListener(TreeModelListener l);

    /**
     * Removes a listener previously added with
     * <code>addTreeModelListener</code>.
     *
     * @see     #addTreeModelListener
     * @param   l       the listener to remove
     */  
    void removeTreeModelListener(TreeModelListener l);

}
