package ro.linic.ui.legacy.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;

/** A sample preference page to show how it works */
public class RegisterZPage extends FieldEditorPreferencePage {

	public RegisterZPage() {
		super(GRID);
	}

	@Override
	protected void createFieldEditors() {
		addField(new BooleanFieldEditor(PreferenceKey.RAPORT_Z_AND_D_KEY, "Scoate si raportul D la inchiderea zilei", getFieldEditorParent()));
		addField(new ComboFieldEditor(PreferenceKey.REGISTER_Z_DIALOG_KEY, "Ce fereastra se foloseste la inregistrarea Z", 
				new String[][]{{"Standard", PreferenceKey.REGISTER_Z_DIALOG_STANDARD_VALUE},
					{"Cafe", PreferenceKey.REGISTER_Z_DIALOG_CAFE_VALUE}},
				getFieldEditorParent()));
	}
}