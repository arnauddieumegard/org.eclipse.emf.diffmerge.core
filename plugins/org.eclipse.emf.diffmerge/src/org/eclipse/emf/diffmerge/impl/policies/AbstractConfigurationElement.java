/**
 * <copyright>
 * 
 * Copyright (c) 2017-2018 Thales Global Services S.A.S.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Thales Global Services S.A.S. - initial API and implementation
 * 
 * </copyright>
 */
package org.eclipse.emf.diffmerge.impl.policies;

import org.eclipse.emf.diffmerge.api.config.IConfigurationElement;


/**
 * A base implementation of IConfigurationElement.
 * @author Olivier Constant
 */
public abstract class AbstractConfigurationElement implements IConfigurationElement {
  
  /** A non-null label for the element */
  private final String _label;
  
  /** An optional description for the element */
  private final String _description;
  
  
  /**
   * Constructor
   * @param label_p a non-null label
   * @param description_p a potentially null description
   */
  protected AbstractConfigurationElement(String label_p, String description_p) {
    _label = label_p;
    _description = description_p;
  }
  
  /**
   * Return the description of this criterion, if any
   * @return a potentially null object
   */
  public String getDescription() {
    return _description;
  }
  
  /**
   * Return the label of this criterion
   * @return a non-null object
   */
  public String getLabel() {
    return _label;
  }
  
}