package wbs.framework.entity.generate;

import static wbs.framework.utils.etc.Misc.capitalise;
import static wbs.framework.utils.etc.Misc.ifNull;
import static wbs.framework.utils.etc.Misc.stringFormat;

import java.io.IOException;

import javax.inject.Inject;

import wbs.framework.application.annotations.PrototypeComponent;
import wbs.framework.application.scaffold.PluginCustomTypeSpec;
import wbs.framework.application.scaffold.PluginEnumTypeSpec;
import wbs.framework.application.scaffold.PluginManager;
import wbs.framework.application.scaffold.PluginSpec;
import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.entity.meta.AnnotationWriter;
import wbs.framework.entity.meta.EnumFieldSpec;
import wbs.framework.entity.meta.ModelMetaSpec;
import wbs.framework.utils.etc.FormatWriter;

@PrototypeComponent ("enumFieldWriter")
@ModelWriter
public
class EnumFieldWriter {

	// dependencies

	@Inject
	PluginManager pluginManager;

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	EnumFieldSpec spec;

	@BuilderTarget
	FormatWriter javaWriter;

	// build

	@BuildMethod
	public
	void build (
			Builder builder)
		throws IOException {

		String fieldTypePackageName;

		PluginEnumTypeSpec fieldTypePluginEnumType =
			pluginManager.pluginEnumTypesByName ().get (
				spec.typeName ());

		PluginCustomTypeSpec fieldTypePluginCustomType =
			pluginManager.pluginCustomTypesByName ().get (
				spec.typeName ());

		if (fieldTypePluginEnumType != null) {

			PluginSpec fieldTypePlugin =
				fieldTypePluginEnumType.plugin ();

			fieldTypePackageName =
				fieldTypePlugin.packageName ();

		} else if (fieldTypePluginCustomType != null) {

			PluginSpec fieldTypePlugin =
				fieldTypePluginCustomType.plugin ();

			fieldTypePackageName =
				fieldTypePlugin.packageName ();

		} else {

			throw new RuntimeException (
				stringFormat (
					"No such enum or custom type: %s",
					spec.typeName ()));

		}

		String fieldName =
			ifNull (
				spec.name (),
				spec.typeName ());

		// write field annotation

		AnnotationWriter annotationWriter =
			new AnnotationWriter ()

			.name (
				"SimpleField");

		if (ifNull (spec.nullable (), false)) {

			annotationWriter.addAttributeFormat (
				"nullable",
				"true");

		}

		if (spec.columnName () != null) {

			annotationWriter.addAttributeFormat (
				"column",
				"\"%s\"",
				spec.columnName ().replace ("\"", "\\\""));

		}

		annotationWriter.write (
			javaWriter,
			"\t");

		// write field

		if (spec.defaultValue () != null) {

			javaWriter.writeFormat (
				"\t%s.model.%s %s =\n",
				fieldTypePackageName,
				capitalise (
					spec.typeName ()),
				fieldName);

			javaWriter.writeFormat (
				 "\t\t%s.model.%s.%s;\n",
				fieldTypePackageName,
				capitalise (
					spec.typeName ()),
				spec.defaultValue ());

		} else {

			javaWriter.writeFormat (
				"\t%s.model.%s %s;\n",
				fieldTypePackageName,
				capitalise (
					spec.typeName ()),
				fieldName);

		}

		javaWriter.writeFormat (
			"\n");

	}

}
