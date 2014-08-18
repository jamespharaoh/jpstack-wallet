package wbs.platform.console.responder;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.record.GlobalId;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.exception.logic.ExceptionLogicImpl;
import wbs.platform.priv.console.PrivChecker;

@Accessors (fluent = true)
@PrototypeComponent ("errorResponder")
public
class ErrorResponder
	extends HtmlResponder {

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	PrivChecker privChecker;

	public final static
	String defaultTitle =
		"Internal error.";

	public final static
	String defaultMessage =
		"This page cannot be displayed, due to an internal error.";

	@Getter @Setter
	Throwable exception;

	@Getter @Setter
	String title = defaultTitle;

	@Getter @Setter
	String message = defaultMessage;

	@Override
	public
	void goBodyStuff () {

		printFormat (
			"<h1>%h</h1>\n",
			title);

		requestContext.flushNotices ();

		printFormat (
			"<p class=\"error\">%h</p>\n",
			title);

		printFormat (
			"<p>%h</p>\n",
			message);

		if (
			exception != null
			&& privChecker.can (
				GlobalId.root,
				"debug")
		) {

			printFormat (
				"<p><pre>%h</pre></p>\n",
				ExceptionLogicImpl.throwableDump(exception));

		}

	}

}