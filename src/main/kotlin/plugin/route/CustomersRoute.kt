package plugin.route

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import model.Account
import model.Customer
import model.Loan
import service.CustomerService
import model.param.CreateCustomerRequest
import model.param.CustomerAccountsAndLoansResponse
import model.param.Response
import service.AccountService
import service.LoanService

fun Route.customers(podName: String, customerService: CustomerService, accountService: AccountService, loanService: LoanService) {
    route("/customers") {
        authenticate("auth-basic") {

            get {
                val customers: List<Customer> = customerService.getAllCustomers()
                call.respond(Response(podName, customers))
            }

            get("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val customer: Customer? = customerService.getCustomer(id)

                when (customer != null) {
                    true ->  call.respond(Response(podName, customer))
                    false -> {
                        val errorMessage = "The requested customer does not exist on pod $podName"
                        call.respond(HttpStatusCode.NotFound, errorMessage)
                    }
                }
            }

            get("/{id}/accounts-and-loans") {
                val id = call.parameters["id"]!!.toInt()
                val customer: Customer? = customerService.getCustomer(id)
                val accounts: List<Account?> = accountService.getAllAccountsForCustomer(id)
                val loans: List<Loan?> = loanService.getAllLoansForCustomer(id)

                if(customer == null) {
                    val errorMessage = "The requested customer does not exist on pod $podName"
                    call.respond(HttpStatusCode.NotFound, errorMessage)
                } else {
                    val response = CustomerAccountsAndLoansResponse(podName, customer, accounts, loans)
                    val json = Json.encodeToString(response)
                    call.respondText(json, contentType = ContentType.Application.Json)
                }
            }

            post {
                val request = call.receive<CreateCustomerRequest>()
                val customer: Customer? = customerService.createCustomer(request)
                call.respond(Response(podName, customer))
            }

            put("/{id}") {
                val id = call.parameters["id"]!!.toInt()
                val request = call.receive<CreateCustomerRequest>()
                val updatedCustomer: Customer? = customerService.updateCustomer(id, request)

                when (updatedCustomer != null) {
                    true -> call.respond(Response(podName, updatedCustomer))
                    false -> {
                        val errorMessage = "The requested customer does not exist on pod $podName"
                        call.respond(HttpStatusCode.NotFound, errorMessage)
                    }
                }
            }

            delete("/{id}") {
                val id = call.parameters["id"]!!.toInt()

                when (customerService.deleteCustomer(id)) {
                    true -> call.respond("All data associated with customer $id has been wiped on pod $podName")
                    false -> {
                        val errorMessage = "The requested customer does not exist on pod $podName"
                        call.respond(HttpStatusCode.NotFound, errorMessage)
                    }
                }
            }
        }
    }
}