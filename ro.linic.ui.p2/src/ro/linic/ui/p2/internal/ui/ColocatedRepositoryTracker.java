package ro.linic.ui.p2.internal.ui;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.ui.di.UISynchronize;
import org.eclipse.e4.ui.workbench.swt.DisplayUISynchronize;
import org.eclipse.equinox.internal.provisional.p2.repository.RepositoryEvent;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.RepositoryTracker;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.IRepositoryManager;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import ro.linic.ui.p2.internal.ui.dialogs.RepositoryNameAndLocationDialog;
import ro.linic.ui.p2.internal.ui.statushandlers.StatusManager;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Provides a repository tracker that interprets URLs as colocated artifact and
 * metadata repositories.
 *
 * @since 2.0
 */

public class ColocatedRepositoryTracker extends RepositoryTracker {

	ProvisioningUI ui;
	String parsedNickname;
	URI parsedLocation;
	private UISynchronize sync;

	public ColocatedRepositoryTracker(final ProvisioningUI ui) {
		this.ui = ui;
		sync = new DisplayUISynchronize(Display.getDefault());
		setArtifactRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
		setMetadataRepositoryFlags(IRepositoryManager.REPOSITORIES_NON_SYSTEM);
	}

	@Override
	public URI[] getKnownRepositories(final ProvisioningSession session) {
		return getMetadataRepositoryManager().getKnownRepositories(getMetadataRepositoryFlags());
	}

	@Override
	public void addRepository(final URI repoLocation, final String nickname, final ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			getMetadataRepositoryManager().addRepository(repoLocation);
			getArtifactRepositoryManager().addRepository(repoLocation);
			getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_SYSTEM,
					Boolean.FALSE.toString());
			getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_SYSTEM,
					Boolean.FALSE.toString());
			if (nickname != null) {
				getMetadataRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);
				getArtifactRepositoryManager().setRepositoryProperty(repoLocation, IRepository.PROP_NICKNAME, nickname);

			}
		} finally {
			// We know that the UI only responds to metadata repo events so we cheat...
			ui.signalRepositoryOperationComplete(
					new RepositoryEvent(repoLocation, IRepository.TYPE_METADATA, RepositoryEvent.ADDED, true), true);
		}
	}

	@Override
	public void removeRepositories(final URI[] repoLocations, final ProvisioningSession session) {
		ui.signalRepositoryOperationStart();
		try {
			for (final URI repoLocation : repoLocations) {
				getMetadataRepositoryManager().removeRepository(repoLocation);
				getArtifactRepositoryManager().removeRepository(repoLocation);
			}
		} finally {
			ui.signalRepositoryOperationComplete(null, true);
		}
	}

	@Override
	public void refreshRepositories(final URI[] locations, final ProvisioningSession session, final IProgressMonitor monitor) {
		ui.signalRepositoryOperationStart();
		final SubMonitor mon = SubMonitor.convert(monitor, locations.length * 100);
		for (final URI location : locations) {
			try {
				getArtifactRepositoryManager().refreshRepository(location, mon.newChild(50));
				getMetadataRepositoryManager().refreshRepository(location, mon.newChild(50));
			} catch (final ProvisionException e) {
				// ignore problematic repositories when refreshing
			}
		}
		// We have no idea how many repos may have been added/removed as a result of
		// refreshing these, this one, so we do not use a specific repository event to
		// represent it.
		ui.signalRepositoryOperationComplete(null, true);
	}

	@Override
	public void reportLoadFailure(final URI location, final ProvisionException e) {
		final int code = e.getStatus().getCode();
		// If the user doesn't have a way to manage repositories, then don't report
		// failures.
		if (!ui.getPolicy().getRepositoriesVisible()) {
			super.reportLoadFailure(location, e);
			return;
		}

		// Special handling when the location is bad (not found, etc.) vs. a failure
		// associated with a known repo.
		if (code == ProvisionException.REPOSITORY_NOT_FOUND || code == ProvisionException.REPOSITORY_INVALID_LOCATION) {
			if (!hasNotFoundStatusBeenReported(location)) {
				addNotFound(location);
				sync.asyncExec(() -> {
					if (Display.getCurrent() == null)
						return;
					final Shell shell = ProvUI.getDefaultParentShell();
					final int result = MessageDialog.open(MessageDialog.QUESTION, shell,
							ProvUIMessages.ColocatedRepositoryTracker_SiteNotFoundTitle,
							NLS.bind(ProvUIMessages.ColocatedRepositoryTracker_PromptForSiteLocationEdit,
									URIUtil.toUnencodedString(location)),
							SWT.NONE, ProvUIMessages.ColocatedRepositoryTracker_SiteNotFound_EditButtonLabel,
							IDialogConstants.NO_LABEL);
					if (result == 0) {
						final RepositoryNameAndLocationDialog dialog = new RepositoryNameAndLocationDialog(shell, ui) {
							@Override
							protected String getInitialLocationText() {
								return URIUtil.toUnencodedString(location);
							}

							@Override
							protected String getInitialNameText() {
								final String nickname = getMetadataRepositoryManager().getRepositoryProperty(location,
										IRepository.PROP_NICKNAME);
								return nickname == null ? "" : nickname; //$NON-NLS-1$
							}
						};
						final int ret = dialog.open();
						if (ret == Window.OK) {
							final URI correctedLocation = dialog.getLocation();
							if (correctedLocation != null) {
								ui.signalRepositoryOperationStart();
								try {
									removeRepositories(new URI[] { location }, ui.getSession());
									addRepository(correctedLocation, dialog.getName(), ui.getSession());
								} finally {
									ui.signalRepositoryOperationComplete(null, true);
								}
							}
						}
					}
				});
			}
		} else {
			ProvUI.handleException(e, null, StatusManager.SHOW | StatusManager.LOG);
		}
	}
	
	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(ui.getSession());
	}

	IArtifactRepositoryManager getArtifactRepositoryManager() {
		return ProvUI.getArtifactRepositoryManager(ui.getSession());
	}

	/*
	 * Overridden to support "Name - Location" parsing
	 */
	@Override
	public URI locationFromString(final String locationString) {
		URI uri = super.locationFromString(locationString);
		if (uri != null)
			return uri;
		// Look for the "Name - Location" pattern
		// There could be a hyphen in the name or URI, so we have to visit all
		// combinations
		int start = 0;
		int index = 0;
		String locationSubset;
		final String pattern = ProvUIMessages.RepositorySelectionGroup_NameAndLocationSeparator;
		while (index >= 0) {
			index = locationString.indexOf(pattern, start);
			if (index >= 0) {
				start = index + pattern.length();
				locationSubset = locationString.substring(start);
				uri = super.locationFromString(locationSubset);
				if (uri != null) {
					parsedLocation = uri;
					parsedNickname = locationString.substring(0, index);
					return uri;
				}
			}
		}
		return null;
	}

	/*
	 * Used by the UI to get a name that might have been supplied when the location
	 * was originally parsed. see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=293068
	 */
	public String getParsedNickname(final URI location) {
		if (parsedNickname == null || parsedLocation == null)
			return null;
		if (location.toString().equals(parsedLocation.toString()))
			return parsedNickname;
		return null;
	}

	@Override
	protected boolean contains(final URI location, final ProvisioningSession session) {
		return ProvUI.getMetadataRepositoryManager(session).contains(location);
	}
}
