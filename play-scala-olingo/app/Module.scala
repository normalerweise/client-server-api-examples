
import java.time.Clock
import javax.inject.Inject

import com.google.inject.AbstractModule
import odata.OdataComponentInstances
import play.api.http._
import play.api.mvc._
import play.api.routing.Router
import services.{ApplicationTimer, AtomicCounter, Counter}


/**
 * This class is a Guice module that tells Guice how to bind several
 * different types. This Guice module is created when the Play
 * application starts.

 * Play will automatically use any class called `Module` that is in
 * the root package. You can create modules in other locations by
 * adding `play.modules.enabled` settings to the `application.conf`
 * configuration file.
 */
class Module extends AbstractModule {

  override def configure() = {
    // Use the system clock as the default implementation of Clock
    bind(classOf[Clock]).toInstance(Clock.systemDefaultZone)
    // Ask Guice to create an instance of ApplicationTimer when the
    // application starts.
    bind(classOf[ApplicationTimer]).asEagerSingleton()
    // Set AtomicCounter as the implementation for Counter.
    bind(classOf[Counter]).to(classOf[AtomicCounter])
  }

}


class CustomRequestHandler @Inject() (router: Router, errorHandler: HttpErrorHandler,
                                           configuration: HttpConfiguration, filters: HttpFilters) extends DefaultHttpRequestHandler(
  router , errorHandler, configuration, filters
) {

  override def routeRequest(request: RequestHeader) = {
    // hardcoded odata path
    request.path.startsWith("/oData.svc") match {
      case true => Some(OdataComponentInstances.handler.handlerForRequest(request)._2)
      case false => super.routeRequest(request)
    }
  }
}


