# Pages stay blank after a deploy (#178)

## What, Why and Constraints

After #175 was deployed to staging, the Participants page was blank. Every static file, `index.html` and the web component scripts included, was served with `Cache-Control: public, immutable, max-age=86400`. Those names carry no content hash, so the browser kept the previous deploy's files for a day without asking the server. The new `participants-page.js` imports `solidIcon` from `components/icons.js`. The cached `icons.js` didn't export it, so the module failed to load, `<fns-participants-page>` was never defined, and the page stayed empty.

The Playwright tests start from an empty browser cache, so they can't see this.

Constraint: the hashed bundle (`/static/bundle/app-<hash>.js|css`) must keep its long cache. Its name changes with its content.

## How

`application.properties`: a Quarkus HTTP filter sets `Cache-Control: no-cache` on GET/HEAD for every path except `/static/bundle/`, `/api/` and `/q/`. The filter overrides the default header of the static resources. The browser revalidates each file on load: it gets a `304 Not Modified` when the file is unchanged (checked against the prod jar), and the new file after a deploy.

Browsers that already hold an `immutable` copy keep it until it expires (24 h at most), or until a hard refresh.

## Tests

- `StaticCacheTest` (new):
  - `/`, an app URL, `icons.js`, `participants-page.js` and `theme.css` are `no-cache`;
  - the hashed bundle is not.
- Manual check on the prod jar:
  - `no-cache` on the page, the app URLs and the scripts;
  - `public, immutable, max-age=86400` on the bundle;
  - `304` on a conditional request.
