package com.cluster

import com.amazonaws.ClientConfiguration
import com.amazonaws.auth.EnvironmentVariableCredentialsProvider
import com.amazonaws.client.builder.AwsClientBuilder

object AwsConfigurator{
    private val PROXY_HOST = "PROXY_HOST"
    private val PROXY_PORT = "PROXY_PORT"
    private val PROXY_USER = "PROXY_USER"
    private val PROXY_PASS = "PROXY_PASS"
    private val AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID"

    fun <T: AwsClientBuilder<*,*>, R> regionClientFrom(builder: AwsClientBuilder<T,R>): (region: String) -> R = {
        defaultClient(builder, it)
    }

    fun <T: AwsClientBuilder<*,*>, R> defaultClient(builder: AwsClientBuilder<T,R>): R {
        return defaultClientBuilderFrom(builder).build()
    }

    fun <T: AwsClientBuilder<*,*>, R> defaultClient(builder: AwsClientBuilder<T,R>, region: String): R {
        builder.region = region
        return defaultClientBuilderFrom(builder).build()
    }

    fun <T: AwsClientBuilder<*,*>, R> defaultClientBuilderFrom(builder: AwsClientBuilder<T,R>): AwsClientBuilder<T,R> {
        if(builder.region == null) builder.region = "eu-west-1"
        builder.withClientConfiguration(clientConfiguration())
        if(PropertyFinder.get(AWS_ACCESS_KEY_ID) != null)builder.withCredentials(EnvironmentVariableCredentialsProvider())
        return builder
    }


    fun clientConfiguration(): ClientConfiguration?{
        val proxyHost = PropertyFinder.get(PROXY_HOST)
        val proxyPort = PropertyFinder.get(PROXY_PORT)
        return if(proxyHost != null && proxyPort != null)
            ClientConfiguration()
                    .withProxyHost(proxyHost)
                    .withProxyPort(Integer.parseInt(proxyPort))
                    .withProxyUsername(PropertyFinder.get(PROXY_USER))
                    .withProxyPassword(PropertyFinder.get(PROXY_PASS))
        else null
    }


}