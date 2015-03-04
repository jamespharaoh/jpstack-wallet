package wbs.applications.imchat.console;

import javax.inject.Inject;

import lombok.Cleanup;
import wbs.applications.imchat.model.ImChatConversationRec;
import wbs.applications.imchat.model.ImChatMessageObjectHelper;
import wbs.applications.imchat.model.ImChatMessageRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.web.Responder;
import wbs.platform.console.action.ConsoleAction;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.queue.logic.QueueLogic;
import wbs.platform.user.model.UserObjectHelper;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("imChatMessagePendingFormAction")
public
class ImChatMessagePendingFormAction
	extends ConsoleAction {

	// dependencies

	@Inject
	ConsoleRequestContext requestContext;

	@Inject
	Database database;

	@Inject
	ImChatMessageObjectHelper imChatMessageHelper;

	@Inject
	QueueLogic queueLogic;

	@Inject
	UserObjectHelper userHelper;

	// details

	@Override
	public
	Responder backupResponder () {

		return responder (
			"imChatMessagePendingFormResponder");

	}

	// implementation

	@Override
	public
	Responder goReal () {

		String messageText =
			requestContext.parameter ("reply");

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite ();

		// find user

		UserRec myUser =
			userHelper.find (
				requestContext.userId ());

		// find message

		ImChatMessageRec imChatMessage =
			imChatMessageHelper.find (
				requestContext.stuffInt (
					"imChatMessageId"));

		ImChatConversationRec conversation =
			imChatMessage.getImChatConversation ();

		// create reply

		imChatMessageHelper.insert (
			new ImChatMessageRec ()

			.setImChatConversation (
				conversation)

			.setIndex (
				conversation.getNumMessages ())

			.setSenderUser (
				myUser)

			.setTimestamp (
				transaction.now ())

				.setMessageText (
				messageText)

		);

		// update conversation

		conversation

			.setNumMessages (
				conversation.getNumMessages () + 1);

		// remove queue item

		queueLogic.processQueueItem (
			imChatMessage.getQueueItem (),
			myUser);

		// done

		transaction.commit ();

		requestContext.addNotice (
			"Reply sent");

		// return

		return responder (
			"queueHomeResponder");

	}


}
