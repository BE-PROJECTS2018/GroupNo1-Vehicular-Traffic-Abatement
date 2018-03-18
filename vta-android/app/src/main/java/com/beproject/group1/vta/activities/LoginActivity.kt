package com.beproject.group1.vta.activities

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.annotation.TargetApi
import android.content.pm.PackageManager
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.app.LoaderManager.LoaderCallbacks
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.ContactsContract
import android.text.TextUtils
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.TextView

import android.Manifest.permission.READ_CONTACTS
import android.content.*
import android.util.Log
import android.util.Patterns
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.android.volley.Response
import com.beproject.group1.vta.R
import com.beproject.group1.vta.VTAApplication
import com.beproject.group1.vta.helpers.APIController
import com.beproject.group1.vta.helpers.ExtraTreesClassifier
//import com.beproject.group1.vta.helpers.TFPredictor.Companion.model_name
import com.beproject.group1.vta.helpers.VolleyService

import kotlinx.android.synthetic.main.activity_login.*
import org.jetbrains.anko.doAsync
import org.jetbrains.anko.uiThread
import org.json.JSONObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream

/**
 * A login screen that offers login via email/password.
 */
class LoginActivity : AppCompatActivity(), LoaderCallbacks<Cursor> {
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private var isLoggingIn = false
    private lateinit var service: VolleyService
    private lateinit var apiController: APIController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        // Set up volley
        
