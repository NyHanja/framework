package mg.itu.model;

import java.lang.reflect.Method;


public class RouteMapping {

    private Class<?> controller;
    private Method method;


    public RouteMapping(Class<?> controller, Method method) {

        this.controller = controller;
        this.method = method;

    }


    public Class<?> getController() {

        return controller;

    }


    public Method getMethod() {

        return method;

    }

}