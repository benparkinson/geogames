# Phase 5: country autocomplete, dark mode, motion, clean-up

Read `README.md` in this folder first. This phase assumes phases 1–4 are done. It changes the backend and the frontend.

## Why

- Tripoint players often misspell countries, or don't know which form of a name the game accepts. They also can't tell a misspelling ("Andora") from a wrong answer, because both show the same "isn't right" message.
- Every country is a possible answer, so a list of all countries gives nothing away. (Rivers gets **no** autocomplete: a list of the 20 rivers would narrow the answer too much.)
- The page ignores the system dark-mode setting.
- A few files are unused.

## Part A: `GET /api/countries` (backend)

### A1. Model

Add `model/CountryName.java`:

```java
public record CountryName(String name, List<String> additionalNames) {}
```

### A2. Loader

In `service/CountryLoaderService.java`:

- Add a constructor that takes a `ResourcePatternResolver`. Spring injects the `ApplicationContext` for it; see how `RiverLoaderService` takes `ApplicationContext`.
  - `TripointFinderTest` mocks this class with `mock(CountryLoaderService.class)`, so a constructor parameter is fine.
  - Check that nothing else constructs it with `new`.
- Add:

  ```java
  @Cacheable("countryNames")
  public List<CountryName> loadCountryNames() throws IOException
  ```

  It resolves `classpath*:/processed/countries/*.json` and reads each file with `readFile(resource.getInputStream(), CountryName.class)`. The shared `ObjectMapper` has `FAIL_ON_UNKNOWN_PROPERTIES` off, so `geoData` is skipped. Map a null `additionalNames` to `List.of()`, and return the list sorted by `name`.
- Close each input stream with try-with-resources. Fix `FileLoaderService.loadFile` the same way while you're there; it currently never closes its stream.

### A3. Controller

Add `controllers/CountryController.java`:

```java
@RestController
@CrossOrigin(origins = {"http://localhost:3000", "https://geogauntlet.onrender.com"})  // keep in sync with GameController
public class CountryController {
  // constructor-inject CountryLoaderService
  @GetMapping("api/countries")
  public ResponseEntity<List<CountryName>> countries() throws IOException {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(Duration.ofDays(1))).body(countryLoaderService.loadCountryNames());
  }
}
```

### A4. Test

Add `src/test/java/com/parkinson/benjamin/geogames/service/CountryLoaderServiceTest.java`, a plain JUnit test. It constructs `new CountryLoaderService(new PathMatchingResourcePatternResolver())` and asserts:
- 255 entries, matching the number of files in `src/main/resources/processed/countries`;
- the list is sorted;
- it contains `France`;
- no entry has a null `additionalNames`.

Run `mvn spotless:apply` and `mvn test`.

### A5. Docs

In `CLAUDE.md`, under "Environment switching", change the CORS sentence to say the origins are hard-coded on **both** `GameController` and `CountryController`. Under "Game flow / API", add one line for `GET /api/countries`: it returns `[{name, additionalNames}]` for every processed country, is used for Tripoint autocomplete and guess validation, and is cached.

## Part B: autocomplete and "unknown country" feedback (frontend)

### B1. Hook

Create `client/src/api/countries.ts`:

```ts
export interface CountryName { name: string; additionalNames: string[] }
export function useCountryNames() // useQuery("countries", fetch serverEndpoint()+"/api/countries", { staleTime: Infinity, cacheTime: Infinity, retry: 1 })
```

### B2. Suggestions

Give `GuessBox` an optional `suggestions?: string[]` prop. When it's set, render a native `<datalist id={useId()}>` with one `<option value={name}/>` per country name, and set `list` on the input. Leave the aliases out of the datalist; they still count when matching.

Use a native datalist, not a custom dropdown: it needs no dependency and works with mobile keyboards.

### B3. Unknown guesses

- Add `{ kind: "unknown"; guess: string }` to `GuessResult`.
- Add an optional `isKnownGuess?: (guess: string) => boolean` prop to `MapGame`. In `submitGuess`, after the empty and duplicate checks and **before** checking correctness: if `isKnownGuess` is set and returns false, return `unknown`. Don't record the guess and don't clear the input.
- In `MapGameInput`, show `We don't recognise "{guess}" as a country. Check the spelling.` and shake the input.

