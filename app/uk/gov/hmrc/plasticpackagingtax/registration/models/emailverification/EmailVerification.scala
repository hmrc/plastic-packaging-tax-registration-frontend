package uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification

import play.api.libs.json
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationStatus.EmailVerificationStatus
import uk.gov.hmrc.plasticpackagingtax.registration.models.registration.Registration

object EmailVerificationStatus extends Enumeration {
  type EmailVerificationStatus = Value
  val VERIFIED: Value     = Value
  val LOCKED_OUT: Value   = Value
  val NOT_VERIFIED: Value = Value

  implicit val format: Format[EmailVerificationStatus] =
    json.Format(Reads.enumNameReads(EmailVerificationStatus), Writes.enumNameWrites)

}

object EmailVerificationStatusMapper {

  def toMap(emailStatuses: Seq[EmailStatus]): Map[String, EmailVerificationStatus] =
    emailStatuses.map(email => email.emailAddress -> toStatus(email)).toMap

  private def toStatus(emailStatus: EmailStatus): EmailVerificationStatus =
    if (emailStatus.verified)
      EmailVerificationStatus.VERIFIED
    else if (emailStatus.locked)
      EmailVerificationStatus.LOCKED_OUT
    else
      EmailVerificationStatus.NOT_VERIFIED

}

//class EmailVerificationService(emailVerificationConnector: EmailVerificationConnector) {
//  def getStatus(f: VerificationStatus => Unit)(implicit hc: HeaderCarrier, request: JourneyRequest[AnyContent]): Unit = {
//    request.user.identityData.credentials.g { creds =>
//      emailVerificationConnector.getStatus(creds.providerId).flatMap {
//        case Right(emailStatusResponse) =>
//          emailStatusResponse match {
//            case Some(response) => f(response)
//            case None =>
//              throw DownstreamServiceError(
//                "Error while getting email verification status. No data returned for user."
//              )
//          }
//        case Left(error) => throw error
//      }
//    }
//  }
//}

object EmailVerificationService {

  def getPrimaryEmailStatus(registration: Registration): EmailVerificationStatus =
    registration.primaryContactDetails.email.map(
      email => registration.metaData.getEmailStatus(email)
    ).getOrElse(EmailVerificationStatus.NOT_VERIFIED)

}
