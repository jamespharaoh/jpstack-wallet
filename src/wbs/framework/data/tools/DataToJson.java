package wbs.framework.data.tools;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

import lombok.SneakyThrows;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataClass;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public
class DataToJson {

	@SneakyThrows ({
		IllegalAccessException.class
	})
	public
	Object toJson (
			Object dataValue) {

		Class<?> dataClass =
			dataValue.getClass ();

		if (simpleClasses.contains (dataValue.getClass ())) {

			return dataValue;

		} else if (dataValue instanceof List) {

			List<?> dataList =
				(List<?>) dataValue;

			ImmutableList.Builder<Object> jsonListBuilder =
				ImmutableList.<Object>builder ();

			for (
				Object dataListElement
					: dataList
			) {

				jsonListBuilder.add (
					toJson (dataListElement));

			}

			return jsonListBuilder.build ();

		} else {

			DataClass dataClassAnnotation =
				dataClass.getAnnotation (
					DataClass.class);

			if (dataClassAnnotation == null)
				throw new RuntimeException ();

			ImmutableMap.Builder<String,Object> jsonValueBuilder =
				ImmutableMap.<String,Object>builder ();

			for (
				Field field
					: dataClass.getDeclaredFields ()
			) {

				DataAttribute dataAttribute =
					field.getAnnotation (
						DataAttribute.class);

				if (dataAttribute == null)
					continue;

				field.setAccessible (true);

				Object fieldValue =
					field.get (
						dataValue);

				jsonValueBuilder.put (
					field.getName (),
					toJson (fieldValue));

			}

			return jsonValueBuilder.build ();

		}

	}

	// data

	Set<Class<?>> simpleClasses =
		ImmutableSet.<Class<?>>builder ()
			.add (String.class)
			.add (Integer.class)
			.build ();

}