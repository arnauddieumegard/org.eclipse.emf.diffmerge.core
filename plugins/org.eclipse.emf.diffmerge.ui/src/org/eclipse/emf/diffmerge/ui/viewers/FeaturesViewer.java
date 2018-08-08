/**
 * <copyright>
 * 
 * Copyright (c) 2010-2018 Thales Global Services S.A.S.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 *    Stephane Bouchet (Intel Corporation) - Bug #489142 : use the delegate label provider to display text
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.ui.viewers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.emf.diffmerge.api.IMatch;
import org.eclipse.emf.diffmerge.api.Role;
import org.eclipse.emf.diffmerge.diffdata.EMatch;
import org.eclipse.emf.diffmerge.ui.EMFDiffMergeUIPlugin;
import org.eclipse.emf.diffmerge.ui.diffuidata.MatchAndFeature;
import org.eclipse.emf.diffmerge.ui.diffuidata.impl.MatchAndFeatureImpl;
import org.eclipse.emf.diffmerge.ui.util.AbstractDiffDelegatingLabelProvider;
import org.eclipse.emf.diffmerge.ui.util.UIUtil;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EReference;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;


/**
 * A viewer which provides a representation of the features of a match.
 * Input: FeaturesViewer.FeaturesInput ; Elements: MatchAndFeature.
 * @author Olivier Constant
 */
public class FeaturesViewer extends TableViewer implements IDifferenceRelatedViewer {
  
