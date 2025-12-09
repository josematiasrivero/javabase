package com.adavance.javabase.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark entities that should be automatically exposed
 * via the GenericRestController.
 * 
 * Only entities annotated with @AutoController will be accessible
 * through the /rest/{entity-name} endpoints.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoController {
}

