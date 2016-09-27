package wbs.platform.postgresql.console;

import static wbs.utils.collection.CollectionUtils.collectionIsEmpty;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.web.HtmlBlockUtils.htmlHeadingTwoWrite;
import static wbs.utils.web.HtmlBlockUtils.htmlParagraphWrite;
import static wbs.utils.web.HtmlTableUtils.htmlTableClose;
import static wbs.utils.web.HtmlTableUtils.htmlTableHeaderCellWrite;
import static wbs.utils.web.HtmlTableUtils.htmlTableHeaderRowWrite;
import static wbs.utils.web.HtmlTableUtils.htmlTableOpenList;
import static wbs.utils.web.HtmlTableUtils.htmlTableRowClose;
import static wbs.utils.web.HtmlTableUtils.htmlTableRowOpen;

import java.util.ArrayList;
import java.util.List;

import lombok.NonNull;

import wbs.console.part.AbstractPagePart;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.platform.postgresql.model.PostgresqlStatActivityObjectHelper;
import wbs.platform.postgresql.model.PostgresqlStatActivityRec;

@PrototypeComponent ("postgresqlActivityPart")
public
class PostgresqlActivityPart
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	PostgresqlStatActivityObjectHelper postgresqlStatActivityHelper;

	// state

	List <PostgresqlStatActivityRec> activeStatActivities;

	List <PostgresqlStatActivityRec> idleStatActivities;

	// implementation

	@Override
	public
	void prepare () {

		List<PostgresqlStatActivityRec> allStatActivities =
			postgresqlStatActivityHelper.findAll ();

		activeStatActivities =
			new ArrayList<PostgresqlStatActivityRec> ();

		idleStatActivities =
			new ArrayList<PostgresqlStatActivityRec> ();

		for (
			PostgresqlStatActivityRec statActivity
				: allStatActivities
		) {

			if (
				stringEqualSafe (
					statActivity.getCurrentQuery (),
					"<IDLE>")
			) {

				idleStatActivities.add (
					statActivity);

			} else {

				activeStatActivities.add (
					statActivity);

			}

		}

	}

	@Override
	public
	void renderHtmlBodyContent () {

		doList (
			activeStatActivities);

		htmlHeadingTwoWrite (
			"Idle");

		doList (
			idleStatActivities);

	}

	void doList (
			@NonNull List <PostgresqlStatActivityRec> statActivities) {

		if (
			collectionIsEmpty (
				statActivities)
		) {

			htmlParagraphWrite (
				"(none)");

			return;

		}

		htmlTableOpenList ();

		htmlTableHeaderRowWrite (
			"PID",
			"Database",
			"User",
			"Query");

		for (
			PostgresqlStatActivityRec statActivity
				: statActivities
		) {

			htmlTableRowOpen ();

			htmlTableHeaderCellWrite (
				integerToDecimalString (
					statActivity.getId ()));

			htmlTableHeaderCellWrite (
				statActivity.getDatabaseName ());

			htmlTableHeaderCellWrite (
				statActivity.getUserName ());

			htmlTableHeaderCellWrite (
				statActivity.getCurrentQuery ());

			htmlTableRowClose ();

		}

		htmlTableClose ();

	}

}
