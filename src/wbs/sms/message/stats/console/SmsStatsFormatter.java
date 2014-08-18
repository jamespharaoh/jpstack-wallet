package wbs.sms.message.stats.console;

import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

import org.joda.time.LocalDate;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.utils.etc.BeanLogic;
import wbs.framework.utils.etc.Html;
import wbs.platform.console.request.ConsoleRequestContext;
import wbs.platform.currency.logic.CurrencyLogic;
import wbs.sms.message.stats.console.GroupedStatsSource.GroupStats;
import wbs.sms.message.stats.model.MessageStats;
import wbs.sms.route.core.model.RouteRec;

/**
 * Responsible for outputing standardised tables of message statistics. Requires
 * a SmsStatsSource class which is responsible for retrieving the data, and is then
 * invoked with a date and a timescheme, which conspire to work out which stats
 * to retrieve and how to aggregate them by date. Really quite complicated
 * unless you look at the results and the various client classes.
 */
@Accessors (fluent = true)
@PrototypeComponent ("smsStatsFormatter")
public
class SmsStatsFormatter {

	// dependencies

	@Inject
	CurrencyLogic currencyLogic;

	@Inject
	ConsoleRequestContext requestContext;

	// properties

	@Getter @Setter
	GroupedStatsSource groupedStatsSource;

	@Getter @Setter
	LocalDate mainDate;

	@Getter @Setter
	SmsStatsTimeScheme timeScheme;

	// state

	PrintWriter out;

	LocalDate startDate;
	LocalDate endDate;
	Map<String,GroupStats> stats;

	// implementation

	public
	void go () {

		setupDates ();

		loadStats ();

		goOutput ();

	}

	void setupDates () {

		DateRange dateRange =
			timeScheme.dateRange (mainDate);

		startDate =
			LocalDate.fromDateFields (
				dateRange.getStart ());

		endDate =
			LocalDate.fromDateFields (
				dateRange.getEnd ());

	}

	void loadStats () {

		stats =
			groupedStatsSource.load (
				startDate,
				endDate);

	}

	static
	enum RowDirection {
		in,
		out;
	};

	@Accessors (fluent = true)
	static
	class Row {

		@Getter @Setter
		RowDirection direction;

		@Getter @Setter
		String name;

		@Getter @Setter
		String className;

		@Getter @Setter
		String fieldName;

		public
		int getData (
				MessageStats messageStats) {

			return (Integer)
				BeanLogic.getProperty (
					messageStats,
					fieldName);

		}

	}

	static
	List<Row> rows =
		new ArrayList<Row> ();

	static {

		rows.add (
			new Row ()
				.name ("In total")
				.className ("plain")
				.direction (RowDirection.in)
				.fieldName ("inTotal"));

		rows.add (
			new Row ()
				.name ("Out total")
				.className ("plain")
				.direction (RowDirection.out)
				.fieldName ("outTotal"));

		rows.add (
			new Row ()
				.name ("Out pending")
				.className ("unknown")
				.direction (RowDirection.out)
				.fieldName ("outPending"));

		rows.add (
			new Row ()
				.name ("Out sent")
				.className ("unknown")
				.direction (RowDirection.out)
				.fieldName ("outSent"));

		rows.add (
			new Row ()
				.name ("Out submitted")
				.className ("unknown")
				.direction (RowDirection.out)
				.fieldName ("outSubmitted"));

		rows.add (
			new Row ()
				.name ("Out delivered")
				.className ("succeeded")
				.direction (RowDirection.out)
				.fieldName ("outDelivered"));

		rows.add (
			new Row ()
				.name ("Out undelivered")
				.className ("failed")
				.direction (RowDirection.out)
				.fieldName ("outUndelivered"));

		rows.add (
			new Row ()
				.name ("Out cancelled")
				.className ("failed")
				.direction (RowDirection.out)
				.fieldName ("outCancelled"));

		rows.add (
			new Row ()
				.name ("Out failed")
				.className ("failed")
				.direction (RowDirection.out)
				.fieldName ("outFailed"));

		rows.add (
			new Row ()
				.name ("Out no report")
				.className ("failed")
				.direction (RowDirection.out)
				.fieldName ("outReportTimedOut"));

	}

