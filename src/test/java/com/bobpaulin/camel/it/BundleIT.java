/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.bobpaulin.camel.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.apache.camel.CamelContext;
import org.apache.camel.Route;
import org.apache.camel.builder.RouteBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerMethod;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerMethod.class)
public class BundleIT {
    @Inject
    private BundleContext bc;
    
    private final File TARGET = new File("target");

    @Configuration
    public Option[] configuration() throws IOException, URISyntaxException, ClassNotFoundException {
        File base = new File(TARGET, "test-bundles");
        return options(
                mavenBundle("org.apache.camel", "camel-core", "2.24.2"),
                mavenBundle("org.apache.camel", "camel-core-osgi", "2.24.2"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-api", "1.8.5"),
                mavenBundle("org.ops4j.pax.logging", "pax-logging-service", "1.8.5"),
                junitBundles(),
                bundle(new File(base, "camel-main-osgi.jar").toURI().toURL().toString()));
    }
    
    @Test
    public void testBundleLoaded() throws Exception {
        boolean hasCore = false, hasOsgi = false, hasDynamicRouteContext = false;
        for (Bundle b : bc.getBundles()) {
            if ("org.apache.camel.camel-core".equals(b.getSymbolicName())) {
                hasCore = true;
                assertEquals("Camel Core not activated", Bundle.ACTIVE, b.getState());
            }
            if ("org.apache.camel.camel-core-osgi".equals(b.getSymbolicName())) {
                hasOsgi = true;
                assertEquals("Camel Core OSGi not activated", Bundle.ACTIVE, b.getState());
            }
            
            if ("com.bobpaulin.camel.main-osgi".equals(b.getSymbolicName())) {
                hasDynamicRouteContext = true;
                assertEquals("Dynamic Route Context not activated", Bundle.ACTIVE, b.getState());
            }
        }
        assertTrue("Core bundle not found", hasCore);
        assertTrue("Osgi bundle not found", hasOsgi);
        assertTrue("Main Osgi bundle not found", hasDynamicRouteContext);
    }
    
    @Test
    public void testRouteLoaded() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        bc.registerService(RouteBuilder.class, new RouteBuilder() {
            
            @Override
            public void configure() throws Exception {
                from("timer:test?fixedRate=true&period=300")
                    .process((exchange)->{
                        latch.countDown();
                    });
                    
                
            }
        }, null);
        
        latch.await(10, TimeUnit.SECONDS);
        
        CamelContext camelContext = bc.getService(bc.getServiceReference(CamelContext.class));
        
        List<Route> routes = camelContext.getRoutes();
        
        assertEquals("There should be one route in the context.", 1, routes.size());
        
    }
    
    
    @Test
    public void testRouteRemoved() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        ServiceRegistration<RouteBuilder> testServiceRegistration = 
                bc.registerService(RouteBuilder.class, new RouteBuilder() {
            
            @Override
            public void configure() throws Exception {
                from("timer:test?fixedRate=true&period=300")
                    .process((exchange)->{
                        latch.countDown();
                    });
            }
        }, null);
        
        latch.await(10, TimeUnit.SECONDS);
        
        testServiceRegistration.unregister();
        
        CamelContext camelContext = bc.getService(bc.getServiceReference(CamelContext.class));
        
        List<Route> routes = camelContext.getRoutes();
        
        assertEquals("There should be no routes in the context.", 0, routes.size());
        
    }

}
