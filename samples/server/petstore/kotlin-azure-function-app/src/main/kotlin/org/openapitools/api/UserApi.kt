package org.openapitools.api

import com.microsoft.azure.functions.HttpRequestMessage
import com.microsoft.azure.functions.HttpStatus
import org.openapitools.api.UserAzureFunctionInterface.*
import org.openapitools.api.model.User

open class UserApi: UserAzureFunctionInterface {
  override fun createUser(
    request: HttpRequestMessage<User>,
    users: Collection<User>,
    user: User
  ): CreateUserResult {
    users.plus(user)
    return CreateUserResult.respond(HttpStatus.CREATED)
  }

  override fun createUsersWithArrayInput(
    request: HttpRequestMessage<Array<User>>,
    users: Collection<User>,
    user: Array<User>
  ): CreateUsersWithArrayInputResult {
    users.plus(user);
    return CreateUsersWithArrayInputResult.respond(HttpStatus.CREATED)
  }

  override fun createUsersWithListInput(
    request: HttpRequestMessage<Array<User>>,
    users: Collection<User>,
    user: Array<User>
  ): CreateUsersWithListInputResult {
    users.plus(user);
    return CreateUsersWithListInputResult.respond(HttpStatus.CREATED)
  }
}