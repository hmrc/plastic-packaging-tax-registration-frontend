package uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification

import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.plasticpackagingtax.registration.connectors.EmailVerificationConnector

class EmailVerificationService(emailVerificationConnector: EmailVerificationConnector) {

  def isVerified(email: String, emailStatuses: Seq[EmailStatus]): Boolean = {
    val ea = Map("email@gmail.com" -> Status(true, false))

    return ea.get("email@gmail.com").filter(s => s.verified).getOrElse(false)
  }

  def createEmailVerification()(implicit hc: HeaderCarrier) =
    emailVerificationConnector.create(
      CreateEmailVerificationRequest(credId = "credId",
                                     continueUrl = "http://continue",
                                     origin = "origin",
                                     accessibilityStatementUrl = "http://accessibility",
                                     email = Email(address = "test@hmrc.com",
                                                   enterUrl = "hhtp://enterUrl"
                                     ),
                                     backUrl = "http://back",
                                     pageTitle = "PPT Title",
                                     deskproServiceName = "ppt"
      )
    )

}
