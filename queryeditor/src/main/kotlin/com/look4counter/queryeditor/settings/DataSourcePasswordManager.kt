package com.look4counter.queryeditor.settings

import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.CredentialStore
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager

class DataSourcePasswordManager(
    private val projectKey: String,
    private val store: CredentialStore = PasswordSafe.instance,
) {
    fun readPassword(id: String): String? {
        check(!ApplicationManager.getApplication().isDispatchThread) { "Credential storage must run outside EDT" }
        return store.get(CredentialAttributes(generateServiceName("GlueFramework GlueQueryEditor", "$projectKey/$id")))?.getPasswordAsString()
    }

    fun applyChanges(changes: Map<String, String?>) {
        check(!ApplicationManager.getApplication().isDispatchThread) { "Credential storage must run outside EDT" }
        val attributes = changes.keys.associateWith {
            CredentialAttributes(generateServiceName("GlueFramework GlueQueryEditor", "$projectKey/$it"))
        }
        val previous = attributes.mapValues { store.get(it.value) }
        try {
            changes.forEach { (id, password) ->
                store.set(attributes.getValue(id), password?.let { Credentials(null, it) })
            }
        } catch (failure: Exception) {
            previous.forEach { (id, credentials) -> runCatching { store.set(attributes.getValue(id), credentials) } }
            throw failure
        }
    }
}
