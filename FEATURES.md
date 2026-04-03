# ICSS compiler — requirements & extensions overview

This table follows the structure of [ASSIGNMENT.md](ASSIGNMENT.md). Use it for your PDF appendix: tick what the product satisfies and describe agreed extension points with your lecturer.

**Legend:** _Prio_ = Must / Should as in the assignment. _Punten_ = max points from the assignment table (where given).

## 4.1 General (AL)

| ID   | Prio | Punten | Requirement (summary)                                         | In this project                  |
| ---- | ---- | ------ | ------------------------------------------------------------- | -------------------------------- |
| AL01 | Must | 0      | Keep startcode package layout; new code in relevant packages. | Yes                              |
| AL02 | Must | 0      | Builds with Maven 3.6+, OpenJDK 13 (not Oracle).              | Yes (`mvn test` / `mvn package`) |
| AL03 | Must | 0      | Readable, maintainable code (docent judgment).                | Yes                              |
| AL04 | Must | 0      | Own work; meets APP-6 compiler/language criteria (docent).    | Yes                              |

## 4.2 Parsing (PA)

| ID   | Prio | Punten | Requirement (summary)                                                   | In this project                    |
| ---- | ---- | ------ | ----------------------------------------------------------------------- | ---------------------------------- |
| PA00 | Must | 0      | Parser uses your generic stack for `ASTNode` (`IHANStack`).             | Yes (`ASTListener`)                |
| PA01 | Must | 10     | Grammar + listener: simple styling; `level0.icss`; `testParseLevel0()`. | Yes                                |
| PA02 | Must | 10     | Variables `:=` and use; `level1.icss`; `testParseLevel1()`.             | Yes                                |
| PA03 | Must | 10     | `+`, `-`, `*` with precedence; `level2.icss`; `testParseLevel2()`.      | Yes (+ `/` as extension, see EX01) |
| PA04 | Must | 10     | `if` / `else`; `level3.icss`; `testParseLevel3()`.                      | Yes                                |
| PA05 | Must | 0      | PA01–PA04 together ≥ 30 points.                                         | Yes                                |

## 4.3 Checking (CH)

| ID   | Prio   | Punten | Requirement (summary)                                               | In this project                        |
| ---- | ------ | ------ | ------------------------------------------------------------------- | -------------------------------------- |
| CH00 | Must   | 0      | At least four of CH01–CH06 implemented.                             | Yes                                    |
| CH01 | Should | 5      | No use of undefined variables.                                      | Yes                                    |
| CH02 | Should | 5      | `+`/`-`: same type; `*`: at least one scalar (e.g. no `2px * 3px`). | Yes (`/` same spirit as `*`, see EX01) |
| CH03 | Should | 5      | No colors in arithmetic (`+`, `-`, `*`).                            | Yes (also applies to `/`)              |
| CH04 | Should | 5      | Declaration value type matches property (`width`, `color`, etc.).   | Yes                                    |
| CH05 | Should | 5      | `if` condition is boolean (literal or variable).                    | Yes                                    |
| CH06 | Must   | 5      | Variables only used within scope.                                   | Yes                                    |

## 4.4 Transform (TR)

| ID   | Prio | Punten | Requirement (summary)                                                        | In this project                 |
| ---- | ---- | ------ | ---------------------------------------------------------------------------- | ------------------------------- |
| TR01 | Must | 10     | `Evaluator`: replace expressions with evaluated literals (single traversal). | Yes (+ divide evaluation, EX01) |
| TR02 | Must | 10     | `Evaluator`: remove `IfClause`; branch on boolean; optional `ElseClause`.    | Yes                             |

## 4.5 Generate (GE)

| ID   | Prio | Punten | Requirement (summary)                       | In this project |
| ---- | ---- | ------ | ------------------------------------------- | --------------- |
| GE01 | Must | 5      | `Generator`: AST → CSS2-compliant string.   | Yes             |
| GE02 | Must | 5      | Indent CSS with two spaces per scope level. | Yes             |

## 4.6 Own extensions

