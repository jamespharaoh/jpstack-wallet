package wbs.services.ticket.core.console;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.record.Record;
import wbs.platform.console.forms.FormFieldSet;


@PrototypeComponent(value = "staticFieldsProvider")
public
class StaticFieldsProvider
	implements FieldsProvider {

	FormFieldSet formFields;
	String mode;

	@Override
	public FormFieldSet getFields (Record<?> parent) {

		return formFields;

	}

	@Override
	public FieldsProvider setFields (FormFieldSet fields) {

		formFields = fields;
		return this;

	}

	@Override
	public FieldsProvider setMode(String modeSet) {
		mode = modeSet;
		return this;
	}

}