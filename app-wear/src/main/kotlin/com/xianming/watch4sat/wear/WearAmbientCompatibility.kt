package com.xianming.watch4sat.wear

object WearAmbientCompatibility {
    val requiredRuntimeClasses: Set<String> = setOf(
        "com.google.wear.Sdk",
        "com.google.wear.services.ambient.AmbientComponentState",
        "com.google.wear.services.ambient.AmbientComponentState\$ActivityStateRegistry",
        "com.google.wear.services.ambient.AmbientManager",
        "com.google.wear.services.ambient.AmbientManager\$AmbientComponentListener",
        "com.google.wear.services.ambient.AmbientManager\$AmbientTransitionListener",
        "com.google.wear.services.ambient.AmbientManager\$Controller",
        "com.google.wear.services.ambient.AmbientOptions"
    )

    fun isRuntimeAvailable(classLoader: ClassLoader): Boolean {
        return requiredRuntimeClasses.all { className ->
            try {
                Class.forName(className, false, classLoader)
                true
            } catch (_: ClassNotFoundException) {
                false
            } catch (_: LinkageError) {
                false
            }
        }
    }
}
