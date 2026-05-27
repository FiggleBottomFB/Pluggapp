package com.example.myapplication

import io.github.jan.supabase.createSupabaseClient

import io.github.jan.supabase.postgrest.Postgrest

object SupabaseManager {

    val client = createSupabaseClient(
        supabaseUrl = "https://rmxabzjvogsdotplqghk.supabase.co",
        supabaseKey = "sb_publishable_gGgjMGOqX2ooZ_kSRrVvxQ_9qxzyg6f"
    ) {

        install(Postgrest)
    }
}