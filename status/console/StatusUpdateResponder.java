package wbs.platform.status.console;

import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.thread.ConcurrentUtils.futureGet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.joda.time.Instant;

import wbs.console.request.ConsoleRequestContext;
import wbs.console.responder.ConsoleResponder;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.config.WbsConfig;
import wbs.platform.scaffold.console.RootConsoleHelper;
import wbs.platform.scaffold.model.RootRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("statusUpdateResponder")
public
class StatusUpdateResponder
	extends ConsoleResponder {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	RootConsoleHelper rootHelper;

	@SingletonDependency
	StatusLineManager statusLineManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserObjectHelper userHelper;

	@SingletonDependency
	WbsConfig wbsConfig;

	// state

	RootRec root;
	UserRec user;
	SliceRec slice;

	String javascript;

	// implementation

	@Override
	protected
	void prepare () {

		// redirect to login page if not logged in

		if (userConsoleLogic.notLoggedIn ()) {

			javascript =
				stringFormat (
					"window.top.location = '%h';",
					requestContext.resolveApplicationUrl (
						"/"));

			return;

		}

		// find objects

		root =
			rootHelper.findRequired (
				0l);

		// get status lines

		List<Future<String>> futures =
			statusLineManager.getStatusLines ().stream ()

			.map (
				statusLine ->
					statusLine.getUpdateScript ())

			.collect (
				Collectors.toList ());

		// create the html

		StringWriter stringWriter =
			new StringWriter ();

		PrintWriter printWriter =
			new PrintWriter (stringWriter);

		printWriter.print (
			stringFormat (
				"updateTimestamp ('%j');\n",
				userConsoleLogic.timestampWithTimezoneString (
					transaction.now ())));

		if (
			isNotNull (
				root.getNotice ())
		) {

			printWriter.print (
				stringFormat (
					"updateNotice ('%j');\n",
					root.getNotice ()));

		} else {

			printWriter.print (
				stringFormat (
					"updateNotice (undefined);\n"));

		}

		// close transaction

		transaction.close ();

		// wait for status lines

		futures.forEach (
			future ->
				printWriter.print (
					futureGet (
						future)));

		// convert to string

		javascript =
			stringWriter.toString ();

	}

	@Override
	protected
	void setHtmlHeaders ()
		throws IOException {

		super.setHtmlHeaders ();

		requestContext.setHeader (
			"Content-Type",
			"text/xml");

		requestContext.setHeader (
			"Cache-Control",
			"no-cache");

		requestContext.setHeader (
			"Expiry",
			userConsoleLogic.httpTimestampString (
				Instant.now ()));

	}

	@Override
	protected
	void render ()
		throws IOException {

		Element statusUpdateElem =
			new Element ("status-update");

		Document document =
			new Document (statusUpdateElem);

		Element javascriptElem =
			new Element ("javascript");

		statusUpdateElem.addContent (
			javascriptElem);

		javascriptElem.setText (
			javascript);

		XMLOutputter xmlOutputter =
			new XMLOutputter (
				Format.getPrettyFormat ());

		xmlOutputter.output (
			document,
			requestContext.writer ());

	}

}
