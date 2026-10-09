import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters

/**
 * A shared service (with no state) for running the E2E test tasks that launch WinUI one at a time.
 *
 * The clipboard is shared across the whole OS, so test processes running in parallel compete for it,
 * causing test failures, hangs and crashes of other processes (NTSTATUS 0xC000027B).
 * winui4k.ui-test makes the Test tasks of all modules and all JDKs use it with maxParallelUsages = 1.
 */
abstract class UiTestLock : BuildService<BuildServiceParameters.None>
