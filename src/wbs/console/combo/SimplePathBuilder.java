package wbs.console.combo;

import javax.inject.Inject;

import wbs.console.annotations.ConsoleModuleBuilderHandler;
import wbs.console.module.ConsoleModuleImpl;
import wbs.console.module.SimpleConsoleBuilderContainer;
import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.context.ApplicationContext;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.web.PathHandler;

@PrototypeComponent ("simplePathBuilder")
@ConsoleModuleBuilderHandler
public
class SimplePathBuilder {

	// dependencies

	@Inject
	ApplicationContext applicationContext;

	// builder

	@BuilderParent
	SimpleConsoleBuilderContainer simpleContainerSpec;

	@BuilderSource
	SimplePathSpec simplePathSpec;

	@BuilderTarget
	ConsoleModuleImpl consoleModule;

	// state

	String path;
	String pathHandlerName;

	// build

	@BuildMethod
	public
	void build (
			Builder builder) {

		setDefaults ();

		buildPath ();

	}

	void buildPath () {

		PathHandler pathHandler =
			applicationContext.getBean (
				pathHandlerName,
				PathHandler.class);

		if (pathHandler == null)
			throw new RuntimeException ();

		consoleModule.addPath (
			path,
			pathHandler);

	}

	// defaults

	void setDefaults () {

		path =
			simplePathSpec.path ();

		pathHandlerName =
			simplePathSpec.pathHandlerName ();

	}

}