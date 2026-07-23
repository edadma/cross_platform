package io.github.edadma.cross_platform

/** The outcome of running an external command with `exec`: the process exit code and its
 * captured standard output and standard error.
 *
 * `exitCode` is the command's own exit status when it ran. A failure to start (e.g. the
 * executable was not found) or termination by a signal surfaces as `-1` where the platform
 * detects it at spawn time (JVM, Node); on a `posix_spawn` platform a missing executable
 * instead surfaces as the child's conventional `127`. In all cases a failed run is non-zero.
 */
case class ProcessResult(exitCode: Int, stdout: String, stderr: String)
