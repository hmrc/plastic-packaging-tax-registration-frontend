package uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification

import play.api.libs.json
import play.api.libs.json.{Format, Reads, Writes}
import uk.gov.hmrc.plasticpackagingtax.registration.models.emailverification.EmailVerificationStatus.EmailVerificationStatus

import java.text.Format

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
