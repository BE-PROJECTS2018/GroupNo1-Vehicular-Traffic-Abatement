package com.beproject.group1.vta.helpers

import android.util.Log
import com.android.volley.AuthFailureError
import com.android.volley.Response
import com.android.volley.VolleyLog
import com.android.volley.toolbox.JsonObjectRequest
import com.beproject.group1.vta.VTAApplication
import com.beproject.group1.vta.interfaces.ServiceInterface
import org.json.JSONObject

class VolleyService : ServiceInterface {
    val TAG = VolleyService::class.java.simpleName
    val basePath = "http://139.59.26.224:3000/api/"

    override fun post(path: String, params: JSONObject, completionHandler: (response: JSONObject?) -> Unit) {
        val jsonObjReq = object : JsonObjectRequest(Method.POST, basePath + path, params,
                Response.Listener<JSONObject> { response ->
                    Log.d(TAG, "/post request OK! Response: $response")
                    completionHandler(response)
                },
                Response.ErrorListener { error ->
                    VolleyLog.e(TAG, "/post request fail! Error: ${error.message}")
                    completionHandler(null)
                }) {
            @Throws(AuthFailureError::class)
            override fun getHeaders(): Map<String, String> {
                val headers = HashMap<String, String>()
                headers.put("Content-Type", "application/json")
                return headers
            }
        }

        VTAApplication.instance?.addToRequestQueue(jsonObjReq, TAG)
    }
}