package com.cluster

object PropertyFinder{
    fun get(property: String): String? = System.getenv(property) ?: System.getProperty(property)
}