| ID   | Prio | Punten | Extension (summary)                                                                                                                                                                                                                                                                  | In this project |
| ---- | ---- | ------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | --------------- |
| EX01 | —    |        | **Division (`/`)** — same precedence as `*`; parse (`ICSS.g4`, `ASTListener`), `DivideOperation`, checker rules, `Evaluator`; example `divide.icss` (GUI: Load example).                                                                                                             | Yes             |
| EX02 | —    |        | **Fixed variable types** — after a variable’s first concrete type, later assignments must match (as in assignment §4.6 example `10px` then `5%` forbidden). Implemented in `Checker` (`fixedVariableTypes`); examples `strict-types-valid.icss` / `strict-types-invalid.icss` (GUI). | Yes             |

Twee uitbreidingen die de volledige pipeline volgen waar nodig: **lexer → parser → AST → checker → evaluator → gegenereerde CSS**. De generator blijft werken op literalen na transformatie; er is geen aparte CSS-syntax voor de nieuwe taalconstructies nodig.

### 1. Delen (`/`)

Delen heeft dezelfde prioriteit als vermenigvuldigen en is links-associatief (net als `*` in de bestaande grammatica). Er is een lexer-token `DIV`, een parserregel in `multiplicativeExpression`, een AST-knoop `DivideOperation`, semantische regels in de `Checker` (vergelijkbaar met `*`: geen kleuren in rekenkundige expressies; geldige combinaties zoals scalar/scalar, pixel gedeeld door scalar, percentage gedeeld door scalar, ofzelfde eenheid gedeeld door dezelfde eenheid voor een scalar quotiënt), en evaluatie in de `Evaluator` (o.a. deling door nul wordt afgewezen met een fout op de knoop).

**Voorbeeld ICSS** (`divide.icss`, ook via _File → Load example ICSS_):

```icss
p {
  width: 100px / 2;
}
```

Na **Parse → Check → Transform → Generate** wordt de breedte een enkele pixel-literal; de CSS bevat effectief `50px` (geheeltallige deling zoals bij de andere literals).

### 2. Vast type per variabelenaam

Elke variabelenaam krijgt bij de **eerste** toekenning met een **concreet** afgeleid type (pixel, percentage, kleur, scalar of boolean) een vast type voor de rest van het hele stylesheet. Een latere toekenning aan **dezelfde naam** met een **ander** concreet type levert een semantische fout van de checker (in de trant van: variabele moet één type houden). Dit sluit aan bij het voorbeeld in de opdrachttekst (`Var := 10px;` daarna `Var := 5%;` mag niet). Toekenningen waarvan het type nog `UNDEFINED` is (bijvoorbeeld door een eerdere fout) leggen het vaste type niet vast.

**Voorbeeld dat wél mag** (`strict-types-valid.icss`):

```icss
Size := 10px;
Size := 40px;

p {
  width: Size;
}
```

**Voorbeeld dat wordt afgekeurd** (`strict-types-invalid.icss` — na _Check_ verschijnt een fout op de tweede assignment):

```icss
Size := 10px;
Size := 5%;

p {
  width: Size;
}
```

### Tests bij de uitbreiding

- **Delen:** in `ParserTest` staat `testParseDivideFeature()`: die parseert `divide.icss` uit `src/test/resources` en vergelijkt de AST met de hand opgebouwde verwachting in `Fixtures.uncheckedDivideFeature()`. Zo is gecontroleerd dat lexer, grammatica en `ASTListener` het `/`-pad correct in de boom zetten. (De level0–level3-tests blijven de basisdekking van de opdracht.)
- **Vaste types:** er is **geen** aparte JUnit-testklasse voor alleen de checker; je kunt het gedrag verifiëren met de voorbeelden `strict-types-valid.icss` en `strict-types-invalid.icss` in `src/main/resources` via de GUI (_Parse_ en daarna _Check_). Wil je hetzelfde niveau automatisering als in het min/max-voorbeeld, dan kun je later nog een kleine testklasse toevoegen die de volledige `Pipeline` gebruikt en op fouten / succes assert.
