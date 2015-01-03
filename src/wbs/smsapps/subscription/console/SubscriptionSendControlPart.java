package wbs.smsapps.subscription.console;

import javax.inject.Inject;

import org.joda.time.Instant;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.platform.console.misc.TimeFormatter;
import wbs.platform.console.part.AbstractPagePart;
import wbs.smsapps.subscription.model.SubscriptionSendRec;

@PrototypeComponent ("subscriptionSendControlPart")
public
class SubscriptionSendControlPart
	extends AbstractPagePart {

	// dependencies

	@Inject
	SubscriptionSendConsoleHelper subscriptionSendHelper;

	@Inject
	TimeFormatter timeFormatter;

	// state

	SubscriptionSendRec subscriptionSend;

	// implementation

	@Override
	public
	void prepare () {

		subscriptionSend =
			subscriptionSendHelper.find (
				requestContext.stuffInt (
					"subscriptionSendId"));

	}

	@Override
	public
	void goBodyStuff () {

		goDetails ();

		switch (subscriptionSend.getState ()) {

		case cancelled:

			printFormat (
				"<p>This send has been cancelled and can no longer be sent.",
				"</p>\n");

			break;

		case partiallySent:

			printFormat (
				"<p>This send was partially sent and then cancelled. It can ",
				"no longer be sent.</p>\n");

			break;

		case scheduled:

			printFormat (
				"<p>This send has been scheduled but not yet sent. It can be ",
				"unscheduled or cancelled.</p>\n");

			goUnschedule ();
			goCancel ();

			break;

		case sending:

			printFormat (
				"<p>This send is being sent. It can be cancelled.</p>\n");

			goCancel ();

			break;

		case sent:

			printFormat (
				"<p>This send has already been sent.</p>\n");

			break;

		case notSent:

			printFormat (
				"<p>This send has not yet been sent. It can be sent now or ",
				"scheduled to automatically sent at a specific time in the ",
				"future. Alternatively, it can be cancelled.</p>\n");

			goSendNow ();
			goSchedule ();
			goCancel ();

			break;

		default:

			throw new RuntimeException ();

		}

	}

	void goDetails () {

		printFormat (
			"<table class=\"details\">\n");

		printFormat (
			"<tr>\n",
			"<th>Description</th>\n",
			"<td>%h</td>\n",
			subscriptionSend.getDescription (),
			"</tr>\n");


		printFormat (
			"</table>\n");

	}

	void goSchedule () {

		printFormat (
			"<h2>Schedule</h2>\n");

		printFormat (
			"<p>Scheduling this send will cause it to be sent ",
			"automatically at the specified time in the future.</p>\n");

		printFormat (
			"<form method=\"post\">\n",

			"<p>Time and date<br>\n",
			"<input",
			" type=\"text\"",
			" name=\"timestamp\"",
			" value=\"%h\"",
			timeFormatter.instantToTimestampString (
				timeFormatter.defaultTimezone (),
				Instant.now ()),
			"></p>\n",

			"<p><input",
			" type=\"submit\"",
			" name=\"schedule\"",
			" value=\"schedule\"",
			"></p>\n",

			"</form>\n");

	}

	void goUnschedule () {

		printFormat (
			"<h2>Unschedule</h2>\n");

		printFormat (
			"<p>Unscheduling a send will prevent it from being sent. You ",
			"will be able to add and remove numbers and send or schedule ",
			"it again.</p>\n");

		printFormat (
			"<form method=\"post\">\n",
			"<p><input",
			" type=\"submit\"",
			" name=\"unschedule\"",
			" value=\"unschedule\"",
			"></p>\n",
			"</form>\n");

	}

	void goSendNow () {

		printFormat (
			"<h2>Send now</h2>\n");

		printFormat (
			"<p>Sending a send will begin sending messages immediately.",
			"</p>\n");

		printFormat (
			"<form method=\"post\">\n",
			"<p><input",
			" type=\"submit\"",
			" name=\"send\"",
			" value=\"send\"",
			"></p>\n",
			"</form>\n");

	}

	void goCancel () {

		printFormat (
			"<h2>Cancel</h2>\n");

		printFormat (
			"<p>Cancelling a send will stop it from being sent, now or in ",
			"the future.</p>\n");

		printFormat (
			"<form method=\"post\">\n",
			"<p><input",
			" type=\"submit\"",
			" name=\"cancel\"",
			" value=\"cancel\"",
			"></p>\n",
			"</form>\n");

	}

}