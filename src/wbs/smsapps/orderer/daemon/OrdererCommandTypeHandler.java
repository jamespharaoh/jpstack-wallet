package wbs.smsapps.orderer.daemon;

import java.util.List;

import javax.inject.Provider;

import com.google.common.base.Optional;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.object.ObjectManager;
import wbs.platform.affiliate.model.AffiliateRec;
import wbs.platform.lock.logic.LockLogic;
import wbs.platform.service.model.ServiceObjectHelper;
import wbs.platform.service.model.ServiceRec;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.message.core.model.MessageObjectHelper;
import wbs.sms.message.core.model.MessageRec;
import wbs.sms.message.delivery.model.DeliveryDao;
import wbs.sms.message.inbox.daemon.CommandHandler;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.message.inbox.model.InboxAttemptRec;
import wbs.sms.message.inbox.model.InboxRec;
import wbs.sms.message.outbox.logic.SmsMessageSender;
import wbs.sms.number.core.model.NumberRec;
import wbs.smsapps.orderer.model.OrdererOrderObjectHelper;
import wbs.smsapps.orderer.model.OrdererOrderRec;
import wbs.smsapps.orderer.model.OrdererRec;
import wbs.utils.email.EmailLogic;

@Accessors (fluent = true)
@SingletonComponent ("ordererCommandTypeHandler")
public
class OrdererCommandTypeHandler
	implements CommandHandler {

	// singleton dependencies

	@SingletonDependency
	CommandObjectHelper commandHelper;

	@SingletonDependency
	LockLogic coreLogic;

	@SingletonDependency
	Database database;

	@SingletonDependency
	DeliveryDao deliveryDao;

	@SingletonDependency
	EmailLogic emailUtils;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	@SingletonDependency
	MessageObjectHelper messageHelper;

	@SingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	OrdererOrderObjectHelper ordererOrderHelper;

	@SingletonDependency
	ServiceObjectHelper serviceHelper;

	// prototype dependencies

	@PrototypeDependency
	Provider <SmsMessageSender> messageSenderProvider;

	// properties

	@Getter @Setter
	InboxRec inbox;

	@Getter @Setter
	CommandRec command;

	@Getter @Setter
	Optional<Long> commandRef;

	@Getter @Setter
	String rest;

	// details

	@Override
	public
	String[] getCommandTypes () {

		return new String [] {
			"orderer.default"
		};

	}

	// implementation

	@Override
	public
	InboxAttemptRec handle () {

		OrdererRec orderer =
			(OrdererRec)
			objectManager.getParent (
				command);

		ServiceRec defaultService =
			serviceHelper.findByCodeRequired (
				orderer,
				"default");

		MessageRec message =
			inbox.getMessage ();

		NumberRec number =
			message.getNumber ();

		// set the message service

		message.setService (
			serviceHelper.findByCodeRequired (
				orderer,
				"default"));

		// check its not a repeat order

		if (! orderer.getAllowRepeat ()) {

			coreLogic.magicLock (
				orderer,
				number);

			List<OrdererOrderRec> ordererOrders =
				ordererOrderHelper.find (
					orderer,
					number);

			if (! ordererOrders.isEmpty ()) {

				return smsInboxLogic.inboxProcessed (
					inbox,
					Optional.of (defaultService),
					Optional.<AffiliateRec>absent (),
					command);

			}

		}

		// create the order record

		OrdererOrderRec order =
			ordererOrderHelper.insert (
				ordererOrderHelper.createInstance ()

			.setOrderer (
				orderer)

			.setNumber (
				number)

			.setReceivedMessage (
				message)

			.setText (
				rest)

		);

		// send the billed message

		MessageRec billedMessage =
			messageSenderProvider.get ()

			.threadId (
				message.getThreadId ())

			.number (
				number)

			.messageString (
				orderer.getBillTemplate ())

			.numFrom (
				orderer.getBillNumber ())

			.route (
				orderer.getBillRoute ())

			.service (
				defaultService)

			.deliveryTypeCode (
				"orderer")

			.ref (
				(long)
				order.getId ())

			.send ();

		order

			.setBilledMessage (
				billedMessage);

		return smsInboxLogic.inboxProcessed (
			inbox,
			Optional.of (defaultService),
			Optional.<AffiliateRec>absent (),
			command);

	}

}
