package com.beproject.group1.vta.helpers

import com.beproject.group1.vta.interfaces.ServiceInterface
import org.json.JSONObject

class APIController constructor(serviceInjection: ServiceInterface) {
    private val service: ServiceInterface = serviceInjection

    fun login(params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val path = "Users/login"
        service.post(path, params, completionHandler)
    }

    fun signup(params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val path = "Users"
        service.post(path, params, completionHandler)
    }
}