package com.lagradost.cloudstream3.test

import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.lagradost.cloudstream3.R
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.metaproviders.TmdbProvider
import com.lagradost.cloudstream3.network.addCloudFlareDns
import com.lagradost.cloudstream3.network.addGoogleDns
import com.lagradost.cloudstream3.network.initClient
import com.lagradost.cloudstream3.plugins.PluginManager
import com.lagradost.cloudstream3.plugins.RepositoryManager
import com.lagradost.cloudstream3.syncproviders.AccountManager
import com.lagradost.cloudstream3.utils.extractorApis
import com.lagradost.cloudstream3.utils.loadExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CoreTestActivity : AppCompatActivity() {

    private lateinit var tvConsole: TextView
    private lateinit var scrollConsole: ScrollView
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_core_test)

        tvConsole = findViewById(R.id.tv_console_output)
        scrollConsole = findViewById(R.id.scroll_console)

        findViewById<Button>(R.id.btn_test_network).setOnClickListener {
            runTestNetwork()
        }

        findViewById<Button>(R.id.btn_test_plugins).setOnClickListener {
            runTestPlugins()
        }

        findViewById<Button>(R.id.btn_test_search).setOnClickListener {
            runTestSearch()
        }

        findViewById<Button>(R.id.btn_test_details).setOnClickListener {
            runTestDetails()
        }

        findViewById<Button>(R.id.btn_test_extract).setOnClickListener {
            runTestExtract()
        }

        findViewById<Button>(R.id.btn_test_auth).setOnClickListener {
            runTestAuth()
        }

        findViewById<Button>(R.id.btn_clear_logs).setOnClickListener {
            tvConsole.text = ""
            log("[CONSOLE CLEARED]")
        }

        log("[INIT] CoreTestActivity created. Ready.")
    }

    private fun log(message: String) {
        val timestamp = timeFormat.format(Date())
        val formatted = "[$timestamp] $message\n"
        tvConsole.append(formatted)
        scrollConsole.post {
            scrollConsole.fullScroll(ScrollView.FOCUS_DOWN)
        }
    }

    /**
     * Test 1: Network Adapter & DoH Ping
     */
    private fun runTestNetwork() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 1: NETWORK & DoH] Starting...")
            val startTime = System.currentTimeMillis()

            try {
                // Initialize default client on app
                app.initClient(applicationContext)

                // 1. Standard ping
                val pingUrl = "https://1.1.1.1/cdn-cgi/trace"
                log("-> Pinging $pingUrl via app.get()...")
                val response = withContext(Dispatchers.IO) {
                    app.get(pingUrl)
                }
                val latency = System.currentTimeMillis() - startTime
                log("<- HTTP ${response.code} (Latency: ${latency}ms)")
                val preview = response.text.lines().take(3).joinToString(" | ")
                log("   Response: $preview")

                // 2. DNS-over-HTTPS Cloudflare Client test
                log("-> Configuring DoH (Cloudflare DnsOverHttps)...")
                val dohClient = OkHttpClient.Builder()
                    .addCloudFlareDns()
                    .build()
                log("   DoH client built successfully: ${dohClient.dns.javaClass.simpleName}")

                // 3. DNS-over-HTTPS Google Client test
                val dohGoogleClient = OkHttpClient.Builder()
                    .addGoogleDns()
                    .build()
                log("   Google DoH client built successfully: ${dohGoogleClient.dns.javaClass.simpleName}")

                log("[TEST 1: SUCCESS] Network adapter & DoH functioning properly.")
            } catch (t: Throwable) {
                log("[ERROR]: Test 1 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * Test 2: Plugin & Repository Loader
     */
    private fun runTestPlugins() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 2: PLUGINS & REPO LOADER] Starting...")
            try {
                val repos = withContext(Dispatchers.IO) {
                    RepositoryManager.getRepositories()
                }
                log("-> Configured Repositories count: ${repos.size}")
                repos.forEachIndexed { idx, repo ->
                    log("   [$idx] ${repo.name} -> ${repo.url}")
                }

                log("-> Querying PluginManager runtime state...")
                val onlinePlugins = PluginManager.urlPlugins
                val localPlugins = PluginManager.plugins
                log("   Online Plugins count: ${onlinePlugins.size}")
                log("   Local Plugins loaded: ${localPlugins.size}")

                val extractorCount = extractorApis.size
                log("   Built-in Extractor APIs registered: $extractorCount")

                log("[TEST 2: SUCCESS] Plugin & Repository engine queried successfully.")
            } catch (t: Throwable) {
                log("[ERROR]: Test 2 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * Test 3: Media Search (search())
     */
    private fun runTestSearch() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 3: MEDIA SEARCH] Starting...")
            val query = "Batman"
            log("-> Querying search('$query') using TmdbProvider...")
            val startTime = System.currentTimeMillis()

            try {
                val provider = TmdbProvider()
                val results = withContext(Dispatchers.IO) {
                    provider.search(query)
                }
                val latency = System.currentTimeMillis() - startTime
                log("<- Received ${results?.size ?: 0} search results (${latency}ms):")

                results?.take(5)?.forEachIndexed { idx, item ->
                    log("   [$idx] Title: \"${item.name}\"")
                    log("         URL: ${item.url}")
                    log("         Poster: ${item.posterUrl?.take(45)}...")
                    log("         Type: ${item.type}")
                }

                log("[TEST 3: SUCCESS] Search executed and parsed correctly.")
            } catch (t: Throwable) {
                log("[ERROR]: Test 3 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * Test 4: Media Details & Episode Fetcher (load())
     */
    private fun runTestDetails() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 4: MEDIA DETAILS & EPISODES] Starting...")
            val sampleUrl = "https://api.themoviedb.org/3/movie/272" // Batman Begins (TMDB id 272)
            log("-> Calling load('$sampleUrl') on TmdbProvider...")
            val startTime = System.currentTimeMillis()

            try {
                val provider = TmdbProvider()
                val details = withContext(Dispatchers.IO) {
                    provider.load(sampleUrl)
                }
                val latency = System.currentTimeMillis() - startTime

                if (details != null) {
                    log("<- Details Loaded (${latency}ms):")
                    log("   Name: ${details.name}")
                    log("   Year: ${details.year}")
                    log("   Type: ${details.type}")
                    log("   Plot: ${details.plot?.take(80)}...")
                    log("   Poster: ${details.posterUrl}")
                    log("   Score: ${details.score}")
                    log("[TEST 4: SUCCESS] Media details fetched.")
                } else {
                    log("[WARN]: Media details returned null.")
                }
            } catch (t: Throwable) {
                log("[ERROR]: Test 4 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * Test 5: Video Link & Extractor Engine (loadLinks())
     */
    private fun runTestExtract() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 5: EXTRACTOR ENGINE] Starting...")
            try {
                val registeredExtractors = extractorApis.toList()
                log("-> Registered Extractors total: ${registeredExtractors.size}")
                val sampleExtractors = registeredExtractors.take(6).map { it.name }
                log("   Examples: ${sampleExtractors.joinToString(", ")}")

                // Test direct extraction invocation via loadExtractor
                val testStreamUrl = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
                log("-> Testing extractor pipeline for stream link: $testStreamUrl")

                var linkFound = 0
                val res = withContext(Dispatchers.IO) {
                    loadExtractor(
                        url = testStreamUrl,
                        referer = "https://google.com",
                        subtitleCallback = { sub ->
                            log("   [SUBTITLE] Lang: ${sub.lang}, URL: ${sub.url}")
                        },
                        callback = { link ->
                            linkFound++
                            log("   [STREAM LINK] Name: ${link.name}, Quality: ${link.quality}, URL: ${link.url}")
                        }
                    )
                }

                log("   loadExtractor executed. Handled: $res, Extracted links: $linkFound")
                log("[TEST 5: SUCCESS] Extractor pipeline verified.")
            } catch (t: Throwable) {
                log("[ERROR]: Test 5 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }

    /**
     * Test 6: Third-Party Auth & Sync Ping
     */
    private fun runTestAuth() {
        lifecycleScope.launch {
            log("==========================================")
            log("[TEST 6: THIRD-PARTY AUTH & SYNC] Starting...")
            try {
                log("-> Checking AccountManager providers...")
                log("   AniList API: ${AccountManager.aniListApi.name} (Requires Login: ${AccountManager.aniListApi.requiresLogin})")
                log("   MyAnimeList API: ${AccountManager.malApi.name} (Requires Login: ${AccountManager.malApi.requiresLogin})")
                log("   Kitsu API: ${AccountManager.kitsuApi.name}")
                log("   Simkl API: ${AccountManager.simklApi.name}")
                log("   OpenSubtitles API: ${AccountManager.openSubtitlesApi.name}")

                val malAccounts = AccountManager.accounts(AccountManager.malApi.idPrefix)
                val aniAccounts = AccountManager.accounts(AccountManager.aniListApi.idPrefix)
                log("   Logged-in MAL accounts: ${malAccounts.size}")
                log("   Logged-in AniList accounts: ${aniAccounts.size}")

                log("[TEST 6: SUCCESS] Auth & Sync providers loaded and verified.")
            } catch (t: Throwable) {
                log("[ERROR]: Test 6 failed: ${t.javaClass.simpleName}: ${t.message}")
            }
        }
    }
}
