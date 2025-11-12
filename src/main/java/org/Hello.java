package org;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Hello {
    private static final Logger LOGGER = LoggerFactory.getLogger(Hello.class);


    public String sayHello() {
        return "Hello World";
    }

    public static void main(final String[] args) {
        final Hello hello = new Hello();
        LOGGER.info(hello.sayHello());
    }
}
