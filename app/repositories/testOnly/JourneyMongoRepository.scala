/*
 * Copyright 2023 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package repositories.testOnly

import com.github.ghik.silencer.silent
import com.google.inject.ImplementedBy
import models.testOnly.{Journey, Language}
import org.mongodb.scala.model.{Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes, ReturnDocument, Updates}
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{OFormat, __}
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.Instant
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[JourneyMongoRepository])
trait JourneyRepository {
  def findByCredId(credId: String): Future[Seq[Journey]]
  def findAll: Future[Seq[Journey]]
}

@Singleton
class JourneyMongoRepository @Inject() (mongoComponent: TestEmailVerificationMongoComponent)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[JourneyMongoRepository.Entity](
    collectionName = "journey",
    mongoComponent = mongoComponent,
    domainFormat   = JourneyMongoRepository.mongoFormat,
    indexes        = Seq(
      IndexModel(Indexes.ascending("createdAt"), IndexOptions().name("ttl").expireAfter(4, TimeUnit.HOURS))
    ),
    replaceIndexes = false
  )
    with JourneyRepository {


  def findByCredId(credId: String): Future[Seq[Journey]] = collection.find(
    Filters.equal("credId", credId))
    .toFuture()
    .map(_.map(_.toJourney))

  def findAll: Future[Seq[Journey]] =
    collection
      .find
      .toFuture()
      .map(_.map(_.toJourney))
}

object JourneyMongoRepository {

  case class Entity(
                     journeyId:                 String,
                     credId:                    String,
                     continueUrl:               String,
                     origin:                    String,
                     accessibilityStatementUrl: String,
                     serviceName:               String,
                     language:                  Language,
                     emailAddress:              Option[String],
                     enterEmailUrl:             Option[String],
                     backUrl:                   Option[String],
                     pageTitle:                 Option[String],
                     passcode:                  String,
                     createdAt:                 Instant,
                     emailAddressAttempts:      Int,
                     passcodesSentToEmail:      Int,
                     passcodeAttempts:          Int
                   ) {
    def toJourney: Journey = Journey(
      journeyId,
      credId,
      continueUrl,
      origin,
      accessibilityStatementUrl,
      serviceName,
      language,
      emailAddress,
      enterEmailUrl,
      backUrl,
      pageTitle,
      passcode,
      emailAddressAttempts,
      passcodesSentToEmail,
      passcodeAttempts
    )
  }

  val mongoFormat: OFormat[Entity] = (
    (__ \ "_id").format[String] and
      (__ \ "credId").format[String] and
      (__ \ "continueUrl").format[String] and
      (__ \ "origin").format[String] and
      (__ \ "accessibilityStatementUrl").format[String] and
      (__ \ "serviceName").format[String] and
      (__ \ "language").format[Language] and
      (__ \ "emailAddress").formatNullable[String] and
      (__ \ "enterEmailUrl").formatNullable[String] and
      (__ \ "backUrl").formatNullable[String] and
      (__ \ "pageTitle").formatNullable[String] and
      (__ \ "passcode").format[String] and
      (__ \ "createdAt").format[Instant](uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats.instantFormat) and
      (__ \ "emailAddressAttempts").format[Int] and
      (__ \ "passcodesSentToEmail").format[Int] and
      (__ \ "passcodeAttempts").format[Int]
    ) (Entity.apply, unlift(Entity.unapply))
}

