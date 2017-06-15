package filters

import javax.inject._

import play.api._
import play.api.http.HttpFilters
import org.pac4j.play.filters.SecurityFilter
// This filter is allowed through the application.conf and build.sbt
import play.filters.cors.CORSFilter
/**
 * This class configures filters that run on every request. This
 * class is queried by Play to get a list of filters.
 *
 * Play will automatically use filters from any class called
 * `filters.Filters` that is placed the root package. You can load filters
 * from a different class by adding a `play.http.filters` setting to
 * the `application.conf` configuration file.
 *
 * @param env Basic environment settings for the current application.
 */
@Singleton
class Filters @Inject() (
  env: Environment,
  corsFilter: CORSFilter, securityFilter: SecurityFilter) extends HttpFilters {

  override val filters = {
    // Use the cors filter if we're running development mode. If
    // we're running in production or test mode then don't use any
    // filters at all.
    // I am leaving this commented for future reference
    // if (env.mode == Mode.Dev) Seq(exampleFilter) else Seq.empty
    Seq(corsFilter, securityFilter)
  }

}
