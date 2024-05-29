package ro.linic.ui.base.services.nattable;

import java.beans.PropertyChangeListener;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;

import org.eclipse.nebula.widgets.nattable.NatTable;
import org.eclipse.nebula.widgets.nattable.blink.BlinkConfigAttributes;
import org.eclipse.nebula.widgets.nattable.blink.BlinkingCellResolver;
import org.eclipse.nebula.widgets.nattable.blink.IBlinkingCellResolver;
import org.eclipse.nebula.widgets.nattable.columnChooser.command.DisplayColumnChooserCommandHandler;
import org.eclipse.nebula.widgets.nattable.config.CellConfigAttributes;
import org.eclipse.nebula.widgets.nattable.config.ConfigRegistry;
import org.eclipse.nebula.widgets.nattable.config.DefaultNatTableStyleConfiguration;
import org.eclipse.nebula.widgets.nattable.config.IConfiguration;
import org.eclipse.nebula.widgets.nattable.data.IRowIdAccessor;
import org.eclipse.nebula.widgets.nattable.data.ListDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultCornerDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.data.DefaultSummaryRowHeaderDataProvider;
import org.eclipse.nebula.widgets.nattable.grid.layer.CornerLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.DefaultRowHeaderDataLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.GridLayer;
import org.eclipse.nebula.widgets.nattable.grid.layer.RowHeaderLayer;
import org.eclipse.nebula.widgets.nattable.group.ColumnGroupModel;
import org.eclipse.nebula.widgets.nattable.layer.DataLayer;
import org.eclipse.nebula.widgets.nattable.layer.ILayer;
import org.eclipse.nebula.widgets.nattable.layer.cell.ColumnOverrideLabelAccumulator;
import org.eclipse.nebula.widgets.nattable.sort.config.SingleClickSortConfiguration;
import org.eclipse.nebula.widgets.nattable.style.CellStyleAttributes;
import org.eclipse.nebula.widgets.nattable.style.DisplayMode;
import org.eclipse.nebula.widgets.nattable.style.Style;
import org.eclipse.nebula.widgets.nattable.tooltip.NatTableContentTooltip;
import org.eclipse.nebula.widgets.nattable.ui.menu.HeaderMenuConfiguration;
import org.eclipse.nebula.widgets.nattable.ui.menu.PopupMenuBuilder;
import org.eclipse.nebula.widgets.nattable.util.GUIHelper;
import org.eclipse.swt.widgets.Composite;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.FilterList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.ObservableElementList;
import ca.odell.glazedlists.SortedList;
import ro.linic.ui.base.services.nattable.internal.BodyMenuConfiguration;
import ro.linic.ui.base.services.nattable.internal.FluentTableConfigurer;
import ro.linic.ui.base.services.nattable.internal.FullFeaturedBodyLayerStack;
import ro.linic.ui.base.services.nattable.internal.FullFeaturedColumnHeaderLayerStack;

public class FullFeaturedNatTable<T> {
	public static final String BODY_DATA_PROVIDER_CONFIG_KEY = "bodyDataProvider"; //$NON-NLS-1$
	public static final String CONFIG_REGISTRY_CONFIG_KEY = "configRegistry"; //$NON-NLS-1$
	
	private static final String BLINK_CONFIG_LABEL = "blinkConfigLabel";

	final private Class<T> entityClass;
	final private List<Column> columns;
	final private EventList<T> baseEventList;
	private PropertyChangeListener propertyChangeListener;
	private ListDataProvider<T> bodyDataProvider;
	private NatTable natTable;
	
	/**
	 * These are for clients that require some data, such as the bodyDataProvider 
	 * when configuring the extra layers. Register a known key here, such as 
	 * BODY_DATA_PROVIDER_CONFIG_KEY and the passed function will be called 
	 * when configuring NatTable with the required object passed(eg.: bodyDataProvider). 
	 * Client should then return the IConfiguration they want to apply to the nattable 
	 * or null to skip.<br>
	 * For invalid keys or empty keys null is passed to the function.
	 */
	final private Map<String, List<Function<Object, IConfiguration>>> dynamicConfigs;
	
	public FullFeaturedNatTable(final FluentTableConfigurer<T> configurer) {
		this.entityClass = configurer.getEntityClass();
		this.columns = configurer.getColumns();
		this.baseEventList = configurer.getSourceData();
		this.dynamicConfigs = configurer.getDynamicConfigs();
	}

