/*
 * Copyright 2018 HM Revenue & Customs
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

package uk.gov.hmrc.tai.connectors

import com.google.inject.{Inject, Singleton}
import play.api.{Configuration, Environment}
import play.api.Mode.Mode
import uk.gov.hmrc.tai.model.domain.tracking.TrackedForm
import uk.gov.hmrc.tai.model.domain.tracking.formatter.TrackedFormFormatters
import uk.gov.hmrc.play.config.ServicesConfig

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.play.http.ws.WSHttp

@Singleton
class TrackingConnector @Inject()(environment: Environment, wsHttp: WSHttp, conf: Configuration) extends TrackedFormFormatters with ServicesConfig {
  val serviceUrl: String = baseUrl("tracking")

  override protected def mode: Mode = environment.mode

  override protected def runModeConfiguration: Configuration = conf

  private val IdType = "nino"

  def trackingUrl(id: String) = s"$serviceUrl/tracking-data/user/$IdType/$id"

  def getUserTracking(nino: String)(implicit hc: HeaderCarrier): Future[Seq[TrackedForm]] = {
    wsHttp.doGet(trackingUrl(nino)) map {
      _.json.as[Seq[TrackedForm]](trackedFormSeqReads)
    }
  }

}
