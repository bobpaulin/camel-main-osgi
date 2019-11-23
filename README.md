# camel-main-osgi
A small bundle for starting an OSGi Apache Camel Project.  The bundle starts the CamelContext and registering RouteBuilders from any bundle.

Install bundle

Register an Apache Camel RouteBuilder as an OSGi service.

Using annotations
````
@Component(service = RouteBuilder.class)
public class MyRouteBuilder extends RouteBuilder {
    @Override
    public void configure() throws Exception {
        from("timer:test?fixedRate=true&period=1000")
            .log("Hello");
    }
}
````

Or Manually
````
public void start(BundleContext context) throws Exception {
  context.registerService(RouteBuilder.class, new MyRouteBuilder(), null);
}
````

And it's automatically added or removed to the context from any bundle!

````
Route: route1 started and consuming from: timer://test?fixedRate=true&period=1000
````
