package wbs.integrations.mig.api;

import static wbs.utils.string.StringUtils.bytesToString;
import static wbs.utils.string.StringUtils.stringFormat;
import static wbs.utils.string.StringUtils.stringNotEqualSafe;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.ServletException;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;

import lombok.Cleanup;
import lombok.extern.log4j.Log4j;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.fileupload.MultipartStream;

import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.exception.ExceptionLogger;
import wbs.framework.exception.ExceptionUtils;
import wbs.framework.exception.GenericExceptionResolution;
import wbs.framework.web.AbstractWebFile;
import wbs.framework.web.PathHandler;
import wbs.framework.web.RegexpPathHandler;
import wbs.framework.web.RequestContext;
import wbs.framework.web.ServletModule;
import wbs.framework.web.WebFile;
import wbs.integrations.mig.logic.MigLogic;
import wbs.platform.media.logic.MediaLogic;
import wbs.platform.media.model.MediaObjectHelper;
import wbs.platform.media.model.MediaRec;
import wbs.platform.text.model.TextObjectHelper;
import wbs.sms.message.inbox.logic.SmsInboxLogic;
import wbs.sms.network.model.NetworkRec;
import wbs.sms.route.core.model.RouteObjectHelper;
import wbs.sms.route.core.model.RouteRec;

