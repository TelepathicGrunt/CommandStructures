Smarter way to handle structure pieces

**____________________________________________________________________________**

# MAVEN

For developers that want to add CommandStructures to their mod's workspace:

<blockquote>repositories {

&nbsp; maven {

&nbsp; &nbsp; url "https://nexus.resourcefulbees.com/repository/maven-public/"

&nbsp; }

}</blockquote>

&nbsp;

Don't forget to change &lt;modversion> with the actual latest version of this mod.

<blockquote>dependencies {


&nbsp; ...


&nbsp; implementation fg.deobf("com.telepathicgrunt:CommandStructures-Neoforge:&lt;modversion>+1.20.6")


}</blockquote>


-------

## YourKit

![https://www.yourkit.com/images/yklogo.png](https://www.yourkit.com/images/yklogo.png)

YourKit supports open source projects with innovative and intelligent tools
for monitoring and profiling Java and .NET applications.
YourKit is the creator of <a href="https://www.yourkit.com/java/profiler/">YourKit Java Profiler</a>,
<a href="https://www.yourkit.com/dotnet-profiler/">YourKit .NET Profiler</a>,
and <a href="https://www.yourkit.com/youmonitor/">YourKit YouMonitor</a>.

-------
