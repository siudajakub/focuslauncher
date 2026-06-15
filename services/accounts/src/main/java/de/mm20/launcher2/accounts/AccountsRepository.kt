package de.mm20.launcher2.accounts

import android.app.Activity
import android.content.Context

interface AccountsRepository {
    fun signin(context: Activity, accountType: AccountType)
    fun signout(accountType: AccountType)

    /**
     * Whether support for this account type is enabled in this build
     */
    fun isSupported(accountType: AccountType): Boolean

    suspend fun getCurrentlySignedInAccount(accountType: AccountType): Account?
}

internal class AccountsRepositoryImpl(
    context: Context
) : AccountsRepository {
    override fun signin(context: Activity, accountType: AccountType) {}
    override fun signout(accountType: AccountType) {}
    override fun isSupported(accountType: AccountType): Boolean = false
    override suspend fun getCurrentlySignedInAccount(accountType: AccountType): Account? = null
}