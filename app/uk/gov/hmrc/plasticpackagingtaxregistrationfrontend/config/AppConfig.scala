package uk.gov.hmrc.plasticpackagingtaxregistrationfrontend.config

import javax.inject.{Inject, Singleton}
import play.api.Configuration
import uk.gov.hmrc.play.bootstrap.config.ServicesConfig

@Singleton
class AppConfig @Inject()(config: Configuration, servicesConfig: ServicesConfig) {
  private val contactBaseUrl = servicesConfig.baseUrl("contact-frontend")

  private val assetsUrl         = config.get[String]("assets.url")
  private val serviceIdentifier = "MyService"

  val assetsPrefix: String   = assetsUrl + config.get[String]("assets.version")
  val analyticsToken: String = config.get[String](s"google-analytics.token")
  val analyticsHost: String  = config.get[String](s"google-analytics.host")

  val reportAProblemPartialUrl: String = s"$contactBaseUrl/contact/problem_reports_ajax?service=$serviceIdentifier"
  val reportAProblemNonJSUrl: String   = s"$contactBaseUrl/contact/problem_reports_nonjs?service=$serviceIdentifier"

}
