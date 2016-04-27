package wbs.sms.message.core.console;

import java.util.Set;
import java.util.TreeSet;

import javax.inject.Inject;

import wbs.console.part.AbstractPagePart;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.web.PageNotFoundException;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.report.model.MessageReportRec;

@PrototypeComponent ("messageReportsPart")
public
class MessageReportsPart
	extends AbstractPagePart {

	// dependencies

	@Inject
	MessageConsoleLogic messageConsoleLogic;

	@Inject
	MessageObjectHelper messageHelper;

	@Inject
	UserConsoleLogic userConsoleLogic;

	// state

	MessageRec message;
	Set<MessageReportRec> messageReports;

	// implementation

	@Override
	public
	void prepare () {

		message =
			messageHelper.find (
				requestContext.stuffInt ("messageId"));

		if (message == null)
			throw new PageNotFoundException ();

		messageReports =
			new TreeSet<MessageReportRec> (
				message.getReports ());

	}

	@Override
	public
	void renderHtmlBodyContent () {

		printFormat (
			"<table class=\"list\">\n");

		printFormat (
			"<tr>\n",
			"<th colspan=\"2\">Time</th>\n",
			"<th>Status</th>\n",
			"<th>Code</th>\n",
			"</tr>\n");

		if (messageReports.isEmpty ()) {

			printFormat (
				"<tr>\n",
				"<td colspan=\"4\">No reports</td>\n",
				"</tr>\n");

		} else {

			for (
				MessageReportRec messageReport
					: messageReports
			) {

				printFormat (
					"<tr>\n");

				printFormat (
					"<td>%h</td>\n",
					userConsoleLogic.timestampWithTimezoneString (
						messageReport.getReceivedTime ()));

				printFormat (
					"<td>%h</td>\n",
					userConsoleLogic.prettyDuration (
						messageReport.getReceivedTime (),
						message.getProcessedTime ()));

				printFormat (
					"%s\n",
					messageConsoleLogic.tdForMessageStatus (
						messageReport.getNewMessageStatus ()));

				if (messageReport.getCode () != null) {

					printFormat (
						"<td>%h</td>\n",
						messageReport.getCode ());

				} else if (messageReport.getMessageReportCode () != null) {

					printFormat (
						"<td>%h</td>\n",
						messageReport.getMessageReportCode ().toString ());

				} else {

					printFormat (
						"<td>-</td>\n");

				}

				printFormat (
					"</tr>\n");

			}

		}

		printFormat (
			"</table>\n");

	}

}
