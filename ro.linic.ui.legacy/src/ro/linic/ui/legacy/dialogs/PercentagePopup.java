package ro.linic.ui.legacy.dialogs;

import static ro.colibri.util.NumberUtils.parse;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.eclipse.jface.dialogs.PopupDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

import ro.linic.ui.legacy.session.UIUtils;

public class PercentagePopup extends PopupDialog
{
	private Point initialLocation;
	
	private Supplier<BigDecimal> baseSupplier;
	private Consumer<BigDecimal> resultConsumer;
	private BigDecimal percentageAdder;
	
	private Text percentage;

	public PercentagePopup(final Shell shell, final Point initialLocation, final Supplier<BigDecimal> baseSupplier,
			final Consumer<BigDecimal> resultConsumer, final BigDecimal percentageAdder)
	{
		super(shell, SWT.ON_TOP | SWT.TOOL, true, false, false, false, false, null, null);
		this.initialLocation = initialLocation;
		this.baseSupplier = baseSupplier;
		this.resultConsumer = resultConsumer;
		this.percentageAdder = percentageAdder == null ? BigDecimal.ZERO : percentageAdder;
	}
	
	@Override
	protected Control createDialogArea(final Composite parent)
	{
		final Composite container = (Composite) super.createDialogArea(parent);
		percentage = new Text(container, SWT.SINGLE);
		UIUtils.setFont(percentage);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(percentage);
		
		percentage.addKeyListener(new KeyAdapter()
		{
			@Override public void keyPressed(final KeyEvent e)
			{
				if (e.keyCode == SWT.CR || e.keyCode == SWT.KEYPAD_CR)
				{
					if (baseSupplier.get() != null)
						resultConsumer.accept(parse(percentage.getText())
								.divide(new BigDecimal(100))
								.add(percentageAdder)
								.multiply(baseSupplier.get())
								.setScale(2, RoundingMode.HALF_EVEN));
					close();
				}
			}
		});
		
		return container;
	}
	
	@Override
	protected Point getDefaultLocation(final Point initialSize)
	{
		if (this.initialLocation != null)
			return initialLocation;
		
		return super.getDefaultLocation(initialSize);
	}
}
