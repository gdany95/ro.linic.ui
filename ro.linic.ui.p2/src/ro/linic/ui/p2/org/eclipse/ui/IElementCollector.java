package ro.linic.ui.p2.org.eclipse.ui;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * IElementCollector is a type that allows for the incremental update of a
 * collection of objects. This used for updating trees incrementally with a
 * progress monitor so that the update can be reported.
 *
 * @see org.eclipse.ui.progress.IDeferredWorkbenchAdapter
 * @see org.eclipse.ui.progress.DeferredTreeContentManager
 * @since 3.0
 */
public interface IElementCollector {
	/**
	 * Add the element to the IElementCollector. Send any progress information to
	 * monitor.
	 *
	 * @param element The element being added
	 * @param monitor The monitor to send updates to.
	 */
	void add(Object element, IProgressMonitor monitor);

	/**
	 * Add the elements to the IElementCollector. Send any progress information to
	 * monitor.
	 *
	 * @param elements The elements being added
	 * @param monitor  The monitor to send updates to.
	 */
	void add(Object[] elements, IProgressMonitor monitor);

	/**
	 * The element collection is done. Clean up any temporary state.
	 *
	 */
	void done();
}
