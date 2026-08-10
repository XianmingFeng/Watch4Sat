# osmdroid is used through direct API references in QTH and Orbit maps. Do not
# keep the whole package; release map QA must prove whether narrower rules are
# needed for specific reflective/resource paths.
-dontwarn org.osmdroid.**

# Kotlin serialization's consumer rules retain generated serializers. Project
# models use compile-time serializers and do not require a broad member keep.
