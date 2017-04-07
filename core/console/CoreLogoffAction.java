package wbs.platform.core.console;

import lombok.NonNull;

import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;

import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

import wbs.web.responder.Responder;

@PrototypeComponent ("coreLogoffAction")
public
class CoreLogoffAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@SingletonDependency
	UserObjectHelper userHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// details

	@Override
	public
	Responder backupResponder () {

		return responder (
			"coreRedirectResponder");

	}

	// implementation

	@Override
	public
	Responder goReal (
			@NonNull TaskLogger taskLogger) {

		if (! userConsoleLogic.loggedIn ()) {
			return null;
		}

		Long userId =
			userConsoleLogic.userIdRequired ();

		try (

			Transaction transaction =
				database.beginReadWrite (
					"CoreLogoffAction.goReal ()",
					this);

		) {

			UserRec user =
				userHelper.findRequired (
					userId);

			userSessionLogic.userLogoff (
				taskLogger,
				user);

			transaction.commit ();

			return null;

		}

	}

}
