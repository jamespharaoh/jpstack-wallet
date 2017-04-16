package wbs.platform.object.search;

import static wbs.utils.collection.CollectionUtils.collectionSize;
import static wbs.utils.collection.CollectionUtils.listSlice;
import static wbs.utils.collection.IterableUtils.iterableMapToList;
import static wbs.utils.collection.MapUtils.emptyMap;
import static wbs.utils.etc.Misc.isNotNull;
import static wbs.utils.etc.NumberUtils.integerToDecimalString;
import static wbs.utils.etc.NumberUtils.parseIntegerRequired;
import static wbs.utils.etc.OptionalUtils.optionalGetRequired;
import static wbs.utils.etc.OptionalUtils.optionalIf;
import static wbs.utils.etc.OptionalUtils.optionalIsNotPresent;
import static wbs.utils.etc.OptionalUtils.optionalIsPresent;
import static wbs.utils.etc.OptionalUtils.optionalOf;
import static wbs.utils.etc.OptionalUtils.presentInstances;
import static wbs.utils.etc.ReflectionUtils.methodGetRequired;
import static wbs.utils.etc.ReflectionUtils.methodInvoke;
import static wbs.utils.etc.TypeUtils.classEqualSafe;
import static wbs.utils.etc.TypeUtils.genericCastUnchecked;
import static wbs.utils.etc.TypeUtils.isNotInstanceOf;
import static wbs.utils.string.StringUtils.stringEqualSafe;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringSplitComma;
import static wbs.utils.time.TimeUtils.localDateNotEqual;
import static wbs.web.utils.HtmlAttributeUtils.htmlClassAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlColumnSpanAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttribute;
import static wbs.web.utils.HtmlAttributeUtils.htmlDataAttributeFormat;
import static wbs.web.utils.HtmlAttributeUtils.htmlStyleAttribute;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphClose;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphOpen;
import static wbs.web.utils.HtmlBlockUtils.htmlParagraphWriteFormat;
import static wbs.web.utils.HtmlFormUtils.htmlFormClose;
import static wbs.web.utils.HtmlFormUtils.htmlFormOpenPost;
import static wbs.web.utils.HtmlStyleUtils.htmlStyleRuleEntry;
import static wbs.web.utils.HtmlTableUtils.htmlTableCellWrite;
import static wbs.web.utils.HtmlTableUtils.htmlTableClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableOpenList;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowClose;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowOpen;
import static wbs.web.utils.HtmlTableUtils.htmlTableRowSeparatorWrite;
import static wbs.web.utils.HtmlUtils.htmlLinkWrite;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.Instant;
import org.joda.time.LocalDate;

import wbs.console.context.ConsoleContext;
import wbs.console.context.ConsoleContextType;
import wbs.console.forms.FormFieldLogic;
import wbs.console.forms.FormFieldSet;
import wbs.console.helper.core.ConsoleHelper;
import wbs.console.helper.core.ConsoleHooks;
import wbs.console.helper.manager.ConsoleObjectManager;
import wbs.console.html.MagicTableScriptRef;
import wbs.console.html.ScriptRef;
import wbs.console.misc.JqueryScriptRef;
import wbs.console.module.ConsoleManager;
import wbs.console.part.AbstractPagePart;

import wbs.framework.component.annotations.ClassSingletonDependency;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.entity.record.IdObject;
import wbs.framework.entity.record.Record;
import wbs.framework.logging.LogContext;
import wbs.framework.logging.TaskLogger;

import wbs.platform.user.console.UserConsoleLogic;
import wbs.platform.user.console.UserSessionLogic;

import wbs.utils.etc.NumberUtils;
import wbs.utils.etc.PropertyUtils;

@Accessors (fluent = true)
@PrototypeComponent ("objectSearchResultsPart")
public
class ObjectSearchResultsPart <
	ObjectType extends Record <ObjectType>,
	ResultType extends IdObject
