package ro.linic.ui.legacy;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.operations.ProvisioningJob;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.operations.UpdateOperation;

/**
 * This class shows an example for checking for updates and performing the
 * update synchronously. It is up to the caller to run this in a job if a
 * background update check is desired. This is a reasonable way to run an
 * operation when user intervention is not required. Another approach is to
 * separately perform the resolution and provisioning steps, deciding whether to
 * perform these synchronously or in a job.
 * 
 * Any p2 operation can be run modally (synchronously), or the job can be
 * requested and scheduled by the caller.
 * 
 * @see UpdateOperation#resolveModal(IProgressMonitor)
 * @see UpdateOperation#getResolveJob(IProgressMonitor)
 * @see UpdateOperation#getProvisioningJob(IProgressMonitor)
 */
public class P2Util
{
	// Check for updates to this application and return a status.
	static IStatus checkForUpdates(final IProvisioningAgent agent, final IProgressMonitor monitor) throws OperationCanceledException
	{
		final ProvisioningSession session = new ProvisioningSession(agent);
		// the default update operation looks for updates to the currently
		// running profile, using the default profile root marker. To change
		// which installable units are being updated, use the more detailed
		// constructors.
		final UpdateOperation operation = new UpdateOperation(session);
		final SubMonitor sub = SubMonitor.convert(monitor, "Checking for application updates...", 200);
		IStatus status = operation.resolveModal(sub.newChild(100));
		if (status.getCode() == UpdateOperation.STATUS_NOTHING_TO_UPDATE)
		{
			return status;
		}
		if (status.getSeverity() == IStatus.CANCEL)
			throw new OperationCanceledException();

		if (status.getSeverity() != IStatus.ERROR)
		{
			// More complex status handling might include showing the user what updates
			// are available if there are multiples, differentiating patches vs. updates,
			// etc.
			// In this example, we simply update as suggested by the operation.
			final ProvisioningJob job = operation.getProvisioningJob(null);
			status = job.runModal(sub.newChild(100));
			if (status.getSeverity() == IStatus.CANCEL)
				throw new OperationCanceledException();
		}
		return status;
	}
}