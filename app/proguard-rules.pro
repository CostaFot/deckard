# R8 rules for the release build. Only what is specific to this app belongs here:
# proguard-android-optimize.txt covers the platform, and every library that needs
# rules ships its own (okhttp, retrofit2, kotlinx.serialization, and gson, which
# arrives with LiteRT-LM).

# The Pangram API models are deserialized straight off the wire.
-keep class com.costafotiadis.deckard.net.model.** { *; }

# okhttp reads the public-suffix list as a resource sitting next to this class, so
# the class's package has to survive. okhttp 5 no longer ships this rule itself.
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase
