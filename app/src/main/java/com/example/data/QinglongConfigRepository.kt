package com.example.data

import kotlinx.coroutines.flow.Flow

class QinglongConfigRepository(
    private val qinglongConfigDao: QinglongConfigDao,
    private val jdCredentialDao: JDCredentialDao
) {
    val allConfigs: Flow<List<QinglongConfig>> = qinglongConfigDao.getAllConfigs()

    suspend fun getAllConfigsList(): List<QinglongConfig> {
        return qinglongConfigDao.getAllConfigsList()
    }

    suspend fun insert(config: QinglongConfig): Long {
        return qinglongConfigDao.insertConfig(config)
    }

    suspend fun update(config: QinglongConfig) {
        qinglongConfigDao.updateConfig(config)
    }

    suspend fun delete(config: QinglongConfig) {
        qinglongConfigDao.deleteConfig(config)
    }

    // JD Credentials operations
    val allCredentials: Flow<List<JDCredential>> = jdCredentialDao.getAllCredentials()

    suspend fun getAllCredentialsList(): List<JDCredential> {
        return jdCredentialDao.getAllCredentialsList()
    }

    suspend fun insertCredential(credential: JDCredential): Long {
        return jdCredentialDao.insertCredential(credential)
    }

    suspend fun updateCredential(credential: JDCredential) {
        jdCredentialDao.updateCredential(credential)
    }

    suspend fun deleteCredential(credential: JDCredential) {
        jdCredentialDao.deleteCredential(credential)
    }
}
