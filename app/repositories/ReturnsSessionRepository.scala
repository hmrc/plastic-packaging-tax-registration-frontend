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

package repositories

import config.AppConfig
import org.mongodb.scala.bson.conversions.Bson
import org.mongodb.scala.model.Filters
import play.api.libs.functional.syntax.{toFunctionalBuilderOps, unlift}
import play.api.libs.json.{Format, JsObject, JsPath, Reads}
import repositories.ReturnsSessionRepository.Entry
import repositories.ReturnsSessionRepository.Paths.AgentSelectedPPTRef
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository
import uk.gov.hmrc.mongo.play.json.formats.MongoJavatimeFormats
import uk.gov.hmrc.mongo.MongoComponent

import java.time.Instant
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class ReturnsSessionRepository @Inject() (
                                           appConfig: AppConfig
                                         )(implicit ec: ExecutionContext)
  extends PlayMongoRepository[Entry](
    mongoComponent = MongoComponent.apply(appConfig.returnsSessionCacheUri),
    collectionName = "session-cache",
    domainFormat = Entry.format,
    indexes = Seq.empty,
    replaceIndexes = false
  ) {

  private def byId(id: String): Bson = Filters.equal("_id", id)

  private def get(id: String): Future[Option[Entry]] =
    collection.find(byId(id)).headOption

  def get[A](id: String, path: JsPath)(implicit rds: Reads[A]): Future[Option[A]] =
    get(id).map{_.flatMap{ entry =>
      Reads.optionNoError(Reads.at(path)(rds)).reads(entry.data).getOrElse(None)
    }}

  def getAgentSelectedPPTRef(id: String): Future[Option[String]] = get[String](id, AgentSelectedPPTRef)

}
object ReturnsSessionRepository {

  final case class Entry(
                          id: String,
                          data: JsObject = JsObject.empty,
                          lastUpdated: Instant = Instant.now()
                        )

  object Entry {
    implicit val format: Format[Entry] = Format(
      (
        (JsPath \ "_id").read[String] and
          (JsPath \ "data").read[JsObject] and
          (JsPath \ "lastUpdated").read[Instant](MongoJavatimeFormats.instantFormat)
        )(Entry.apply _),
      (
        (JsPath \ "_id").write[String] and
          (JsPath \ "data").write[JsObject] and
          (JsPath \ "lastUpdated").write[Instant](MongoJavatimeFormats.instantFormat)
        )(unlift(Entry.unapply))
    )
  }

  object Paths {
    val AmendChargeRef: JsPath = JsPath \ "AmendChargeRef"
    val ReturnChargeRef: JsPath = JsPath \ "ReturnChargeRef"
    val TaxReturnObligation: JsPath = JsPath \ "TaxReturnObligation"
    val SubscriptionIsActive: JsPath = JsPath \ "SubscriptionIsActive"
    val AgentSelectedPPTRef: JsPath = JsPath \ "AgentSelectedPPTRef"
  }

}