	public void postConstruct(final Composite parent) {
		final ConfigRegistry configRegistry = new ConfigRegistry();
		final ColumnGroupModel columnGroupModel = new ColumnGroupModel();

		// Body
		ObservableElementList<T> observableElementList;
		FilterList<T> filterList;
		SortedList<T> sortedList;
		this.baseEventList.getReadWriteLock().readLock().lock();
		try {
			observableElementList = new ObservableElementList<>(
					this.baseEventList, GlazedLists.beanConnector(entityClass));
			filterList = new FilterList<>(observableElementList);
			sortedList = new SortedList<>(filterList, null);
		} finally {
			this.baseEventList.getReadWriteLock().readLock().unlock();
		}

		final FullFeaturedBodyLayerStack<T> bodyLayer = new FullFeaturedBodyLayerStack<>(
				sortedList, new IRowIdAccessor<T>() {
					@Override
					public Serializable getRowId(final T rowObject) {
						return rowObject.hashCode();
					}
				}, columns, configRegistry, columnGroupModel);

		this.bodyDataProvider = bodyLayer.getBodyDataProvider();
		this.propertyChangeListener = bodyLayer.getGlazedListEventsLayer();

		// blinking
		registerBlinkingConfigCells(configRegistry);

		// Column header
		final FullFeaturedColumnHeaderLayerStack<T> columnHeaderLayer = new FullFeaturedColumnHeaderLayerStack<>(
				sortedList, filterList, this.columns, bodyLayer, bodyLayer.getSelectionLayer(),
				columnGroupModel, configRegistry);

		// Row header
		final DefaultRowHeaderDataProvider rowHeaderDataProvider = new DefaultSummaryRowHeaderDataProvider(
				this.bodyDataProvider);
		final DefaultRowHeaderDataLayer rowHeaderDataLayer = new DefaultRowHeaderDataLayer(rowHeaderDataProvider);
		rowHeaderDataLayer.setDefaultColumnWidth(60);
		final ILayer rowHeaderLayer = new RowHeaderLayer(rowHeaderDataLayer, bodyLayer, bodyLayer.getSelectionLayer());

		// Corner
		final DefaultCornerDataProvider cornerDataProvider = new DefaultCornerDataProvider(
				columnHeaderLayer.getColumnHeaderDataProvider(), rowHeaderDataProvider);
		final DataLayer cornerDataLayer = new DataLayer(cornerDataProvider);
		final ILayer cornerLayer = new CornerLayer(cornerDataLayer, rowHeaderLayer, columnHeaderLayer);

		// Grid
		final GridLayer gridLayer = new GridLayer(bodyLayer, columnHeaderLayer, rowHeaderLayer, cornerLayer);

		this.natTable = new NatTable(parent, gridLayer, false);
		this.natTable.setConfigRegistry(configRegistry);
		this.natTable.addConfiguration(new DefaultNatTableStyleConfiguration());
		// Popup menu
		this.natTable.addConfiguration(new HeaderMenuConfiguration(this.natTable) {
			@Override
			protected PopupMenuBuilder createColumnHeaderMenu(final NatTable natTable) {
				return super.createColumnHeaderMenu(natTable).withColumnChooserMenuItem();
			}
		});
		this.natTable.addConfiguration(new BodyMenuConfiguration(this.natTable));
		this.natTable.addConfiguration(new SingleClickSortConfiguration());
		new NatTableContentTooltip(this.natTable);

		// Editing
		final ColumnOverrideLabelAccumulator columnLabelAccumulator = new ColumnOverrideLabelAccumulator(
				bodyLayer.getBodyDataLayer());
		bodyLayer.getBodyDataLayer().setConfigLabelAccumulator(columnLabelAccumulator);

		// Column chooser
		final DisplayColumnChooserCommandHandler columnChooserCommandHandler = new DisplayColumnChooserCommandHandler(
				bodyLayer.getSelectionLayer(), bodyLayer.getColumnHideShowLayer(),
				columnHeaderLayer.getColumnHeaderLayer(), columnHeaderLayer.getColumnHeaderDataLayer(),
				columnHeaderLayer.getColumnGroupHeaderLayer(), columnGroupModel);
		bodyLayer.registerCommandHandler(columnChooserCommandHandler);

		// Extra configuration
		for (final Entry<String, List<Function<Object, IConfiguration>>> entry : dynamicConfigs.entrySet()) {
			final Object argument = switch (entry.getKey()) {
			case BODY_DATA_PROVIDER_CONFIG_KEY -> bodyDataProvider;
			case CONFIG_REGISTRY_CONFIG_KEY -> configRegistry;
			default -> null;
			};
			
			entry.getValue().stream()
			.map(f -> f.apply(argument))
			.filter(Objects::nonNull)
			.forEach(this.natTable::addConfiguration);
		}
		
		this.natTable.configure();
	}

	private void registerBlinkingConfigCells(final ConfigRegistry configRegistry) {
		configRegistry.registerConfigAttribute(BlinkConfigAttributes.BLINK_RESOLVER, getBlinkResolver(),
				DisplayMode.NORMAL);

		// Styles
		final Style cellStyle = new Style();
		cellStyle.setAttributeValue(CellStyleAttributes.BACKGROUND_COLOR, GUIHelper.COLOR_YELLOW);
		configRegistry.registerConfigAttribute(CellConfigAttributes.CELL_STYLE, cellStyle, DisplayMode.NORMAL,
				BLINK_CONFIG_LABEL);
	}

	private IBlinkingCellResolver getBlinkResolver() {
		return new BlinkingCellResolver() {
			private final String[] configLabels = new String[1];
			@Override
			public String[] resolve(final Object oldValue, final Object newValue) {
				if (!Objects.equals(oldValue, newValue))
					this.configLabels[0] = BLINK_CONFIG_LABEL;
				return this.configLabels;
			}
		};
	}
	
	public NatTable natTable() {
		return natTable;
	}
}
