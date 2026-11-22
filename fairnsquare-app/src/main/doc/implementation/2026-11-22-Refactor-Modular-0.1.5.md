# Refactor: modular 0.0.2 → 0.1.5 (#189)

## What, Why and Constraints

fairnsquare moves to the latest release of modular, 0.1.5, which listit and simpleprojects already use. From 0.1.0,
modular checks the module rules when compiling (an annotation processor), and a module must declare what it
`requires`: the other modules it uses, and every dependency outside `java.*` (jakarta, Quarkus, Jackson...), by its
JPMS name.

Constraints: no change to the code itself, only the module declarations and the build; every test keeps passing.

## How

- `pom.xml`: `asymetrik-modular.version` 0.1.5; the `api` artifact is now `annotations`.
- `fairnsquare-app/pom.xml`:
  - `annotations` in `provided` scope (only read when compiling);
  - the `processor` in `annotationProcessorPaths` (javac no longer finds processors on the class path since JDK 23);
  - `-Aorg.asymetrik.modular.treeScan=false`: the processor's scan of method bodies (`Trees.getElement` /
    `getTypeMirror`) makes javac reject every record with a compact constructor ("invalid canonical constructor"),
    as Champollion's did in the vidocq experiment (#184). Compiling then checks the signatures only.
- Every `package-info.java`: `org.asymetrik.modular.annotations.Module`, and a `requires` list, sorted, with what the
  module uses. They were filled from the processor's and the test's messages ("Add "X" to the requires of module
  'Y'").
- `sharedkernel/validation` becomes a module, `validation` (exports its package, requires `jakarta.validation`):
  `split` uses its `@WholeNumber`, and a package in no module can no longer be used.
- `ModularArchitectureTest` uses `ModuleAnalysis` (main classes only) and lists every violation at once, instead of
  stopping at the first one. It is the check that sees types used only inside method bodies.

## Tests

- `mvn package`: 450 tests pass; the formatter check passes.
- Checked by hand that a missing `requires` is caught: removing `"jnanoid"` from `captcha` compiles (the use is in a
  method body) but fails `ModularArchitectureTest` with "Module 'captcha' should add "jnanoid" to its requires".
- Locally, run `mvn clean` after building another branch in the same folder: leftover classes in `target/` (from the
  unmerged vidocq experiment, #184) showed up as violations. A fresh checkout, as in CI, is not affected.
