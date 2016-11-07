package wbs.sms.route.router.fixture;

import static wbs.utils.etc.NullUtils.ifNull;
import static wbs.utils.string.CodeUtils.simplifyToCodeRequired;
import static wbs.utils.string.StringUtils.camelToUnderscore;
import static wbs.utils.string.StringUtils.stringFormat;

import java.sql.SQLException;

import lombok.Cleanup;
import lombok.NonNull;
import lombok.extern.log4j.Log4j;

import wbs.framework.builder.Builder;
import wbs.framework.builder.annotations.BuildMethod;
import wbs.framework.builder.annotations.BuilderParent;
import wbs.framework.builder.annotations.BuilderSource;
import wbs.framework.builder.annotations.BuilderTarget;
import wbs.framework.component.annotations.PrototypeComponent;
import wbs.framework.component.annotations.SingletonDependency;
import wbs.framework.database.Database;
import wbs.framework.database.Transaction;
import wbs.framework.entity.fixtures.ModelMetaBuilderHandler;
import wbs.framework.entity.helper.EntityHelper;
import wbs.framework.entity.meta.model.ModelMetaSpec;
import wbs.framework.entity.model.Model;
import wbs.framework.entity.record.GlobalId;
import wbs.platform.object.core.model.ObjectTypeObjectHelper;
import wbs.platform.object.core.model.ObjectTypeRec;
import wbs.sms.route.router.metamodel.RouterTypeSpec;
import wbs.sms.route.router.model.RouterTypeObjectHelper;

@Log4j
@PrototypeComponent ("routerTypeBuilder")
@ModelMetaBuilderHandler
public
class RouterTypeBuilder {

	// singleton dependencies

	@SingletonDependency
	Database database;

	@SingletonDependency
	EntityHelper entityHelper;

	@SingletonDependency
	ObjectTypeObjectHelper objectTypeHelper;

	@SingletonDependency
	RouterTypeObjectHelper routerTypeHelper;

	// builder

	@BuilderParent
	ModelMetaSpec parent;

	@BuilderSource
	RouterTypeSpec spec;

	@BuilderTarget
	Model <?> model;

	// build

	@BuildMethod
	public
	void build (
			@NonNull Builder builder) {

		try {

			log.info (
				stringFormat (
					"Create router type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					simplifyToCodeRequired (
						spec.name ())));

			createRouterType ();

		} catch (Exception exception) {

			throw new RuntimeException (
				stringFormat (
					"Error creating router type %s.%s",
					camelToUnderscore (
						ifNull (
							spec.subject (),
							parent.name ())),
					simplifyToCodeRequired (
						spec.name ())),
				exception);

		}

	}

	private
	void createRouterType ()
		throws SQLException {

		// begin transaction

		@Cleanup
		Transaction transaction =
			database.beginReadWrite (
				"RouterTypeBuilder.createRouterType ()",
				this);

		// lookup parent type

		String parentTypeCode =
			camelToUnderscore (
				ifNull (
					spec.subject (),
					parent.name ()));

		ObjectTypeRec parentType =
			objectTypeHelper.findByCodeRequired (
				GlobalId.root,
				parentTypeCode);

		// create router type

		routerTypeHelper.insert (
			routerTypeHelper.createInstance ()

			.setParentType (
				parentType)

			.setCode (
				simplifyToCodeRequired (
					spec.name ()))

			.setDescription (
				spec.description ())

		);

		// commit transaction

		transaction.commit ();

	}

}
