package wbs.console.priv;

import java.util.Collection;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;

import lombok.NonNull;
import lombok.experimental.Accessors;

import wbs.console.misc.ConsoleUserHelper;
import wbs.console.request.ConsoleRequestContext;
import wbs.framework.application.annotations.ProxiedRequestComponent;
import wbs.framework.record.GlobalId;
import wbs.framework.record.Record;

@Accessors (fluent = true)
@ProxiedRequestComponent (
	value = "userPrivChecker",
	proxyInterface = UserPrivChecker.class)
public
class UserPrivCheckerImplementation
	implements UserPrivChecker {

	// dependencies

	@Inject
	ConsoleUserHelper consoleUserHelper;

	@Inject
	UserPrivDataLoader privDataLoader;

	@Inject
	ConsoleRequestContext requestContext;

	// prototype dependencies

	@Inject
	Provider<UserPrivCheckerBuilder> privCheckerBuilderProvider;

	// state

	UserPrivChecker target;

	// lifecycle

	@PostConstruct
	public
	void init () {

		if (! consoleUserHelper.loggedIn ()) {

			target = null;

		} else {

			target =
				privCheckerBuilderProvider.get ()

				.userId (
					consoleUserHelper.loggedInUserIdRequired ())

				.build ();

		}

	}

	// implementation

	@Override
	public
	boolean canRecursive (
			int privId) {

		return target.canRecursive (
			privId);

	}

	@Override
	public
	boolean canRecursive (
			GlobalId parentGlobalId,
			String... privCodes) {

		return target.canRecursive (
			parentGlobalId,
			privCodes);

	}

	@Override
	public
	boolean canRecursive (
			Class<? extends Record<?>> parentClass,
			int parentId,
			String... privCodes) {

		return target.canRecursive (
			parentClass,
			parentId,
			privCodes);

	}

	@Override
	public
	boolean canRecursive (
			@NonNull Record<?> parentObject,
			@NonNull String... privCodes) {

		return target.canRecursive (
			parentObject,
			privCodes);

	}

	@Override
	public
	boolean canSimple (
			GlobalId parentGlobalId,
			String... privCodes) {

		return target.canSimple (
			parentGlobalId,
			privCodes);

	}

	@Override
	public
	boolean canSimple (
			@NonNull Record<?> parentObject,
			@NonNull String... privCodes) {

		return target.canSimple (
			parentObject,
			privCodes);

	}

	@Override
	public
	boolean canRecursive (
			Map<Object,Collection<String>> map) {

		return target.canRecursive (
			map);

	}

	@Override
	public
	boolean canGrant (
			int privId) {

		return target.canGrant (
			privId);

	}

	@Override
	public
	void refresh () {

		privDataLoader.refresh ();

		init ();

	}

}
