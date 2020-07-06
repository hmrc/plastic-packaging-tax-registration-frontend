package uk.gov.hmrc.plasticpackagingtaxregistrationfrontend.config

import javax.inject.{Inject, Singleton}

import play.api.i18n.MessagesApi
import play.api.mvc.Request
import play.twirl.api.Html
import uk.gov.hmrc.play.bootstrap.frontend.http.FrontendErrorHandler
import uk.gov.hmrc.plasticpackagingtaxregistrationfrontend.views.html.error_template

@Singleton
class ErrorHandler @Inject()(error_template: error_template, val messagesApi: MessagesApi)(implicit appConfig: AppConfig)
    extends FrontendErrorHandler {

  override def standardErrorTemplate(pageTitle: String, heading: String, message: String)(implicit request: Request[_]): Html =
    error_template(pageTitle, heading, message)
}
