plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt)
    alias(libs.plugins.androidx.room)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "app.jammes.boletim"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "app.jammes.boletim"
        minSdk = 26
        targetSdk = 37
        versionCode = 2
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Os limites do plano grátis. Desligados, todo mundo é Pro: o release fica assim até dar para comprar
        buildConfigField("boolean", "LIMITES_DO_FREE", "false")

        // Os relatórios de falhas do Crashlytics, no meta-data do manifesto
        manifestPlaceholders["crashlyticsLigado"] = true
        // Os números de uso do Analytics, no outro meta-data: true desliga a coleta
        manifestPlaceholders["analyticsDesligado"] = false
    }

    buildTypes {
        debug {
            // Os limites valem, e a chave "Pro de teste" das Configurações mostra os dois lados
            buildConfigField("boolean", "LIMITES_DO_FREE", "true")
            // Tem o mesmo pacote do release: as falhas do desenvolvimento ficam fora do painel
            manifestPlaceholders["crashlyticsLigado"] = false
            // Pelo mesmo motivo, o uso durante o desenvolvimento não entra nos números
            manifestPlaceholders["analyticsDesligado"] = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("minificado") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".min"
            matchingFallbacks += "release"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true // DEBUG liga os dados de teste; LIMITES_DO_FREE, os limites do plano grátis
    }
}

room {
    // Um N.json por versão do banco, no git: é o que as AutoMigrations e o MigrationTestHelper leem
    schemaDirectory("$projectDir/schemas")
}

// O SchemasTest lê esses N.json, então só roda depois de o Room regravá-los
tasks.withType<Test>().configureEach { mustRunAfter("copyRoomSchemas") }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)

    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.vico.compose)
    implementation(libs.vico.compose.m3)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)

    implementation(libs.billing.ktx)
    implementation(libs.play.review.ktx)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.analytics)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}