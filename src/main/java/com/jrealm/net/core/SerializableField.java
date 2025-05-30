package com.jrealm.net.core;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.LOCAL_VARIABLE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Retention(RUNTIME)
@Target({ FIELD, LOCAL_VARIABLE })
public @interface SerializableField {
	int order();
	Class<? extends SerializableFieldType<?>> type() default EmptyField.class; 
	boolean  isCollection() default false; 

}
