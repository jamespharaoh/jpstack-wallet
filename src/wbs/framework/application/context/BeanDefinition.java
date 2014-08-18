package wbs.framework.application.context;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import wbs.framework.data.annotations.DataAttribute;
import wbs.framework.data.annotations.DataChildren;
import wbs.framework.data.annotations.DataClass;
import wbs.framework.data.annotations.DataName;

@Accessors (fluent = true)
@DataClass
public
class BeanDefinition {

	@DataName
	@Getter @Setter
	String name;

	@DataAttribute
	@Getter @Setter
	String scope;

	@DataAttribute
	@Getter @Setter
	Class<?> beanClass;

	@DataAttribute
	@Getter @Setter
	Class<? extends BeanFactory> factoryClass;

	@DataAttribute
	@Getter @Setter
	Boolean hide = false;

	@DataChildren
	@Getter @Setter
	Map<String,Object> valueProperties =
		new LinkedHashMap<String,Object> ();

	@DataChildren
	@Getter @Setter
	Map<String,String> referenceProperties =
		new LinkedHashMap<String,String> ();

	@DataChildren
	@Getter @Setter
	List<InjectedProperty> injectedProperties =
		new ArrayList<InjectedProperty> ();

	@DataChildren
	@Getter @Setter
	Set<String> orderedDependencies =
		new HashSet<String> ();

	public
	BeanDefinition addValueProperty (
			String name,
			Object value) {

		valueProperties.put (
			name,
			value);

		return this;

	}

	public
	BeanDefinition addReferenceProperty (
			String name,
			String referencedBeanName) {

		referenceProperties.put (
			name,
			referencedBeanName);

		return this;

	}

}