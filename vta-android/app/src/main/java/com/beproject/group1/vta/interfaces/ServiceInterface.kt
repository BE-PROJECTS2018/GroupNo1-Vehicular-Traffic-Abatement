package com.beproject.group1.vta.interfaces

import org.json.JSONObject

/**
 * Created by pavan on 2/1/18.
 */
interface ServiceInterface {
    fun post(path: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit)
}