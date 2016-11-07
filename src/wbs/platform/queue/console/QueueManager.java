package wbs.platform.queue.console;

import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import org.joda.time.Duration;

import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.component.annotations.NormalLifecycleSetup;
import wbs.framework.component.annotations.PrototypeDependency;
import wbs.framework.component.annotations.SingletonComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.Record;
import wbs.framework.web.Responder;
import wbs.platform.queue.metamodel.QueueTypeSpec;
import wbs.platform.queue.model.QueueItemRec;
import wbs.platform.queue.model.QueueRec;
import wbs.platform.queue.model.QueueSubjectRec;
import wbs.platform.queue.model.QueueTypeRec;

@Log4j
@SingletonComponent ("queueManager")
public
class QueueManager {

	// singleton dependencies

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	QueueConsoleLogic queueConsoleLogic;

	// prototype dependencies

	@PrototypeDependency
	Map <String, Provider <QueueConsolePlugin>> queueHelpersByBeanName =
		Collections.emptyMap ();

	// state

	Map <String, QueueConsolePlugin> queueHelpers =
		new HashMap<> ();

	// lifecycle

	@NormalLifecycleSetup
	public
	void init () {

		// initialise queuePageFactories by querying each factory

		for (
			Map.Entry <String, Provider <QueueConsolePlugin>> entry
				: queueHelpersByBeanName.entrySet ()
		) {

			String beanName =
				entry.getKey ();

			QueueConsolePlugin queueHelper =
				entry.getValue ().get ();

			for (String queueTypeCode : queueHelper.queueTypeCodes ()) {

				if (queueHelpers.containsKey (queueTypeCode)) {

					throw new RuntimeException (
						"Duplicated queue page factory: " + queueTypeCode);

				}

				queueHelpers.put (
					queueTypeCode,
					queueHelper);

				log.debug (
					stringFormat (
						"Adding queue page factory %s from %s",
						queueTypeCode,
						beanName));

			}

		}

		log.info (
			stringFormat (
				"Added %s queue page factories for %s queue types",
				queueHelpersByBeanName.size (),
				queueHelpers.size ()));

	}

	// implementation

	public
	Responder getItemResponder (
			@NonNull ConsoleRequestContext  requestContext,
			@NonNull QueueItemRec queueItem) {

		QueueSubjectRec queueSubject =
			queueItem.getQueueSubject ();

		QueueRec queue =
			queueSubject.getQueue ();

		QueueTypeRec queueType =
			queue.getQueueType ();

		String key =
			stringFormat (
				"%s.%s",
				queueType.getParentType ().getCode (),
				queueType.getCode ());

		QueueConsolePlugin queuePageFactory =
			queueHelpers.get (
				key);

		if (queuePageFactory == null) {

			throw new RuntimeException (
				stringFormat (
					"Queue page factory not found: %s",
					key));

		}

		return queuePageFactory.makeResponder (
			queueItem);

	}

	public
	Duration getPreferredUserDelay (
			@NonNull QueueRec queue) {

		Record<?> queueParent =
			objectManager.getParent (
				queue);

		QueueTypeSpec queueTypeSpec =
			queueConsoleLogic.queueTypeSpec (
				queue.getQueueType ());

		if (
			stringEqualSafe (
				queueTypeSpec.preferredUserDelay (),
				"0")
		) {
			return Duration.ZERO;
		}

		Long preferredUserDelay =
			(Long)
			objectManager.dereference (
				queueParent,
				queueTypeSpec.preferredUserDelay ());

		return Duration.standardSeconds (
			preferredUserDelay);

	}

}
