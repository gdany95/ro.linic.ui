package ro.linic.ui.p2.internal.ui.viewers;

import java.util.HashMap;
import java.util.HashSet;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.Viewer;

import ro.linic.ui.p2.internal.ui.model.MetadataRepositories;
import ro.linic.ui.p2.internal.ui.model.MetadataRepositoryElement;
import ro.linic.ui.p2.internal.ui.model.QueriedElement;
import ro.linic.ui.p2.internal.ui.model.RemoteQueriedElement;

/**
 * Content provider that retrieves children asynchronously where
 * possible using the IDeferredWorkbenchAdapter and provisioning
 * query mechanisms.
 *
 * @since 3.4
 *
 */
public class DeferredQueryContentProvider extends ProvElementContentProvider {

	DeferredQueryTreeContentManager manager;
	Object currentInput;
	HashMap<Object, Object> alreadyQueried = new HashMap<>();
	HashSet<Object> queryCompleted = new HashSet<>();
	AbstractTreeViewer viewer = null;
	ListenerList<IInputChangeListener> listeners = new ListenerList<>();
	boolean synchronous = false;
	IDeferredQueryTreeListener onFetchingActionListener;

	/**
	 *
	 */
	public DeferredQueryContentProvider() {
		// Default constructor
	}

	public void addListener(final IInputChangeListener listener) {
		listeners.add(listener);
	}

	public void removeListener(final IInputChangeListener listener) {
		listeners.remove(listener);
	}

	public void addOnFetchingActionListener(final IDeferredQueryTreeListener listener) {
		onFetchingActionListener = listener;
	}

	@Override
	public void inputChanged(final Viewer v, final Object oldInput, final Object newInput) {
		super.inputChanged(v, oldInput, newInput);

		if (manager != null)
			manager.cancel(oldInput);
		if (v instanceof AbstractTreeViewer) {
			manager = new DeferredQueryTreeContentManager((AbstractTreeViewer) v);
			manager.addListener(onFetchingActionListener);
			viewer = (AbstractTreeViewer) v;
			manager.addListener(new IDeferredQueryTreeListener() {

				@Override
				public void fetchingDeferredChildren(final Object parent, final Object placeholder) {
					alreadyQueried.put(parent, placeholder);
				}

				@Override
				public void finishedFetchingDeferredChildren(final Object parent, final Object placeholder) {
					queryCompleted.add(parent);
				}
			});
		} else
			viewer = null;
		alreadyQueried = new HashMap<>();
		queryCompleted = new HashSet<>();
		currentInput = newInput;
		for (final IInputChangeListener listener : listeners) {
			listener.inputChanged(v, oldInput, newInput);
		}
	}

	@Override
	public Object[] getElements(final Object input) {
		if (input instanceof QueriedElement) {
			return getChildren(input);
		}
		return super.getElements(input);
	}

	@Override
	public void dispose() {
		super.dispose();
		if (manager != null) {
			manager.cancel(currentInput);
		}
	}

	@Override
	public boolean hasChildren(final Object element) {
		if (manager != null) {
			if (manager.isDeferredAdapter(element))
				return manager.mayHaveChildren(element);
		}
		return super.hasChildren(element);
	}

	@Override
	public Object[] getChildren(final Object parent) {
		if (parent instanceof RemoteQueriedElement) {
			final RemoteQueriedElement element = (RemoteQueriedElement) parent;
			// We rely on the assumption that the queryable is the most expensive
			// thing to get vs. the query itself being expensive.
			// (loading a repo vs. querying a repo afterward)
			if (manager != null && !synchronous && (element instanceof MetadataRepositoryElement || element instanceof MetadataRepositories)) {
				if (element.getCachedChildren().length == 0)
					return manager.getChildren(element);
				return element.getChildren(element);
			}
			if (element.hasQueryable())
				return element.getChildren(element);
		}
		return super.getChildren(parent);
	}

	public void setSynchronous(final boolean sync) {
		if (sync == true && manager != null)
			manager.cancel(currentInput);
		this.synchronous = sync;
	}

	public boolean getSynchronous() {
		return synchronous;
	}
}
