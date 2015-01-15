package wbs.test.simulator.console;

import static wbs.framework.utils.etc.Misc.doesNotStartWith;
import static wbs.framework.utils.etc.Misc.equal;
import static wbs.framework.utils.etc.Misc.instantToDate;
import static wbs.framework.utils.etc.Misc.stringFormat;
import static wbs.framework.utils.etc.Misc.toBoolean;
import static wbs.framework.utils.etc.Misc.toInteger;

import javax.inject.Inject;
import javax.inject.Provider;

import lombok.Cleanup;

import org.json.simple.JSONValue;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.framework.web.Responder;
import wbs.platform.console.action.ConsoleAction;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.scaffold.console.RootConsoleHelper;
import wbs.platform.scaffold.console.SliceConsoleHelper;
import wbs.platform.text.model.TextObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.core.model.MessageStatus;
import wbs.sms.message.inbox.logic.InboxLogic;
import wbs.sms.message.report.logic.ReportLogic;
import wbs.sms.network.console.NetworkConsoleHelper;
import wbs.sms.network.model.NetworkRec;
import wbs.sms.number.core.console.NumberConsoleHelper;
import wbs.sms.number.core.model.NumberRec;
import wbs.sms.route.core.console.RouteConsoleHelper;
import wbs.sms.route.core.model.RouteRec;
import wbs.test.simulator.model.SimulatorEventObjectHelper;
import wbs.test.simulator.model.SimulatorEventRec;
import wbs.test.simulator.model.SimulatorRec;
import wbs.test.simulator.model.SimulatorRouteRec;
import wbs.test.simulator.model.SimulatorSessionNumberObjectHelper;
import wbs.test.simulator.model.SimulatorSessionNumberRec;
import wbs.test.simulator.model.SimulatorSessionObjectHelper;
import wbs.test.simulator.model.SimulatorSessionRec;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