### B4. Wire it up in `Tripoint.tsx`

- Call `useCountryNames()`. Build a memoised `Set` of `normaliseString` for every name and alias.
- Pass the names to the input as `suggestions` (thread them through `MapGame`), and pass `isKnownGuess`.
- **If the query is still loading or has failed, pass neither.** The game then behaves exactly as in phase 1, so a missing endpoint never blocks play.
- Leave `RiverShapes` unchanged.

## Part C: dark mode

- Add `client/pages/_document.tsx` with the standard `Html` / `Head` / `Main` / `NextScript` structure. In `<Head>`, add an inline script (`dangerouslySetInnerHTML`) that:
  - sets `document.documentElement.setAttribute("data-bs-theme", matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")`;
  - adds a `change` listener on the same media query that updates the attribute.

  Bootstrap 5.3 then themes all of its components. Follow the system setting only; **don't add a theme toggle**.
- In `styles.css`, add `[data-bs-theme="dark"] { --gg-bg: #161a23; --gg-panel-bg: #222838; }`. Keep `--gg-map-blank` the same in both themes.
- Replace hard-coded light colours with variables or Bootstrap's theme-aware classes, for example:
  - `bg-white` → `bg-body`;
  - the `.tooltip-inner` colours: delete the rule and let Bootstrap style tooltips;
  - home-card backgrounds → `bg-body` and `bg-primary-subtle`, which are already theme-aware.
- Leave the Leaflet controls and the satellite tiles alone.

## Part D: motion

Add these to `styles.css`, all inside `@media (prefers-reduced-motion: no-preference)`:
- An answer slot that changes from empty to found or revealed plays a 150ms scale-in (`transform: scale(0.85)` → `1`, with opacity). Trigger it with a CSS animation on a `slot--filled` class.
- `RoundResult` fades and slides in over 200ms.
- Home cards and progress dots get a 120ms `transition` on `box-shadow`, `border-color` and `background-color`.

## Part E: clean-up

- Delete `client/src/components/Button.tsx` and `client/src/helper/buttonHelper.ts`. Nothing imports them; confirm with a grep before deleting.
- `client/src/components/Spinner.tsx`: if it's still used, replace its uses with react-bootstrap's `<Spinner animation="border" />` and delete it.
- Check `public/globe.png`. If nothing references it, leave the file in place but tell the user in your report.
- **Keep `client/src/credits.txt`.** It holds attribution that the licences require (Flaticon icons, OpenStreetMap ODbL data).
  - Show it on the home page as a small muted footer: "Icons: Earthz Stocker, Freepik (Flaticon) · River data © OpenStreetMap contributors (ODbL)", with links to the same URLs as the file.
  - Keep the file as the source and update it if anything changes. If the globe icon is no longer used, still keep its credit and mention it in your report.
- Grep for any remaining `id=` / `className` values that refer to CSS rules deleted in phase 2, and remove them.

## Manual checks

- **Tripoint:**
  - Typing "fra" offers "France".
  - "Andora" shows the "don't recognise" message, isn't added to the wrong guesses, and keeps your text.
  - An alias from `geodata/countries/additionalNames.json` is accepted.
- Stop the backend's `/api/countries`, or block the request in DevTools. Tripoint still plays, with no suggestions.
- **Rivers:** no suggestions appear.
- **Dark mode:** switch the OS to dark. The home page, the game panel, the summary, alerts and tooltips are all readable, and the Rivers blank map still contrasts with the river.
- With reduced motion turned on, no animations play.
- The footer credits show on the home page.

## Done when

- [ ] `GET /api/countries` returns the sorted list with cache headers. CORS covers it. `CountryLoaderServiceTest` passes. `mvn spotless:apply` and `mvn test` are clean. `CLAUDE.md` is updated.
- [ ] Tripoint has datalist suggestions and "unknown" feedback, and degrades gracefully without the endpoint.
- [ ] Dark mode follows the system setting with no unreadable elements.
- [ ] The animations respect `prefers-reduced-motion`.
- [ ] Unused files are removed and the credits are shown.
- [ ] `npm run lint` and `npm run build` pass.
