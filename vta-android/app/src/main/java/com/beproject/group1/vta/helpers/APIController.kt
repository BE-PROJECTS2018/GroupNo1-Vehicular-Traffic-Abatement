package com.beproject.group1.vta.helpers

import com.beproject.group1.vta.interfaces.ServiceInterface
import org.json.JSONObject
import com.android.volley.Request
import com.android.volley.Response
import com.beproject.group1.vta.VTAApplication


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

    fun getFileInfo(filename: String, access_token: String, completionHandler: (response: JSONObject?) -> Unit) {
        val containerName = "frozen"
        val path = "TensorFlowExports/$containerName/files/$filename?access_token=$access_token"
        val params = JSONObject()
        service.get(path, params, completionHandler)
    }

    fun downloadFile(filename: String, access_token: String, responseCB: Response.Listener<ByteArray>, errCB: Response.ErrorListener) {
        val mUrl = "${VolleyService.basePath}TensorFlowExports/frozen/download/$filename?access_token=$access_token"
        val request = InputStreamVolleyRequest(Request.Method.GET, mUrl, responseCB, errCB, null)
        VTAApplication.instance?.addToRequestQueue(request)
    }
}