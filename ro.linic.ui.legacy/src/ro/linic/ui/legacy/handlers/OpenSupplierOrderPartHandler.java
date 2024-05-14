package ro.linic.ui.legacy.handlers;

import jakarta.annotation.PostConstruct;

import org.eclipse.e4.core.di.annotations.CanExecute;
import org.eclipse.e4.core.di.annotations.Execute;
import org.eclipse.e4.ui.model.application.MApplication;
import org.eclipse.e4.ui.model.application.ui.menu.impl.HandledToolItemImpl;
import org.eclipse.e4.ui.workbench.modeling.EModelService;
import org.eclipse.e4.ui.workbench.modeling.EPartService;
import org.eclipse.e4.ui.workbench.modeling.EPartService.PartState;

import ro.colibri.security.Permissions;
import ro.linic.ui.legacy.parts.SupplierOrderPart;
import ro.linic.ui.legacy.session.ClientSession;

public class OpenSupplierOrderPartHandler
{
	private HandledToolItemImpl toolItem;
	
	@PostConstruct
	public void postConstruct(final MApplication application, final EModelService modelService)
	{
		toolItem = (HandledToolItemImpl) modelService.find("linic_gest_client.handledtoolitem.trimbar.comenzi_furnizori", 
				application.getToolBarContributions().iterator().next());
	}
	
	@Execute
	public void execute(final EPartService partService)
	{
		partService.showPart(SupplierOrderPart.PART_ID, PartState.ACTIVATE);
	}
	
	@CanExecute
	boolean canExecute(final EPartService partService)
	{
		final boolean canExecute = ClientSession.instance().hasPermission(Permissions.COMANDA_MARFA);
		toolItem.setVisible(canExecute);
		return canExecute;
	}
}
