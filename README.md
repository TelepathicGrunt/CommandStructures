command is /spawnstructure

# MAVEN

For developers that want to add this mod to their mod's workspace:

<blockquote>repositories {

&nbsp; maven {

&nbsp; &nbsp; url "https://nexus.resourcefulbees.com/repository/maven-public/"

&nbsp; }

}</blockquote>

&nbsp;

Don't forget to change \<modversion> with the actual latest version of this mod like `3.0.0` for example.
(note, see 1.18 Forge branch for old format if you're on 1.18 Forge)

<blockquote>dependencies {


&nbsp; ...


&nbsp; runtimeOnly fg.deobf("com.telepathicgrunt:CommandStructures-Forge:\<modversion>+1.19")


}</blockquote>
