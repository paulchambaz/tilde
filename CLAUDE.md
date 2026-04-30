# Tilde

Minimalistic Android launcher. Three-page vertical swipe: notifications (up),
home (center), app drawer (down). Home shows time, date, and configurable
favorites. App drawer has a fuzzy search bar. Settings control favorites,
labels, and gesture targets.

---

## Philosophy

This project follows the suckless philosophy.

> Designing simple and elegant software is far more difficult than letting
> ad-hoc or over-ambitious features obscure the code over time. However one
> has to pay this price to achieve reliability and maintainability.

Concretely:

- **Less code is more.** Every line added is a liability. If something can be
  removed without losing function, remove it.
- **No abstraction for its own sake.** Don't create layers, managers, helpers,
  or factories unless the problem genuinely requires them. Three similar lines
  are better than a premature abstraction.
- **No features by default.** Don't implement what wasn't asked for. Don't
  design for hypothetical future requirements.
- **No clever code.** Clever code is hard to read and hard to delete. Obvious
  code is better even if it is longer.
- **Complexity is the enemy.** When the code is getting complicated, the answer
  is not more code — it is a simpler design.

---

## Workflow

For each development session:

1. **Select** the next unchecked task from `ROADMAP.md`. Work top to bottom,
   one task at a time. Mark it `[x]` when done.

2. **Plan** before writing code. State what files will be created or changed,
   what the interface looks like, and what the acceptance criterion is. Get
   alignment before implementation.

3. **Implement** the task. Keep diffs small. Prefer editing existing files over
   creating new ones. No speculative additions.

4. **Test** after implementation. Write a unit test for every non-trivial piece
   of logic added. Tests live in `app/src/test/`. No test is better than a
   test that only passes because it mocks everything interesting away — test
   real behavior.

5. **Update** the roadmap.

# Tools

To build the project, please run :

```sh
just build
```

To run unit tests:

```sh
just test
```

Tests live in `app/src/test/`. They run on the JVM — no device or emulator
needed. Use `./gradlew testDebugUnitTest` directly for verbose output.
