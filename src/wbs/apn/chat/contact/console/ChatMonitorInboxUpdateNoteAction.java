package wbs.apn.chat.contact.console;

import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.string.StringUtils.stringFormat;

import javax.servlet.ServletException;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j;

import wbs.apn.chat.contact.model.ChatContactNoteObjectHelper;
import wbs.apn.chat.contact.model.ChatContactNoteRec;
import wbs.apn.chat.contact.model.ChatMonitorInboxObjectHelper;
import wbs.apn.chat.contact.model.ChatMonitorInboxRec;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.console.action.ConsoleAction;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.object.ObjectManager;
import wbs.framework.web.Responder;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserObjectHelper;

@Log4j
@PrototypeComponent ("chatMonitorInboxUpdateNoteAction")
public
class ChatMonitorInboxUpdateNoteAction
	extends ConsoleAction {

	// singleton dependencies

	@SingletonDependency
	ChatContactNoteObjectHelper chatContactNoteHelper;

	@SingletonDependency
	ChatMonitorInboxObjectHelper chatMonitorInboxHelper;

	@SingletonDependency
	ConsoleRequestContext requestContext;

	@SingletonDependency
	Database database;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserObjectHelper userHelper;

	// details

	@Override
	public
	Responder backupResponder () {

		return responder (
			"chatMonitorInboxSummaryResponder");

	}

	@Override
	protected
	Responder goReal ()
		throws ServletException {

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"ChatMonitorInboxUpdateNodeAction.goReal ()",
				this);

		ChatMonitorInboxRec chatMonitorInbox =
			chatMonitorInboxHelper.findRequired (
				requestContext.stuffInteger (
					"chatMonitorInboxId"));

		ChatUserRec monitorChatUser =
			chatMonitorInbox.getMonitorChatUser ();

		ChatContactNoteRec note =
			chatContactNoteHelper.findRequired (
				requestContext.parameterIntegerRequired (
					"id"));

		if (
			isNotNull (
				requestContext.getForm (
					"deleteNote"))
		) {

			log.info (
				stringFormat (
					"deleting note from %s",
					monitorChatUser.getName ()));

			objectManager.remove (
				note);

			transaction.commit ();

			requestContext.addNotice (
				"Note deleted");

		} else if (
			isNotNull (
				requestContext.getForm (
					"pegNote"))

		) {

			note.setPegged (true);

			transaction.commit ();

			log.info (
				stringFormat (
					"User %s pegged chat user contact note %s",
					userConsoleLogic.userIdRequired (),
					note.getId ()));

			requestContext.addNotice (
				"Note pegged");

		} else if (
			isNotNull (
				requestContext.getForm (
					"unpegNote"))
		) {

			note.setPegged (false);

			transaction.commit ();

			log.info (
				stringFormat (
					"User %s unpegged chat user contact note %s",
					userConsoleLogic.userIdRequired (),
					note.getId ()));

			requestContext.addNotice (
				"Note unpegged");

		} else {

			throw new RuntimeException ();

		}

		return null;

	}

}