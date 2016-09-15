package wbs.sms.command.logic;

import static wbs.utils.etc.OptionalUtils.optionalIsPresent;

import com.google.common.base.Optional;

import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.Record;
import wbs.framework.object.ObjectManager;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.sms.command.model.CommandObjectHelper;
import wbs.sms.command.model.CommandRec;
import wbs.sms.command.model.CommandTypeObjectHelper;
import wbs.sms.command.model.CommandTypeRec;

@SingletonComponent ("commandLogic")
public
class CommandLogicImplementation
	implements CommandLogic {

	// singleton dependencies

	@SingletonDependency
	CommandObjectHelper commandHelper;

	@SingletonDependency
	CommandTypeObjectHelper commandTypeHelper;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	ObjectManager objectManager;

	// implementation

	@Override
	public
	CommandRec findOrCreateCommand (
			Record<?> parent,
			String typeCode,
			String code) {

		// lookup existing command...

		Optional<CommandRec> existingCommandOptional =
			commandHelper.findByCode (
				parent,
				code);

		if (
			optionalIsPresent (
				existingCommandOptional)
		) {
			return existingCommandOptional.get ();
		}

		// ...or create new command

		ObjectTypeRec parentType =
			objectTypeHelper.findRequired (
				objectManager.getObjectTypeId (
					parent));

		CommandTypeRec commandType =
			commandTypeHelper.findByCodeRequired (
				parentType,
				typeCode);

		return commandHelper.insert (
			commandHelper.createInstance ()

			.setCode (
				code)

			.setCommandType (
				commandType)

			.setParentType (
				parentType)

			.setParentId (
				parent.getId ())

		);

	}

}
