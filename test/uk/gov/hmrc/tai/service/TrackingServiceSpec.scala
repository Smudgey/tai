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

package uk.gov.hmrc.tai.service

import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.scalatest.mock.MockitoSugar
import org.scalatestplus.play.PlaySpec
import play.api.test.Helpers._
import uk.gov.hmrc.domain.Generator
import uk.gov.hmrc.http.HeaderCarrier
import uk.gov.hmrc.tai.connectors.TrackingConnector
import uk.gov.hmrc.tai.model.domain.tracking._
import uk.gov.hmrc.tai.repositories.JourneyCacheRepository

import scala.concurrent.Future

class TrackingServiceSpec extends PlaySpec with MockitoSugar {
  val nino: String = new Generator().nextNino.nino
  val noCurrentJourney: Map[String, String] = Map.empty

  def whenTrackingConnectorReturns(forms: Seq[TrackedForm]): Unit =
    when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future successful forms)

  def whenTrackingConnectorFails(): Unit =
    when(trackingConnector.getUserTracking(any())(any())).thenReturn(Future.failed(new RuntimeException("an error occurred")))

  def whenJourneyRepositoryReturns(currentSuccessfulJourneys: Map[String,String]): Unit =
    when(repository.currentCache(any())(any())).thenReturn(Future successful Some(currentSuccessfulJourneys))

  private implicit val hc: HeaderCarrier = HeaderCarrier()

  val repository: JourneyCacheRepository = mock[JourneyCacheRepository]
  val trackingConnector: TrackingConnector = mock[TrackingConnector]

  val sut = new TrackingService(repository, trackingConnector)

  "isAnyIFormInProgress" must {
    "return true" when {
      "there is an iForm with status In Progress" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("TES1", "name1", TrackedFormInProgress)))
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }

      "there is an iForm with status in Acquired" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("TES1", "name1", TrackedFormAcquired)))
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }

      "return true when there is an iForm with status in Received" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("TES1", "name1", TrackedFormReceived)))
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }

      "there is one iForm done and one IForm is in progress" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("TES4", "name1", TrackedFormDone), TrackedForm("TES1", "name1", TrackedFormReceived)))
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }

      "user has completed add employment iFormJourney but tracking service has return empty sequence" in {
        whenTrackingConnectorReturns(Seq.empty)
        whenJourneyRepositoryReturns(Map("TrackSuccessfulJourney_AddEmploymentKey" -> "true"))

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }

      "tracking service throws back an exception but user has completed a journey" in {
        whenTrackingConnectorFails()
        whenJourneyRepositoryReturns(Map("TrackSuccessfulJourney_AddEmploymentKey" -> "true"))

        await(sut.isAnyIFormInProgress(nino)) mustBe true
      }
    }

    "return false" when {
      "there is no iForm in progress" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("TES1", "name1", TrackedFormDone)))
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe false
      }

      "tracking service throws back an exception and there is no current successful journey" in {
        whenTrackingConnectorFails()
        whenJourneyRepositoryReturns(noCurrentJourney)

        await(sut.isAnyIFormInProgress(nino)) mustBe false
      }
    }
  }

  "trackingForTesForms" must {
    "return nil" when {
      "the list from tracking connector has a form that is not TES" in {
        whenTrackingConnectorReturns(Seq(TrackedForm("a1", "name1", TrackedFormDone)))
        await(sut.trackingForTesForms(nino))  mustBe Nil
      }
    }

    "return TES forms from TES1 to TES7" when {
      "the list from tracking connector has TES and non-TES uk.gov.hmrc.tai.forms" in {
        when(trackingConnector.getUserTracking(any())(any())).
          thenReturn(Future.successful(Seq(
            TrackedForm("TES0", "name1", TrackedFormDone),
            TrackedForm("TES1", "name1", TrackedFormDone),
            TrackedForm("TES2", "name1", TrackedFormDone),
            TrackedForm("TES3", "name1", TrackedFormDone),
            TrackedForm("TES4", "name1", TrackedFormDone),
            TrackedForm("TES5", "name1", TrackedFormDone),
            TrackedForm("TES6", "name1", TrackedFormDone),
            TrackedForm("TES7", "name1", TrackedFormDone),
            TrackedForm("TES8", "name1", TrackedFormDone),
            TrackedForm("AAA1", "name1", TrackedFormDone),
            TrackedForm("AAA", "name1", TrackedFormDone))))

        val expectedResult = Seq(
          TrackedForm("TES1", "name1", TrackedFormDone),
          TrackedForm("TES2", "name1", TrackedFormDone),
          TrackedForm("TES3", "name1", TrackedFormDone),
          TrackedForm("TES4", "name1", TrackedFormDone),
          TrackedForm("TES5", "name1", TrackedFormDone),
          TrackedForm("TES6", "name1", TrackedFormDone),
          TrackedForm("TES7", "name1", TrackedFormDone))

        await(sut.trackingForTesForms(new Generator().nextNino.nino)) mustBe expectedResult
      }
    }
  }

}
