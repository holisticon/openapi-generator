package org.openapitools.api

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.OutputBinding
import org.openapitools.api.model.Pet


open class PetApi : PetAzureFunctionInterface {
  override fun addPet(
    request: HttpRequestMessage<Pet>,
    petRepository: OutputBinding<Pet>,
    pet: Pet
  ): PetAzureFunctionInterface.AddPetResult {
    petRepository.value = pet
    return PetAzureFunctionInterface.AddPetResult.respond200(pet)
  }

}
