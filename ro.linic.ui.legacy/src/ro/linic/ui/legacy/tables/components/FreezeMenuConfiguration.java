package ro.linic.ui.legacy.tables.components;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.config.AbstractUiBindingConfiguration;
import org.eclipse.nebula.widgets.nattable.grid.GridRegion;
import org.eclipse.nebula.widgets.nattable.ui.binding.UiBindingRegistry;
import org.eclipse.nebula.widgets.nattable.ui.matcher.MouseEventMatcher;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuAction;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Menu;

//[1] IConfiguration for registering a UI binding to open a menu
public class FreezeMenuConfiguration extends AbstractUiBindingConfiguration
{
	private final Menu menu;

	public FreezeMenuConfiguration(final NatTable natTable)
	{
        // [2] create the menu using the PopupMenuBuilder
        this.menu = new PopupMenuBuilder(natTable)
        			.withFreezeColumnMenuItem()
                    .withFreezePositionMenuItem(false)
                    .withUnfreezeMenuItem()
                    .withAutoResizeSelectedColumnsMenuItem()
                    .withAutoResizeSelectedRowsMenuItem()
                    .build();
    }

	@Override
	public void configureUiBindings(final UiBindingRegistry uiBindingRegistry)
	{
		// [3] bind the PopupMenuAction to a right click
		// using GridRegion.COLUMN_HEADER instead of null would
		// for example open the menu only on performing a right
		// click on the column header instead of any region
		uiBindingRegistry.registerMouseDownBinding(
				new MouseEventMatcher(SWT.NONE, GridRegion.BODY, MouseEventMatcher.RIGHT_BUTTON),
				new PopupMenuAction(this.menu));
	}
}
