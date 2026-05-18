# Cross-Platform - Unified File System Operations for Scala

A lightweight library providing consistent file system and process operations across JVM, Scala.js, and Scala Native platforms.

## Why Cross-Platform?

Different Scala platforms have different APIs for basic operations like reading files or getting command line arguments. This library provides a single, consistent API that works identically across all platforms.

```scala
import io.github.edadma.cross_platform.*

// Same code works on JVM, JS, and Native
val content = readFile("config.json")
writeFile("output.txt", content)
val files = listFiles(".")
```

## Installation

Add to your `build.sbt`:

```scala
libraryDependencies += "io.github.edadma" %%% "cross_platform" % "0.1.7"
```

For cross-platform projects:
```scala
lazy val myProject = crossProject(JSPlatform, JVMPlatform, NativePlatform)
  .settings(
    libraryDependencies += "io.github.edadma" %%% "cross_platform" % "0.1.7"
  )
```

## Core Operations

### File Operations
```scala
// Text files
val content = readFile("data.txt")
writeFile("output.txt", "Hello, World!")
appendFile("log.txt", "New entry\n")

// Binary files
val data = readBytes("image.png")
writeBytes("copy.png", data)

// File information
val exists = exists("file.txt")
val isFile = isFile("document.pdf")
val isDir = isDirectory("folder")
val size = fileSize("data.txt")
val modified = lastModified("file.txt")
```

### Directory Operations
```scala
// List directory contents
val files = listFiles("/home/user")  // Returns file paths
val entries = listDirectoryWithTypes(".")  // Returns DirectoryEntry with types

entries.foreach { entry =>
  entry.fileType match {
    case FileType.File => println(s"File: ${entry.name}")
    case FileType.Directory => println(s"Dir: ${entry.name}")
    case FileType.SymbolicLink => println(s"Link: ${entry.name}")
    case FileType.Other => println(s"Other: ${entry.name}")
  }
}

// Create directories
createDirectory("new-folder")
createDirectories("path/to/nested/folders")  // Creates parent dirs

// Temporary directory under the system's default temp location.
// Returns the absolute path of a freshly created directory; the
// caller owns it and is responsible for cleanup. Uses
// `Files.createTempDirectory` on JVM/Native and `fs.mkdtempSync`
// on Node.
val workDir = createTempDirectory("myapp-")
// → e.g. /var/folders/.../myapp-AbC123 on macOS
```

### File Management
```scala
// Copy and move files
copyFile("source.txt", "backup.txt")
moveFile("temp.txt", "archive/temp.txt")

// Delete files and directories
deleteFile("unwanted.txt")
deleteFile("empty-directory")  // Works for both files and dirs
```

### Process Operations
```scala
// Command line arguments (cross-platform)
val args = processArgs(args)  // Pass main method args

// Output
stdout("Hello, World!")

// Exit
processExit(0)  // or processExit(1) for error
```

### Path Utilities
```scala
// Get platform-specific path separator
val sep = nameSeparator  // "/" on Unix, "\" on Windows

// Check file permissions
val canRead = readableFile("file.txt")
```

### Random Access File I/O
```scala
// Open a file for random read/write access
val raf = openRandomAccessFile("data.bin", "rw")

// Positional read/write
raf.writeInt(0x12345678)
raf.writeLong(42L)
raf.seek(0)
val n = raf.readInt()   // 0x12345678
val v = raf.readLong()  // 42L

// Bulk read/write
val data = Array[Byte](1, 2, 3, 4)
raf.write(data)
raf.seek(0)
val buf = new Array[Byte](4)
raf.readFully(buf)

// File size control
raf.length          // current file size
raf.setLength(1024) // extend or truncate

// Durable writes
raf.fsync()  // flush to disk

raf.close()
```

The `RandomAccessFile` trait implements `java.io.DataInput` and `java.io.DataOutput`, providing the full set of typed read/write methods (`readShort`, `writeDouble`, `readUTF`, etc.). Temporary files can be created with `createTempFile(prefix, suffix)`; temporary directories with `createTempDirectory(prefix)`.

### Unix Domain Sockets
```scala
// Server
val server = createSocketServer("/tmp/my-app.sock")
val conn = server.accept()
val msg = conn.readLine()  // Option[String]
conn.writeLine("response")
conn.close()
server.close()

// Client
val client = connectSocket("/tmp/my-app.sock")
client.writeLine("request")
val response = client.readLine()  // Option[String]
client.close()
```

Line-oriented protocol over POSIX Unix domain sockets. Available on JVM and Scala Native. The JS platform throws `UnsupportedOperationException` (Node.js sockets are async-only).

## Platform Implementations

The library automatically uses the best implementation for each platform:

- **JVM**: Uses `java.nio.files` for robust, high-performance operations
- **Scala.js**: Uses Node.js `fs` and `path` modules for file operations
- **Scala Native**: Uses Java NIO compatibility layer

Same API, optimized implementations.

## File Types

```scala
sealed trait FileType
object FileType {
  case object File extends FileType
  case object Directory extends FileType  
  case object SymbolicLink extends FileType
  case object Other extends FileType  // Device files, pipes, etc.
}

case class DirectoryEntry(name: String, fileType: FileType)
```

## Example: Configuration File Handler

```scala
import io.github.edadma.cross_platform.*

object ConfigManager {
  private val configFile = "app.config"
  
  def loadConfig(): String = {
    if (exists(configFile)) {
      readFile(configFile)
    } else {
      val defaultConfig = """{"theme": "dark", "version": "1.0"}"""
      writeFile(configFile, defaultConfig)
      defaultConfig
    }
  }
  
  def saveConfig(config: String): Unit = {
    writeFile(configFile, config)
  }
  
  def backupConfig(): Unit = {
    if (exists(configFile)) {
      val timestamp = System.currentTimeMillis()
      copyFile(configFile, s"$configFile.backup.$timestamp")
    }
  }
}
```

## Example: Directory Scanner

```scala
import io.github.edadma.cross_platform.*

def scanDirectory(path: String): Unit = {
  if (!exists(path) || !isDirectory(path)) {
    println(s"$path is not a valid directory")
    return
  }
  
  val entries = listDirectoryWithTypes(path)
  
  println(s"Contents of $path:")
  entries.foreach { entry =>
    val size = if (entry.fileType == FileType.File) {
      s" (${fileSize(path + nameSeparator + entry.name)} bytes)"
    } else ""
    
    val typeStr = entry.fileType match {
      case FileType.File => "FILE"
      case FileType.Directory => "DIR "
      case FileType.SymbolicLink => "LINK"
      case FileType.Other => "OTHER"
    }
    
    println(s"  $typeStr: ${entry.name}$size")
  }
}
```

## Error Handling

Operations may throw standard exceptions:
- `IllegalArgumentException` for invalid paths
- `UnsupportedOperationException` for platform limitations
- Platform-specific I/O exceptions for file system errors

## Contributing

This library focuses on providing essential cross-platform operations. Contributions should:

1. Work identically across all three platforms
2. Use platform-optimized implementations
3. Maintain the simple, consistent API
4. Include tests for all platforms

## License

ISC License - see LICENSE file for details.

---

*Write once, run everywhere - the way cross-platform file operations should work in Scala.*