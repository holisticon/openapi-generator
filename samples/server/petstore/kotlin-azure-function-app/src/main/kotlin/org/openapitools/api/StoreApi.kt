package org.openapitools.api

import com.microsoft.azure.functions.HttpRequestMessage



open class StoreApi : StoreAzureFunctionInterface {
  override fun getInventory(
    request: HttpRequestMessage<Void>
  ): StoreAzureFunctionInterface.GetInventoryResult {
    return StoreAzureFunctionInterface.GetInventoryResult.respond200(mapOf("1" to 2 ));
  }
}