        service = VolleyService()
        apiController = APIController(service)
        // Set up the login form.
        populateAutoComplete()
        password.setOnEditorActionListener(TextView.OnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                attemptLogin()
                return@OnEditorActionListener true
            }
            false
        })
        email.setOnEditorActionListener(TextView.OnEditorActionListener{ _, id, _ ->
            if(id == EditorInfo.IME_ACTION_NEXT) {
                password.requestFocus()
                return@OnEditorActionListener true
            }
            false
        })

        email_sign_in_button.setOnClickListener { attemptLogin() }
        sign_up_link.setOnClickListener({_ ->
            val intent = Intent(baseContext, SignUpActivity::class.java)
            startActivity(intent)
        })
    }

    private fun populateAutoComplete() {
        if (!mayRequestContacts()) {
            return
        }

        loaderManager.initLoader(0, null, this)
    }

    private fun mayRequestContacts(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            return true
        }
        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
            Snackbar.make(email, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
                    .setAction(android.R.string.ok,
                            { requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS) })
        } else {
            requestPermissions(arrayOf(READ_CONTACTS), REQUEST_READ_CONTACTS)
        }
        return false
    }

    /**
     * Callback received when a permissions request has been completed.
     */
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == REQUEST_READ_CONTACTS) {
            if (grantResults.size == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                populateAutoComplete()
            }
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private fun attemptLogin() {
        if(isLoggingIn) {
            return
        }

        // Reset errors.
        email.error = null
        password.error = null

        // Store values at the time of the login attempt.
        val emailStr = email.text.toString()
        val passwordStr = password.text.toString()

        var cancel = false
        var focusView: View? = null

        // Check for a valid password, if the user entered one.
        if (!TextUtils.isEmpty(passwordStr) && !isPasswordValid(passwordStr)) {
            password.error = getString(R.string.error_invalid_password)
            focusView = password
            cancel = true
        }

        // Check for a valid email address.
        if (TextUtils.isEmpty(emailStr)) {
            email.error = getString(R.string.error_field_required)
            focusView = email
            cancel = true
        } else if (!isEmailValid(emailStr)) {
            email.error = getString(R.string.error_invalid_email)
            focusView = email
            cancel = true
        }

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView?.requestFocus()
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            showProgress(true)
            val params = JSONObject()
            params.put("email", emailStr)
            params.put("password", passwordStr)
            apiController.login(params) {response ->

                if(response == null) {
                    isLoggingIn = false
                    showProgress(false)
                    password.error = getString(R.string.error_incorrect_password)
                    password.requestFocus()
                } else {
                    val sp = getSharedPreferences(VTAApplication.PREF_FILE, Context.MODE_PRIVATE)
                    val spe = sp.edit()
                    for (key in response.keys()) {
                        Log.d("RESP", key)
                        spe.putString(key, response.getString(key))
                    }
                    spe.putString("email", emailStr)
                    spe.putString("password", passwordStr)
                    spe.apply()
                    sklearnSync(sp)
//                    maybeSync(sp)
                }

            }
        }
    }

    private fun sklearnSync(sp: SharedPreferences) {
        val accessToken = sp.getString("id", null)
        val smtime = sp.getString("smtime", null)
        val intent = Intent(this@LoginActivity, MapsActivity::class.java)
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = tz
        apiController.getFileInfo("${ExtraTreesClassifier.model_name}.zip", accessToken){ response ->
            if(response == null) {
                Toast.makeText(applicationContext, getString(R.string.offline_alert), Toast.LENGTH_SHORT).show()
                this@LoginActivity.startActivity(intent)
                this@LoginActivity.finish()
            } else {
                val newsmtime = response.getString("mtime")
                if(smtime == null) {
                    val spe = sp.edit()
                    spe.putString("smtime", newsmtime)
                    spe.apply()
                    syncSklearnAndLaunchApp(intent, accessToken)
                } else {
                    val date1 = sdf.parse(smtime)
                    val date2 = sdf.parse(newsmtime)
                    if (date1.before(date2)) {
                        val spe = sp.edit()
                        spe.putString("smtime", newsmtime)
                        spe.apply()
                        syncSklearnAndLaunchApp(intent, accessToken)
                    } else {
                        this@LoginActivity.startActivity(intent)
                        this@LoginActivity.finish()
                    }
                }
            }
        }
    }

    private fun syncSklearnAndLaunchApp(intent: Intent, accessToken: String) {
        sync_message.visibility = View.VISIBLE
        apiController.downloadFile("${ExtraTreesClassifier.model_name}.zip", accessToken, Response.Listener<ByteArray> { response ->
            try {
                if (response != null) {
                    val outputStream: FileOutputStream
                    val name = "${ExtraTreesClassifier.model_name}.zip"
                    outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                    outputStream.write(response)
                    outputStream.close()
                    Toast.makeText(this, "Model Download complete.", Toast.LENGTH_LONG).show()
                    doAsync {
                        val b = unpackZip("${filesDir.absolutePath}/", name)
                        uiThread {
                            Toast.makeText(applicationContext, "Model unzip: $b.", Toast.LENGTH_LONG).show()
                        }
                    }
                    apiController.downloadFile("${ExtraTreesClassifier.map_prefix}-map.zip", accessToken, Response.Listener<ByteArray> {res ->
                        try {
                            if (res != null) {
                                val os: FileOutputStream
                                val zname = "${ExtraTreesClassifier.map_prefix}-map.zip"
                                os = openFileOutput(zname, Context.MODE_PRIVATE)
                                os.write(res)
                                os.close()
                                Toast.makeText(this, "Mapping Download complete.", Toast.LENGTH_LONG).show()
                                doAsync {
                                    val zb = unpackZip("${filesDir.absolutePath}/", zname)
                                    uiThread {
                                        Toast.makeText(applicationContext, "Mapping unzip: $zb.", Toast.LENGTH_LONG).show()
                                    }
                                }
                                sync_message.visibility = View.GONE
                                this@LoginActivity.startActivity(intent)
                                this@LoginActivity.finish()
                            }
                        } catch (e: Exception) {
                            Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD MAP FILE")
                            e.printStackTrace()
                        }
                    }, Response.ErrorListener { err ->
                        err.printStackTrace()
                    })

                }
            } catch (e: Exception) {
                Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD MODEL FILE")
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            error.printStackTrace()
        })
    }

    private fun unpackZip(path: String, zipname: String): Boolean {
        val `is`: InputStream
        val zis: ZipInputStream
        try {
            var filename: String
            `is` = FileInputStream(path + zipname)
            zis = ZipInputStream(BufferedInputStream(`is`))
            var ze: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(1024)
            var count: Int
            while (ze != null) {
                // zapis do souboru
                filename = ze.getName()

                // Need to create directories if not exists, or
                // it will generate an Exception...
                if (ze.isDirectory()) {
                    val fmd = File(path + filename)
                    fmd.mkdirs()
                    continue
                }

                val fout = FileOutputStream(path + filename)

                // cteni zipu a zapis
                count = zis.read(buffer)
                while (count != -1) {
                    fout.write(buffer, 0, count)
                    count = zis.read(buffer)
                }

                fout.close()
                zis.closeEntry()
                ze = zis.nextEntry
            }

            zis.close()
            val f = File(path+zipname)
            if(f.delete()) {
                Log.d("ZIP", "deleted zip")
            } else {
                Log.d("ZIP", "failed to delete zip")
            }
        } catch (e: IOException) {
            e.printStackTrace()
            return false
        }
        return true
    }

    /*private fun maybeSync(sp: SharedPreferences) {
        val accessToken = sp.getString("id", null)
        val fmtime = sp.getString("fmtime", null)
        val intent = Intent(this@LoginActivity, MapsActivity::class.java)
        val tz = TimeZone.getTimeZone("UTC")
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault())
        sdf.timeZone = tz
        apiController.getFileInfo("freeze_$model_name.pb", accessToken) { response ->
            if(response == null) {
                Toast.makeText(applicationContext, getString(R.string.offline_alert), Toast.LENGTH_SHORT).show()
                this@LoginActivity.startActivity(intent)
                this@LoginActivity.finish()
            } else {
                val newfmtime = response.getString("mtime")
                if(fmtime == null) {
                    val spe = sp.edit()
                    spe.putString("fmtime", newfmtime)
                    spe.apply()
                    syncFilesAndLaunchApp(intent, accessToken)
                } else {
                    val date1 = sdf.parse(fmtime)
                    val date2 = sdf.parse(newfmtime)
                    if (date1.before(date2)) {
                        val spe = sp.edit()
                        spe.putString("fmtime", newfmtime)
                        spe.apply()
                        syncFilesAndLaunchApp(intent, accessToken)
                    } else {
                        this@LoginActivity.startActivity(intent)
                        this@LoginActivity.finish()
                    }
                }
            }
        }
    }

    private fun syncFilesAndLaunchApp(intent: Intent, accessToken: String) {
        apiController.downloadFile("freeze_$model_name.pb", accessToken, Response.Listener<ByteArray> { response ->
            try {
                if (response != null) {
                    val outputStream: FileOutputStream
                    val name = "freeze_$model_name.pb"
                    outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                    outputStream.write(response)
                    outputStream.close()
                    Toast.makeText(this, "PB Download complete.", Toast.LENGTH_LONG).show()
                    apiController.downloadFile("normalize_$model_name.csv", accessToken, Response.Listener<ByteArray> { response ->
                        try {
                            if (response != null) {

                                val outputStream: FileOutputStream
                                val name = "normalize_$model_name.csv"
                                outputStream = openFileOutput(name, Context.MODE_PRIVATE)
                                outputStream.write(response)
                                outputStream.close()
                                Toast.makeText(this, "CSV Download complete.", Toast.LENGTH_LONG).show()
                                this@LoginActivity.startActivity(intent)
                                this@LoginActivity.finish()
                            }
                        } catch (e: Exception) {
                            Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD CSV FILE")
                            e.printStackTrace()
                        }
                    }, Response.ErrorListener { error ->
                        error.printStackTrace()
                    })
                }
            } catch (e: Exception) {
                Log.d("SYNC_ERROR", "UNABLE TO DOWNLOAD PB FILE")
                e.printStackTrace()
            }
        }, Response.ErrorListener { error ->
            error.printStackTrace()
        })
    }
*/
    private fun isEmailValid(email: String): Boolean {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        return password.length > 4
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private fun showProgress(show: Boolean) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        val shortAnimTime = resources.getInteger(android.R.integer.config_shortAnimTime).toLong()

        login_form.visibility = if (show) View.GONE else View.VISIBLE
        login_form.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 0 else 1).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_form.visibility = if (show) View.GONE else View.VISIBLE
                    }
                })

        login_progress.visibility = if (show) View.VISIBLE else View.GONE
        login_progress.animate()
                .setDuration(shortAnimTime)
                .alpha((if (show) 1 else 0).toFloat())
                .setListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        login_progress.visibility = if (show) View.VISIBLE else View.GONE
                    }
                })
    }

    override fun onCreateLoader(i: Int, bundle: Bundle?): Loader<Cursor> {
        return CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE + " = ?", arrayOf(ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE),

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC")
    }

    override fun onLoadFinished(cursorLoader: Loader<Cursor>, cursor: Cursor) {
        val emails = ArrayList<String>()
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS))
            cursor.moveToNext()
        }

        addEmailsToAutoComplete(emails)
    }

    override fun onLoaderReset(cursorLoader: Loader<Cursor>) {

    }

    private fun addEmailsToAutoComplete(emailAddressCollection: List<String>) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        val adapter = ArrayAdapter(this@LoginActivity,
                android.R.layout.simple_dropdown_item_1line, emailAddressCollection)

        email.setAdapter(adapter)
    }

    object ProfileQuery {
        val PROJECTION = arrayOf(
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY
        )
        val IS_PRIMARY = 1
        val ADDRESS = 0
    }


    companion object {

        /**
         * Id to identity READ_CONTACTS permission request.
         */
        private val REQUEST_READ_CONTACTS = 0

    }
}
