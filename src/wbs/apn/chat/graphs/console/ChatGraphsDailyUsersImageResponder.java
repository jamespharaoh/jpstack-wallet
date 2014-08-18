package wbs.apn.chat.graphs.console;

import static wbs.framework.utils.etc.Misc.dateToInstant;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;

import org.joda.time.DateTimeZone;
import org.joda.time.Instant;

import wbs.apn.chat.core.logic.ChatMiscLogic;
import wbs.apn.chat.core.model.ChatObjectHelper;
import wbs.apn.chat.core.model.ChatRec;
import wbs.apn.chat.user.core.model.ChatUserRec;
import wbs.apn.chat.user.core.model.ChatUserSessionObjectHelper;
import wbs.apn.chat.user.core.model.ChatUserSessionRec;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.platform.console.request.ConsoleRequestContext;

@PrototypeComponent ("chatGraphsDailyUsersImageResponder")
public
class ChatGraphsDailyUsersImageResponder
	extends MonthlyHistoGraphImageResponder {

	// dependencies

	@Inject
	ChatObjectHelper chatHelper;

	@Inject
	ChatMiscLogic chatMiscLogic;

	@Inject
	ChatUserSessionObjectHelper chatUserSessionHelper;

	@Inject
	ConsoleRequestContext requestContext;

	// state

	DateTimeZone timezone;

	// implementation

	public
	ChatGraphsDailyUsersImageResponder () {

		super (
			640,
			320,
			10);

	}

	@Override
	protected
	DateTimeZone timezone () {
		return timezone;
	}

	@Override
	protected
	void prepareData (
			Instant minTime,
			Instant maxTime) {

		Map<String,Object> searchMap =
			new LinkedHashMap<String,Object> ();

		searchMap.put (
			"startTimeAfter",
			minTime);

		searchMap.put (
			"startTimeBefore",
			maxTime);

		ChatRec chat =
			chatHelper.find (
				requestContext.stuffInt (
					"chatId"));

		searchMap.put (
			"chatId",
			chat.getId ());

		Integer chatAffiliateId =
			(Integer)
			requestContext.contextStuff ().get ("chatAffiliateId");

		if (chatAffiliateId != null) {

			searchMap.put (
				"chatAffiliateId",
				chatAffiliateId);

		}

		Collection<ChatUserSessionRec> chatUserSessions =
			chatUserSessionHelper.search (
				searchMap);

		List<Set<ChatUserRec>> chatUserSets =
			new ArrayList<Set<ChatUserRec>> ();

		for (int i = 0; i < values.size (); i ++) {

			chatUserSets.add (
				new HashSet<ChatUserRec> ());

		}

		timezone =
			chatMiscLogic.timezone (
				chat);

		for (ChatUserSessionRec chatUserSession
				: chatUserSessions) {

			int index =
				+ dateToInstant (
						chatUserSession.getStartTime ())
					.toDateTime (
						timezone)
					.getDayOfMonth ()
				- 1;

			chatUserSets.get (index).add (
				chatUserSession.getChatUser ());

		}

		values =
			new ArrayList<Integer> ();

		for (Set<ChatUserRec> chatUserSet
				: chatUserSets) {

			values.add (
				chatUserSet.size ());

		}

	}

}