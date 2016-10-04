package wbs.apn.chat.user.core.model;

import java.util.List;
import java.util.Map;

import org.joda.time.Instant;

import wbs.apn.chat.affiliate.model.ChatAffiliateRec;
import wbs.apn.chat.category.model.ChatCategoryRec;
import wbs.apn.chat.core.model.ChatRec;
import wbs.sms.number.core.model.NumberRec;

public
interface ChatUserDaoMethods {

	ChatUserRec find (
			ChatRec chat,
			NumberRec number);

	Long countOnline (
			ChatRec chat,
			ChatUserType type);

	List <ChatUserRec> findOnline (
			ChatRec chat);

	List <ChatUserRec> findOnline (
			ChatUserType type);

	List <ChatUserRec> findOnlineOrMonitorCategory (
			ChatRec chat,
			ChatCategoryRec category);

	List <ChatUserRec> findWantingBill (
			ChatRec chat,
			Instant lastAction,
			Long maximumCredit);

	default
	List <ChatUserRec> findWantingBill (
			ChatRec chat,
			Instant lastAction) {

		return findWantingBill (
			chat,
			lastAction,
			0l);

	}

	List <ChatUserRec> findWantingWarning ();

	List <ChatUserRec> findAdultExpiryLimit (
			Instant now,
			int maxResults);

	List <ChatUserRec> find (
			ChatRec chat,
			ChatUserType type,
			Orient orient,
			Gender gender);

	List <ChatUserRec> findWantingJoinOutbound (
			Instant now);

	List <ChatUserRec> findWantingAdultAd (
			Instant now);

	@Deprecated
	List <Long> searchIds (
			Map <String, Object> searchMap);

	List <Long> searchIds (
			ChatUserSearch search);

	List <ChatUserRec> find (
			ChatAffiliateRec chatAffiliate);

	List <ChatUserRec> findDating (
			ChatRec chat);

	List <ChatUserRec> findWantingAd (
			Instant now);

	List <ChatUserRec> findWantingQuietOutbound (
			Instant now);

}