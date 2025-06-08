package com.icsd.springor;

import com.google.ortools.Loader;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//@SpringBootApplication(exclude = {
//    org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration.class
//})
//public class Application {
//    public static void main(String[] args) {
//        //System.loadLibrary("jniortools");
//        Loader.loadNativeLibraries();
//        SpringApplication.run(Application.class, args);
//    }
//}

@SpringBootApplication
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}