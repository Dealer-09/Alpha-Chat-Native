package com.example.alpha_chat_native.data.remote

import okhttp3.Interceptor
import okhttp3.Response
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * OkHttp Interceptor that adds session cookie to all API requests.
 * The cookie is stored by TokenManager after OAuth callback.
 */
@Singleton
class AuthInterceptor @Inject constructor(
    private val tokenManager: TokenManager
) : Interceptor {
    
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        
        val requestBuilder = originalRequest.newBuilder()
        
        // Add session cookie if available
        val cookie = tokenManager.getSessionCookie()
        if (cookie != null) {
            Timber.d("AuthInterceptor: Adding cookie to ${originalRequest.url}")
            requestBuilder.addHeader("Cookie", cookie)
        } else {
            Timber.w("AuthInterceptor: No cookie available for ${originalRequest.url}")
        }
        
        // Add common headers
        requestBuilder.addHeader("Accept", "application/json")
        // Only add Content-Type for requests that have a body (POST, PATCH, PUT)
        // Adding it to GET requests is technically incorrect
        if (originalRequest.body != null) {
            requestBuilder.addHeader("Content-Type", "application/json")
        }

        return chain.proceed(requestBuilder.build())
    }
}
