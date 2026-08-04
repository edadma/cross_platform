package io.github.edadma.cross_platform

/** Well-known per-user directories, derived from the environment.
 *
 * `envVar` and `homeDirectory` have to be written once per platform, because reading an environment
 * is exactly what differs between a JVM, a Node process and a native binary. What is built *on* them
 * does not differ at all, so it is written once here — three copies of one `orElse` chain is three
 * chances for the platforms to disagree about where a program's files go, which is the one thing this
 * library exists to prevent.
 */

/** The per-user directory for **derived** files — things a program can rebuild if they are gone.
 *
 * Each platform keeps them somewhere different and each convention is worth honouring, since it is
 * what the platform's own housekeeping knows to look at: `XDG_CACHE_HOME` or `~/.cache` on Linux,
 * `~/Library/Caches` on macOS, `%LOCALAPPDATA%` on Windows. A caller appends its own name to whatever
 * comes back, and should be prepared for `None` — a machine with no home directory has nowhere for
 * this to be, and the caller's fallback is its own business.
 */
def cacheDirectory: Option[String] =
  cacheRoot(envVar("XDG_CACHE_HOME"), envVar("LOCALAPPDATA"), homeDirectory,
            home => isDirectory(s"$home/Library/Caches"))

/** The choice `cacheDirectory` makes, with everything it depends on passed in.
 *
 * Separated so it can be *tested*. The answer is a function of two variables, a home directory and
 * one property of the filesystem — none of which a test can set, since they belong to the machine
 * running it. Written inline, the precedence could only be checked on a machine that happened to be
 * configured to exercise the branch, which means in practice it would never be checked at all: on
 * this author's macOS, `XDG_CACHE_HOME` is unset and the test for it would be skipped every time.
 *
 * macOS is asked about by probing for `~/Library/Caches` rather than by reading an OS name, because
 * the question actually being answered is *does this machine keep caches there* — and a probe cannot
 * disagree with the filesystem the way a name can. An explicit variable wins over the probe
 * everywhere, including on macOS, on the grounds that somebody who set it meant it.
 */
private[cross_platform] def cacheRoot(xdgCacheHome: Option[String], localAppData: Option[String],
                                      home: Option[String], keepsCachesInLibrary: String => Boolean)
    : Option[String] =
  xdgCacheHome
    .orElse(localAppData)
    .orElse(home.map(h => if (keepsCachesInLibrary(h)) s"$h/Library/Caches" else s"$h/.cache"))