	protected
	void goTotalsHeader (
			int colSpan) {

		out.print (
			stringFormat (
				"<tr>\n",
				"<td class=\"group-name\" colspan=\"%h\">TOTALS</td>",
				colSpan,
				"</tr>\n"));

	}

	protected
	void goSectionHeader (
			String group,
			String url,
			int colSpan) {

		if (url != null) {

			out.println("<tr>");

			out.print("<td");
			out.print(" class=\"group-name\"");
			out.print(" colspan=\"" + colSpan + "\"");
			out.print(" style=\"cursor: pointer;\"");
			out.print(" onmouseover=\"this.className='group-name-hover'\"");
			out.print(" onmouseout=\"this.className='group-name'\"");
			out.print(" onclick=\"window.location='" + Html.jsqe(url) + "'\"");
			out.println(">");

			out.println(Html.encode(group));

			out.println("</td>");

			out.println("</tr>");
		} else {
			out.println("<tr> <td class=\"group-name\" colspan=\"" + colSpan
					+ "\">" + Html.encode(group) + "</td> </tr>");
		}

	}

	protected
	void goSectionBody (
			RouteRec route,
			MessageStats[] data,
			boolean[] hilites) {

		for (Row row : rows) {

			StringBuilder stringBuilder =
				new StringBuilder ();

			stringBuilder.append (
				stringFormat (
					"<tr>\n",

					"<td",
					" class=\"%h\"",
					row.className,
					">%h</td>\n",
					row.name));

			boolean foundSomething =
				false;

			for (
				int index = 0;
				index < data.length;
				index ++
			) {

				MessageStats messageStatus =
					data [index];

				String className =
					hilites [index]
						? "hi-" + row.className
						: row.className;

				String charge = "";

				if (messageStatus == null) {

					charge = "";

				} else {

					int messageCount =
						row.getData (messageStatus);

					if (messageCount > 0)
						foundSomething = true;

					switch (row.direction) {

					case in:

						charge =
							route != null && route.getInCharge () > 0
								? currencyLogic.formatText (
									route.getCurrency (),
									route.getInCharge () * messageCount)
								: noZero (messageCount);

						break;

					case out:

						charge =
							route != null && route.getOutCharge () > 0
								? currencyLogic.formatText (
									route.getCurrency (),
									route.getOutCharge () * messageCount)
								: noZero (messageCount);

						break;

					}

				}

				stringBuilder.append (
					stringFormat (
						"<td",
						" class=\"%h\"",
						className,
						" style=\"text-align: right\"",
						">%h</td>\n",
						charge));

			}

			stringBuilder.append (
				stringFormat (
					"</tr>\n"));

			if (foundSomething)
				out.print (stringBuilder.toString ());

		}

	}

	void goOutput () {

		out =
			requestContext.writer ();

		out.print (
			stringFormat (
				"<table class=\"list\">\n"));

		timeScheme.goTableHeader (
			out,
			startDate);

		boolean[] hilites =
			timeScheme.getHilites (startDate);

		// goTotals (job, out, hilites);

		for (Map.Entry<String,GroupedStatsSource.GroupStats> entry
				: stats.entrySet ()) {

			GroupedStatsSource.GroupStats groupStats =
				entry.getValue ();

			String group =
				entry.getKey ();

			MessageStats[] data =
				timeScheme.getData (
					startDate,
					groupStats.getStatsByDate ());

			out.print (
				stringFormat (
					"<tr class=\"sep\">\n"));

			goSectionHeader (
				group,
				groupStats.getUrl (),
				data.length + 1);

			goSectionBody (
				entry.getValue ().getRoute (),
				data,
				hilites);

		}

		out.print (
			stringFormat (
				"</table>\n"));

	}

	String noZero (
			int num) {

		return num == 0
			? ""
			: Integer.toString (num);

	}

}