# ====================================================================
# Regras ProGuard / R8 para o GiroCusto
# ====================================================================

# Preserva nomes de arquivos e números de linha para relatórios de crash legíveis
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Preserva anotações de runtime e assinaturas genéricas (essencial para reflexão e serialização)
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations

# --------------------------------------------------------------------
# Room Database
# --------------------------------------------------------------------
-keep class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep @androidx.room.Dao interface * { *; }
-keep @androidx.room.Entity class * { *; }
-dontwarn androidx.room.paging.**

# --------------------------------------------------------------------
# Moshi (JSON Serialization & Codegen)
# --------------------------------------------------------------------
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}
-keep @com.squareup.moshi.JsonClass class * { *; }
-keep class *JsonAdapter {
    public <init>(com.squareup.moshi.Moshi);
    public <init>(com.squareup.moshi.Moshi, java.lang.reflect.Type[]);
}
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# --------------------------------------------------------------------
# Retrofit 2
# --------------------------------------------------------------------
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*

# --------------------------------------------------------------------
# OkHttp 3 & Okio
# --------------------------------------------------------------------
-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn javax.annotation.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**

# --------------------------------------------------------------------
# Osmdroid (OpenStreetMap)
# --------------------------------------------------------------------
-keep class org.osmdroid.** { *; }
-dontwarn org.osmdroid.**

# --------------------------------------------------------------------
# Regras Cirúrgicas do GiroCusto (T-29 / SEC-05B)
# Permite a ofuscação e minificação completa de repositórios, utilitários,
# clientes de rede e lógicas internas, preservando cirurgicamente apenas
# o estritamente exigido em tempo de execução por reflexão / contratos.
# --------------------------------------------------------------------

# Interfaces de Serviço Retrofit (acessadas via Proxy dinâmico e reflexão)
-keep,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-keep interface com.example.network.supabase.SupabaseAuthApi { *; }
-keep interface com.example.network.supabase.SupabaseRestApi { *; }
-keep interface com.example.network.fipe.FipeApi { *; }
-keep interface com.example.network.viacep.ViaCepApi { *; }
-keep interface com.example.network.OverpassApiService { *; }

# Adapters gerados pelo Moshi Codegen (preserva métodos fromJson/toJson)
-keep class *JsonAdapter extends com.squareup.moshi.JsonAdapter { *; }

# Contratos essenciais de integridade e isolamento de sessão/dono
-keep interface com.example.data.DataOwnerStorage {
    public abstract <methods>;
}
-keep interface com.example.network.supabase.SecureSessionStorage {
    public abstract <methods>;
}

