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

import config.AppConfig
import models.testOnly.PasscodeDoc
import org.mongodb.scala.model.{CountOptions, Filters, FindOneAndUpdateOptions, IndexModel, IndexOptions, Indexes, ReturnDocument, Updates}
import uk.gov.hmrc.http.SessionId
import uk.gov.hmrc.mongo.MongoComponent
import uk.gov.hmrc.mongo.play.json.PlayMongoRepository

import java.time.{Clock, Instant}
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@Singleton
class PasscodeMongoRepository @Inject() (mongoComponent: TestEmailVerificationMongoComponent, config: AppConfig)(implicit ec: ExecutionContext)
  extends PlayMongoRepository[PasscodeDoc](
    collectionName = "passcode",
    mongoComponent = mongoComponent,
    domainFormat   = PasscodeDoc.format,
    indexes        = Seq(
      IndexModel(Indexes.ascending("sessionId", "email"), IndexOptions().name("sessionIdUnique").unique(true)),
      IndexModel(Indexes.ascending("expireAt"), IndexOptions().name("expireAtIndex").expireAfter(0, TimeUnit.SECONDS))
    ),
    replaceIndexes = false
  ) {

  def findPasscodesBySessionId(sessionId: String): Future[Seq[PasscodeDoc]] =
    collection.find(Filters.equal("sessionId", sessionId)).toFuture()

}