@Log4j
public
class MigMmsApiServletModule
	implements ServletModule {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	ExceptionLogger exceptionLogger;

	@SingletonDependency
	ExceptionUtils exceptionLogic;

	@SingletonDependency
	SmsInboxLogic smsInboxLogic;

	@SingletonDependency
	MediaObjectHelper mediaHelper;

	@SingletonDependency
	MediaLogic mediaLogic;

	@SingletonDependency
	MigLogic migLogic;

	@SingletonDependency
	RequestContext requestContext;

	@SingletonDependency
	RouteObjectHelper routeHelper;

	@SingletonDependency
	TextObjectHelper textHelper;

	// implementation

	String getException (
			Throwable throwable,
			RequestContext requestContext) {

		StringBuilder stringBuilder =
			new StringBuilder ();

		stringBuilder.append (
			exceptionLogic.throwableDump (
				throwable));

		stringBuilder.append (
			"\n\nHTTP INFO\n\n");

		stringBuilder.append (
			"METHOD = " + requestContext.method () + "\n\n");

		for (Map.Entry<String,List<String>> ent
				: requestContext.parameterMap ().entrySet ()) {

			for (String value
					: ent.getValue ()) {

				stringBuilder.append (ent.getKey ());
				stringBuilder.append (" = \"");
				stringBuilder.append (value);
				stringBuilder.append ("\"\n");

			}

		}

		return stringBuilder.toString ();

	}

	Pattern boundaryPattern =
		Pattern.compile (
			"boundary=([-a-zA-Z0-9_]+)",
			Pattern.CASE_INSENSITIVE);

	Pattern contentTypePattern =
		Pattern.compile (
			"content-type:\\s*([^;]*)\\s*",
			Pattern.CASE_INSENSITIVE);

	Pattern filenamePattern =
		Pattern.compile (
			"filename=\"([^\"]*)\"",
			Pattern.CASE_INSENSITIVE);

	List<MediaRec> parseMMS (
			RequestContext requestContext) {

		try {

			List<MediaRec> medias =
				new ArrayList<MediaRec> ();

			String contentType =
				requestContext.header ("content-type");

			log.debug ("Content-Type: " + contentType);

			Matcher matcher =
				boundaryPattern.matcher (contentType);

			if (! matcher.find ())
				throw new RuntimeException ();

			String boundaryString =
				matcher.group (1);

			log.debug ("Boundary: " + boundaryString);

			byte[] boundaryBytes =
				boundaryString.getBytes ("utf-8");

			@SuppressWarnings ("deprecation")
			MultipartStream multipartStream =
				new MultipartStream (
					requestContext.inputStream (),
					boundaryBytes);

			boolean nextPart =
				multipartStream.skipPreamble ();

			while (nextPart) {

				String header =
					multipartStream.readHeaders ();

				log.debug ("header: " + header);

				matcher =
					contentTypePattern.matcher (header);

				if (! matcher.find ())
					throw new RuntimeException ();

				contentType =
					matcher.group (1);

				log.debug ("content type: " + contentType);

				matcher =
					filenamePattern.matcher (header);

				if (! matcher.find ())
					throw new RuntimeException ();

				String filename = matcher.group (1);

				log.debug ("filename: " + filename);

				ByteArrayOutputStream output =
					new ByteArrayOutputStream ();

				multipartStream.readBodyData (output);

				log.debug ("raw content size: " + output.size ());

				byte[] data =
					Base64.decodeBase64 (
						output.toByteArray ());

				log.debug (
					stringFormat (
						"decoded content size: %s",
						data.length));

				log.debug (
					stringFormat (
						"Got part: len=[%d], type=[%s], name=[%s]",
						data.length,
						contentType,
						filename));

				MediaRec media =
					mediaLogic.createMediaRequired (
						data,
						contentType,
						filename,
						Optional.of (
							"utf-8"));

				log.debug (
					stringFormat (
						"media id: %s",
						media.getId ()));

				medias.add (
					media);

				nextPart =
					multipartStream.readBoundary ();

			}

			/*
logger.error ("About to parse MMS");
			for (FileItem item : items) {
logger.error ("Got item");

				logger.debug (sf (
					"Got part: len=[%d], type=[%s], name=[%s]",
					item.get ().length,
					item.getContentType (),
					item.getName ()));

				MediaRec media =
					mediaUtils.createMedia (
						item.get (),
						item.getContentType (),
						item.getName (),
						"utf-8");

				medias.add (media);
			}
			*/

			// return
			return medias;

		} catch (Exception exception) {

			throw new RuntimeException (
				exception);

		}

	}

	String getText (
			String subject,
			List<MediaRec> medias) {

		subject =
			subject.trim ();

		List <String> texts =
			new ArrayList<> ();

		for (
			MediaRec media
				: medias
		) {

			if (
				stringNotEqualSafe (
					"text/plain",
					media.getMediaType ().getMimeType ())
			) {
				continue;
			}

			String text =
				bytesToString (
					media.getContent ().getData (),
					media.getEncoding ());

			text = text.trim ();

			if (text.length () == 0)
				continue;

			texts.add (text);
		}

		StringBuilder stringBuilder =
			new StringBuilder ();

		// add subject, unless it's a prefix of the first text

		if (

			! subject.isEmpty ()

			&& (

				texts.isEmpty ()

				|| texts.get (0).length ()
					< subject.length ()

				|| stringNotEqualSafe (
					subject,
					texts.get (0).substring (
						0,
						subject.length ()))
			)

		) {

			stringBuilder.append (
				subject);

		}

		for (String text : texts) {

			if (stringBuilder.length () > 0)
				stringBuilder.append ("\n");

			stringBuilder.append (text);
		}

		return stringBuilder.toString ();

	}

	WebFile inFile =
		new AbstractWebFile () {

		@Override
		public
		void doPost ()
			throws
				ServletException,
				IOException {

			doGet ();

		}

		@Override
		public
		void doGet ()
			throws
				ServletException,
				IOException {

			@Cleanup
			Transaction transaction =
				database.beginReadWrite (
					"MigMmsApiServletModule.inFile.doGet ()",
					this);

			requestContext.debugDump (
				log,
				false);

			String guid = requestContext.header ("X-QUINSY-MMS-GUID");
			String subject = requestContext.header ("X-QUINSY-MMS-SUBJECT");
			String sender = requestContext.header ("X-QUINSY-MMS-SENDER");
			String recipient = requestContext.header ("X-QUINSY-MMS-RECIPIENT");
			String connection = requestContext.header ("X-QUINSY-MMS-CONNECTION");

			if (sender.startsWith ("00")) {

				sender =
					sender.substring (
						2,
						sender.length ());

			}

			if (sender.startsWith ("+")) {

				sender =
					sender.substring (
						1,
						sender.length ());

			}

			List<MediaRec> medias =
				parseMMS (requestContext);

			for (MediaRec media : medias) {

				mediaHelper.insert (
					media);

			}

			NetworkRec network =
				migLogic.getNetwork (
					connection,
					sender);

			RouteRec route =
				routeHelper.findRequired (
					requestContext.requestIntegerRequired (
						"routeId"));

			smsInboxLogic.inboxInsert (
				Optional.of (
					guid),
				textHelper.findOrCreate (
					getText (
						subject,
						medias)),
				sender,
				recipient,
				route,
				Optional.of (
					network),
				Optional.absent (),
				medias,
				Optional.absent (),
				Optional.of (
					subject));

			transaction.commit ();

		}

	};

	WebFile reportFile =
		new AbstractWebFile () {

		@Override
		public
		void doPost ()
			throws
				ServletException,
				IOException {

			doGet ();

		}

		@Override
		public
		void doGet ()
			throws
				ServletException,
				IOException {

			requestContext.debugDump (
				log,
				false);

			Transaction transaction =
				database.beginReadWrite (
					"MigMmsApiServletModule.reportFile.doGet ()",
					this);

			try {

				// process

				transaction.commit ();

				String response = "000";

				PrintWriter out =
					requestContext.writer ();

				out.println(response);

				// logger.info ("Response 000 "+message.getId());

			} catch (Exception exception) {

				exceptionLogger.logSimple (
					"webapi",
					requestContext.requestUri (),
					exceptionLogic.throwableSummary (
						exception),
					getException (
						exception,
						requestContext),
					Optional.absent (),
					GenericExceptionResolution.ignoreWithThirdPartyWarning);

				PrintWriter out =
					requestContext.writer ();

				out.println (
					"400");

				log.info (
					"Response 400 ");

			} finally {

				transaction.close ();

			}

		}

	};

	// ========================================================= servlet module

	Map<String,WebFile> routeFiles =
		ImmutableMap.<String,WebFile>builder ()
			.put ("report", reportFile)
			.put ("in", inFile)
			.build ();

	RegexpPathHandler.Entry routeEntry =
		new RegexpPathHandler.Entry (
			"/route/([0-9]+)/([^/]+)") {

			@Override
			protected
			WebFile handle (
					Matcher matcher) {

				requestContext.request (
					"routeId",
					Integer.parseInt (
						matcher.group (1)));

				return routeFiles.get (
					matcher.group (2));

			}

		};

	RegexpPathHandler.Entry inEntry =
		new RegexpPathHandler.Entry (
			"/in/([0-9]+)") {

			@Override
			protected
			WebFile handle (
					Matcher matcher) {

				requestContext.request ("routeId",
					Integer.parseInt (
						matcher.group (1)));

				return inFile;

			}

		};

	PathHandler pathHandler =
		new RegexpPathHandler (
			routeEntry,
			inEntry);

	@Override
	public
	Map<String,WebFile> files () {
		return null;
	}

	@Override
	public
	Map<String,PathHandler> paths () {

		return ImmutableMap.<String,PathHandler>builder ()
			.put ("/migMMS", pathHandler)
			.build ();

	}

}
