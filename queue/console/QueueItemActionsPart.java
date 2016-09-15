package wbs.platform.queue.console;

import static wbs.utils.etc.LogicUtils.referenceEqualWithClass;
import static wbs.utils.etc.Misc.isNotNull;

import wbs.console.helper.ConsoleObjectManager;
import wbs.console.part.AbstractPagePart;
import wbs.console.priv.UserPrivChecker;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.platform.object.core.console.ObjectTypeConsoleHelper;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.model.UserRec;

@PrototypeComponent ("queueItemActionsPart")
public
class QueueItemActionsPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	ObjectTypeConsoleHelper objectTypeHelper;

	@SingletonDependency
	UserPrivChecker privChecker;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	@SingletonDependency
	QueueItemConsoleHelper queueItemHelper;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	// state

	QueueItemRec queueItem;

	boolean canSupervise;

	// implementation

	@Override
	public
	void prepare () {

		queueItem =
			queueItemHelper.findRequired (
				requestContext.stuffInteger (
					"queueItemId"));

		canSupervise =
			queueConsoleLogic.canSupervise (
				queueItem.getQueue ());

	}

	@Override
	public
	void renderHtmlBodyContent () {

		if (! canSupervise) {

			printFormat (
				"<p>You do not have permission to perform actions on this ",
				"queue item.</p>\n");

			return;

		}

		printFormat (
			"<form",
			" method=\"post\"",
			" action=\"%h\"",
			requestContext.resolveLocalUrl (
				"/queueItem.actions"),
			">\n");

		if (
			isNotNull (
				queueItem.getQueueItemClaim ())
		) {

			if (
				referenceEqualWithClass (
					UserRec.class,
					queueItem.getQueueItemClaim ().getUser (),
					userConsoleLogic.userRequired ())
			) {

				printFormat (
					"<p>This queue item is claimed by you. You may return it ",
					"to the queue.</p>\n");

				printFormat (
					"<p><input",
					" type=\"submit\"",
					" name=\"unclaim\"",
					" value=\"unclaim\"",
					">\n");

			} else {

				printFormat (
					"<p>This queue item is claimed by \"%h\". You may return ",
					objectManager.objectPathMini (
						queueItem.getQueueItemClaim ().getUser ()),
					"it to the queue or reclaim it yourself.</p>\n");

				printFormat (
					"<p><input",
					" type=\"submit\"",
					" name=\"unclaim\"",
					" value=\"unclaim\"",
					">\n");

				printFormat (
					"<input",
					" type=\"submit\"",
					" name=\"reclaim\"",
					" value=\"reclaim\"",
					"></p>\n");

			}

		} else {

			printFormat (
				"<p>There are no actions that you can perform on this queue ",
				"item at this time.</p>\n");

		}

		printFormat (
			"</form>\n");

	}

}
