package wbs.test.simulator.model;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.experimental.Accessors;

import org.apache.commons.lang3.builder.CompareToBuilder;

import wbs.framework.entity.annotations.CodeField;
import wbs.framework.entity.annotations.DeletedField;
import wbs.framework.entity.annotations.DescriptionField;
import wbs.framework.entity.annotations.GeneratedIdField;
import wbs.framework.entity.annotations.MajorEntity;
import wbs.framework.entity.annotations.NameField;
import wbs.framework.entity.annotations.ParentField;
import wbs.framework.record.MajorRecord;
import wbs.framework.record.Record;
import wbs.platform.scaffold.model.SliceRec;

@Accessors (chain = true)
@Data
@EqualsAndHashCode (of = "id")
@ToString (of = "id")
@MajorEntity
public
class SimulatorRec
	implements MajorRecord<SimulatorRec> {

	// id

	@GeneratedIdField
	Integer id;

	// identity

	@ParentField
	SliceRec slice;

	@CodeField
	String code;

	// details

	@NameField
	String name;

	@DescriptionField
	String description;

	@DeletedField
	Boolean deleted = false;

	// compare to

	@Override
	public
	int compareTo (
			Record<SimulatorRec> otherRecord) {

		SimulatorRec other =
			(SimulatorRec) otherRecord;

		return new CompareToBuilder ()

			.append (
				getSlice (),
				other.getSlice ())

			.append (
				getCode (),
				other.getCode ())

			.toComparison ();

	}

}