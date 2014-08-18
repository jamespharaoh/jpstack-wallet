package wbs.platform.console.context;

import static wbs.framework.utils.etc.Misc.camelToSpaces;
import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.platform.console.annotations.ConsoleModuleBuilderHandler;
import wbs.platform.console.helper.ConsoleHelper;
import wbs.platform.console.metamodule.ConsoleMetaManager;
import wbs.platform.console.module.ConsoleModuleImpl;
import wbs.platform.console.object.BabyObjectContext;
import wbs.platform.console.tab.ConsoleContextTab;

import com.google.common.collect.Iterables;

@PrototypeComponent ("consoleContextSectionBuilder")
@ConsoleModuleBuilderHandler
public
class ConsoleContextSectionBuilder {

	// dependencies

	@Inject
	ConsoleMetaManager consoleMetaManager;

	// prototype dependencies

	@Inject
	Provider<BabyObjectContext> babyObjectContextProvider;

	@Inject
	Provider<ConsoleContextTab> contextTabProvider;

	@Inject
	Provider<ConsoleContextType> contextTypeProvider;

	// builder

	@BuilderParent
	ConsoleContextBuilderContainer container;

	@BuilderSource
	ConsoleContextSectionSpec spec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// state

	ConsoleHelper<?> consoleHelper;

	String name;
	String structuralName;
	String aliasOf;
	String label;
	String contextTypeName;
	String tabName;
	String tabTarget;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		buildContextTypes ();

		for (ResolvedConsoleContextExtensionPoint resolvedExtensionPoint
				: consoleMetaManager.resolveExtensionPoint (
					container.extensionPointName ())) {

			buildContexts (
				resolvedExtensionPoint);

			buildContextTabs (
				resolvedExtensionPoint);

		}

		ConsoleContextBuilderContainer nextContextBuilderContainer =
			new ConsoleContextBuilderContainerImpl ()

			.consoleHelper (
				consoleHelper)

			.structuralName (
				structuralName)

			.extensionPointName (
				"section:" + contextTypeName)

			.pathPrefix (
				contextTypeName)

			.existingBeanNamePrefix (
				stringFormat (
					"%s%s",
					container.existingBeanNamePrefix (),
					capitalise (aliasOf)))

			.newBeanNamePrefix (
				stringFormat (
					"%s%s",
					container.newBeanNamePrefix (),
					capitalise (name)))

			.tabLocation (
				"end")

			.friendlyName (
				consoleHelper.friendlyName ());

		builder.descend (
			nextContextBuilderContainer,
			spec.children (),
			consoleModule);

	}

	void buildContextTypes () {

		consoleModule.addContextType (
			contextTypeProvider.get ()

			.name (
				contextTypeName));

	}

	void buildContexts (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		Map<String,Object> stuffMap =
			new HashMap<String,Object> ();

		for (ConsoleContextStuffSpec contextStuffSpec
				: Iterables.filter (
					spec.children (),
					ConsoleContextStuffSpec.class)) {

			stuffMap.put (
				contextStuffSpec.name (),
				contextStuffSpec.value ());

		}

		for (String parentContextName
				: resolvedExtensionPoint.parentContextNames ()) {

			consoleModule.addContext (
				babyObjectContextProvider.get ()

				.name (
					stringFormat (
						"%s.%s",
						parentContextName,
						spec.name ()))

				.typeName (
					contextTypeName)

				.pathPrefix (
					stringFormat (
						"/%s.%s",
						parentContextName,
						spec.name ()))

				.global (
					true)

				.title (
					label)

				.parentContextName (
					parentContextName)

				.parentContextTabName (
					tabName)

				.stuff (
					stuffMap));

			consoleModule.addContext (
				babyObjectContextProvider.get ()

				.name (
					stringFormat (
						"link:%s.%s",
						parentContextName,
						spec.name ()))

				.typeName (
					contextTypeName)

				.pathPrefix (
					stringFormat (
						"/%s.%s",
						parentContextName,
						spec.name ()))

				.global (
					false)

				.title (
					label)

				.parentContextName (
					"link:" + parentContextName)

				.parentContextTabName (
					tabName)

				.stuff (
					stuffMap));

		}

	}

	void buildContextTabs (
			ResolvedConsoleContextExtensionPoint resolvedExtensionPoint) {

		consoleModule.addContextTab (

			container.tabLocation (),

			contextTabProvider.get ()

				.name (
					tabName)

				.defaultLabel (
					label)

				.localFile (
					tabTarget),

			resolvedExtensionPoint.contextTypeNames ());

	}

	void setDefaults () {

		consoleHelper =
			container.consoleHelper ();

		name =
			spec.name ();

		structuralName =
			stringFormat (
				"%s.%s",
				container.structuralName (),
				name);

		aliasOf =
			ifNull (
				spec.aliasOf (),
				name);

		label =
			ifNull (
				spec.label (),
				camelToSpaces (name));

		contextTypeName =
			structuralName;

		tabName =
			structuralName;

		tabTarget =
			stringFormat (
				"type:%s",
				contextTypeName);

	}

}