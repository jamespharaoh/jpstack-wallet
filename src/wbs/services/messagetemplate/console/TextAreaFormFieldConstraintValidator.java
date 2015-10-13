package wbs.services.messagetemplate.console;

import java.util.List;

import wbs.console.forms.FormFieldConstraintValidator;
import wbs.framework.application.annotations.PrototypeComponent;

@PrototypeComponent ("textAreaFormFieldValueValidator")
public
class TextAreaFormFieldConstraintValidator<Container>
	implements FormFieldConstraintValidator<Container,String> {

	@Override
	public
	void validate (
			Container container,
			String nativeValue,
			List<String> errors) {

		/*
		MessageTemplateTypeRec messageTemplateType =
			(MessageTemplateTypeRec) container;

		List<String> messageTemplateUsedParameters =
			new ArrayList<String>();

		String message = nativeValue;

		// length of non variable parts

		Integer messageLength = 0;

		String[] parts =
			message.split("\\{(.*?)\\}");

		for (int i = 0; i < parts.length; i++) {

			// length of special chars if gsm encoding

			if (messageTemplateType.getCharset() == MessageTemplateTypeCharset.gsm) {

				if (! Gsm.isGsm (parts[i]))
					throw new RuntimeException ("Message text is invalid");

				messageLength +=
					Gsm.length (parts[i]);

			}
			else {
				messageLength +=
						parts[i].length();
			}

		}

		// length of the parameters

		Pattern regExp = Pattern.compile("\\{(.*?)\\}");
		Matcher matcher = regExp.matcher(message);

		while (matcher.find()) {
			String parameterName =
				matcher.group(1);

			MessageTemplateParameterRec messageTemplateParameter =
					messageTemplateParameterHelper
						.findByCode (
							messageTemplateType, parameterName);

			if (messageTemplateParameter == null) {

				errors.add (
					stringFormat (
						"The parameter "+parameterName+" does not exist!"));

			}
			else {

				if (messageTemplateParameter.getLength() != null) {
					messageLength +=
						messageTemplateParameter.getLength();
				}

				messageTemplateUsedParameters
					.add(messageTemplateParameter.getName());
			}

		}

		// check if the rest of parameters which are not present were required

		for (MessageTemplateParameterRec messageTemplateParameter : messageTemplateType.getMessageTemplateParameters()) {

			if (
					! messageTemplateUsedParameters.contains(messageTemplateParameter.getName())
					&& messageTemplateParameter.getRequired()
			) {
				throw new RuntimeException ("Parameter "+messageTemplateParameter.getName()+" required but not present!");
			}

		}

		// check if the length is correct

		if (
			messageLength < messageTemplateType.getMinLength () ||
			messageLength > messageTemplateType.getMaxLength ())
		{

			errors.add (
				stringFormat (
					"The message length is out of it's template type bounds!"));
		}

		*/

	}

}