command is /spawnstructure

# MAVEN

For developers that want to add this mod to their mod's workspace:

<blockquote>repositories {

&nbsp; maven {

&nbsp; &nbsp; url "https://nexus.resourcefulbees.com/repository/maven-public/"

&nbsp; }

}</blockquote>

&nbsp;

Don't forget to change \<modversion> with the actual latest version of this mod like `1.2.0` for example.

<blockquote>dependencies {


&nbsp; ...


&nbsp; modRuntimeOnly fg.deobf("com.telepathicgrunt:CommandStructures-Fabric:\<modversion>+1.18.1")


}</blockquote>
