package ro.linic.ui.p2.internal.ui.model;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.URIUtil;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.eclipse.equinox.internal.p2.metadata.repository.MetadataRepositoryManager;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.IQueryable;
import org.eclipse.equinox.p2.repository.IRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;

import ro.linic.ui.p2.internal.ui.ProvUI;
import ro.linic.ui.p2.internal.ui.ProvUIImages;
import ro.linic.ui.p2.internal.ui.ProvUIMessages;
import ro.linic.ui.p2.internal.ui.QueryProvider;
import ro.linic.ui.p2.internal.ui.query.IUViewQueryContext;
import ro.linic.ui.p2.ui.Policy;
import ro.linic.ui.p2.ui.ProvisioningUI;

/**
 * Element wrapper class for a metadata repository that gets its contents in a
 * deferred manner. A metadata repository can be the root (input) of a viewer,
 * when the view is filtered by repo, or a child of an input, when the view is
 * showing many repos.
 *
 * @since 3.4
 */
public class MetadataRepositoryElement extends RootElement implements IRepositoryElement<IInstallableUnit> {

	URI location;
	boolean isEnabled;
	String name;
	Object[] cache;

	public MetadataRepositoryElement(final IEclipseContext ctx, final Object parent, final URI location, final boolean isEnabled) {
		this(ctx, parent, null, null, location, isEnabled);
	}

	public MetadataRepositoryElement(final IEclipseContext ctx, final IUViewQueryContext queryContext, final ProvisioningUI ui, final URI location,
			final boolean isEnabled) {
		this(ctx, null, queryContext, ui, location, isEnabled);
	}

	public MetadataRepositoryElement(final IEclipseContext ctx, final Object parent, final IUViewQueryContext queryContext, final ProvisioningUI ui, final URI location,
			final boolean isEnabled) {
		super(ctx, parent, queryContext, ui);
		this.location = location;
		this.isEnabled = isEnabled;
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(final Class<T> adapter) {
		if (adapter == IMetadataRepository.class)
			return (T) getQueryable();
		if (adapter == IRepository.class)
			return (T) getQueryable();
		return super.getAdapter(adapter);
	}

	@Override
	protected Object[] fetchChildren(final Object o, final IProgressMonitor monitor) {
		if (cache != null)
			return cache;

		final SubMonitor sub = SubMonitor.convert(monitor, 200);
		// Ensure the repository is loaded using the monitor, so we respond to
		// cancelation.
		// Otherwise, a non-loaded repository could be loaded in the query provider
		// without a monitor.
		// If the load fails, return an explanation element.
		try {
			getMetadataRepository(sub.newChild(100));
			// only invoke super if we successfully loaded the repository
			cache = super.fetchChildren(o, sub.newChild(100));
		} catch (final ProvisionException e) {
			getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
			// TODO see https://bugs.eclipse.org/bugs/show_bug.cgi?id=276784
			cache = new Object[] { new EmptyElementExplanation(ctx, this, IStatus.ERROR, e.getLocalizedMessage(), "") }; //$NON-NLS-1$
		}
		return cache;
	}

	@Override
	protected String getImageId(final Object obj) {
		return ProvUIImages.IMG_METADATA_REPOSITORY;
	}

	@Override
	protected int getDefaultQueryType() {
		return QueryProvider.AVAILABLE_IUS;
	}

	@Override
	public String getLabel(final Object o) {
		final String n = getName();
		if (n != null && n.length() > 0) {
			return n;
		}
		return URIUtil.toUnencodedString(getLocation());
	}

	/*
	 * overridden to lazily fetch repository
	 */
	@Override
	public IQueryable<?> getQueryable() {
		if (queryable == null)
			queryable = getRepository(new NullProgressMonitor());
		return queryable;
	}

	@Override
	public IMetadataRepository getRepository(final IProgressMonitor monitor) {
		try {
			return getMetadataRepository(monitor);
		} catch (final ProvisionException e) {
			getProvisioningUI().getRepositoryTracker().reportLoadFailure(location, e);
		} catch (final OperationCanceledException e) {
			// nothing to report
		}
		return null;
	}

	private IMetadataRepository getMetadataRepository(final IProgressMonitor monitor) throws ProvisionException {
		if (queryable == null) {
			queryable = getProvisioningUI().loadMetadataRepository(location, false, monitor);
		}
		return (IMetadataRepository) queryable;

	}

	/*
	 * overridden to check whether url is specified rather than loading the repo via
	 * getQueryable()
	 */
	@Override
	public boolean knowsQueryable() {
		return location != null;
	}

	@Override
	public URI getLocation() {
		return location;
	}

	@Override
	public String getName() {
		if (name == null) {
			name = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NICKNAME);
			if (name == null)
				name = getMetadataRepositoryManager().getRepositoryProperty(location, IRepository.PROP_NAME);
			if (name == null)
				name = ""; //$NON-NLS-1$
		}
		return name;
	}

