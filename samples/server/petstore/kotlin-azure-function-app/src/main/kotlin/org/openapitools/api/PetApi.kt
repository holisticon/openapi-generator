package org.openapitools.api

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.OutputBinding
import mu.KotlinLogging
import org.openapitools.api.model.Pet
import java.util.*
import java.util.List


open class PetApi : PetAzureFunctionInterface {
  private val logger = KotlinLogging.logger {}

  override fun addPet(
    request: HttpRequestMessage<Pet>,
    petRepository: OutputBinding<Pet>,
    pet: Pet
  ): PetAzureFunctionInterface.AddPetResult {

    logger.warn { ">>>>>>>>>>>>>>>> $pet" }
    petRepository.value = pet
    return PetAzureFunctionInterface.AddPetResult.respond200Json(pet, mapOf("Content-Type" to "application/json"))
  }

  override fun findPetsByStatus(
    request: HttpRequestMessage<Void>,
    petRepository: kotlin.collections.List<Pet>,
    status: kotlin.Array<kotlin.String>,
  ): PetAzureFunctionInterface.FindPetsByStatusResult {
    return PetAzureFunctionInterface.FindPetsByStatusResult.respond200Json(petRepository.toTypedArray())
  }

  override fun findPetsByTags(
    request: HttpRequestMessage<Void>,
    tags: Array<String>
  ): PetAzureFunctionInterface.FindPetsByTagsResult {
    return PetAzureFunctionInterface.FindPetsByTagsResult.respond400("oh yeah")
  }

//  fun findPetsByStatus(
//    request: HttpRequestMessage<Void>,
//    petRepository: List<Pet>,
//    status: Array<String>
//  ): PetAzureFunctionInterface.FindPetsByStatusResult {
//    return PetAzureFunctionInterface.FindPetsByStatusResult.respond200(petRepository.toTypedArray())
//  }
}
