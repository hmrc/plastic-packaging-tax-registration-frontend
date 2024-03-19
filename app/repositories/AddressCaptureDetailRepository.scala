/*
 * Copyright 2024 HM Revenue & Customs
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

import com.google.inject.ImplementedBy
import models.request.AuthenticatedRequest
import repositories.AddressCaptureDetailRepository.repositoryKey
import services.AddressCaptureDetail

import javax.inject.{Inject, Singleton}
import scala.concurrent.{ExecutionContext, Future}

@ImplementedBy(classOf[AddressCaptureDetailRepositoryImpl])
trait AddressCaptureDetailRepository {

  def put(addressCaptureDetail: AddressCaptureDetail)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[AddressCaptureDetail]

  def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[AddressCaptureDetail]]

  def update(update: AddressCaptureDetail => AddressCaptureDetail)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[AddressCaptureDetail]

  def delete()(implicit request: AuthenticatedRequest[Any]): Future[Unit]
}

@Singleton
class AddressCaptureDetailRepositoryImpl @Inject() (userDataRepository: UserDataRepository)(implicit
  ec: ExecutionContext
) extends AddressCaptureDetailRepository {

  def put(addressCaptureDetail: AddressCaptureDetail)(implicit
    request: AuthenticatedRequest[Any]
  ): Future[AddressCaptureDetail] =
    userDataRepository.putData[AddressCaptureDetail](repositoryKey, addressCaptureDetail)

  def get()(implicit request: AuthenticatedRequest[Any]): Future[Option[AddressCaptureDetail]] =
    userDataRepository.getData[AddressCaptureDetail](repositoryKey)

  def update(
    update: AddressCaptureDetail => AddressCaptureDetail
  )(implicit request: AuthenticatedRequest[Any]): Future[AddressCaptureDetail] =
    get().flatMap {
      case Some(addressCaptureDetail) => put(update(addressCaptureDetail))
      case _                          => throw new IllegalStateException("Existing address capture detail not found")
    }(ec)

  def delete()(implicit request: AuthenticatedRequest[Any]): Future[Unit] =
    userDataRepository.deleteData[AddressCaptureDetail](repositoryKey)

}

object AddressCaptureDetailRepository {
  val repositoryKey = "addressCaptureDetails"
}
