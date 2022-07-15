package mytest

import io.kotest.core.spec.style.FreeSpec
import io.kotest.matchers.shouldBe
import org.openapitools.okhttpclient.api.PetApi
import org.openapitools.okhttpclient.model.Pet


class PetApiTest : FreeSpec({
  val baseUrl = "http://localhost:7071/api"


  "add pet" {
    val client = PetApi(baseUrl)
    val pet = Pet("Dieter", emptyList(), "1", null, null, Pet.Status.PENDING)

    val resp = client.addPet(pet)

    resp shouldBe pet
  }
})
