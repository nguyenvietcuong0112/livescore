package com.livescore.football.livescores.footballscores

import com.livescore.football.livescores.footballscores.data.remote.ApiService
import com.livescore.football.livescores.footballscores.data.remote.DecryptionInterceptor
import com.livescore.football.livescores.footballscores.data.remote.model.BaseResponse
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NetworkParseTest {

    private val packageName = "com.livescore.football.livescores.footballscores"
    private val versionCode = "3"

    private fun createClient(logging: HttpLoggingInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor { chain ->
                val originalRequest = chain.request()
                val newUrl = originalRequest.url.newBuilder()
                    .addQueryParameter("param1", packageName)
                    .addQueryParameter("param2", versionCode)
                    .build()

                val request = originalRequest.newBuilder()
                    .url(newUrl)
                    .addHeader("x-param1", packageName)
                    .addHeader("x-param2", versionCode)
                    .addHeader("x-apisports-key", "86cae86b105350834620f2888fe1445e")
                    .build()
                chain.proceed(request)
            }
            .addInterceptor(DecryptionInterceptor(packageName, versionCode))
            .addInterceptor(logging)
            .build()
    }

    @Test
    fun testLeaguesFetch() = runBlocking {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = createClient(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.1teps.com/livescore/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.getLeagues()
            println("SUCCESS: Code: ${response.code}, Msg: ${response.message}, Size: ${response.data.size}")
        } catch (e: Exception) {
            println("ERROR OCCURRED:")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testTopAssistsFetch() = runBlocking {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = createClient(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.1teps.com/livescore/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.getTopAssists(39, 2025)
            println("SUCCESS: Code: ${response.code}, Msg: ${response.message}, Size: ${response.data.size}")
        } catch (e: Exception) {
            println("ERROR OCCURRED:")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testTopScorersFetch() = runBlocking {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = createClient(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.1teps.com/livescore/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.getTopScorers(39, 2025)
            println("SUCCESS: Code: ${response.code}, Msg: ${response.message}, Size: ${response.data.size}")
        } catch (e: Exception) {
            println("ERROR OCCURRED:")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testStandingsFetch() = runBlocking {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = createClient(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.1teps.com/livescore/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.getStandings(1, 2026)
            println("SUCCESS: Code: ${response.code}, Msg: ${response.message}")
            val standings = response.data
            println("Standings leagues wrapper size: ${standings.size}")
            for (w in standings) {
                val league = w.league
                println("League: ${league.name}, season: ${league.season}")
                println("Standings groups list outer size: ${league.standings.size}")
                for ((idx, groupList) in league.standings.withIndex()) {
                    println("  Group $idx size: ${groupList.size}")
                    for (row in groupList) {
                        println("    Team: ${row.team.name}, rank: ${row.rank}, group: ${row.group}")
                    }
                }
            }
        } catch (e: Exception) {
            println("ERROR OCCURRED:")
            e.printStackTrace()
            throw e
        }
    }

    @Test
    fun testMatchDetailsFetch() = runBlocking {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val client = createClient(logging)
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.1teps.com/livescore/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        val apiService = retrofit.create(ApiService::class.java)
        try {
            val response = apiService.getMatchDetail(1504447)
            println("SUCCESS: Code: ${response.code}, Msg: ${response.message}")
        } catch (e: Exception) {
            println("ERROR OCCURRED:")
            e.printStackTrace()
            throw e
        }
    }
}