@PrototypeComponent ("simulatorSessionCreateEventAction")
public
class SimulatorSessionCreateEventAction
	extends ConsoleAction {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	InboxLogic inboxLogic;

	@Inject
	NetworkConsoleHelper networkHelper;

	@Inject
	NumberConsoleHelper numberHelper;

	@Inject
	ObjectManager objectManager;

	@Inject
	ReportLogic reportLogic;

	@Inject
	RootConsoleHelper rootHelper;

	@Inject
	RouteConsoleHelper routeHelper;

	@Inject
	SimulatorEventObjectHelper simulatorEventHelper;

	@Inject
	SimulatorSessionObjectHelper simulatorSessionHelper;

	@Inject
	SimulatorSessionNumberObjectHelper simulatorSessionNumberHelper;

	@Inject
	SliceConsoleHelper sliceHelper;

	@Inject
	TextObjectHelper textHelper;

	@Inject
	Provider<JsonResponder> jsonResponder;

	// details

	@Override
	protected
	Responder backupResponder () {
		return null;
	}

	@Override
	protected
	Responder goReal () {

		try {

			String type =
				requestContext.getForm ("type");

			if (equal (type, "sendMessage"))
				return sendMessage ();

			if (equal (type, "deliveryReport"))
				return deliveryReport ();

			throw new AjaxException (
				stringFormat (
					"Invalid event type: %s",
					type));

		} catch (AjaxException error) {

			return jsonResponder.get ()
				.value (
					ImmutableMap.<Object,Object>builder ()
						.put ("success", false)
						.put ("error", error.message)
						.build ());

		} catch (RuntimeException error) {

			return jsonResponder.get ()
				.value (
					ImmutableMap.<Object,Object>builder ()
						.put ("success", false)
						.put ("error", "internal error")
						.build ());
		}

	}

	Responder sendMessage () {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		SimulatorSessionRec simulatorSession =
			simulatorSessionHelper.find (
				requestContext.stuffInt (
					"simulatorSessionId"));

		SimulatorRec simulator =
			simulatorSession.getSimulator ();

		String numFrom =
			requestContext.getForm ("numFrom");

		String numTo =
			requestContext.getForm ("numTo");

		String messageText =
			requestContext.getForm ("message");

		NetworkRec network =
			networkHelper.find (
				toInteger (
					requestContext.getForm ("networkId")));

		// store in session

		requestContext.session (
			"simulatorNumFrom",
			numFrom);

		requestContext.session (
			"simulatorNumTo",
			numTo);

		requestContext.session (
			"simulatorMessage",
			messageText);

		requestContext.session (
			"simulatorNetworkId",
			network.getId ().toString ());

		// work out route

		Optional<RouteRec> routeOption =
			resolveRoute (
				simulator,
				numTo);

		if (! routeOption.isPresent ()) {

			throw new AjaxException (
				"No route configured for that number");

		}

		RouteRec route =
			routeOption.get ();

		// insert inbox

		MessageRec message =
			inboxLogic.inboxInsert (
				null,
				textHelper.findOrCreate (messageText),
				numFrom,
				numTo,
				route,
				network,
				null,
				null,
				null,
				null);

		// create event data

		Object data =
			ImmutableMap.<String,Object>builder ()
				.put ("message", ImmutableMap.<String,Object>builder ()
					.put ("id", message.getId ())
					.put ("numFrom", numFrom)
					.put ("numTo", numTo)
					.put ("text", messageText)
					.build ())
				.put ("route", ImmutableMap.<String,Object>builder ()
					.put ("id", route.getId ())
					.put ("code", route.getCode ())
					.build ())
				.put ("network", ImmutableMap.<String,Object>builder ()
					.put ("id", network.getId ())
					.put ("code", network.getCode ())
					.build ())
				.build ();

		// create event

		simulatorEventHelper.insert (
			new SimulatorEventRec ()

			.setSimulatorSession (
				simulatorSession)

			.setType (
				"message_in")

			.setTimestamp (
				transaction.now ())

			.setData (
				JSONValue.toJSONString (data)));

		// associate number with session

		NumberRec number =
			numberHelper.findOrCreate (
				numFrom);

		SimulatorSessionNumberRec simulatorSessionNumber =
			simulatorSessionNumberHelper.findOrCreate (
				number);

		simulatorSessionNumber

			.setSimulatorSession (
				simulatorSession);

		// done

		transaction.commit ();

		return jsonResponder.get ()
			.value (
				ImmutableMap.<Object,Object>builder ()
					.put ("success", true)
					.build ());

	}

	Responder deliveryReport () {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		SimulatorSessionRec simulatorSession =
			simulatorSessionHelper.find (
				requestContext.stuffInt (
					"simulatorSessionId"));

		Integer messageId =
			toInteger (requestContext.getForm ("messageId"));

		Boolean success =
			toBoolean (requestContext.getForm ("success"));

		// submit delivery report

		reportLogic.deliveryReport (
			messageId,
			success
				? MessageStatus.delivered
				: MessageStatus.undelivered,
			instantToDate (
				transaction.now ()),
			null);

		// create event data

		Object data =
			ImmutableMap.<String,Object>builder ()
				.put ("deliveryReport", ImmutableMap.<String,Object>builder ()
					.put ("messageId", messageId)
					.put ("success", success)
					.build ())
				.build ();

		// create event

		simulatorEventHelper.insert (
			new SimulatorEventRec ()

			.setSimulatorSession (
				simulatorSession)

			.setType (
				"delivery_report")

			.setTimestamp (
				transaction.now ())

			.setData (
				JSONValue.toJSONString (data)));

		// done

		transaction.commit ();

		return jsonResponder.get ()
			.value (
				ImmutableMap.<Object,Object>builder ()
					.put ("success", true)
					.build ());

	}

	Optional<RouteRec> resolveRoute (
			SimulatorRec simulator,
			String number) {

		for (
			SimulatorRouteRec simulatorRoute
				: simulator.getSimulatorRoutes ()
		) {

			if (
				doesNotStartWith (
					number,
					simulatorRoute.getPrefix ())
			) {
				continue;
			}

			return Optional.of (
				simulatorRoute.getRoute ());

		}

		return Optional.absent ();

	}

	static
	class AjaxException
		extends RuntimeException {

		String message;

		AjaxException (
				String message) {

			this.message = message;

		}

	}

}