  /**
   * A simple structure for defining inputs for this viewer.
   */
  public static class FeaturesInput {
    /** The non-null comparison context */
    private final EMFDiffNode _context;
    /** The non-null specific part */
    private final IMatch _match;
    /**
     * Constructor
     * @param context_p a non-null object
     * @param match_p a non-null object
     */
    public FeaturesInput(EMFDiffNode context_p, IMatch match_p) {
      _context = context_p;
      _match = match_p;
    }
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object object_p) {
      boolean result = false;
      if (object_p instanceof FeaturesInput) {
        FeaturesInput peer = (FeaturesInput)object_p;
        result = _context == peer.getContext() &&
          _match.equals(peer.getMatch());
      }
      return result;
    }
    /**
     * Return the comparison context
     * @return a non-null object
     */
    public EMFDiffNode getContext() {
      return _context;
    }
    /**
     * Return the match
     * @return a non-null object
     */
    public IMatch getMatch() {
      return _match;
    }
    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return _context.hashCode() + _match.hashCode();
    }
  }
  
  
  /** Whether all features must be shown, including those with no difference */
  private boolean _showAllFeatures;
  
  /** Whether a technical, more precise but less user-friendly representation must be used */
  private boolean _useTechnicalRepresentation;
  
  
  /**
   * Constructor
   * @param parent_p a non-null composite
   */
  public FeaturesViewer(Composite parent_p) {
    this(parent_p, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
  }
  
  /**
   * Constructor
   * @param parent_p a non-null composite
   * @param style_p a style for the tree
   */
  public FeaturesViewer(Composite parent_p, int style_p) {
    super(parent_p, style_p);
    setContentProvider(new ContentProvider());
    setLabelProvider(new LabelProvider());
    _showAllFeatures = false;
    _useTechnicalRepresentation = false;
    getControl().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    setComparator(new ViewerComparator());
  }
  
  /**
   * Return the first 'match and feature' to show for the given input, if any
   * @param input_p a potentially null input object
   * @return the first match and feature to show, or null if none
   */
  public MatchAndFeature getFirstIn(FeaturesInput input_p) {
    MatchAndFeature result = null;
    if (input_p != null) {
      Object[] elements = getSortedChildren(input_p);
      if (elements != null && elements.length > 0) {
        Object firstElement = elements[0];
        if (firstElement instanceof MatchAndFeature)
          result = (MatchAndFeature)firstElement;
      }
    }
    return result;
  }
  
  /**
   * @see org.eclipse.jface.viewers.ContentViewer#getInput()
   */
  @Override
  public FeaturesInput getInput() {
    return (FeaturesInput)super.getInput();
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.IDifferenceRelatedViewer#isDifferenceAgnostic()
   */
  public boolean isDifferenceAgnostic() {
    return _showAllFeatures;
  }
  
  /**
   * Return whether the given object represents the virtual ownership feature
   * @param object_p a potentially null object
   */
  protected boolean isOwnershipFeature(Object object_p) {
    return EMFDiffMergeUIPlugin.getDefault().getOwnershipFeature().equals(object_p);
  }
  
  /**
   * Return whether a technical, more precise but less user-friendly representation is being used
   */
  public boolean isTechnical() {
    return _useTechnicalRepresentation;
  }
  
  /**
   * @see org.eclipse.emf.diffmerge.ui.viewers.IDifferenceRelatedViewer#setDifferenceAgnostic(boolean)
   */
  public void setDifferenceAgnostic(boolean agnostic_p) {
    if (agnostic_p != isDifferenceAgnostic()) {
      _showAllFeatures = agnostic_p;
      refresh(false);
    }
  }
  
  /**
   * Set whether a technical, more precise but less user-friendly representation must be used
   */
  public void setTechnical(boolean technical_p) {
    if (technical_p != isTechnical()) {
      _useTechnicalRepresentation = technical_p;
      refresh(true);
    }
  }
  
  
  /**
   * The content provider for this viewer.
   */
  protected class ContentProvider implements IStructuredContentProvider {
    /**
     * Return a list of all the relevant features for the given match
     * @param match_p a non-null match
     * @return a non-null, potentially empty, modifiable list
     */
    private List<EStructuralFeature> getAllFeatures(IMatch match_p) {
      Role drivingRole = getInput().getContext().getDrivingRole();
      EObject element = match_p.get(drivingRole);
      if (element == null)
        element = match_p.get(drivingRole.opposite());
      assert element != null; // An IMatch may not have null elements for both roles
      EClass eClass = element.eClass();
      List<EStructuralFeature> result = new ArrayList<EStructuralFeature>();
      result.addAll(eClass.getEAllAttributes());
      for (EReference ref : eClass.getEAllReferences()) {
        if (qualifies(ref) || match_p.getOrderDifference(ref, drivingRole) != null)
          result.add(ref);
      }
      return result;
    }
    /**
     * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
     */
    public Object[] getElements(Object inputElement_p) {
      EMFDiffNode context = getInput().getContext();
      Role drivingRole = context.getDrivingRole();
      IMatch match = ((FeaturesInput)inputElement_p).getMatch();
      List<EStructuralFeature> features;
      if (isDifferenceAgnostic())
        features = getAllFeatures(match);
      else {
        features = new ArrayList<EStructuralFeature>(match.getAttributesWithDifferences());
        for (EReference ref : match.getReferencesWithDifferences()) {
          if (!context.isContainment(ref) || match.getOrderDifference(ref, drivingRole) != null)
            features.add(ref);
        }
      }
      if (getInput().getContext().getCategoryManager().representAsMove(match))
        features.add(EMFDiffMergeUIPlugin.getDefault().getOwnershipFeature());
      List<MatchAndFeature> result = new ArrayList<MatchAndFeature>();
      for (EStructuralFeature feature : features) {
        MatchAndFeature maf = new MatchAndFeatureImpl((EMatch)match, feature);
        result.add(maf);
      }
      return result.toArray();
    }
    /**
     * @see org.eclipse.jface.viewers.IContentProvider#dispose()
     */
    public void dispose() {
      // Nothing needed
    }
    /**
     * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
     */
    public void inputChanged(Viewer viewer_p, Object oldInput_p, Object newInput_p) {
      // Nothing needed
    }
    /**
     * Return whether the given reference may be shown
     * @param reference_p a non-null reference
     */
    private boolean qualifies(EReference reference_p) {
      return isOwnershipFeature(reference_p) ||
        !getInput().getContext().isContainment(reference_p) && !reference_p.isContainer();
    }
  }
  
  
  /**
   * The label provider for this viewer
   */
  protected class LabelProvider extends AbstractDiffDelegatingLabelProvider {
    /**
     * @see org.eclipse.emf.diffmerge.ui.util.AbstractDiffDelegatingLabelProvider#getDiffNode()
     */
    @Override
    protected EMFDiffNode getDiffNode() {
      return getInput() == null? null: getInput().getContext();
    }
    /**
     * @see org.eclipse.emf.diffmerge.ui.util.AbstractDiffDelegatingLabelProvider#getSide()
     */
    @Override
    protected Role getSide() {
      return null;
    }
    /**
     * @see org.eclipse.emf.diffmerge.ui.util.AbstractDiffDelegatingLabelProvider#getUndecoratedText(java.lang.Object)
     */
    @Override
    public String getUndecoratedText(Object element_p) {
      String result;
      if (element_p instanceof EStructuralFeature && !isTechnical()) {
        result = UIUtil.getFormattedFeatureText((EStructuralFeature)element_p);
      } else {
        result = super.getUndecoratedText(element_p); 
      }
      return result;
    }
  }
  
}
