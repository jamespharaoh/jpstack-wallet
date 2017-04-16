package wbs.platform.service.logic;

import static wbs.utils.etc.Misc.doesNotContain;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Optional;

import lombok.NonNull;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.component.annotations.WeakSingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;
import wbs.framework.object.ObjectHelper;
import wbs.framework.object.ObjectHooks;
import wbs.framework.object.ObjectManager;

import wbs.platform.object.core.model.ObjectTypeDao;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.platform.scaffold.model.SliceRec;
import wbs.platform.service.model.ServiceRec;
import wbs.platform.service.model.ServiceTypeDao;
import wbs.platform.service.model.ServiceTypeRec;

public
class ServiceHooks
	implements ObjectHooks <ServiceRec> {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@ClassSingletonDependency
	LogContext logContext;

	@WeakSingletonDependency
	ObjectManager objectManager;

	@SingletonDependency
	ObjectTypeDao objectTypeDao;

	@SingletonDependency
	ServiceTypeDao serviceTypeDao;

	// state

	Map <Long, List <Long>> serviceTypeIdsByParentTypeId =
		new HashMap<> ();

	// lifecycle

	@NormalLifecycleSetup
	public
	void setup (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"setup");

		try (

			Transaction transaction =
				database.beginReadOnly (
					taskLogger,
					"serviceHooks.setup ()",
					this);

		) {

			// preload object types

			objectTypeDao.findAll ();

			// load service types and construct index

			serviceTypeIdsByParentTypeId =
				serviceTypeDao.findAll ().stream ().collect (
					Collectors.groupingBy (

				serviceType ->
					serviceType.getParentType ().getId (),

				Collectors.mapping (
					serviceType ->
						serviceType.getId (),
					Collectors.toList ()))

			);

		}

	}

	// implementation

	@Override
	public
	void createSingletons (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull ObjectHelper <ServiceRec> serviceHelper,
			@NonNull ObjectHelper <?> parentHelper,
			@NonNull Record <?> parent) {

		if (
			doesNotContain (
				serviceTypeIdsByParentTypeId.keySet (),
				parentHelper.objectTypeId ())
		) {
			return;
		}

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"createSingletons");

		Optional <SliceRec> slice =
			objectManager.getAncestor (
				SliceRec.class,
				parent);

		ObjectTypeRec parentType =
			objectTypeDao.findById (
				parentHelper.objectTypeId ());

		for (
			Long serviceTypeId
				: serviceTypeIdsByParentTypeId.get (
					parentHelper.objectTypeId ())
		) {

			ServiceTypeRec serviceType =
				serviceTypeDao.findRequired (
					serviceTypeId);

			serviceHelper.insert (
				taskLogger,
				serviceHelper.createInstance ()

				.setServiceType (
					serviceType)

				.setCode (
					serviceType.getCode ())

				.setParentType (
					parentType)

				.setParentId (
					parent.getId ())

				.setSlice (
					slice.orNull ())

			);

		}

	}

}