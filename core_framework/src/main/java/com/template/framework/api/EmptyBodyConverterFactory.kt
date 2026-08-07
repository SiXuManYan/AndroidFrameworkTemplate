package com.template.framework.api

import okhttp3.ResponseBody
import retrofit2.Converter
import retrofit2.Retrofit
import java.lang.reflect.Type

/** Converts an empty HTTP response body to `null` before delegating non-empty bodies. */
class EmptyBodyConverterFactory private constructor() : Converter.Factory() {

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, *> {
        val delegate = retrofit.nextResponseBodyConverter<Any>(this, type, annotations)
        return Converter { body ->
            if (body.isEmpty()) {
                body.close()
                null
            } else {
                delegate.convert(body)
            }
        }
    }

    private fun ResponseBody.isEmpty(): Boolean {
        if (contentLength() == 0L) return true
        return !source().request(1L)
    }

    companion object {
        @JvmStatic
        fun create(): EmptyBodyConverterFactory = EmptyBodyConverterFactory()
    }
}