	public void setNickname(final String name) {
		this.name = name;
	}

	public void setLocation(final URI location) {
		this.location = location;
		setQueryable(null);
	}

	@Override
	public String getDescription() {
		if (getProvisioningUI().getRepositoryTracker().hasNotFoundStatusBeenReported(location))
			return ProvUIMessages.RepositoryElement_NotFound;
		final String description = getMetadataRepositoryManager().getRepositoryProperty(location,
				IRepository.PROP_DESCRIPTION);
		if (description == null)
			return ""; //$NON-NLS-1$
		return description;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}

	@Override
	public void setEnabled(final boolean enabled) {
		isEnabled = enabled;
	}

	/*
	 * Overridden to check whether a repository instance has already been loaded.
	 * This is necessary to prevent background loading of an already loaded
	 * repository by the DeferredTreeContentManager, which will add redundant
	 * children to the viewer. see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=229069 see
	 * https://bugs.eclipse.org/bugs/show_bug.cgi?id=226343
	 *
	 */
	@Override
	public boolean hasQueryable() {
		if (queryable != null)
			return true;
		if (location == null)
			return false;
		final IMetadataRepositoryManager manager = getMetadataRepositoryManager();
		if (manager == null || !(manager instanceof MetadataRepositoryManager))
			return false;
		final IMetadataRepository repo = ((MetadataRepositoryManager) manager).getRepository(location);
		if (repo == null)
			return false;
		queryable = repo;
		return true;
	}

	@Override
	public Policy getPolicy() {
		final Object parent = getParent(this);
		if (parent == null)
			return super.getPolicy();
		if (parent instanceof QueriedElement)
			return ((QueriedElement) parent).getPolicy();
		return getProvisioningUI().getPolicy();
	}

	@Override
	public String toString() {
		final StringBuilder result = new StringBuilder();
		result.append("Metadata Repository Element - "); //$NON-NLS-1$
		result.append(URIUtil.toUnencodedString(location));
		if (hasQueryable())
			result.append(" (loaded)"); //$NON-NLS-1$
		else
			result.append(" (not loaded)"); //$NON-NLS-1$
		return result.toString();
	}

	IMetadataRepositoryManager getMetadataRepositoryManager() {
		return ProvUI.getMetadataRepositoryManager(getProvisioningUI().getSession());
	}

	/**
	 * MetadataRepositoryElements can sometimes be roots and sometimes children.
	 * When they are roots the should have a ui set directly. As children they
	 * should defer to the parent to get the ui.
	 */
	@Override
	public ProvisioningUI getProvisioningUI() {
		final ProvisioningUI ui = super.getProvisioningUI();
		if (ui != null)
			return ui;
		final Object parent = getParent(this);
		if (parent != null && parent instanceof QueriedElement)
			return ((QueriedElement) parent).getProvisioningUI();
		// if all else fails get the global UI. This should not really happen but
		// we need to account for some possible historical cases.
		return ProvisioningUI.getDefaultUI(ctx);
	}
}
