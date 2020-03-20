package com.procurement.dossier.domain.model

import com.procurement.dossier.domain.util.Result

class Ocid private constructor(val value: String) {
    companion object {
        private val regex = "^([a-z]{4})-([a-z0-9]{6})-([A-Z]{2})-([0-9]{13})-([A-Z]{2})-([0-9]{13})\$".toRegex()

        fun tryCreate(ocid: String): Result<Ocid, String> {
            return if (ocid.matches(regex = regex))
                Result.success(Ocid(value = ocid))
            else
                Result.failure(regex.pattern)
        }
    }
}
