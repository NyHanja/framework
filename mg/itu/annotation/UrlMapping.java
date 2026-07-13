package mg.itu.annotation;

import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface UrlMapping {
    String value();
    String method(); //GET / POST
}
