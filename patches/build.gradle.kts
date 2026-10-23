group = "pixkk"

patches {
    about {
        name = "pixkk patches"
        description = "Personal TikTok 47.4.3 patches: expand descriptions in place, sound names, " +
            "right column spacing, comment menu, old-style seeking and counts, animated profile " +
            "covers, Discover tab."
        source = "local"
        author = "pixkk"
        contact = ""
        website = ""
        license = "GNU General Public License v3.0"
    }
}

dependencies {
    compileOnly(libs.morphe.patcher)

    // Used by JsonGenerator.
    implementation(libs.gson)

    // Required due to smali, or build fails. Can be removed once smali is bumped.
    implementation(libs.guava)

    // Android API stubs defined here.
    compileOnly(project(":patches:stub"))
}
