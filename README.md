command is /spawnstructure

# MAVEN

For developers that want to add this mod to their mod's workspace:

<blockquote>repositories {

&nbsp; maven {

&nbsp; &nbsp; url "https://nexus.resourcefulbees.com/repository/maven-public/"

&nbsp; }

}</blockquote>

&nbsp;

Don't forget to change \<modversion> with the actual latest version of this mod like `1.2.1` for example. (note, see 1.19 branch for new format if you're on 1.19)

<blockquote>dependencies {


&nbsp; ...


&nbsp; runtimeOnly fg.deobf("com.telepathicgrunt:CommandStructures:\<modversion>+1.18.1")


}</blockquote>
