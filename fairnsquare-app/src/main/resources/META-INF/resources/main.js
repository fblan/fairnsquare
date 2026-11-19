import { apiRequest } from './infra/api.js';
import './components/toaster.js';

/**
 * The pages, by the address: the home (/), a split's dashboard (/splits/<id>) and its expenses, participants and
 * settlement (/splits/<id>/expenses...), and the admin page (/admin). A split page gets the split id as its
 * `split-id` attribute. Anything else shows the home, at /.
 *
 * The server sends index.html for all these addresses, so a shared link or a reload opens the right page. Inside
 * the app, navigate() (infra/navigation.js) changes the address and fires `popstate`, like the back button.
 */
const HOME = { tag: 'fns-home-page', module: './biz/home/home-page.js' };
const ROUTES = [
  { path: /^\/$/, ...HOME },
  { path: /^\/splits\/([^/]+)$/, tag: 'fns-split-page', module: './biz/split/split-page.js' },
  { path: /^\/splits\/([^/]+)\/expenses$/, tag: 'fns-expenses-page', module: './biz/expenses/expenses-page.js' },
  { path: /^\/splits\/([^/]+)\/participants$/, tag: 'fns-participants-page', module: './biz/participants/participants-page.js' },
  { path: /^\/splits\/([^/]+)\/settlement$/, tag: 'fns-settlement-page', module: './biz/settlement/settlement-page.js' },
  { path: /^\/admin$/, tag: 'fns-admin-page', module: './biz/admin/admin-page.js' },
];

function pageFor(path) {
  for (const route of ROUTES) {
    const match = path.match(route.path);
    if (match) return { ...route, splitId: match[1] && decodeURIComponent(match[1]) };
  }
  return null;
}

let showing = 0;

async function show() {
  const call = ++showing;
  // /splits/<id>/expenses/ is /splits/<id>/expenses: the address is rewritten, so the pages (and the tabs) see one form.
  const path = location.pathname.length > 1 ? location.pathname.replace(/\/+$/, '') : location.pathname;
  let page = pageFor(path);
  if (!page) {
    history.replaceState({}, '', '/');
    page = { ...HOME };
  } else if (path !== location.pathname) {
    history.replaceState({}, '', path + location.search);
  }
  await import(page.module);
  // The address changed again while the module loaded: that later call shows its page.
  if (call !== showing) return;
  const container = document.querySelector('.page');
  const current = container.firstElementChild;
  // A new element for another page or another split: each page starts from a clean state.
  if (current?.localName !== page.tag || (page.splitId && current.getAttribute('split-id') !== page.splitId)) {
    const element = document.createElement(page.tag);
    if (page.splitId) element.setAttribute('split-id', page.splitId);
    container.replaceChildren(element);
    window.scrollTo(0, 0);
  }
}

function showLoadError() {
  const message = document.createElement('p');
  message.className = 'load-error';
  message.textContent = 'The page could not be loaded. Check your connection and reload.';
  document.querySelector('.page').replaceChildren(message);
}

/** The build's commit, under every page. Not essential: nothing shows when it cannot be read. */
async function showBuild() {
  try {
    const info = await apiRequest('/info');
    const footer = document.querySelector('footer');
    footer.textContent = info.commit.slice(0, 7);
    footer.hidden = false;
  } catch {
    // no footer
  }
}

window.addEventListener('popstate', () => show().catch(showLoadError));
show().catch(showLoadError);
showBuild();
