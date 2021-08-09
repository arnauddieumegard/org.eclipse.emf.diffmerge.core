/*******************************************************************************
 * Copyright (c) 2015-2019 Intel Corporation and others.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Stephane Bouchet (Intel Corporation) - initial API and implementation
 *    Olivier Constant (Thales Global Services) - tight integration
 *    Stephane Bouchet (Intel Corporation) - bug #496397
 *******************************************************************************/
package org.eclipse.emf.diffmerge.connector.git.ext;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.egit.core.RepositoryCache;
import org.eclipse.egit.core.project.RepositoryMapping;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.diffmerge.connector.git.EMFDiffMergeGitConnectorPlugin;
import org.eclipse.emf.diffmerge.connector.git.Messages;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.team.core.history.IFileRevision;


/**
 * A helper for Git behaviors.
 */
@SuppressWarnings("restriction") // Specific EGit behaviors
public final class GitHelper {
  
  /** The singleton instance */
  public static final GitHelper INSTANCE = new GitHelper();
  
  /** Scheme for remote resources */
	private static final String REMOTE_SCHEME = "remote"; //$NON-NLS-1$

  /** Scheme for index revisions */
	private static final String INDEX_SCHEME = "index"; //$NON-NLS-1$
  
	/** Scheme for commit revisions */
	private static final String COMMIT_SCHEME = "commit"; //$NON-NLS-1$
	
  /** Post-scheme separator */
  private static final String SCHEME_SEP = ":/"; //$NON-NLS-1$
	
	
	/**
	 * Constructor
	 */
	protected GitHelper() {
	  // Nothing needed
	}
	
  /**
   * Return all supported Git schemes
   * @return a non-null set
   */
  public Collection<String> getGitSchemes() {
    return Arrays.asList(getSchemeCommit(), getSchemeIndex(), getSchemeRemote());
  }
  
  /**
   * Return the Git repository for the given resource path, if any
   * @param path_p a non-null path
   * @return a potentially null object
   */
  public Repository getRepository(IPath path_p) {
    // First look directly for connected projects using the repository mapping
    if (RepositoryMapping.getMapping(path_p) != null) {
      return RepositoryMapping.getMapping(path_p).getRepository();
    }
    // Then iterate over known repositories.
    for (Repository repo: RepositoryCache.getInstance().getAllRepositories()) {
      Path fullPath=new Path(repo.getWorkTree().toString().concat(path_p.makeAbsolute().toString()));
      if (fullPath.toFile().exists()) {
        return repo;
      }
    }
    EMFDiffMergeGitConnectorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,
        EMFDiffMergeGitConnectorPlugin.getDefault().getPluginId(),
        "Cannot find Git repository for resource at: " + path_p)); //$NON-NLS-1$
    return null;
  }
  
  /**
   * Return the Git repository for the given revision
   * @param revision_p a potentially null revision
   * @return a potentially null object
   */
  public Repository getRepository(IFileRevision revision_p) {
    if (revision_p != null) {
      IPath revisionPath = toPath(revision_p);
      if (revisionPath != null && !revisionPath.isAbsolute()) {
        return getRepository(revisionPath);
      }
      EMFDiffMergeGitConnectorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,
          EMFDiffMergeGitConnectorPlugin.getDefault().getPluginId(),
          String.format(Messages.GitHelper_NoRepoFound, revisionPath)));
    }
    return null;
  }
  
  /**
   * Return the scheme for commit revisions
   * @return a non-null string
   */
  public String getSchemeCommit() {
    return COMMIT_SCHEME;
  }
  
  /**
   * Return the scheme for index revisions
   * @return a non-null string
   */
  public String getSchemeIndex() {
    return INDEX_SCHEME;
  }
  
  /**
   * Return the scheme for remote revisions
   * @return a non-null string
   */
  public String getSchemeRemote() {
    return REMOTE_SCHEME;
  }
  
  /**
   * Return the separator that follows the scheme
   * @return a non-null string
   */
  public String getSchemeSeparator() {
    return SCHEME_SEP;
  }
  
  /**
   * Return whether there is a conflict on the given revision
   * @param revision_p a non-null revision
   */
  @SuppressWarnings("resource")
  public boolean isConflicting(IFileRevision revision_p)
      throws NoWorkTreeException, IOException {
    boolean result = false;
    Repository repo = getRepository(revision_p);
    if (repo != null)
      result = isConflicting(repo, revision_p);
    return result;
  }
  
  /**
   * Return whether there is a conflict on the given revision in the given repository
   * @param repository_p a non-null repository
   * @param revision_p a non-null file revision
   * @throws NoWorkTreeException if repository is bare
   * @throws IOException if a low-level reading problem occurred
   */
  public boolean isConflicting(Repository repository_p,
      IFileRevision revision_p) throws NoWorkTreeException, IOException {
    IPath revisionPath = toPath(revision_p);
    if (!revisionPath.isAbsolute()) {
      return isConflicting(repository_p, revisionPath.toString());
    }
    EMFDiffMergeGitConnectorPlugin.getDefault().getLog().log(new Status(IStatus.ERROR,
        EMFDiffMergeGitConnectorPlugin.getDefault().getPluginId(),
        String.format(Messages.GitHelper_NoConflictInfoFound, revisionPath)));
    return false;
  }
  
  /**
   * Return whether there is a conflict on the given path in the given repository
   * @param repository_p a non-null repository
   * @param path_p a non-null string
   * @throws NoWorkTreeException if repository is bare
   * @throws IOException if a low-level reading problem occurred
   */
  public boolean isConflicting(Repository repository_p, String path_p)
      throws NoWorkTreeException, IOException {
    return repository_p.readDirCache().getEntry(path_p).getStage() > 0;
  }
  
  /**
   * Return an Eclipse path for the given file revision
   * @param revision_p a non-null file revision
   * @return a potentially null path
   */
  public IPath toPath(IFileRevision revision_p) {
    IPath result = null;
    java.net.URI uri = revision_p.getURI();
    if (uri != null) {
      String uriString = URI.decode(uri.toString());
      result = new Path(uriString);
    }
    return result;
  }
  
}