>
	extends AbstractPagePart {

	// singleton dependencies

	@SingletonDependency
	ConsoleManager consoleManager;

	@SingletonDependency
	FormFieldLogic formFieldLogic;

	@ClassSingletonDependency
	LogContext logContext;

	@SingletonDependency
	ConsoleObjectManager objectManager;

	@SingletonDependency
	UserConsoleLogic userConsoleLogic;

	@SingletonDependency
	UserSessionLogic userSessionLogic;

	// properties

	@Getter @Setter
	ConsoleHelper <ObjectType> consoleHelper;

	@Getter @Setter
	FormFieldSet <ResultType> formFieldSet;

	@Getter @Setter
	FormFieldSet <ResultType> rowsFormFieldSet;

	@Getter @Setter
	Class <ResultType> resultsClass;

	@Getter @Setter
	String resultsDaoMethodName;

	@Getter @Setter
	String sessionKey;

	@Getter @Setter
	Long itemsPerPage;

	@Getter @Setter
	String targetContextTypeName;

	// state

	IdObject currentObject;
	List <Optional <ResultType>> objects;
	Integer totalObjects;

	Boolean singlePage;
	Long pageNumber;
	Long pageCount;

	Optional <ConsoleContext> targetContext;

	// details

	@Override
	public
	Set <ScriptRef> scriptRefs () {

		return ImmutableSet.<ScriptRef> builder ()

			.addAll (
				super.scriptRefs ())

			.add (
				JqueryScriptRef.instance)

			.add (
				MagicTableScriptRef.instance)

			.build ();

	}

	// implementation

	@Override
	public
	void prepare (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"prepare");

		// current object

		if (
			classEqualSafe (
				consoleHelper.objectClass (),
				resultsClass)
		) {

			Optional <Long> currentObjectIdOptional =
				requestContext.stuffInteger (
					consoleHelper.objectName () + "Id");

			if (
				optionalIsPresent (
					currentObjectIdOptional)
			) {

				currentObject =
					consoleHelper.findRequired (
						optionalGetRequired (
							currentObjectIdOptional));

			}

		}

		// set search object

		Object searchObject =
			userSessionLogic.userDataObjectRequired (
				taskLogger,
				userConsoleLogic.userRequired (),
				stringFormat (
					"object_search_%s_fields",
					sessionKey));

		// get search results for page

		List <Long> allObjectIds =
			iterableMapToList (
				NumberUtils::parseIntegerRequired,
				stringSplitComma (
					userSessionLogic.userDataStringRequired (
						userConsoleLogic.userRequired (),
						stringFormat (
							"object_search_%s_results",
							sessionKey))));

		totalObjects =
			allObjectIds.size ();

		if (
			stringEqualSafe (
				requestContext.parameterOrDefault (
					"page",
					"0"),
				"all")
		) {

			singlePage = true;

		} else {

			singlePage = false;

			pageNumber =
				parseIntegerRequired (
					requestContext.parameterOrDefault (
						"page",
						"0"));

		}

		List <Long> pageObjectIds =
			singlePage

			? allObjectIds

			: listSlice (
				allObjectIds,
				pageNumber * itemsPerPage,
				Math.min (
					(pageNumber + 1) * itemsPerPage,
					allObjectIds.size ()));

		pageCount =
			(allObjectIds.size () - 1) / itemsPerPage + 1;

		// load objects

		if (
			isNotNull (
				resultsDaoMethodName)
		) {

			Method method =
				methodGetRequired (
					consoleHelper.getClass (),
					resultsDaoMethodName,
					ImmutableList.<Class <?>> of (
						searchObject.getClass (),
						List.class));

			objects =
				genericCastUnchecked (
					methodInvoke (
						method,
						consoleHelper,
						searchObject,
						pageObjectIds));

		} else {

			objects =
				genericCastUnchecked (
					iterableMapToList (
						consoleHelper::find,
						pageObjectIds));

		}

		// other stuff

		prepareTargetContext (
			taskLogger);

	}

	void prepareTargetContext (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"prepareTargetContext");

		ConsoleContextType targetContextType =
			consoleManager.contextType (
				targetContextTypeName,
				true);

		targetContext =
			consoleManager.relatedContext (
				taskLogger,
				requestContext.consoleContextRequired (),
				targetContextType);

	}

	@Override
	public
	void renderHtmlBodyContent (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"renderHtmlBodyContent");

		goNewSearch ();
		goTotalObjects ();
		goPageNumbers ();

		goSearchResults (
			taskLogger);

		goPageNumbers ();

	}

	void goNewSearch () {

		htmlFormOpenPost ();

		htmlParagraphOpen ();

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" name=\"new-search\"",
			" value=\"new search\"",
			">");

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" name=\"repeat-search\"",
			" value=\"repeat search\"",
			">");

		formatWriter.writeLineFormat (
			"<input",
			" type=\"submit\"",
			" name=\"download-csv\"",
			" value=\"download csv\"",
			">");

		htmlParagraphClose ();

		htmlFormClose ();

	}

	void goTotalObjects () {

		htmlParagraphWriteFormat (
			"Search returned %h items",
			integerToDecimalString (
				totalObjects));

	}

	void goPageNumbers () {

		if (pageCount == 1)
			return;

		htmlParagraphOpen (
			htmlClassAttribute (
				"links"));

		formatWriter.writeLineFormat (
			"Select page");

		htmlLinkWrite (
			"?page=all",
			"All",
			presentInstances (
				optionalIf (
					singlePage,
					() -> htmlClassAttribute (
						"selected"))));

		for (
			long page = 0;
			page < pageCount;
			page ++
		) {

			htmlLinkWrite (
				stringFormat (
					"?page=%h",
					integerToDecimalString (
						page)),
				integerToDecimalString (
					page + 1),
				presentInstances (
					optionalIf (
						! singlePage && page == pageNumber,
						() -> htmlClassAttribute (
							"selected"))));

		}

		htmlParagraphClose ();

	}

	void goSearchResults (
			@NonNull TaskLogger parentTaskLogger) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"goSearchResults");

		htmlTableOpenList ();

		htmlTableRowOpen ();

		formFieldLogic.outputTableHeadings (
			formatWriter,
			formFieldSet);

		htmlTableRowClose ();

		LocalDate currentDate =
			new LocalDate (0);

		for (
			Optional <ResultType> resultOptional
				: objects
		) {

			if (
				optionalIsNotPresent (
					resultOptional)
			) {

				htmlTableRowOpen ();

				htmlTableCellWrite (
					"(deleted)",
					htmlColumnSpanAttribute (
						collectionSize (
							formFieldSet.formItems ())));

				htmlTableRowClose ();

				continue;

			}

			ResultType result =
				resultOptional.get ();

			if (

				result instanceof Record

				&& ! consoleHelper.canView (
					taskLogger,
					genericCastUnchecked (
						result))

			)  {

				htmlTableRowOpen ();

				htmlTableCellWrite (
					"(restricted)",
					htmlColumnSpanAttribute (
						collectionSize (
							formFieldSet.formItems ())));

				htmlTableRowClose ();

				continue;

			}

			if (

				classEqualSafe (
					consoleHelper.objectClass (),
					resultsClass)

				&& consoleHelper.event ()

			) {

				Instant rowTimestamp =
					(Instant)
					PropertyUtils.propertyGetAuto (
						result,
						consoleHelper.timestampFieldName ());

				LocalDate rowDate =
					rowTimestamp.toDateTime ().toLocalDate ();

				if (
					localDateNotEqual (
						currentDate,
						rowDate)
				) {

					currentDate =
						rowDate;

					htmlTableRowSeparatorWrite ();

					htmlTableRowOpen (
						htmlStyleAttribute (
							htmlStyleRuleEntry (
								"font-weight",
								"bold")));

					htmlTableCellWrite (
						userConsoleLogic.dateStringLong (
							rowTimestamp),
						htmlColumnSpanAttribute (
							collectionSize (
								formFieldSet.formItems ())));

					htmlTableRowClose ();

				}

			}

			if (
				isNotNull (
					rowsFormFieldSet)
			) {

				htmlTableRowSeparatorWrite ();

			}

			if (result instanceof Record) {

				htmlTableRowOpen (

					htmlClassAttribute (
						presentInstances (

						Optional.of (
							"magic-table-row"),

						Optional.of (
							stringFormat (
								"search-result-%s",
								integerToDecimalString (
									result.getId ()))),

						optionalIf (
							result == currentObject,
							() -> "selected"),

						getListClass (
							result)

					)),

					htmlDataAttribute (
						"rows-class",
						stringFormat (
							"search-result-%s",
							integerToDecimalString (
								result.getId ()))),

					htmlDataAttribute (
						"target-href",
						objectUrl (
							taskLogger,
							genericCastUnchecked (
								result)))

				);

			} else {

				htmlTableRowOpen ();

			}

			formFieldLogic.outputTableCellsList (
				taskLogger,
				formatWriter,
				formFieldSet,
				result,
				emptyMap (),
				false);

			if (
				isNotNull (
					rowsFormFieldSet)
			) {

				htmlTableRowClose ();

				htmlTableRowOpen (
					presentInstances (

					optionalOf (
						htmlClassAttribute (
							presentInstances (

						Optional.of (
							"magic-table-row"),

						Optional.of (
							stringFormat (
								"search-result-%s",
								integerToDecimalString (
									result.getId ()))),

						optionalIf (
							result == currentObject,
							() -> "selected"),

						getListClass (
							result)

					))),

					optionalOf (
						htmlDataAttributeFormat (
							"rows-class",
							"search-result-%s",
							integerToDecimalString (
								result.getId ()))),

					optionalIf (
						result instanceof Record,
						() -> htmlDataAttribute (
							"target-href",
							objectUrl (
								taskLogger,
								genericCastUnchecked (
									result))))

				));

				formFieldLogic.outputTableRowsList (
					taskLogger,
					formatWriter,
					rowsFormFieldSet,
					result,
					false,
					formFieldSet.columns ());

			}

			htmlTableRowClose ();

		}

		htmlTableClose ();

	}

	private
	String objectUrl (
			@NonNull TaskLogger parentTaskLogger,
			@NonNull Record <?> object) {

		TaskLogger taskLogger =
			logContext.nestTaskLogger (
				parentTaskLogger,
				"objectUrl");

		if (
			optionalIsPresent (
				targetContext)
		) {

			return requestContext.resolveContextUrl (
				stringFormat (
					"%s",
					targetContext.get ().pathPrefix (),
					"/%s",
					consoleHelper.getPathIdGeneric (
						taskLogger,
						object)));

		} else {

			return requestContext.resolveLocalUrl (
				consoleHelper.getDefaultLocalPathGeneric (
					taskLogger,
					object));

		}

	}

	private
	Optional <String> getListClass (
			@NonNull IdObject object) {

		if (
			isNotInstanceOf (
				Record.class,
				object)
		) {
			return Optional.absent ();
		}

		ConsoleHooks <ObjectType> consoleHooks =
			consoleHelper.consoleHooks ();

		return consoleHooks.getListClass (
			genericCastUnchecked (
				object));

	}

}
